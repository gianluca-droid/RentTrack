package com.renttrack.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.renttrack.app.PropertyManager
import com.renttrack.app.data.export.CsvExporter
import com.renttrack.app.data.export.XlsxExporter
import com.renttrack.app.data.export.ExportService
import com.renttrack.app.data.model.*
import com.renttrack.app.data.repository.SupabaseRentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SPropertySummaryEntry(
    val condominioId: String,
    val unitCount: Int,
    val totalMonthlyRent: Double,
    val totalMorosita: Double,
    val expiringContracts: Int
)

class SupabaseRentViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("renttrack_prefs", Context.MODE_PRIVATE)
    val repo = SupabaseRentRepository(prefs)

    // ── Loading ──────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Diventa true dopo il PRIMO refresh() completato — usato da MainActivity
    // per evitare il flash di CondominioSelector durante il login
    private val _initialLoadDone = MutableStateFlow(false)
    val initialLoadDone: StateFlow<Boolean> = _initialLoadDone

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun clearError() { _error.value = null }

    // ── Active condominio ─────────────────────────────────────────────
    private val _activeCondominioId = MutableStateFlow(
        PropertyManager.getActiveCondominioId(application)
    )
    val activeCondominioId: StateFlow<String> = _activeCondominioId

    fun setActiveCondominio(id: String) {
        PropertyManager.setActiveCondominioId(getApplication(), id)
        _activeCondominioId.value = id
        refresh()
    }

    fun clearActiveCondominio() {
        PropertyManager.clearActiveCondominio(getApplication())
        _activeCondominioId.value = ""
    }

    // ── Data StateFlows ───────────────────────────────────────────────
    private val _allCondomini = MutableStateFlow<List<SCondominio>>(emptyList())
    val allCondomini: StateFlow<List<SCondominio>> = _allCondomini

    val activeCondominio: StateFlow<SCondominio?> = combine(_activeCondominioId, _allCondomini) { id, list ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _units = MutableStateFlow<List<SCondoUnit>>(emptyList())
    val units: StateFlow<List<SCondoUnit>> = _units

    private val _expenses = MutableStateFlow<List<SExpense>>(emptyList())
    val expenses: StateFlow<List<SExpense>> = _expenses

    private val _payments = MutableStateFlow<List<SPayment>>(emptyList())
    val payments: StateFlow<List<SPayment>> = _payments

    private val _cedolini = MutableStateFlow<List<SCedolino>>(emptyList())
    val cedolini: StateFlow<List<SCedolino>> = _cedolini

    private val _cedoliniWithItems = MutableStateFlow<List<SCedolinoWithItems>>(emptyList())
    val cedoliniWithItems: StateFlow<List<SCedolinoWithItems>> = _cedoliniWithItems

    private val _tenantHistory = MutableStateFlow<List<STenantHistory>>(emptyList())
    val tenantHistory: StateFlow<List<STenantHistory>> = _tenantHistory

    // ── Derived ───────────────────────────────────────────────────────
    val totalExpenses: StateFlow<Double> = _expenses
        .map { it.sumOf { e -> e.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayments: StateFlow<Double> = _payments
        .map { it.sumOf { p -> p.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pendingCedolini: StateFlow<Int> = _cedolini
        .map { it.count { c -> c.status == "Emesso" || c.status == "Scaduto" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val morositaByUnit: StateFlow<Map<String, Double>> = _cedolini
        .map { list ->
            list.filter { it.status != "Pagato" && it.total > it.paidAmount }
                .groupBy { it.unitId }
                .mapValues { (_, ceds) -> ceds.sumOf { it.total - it.paidAmount } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val mesiArretratiByUnit: StateFlow<Map<String, Int>> = _cedolini
        .map { list ->
            list.filter { it.status != "Pagato" }
                .groupBy { it.unitId }
                .mapValues { (_, ceds) -> ceds.size }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val lastCedolinoByUnit: StateFlow<Map<String, SCedolino>> = _cedolini
        .map { list ->
            list.groupBy { it.unitId }
                .mapValues { (_, ceds) -> ceds.maxByOrNull { it.dueDate } }
                .filterValues { it != null }
                .mapValues { it.value!! }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val expensesByCategory: StateFlow<List<CategorySummary>> = _expenses
        .map { list ->
            list.groupBy { it.category }
                .map { (cat, items) -> CategorySummary(cat, items.sumOf { it.amount }) }
                .sortedByDescending { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val propertySummaryMap: StateFlow<Map<String, SPropertySummaryEntry>> =
        combine(_allCondomini, _units, _cedolini) { condos, units, cedolini ->
            val now = System.currentTimeMillis()
            val soon = now + 60L * 24 * 60 * 60 * 1000
            val cedByUnit = cedolini.groupBy { it.unitId }
            condos.associate { condo ->
                val propUnits = units.filter { it.condominioId == condo.id }
                condo.id to SPropertySummaryEntry(
                    condominioId = condo.id,
                    unitCount = propUnits.size,
                    totalMonthlyRent = propUnits.sumOf { it.millesimi },
                    totalMorosita = propUnits.sumOf { unit ->
                        (cedByUnit[unit.id] ?: emptyList())
                            .filter { it.status != "Pagato" && it.total > it.paidAmount }
                            .sumOf { it.total - it.paidAmount }
                    },
                    expiringContracts = propUnits.count {
                        it.leaseEndDate != null && it.leaseEndDate > now && it.leaseEndDate < soon
                    }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // ── Year selection (Reports) ───────────────────────────────────────
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear
    fun setSelectedYear(year: Int) { _selectedYear.value = year }

    val availableYears: StateFlow<List<Int>> = combine(_expenses, _payments) { exp, pay ->
        val cal = Calendar.getInstance()
        val expYears = exp.map { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) }
        val payYears = pay.map { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) }
        (expYears + payYears).distinct().sortedDescending()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyExpenses: StateFlow<List<MonthlyTotal>> = combine(_expenses, _selectedYear) { exp, year ->
        val cal = Calendar.getInstance()
        exp.filter { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) == year }
            .groupBy { cal.apply { timeInMillis = it.date }.get(Calendar.MONTH) + 1 }
            .map { (month, items) -> MonthlyTotal(month, items.sumOf { it.amount }) }
            .sortedBy { it.month }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyPayments: StateFlow<List<MonthlyTotal>> = combine(_payments, _selectedYear) { pay, year ->
        val cal = Calendar.getInstance()
        pay.filter { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) == year }
            .groupBy { cal.apply { timeInMillis = it.date }.get(Calendar.MONTH) + 1 }
            .map { (month, items) -> MonthlyTotal(month, items.sumOf { it.amount }) }
            .sortedBy { it.month }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yearlyExpenses: StateFlow<List<YearlyTotal>> = _expenses
        .map { list ->
            val cal = Calendar.getInstance()
            list.groupBy { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) }
                .map { (year, items) -> YearlyTotal(year, items.sumOf { it.amount }, items.size) }
                .sortedByDescending { it.year }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yearlyPayments: StateFlow<List<YearlyTotal>> = _payments
        .map { list ->
            val cal = Calendar.getInstance()
            list.groupBy { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) }
                .map { (year, items) -> YearlyTotal(year, items.sumOf { it.amount }, items.size) }
                .sortedByDescending { it.year }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Report Scope (multi-proprieta) ─────────────────────────────────────
    enum class ReportScope { ACTIVE, ALL, CUSTOM }
    private val _reportScope = MutableStateFlow(ReportScope.ACTIVE)
    val reportScope: StateFlow<ReportScope> = _reportScope
    private val _reportSelectedIds = MutableStateFlow<Set<String>>(emptySet())
    val reportSelectedIds: StateFlow<Set<String>> = _reportSelectedIds
    private val _reportExpenses = MutableStateFlow<List<SExpense>>(emptyList())
    private val _reportPayments = MutableStateFlow<List<SPayment>>(emptyList())
    private val _reportIsLoading = MutableStateFlow(false)
    val reportIsLoading: StateFlow<Boolean> = _reportIsLoading

    val reportTotalExpenses: StateFlow<Double> = _reportExpenses
        .map { it.sumOf { e -> e.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val reportTotalPayments: StateFlow<Double> = _reportPayments
        .map { it.sumOf { p -> p.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val reportExpensesByCategory: StateFlow<List<CategorySummary>> = _reportExpenses
        .map { list -> list.groupBy { it.category }
            .map { (cat, items) -> CategorySummary(cat, items.sumOf { it.amount }) }
            .sortedByDescending { it.total } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reportMonthlyExpenses: StateFlow<List<MonthlyTotal>> =
        combine(_reportExpenses, _selectedYear) { exp, year ->
            val cal = Calendar.getInstance()
            exp.filter { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) == year }
                .groupBy { cal.apply { timeInMillis = it.date }.get(Calendar.MONTH) + 1 }
                .map { (month, items) -> MonthlyTotal(month, items.sumOf { it.amount }) }
                .sortedBy { it.month }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reportMonthlyPayments: StateFlow<List<MonthlyTotal>> =
        combine(_reportPayments, _selectedYear) { pay, year ->
            val cal = Calendar.getInstance()
            pay.filter { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) == year }
                .groupBy { cal.apply { timeInMillis = it.date }.get(Calendar.MONTH) + 1 }
                .map { (month, items) -> MonthlyTotal(month, items.sumOf { it.amount }) }
                .sortedBy { it.month }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reportYearlyExpenses: StateFlow<List<YearlyTotal>> = _reportExpenses
        .map { list -> val cal = Calendar.getInstance()
            list.groupBy { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) }
                .map { (year, items) -> YearlyTotal(year, items.sumOf { it.amount }, items.size) }
                .sortedByDescending { it.year } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reportYearlyPayments: StateFlow<List<YearlyTotal>> = _reportPayments
        .map { list -> val cal = Calendar.getInstance()
            list.groupBy { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) }
                .map { (year, items) -> YearlyTotal(year, items.sumOf { it.amount }, items.size) }
                .sortedByDescending { it.year } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reportAvailableYears: StateFlow<List<Int>> =
        combine(_reportExpenses, _reportPayments) { exp, pay ->
            val cal = Calendar.getInstance()
            val ey = exp.map { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) }
            val py = pay.map { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) }
            (ey + py).distinct().sortedDescending()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setReportScope(scope: ReportScope, customIds: Set<String> = emptySet()) {
        _reportScope.value = scope
        _reportSelectedIds.value = customIds
        viewModelScope.launch {
            _reportIsLoading.value = true
            try {
                val condIds = when (scope) {
                    ReportScope.ACTIVE -> listOf(_activeCondominioId.value).filter { it.isNotBlank() }
                    ReportScope.ALL    -> _allCondomini.value.map { it.id }
                    ReportScope.CUSTOM -> customIds.toList()
                }
                val allExp = mutableListOf<SExpense>()
                val allPay = mutableListOf<SPayment>()
                withContext(Dispatchers.IO) {
                    condIds.forEach { id ->
                        allExp += repo.getExpensesByCondominio(id)
                        allPay += repo.getPaymentsByCondominio(id)
                    }
                }
                _reportExpenses.value = allExp
                _reportPayments.value = allPay
            } catch (e: Exception) { _error.value = e.message
            } finally { _reportIsLoading.value = false }
        }
    }

    // ── Export CSV — delega a CsvExporter + ExportService ───────────────

    /**
     * Export diretto con selezione immobile.
     * Carica i dati da Supabase, applica filtri, genera e condivide il CSV.
     *
     * @param mode       0=proprietà attiva, 1=tutte, 2=selezione personalizzata
     * @param customIds  IDs usati quando mode=2
     * @param filterYear Anno di filtro (null = tutti)
     */
    fun exportCSVAfterScope(
        context: Context,
        mode: Int,
        customIds: Set<String>,
        activeCondoId: String,
        filterYear: Int? = null
    ) = viewModelScope.launch {
        try {
            // Determina gli ID condominio da includere
            val condIds = when (mode) {
                0    -> listOf(activeCondoId).filter { it.isNotBlank() }
                1    -> _allCondomini.value.map { it.id }
                2    -> customIds.toList()
                else -> listOf(activeCondoId)
            }

            // Etichetta leggibile per l'intestazione del CSV
            val activeCondo = _allCondomini.value.find { it.id == activeCondoId }
            val scopeLabel = when (mode) {
                0    -> activeCondo?.run { "$nome${if (indirizzo.isNotBlank()) " — $indirizzo" else ""}" } ?: "Proprietà attiva"
                1    -> "Tutte le proprietà (${_allCondomini.value.size})"
                2    -> "${customIds.size} proprietà selezionate"
                else -> "Proprietà attiva"
            }

            // Recupera dati da Supabase (IO thread)
            val allExp = mutableListOf<SExpense>()
            val allPay = mutableListOf<SPayment>()
            withContext(Dispatchers.IO) {
                condIds.forEach { id ->
                    allExp += repo.getExpensesByCondominio(id)
                    allPay += repo.getPaymentsByCondominio(id)
                }
            }

            // Filtro per anno
            val cal = Calendar.getInstance()
            val filteredPay = if (filterYear != null)
                allPay.filter { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) == filterYear }
            else allPay
            val filteredExp = if (filterYear != null)
                allExp.filter { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) == filterYear }
            else allExp

            // Mappa id → condominio per la risoluzione delle etichette
            val condoMap = _allCondomini.value.associateBy { it.id }

            // Genera contenuto CSV professionale
            val csvContent = CsvExporter.buildCsvContent(
                payments   = filteredPay,
                expenses   = filteredExp,
                condoMap   = condoMap,
                scopeLabel = scopeLabel,
                filterYear = filterYear
            )

            // Nome file leggibile
            val fileName = when (mode) {
                0    -> ExportService.buildFileName(
                            condoName    = activeCondo?.nome ?: "",
                            condoAddress = activeCondo?.indirizzo ?: "",
                            condoCitta   = activeCondo?.citta ?: "",
                            filterYear   = filterYear
                        )
                1    -> ExportService.buildFileName("tutte_le_proprieta", "", "", filterYear)
                2    -> ExportService.buildFileName("selezione_${customIds.size}_proprieta", "", "", filterYear)
                else -> ExportService.buildFileName("renttrack", "", "", filterYear)
            }

            // Scrivi in cache (IO) e apri share sheet (main)
            val file = withContext(Dispatchers.IO) {
                ExportService.writeToCache(context, csvContent, fileName)
            }
            ExportService.shareFile(context, file)

            val rowCount = filteredPay.size + filteredExp.size
            _backupStatus.value = "✅ CSV esportato: $rowCount righe — $fileName"

        } catch (e: Exception) {
            _backupStatus.value = "❌ Errore esportazione: ${e.message}"
        }
    }

    /**
     * Export XLSX professionale con fogli multipli e styling brand.
     * Stessa firma di exportCSVAfterScope, produce .xlsx via XlsxExporter.
     */
    fun exportXLSXAfterScope(
        context: Context,
        mode: Int,
        customIds: Set<String>,
        activeCondoId: String,
        filterYear: Int? = null
    ) = viewModelScope.launch {
        _backupStatus.value = "⏳ Generazione Excel in corso…"
        try {
            val condIds = when (mode) {
                0    -> listOf(activeCondoId).filter { it.isNotBlank() }
                1    -> _allCondomini.value.map { it.id }
                2    -> customIds.toList()
                else -> listOf(activeCondoId)
            }
            val activeCondo = _allCondomini.value.find { it.id == activeCondoId }
            val scopeLabel = when (mode) {
                0    -> activeCondo?.run { "$nome${if (indirizzo.isNotBlank()) " — $indirizzo" else ""}" } ?: "Proprietà attiva"
                1    -> "Tutte le proprietà (${_allCondomini.value.size})"
                2    -> "${customIds.size} proprietà selezionate"
                else -> "Proprietà attiva"
            }
            val allExp = mutableListOf<SExpense>()
            val allPay = mutableListOf<SPayment>()
            withContext(Dispatchers.IO) {
                condIds.forEach { id ->
                    allExp += repo.getExpensesByCondominio(id)
                    allPay += repo.getPaymentsByCondominio(id)
                }
            }
            val cal = Calendar.getInstance()
            val filteredPay = if (filterYear != null)
                allPay.filter { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) == filterYear }
            else allPay
            val filteredExp = if (filterYear != null)
                allExp.filter { cal.apply { timeInMillis = it.date }.get(Calendar.YEAR) == filterYear }
            else allExp
            val condoMap = _allCondomini.value.associateBy { it.id }
            val xlsxBytes = withContext(Dispatchers.IO) {
                XlsxExporter.buildXlsx(
                    payments   = filteredPay,
                    expenses   = filteredExp,
                    condoMap   = condoMap,
                    scopeLabel = scopeLabel,
                    filterYear = filterYear
                )
            }
            val fileName = when (mode) {
                0    -> ExportService.buildXlsxFileName(
                            condoName    = activeCondo?.nome ?: "",
                            condoAddress = activeCondo?.indirizzo ?: "",
                            condoCitta   = activeCondo?.citta ?: "",
                            filterYear   = filterYear
                        )
                1    -> ExportService.buildXlsxFileName("tutte_le_proprieta", "", "", filterYear)
                2    -> ExportService.buildXlsxFileName("selezione_${customIds.size}_proprieta", "", "", filterYear)
                else -> ExportService.buildXlsxFileName("renttrack", "", "", filterYear)
            }
            val file = withContext(Dispatchers.IO) {
                ExportService.writeBytesToCache(context, xlsxBytes, fileName)
            }
            ExportService.shareXlsxFile(context, file)
            val rowCount = filteredPay.size + filteredExp.size
            _backupStatus.value = "✅ Excel esportato: $rowCount movimenti — $fileName"
        } catch (e: Exception) {
            _backupStatus.value = "❌ Errore Excel: ${e.message}"
        }
    }

    /**
     * Export veloce basato sullo scope corrente del report
     * (usato dal pulsante diretto in ReportsScreen).
     */
    fun exportCSV(context: Context) = viewModelScope.launch {
        try {
            val condoMap   = _allCondomini.value.associateBy { it.id }
            val activeCondo = _allCondomini.value.find { it.id == _activeCondominioId.value }

            val scopeLabel = when (_reportScope.value) {
                ReportScope.ACTIVE -> activeCondo?.run { "$nome${if (indirizzo.isNotBlank()) " — $indirizzo" else ""}" } ?: "Proprietà attiva"
                ReportScope.ALL    -> "Tutte le proprietà (${_allCondomini.value.size})"
                ReportScope.CUSTOM -> "${_reportSelectedIds.value.size} proprietà selezionate"
            }

            val csvContent = CsvExporter.buildCsvContent(
                payments   = _reportPayments.value,
                expenses   = _reportExpenses.value,
                condoMap   = condoMap,
                scopeLabel = scopeLabel,
                filterYear = null
            )

            val fileName = when (_reportScope.value) {
                ReportScope.ACTIVE -> ExportService.buildFileName(
                    condoName    = activeCondo?.nome ?: "",
                    condoAddress = activeCondo?.indirizzo ?: "",
                    condoCitta   = activeCondo?.citta ?: "",
                    filterYear   = null
                )
                ReportScope.ALL    -> ExportService.buildFileName("tutte_le_proprieta", "", "", null)
                ReportScope.CUSTOM -> ExportService.buildFileName("selezione", "", "", null)
            }

            val file = withContext(Dispatchers.IO) {
                ExportService.writeToCache(context, csvContent, fileName)
            }
            ExportService.shareFile(context, file)

            val rowCount = _reportPayments.value.size + _reportExpenses.value.size
            _backupStatus.value = "✅ CSV esportato: $rowCount righe — $fileName"

        } catch (e: Exception) {
            _backupStatus.value = "❌ Errore esportazione: ${e.message}"
        }
    }

    private val _documenti = MutableStateFlow<List<SDocumento>>(emptyList())
    val documenti: StateFlow<List<SDocumento>> = _documenti
    val documentCount: StateFlow<Int> = _documenti.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Backup status ─────────────────────────────────────────────────
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus
    fun clearBackupStatus() { _backupStatus.value = null }

    fun backupDatabase(context: Context) {
        _backupStatus.value = "ℹ️ I dati sono su Supabase cloud — nessun backup locale necessario"
    }

    fun restoreDatabase(context: Context, uri: Uri) {
        _backupStatus.value = "ℹ️ Ripristino non disponibile — i dati sono sincronizzati automaticamente su Supabase"
    }

    // ── Documenti — implementazione reale ─────────────────────────────
    fun addDocumento(
        uri: Uri, titolo: String, categoria: String, note: String, mimeType: String,
        sommario: String = "", visibilita: String = "Tutti", destinatari: String = ""
    ) = viewModelScope.launch {
        try {
            val condId = _activeCondominioId.value.ifBlank {
                _backupStatus.value = "❌ Nessuna proprietà selezionata"; return@launch
            }
            val context = getApplication<android.app.Application>()
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.readBytes()
            } ?: run { _backupStatus.value = "❌ File non leggibile"; return@launch }

            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "documento"
            _backupStatus.value = "⬆️ Upload in corso…"

            val filePath = repo.uploadDocumentoFile(bytes, fileName, mimeType, condId)
            if (filePath == null) {
                _backupStatus.value = "❌ Upload fallito — controlla la connessione"
                return@launch
            }

            repo.insertDocumento(SDocumento(
                condominioId = condId,
                titolo = titolo.ifBlank { fileName },
                categoria = categoria,
                filePath = filePath,
                fileName = fileName,
                fileSize = bytes.size.toLong(),
                fileType = com.renttrack.app.data.model.FileTypes.fromMimeType(mimeType),
                note = note,
                sommario = sommario,
                visibilita = visibilita,
                destinatariUnitIds = destinatari
            ))
            refresh()
            _backupStatus.value = "✅ Documento caricato con successo"
        } catch (e: Exception) {
            _backupStatus.value = "❌ Errore: ${e.message}"
        }
    }

    fun updateDocumento(doc: SDocumento) = viewModelScope.launch {
        try { repo.updateDocumento(doc); refresh() }
        catch (e: Exception) { _backupStatus.value = "❌ Errore modifica: ${e.message}" }
    }

    fun deleteDocumento(doc: SDocumento) = viewModelScope.launch {
        try { repo.deleteDocumento(doc.id); refresh() }
        catch (e: Exception) { _backupStatus.value = "❌ Errore eliminazione: ${e.message}" }
    }

    // ── UI state ──────────────────────────────────────────────────────
    private val _collapsedScales = MutableStateFlow<Set<String>>(emptySet())
    val collapsedScales: StateFlow<Set<String>> = _collapsedScales
    fun toggleScala(scala: String) {
        _collapsedScales.update { if (scala in it) it - scala else it + scala }
    }

    // ── Init & Refresh ────────────────────────────────────────────
    // refresh() NON viene chiamato automaticamente qui:
    // viene triggerato da MainActivity solo dopo AuthState.LoggedIn.

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _allCondomini.value = repo.getCondomini()
                val condId = _activeCondominioId.value
                if (condId.isNotBlank()) {
                    _units.value = repo.getUnitsByCondominio(condId)
                    _expenses.value = repo.getExpensesByCondominio(condId)
                    _payments.value = repo.getPaymentsByCondominio(condId)
                    val ceds = repo.getCedoliniByCondominio(condId)
                    _cedolini.value = ceds
                    _cedoliniWithItems.value = ceds.map { c ->
                        SCedolinoWithItems(c, repo.getCedolinoItems(c.id))
                    }
                    _tenantHistory.value = repo.getTenantHistory(condId)
                    _documenti.value = repo.getDocumenti(condId)
                    // Sync report data in base allo scope corrente
                    when (_reportScope.value) {
                        ReportScope.ACTIVE -> {
                            _reportExpenses.value = _expenses.value
                            _reportPayments.value = _payments.value
                        }
                        ReportScope.ALL -> {
                            val allExp = mutableListOf<SExpense>()
                            val allPay = mutableListOf<SPayment>()
                            _allCondomini.value.forEach { condo ->
                                allExp += repo.getExpensesByCondominio(condo.id)
                                allPay += repo.getPaymentsByCondominio(condo.id)
                            }
                            _reportExpenses.value = allExp
                            _reportPayments.value = allPay
                        }
                        ReportScope.CUSTOM -> {
                            val ids = _reportSelectedIds.value
                            if (ids.isNotEmpty()) {
                                val allExp = mutableListOf<SExpense>()
                                val allPay = mutableListOf<SPayment>()
                                ids.forEach { id ->
                                    allExp += repo.getExpensesByCondominio(id)
                                    allPay += repo.getPaymentsByCondominio(id)
                                }
                                _reportExpenses.value = allExp
                                _reportPayments.value = allPay
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
                _initialLoadDone.value = true   // sblocca il NavHost
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────
    fun getUnitName(unitId: String) =
        _units.value.find { it.id == unitId }?.let { "Int. ${it.number} - ${it.ownerName}" } ?: "Sconosciuto"

    // ── Condominio CRUD ───────────────────────────────────────────────
    fun addCondominio(c: SCondominio, andSelect: Boolean = false) = viewModelScope.launch {
        try {
            val newId = repo.insertCondominio(c)
            if (andSelect) setActiveCondominio(newId) else refresh()
        } catch (e: Exception) { _error.value = e.message }
    }

    fun updateCondominio(c: SCondominio) = viewModelScope.launch {
        try { repo.updateCondominio(c); refresh() } catch (e: Exception) { _error.value = e.message }
    }

    fun deleteCondominio(c: SCondominio) = viewModelScope.launch {
        try {
            repo.deleteCondominio(c.id)
            if (_activeCondominioId.value == c.id) clearActiveCondominio()
            refresh()
        } catch (e: Exception) { _error.value = e.message }
    }

    // ── Unit CRUD ─────────────────────────────────────────────────────
    fun addUnit(u: SCondoUnit) = viewModelScope.launch {
        try { repo.insertUnit(u); refresh() } catch (e: Exception) { _error.value = e.message }
    }

    fun updateUnit(u: SCondoUnit) = viewModelScope.launch {
        try { repo.updateUnit(u); refresh() } catch (e: Exception) { _error.value = e.message }
    }

    fun deleteUnit(u: SCondoUnit) = viewModelScope.launch {
        try { repo.deleteUnit(u.id); refresh() } catch (e: Exception) { _error.value = e.message }
    }

    // ── Expense CRUD ──────────────────────────────────────────────────
    fun addExpense(e: SExpense) = viewModelScope.launch {
        try { repo.insertExpense(e); refresh() } catch (e2: Exception) { _error.value = e2.message }
    }

    fun updateExpense(e: SExpense) = viewModelScope.launch {
        try { repo.updateExpense(e); refresh() } catch (e2: Exception) { _error.value = e2.message }
    }

    fun deleteExpense(e: SExpense) = viewModelScope.launch {
        try { repo.deleteExpense(e.id); refresh() } catch (e2: Exception) { _error.value = e2.message }
    }

    // ── Payment CRUD ──────────────────────────────────────────────────
    fun addPayment(p: SPayment) = viewModelScope.launch {
        try { repo.insertPayment(p); refresh() } catch (e: Exception) { _error.value = e.message }
    }

    fun deletePayment(p: SPayment) = viewModelScope.launch {
        try { repo.deletePayment(p.id); refresh() } catch (e: Exception) { _error.value = e.message }
    }

    // ── Cedolino CRUD ─────────────────────────────────────────────────
    fun addCedolinoWithItems(c: SCedolino, items: List<SCedolinoItem>) = viewModelScope.launch {
        try {
            // Safety: se il caller non ha passato condominioId usa l'active
            val condId = c.condominioId.ifBlank { _activeCondominioId.value }
            if (condId.isBlank()) { _error.value = "Nessuna proprietà selezionata"; return@launch }
            repo.insertCedolinoWithItems(c.copy(condominioId = condId), items)
            refresh()
        } catch (e: Exception) { _error.value = e.message }
    }

    fun updateCedolino(c: SCedolino) = viewModelScope.launch {
        try { repo.updateCedolino(c); refresh() } catch (e: Exception) { _error.value = e.message }
    }

    fun deleteCedolino(c: SCedolino) = viewModelScope.launch {
        try { repo.deleteCedolino(c.id, c.unitId, c.condominioId, c.dueDate); refresh() } catch (e: Exception) { _error.value = e.message }
    }

    fun markCedolinoPaid(c: SCedolino) = viewModelScope.launch {
        try {
            repo.updateCedolino(c.copy(status = "Pagato", paidAmount = c.total, paidDate = System.currentTimeMillis()))
            refresh()
        } catch (e: Exception) { _error.value = e.message }
    }

    fun markCedolinoSent(c: SCedolino) = viewModelScope.launch {
        try {
            repo.updateCedolino(c.copy(sentToResident = true, sentAt = System.currentTimeMillis()))
            refresh()
        } catch (e: Exception) { _error.value = e.message }
    }

    fun markCedolinoPaidWithPayment(c: SCedolino, method: String, reference: String = "") = viewModelScope.launch {
        try {
            repo.updateCedolino(c.copy(status = "Pagato", paidAmount = c.total, paidDate = System.currentTimeMillis()))
            repo.insertPayment(SPayment(
                unitId = c.unitId,
                condominioId = c.condominioId,
                amount = c.total,
                date = System.currentTimeMillis(),
                method = method,
                reference = reference.ifBlank { "Cedolino ${c.period}" },
                cedolinoId = c.id,
                notes = "Pagamento automatico da cedolino"
            ))
            refresh()
        } catch (e: Exception) { _error.value = e.message }
    }

    fun generateCedoliniForAllUnits(period: String, dueDate: Long) = viewModelScope.launch {
        try {
            val condId = _activeCondominioId.value.ifBlank { return@launch }
            val currentUnits = _units.value.filter { it.millesimi > 0 }
            val existingPeriods = _cedolini.value.groupBy { it.unitId }
                .mapValues { (_, ceds) -> ceds.map { it.period }.toSet() }
            // Mese/anno di riferimento dalla dueDate (per posizionare la scadenza nel mese giusto)
            val refCal = Calendar.getInstance().apply { timeInMillis = dueDate }
            for (unit in currentUnits) {
                if (existingPeriods[unit.id]?.contains(period) == true) continue
                // Scadenza personalizzata per unità: giorno dall'anagrafica, mese/anno dal riferimento
                val unitDueCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR,         refCal.get(Calendar.YEAR))
                    set(Calendar.MONTH,        refCal.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, unit.paymentDayOfMonth.coerceIn(1, 28))
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
                }
                repo.insertCedolinoWithItems(
                    SCedolino(unitId = unit.id, condominioId = condId, period = period,
                        issueDate = System.currentTimeMillis(), dueDate = unitDueCal.timeInMillis,
                        total = unit.millesimi, status = "Emesso"),
                    listOf(SCedolinoItem(description = "Canone affitto $period", amount = unit.millesimi))
                )
            }
            refresh()
        } catch (e: Exception) { _error.value = e.message }
    }

    fun generateMonthlyPaymentPlan(unit: SCondoUnit) = viewModelScope.launch {
        try {
            val condId = _activeCondominioId.value.ifBlank { return@launch }
            val start = unit.leaseStartDate ?: return@launch
            val end = unit.leaseEndDate ?: return@launch
            if (unit.millesimi <= 0) return@launch
            val mesi = listOf("Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno",
                "Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre")
            val cal = Calendar.getInstance().apply {
                timeInMillis = start; set(Calendar.DAY_OF_MONTH, 1)
            }
            val calEnd = Calendar.getInstance().apply { timeInMillis = end }
            val existing = _cedolini.value.filter { it.unitId == unit.id }.map { it.period }.toSet()
            while (!cal.after(calEnd)) {
                val month = cal.get(Calendar.MONTH)
                val year = cal.get(Calendar.YEAR)
                val period = "${mesi[month]} $year"
                if (period !in existing) {
                    val dueCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, if (month == 11) year + 1 else year)
                        set(Calendar.MONTH, if (month == 11) 0 else month + 1)
                        set(Calendar.DAY_OF_MONTH, unit.paymentDayOfMonth.coerceIn(1, 28))
                    }
                    repo.insertCedolinoWithItems(
                        SCedolino(unitId = unit.id, condominioId = condId, period = period,
                            issueDate = System.currentTimeMillis(), dueDate = dueCal.timeInMillis,
                            total = unit.millesimi, status = "Emesso"),
                        listOf(SCedolinoItem(description = "Canone affitto $period", amount = unit.millesimi))
                    )
                }
                cal.add(Calendar.MONTH, 1)
            }
            refresh()
        } catch (e: Exception) { _error.value = e.message }
    }

    fun duplicateCedolino(cwi: SCedolinoWithItems) = viewModelScope.launch {
        try {
            val mesi = listOf("Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno",
                "Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre")
            val parts = cwi.cedolino.period.trim().split(" ")
            val nextPeriod = if (parts.size == 2) {
                val idx = mesi.indexOfFirst { it.equals(parts[0], true) }
                if (idx >= 0) {
                    val ni = (idx + 1) % 12
                    val ny = if (ni == 0) (parts[1].toIntOrNull() ?: 0) + 1 else parts[1].toIntOrNull() ?: 0
                    "${mesi[ni]} $ny"
                } else "Copia - ${cwi.cedolino.period}"
            } else "Copia - ${cwi.cedolino.period}"
            repo.insertCedolinoWithItems(
                cwi.cedolino.copy(id = "", period = nextPeriod, issueDate = System.currentTimeMillis(),
                    status = "Emesso", sentToResident = false, sentAt = null, paidAmount = 0.0, paidDate = null),
                cwi.items.map { it.copy(id = "", cedolinoId = "") }
            )
            refresh()
        } catch (e: Exception) { _error.value = e.message }
    }

    // ── Tenant History ────────────────────────────────────────────────
    fun changeTenant(
        unit: SCondoUnit,
        exitNotes: String,
        newOwnerName: String,
        newOwnerEmail: String = "",
        newOwnerPhone: String = "",
        newLeaseStart: Long? = null,
        newLeaseEnd: Long? = null,
        newMonthlyRent: Double = unit.millesimi,
        newPaymentDay: Int = unit.paymentDayOfMonth
    ) = viewModelScope.launch {
        try {
            repo.insertTenantHistory(STenantHistory(
                unitId = unit.id, condominioId = unit.condominioId,
                tenantName = unit.ownerName, tenantEmail = unit.ownerEmail,
                tenantPhone = unit.ownerPhone,
                leaseStart = unit.leaseStartDate, leaseEnd = unit.leaseEndDate,
                monthlyRent = unit.millesimi, exitNotes = exitNotes,
                exitDate = System.currentTimeMillis()
            ))
            repo.updateUnit(unit.copy(
                ownerName = newOwnerName, ownerEmail = newOwnerEmail, ownerPhone = newOwnerPhone,
                leaseStartDate = newLeaseStart, leaseEndDate = newLeaseEnd,
                millesimi = newMonthlyRent, paymentDayOfMonth = newPaymentDay
            ))
            refresh()
        } catch (e: Exception) { _error.value = e.message }
    }

    fun deleteTenantHistory(h: STenantHistory) = viewModelScope.launch {
        try { repo.deleteTenantHistory(h.id); refresh() } catch (e: Exception) { _error.value = e.message }
    }

    // ── Quota Diretta ─────────────────────────────────────────────────
    fun addQuotaDirecta(
        unitId: String, importo: Double, descrizione: String,
        categoria: String, periodo: String, dueDate: Long
    ) = viewModelScope.launch {
        try {
            val condId = _activeCondominioId.value.ifBlank { return@launch }
            repo.insertCedolinoWithItems(
                SCedolino(
                    unitId = unitId, condominioId = condId,
                    period = periodo, issueDate = System.currentTimeMillis(),
                    dueDate = dueDate, total = importo, status = "Emesso"
                ),
                listOf(SCedolinoItem(description = descrizione, amount = importo))
            )
            refresh()
        } catch (e: Exception) { _error.value = e.message }
    }
}

data class CategorySummary(val category: String, val total: Double)
data class MonthlyTotal(val month: Int, val total: Double)
data class YearlyTotal(val year: Int, val total: Double, val count: Int)

package com.renttrack.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.renttrack.app.PropertyManager
import com.renttrack.app.data.SampleData
import com.renttrack.app.data.database.AppDatabase
import com.renttrack.app.data.model.*
import com.renttrack.app.data.repository.RentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Riepilogo per singola proprietà — usato nella PropertySelectorScreen */
data class PropertySummary(
    val unitCount: Int,
    val totalMonthlyRent: Double,
    val totalMorosita: Double,
    val expiringContracts: Int     // contratti in scadenza entro 60 giorni
)

@OptIn(ExperimentalCoroutinesApi::class)
class RentViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = RentRepository(
        db.condominioDao(), db.unitDao(), db.expenseDao(),
        db.paymentDao(), db.cedolinoDao(), db.documentoDao(),
        db.tenantHistoryDao()
    )

    // ─── Condominio Attivo ───────────────────────────────────────
    private val _activeCondominioId = MutableStateFlow(
        PropertyManager.getActiveCondominioId(application)
    )
    val activeCondominioId: StateFlow<Long> = _activeCondominioId

    val activeCondominio: StateFlow<Condominio?> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { id -> flow { emit(repository.getCondominioById(id)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Eagerly: il flow resta sempre attivo, nessun flash di emptyList() alla riconnessione
    val allCondomini: StateFlow<List<Condominio>> = repository.allCondomini
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setActiveCondominio(id: Long) {
        PropertyManager.setActiveCondominioId(getApplication(), id)
        _activeCondominioId.value = id
        resetUiState()  // reset filtri/viste al cambio condominio
    }

    fun clearActiveCondominio() {
        PropertyManager.clearActiveCondominio(getApplication())
        _activeCondominioId.value = -1L
        resetUiState()
    }

    /** Azzera tutto lo stato UI legato al condominio corrente */
    private fun resetUiState() {
        _collapsedScales.value = emptySet()
    }


    // ─── State Flows (dipendono dal condominio attivo) ───────────
    val units: StateFlow<List<CondoUnit>> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getUnitsByCondominio(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getExpensesByCondominio(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getPaymentsByCondominio(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cedolini: StateFlow<List<Cedolino>> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getAllCedolini(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cedoliniWithItems: StateFlow<List<CedolinoWithItems>> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getAllCedoliniWithItems(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExpenses: StateFlow<Double> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getTotalExpenses(it).map { v -> v ?: 0.0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayments: StateFlow<Double> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getTotalPayments(it).map { v -> v ?: 0.0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pendingCedolini: StateFlow<Int> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getPendingCedoliniCount(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Cedolini NON ancora inviati al condomino — usato per badge nella bottom bar */
    val unsentCedoliniCount: StateFlow<Int> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getUnsentCedoliniCount(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val expensesByCategory = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getExpensesByGroupedCategory(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documenti: StateFlow<List<Documento>> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getDocumentiByCondominio(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documentCount: StateFlow<Int> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getDocumentCount(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ─── Riepilogo globale per PropertySelectorScreen ───────────────────────
    private val allUnitsGlobal: StateFlow<List<CondoUnit>> = repository.getAllUnitsGlobal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allCedoliniGlobal: StateFlow<List<Cedolino>> = repository.getAllCedoliniGlobal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Mappa condominioId → PropertySummary, aggiornata in tempo reale */
    val propertySummaryMap: StateFlow<Map<Long, PropertySummary>> =
        combine(allUnitsGlobal, allCedoliniGlobal) { units, cedolini ->
            val now  = System.currentTimeMillis()
            val soon = now + 60L * 24 * 60 * 60 * 1000   // 60 giorni
            val cedByUnit = cedolini.groupBy { it.unitId }
            units.groupBy { it.condominioId }.mapValues { (_, propUnits) ->
                PropertySummary(
                    unitCount        = propUnits.size,
                    totalMonthlyRent = propUnits.sumOf { it.millesimi },
                    totalMorosita    = propUnits.sumOf { unit ->
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
        // Eagerly: stesso motivo di allCondomini — nessun flash sul ritorno alla schermata
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // ─── Anno selezionato (per ripartizione mensile) ──────────────
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    fun setSelectedYear(year: Int) { _selectedYear.value = year }

    val monthlyExpenses: StateFlow<List<com.renttrack.app.data.dao.MonthTotal>> =
        combine(_activeCondominioId, _selectedYear) { condId, year -> condId to year }
            .filter { it.first > 0 }
            .flatMapLatest { (condId, year) -> repository.getMonthlyExpenses(condId, year) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyPayments: StateFlow<List<com.renttrack.app.data.dao.MonthTotal>> =
        combine(_activeCondominioId, _selectedYear) { condId, year -> condId to year }
            .filter { it.first > 0 }
            .flatMapLatest { (condId, year) -> repository.getMonthlyPayments(condId, year) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yearlyExpenses: StateFlow<List<com.renttrack.app.data.dao.YearTotal>> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getYearlyExpenses(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yearlyPayments: StateFlow<List<com.renttrack.app.data.dao.YearTotal>> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getYearlyPayments(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableYears: StateFlow<List<Int>> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { condId ->
            combine(
                repository.getExpenseYears(condId),
                repository.getPaymentYears(condId)
            ) { ey, py -> (ey + py).distinct().sortedDescending() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Morosità: importo non pagato per unit ──────────────────────────────
    /** Totale affitti non pagati (o scaduti) per ogni inquilino */
    val morositaByUnit: StateFlow<Map<Long, Double>> = cedolini
        .map { list ->
            list.filter { it.status != "Pagato" && it.total > it.paidAmount }
                .groupBy { it.unitId }
                .mapValues { (_, ceds) -> ceds.sumOf { it.total - it.paidAmount } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Numero di mesi in arretrato per ogni inquilino */
    val mesiArretratiByUnit: StateFlow<Map<Long, Int>> = cedolini
        .map { list ->
            list.filter { it.status != "Pagato" }
                .groupBy { it.unitId }
                .mapValues { (_, ceds) -> ceds.size }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Ultimo cedolino per unit (per semaforo dashboard) */
    val lastCedolinoByUnit: StateFlow<Map<Long, Cedolino>> = cedolini
        .map { list ->
            list.groupBy { it.unitId }
                .mapValues { (_, ceds) -> ceds.maxByOrNull { it.dueDate } }
                .filterValues { it != null }
                .mapValues { it.value!! }   // safe: null già filtrati sopra
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ─── UI State persistente: Unità ─────────────────────────────
    // Set delle scale COLLASSATE (default = tutte espanse = set vuoto)
    private val _collapsedScales = MutableStateFlow<Set<String>>(emptySet())
    val collapsedScales: StateFlow<Set<String>> = _collapsedScales

    fun toggleScala(scala: String) {
        _collapsedScales.update { current ->
            if (scala in current) current - scala else current + scala
        }
    }

    fun isScalaExpanded(scala: String): Boolean = scala !in _collapsedScales.value

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            try {
                val count = repository.condominioCount.first()
                if (count == 0) {
                    val defaultCondoId = SampleData.populateDatabase(repository)
                    setActiveCondominio(defaultCondoId)
                }
            } catch (e: Exception) {
                android.util.Log.e("RentViewModel", "Errore init DB: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── Condominio CRUD ─────────────────────────────────────────
    fun addCondominio(condominio: Condominio, andSelect: Boolean = false) =
        viewModelScope.launch {
            val id = repository.insertCondominio(condominio)
            if (andSelect) setActiveCondominio(id)
        }

    fun updateCondominio(condominio: Condominio) = viewModelScope.launch {
        repository.updateCondominio(condominio)
    }

    fun deleteCondominio(condominio: Condominio) = viewModelScope.launch {
        repository.deleteCondominio(condominio)
        if (_activeCondominioId.value == condominio.id) clearActiveCondominio()
    }

    // ─── Unit CRUD ───────────────────────────────────────────────
    fun addUnit(unit: CondoUnit) = viewModelScope.launch { repository.insertUnit(unit) }
    fun updateUnit(unit: CondoUnit) = viewModelScope.launch { repository.updateUnit(unit) }
    fun deleteUnit(unit: CondoUnit) = viewModelScope.launch { repository.deleteUnit(unit) }

    // ─── Expense CRUD ────────────────────────────────────────────
    fun addExpense(expense: Expense) = viewModelScope.launch { repository.insertExpense(expense) }
    fun updateExpense(expense: Expense) = viewModelScope.launch { repository.updateExpense(expense) }
    fun deleteExpense(expense: Expense) = viewModelScope.launch { repository.deleteExpense(expense) }

    // ─── Payment CRUD ────────────────────────────────────────────
    fun addPayment(payment: Payment) = viewModelScope.launch { repository.insertPayment(payment) }
    fun updatePayment(payment: Payment) = viewModelScope.launch { repository.updatePayment(payment) }
    fun deletePayment(payment: Payment) = viewModelScope.launch { repository.deletePayment(payment) }

    // ─── Cedolino CRUD ───────────────────────────────────────────
    fun addCedolinoWithItems(cedolino: Cedolino, items: List<CedolinoItem>) =
        viewModelScope.launch { repository.insertCedolinoWithItems(cedolino, items) }

    fun updateCedolino(cedolino: Cedolino) = viewModelScope.launch { repository.updateCedolino(cedolino) }
    fun deleteCedolino(cedolino: Cedolino) = viewModelScope.launch { repository.deleteCedolino(cedolino) }

    fun markCedolinoPaid(cedolino: Cedolino) = viewModelScope.launch {
        repository.updateCedolino(cedolino.copy(status = "Pagato", paidAmount = cedolino.total, paidDate = System.currentTimeMillis()))
    }

    fun markCedolinoSent(cedolino: Cedolino) = viewModelScope.launch {
        repository.updateCedolino(cedolino.copy(sentToResident = true, sentAt = System.currentTimeMillis()))
    }

    /**
     * Segna il cedolino come Pagato E registra automaticamente un Payment
     * nel registro dei pagamenti con il metodo scelto dall'admin.
     */
    fun markCedolinoPaidWithPayment(
        cedolino: Cedolino,
        method: String,
        reference: String = ""
    ) = viewModelScope.launch {
        // 1. Aggiorna stato cedolino
        repository.updateCedolino(
            cedolino.copy(
                status = "Pagato",
                paidAmount = cedolino.total,
                paidDate = System.currentTimeMillis()
            )
        )
        // 2. Inserisce il pagamento nel registro
        repository.insertPayment(
            Payment(
                unitId = cedolino.unitId,
                amount = cedolino.total,
                date = System.currentTimeMillis(),
                method = method,
                reference = reference.ifBlank { "Cedolino ${cedolino.period}" },
                cedolinoId = cedolino.id,
                notes = "Pagamento automatico da cedolino"
            )
        )
    }

    /**
     * Crea una quota diretta per una singola unità senza passare
     * dal calcolo millesimale — utile per addebiti specifici.
     */
    fun addQuotaDirecta(
        unitId: Long,
        importo: Double,
        descrizione: String,
        categoria: String,
        periodo: String,
        dueDate: Long
    ) = viewModelScope.launch {
        repository.insertCedolinoWithItems(
            Cedolino(
                unitId = unitId,
                period = periodo,
                issueDate = System.currentTimeMillis(),
                dueDate = dueDate,
                total = importo,
                status = "Emesso",
                // Auto-inviato: l'admin crea la quota appositamente per quest'unità
                // → il condomino la vede subito nel suo tab Cedolini
                sentToResident = true,
                sentAt = System.currentTimeMillis()
            ),
            listOf(
                CedolinoItem(
                    cedolinoId = 0,
                    description = "$categoria: $descrizione",
                    amount = importo
                )
            )
        )
    }

    fun generateCedoliniForAllUnits(period: String, dueDate: Long) = viewModelScope.launch {
        val currentUnits = units.value.filter { it.millesimi > 0 }
        if (currentUnits.isEmpty()) return@launch

        // Recupera i periodi già esistenti per ogni unità — evita duplicati
        val existingByUnit: Map<Long, Set<String>> = cedolini.value
            .groupBy { it.unitId }
            .mapValues { (_, ceds) -> ceds.map { it.period }.toSet() }

        var skipped = 0
        for (unit in currentUnits) {
            // Se esiste già un cedolino per questo periodo → salta
            if (existingByUnit[unit.id]?.contains(period) == true) {
                skipped++
                continue
            }
            repository.insertCedolinoWithItems(
                Cedolino(
                    unitId    = unit.id,
                    period    = period,
                    issueDate = System.currentTimeMillis(),
                    dueDate   = dueDate,
                    total     = unit.millesimi,
                    status    = "Emesso"
                ),
                listOf(
                    CedolinoItem(
                        cedolinoId  = 0,
                        description = "Canone affitto $period",
                        amount      = unit.millesimi
                    )
                )
            )
        }
        android.util.Log.d("RentViewModel", "generateCedolini: ${currentUnits.size - skipped} creati, $skipped saltati (già esistenti)")
    }

    /**
     * Genera automaticamente i cedolini mensili per l'intera durata del contratto
     * di una singola unità. Salta i mesi che hanno già un cedolino.
     * La scadenza di ogni cedolino = [paymentDayOfMonth] del mese successivo.
     */
    fun generateMonthlyPaymentPlan(unit: CondoUnit) = viewModelScope.launch {
        val start  = unit.leaseStartDate ?: return@launch
        val end    = unit.leaseEndDate   ?: return@launch
        val canone = unit.millesimi
        if (canone <= 0) return@launch

        val mesi = listOf("Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno",
            "Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre")

        // Cursore al primo giorno del mese di inizio contratto
        val cal = Calendar.getInstance().apply {
            timeInMillis = start
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply { timeInMillis = end }
        val existingPeriods = cedolini.value.filter { it.unitId == unit.id }.map { it.period }.toSet()

        while (!cal.after(calEnd)) {
            val month  = cal.get(Calendar.MONTH)
            val year   = cal.get(Calendar.YEAR)
            val period = "${mesi[month]} $year"

            if (period !in existingPeriods) {
                // Scadenza = paymentDayOfMonth del mese successivo
                val dueCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR,         if (month == 11) year + 1 else year)
                    set(Calendar.MONTH,        if (month == 11) 0 else month + 1)
                    set(Calendar.DAY_OF_MONTH, unit.paymentDayOfMonth.coerceIn(1, 28))
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
                }
                repository.insertCedolinoWithItems(
                    Cedolino(
                        unitId    = unit.id,
                        period    = period,
                        issueDate = System.currentTimeMillis(),
                        dueDate   = dueCal.timeInMillis,
                        total     = canone,
                        status    = "Emesso"
                    ),
                    listOf(CedolinoItem(cedolinoId = 0, description = "Canone affitto $period", amount = canone))
                )
            }
            cal.add(Calendar.MONTH, 1)
        }
    }

    // ─── Documento CRUD ──────────────────────────────────────────
    fun addDocumento(
        uri: Uri,
        titolo: String,
        categoria: String,
        note: String,
        mimeType: String,
        sommario: String = "",
        visibilita: String = "Tutti",
        destinatariUnitIds: String = ""
    ) = viewModelScope.launch {
            val condId = _activeCondominioId.value.takeIf { it > 0 } ?: return@launch
            withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                val docsDir = File(context.filesDir, "documents").also { it.mkdirs() }
                val originalName = uri.lastPathSegment?.substringAfterLast('/') ?: "documento"
                val destFile = File(docsDir, "${System.currentTimeMillis()}_$originalName")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                repository.insertDocumento(Documento(
                    condominioId = condId,
                    titolo = titolo, categoria = categoria,
                    fileType = FileTypes.fromMimeType(mimeType),
                    filePath = destFile.absolutePath, fileName = originalName,
                    fileSize = destFile.length(), note = note,
                    sommario = sommario,
                    visibilita = visibilita,
                    destinatariUnitIds = destinatariUnitIds
                ))
            }
        }

    fun deleteDocumento(documento: Documento) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            File(documento.filePath).takeIf { it.exists() }?.delete()
            repository.deleteDocumento(documento)
        }
    }

    /** Aggiorna titolo, categoria, sommario, visibilita e destinatari di un documento già salvato */
    fun updateDocumento(documento: Documento) = viewModelScope.launch {
        withContext(Dispatchers.IO) { repository.updateDocumento(documento) }
    }

    /** Duplica un cedolino per il mese successivo (status Emesso, non inviato) */
    fun duplicateCedolino(cwi: CedolinoWithItems) = viewModelScope.launch {
        val nextPeriod = buildString {
            // Prova a parsare il periodo come "Mese YYYY" → incrementa
            val parts = cwi.cedolino.period.trim().split(" ")
            if (parts.size == 2) {
                val mesi = listOf("Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno",
                    "Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre")
                val idx = mesi.indexOfFirst { it.equals(parts[0], ignoreCase = true) }
                if (idx >= 0) {
                    val nextIdx = (idx + 1) % 12
                    val nextYear = if (nextIdx == 0) (parts[1].toIntOrNull() ?: 0) + 1 else parts[1].toIntOrNull() ?: 0
                    append("${mesi[nextIdx]} $nextYear")
                } else append("Copia - ${cwi.cedolino.period}")
            } else append("Copia - ${cwi.cedolino.period}")
        }
        repository.insertCedolinoWithItems(
            cwi.cedolino.copy(
                id = 0,
                period = nextPeriod,
                issueDate = System.currentTimeMillis(),
                status = "Emesso",
                sentToResident = false,
                sentAt = null,
                paidAmount = 0.0,
                paidDate = null
            ),
            cwi.items.map { it.copy(id = 0, cedolinoId = 0) }
        )
    }

    // ─── Helpers ────────────────────────────────────────────
    fun getUnitName(unitId: Long) =
        units.value.find { it.id == unitId }?.let { "Int. ${it.number} - ${it.ownerName}" } ?: "Sconosciuto"

    // ─── Storico Inquilini ──────────────────────────────────────
    val tenantHistory: StateFlow<List<TenantHistory>> = _activeCondominioId
        .filter { it > 0 }
        .flatMapLatest { repository.getAllTenantHistory() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun getTenantHistoryForUnit(unitId: Long): Flow<List<TenantHistory>> =
        repository.getTenantHistoryByUnit(unitId)

    fun changeTenant(
        unit: CondoUnit,
        exitNotes: String,
        newOwnerName: String,
        newOwnerEmail: String = "",
        newOwnerPhone: String = "",
        newLeaseStart: Long? = null,
        newLeaseEnd: Long? = null,
        newMonthlyRent: Double = unit.millesimi,
        newPaymentDay: Int = unit.paymentDayOfMonth
    ) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            repository.changeTenant(
                unit, exitNotes,
                newOwnerName, newOwnerEmail, newOwnerPhone,
                newLeaseStart, newLeaseEnd, newMonthlyRent, newPaymentDay
            )
        }
    }

    fun deleteTenantHistory(history: TenantHistory) = viewModelScope.launch {
        withContext(Dispatchers.IO) { repository.deleteTenantHistory(history) }
    }

    // ─── Export CSV ────────────────────────────────────────
    /** Esporta cedolini e spese del condominio attivo in due file CSV e apre lo share dialog */
    fun exportCSV(context: Context) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val fmt  = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ITALIAN).format(Date())
            val condName = activeCondominio.value?.nome?.replace(" ", "_") ?: "export"
            val dir = File(context.getExternalFilesDir(null), "export").also { it.mkdirs() }

            val unitMap = units.value.associateBy { it.id }

            // ── 1. Cedolini CSV ──────────────────────────────────────
            val cedFile = File(dir, "cedolini_${condName}_$stamp.csv")
            cedFile.bufferedWriter().use { w ->
                w.write("Periodo;Inquilino;Appartamento;Totale (€);Stato;Data Pagamento;Importo Pagato (€)\n")
                cedolini.value.sortedBy { it.dueDate }.forEach { c ->
                    val unit = unitMap[c.unitId]
                    val paidDate = c.paidDate?.let { fmt.format(Date(it)) } ?: ""
                    w.write("${c.period};${unit?.ownerName ?: ""};${unit?.number ?: ""};" +
                        "${"%,.2f".format(c.total)};${c.status};$paidDate;${"%,.2f".format(c.paidAmount)}\n")
                }
            }

            // ── 2. Spese CSV ─────────────────────────────────────────
            val expFile = File(dir, "spese_${condName}_$stamp.csv")
            expFile.bufferedWriter().use { w ->
                w.write("Data;Categoria;Descrizione;Importo (€);Note\n")
                expenses.value.sortedBy { it.date }.forEach { e ->
                    w.write("${fmt.format(Date(e.date))};${e.category};${e.description};" +
                        "${"%,.2f".format(e.amount)};${e.notes}\n")
                }
            }

            // ── Share entrambi i file ─────────────────────────────────
            val uris = ArrayList<Uri>()
            listOf(cedFile, expFile).forEach { f ->
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.provider", f))
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "text/csv"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "Export RentTrack — $condName ($stamp)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Esporta CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}

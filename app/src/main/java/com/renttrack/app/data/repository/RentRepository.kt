package com.renttrack.app.data.repository

import com.renttrack.app.data.dao.*
import com.renttrack.app.data.model.*
import kotlinx.coroutines.flow.Flow

class RentRepository(
    private val condominioDao: CondominioDao,
    private val unitDao: UnitDao,
    private val expenseDao: ExpenseDao,
    private val paymentDao: PaymentDao,
    private val cedolinoDao: CedolinoDao,
    private val documentoDao: DocumentoDao,
    private val tenantHistoryDao: TenantHistoryDao
) {
    // ─── Condomini ───────────────────────────────────────────────
    val allCondomini: Flow<List<Condominio>> = condominioDao.getAllCondomini()
    val condominioCount: Flow<Int> = condominioDao.getCondominioCount()

    suspend fun insertCondominio(c: Condominio) = condominioDao.insertCondominio(c)
    suspend fun updateCondominio(c: Condominio) = condominioDao.updateCondominio(c)
    suspend fun deleteCondominio(c: Condominio) = condominioDao.deleteCondominio(c)
    suspend fun getCondominioById(id: Long) = condominioDao.getCondominioById(id)

    // ─── Unità ───────────────────────────────────────────────────
    fun getUnitsByCondominio(condId: Long) = unitDao.getUnitsByCondominio(condId)
    fun getUnitCount(condId: Long) = unitDao.getUnitCount(condId)
    fun getTotalMillesimi(condId: Long) = unitDao.getTotalMillesimi(condId)
    fun getAllUnitsWithPayments(condId: Long) = unitDao.getAllUnitsWithPayments(condId)
    fun getAllUnitsGlobal() = unitDao.getAllUnitsGlobal()
    suspend fun getUnitById(id: Long) = unitDao.getUnitById(id)
    suspend fun insertUnit(unit: CondoUnit) = unitDao.insertUnit(unit)
    suspend fun updateUnit(unit: CondoUnit) = unitDao.updateUnit(unit)
    suspend fun deleteUnit(unit: CondoUnit) = unitDao.deleteUnit(unit)

    // ─── Spese ───────────────────────────────────────────────────
    fun getExpensesByCondominio(condId: Long) = expenseDao.getExpensesByCondominio(condId)
    fun getTotalExpenses(condId: Long) = expenseDao.getTotalExpenses(condId)
    fun getExpensesByGroupedCategory(condId: Long) = expenseDao.getExpensesByGroupedCategory(condId)
    fun getRecentExpenses(condId: Long, limit: Int) = expenseDao.getRecentExpenses(condId, limit)
    fun getExpensesByDateRange(condId: Long, s: Long, e: Long) = expenseDao.getExpensesByDateRange(condId, s, e)
    fun getMonthlyExpenses(condId: Long, year: Int) = expenseDao.getMonthlyExpenses(condId, year)
    fun getYearlyExpenses(condId: Long) = expenseDao.getYearlyExpenses(condId)
    fun getExpenseYears(condId: Long) = expenseDao.getExpenseYears(condId)
    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    // ─── Pagamenti ────────────────────────────────────────────────
    fun getPaymentsByCondominio(condId: Long) = paymentDao.getPaymentsByCondominio(condId)
    fun getTotalPayments(condId: Long) = paymentDao.getTotalPayments(condId)
    fun getPaymentsByUnit(unitId: Long) = paymentDao.getPaymentsByUnit(unitId)
    fun getRecentPayments(condId: Long, limit: Int) = paymentDao.getRecentPayments(condId, limit)
    fun getMonthlyPayments(condId: Long, year: Int) = paymentDao.getMonthlyPayments(condId, year)
    fun getYearlyPayments(condId: Long) = paymentDao.getYearlyPayments(condId)
    fun getPaymentYears(condId: Long) = paymentDao.getPaymentYears(condId)
    suspend fun insertPayment(payment: Payment) = paymentDao.insertPayment(payment)
    suspend fun updatePayment(payment: Payment) = paymentDao.updatePayment(payment)
    suspend fun deletePayment(payment: Payment) = paymentDao.deletePayment(payment)

    // ─── Cedolini ────────────────────────────────────────────────
    fun getAllCedolini(condId: Long) = cedolinoDao.getAllCedolini(condId)
    fun getAllCedoliniWithItems(condId: Long) = cedolinoDao.getAllCedoliniWithItems(condId)
    fun getAllCedoliniGlobal() = cedolinoDao.getAllCedoliniGlobal()
    fun getPendingCedoliniCount(condId: Long) = cedolinoDao.getPendingCedoliniCount(condId)
    fun getUnsentCedoliniCount(condId: Long) = cedolinoDao.getUnsentCedoliniCount(condId)
    fun getCedoliniByUnit(unitId: Long) = cedolinoDao.getCedoliniByUnit(unitId)
    suspend fun insertCedolinoWithItems(c: Cedolino, items: List<CedolinoItem>) = cedolinoDao.insertCedolinoWithItems(c, items)
    suspend fun updateCedolino(c: Cedolino) = cedolinoDao.updateCedolino(c)
    suspend fun deleteCedolino(c: Cedolino) = cedolinoDao.deleteCedolino(c)

    // ─── Documenti ────────────────────────────────────────────────
    fun getDocumentiByCondominio(condId: Long) = documentoDao.getDocumentiByCondominio(condId)
    fun getDocumentCount(condId: Long) = documentoDao.getDocumentCount(condId)
    fun getDocumentiByCategoria(condId: Long, categoria: String) = documentoDao.getDocumentiByCategoria(condId, categoria)
    suspend fun insertDocumento(doc: Documento) = documentoDao.insertDocumento(doc)
    suspend fun updateDocumento(doc: Documento) = documentoDao.updateDocumento(doc)
    suspend fun deleteDocumento(doc: Documento) = documentoDao.deleteDocumento(doc)

    // ─── Storico Inquilini ────────────────────────────────────────
    fun getTenantHistoryByUnit(unitId: Long) = tenantHistoryDao.getHistoryByUnit(unitId)
    fun getAllTenantHistory() = tenantHistoryDao.getAllHistory()
    suspend fun insertTenantHistory(h: TenantHistory) = tenantHistoryDao.insertHistory(h)
    suspend fun deleteTenantHistory(h: TenantHistory) = tenantHistoryDao.deleteHistory(h)

    /**
     * Archivia l'inquilino attuale dell'unità in TenantHistory
     * e aggiorna l'unità con i dati del nuovo inquilino.
     * Tutto in un'unica operazione atomica.
     */
    suspend fun changeTenant(
        unit: CondoUnit,
        exitNotes: String,
        newOwnerName: String,
        newOwnerEmail: String,
        newOwnerPhone: String,
        newLeaseStart: Long?,
        newLeaseEnd: Long?,
        newMonthlyRent: Double,
        newPaymentDay: Int = unit.paymentDayOfMonth
    ) {
        // 1. Archivia inquilino attuale (tutti i campi dell'inquilino uscente)
        tenantHistoryDao.insertHistory(
            TenantHistory(
                unitId       = unit.id,
                ownerName    = unit.ownerName,
                ownerEmail   = unit.ownerEmail,
                ownerPhone   = unit.ownerPhone,
                leaseStartDate = unit.leaseStartDate,
                leaseEndDate   = unit.leaseEndDate,
                monthlyRent  = unit.millesimi,
                exitNotes    = exitNotes
            )
        )
        // 2. Aggiorna unità con il nuovo inquilino — preserva tutte le proprietà
        //    dell'appartamento (number, floor, type, areaMq, scala, condominioId)
        //    e sovrascrive solo i campi specifici del nuovo inquilino
        unitDao.updateUnit(
            unit.copy(
                ownerName         = newOwnerName,
                ownerEmail        = newOwnerEmail,
                ownerPhone        = newOwnerPhone,
                leaseStartDate    = newLeaseStart,
                leaseEndDate      = newLeaseEnd,
                millesimi         = newMonthlyRent,
                paymentDayOfMonth = newPaymentDay
            )
        )
    }
}

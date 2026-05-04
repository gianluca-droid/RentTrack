package com.condogest.app.data.repository

import com.condogest.app.data.dao.*
import com.condogest.app.data.model.*
import kotlinx.coroutines.flow.Flow

class CondoRepository(
    private val condominioDao: CondominioDao,
    private val unitDao: UnitDao,
    private val expenseDao: ExpenseDao,
    private val paymentDao: PaymentDao,
    private val cedolinoDao: CedolinoDao,
    private val documentoDao: DocumentoDao
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
}

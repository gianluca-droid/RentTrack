package com.renttrack.app.data.dao

import androidx.room.*
import com.renttrack.app.data.model.*
import kotlinx.coroutines.flow.Flow

// ─── Condominio DAO ─────────────────────────────────────────────────
@Dao
interface CondominioDao {
    @Query("SELECT * FROM condomini ORDER BY nome ASC")
    fun getAllCondomini(): Flow<List<Condominio>>

    @Query("SELECT * FROM condomini WHERE id = :id")
    suspend fun getCondominioById(id: Long): Condominio?

    @Query("SELECT COUNT(*) FROM condomini")
    fun getCondominioCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCondominio(condominio: Condominio): Long

    @Update
    suspend fun updateCondominio(condominio: Condominio)

    @Delete
    suspend fun deleteCondominio(condominio: Condominio)
}

// ─── Unit DAO ───────────────────────────────────────────────────────
@Dao
interface UnitDao {
    @Query("SELECT * FROM units WHERE condominioId = :condominioId ORDER BY number ASC")
    fun getUnitsByCondominio(condominioId: Long): Flow<List<CondoUnit>>

    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun getUnitById(id: Long): CondoUnit?

    @Query("SELECT SUM(millesimi) FROM units WHERE condominioId = :condominioId")
    fun getTotalMillesimi(condominioId: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: CondoUnit): Long

    @Update
    suspend fun updateUnit(unit: CondoUnit)

    @Delete
    suspend fun deleteUnit(unit: CondoUnit)

    @Query("SELECT COUNT(*) FROM units WHERE condominioId = :condominioId")
    fun getUnitCount(condominioId: Long): Flow<Int>

    @Query("SELECT * FROM units ORDER BY condominioId, number ASC")
    fun getAllUnitsGlobal(): Flow<List<CondoUnit>>

    @Transaction
    @Query("SELECT * FROM units WHERE condominioId = :condominioId")
    fun getAllUnitsWithPayments(condominioId: Long): Flow<List<UnitWithPayments>>
}

// ─── Expense DAO ────────────────────────────────────────────────────
@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE condominioId = :condominioId ORDER BY date DESC")
    fun getExpensesByCondominio(condominioId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE condominioId = :condominioId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(condominioId: Long, startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE condominioId = :condominioId")
    fun getTotalExpenses(condominioId: Long): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE condominioId = :condominioId GROUP BY category ORDER BY total DESC")
    fun getExpensesByGroupedCategory(condominioId: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM expenses WHERE condominioId = :condominioId ORDER BY date DESC LIMIT :limit")
    fun getRecentExpenses(condominioId: Long, limit: Int): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
    // ── Mensile e annuale ─────────────────────────────────────────
    @Query("""
        SELECT CAST(strftime('%m', date / 1000, 'unixepoch') AS INTEGER) AS month, SUM(amount) AS total
        FROM expenses
        WHERE condominioId = :condominioId
          AND CAST(strftime('%Y', date / 1000, 'unixepoch') AS INTEGER) = :year
        GROUP BY month ORDER BY month
    """)
    fun getMonthlyExpenses(condominioId: Long, year: Int): Flow<List<MonthTotal>>

    @Query("""
        SELECT DISTINCT CAST(strftime('%Y', date / 1000, 'unixepoch') AS INTEGER) AS year
        FROM expenses WHERE condominioId = :condominioId ORDER BY year DESC
    """)
    fun getExpenseYears(condominioId: Long): Flow<List<Int>>

    @Query("""
        SELECT CAST(strftime('%Y', date / 1000, 'unixepoch') AS INTEGER) AS year,
               SUM(amount) AS total, COUNT(*) AS count
        FROM expenses WHERE condominioId = :condominioId
        GROUP BY year ORDER BY year DESC
    """)
    fun getYearlyExpenses(condominioId: Long): Flow<List<YearTotal>>
}

data class CategoryTotal(val category: String, val total: Double)
data class MonthTotal(val month: Int, val total: Double)
data class YearTotal(val year: Int, val total: Double, val count: Int)

// ─── Payment DAO ────────────────────────────────────────────────────
@Dao
interface PaymentDao {
    @Query("SELECT p.* FROM payments p JOIN units u ON p.unitId = u.id WHERE u.condominioId = :condominioId ORDER BY p.date DESC")
    fun getPaymentsByCondominio(condominioId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE unitId = :unitId ORDER BY date DESC")
    fun getPaymentsByUnit(unitId: Long): Flow<List<Payment>>

    @Query("SELECT SUM(p.amount) FROM payments p JOIN units u ON p.unitId = u.id WHERE u.condominioId = :condominioId")
    fun getTotalPayments(condominioId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE unitId = :unitId")
    fun getTotalPaymentsByUnit(unitId: Long): Flow<Double?>

    @Query("SELECT p.* FROM payments p JOIN units u ON p.unitId = u.id WHERE u.condominioId = :condominioId ORDER BY p.date DESC LIMIT :limit")
    fun getRecentPayments(condominioId: Long, limit: Int): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)

    // ── Mensile e annuale ─────────────────────────────────────────
    @Query("""
        SELECT CAST(strftime('%m', p.date / 1000, 'unixepoch') AS INTEGER) AS month, SUM(p.amount) AS total
        FROM payments p JOIN units u ON p.unitId = u.id
        WHERE u.condominioId = :condominioId
          AND CAST(strftime('%Y', p.date / 1000, 'unixepoch') AS INTEGER) = :year
        GROUP BY month ORDER BY month
    """)
    fun getMonthlyPayments(condominioId: Long, year: Int): Flow<List<MonthTotal>>

    @Query("""
        SELECT DISTINCT CAST(strftime('%Y', p.date / 1000, 'unixepoch') AS INTEGER) AS year
        FROM payments p JOIN units u ON p.unitId = u.id
        WHERE u.condominioId = :condominioId ORDER BY year DESC
    """)
    fun getPaymentYears(condominioId: Long): Flow<List<Int>>

    @Query("""
        SELECT CAST(strftime('%Y', p.date / 1000, 'unixepoch') AS INTEGER) AS year,
               SUM(p.amount) AS total, COUNT(*) AS count
        FROM payments p JOIN units u ON p.unitId = u.id
        WHERE u.condominioId = :condominioId
        GROUP BY year ORDER BY year DESC
    """)
    fun getYearlyPayments(condominioId: Long): Flow<List<YearTotal>>
}

// ─── Cedolino DAO ───────────────────────────────────────────────────
@Dao
interface CedolinoDao {
    @Transaction
    @Query("SELECT c.* FROM cedolini c JOIN units u ON c.unitId = u.id WHERE u.condominioId = :condominioId ORDER BY c.issueDate DESC")
    fun getAllCedoliniWithItems(condominioId: Long): Flow<List<CedolinoWithItems>>

    @Query("SELECT c.* FROM cedolini c JOIN units u ON c.unitId = u.id WHERE u.condominioId = :condominioId ORDER BY c.issueDate DESC")
    fun getAllCedolini(condominioId: Long): Flow<List<Cedolino>>

    @Query("SELECT * FROM cedolini WHERE id = :id")
    suspend fun getCedolinoById(id: Long): Cedolino?

    @Transaction
    @Query("SELECT * FROM cedolini WHERE id = :id")
    suspend fun getCedolinoWithItems(id: Long): CedolinoWithItems?

    @Query("SELECT * FROM cedolini WHERE unitId = :unitId ORDER BY issueDate DESC")
    fun getCedoliniByUnit(unitId: Long): Flow<List<Cedolino>>

    @Query("SELECT COUNT(*) FROM cedolini c JOIN units u ON c.unitId = u.id WHERE u.condominioId = :condominioId AND (c.status = 'Emesso' OR c.status = 'Scaduto')")
    fun getPendingCedoliniCount(condominioId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM cedolini c JOIN units u ON c.unitId = u.id WHERE u.condominioId = :condominioId AND c.sentToResident = 0")
    fun getUnsentCedoliniCount(condominioId: Long): Flow<Int>

    @Query("SELECT c.* FROM cedolini c JOIN units u ON c.unitId = u.id ORDER BY c.issueDate DESC")
    fun getAllCedoliniGlobal(): Flow<List<Cedolino>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCedolino(cedolino: Cedolino): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCedolinoItems(items: List<CedolinoItem>)

    @Update
    suspend fun updateCedolino(cedolino: Cedolino)

    @Delete
    suspend fun deleteCedolino(cedolino: Cedolino)

    @Query("DELETE FROM cedolino_items WHERE cedolinoId = :cedolinoId")
    suspend fun deleteCedolinoItems(cedolinoId: Long)

    @Transaction
    suspend fun insertCedolinoWithItems(cedolino: Cedolino, items: List<CedolinoItem>) {
        val cedolinoId = insertCedolino(cedolino)
        insertCedolinoItems(items.map { it.copy(cedolinoId = cedolinoId) })
    }
}

// ─── Documento DAO ──────────────────────────────────────────────────
@Dao
interface DocumentoDao {
    @Query("SELECT * FROM documents WHERE condominioId = :condominioId ORDER BY dataInserimento DESC")
    fun getDocumentiByCondominio(condominioId: Long): Flow<List<Documento>>

    @Query("SELECT * FROM documents WHERE condominioId = :condominioId AND categoria = :categoria ORDER BY dataInserimento DESC")
    fun getDocumentiByCategoria(condominioId: Long, categoria: String): Flow<List<Documento>>

    @Query("SELECT COUNT(*) FROM documents WHERE condominioId = :condominioId")
    fun getDocumentCount(condominioId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumento(documento: Documento): Long

    @Update
    suspend fun updateDocumento(documento: Documento)

    @Delete
    suspend fun deleteDocumento(documento: Documento)
}

// ─── TenantHistory DAO ──────────────────────────────────────────────
@Dao
interface TenantHistoryDao {
    @Query("SELECT * FROM tenant_history WHERE unitId = :unitId ORDER BY archivedAt DESC")
    fun getHistoryByUnit(unitId: Long): Flow<List<TenantHistory>>

    @Query("SELECT * FROM tenant_history ORDER BY archivedAt DESC")
    fun getAllHistory(): Flow<List<TenantHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TenantHistory): Long

    @Delete
    suspend fun deleteHistory(history: TenantHistory)

    @Query("DELETE FROM tenant_history WHERE unitId = :unitId")
    suspend fun deleteAllHistoryForUnit(unitId: Long)
}

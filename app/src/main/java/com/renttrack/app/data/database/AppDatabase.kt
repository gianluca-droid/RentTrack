package com.renttrack.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.renttrack.app.data.dao.*
import com.renttrack.app.data.model.*

@Database(
    entities = [
        Condominio::class,
        CondoUnit::class,
        Expense::class,
        Payment::class,
        Cedolino::class,
        CedolinoItem::class,
        Documento::class,
        TenantHistory::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun condominioDao(): CondominioDao
    abstract fun unitDao(): UnitDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentDao(): PaymentDao
    abstract fun cedolinoDao(): CedolinoDao
    abstract fun documentoDao(): DocumentoDao
    abstract fun tenantHistoryDao(): TenantHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // ─── Migrazione 5 → 6 ────────────────────────────────────────────────────
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN sommario TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE documents ADD COLUMN visibilita TEXT NOT NULL DEFAULT 'Tutti'")
                db.execSQL("ALTER TABLE documents ADD COLUMN destinatariUnitIds TEXT NOT NULL DEFAULT ''")
            }
        }

        // ─── Migrazione 6 → 7 ────────────────────────────────────────────────────
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE units ADD COLUMN leaseStartDate INTEGER")
                db.execSQL("ALTER TABLE units ADD COLUMN leaseEndDate INTEGER")
            }
        }

        // ─── Migrazione 7 → 8 ────────────────────────────────────────────────────
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE units ADD COLUMN paymentDayOfMonth INTEGER NOT NULL DEFAULT 5")
            }
        }

        // ─── Migrazione 8 → 9: crea tabella storico inquilini ────────────────────
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tenant_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        unitId INTEGER NOT NULL,
                        ownerName TEXT NOT NULL,
                        ownerEmail TEXT NOT NULL DEFAULT '',
                        ownerPhone TEXT NOT NULL DEFAULT '',
                        leaseStartDate INTEGER,
                        leaseEndDate INTEGER,
                        monthlyRent REAL NOT NULL DEFAULT 0.0,
                        exitNotes TEXT NOT NULL DEFAULT '',
                        archivedAt INTEGER NOT NULL,
                        FOREIGN KEY (unitId) REFERENCES units(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tenant_history_unitId ON tenant_history(unitId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "condogest_v3"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /** Chiude il DB e azzera l'istanza singleton.
         *  Va chiamato prima di operazioni sui file (backup/ripristino). */
        fun closeAndReset() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

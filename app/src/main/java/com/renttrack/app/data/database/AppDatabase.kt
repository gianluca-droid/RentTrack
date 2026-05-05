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
        Documento::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun condominioDao(): CondominioDao
    abstract fun unitDao(): UnitDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentDao(): PaymentDao
    abstract fun cedolinoDao(): CedolinoDao
    abstract fun documentoDao(): DocumentoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // ─── Migrazione 5 → 6: aggiunge sommario, visibilita, destinatariUnitIds ───
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN sommario TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE documents ADD COLUMN visibilita TEXT NOT NULL DEFAULT 'Tutti'")
                db.execSQL("ALTER TABLE documents ADD COLUMN destinatariUnitIds TEXT NOT NULL DEFAULT ''")
            }
        }

        // ─── Migrazione 6 → 7: aggiunge date contratto a units ──────────────────────
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE units ADD COLUMN leaseStartDate INTEGER")
                db.execSQL("ALTER TABLE units ADD COLUMN leaseEndDate INTEGER")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "condogest_v3"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

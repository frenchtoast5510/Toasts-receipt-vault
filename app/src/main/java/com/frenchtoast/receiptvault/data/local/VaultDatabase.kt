package com.frenchtoast.receiptvault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VaultItemEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultItemDao(): VaultItemDao

    companion object {
        @Volatile
        private var instance: VaultDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vault_items ADD COLUMN entryType TEXT NOT NULL DEFAULT 'RECEIPT'")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN documentPath TEXT")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN documentDisplayName TEXT")
                db.execSQL("ALTER TABLE vault_items ADD COLUMN documentMimeType TEXT")
            }
        }

        fun getInstance(context: Context): VaultDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "toasts_receipt_vault.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

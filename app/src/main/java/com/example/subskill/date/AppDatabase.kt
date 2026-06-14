package com.example.subskill.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AppSettings::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN isManualSubscription INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN monthlyFeeDouble REAL")
                db.execSQL("UPDATE app_settings SET monthlyFeeDouble = CAST(monthlyFee AS REAL)")
                db.execSQL("CREATE TABLE app_settings_new (packageName TEXT NOT NULL PRIMARY KEY, serviceName TEXT, monthlyFee REAL, isCandidate INTEGER NOT NULL DEFAULT 0, isManualSubscription INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("INSERT INTO app_settings_new SELECT packageName, serviceName, monthlyFeeDouble, isCandidate, isManualSubscription FROM app_settings")
                db.execSQL("DROP TABLE app_settings")
                db.execSQL("ALTER TABLE app_settings_new RENAME TO app_settings")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
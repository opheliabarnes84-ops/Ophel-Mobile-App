package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ThermalLogEntity::class, CoolingSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ThermalDatabase : RoomDatabase() {

    abstract fun thermalDao(): ThermalDao

    companion object {
        @Volatile
        private var INSTANCE: ThermalDatabase? = null

        fun getDatabase(context: Context): ThermalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ThermalDatabase::class.java,
                    "frostguard_thermal.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

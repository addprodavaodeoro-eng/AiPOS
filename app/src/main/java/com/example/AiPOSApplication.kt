package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.migration.MIGRATION_4_5
import com.example.data.local.migration.MIGRATION_5_6

class AiPOSApplication : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "aipos-db"
        )
        .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
        .fallbackToDestructiveMigration()
        .build()
    }
}

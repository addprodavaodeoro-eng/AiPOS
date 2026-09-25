package com.example.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE products ADD COLUMN sku TEXT DEFAULT NULL")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_products_sku ON products(sku)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_products_barcode ON products(barcode)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_products_name ON products(name)")
    }
}

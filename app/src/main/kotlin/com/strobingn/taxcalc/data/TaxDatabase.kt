package com.strobingn.taxcalc.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [County::class, CalculationHistory::class],
    version = 1,
    exportSchema = false
)
abstract class TaxDatabase : RoomDatabase() {
    abstract fun taxDao(): TaxDao

    companion object {
        val DEFAULT_COUNTIES = listOf(
            County(name = "New York City, NY", taxRate = 8.875, isFavorite = true),
            County(name = "Buffalo, NY", taxRate = 8.75),
            County(name = "Rochester, NY", taxRate = 8.0),
            County(name = "Yonkers, NY", taxRate = 8.875),
            County(name = "Syracuse, NY", taxRate = 8.0),
            County(name = "Los Angeles County, CA", taxRate = 9.75),
            County(name = "Cook County, IL", taxRate = 10.25),
            County(name = "Harris County, TX", taxRate = 8.25),
            County(name = "Maricopa County, AZ", taxRate = 8.6),
            County(name = "King County, WA", taxRate = 10.25)
        )
    }
}

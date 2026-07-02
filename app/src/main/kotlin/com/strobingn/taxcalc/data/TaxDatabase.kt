package com.strobingn.taxcalc.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [County::class, CalculationHistory::class],
    version = 2,
    exportSchema = false
)
abstract class TaxDatabase : RoomDatabase() {
    abstract fun taxDao(): TaxDao

    companion object {
        @Volatile
        private var INSTANCE: TaxDatabase? = null

        fun getDatabase(context: Context): TaxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaxDatabase::class.java,
                    "tax_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Expanded NY counties with decimal precision rates (combined state + county, approx as of 2025/2026)
        val DEFAULT_COUNTIES = listOf(
            County(name = "Albany", taxRate = 8.0, isFavorite = true),
            County(name = "Allegany", taxRate = 8.5),
            County(name = "Bronx", taxRate = 8.875, isFavorite = true),
            County(name = "Broome", taxRate = 8.0),
            County(name = "Cattaraugus", taxRate = 8.0),
            County(name = "Cayuga", taxRate = 8.0),
            County(name = "Chautauqua", taxRate = 8.0),
            County(name = "Chemung", taxRate = 8.0),
            County(name = "Chenango", taxRate = 8.0),
            County(name = "Clinton", taxRate = 8.0),
            County(name = "Columbia", taxRate = 8.0),
            County(name = "Cortland", taxRate = 8.0),
            County(name = "Delaware", taxRate = 8.0),
            County(name = "Dutchess", taxRate = 8.125),
            County(name = "Erie", taxRate = 8.75),
            County(name = "Essex", taxRate = 8.0),
            County(name = "Franklin", taxRate = 8.0),
            County(name = "Fulton", taxRate = 8.0),
            County(name = "Genesee", taxRate = 8.0),
            County(name = "Greene", taxRate = 8.0),
            County(name = "Hamilton", taxRate = 8.0),
            County(name = "Herkimer", taxRate = 8.25),
            County(name = "Jefferson", taxRate = 8.0),
            County(name = "Kings (Brooklyn)", taxRate = 8.875, isFavorite = true),
            County(name = "Lewis", taxRate = 8.0),
            County(name = "Livingston", taxRate = 8.0),
            County(name = "Madison", taxRate = 8.0),
            County(name = "Monroe", taxRate = 8.0),
            County(name = "Montgomery", taxRate = 8.0),
            County(name = "Nassau", taxRate = 8.625, isFavorite = true),
            County(name = "New York (Manhattan)", taxRate = 8.875, isFavorite = true),
            County(name = "Niagara", taxRate = 8.0),
            County(name = "Oneida", taxRate = 8.75),
            County(name = "Onondaga", taxRate = 8.0),
            County(name = "Ontario", taxRate = 7.5),
            County(name = "Orange", taxRate = 8.125),
            County(name = "Orleans", taxRate = 8.0),
            County(name = "Oswego", taxRate = 8.0),
            County(name = "Otsego", taxRate = 8.0),
            County(name = "Putnam", taxRate = 8.375),
            County(name = "Queens", taxRate = 8.875, isFavorite = true),
            County(name = "Rensselaer", taxRate = 8.0),
            County(name = "Richmond (Staten Island)", taxRate = 8.875),
            County(name = "Rockland", taxRate = 8.375),
            County(name = "St. Lawrence", taxRate = 8.0),
            County(name = "Saratoga", taxRate = 7.0),
            County(name = "Schenectady", taxRate = 8.0),
            County(name = "Schoharie", taxRate = 8.0),
            County(name = "Schuyler", taxRate = 8.0),
            County(name = "Seneca", taxRate = 8.0),
            County(name = "Steuben", taxRate = 8.0),
            County(name = "Suffolk", taxRate = 8.625, isFavorite = true),
            County(name = "Sullivan", taxRate = 8.0),
            County(name = "Tioga", taxRate = 8.0),
            County(name = "Tompkins", taxRate = 8.0),
            County(name = "Ulster", taxRate = 8.0),
            County(name = "Warren", taxRate = 7.0),
            County(name = "Washington", taxRate = 7.0),
            County(name = "Wayne", taxRate = 8.0),
            County(name = "Westchester", taxRate = 8.375, isFavorite = true),
            County(name = "Wyoming", taxRate = 8.0),
            County(name = "Yates", taxRate = 8.0)
        )
    }
}

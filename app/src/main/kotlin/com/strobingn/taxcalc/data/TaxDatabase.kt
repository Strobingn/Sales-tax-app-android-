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

        // Hudson Valley + surrounding focus with specific accurate combined rates (state + county + local, 2026)
        val DEFAULT_COUNTIES = listOf(
            // === LOWER HUDSON VALLEY (Core - All Favorites) ===
            // Westchester County (State: 4% + County: 4.375% = 8.375%)
            County(name = "Westchester County", taxRate = 8.375, isFavorite = true),
            County(name = "Yonkers", taxRate = 8.875, isFavorite = true),
            County(name = "Mount Vernon", taxRate = 8.875, isFavorite = true),
            County(name = "New Rochelle", taxRate = 8.875, isFavorite = true),
            County(name = "White Plains", taxRate = 8.875, isFavorite = true),
            County(name = "Greenburgh", taxRate = 8.375, isFavorite = true),
            County(name = "Mamaroneck", taxRate = 8.625, isFavorite = true),
            County(name = "Rye", taxRate = 8.375, isFavorite = true),
            County(name = "Scarsdale", taxRate = 8.375, isFavorite = true),
            County(name = "Harrison", taxRate = 8.375, isFavorite = true),
            County(name = "Pleasantville", taxRate = 8.625, isFavorite = true),
            County(name = "Valhalla", taxRate = 8.375, isFavorite = true),
            County(name = "Tuckahoe", taxRate = 8.625, isFavorite = true),
            County(name = "Bronxville", taxRate = 8.625, isFavorite = true),
            
            // Rockland County (State: 4% + County: 4.375% = 8.375%)
            County(name = "Rockland County", taxRate = 8.375, isFavorite = true),
            County(name = "Clarkstown", taxRate = 8.375, isFavorite = true),
            County(name = "Ramapo", taxRate = 8.375, isFavorite = true),
            County(name = "Orangetown", taxRate = 8.375, isFavorite = true),
            County(name = "Haverstraw", taxRate = 8.375, isFavorite = true),
            County(name = "Stony Point", taxRate = 8.375, isFavorite = true),
            County(name = "Nyack", taxRate = 8.375, isFavorite = true),
            County(name = "Spring Valley", taxRate = 8.375, isFavorite = true),
            County(name = "Nanuet", taxRate = 8.375, isFavorite = true),
            County(name = "Pearl River", taxRate = 8.375, isFavorite = true),
            
            // Putnam County (State: 4% + County: 4.375% = 8.375%)
            County(name = "Putnam County", taxRate = 8.375, isFavorite = true),
            County(name = "Carmel", taxRate = 8.375, isFavorite = true),
            County(name = "Kent", taxRate = 8.375, isFavorite = true),
            County(name = "Philipstown", taxRate = 8.375, isFavorite = true),
            County(name = "Putnam Valley", taxRate = 8.375, isFavorite = true),
            County(name = "Patterson", taxRate = 8.375, isFavorite = true),
            County(name = "Southeast", taxRate = 8.375, isFavorite = true),
            County(name = "Brewer Hill", taxRate = 8.375, isFavorite = true),
            County(name = "Cold Spring", taxRate = 8.625, isFavorite = true),
            
            // Orange County (State: 4% + County: 4.125% = 8.125%)
            County(name = "Orange County", taxRate = 8.125, isFavorite = true),
            County(name = "Newburgh", taxRate = 8.125, isFavorite = true),
            County(name = "Middletown", taxRate = 8.125, isFavorite = true),
            County(name = "Monroe", taxRate = 8.125, isFavorite = true),
            County(name = "Woodbury", taxRate = 8.125, isFavorite = true),
            County(name = "New Windsor", taxRate = 8.125, isFavorite = true),
            County(name = "Cornwall", taxRate = 8.125, isFavorite = true),
            County(name = "Wallkill", taxRate = 8.125, isFavorite = true),
            County(name = "Goshen", taxRate = 8.125, isFavorite = true),
            County(name = "Chester", taxRate = 8.125, isFavorite = true),
            County(name = "Blooming Grove", taxRate = 8.125, isFavorite = true),
            County(name = "Washingtonville", taxRate = 8.125, isFavorite = true),
            
            // === MID HUDSON VALLEY ===
            County(name = "Dutchess County", taxRate = 8.125, isFavorite = true),
            County(name = "Poughkeepsie", taxRate = 8.125, isFavorite = true),
            County(name = "Fishkill", taxRate = 8.125, isFavorite = true),
            County(name = "Beacon", taxRate = 8.125, isFavorite = true),

            // === SURROUNDING / FREQUENTLY USED ===
            County(name = "Sullivan", taxRate = 8.0),
            County(name = "Bronx", taxRate = 8.875),
            County(name = "Kings (Brooklyn)", taxRate = 8.875),
            County(name = "New York (Manhattan)", taxRate = 8.875),
            County(name = "Queens", taxRate = 8.875),
            County(name = "Richmond (Staten Island)", taxRate = 8.875),
            County(name = "Nassau", taxRate = 8.625),
            County(name = "Suffolk", taxRate = 8.625),

            // === OTHER NY COUNTIES (for completeness) ===
            County(name = "Allegany", taxRate = 8.5),
            County(name = "Broome", taxRate = 8.0),
            County(name = "Cattaraugus", taxRate = 8.0),
            County(name = "Cayuga", taxRate = 8.0),
            County(name = "Chautauqua", taxRate = 8.0),
            County(name = "Chemung", taxRate = 8.0),
            County(name = "Chenango", taxRate = 8.0),
            County(name = "Clinton", taxRate = 8.0),
            County(name = "Cortland", taxRate = 8.0),
            County(name = "Delaware", taxRate = 8.0),
            County(name = "Erie", taxRate = 8.75),
            County(name = "Essex", taxRate = 8.0),
            County(name = "Franklin", taxRate = 8.0),
            County(name = "Fulton", taxRate = 8.0),
            County(name = "Genesee", taxRate = 8.0),
            County(name = "Hamilton", taxRate = 8.0),
            County(name = "Herkimer", taxRate = 8.25),
            County(name = "Jefferson", taxRate = 8.0),
            County(name = "Lewis", taxRate = 8.0),
            County(name = "Livingston", taxRate = 8.0),
            County(name = "Madison", taxRate = 8.0),
            County(name = "Monroe", taxRate = 8.0),
            County(name = "Montgomery", taxRate = 8.0),
            County(name = "Niagara", taxRate = 8.0),
            County(name = "Oneida", taxRate = 8.75),
            County(name = "Onondaga", taxRate = 8.0),
            County(name = "Ontario", taxRate = 7.5),
            County(name = "Orleans", taxRate = 8.0),
            County(name = "Oswego", taxRate = 8.0),
            County(name = "Otsego", taxRate = 8.0),
            County(name = "St. Lawrence", taxRate = 8.0),
            County(name = "Schenectady", taxRate = 8.0),
            County(name = "Schoharie", taxRate = 8.0),
            County(name = "Schuyler", taxRate = 8.0),
            County(name = "Seneca", taxRate = 8.0),
            County(name = "Steuben", taxRate = 8.0),
            County(name = "Tioga", taxRate = 8.0),
            County(name = "Tompkins", taxRate = 8.0),
            County(name = "Warren", taxRate = 7.0),
            County(name = "Washington", taxRate = 7.0),
            County(name = "Wayne", taxRate = 8.0),
            County(name = "Wyoming", taxRate = 8.0),
            County(name = "Yates", taxRate = 8.0)
        )
    }
}

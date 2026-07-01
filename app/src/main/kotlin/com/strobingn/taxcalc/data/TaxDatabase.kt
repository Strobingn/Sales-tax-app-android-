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
        // 2026 realistic combined state + local sales tax rates for major cities in all 50 states
        val DEFAULT_COUNTIES = listOf(
            // === NORTHEAST ===
            County(name = "New York City, NY", taxRate = 8.875, isFavorite = true),
            County(name = "Buffalo, NY", taxRate = 8.75),
            County(name = "Boston, MA", taxRate = 6.25),
            County(name = "Philadelphia, PA", taxRate = 8.0),
            County(name = "Pittsburgh, PA", taxRate = 7.0),
            County(name = "Baltimore, MD", taxRate = 6.0),
            County(name = "Washington DC", taxRate = 6.0),
            County(name = "Newark, NJ", taxRate = 6.625),
            County(name = "Portland, ME", taxRate = 5.5),
            County(name = "Burlington, VT", taxRate = 7.0),
            County(name = "Providence, RI", taxRate = 7.0),
            County(name = "Hartford, CT", taxRate = 6.35),
            County(name = "Manchester, NH", taxRate = 0.0),
            County(name = "Wilmington, DE", taxRate = 0.0),

            // === SOUTHEAST ===
            County(name = "Atlanta, GA", taxRate = 8.9),
            County(name = "Savannah, GA", taxRate = 7.0),
            County(name = "Miami, FL", taxRate = 7.0),
            County(name = "Orlando, FL", taxRate = 6.5),
            County(name = "Tampa, FL", taxRate = 8.5),
            County(name = "Charlotte, NC", taxRate = 7.25),
            County(name = "Raleigh, NC", taxRate = 7.25),
            County(name = "Charleston, SC", taxRate = 9.0),
            County(name = "Nashville, TN", taxRate = 9.25),
            County(name = "Memphis, TN", taxRate = 9.75),
            County(name = "Birmingham, AL", taxRate = 10.0),
            County(name = "New Orleans, LA", taxRate = 9.45),
            County(name = "Jackson, MS", taxRate = 7.0),
            County(name = "Little Rock, AR", taxRate = 9.5),
            County(name = "Louisville, KY", taxRate = 6.0),
            County(name = "Virginia Beach, VA", taxRate = 6.0),
            County(name = "Charleston, WV", taxRate = 7.0),

            // === MIDWEST ===
            County(name = "Chicago, IL", taxRate = 10.25),
            County(name = "Indianapolis, IN", taxRate = 7.0),
            County(name = "Detroit, MI", taxRate = 6.0),
            County(name = "Columbus, OH", taxRate = 7.5),
            County(name = "Cleveland, OH", taxRate = 8.0),
            County(name = "Milwaukee, WI", taxRate = 5.5),
            County(name = "Minneapolis, MN", taxRate = 8.025),
            County(name = "Des Moines, IA", taxRate = 7.0),
            County(name = "Kansas City, MO", taxRate = 8.85),
            County(name = "St. Louis, MO", taxRate = 9.18),
            County(name = "Omaha, NE", taxRate = 7.0),
            County(name = "Wichita, KS", taxRate = 7.5),
            County(name = "Fargo, ND", taxRate = 7.5),
            County(name = "Sioux Falls, SD", taxRate = 6.5),

            // === SOUTHWEST ===
            County(name = "Houston, TX", taxRate = 8.25),
            County(name = "Dallas, TX", taxRate = 8.25),
            County(name = "San Antonio, TX", taxRate = 8.25),
            County(name = "Austin, TX", taxRate = 8.25),
            County(name = "Oklahoma City, OK", taxRate = 8.625),
            County(name = "Albuquerque, NM", taxRate = 7.875),
            County(name = "Phoenix, AZ", taxRate = 8.6),
            County(name = "Tucson, AZ", taxRate = 8.7),
            County(name = "Denver, CO", taxRate = 8.81),
            County(name = "Colorado Springs, CO", taxRate = 8.25),

            // === WEST ===
            County(name = "Los Angeles, CA", taxRate = 9.5),
            County(name = "San Francisco, CA", taxRate = 8.625),
            County(name = "San Diego, CA", taxRate = 7.75),
            County(name = "Seattle, WA", taxRate = 10.25),
            County(name = "Portland, OR", taxRate = 0.0),
            County(name = "Las Vegas, NV", taxRate = 8.375),
            County(name = "Boise, ID", taxRate = 6.0),
            County(name = "Salt Lake City, UT", taxRate = 7.75),

            // === MOUNTAIN / OTHER ===
            County(name = "Billings, MT", taxRate = 0.0),
            County(name = "Cheyenne, WY", taxRate = 6.0),
            County(name = "Honolulu, HI", taxRate = 4.5),
            County(name = "Anchorage, AK", taxRate = 0.0)
        )
    }
}

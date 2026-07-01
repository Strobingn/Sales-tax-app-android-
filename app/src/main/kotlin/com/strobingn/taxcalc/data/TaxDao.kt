package com.strobingn.taxcalc.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaxDao {
    @Query("SELECT * FROM counties ORDER BY isFavorite DESC, name ASC")
    fun getAllCounties(): Flow<List<County>>

    @Query("SELECT COUNT(*) FROM counties")
    suspend fun getCountyCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounty(county: County): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(counties: List<County>)

    @Update
    suspend fun updateCounty(county: County)

    @Delete
    suspend fun deleteCounty(county: County)

    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CalculationHistory>>

    @Query("SELECT COUNT(*) FROM calculation_history")
    fun getHistoryCount(): Flow<Int>

    @Query("SELECT SUM(taxAmount) FROM calculation_history")
    fun getTotalTaxTracked(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CalculationHistory)

    @Query("DELETE FROM calculation_history")
    suspend fun clearHistory()
}

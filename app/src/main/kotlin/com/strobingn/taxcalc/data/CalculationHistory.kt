package com.strobingn.taxcalc.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class CalculationHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val mode: String,
    val countyName: String,
    val taxRate: Double,
    val inputAmount: Double,
    val taxAmount: Double,
    val outputAmount: Double
)

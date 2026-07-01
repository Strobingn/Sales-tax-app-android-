package com.strobingn.taxcalc.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counties")
data class County(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val taxRate: Double,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

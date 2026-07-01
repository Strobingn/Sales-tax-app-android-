package com.strobingn.taxcalc

import android.app.Application
import androidx.room.Room
import com.strobingn.taxcalc.data.TaxDatabase

class TaxCalcApplication : Application() {
    companion object {
        lateinit var database: TaxDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            TaxDatabase::class.java,
            "taxcalc.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
}

package com.strobingn.taxcalc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.taxcalc.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaxCalcViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TaxCalcApplication.database.taxDao()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    private val _mode = MutableStateFlow(CalcMode.FORWARD)
    val mode = _mode.asStateFlow()

    private val _selectedCounty = MutableStateFlow<County?>(null)
    val selectedCounty = _selectedCounty.asStateFlow()

    private val _inputAmount = MutableStateFlow("")
    val inputAmount = _inputAmount.asStateFlow()

    private val _calculationResult = MutableStateFlow<CalculationResult?>(null)
    val calculationResult = _calculationResult.asStateFlow()

    val counties = dao.getAllCounties().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val history = dao.getAllHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalCalculations = dao.getHistoryCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val totalTaxTracked = dao.getTotalTaxTracked().map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    init {
        viewModelScope.launch {
            if (dao.getCountyCount() == 0) { dao.insertAll(TaxDatabase.DEFAULT_COUNTIES) }
            val first = dao.getAllCounties().first().firstOrNull()
            if (first != null) _selectedCounty.value = first
        }
    }

    fun setTab(tab: Int) { _selectedTab.value = tab }
    fun setMode(newMode: CalcMode) { _mode.value = newMode }
    fun setInputAmount(value: String) { _inputAmount.value = value.filter { it.isDigit() || it == '.' } }
    fun selectCounty(county: County) { _selectedCounty.value = county }

    fun calculateAndSave() {
        val county = _selectedCounty.value ?: return
        val amount = _inputAmount.value.toDoubleOrNull() ?: return
        if (amount <= 0) return
        val rate = county.taxRate / 100.0
        val (tax, output) = if (_mode.value == CalcMode.FORWARD) { val t = amount * rate; t to (amount + t) } else { val pre = amount / (1 + rate); (amount - pre) to pre }
        val hist = CalculationHistory(timestamp = System.currentTimeMillis(), mode = _mode.value.name, countyName = county.name, taxRate = county.taxRate, inputAmount = amount, taxAmount = tax, outputAmount = output)
        viewModelScope.launch {
            dao.insertHistory(hist)
            _calculationResult.value = CalculationResult(inputAmount = amount, taxAmount = tax, outputAmount = output, mode = _mode.value)
        }
    }

    fun clearResult() { _calculationResult.value = null; _inputAmount.value = "" }
    fun addCounty(name: String, rate: Double) { viewModelScope.launch { dao.insertCounty(County(name = name, taxRate = rate)) } }
    fun editCounty(county: County) { viewModelScope.launch { dao.updateCounty(county) } }
    fun deleteCounty(county: County) { viewModelScope.launch { dao.deleteCounty(county) } }
    fun toggleFavorite(county: County) { viewModelScope.launch { dao.updateCounty(county.copy(isFavorite = !county.isFavorite)) } }

    fun loadFromHistory(item: CalculationHistory) {
        _mode.value = if (item.mode == "FORWARD") CalcMode.FORWARD else CalcMode.REVERSE
        _inputAmount.value = item.inputAmount.toString()
        val existing = counties.value.find { it.name == item.countyName }
        _selectedCounty.value = existing ?: County(id = -1L, name = item.countyName, taxRate = item.taxRate)
        _calculationResult.value = CalculationResult(inputAmount = item.inputAmount, taxAmount = item.taxAmount, outputAmount = item.outputAmount, mode = _mode.value)
        _selectedTab.value = 0
    }
}

data class CalculationResult(val inputAmount: Double, val taxAmount: Double, val outputAmount: Double, val mode: CalcMode)

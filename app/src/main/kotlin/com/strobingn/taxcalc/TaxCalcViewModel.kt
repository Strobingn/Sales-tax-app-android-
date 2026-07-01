package com.strobingn.taxcalc

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.taxcalc.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppScreen { CALC, COUNTIES, HISTORY, COMPARE, SETTINGS }

class TaxCalcViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TaxCalcApplication.database.taxDao()
    private val dataStore = application.dataStore

    private val _selectedScreen = MutableStateFlow(AppScreen.CALC)
    val selectedScreen = _selectedScreen.asStateFlow()

    private val _mode = MutableStateFlow(CalcMode.FORWARD)
    val mode = _mode.asStateFlow()

    private val _selectedCounty = MutableStateFlow<County?>(null)
    val selectedCounty = _selectedCounty.asStateFlow()

    private val _inputAmount = MutableStateFlow("")
    val inputAmount = _inputAmount.asStateFlow()

    private val _calculationResult = MutableStateFlow<CalculationResult?>(null)
    val calculationResult = _calculationResult.asStateFlow()

    private val _useCustomKeypad = MutableStateFlow(true)
    val useCustomKeypad = _useCustomKeypad.asStateFlow()

    val counties = dao.getAllCounties().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val history = dao.getAllHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalCalculations = dao.getHistoryCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val totalTaxTracked = dao.getTotalTaxTracked().map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    private val keypadKey = booleanPreferencesKey("custom_keypad")

    init {
        viewModelScope.launch {
            dataStore.data.map { it[keypadKey] ?: true }.collect { _useCustomKeypad.value = it }
        }
        viewModelScope.launch {
            if (dao.getCountyCount() == 0) { dao.insertAll(TaxDatabase.DEFAULT_COUNTIES) }
            val all = dao.getAllCounties().first()
            val fav = all.filter { it.isFavorite }.maxByOrNull { it.createdAt }
            _selectedCounty.value = fav ?: all.firstOrNull()
        }
    }

    // Navigation
    fun setScreen(screen: AppScreen) { _selectedScreen.value = screen }

    // Mode
    fun setMode(newMode: CalcMode) {
        _mode.value = newMode
        _calculationResult.value = null
    }

    // Settings
    fun setUseCustomKeypad(enabled: Boolean) {
        _useCustomKeypad.value = enabled
        viewModelScope.launch { dataStore.edit { it[keypadKey] = enabled } }
    }

    // Input
    fun setInputAmount(value: String) { _inputAmount.value = value.filter { it.isDigit() || it == '.' } }

    fun appendInput(key: String) {
        val current = _inputAmount.value
        if (key == "." && current.contains(".")) return
        if (current.contains(".") && current.substringAfter(".").length >= 2) return
        if (current == "0" && key != ".") { _inputAmount.value = key; return }
        _inputAmount.value = current + key
    }

    fun backspaceInput() {
        val current = _inputAmount.value
        if (current.isNotEmpty()) _inputAmount.value = current.dropLast(1)
    }

    fun clearInput() { _inputAmount.value = "" }

    // County
    fun selectCounty(county: County) { _selectedCounty.value = county }

    // Calculation
    fun calculateAndSave() {
        val county = _selectedCounty.value ?: return
        val amount = _inputAmount.value.toDoubleOrNull() ?: return
        if (amount <= 0) return
        val rate = county.taxRate / 100.0
        val (tax, output) = if (_mode.value == CalcMode.FORWARD) {
            val t = amount * rate; t to (amount + t)
        } else {
            val pre = amount / (1 + rate); (amount - pre) to pre
        }
        val hist = CalculationHistory(
            timestamp = System.currentTimeMillis(),
            mode = _mode.value.name,
            countyName = county.name,
            taxRate = county.taxRate,
            inputAmount = amount,
            taxAmount = tax,
            outputAmount = output
        )
        viewModelScope.launch {
            dao.insertHistory(hist)
            _calculationResult.value = CalculationResult(inputAmount = amount, taxAmount = tax, outputAmount = output, mode = _mode.value)
        }
    }

    fun clearResult() { _calculationResult.value = null; _inputAmount.value = "" }

    // County CRUD
    fun addCounty(name: String, rate: Double) {
        viewModelScope.launch { dao.insertCounty(County(name = name, taxRate = rate)) }
    }
    fun editCounty(county: County) {
        viewModelScope.launch { dao.updateCounty(county) }
    }
    fun deleteCounty(county: County) {
        viewModelScope.launch { dao.deleteCounty(county) }
    }
    fun toggleFavorite(county: County) {
        viewModelScope.launch { dao.updateCounty(county.copy(isFavorite = !county.isFavorite)) }
    }

    // History
    fun loadFromHistory(item: CalculationHistory) {
        _mode.value = if (item.mode == "FORWARD") CalcMode.FORWARD else CalcMode.REVERSE
        _inputAmount.value = item.inputAmount.toString()
        val existing = counties.value.find { it.name == item.countyName }
        _selectedCounty.value = existing ?: County(id = -1L, name = item.countyName, taxRate = item.taxRate)
        _calculationResult.value = CalculationResult(inputAmount = item.inputAmount, taxAmount = item.taxAmount, outputAmount = item.outputAmount, mode = _mode.value)
        _selectedScreen.value = AppScreen.CALC
    }
    fun deleteHistoryItem(item: CalculationHistory) {
        viewModelScope.launch { dao.deleteHistoryItem(item) }
    }
    fun clearHistory() {
        viewModelScope.launch { dao.clearHistory() }
    }
}

data class CalculationResult(val inputAmount: Double, val taxAmount: Double, val outputAmount: Double, val mode: CalcMode)

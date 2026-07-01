package com.strobingn.taxcalc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strobingn.taxcalc.data.CalculationHistory
import com.strobingn.taxcalc.data.County
import com.strobingn.taxcalc.ui.theme.TaxCalcTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TaxCalcTheme { TaxCalcApp() } }
    }
}

enum class CalcMode { FORWARD, REVERSE }

// ================================================================================
// ROOT APP
// ================================================================================
@Composable
fun TaxCalcApp() {
    val viewModel: TaxCalcViewModel = viewModel()
    val selectedScreen by viewModel.selectedScreen.collectAsState()
    val counties by viewModel.counties.collectAsState(initial = emptyList())
    val history by viewModel.history.collectAsState(initial = emptyList())
    val totalCalcs by viewModel.totalCalculations.collectAsState(initial = 0)
    val totalTax by viewModel.totalTaxTracked.collectAsState(initial = 0.0)
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppScreen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = selectedScreen == screen,
                        onClick = { viewModel.setScreen(screen) },
                        icon = {
                            Icon(
                                when (screen) {
                                    AppScreen.CALC -> Icons.Default.Calculate
                                    AppScreen.COUNTIES -> Icons.Default.LocationCity
                                    AppScreen.HISTORY -> Icons.Default.History
                                    AppScreen.COMPARE -> Icons.Default.CompareArrows
                                    AppScreen.SETTINGS -> Icons.Default.Settings
                                }, null
                            )
                        },
                        label = {
                            Text(when (screen) {
                                AppScreen.CALC -> "Calc"
                                AppScreen.COUNTIES -> "Counties"
                                AppScreen.HISTORY -> "History"
                                AppScreen.COMPARE -> "Compare"
                                AppScreen.SETTINGS -> "Settings"
                            })
                        }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedScreen) {
            AppScreen.CALC -> CalculatorScreen(viewModel, counties, padding, snackbarHostState)
            AppScreen.COUNTIES -> CountiesScreen(viewModel, counties, padding)
            AppScreen.HISTORY -> HistoryScreen(viewModel, history, totalCalcs, totalTax, padding, snackbarHostState)
            AppScreen.COMPARE -> CompareScreen(counties, padding)
            AppScreen.SETTINGS -> SettingsScreen(viewModel, padding)
        }
    }
}

// ================================================================================
// CALCULATOR SCREEN
// ================================================================================
@Composable
fun CalculatorScreen(
    viewModel: TaxCalcViewModel,
    counties: List<County>,
    padding: PaddingValues,
    snackbarHostState: SnackbarHostState
) {
    val mode by viewModel.mode.collectAsState()
    val selectedCounty by viewModel.selectedCounty.collectAsState()
    val input by viewModel.inputAmount.collectAsState()
    val result by viewModel.calculationResult.collectAsState()
    val useCustomKeypad by viewModel.useCustomKeypad.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var showCountySheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode selector
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(
                selected = mode == CalcMode.FORWARD,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.setMode(CalcMode.FORWARD) },
                label = "Forward (Add Tax)",
                modifier = Modifier.weight(1f)
            )
            ModeChip(
                selected = mode == CalcMode.REVERSE,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.setMode(CalcMode.REVERSE) },
                label = "Reverse (Find Pre-Tax)",
                modifier = Modifier.weight(1f)
            )
        }

        // County selector
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().clickable { showCountySheet = true },
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationCity, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("County / Jurisdiction", style = MaterialTheme.typography.labelSmall)
                    Text(
                        selectedCounty?.name ?: "Select county",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    selectedCounty?.let { "${it.taxRate}%" } ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }

        // Amount input
        if (!useCustomKeypad) {
            OutlinedTextField(
                value = input,
                onValueChange = { viewModel.setInputAmount(it) },
                label = { Text(if (mode == CalcMode.FORWARD) "Subtotal Amount ($)" else "Total Amount Paid ($)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                shape = RoundedCornerShape(16.dp),
                prefix = { Text("$", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$$${if (input.isEmpty()) "0.00" else input}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (input.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick amounts
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 25, 50, 100, 500).forEach { amt ->
                val scale = remember { Animatable(1f) }
                AssistChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            scale.animateTo(0.9f, tween(50))
                            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        viewModel.setInputAmount(amt.toString())
                    },
                    label = { Text("$$amt") },
                    modifier = Modifier.scale(scale.value)
                )
            }
        }

        // Calculate button
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                when {
                    selectedCounty == null -> scope.launch { snackbarHostState.showSnackbar("Please select a county first") }
                    input.isEmpty() || input.toDoubleOrNull() == null || input.toDoubleOrNull()!! <= 0 ->
                        scope.launch { snackbarHostState.showSnackbar("Enter a valid amount greater than 0") }
                    else -> viewModel.calculateAndSave()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Calculate, null)
            Spacer(Modifier.width(8.dp))
            Text("CALCULATE & SAVE", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        // Results card
        AnimatedVisibility(
            visible = result != null,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            result?.let { res ->
                val taxFraction = if (mode == CalcMode.FORWARD)
                    (res.taxAmount / res.outputAmount).coerceIn(0.0, 1.0)
                else
                    (res.taxAmount / res.inputAmount).coerceIn(0.0, 1.0)
                val preFraction = 1.0 - taxFraction

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (mode == CalcMode.FORWARD) "BREAKDOWN" else "REVERSE BREAKDOWN",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Visual proportion bar
                        Row(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(10.dp))) {
                            val preW = preFraction.toFloat().coerceAtLeast(0.01f)
                            val taxW = taxFraction.toFloat().coerceAtLeast(0.01f)
                            Box(Modifier.weight(preW).fillMaxHeight().background(MaterialTheme.colorScheme.primary)) {
                                if (preW > 0.12f) Text("${(preFraction * 100).toInt()}%", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Box(Modifier.weight(taxW).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary)) {
                                if (taxW > 0.12f) Text("${(taxFraction * 100).toInt()}%", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        val preTaxVal = if (mode == CalcMode.FORWARD) res.inputAmount else res.outputAmount
                        CopyableRow("Pre-Tax / Subtotal", formatCurrency(preTaxVal), context, snackbarHostState)
                        CopyableRow("Tax (${selectedCounty?.taxRate ?: 0}%)", formatCurrency(res.taxAmount), context, snackbarHostState)

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                copyToClipboard(context, formatCurrency(res.outputAmount))
                                scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                            },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (mode == CalcMode.FORWARD) "TOTAL DUE" else "PRE-TAX AMOUNT", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(formatCurrency(res.outputAmount), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }

        // Action buttons
        AnimatedVisibility(visible = result != null, enter = fadeIn(), exit = fadeOut()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.clearResult() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Clear") }
                Button(
                    onClick = { result?.let { shareResult(context, it, mode, selectedCounty) } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            }
        }

        // Custom keypad
        AnimatedVisibility(
            visible = useCustomKeypad,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut()
        ) {
            CustomKeyPad(viewModel)
        }
    }

    if (showCountySheet) {
        CountyPickerSheet(
            counties = counties,
            onSelect = { viewModel.selectCounty(it); showCountySheet = false },
            onDismiss = { showCountySheet = false }
        )
    }
}

// ================================================================================
// MODE CHIP
// ================================================================================
@Composable
fun ModeChip(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        leadingIcon = if (selected) {
            { Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp)) }
        } else null
    )
}

// ================================================================================
// CUSTOM KEYPAD
// ================================================================================
@Composable
fun CustomKeyPad(viewModel: TaxCalcViewModel) {
    val haptic = LocalHapticFeedback.current
    val keys = listOf(
        listOf("7", "8", "9", "DEL"),
        listOf("4", "5", "6", "C"),
        listOf("1", "2", "3", "."),
        listOf("0", "00", "000", "")
    )

    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        keys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { key ->
                    when {
                        key.isEmpty() -> Spacer(Modifier.weight(1f))
                        key == "DEL" -> KeypadButton(
                            icon = { Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.backspaceInput() },
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                        key == "C" -> KeypadButton(
                            text = "C",
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.clearInput() },
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        else -> KeypadButton(
                            text = key,
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.appendInput(key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Surface(
        onClick = {
            scope.launch {
                scale.animateTo(0.88f, tween(40))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            onClick()
        },
        modifier = modifier.height(52.dp).scale(scale.value),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            text?.let { Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            icon?.invoke()
        }
    }
}

// ================================================================================
// COUNTY PICKER SHEET
// ================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountyPickerSheet(counties: List<County>, onSelect: (County) -> Unit, onDismiss: () -> Unit) {
    var search by remember { mutableStateOf("") }
    val filtered = counties.filter { it.name.contains(search, ignoreCase = true) }.sortedByDescending { it.isFavorite }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("Select County", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 12.dp))
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                label = { Text("Search counties") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No counties found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { county ->
                        ListItem(
                            headlineContent = { Text(county.name, fontWeight = if (county.isFavorite) FontWeight.Bold else FontWeight.Normal) },
                            supportingContent = { Text("${county.taxRate}% tax rate") },
                            leadingContent = {
                                if (county.isFavorite) Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                                else Icon(Icons.Default.LocationCity, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = { Text("${county.taxRate}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSelect(county) }
                        )
                    }
                }
            }
        }
    }
}

// ================================================================================
// COUNTIES SCREEN
// ================================================================================
@Composable
fun CountiesScreen(viewModel: TaxCalcViewModel, counties: List<County>, padding: PaddingValues) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCounty by remember { mutableStateOf<County?>(null) }
    var search by remember { mutableStateOf("") }
    val filtered = counties.filter { it.name.contains(search, ignoreCase = true) }
    val haptic = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Column {
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                label = { Text("Search counties") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text(if (search.isEmpty()) "No counties yet. Add your first!" else "No counties match \"$search\"", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { county ->
                        CountyCard(
                            county = county,
                            onEdit = { editingCounty = it; showAddDialog = true },
                            onDelete = { viewModel.deleteCounty(it) },
                            onToggleFavorite = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.toggleFavorite(county) }
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editingCounty = null; showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd),
            shape = CircleShape
        ) { Icon(Icons.Default.Add, "Add County") }
    }

    if (showAddDialog) {
        AddEditCountyDialog(
            countyToEdit = editingCounty,
            onDismiss = { showAddDialog = false; editingCounty = null },
            onSave = { name, rate ->
                if (editingCounty != null) viewModel.editCounty(editingCounty!!.copy(name = name, taxRate = rate))
                else viewModel.addCounty(name, rate)
                showAddDialog = false; editingCounty = null
            }
        )
    }
}

@Composable
fun CountyCard(county: County, onEdit: (County) -> Unit, onDelete: (County) -> Unit, onToggleFavorite: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleFavorite) {
                Icon(if (county.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null, tint = if (county.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text(county.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${county.taxRate}% tax rate", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { onEdit(county) }) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete County?") },
            text = { Text("Remove \"${county.name}\" from your list? Past history entries will be preserved.") },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            confirmButton = { TextButton(onClick = { onDelete(county); showDeleteConfirm = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AddEditCountyDialog(countyToEdit: County?, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var name by remember { mutableStateOf(countyToEdit?.name ?: "") }
    var rateStr by remember { mutableStateOf(countyToEdit?.taxRate?.toString() ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var rateError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (countyToEdit != null) "Edit County" else "Add New County") },
        icon = { Icon(if (countyToEdit != null) Icons.Default.EditLocation else Icons.Default.AddLocation, null) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; nameError = false },
                    label = { Text("County Name") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("Name required", color = MaterialTheme.colorScheme.error) },
                    singleLine = true, shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = rateStr, onValueChange = { rateStr = it; rateError = false },
                    label = { Text("Tax Rate %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = rateError,
                    supportingText = { if (rateError) Text("Valid rate > 0 required", color = MaterialTheme.colorScheme.error) },
                    singleLine = true, shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val rate = rateStr.toDoubleOrNull()
                nameError = name.isBlank(); rateError = rate == null || rate <= 0
                if (!nameError && !rateError) onSave(name, rate!!)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ================================================================================
// HISTORY SCREEN
// ================================================================================
@Composable
fun HistoryScreen(
    viewModel: TaxCalcViewModel,
    history: List<CalculationHistory>,
    totalCalcs: Int,
    totalTax: Double,
    padding: PaddingValues,
    snackbarHostState: SnackbarHostState
) {
    var search by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    val filtered = history.filter { it.countyName.contains(search, ignoreCase = true) || it.mode.contains(search, ignoreCase = true) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All History?") },
            text = { Text("Permanently delete all $totalCalcs calculation records? This cannot be undone.") },
            icon = { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHistory(); showClearConfirm = false; scope.launch { snackbarHostState.showSnackbar("History cleared") } }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("RUNNING TOTALS", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Text("$totalCalcs calculations tracked", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Total tax calculated: ${formatCurrency(totalTax)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Search + clear
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                label = { Text("Search history") },
                modifier = Modifier.weight(1f), singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            if (history.isNotEmpty()) {
                IconButton(onClick = { showClearConfirm = true }) {
                    Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (search.isEmpty()) Icons.Default.History else Icons.Default.SearchOff, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))
                    Text(if (search.isEmpty()) "No calculations yet. Go crunch some numbers!" else "No results for \"$search\"", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.deleteHistoryItem(item)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.Settled)
                                MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.error
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(color).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onError)
                            }
                        }
                    ) {
                        HistoryCard(item = item, onLoad = { viewModel.loadFromHistory(item) })
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: CalculationHistory, onLoad: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onLoad() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    java.text.SimpleDateFormat("MMM dd, yyyy \u2022 HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall
                )
                Badge(containerColor = if (item.mode == "FORWARD") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(if (item.mode == "FORWARD") "FWD" else "REV", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(item.countyName, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${item.taxRate}% rate used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                if (item.mode == "FORWARD")
                    "Subtotal ${formatCurrency(item.inputAmount)} \u2192 Tax ${formatCurrency(item.taxAmount)} \u2192 Total ${formatCurrency(item.outputAmount)}"
                else
                    "Total ${formatCurrency(item.inputAmount)} \u2192 Tax ${formatCurrency(item.taxAmount)} \u2192 Pre-Tax ${formatCurrency(item.outputAmount)}",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ================================================================================
// COMPARE SCREEN
// ================================================================================
@Composable
fun CompareScreen(counties: List<County>, padding: PaddingValues) {
    var amountStr by remember { mutableStateOf("") }
    val amount = amountStr.toDoubleOrNull() ?: 0.0

    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("Tax Comparison", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("See what the same purchase costs across all counties", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = amountStr, onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Purchase Amount ($)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true, prefix = { Text("$") }, shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(12.dp))

        if (amount <= 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CompareArrows, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))
                    Text("Enter an amount above to compare tax across all counties", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else {
            val sorted = counties.sortedBy { it.taxRate }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sorted, key = { it.id }) { county ->
                    val tax = amount * (county.taxRate / 100.0)
                    val total = amount + tax
                    val isLowest = county.taxRate == sorted.first().taxRate
                    val isHighest = county.taxRate == sorted.last().taxRate

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isLowest -> MaterialTheme.colorScheme.primaryContainer
                                isHighest -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceContainer
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(county.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${county.taxRate}% rate", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isLowest) Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("BEST", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                    if (isHighest) { if (isLowest) Spacer(Modifier.width(4.dp)); Badge(containerColor = MaterialTheme.colorScheme.error) { Text("HIGHEST", fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
                                }
                                Text("+${formatCurrency(tax)} tax", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(total), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================================================================================
// SETTINGS SCREEN
// ================================================================================
@Composable
fun SettingsScreen(viewModel: TaxCalcViewModel, padding: PaddingValues) {
    val useCustomKeypad by viewModel.useCustomKeypad.collectAsState()
    val haptic = LocalHapticFeedback.current

    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        SettingItem(Icons.Default.Dialpad, "Custom Calculator Keypad", "Use built-in number pad instead of system keyboard") {
            Switch(checked = useCustomKeypad, onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setUseCustomKeypad(it)
            })
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SettingItem(Icons.Default.Info, "TaxCalc Pro", "Version 1.0 \u2022 Material You \u2022 60+ US jurisdictions") {}
    }
}

@Composable
fun SettingItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

// ================================================================================
// UTILITIES
// ================================================================================
@Composable
fun CopyableRow(label: String, value: String, context: Context, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            copyToClipboard(context, value)
            scope.launch { snackbarHostState.showSnackbar("Copied $value") }
        }.padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("TaxCalc", text))
}

fun shareResult(context: Context, result: CalculationResult, mode: CalcMode, county: County?) {
    val text = buildString {
        appendLine("TaxCalc Pro Calculation")
        appendLine("Mode: ${if (mode == CalcMode.FORWARD) "Forward (Add Tax)" else "Reverse (Find Pre-Tax)"}")
        appendLine("County: ${county?.name ?: "Unknown"} (${county?.taxRate ?: 0}%)")
        appendLine()
        if (mode == CalcMode.FORWARD) {
            appendLine("Subtotal: ${formatCurrency(result.inputAmount)}")
            appendLine("Tax: ${formatCurrency(result.taxAmount)}")
            appendLine("Total: ${formatCurrency(result.outputAmount)}")
        } else {
            appendLine("Total Paid: ${formatCurrency(result.inputAmount)}")
            appendLine("Tax: ${formatCurrency(result.taxAmount)}")
            appendLine("Pre-Tax Amount: ${formatCurrency(result.outputAmount)}")
        }
    }
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "TaxCalc Pro Calculation")
        putExtra(Intent.EXTRA_TEXT, text)
    }, "Share Calculation"))
}

fun formatCurrency(amount: Double): String = NumberFormat.getCurrencyInstance(Locale.US).format(amount)

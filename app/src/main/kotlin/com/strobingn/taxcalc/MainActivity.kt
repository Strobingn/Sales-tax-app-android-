package com.strobingn.taxcalc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strobingn.taxcalc.data.County
import com.strobingn.taxcalc.ui.theme.TaxCalcTheme
import java.text.NumberFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaxCalcTheme {
                TaxCalcApp()
            }
        }
    }
}

enum class CalcMode { FORWARD, REVERSE }

@Composable
fun TaxCalcApp() {
    val viewModel: TaxCalcViewModel = viewModel()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val counties by viewModel.counties.collectAsState(initial = emptyList())
    val history by viewModel.history.collectAsState(initial = emptyList())
    val totalCalcs by viewModel.totalCalculations.collectAsState(initial = 0)
    val totalTax by viewModel.totalTaxTracked.collectAsState(initial = 0.0)

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { viewModel.setTab(0) }, icon = { Icon(Icons.Default.Calculate, null) }, label = { Text("Calc") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { viewModel.setTab(1) }, icon = { Icon(Icons.Default.LocationCity, null) }, label = { Text("Counties") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { viewModel.setTab(2) }, icon = { Icon(Icons.Default.History, null) }, label = { Text("History") })
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> CalculatorScreen(viewModel, counties, padding)
            1 -> CountiesScreen(viewModel, counties, padding)
            2 -> HistoryScreen(viewModel, history, totalCalcs, totalTax, padding)
        }
    }
}

@Composable
fun CalculatorScreen(viewModel: TaxCalcViewModel, counties: List<County>, padding: PaddingValues) {
    val mode by viewModel.mode.collectAsState()
    val selectedCounty by viewModel.selectedCounty.collectAsState()
    val input by viewModel.inputAmount.collectAsState()
    val result by viewModel.calculationResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showCountySheet by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == CalcMode.FORWARD, onClick = { viewModel.setMode(CalcMode.FORWARD) }, label = { Text("Forward (Add Tax)") }, modifier = Modifier.weight(1f))
                FilterChip(selected = mode == CalcMode.REVERSE, onClick = { viewModel.setMode(CalcMode.REVERSE) }, label = { Text("Reverse (Find Pre-Tax)") }, modifier = Modifier.weight(1f))
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { showCountySheet = true }) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationCity, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("County / Jurisdiction", style = MaterialTheme.typography.labelSmall)
                        Text(selectedCounty?.name ?: "Select county", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(selectedCounty?.let { "${it.taxRate}%" } ?: "", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }

            OutlinedTextField(value = input, onValueChange = { viewModel.setInputAmount(it) }, label = { Text(if (mode == CalcMode.FORWARD) "Subtotal Amount ($)" else "Total Amount Paid ($)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, textStyle = MaterialTheme.typography.headlineMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 25, 50, 100, 500).forEach { amt ->
                    AssistChip(onClick = { viewModel.setInputAmount(amt.toString()) }, label = { Text("$$amt") })
                }
            }

            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.calculateAndSave() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Calculate, null)
                Spacer(Modifier.width(8.dp))
                Text("CALCULATE & SAVE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = result != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                result?.let { res ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (mode == CalcMode.FORWARD) "BREAKDOWN" else "REVERSE BREAKDOWN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            val taxFraction = if (mode == CalcMode.FORWARD) (res.taxAmount / res.outputAmount).coerceIn(0.0, 1.0) else (res.taxAmount / res.inputAmount).coerceIn(0.0, 1.0)
                            val preFraction = 1.0 - taxFraction
                            Row(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp))) {
                                Box(Modifier.weight(preFraction.toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                                Box(Modifier.weight(taxFraction.toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pre-Tax / Subtotal", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(formatCurrency(if (mode == CalcMode.FORWARD) res.inputAmount else res.outputAmount), modifier = Modifier.clickable { clipboard.setText(AnnotatedString(formatCurrency(if (mode == CalcMode.FORWARD) res.inputAmount else res.outputAmount))) })
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tax (${selectedCounty?.taxRate ?: 0}%)", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(formatCurrency(res.taxAmount), modifier = Modifier.clickable { clipboard.setText(AnnotatedString(formatCurrency(res.taxAmount))) })
                            }
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (mode == CalcMode.FORWARD) "TOTAL DUE" else "PRE-TAX AMOUNT", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(formatCurrency(if (mode == CalcMode.FORWARD) res.outputAmount else res.outputAmount), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { clipboard.setText(AnnotatedString(formatCurrency(if (mode == CalcMode.FORWARD) res.outputAmount else res.outputAmount))) })
                            }
                        }
                    }
                }
            }

            if (result != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.clearResult() }, modifier = Modifier.weight(1f)) { Text("Clear") }
                    Button(onClick = {
                        val shareText = buildString {
                            appendLine("TaxCalc Pro Calculation")
                            appendLine("County: ${selectedCounty?.name} (${selectedCounty?.taxRate}%)")
                            if (mode == CalcMode.FORWARD) {
                                appendLine("Subtotal: ${formatCurrency(result!!.inputAmount)}")
                                appendLine("Tax: ${formatCurrency(result!!.taxAmount)}")
                                appendLine("Total: ${formatCurrency(result!!.outputAmount)}")
                            } else {
                                appendLine("Total Paid: ${formatCurrency(result!!.inputAmount)}")
                                appendLine("Tax: ${formatCurrency(result!!.taxAmount)}")
                                appendLine("Pre-Tax: ${formatCurrency(result!!.outputAmount)}")
                            }
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share calculation"))
                    }, modifier = Modifier.weight(1f)) { Text("Share") }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }

    if (showCountySheet) {
        ModalBottomSheet(onDismissRequest = { showCountySheet = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("Select County", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 12.dp))
                LazyColumn {
                    items(counties.sortedByDescending { it.isFavorite }) { county ->
                        ListItem(headlineContent = { Text(county.name) }, supportingContent = { Text("${county.taxRate}%") }, trailingContent = { if (county.isFavorite) Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary) }, modifier = Modifier.clickable { viewModel.selectCounty(county); showCountySheet = false })
                    }
                }
            }
        }
    }
}

@Composable
fun CountiesScreen(viewModel: TaxCalcViewModel, counties: List<County>, padding: PaddingValues) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCounty by remember { mutableStateOf<County?>(null) }
    var search by remember { mutableStateOf("") }
    val filtered = counties.filter { it.name.contains(search, ignoreCase = true) }
    val snackbarHostState = remember { SnackbarHostState() }

    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search counties") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) })
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered) { county -> CountyCard(county, onEdit = { editingCounty = it; showAddDialog = true }, onDelete = { viewModel.deleteCounty(it) }, onToggleFavorite = { viewModel.toggleFavorite(it) }) } }
        }

        FloatingActionButton(onClick = { editingCounty = null; showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "Add County") }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showAddDialog) {
        AddEditCountyDialog(countyToEdit = editingCounty, onDismiss = { showAddDialog = false; editingCounty = null }, onSave = { name, rate ->
            if (editingCounty != null) {
                viewModel.editCounty(editingCounty!!.copy(name = name, taxRate = rate))
            } else {
                viewModel.addCounty(name, rate)
            }
            showAddDialog = false
            editingCounty = null
        })
    }
}

@Composable
fun CountyCard(county: County, onEdit: (County) -> Unit, onDelete: (County) -> Unit, onToggleFavorite: (County) -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onToggleFavorite(county) }) { Icon(if (county.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null, tint = if (county.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            Column(Modifier.weight(1f)) { Text(county.name, fontWeight = FontWeight.Bold); Text("${county.taxRate}%", color = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { onEdit(county) }) { Icon(Icons.Default.Edit, null) }
            IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, null) }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = false }, title = { Text("Delete County?") }, text = { Text("This will remove ${county.name} from your list.") }, confirmButton = { Button(onClick = { onDelete(county); showDeleteConfirm = false }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } })
    }
}

@Composable
fun AddEditCountyDialog(countyToEdit: County?, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var name by remember { mutableStateOf(countyToEdit?.name ?: "") }
    var rateStr by remember { mutableStateOf(countyToEdit?.taxRate?.toString() ?: "") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (countyToEdit != null) "Edit County" else "Add New County") }, text = { Column { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("County Name") }); Spacer(Modifier.height(8.dp)); OutlinedTextField(value = rateStr, onValueChange = { rateStr = it }, label = { Text("Tax Rate %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) } }, confirmButton = { Button(onClick = { val rate = rateStr.toDoubleOrNull() ?: 0.0; if (name.isNotBlank() && rate > 0) onSave(name, rate) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun HistoryScreen(viewModel: TaxCalcViewModel, history: List<com.strobingn.taxcalc.data.CalculationHistory>, totalCalcs: Int, totalTax: Double, padding: PaddingValues) {
    var search by remember { mutableStateOf("") }
    val filtered = history.filter { it.countyName.contains(search, ignoreCase = true) || it.mode.contains(search, ignoreCase = true) }

    Column(Modifier.padding(padding).padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(16.dp)) { Text("RUNNING MEMORY", style = MaterialTheme.typography.labelSmall); Text("$totalCalcs calculations tracked", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Total tax calculated: ${formatCurrency(totalTax)}", color = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search history") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No calculations yet. Go crunch some numbers!", color = MaterialTheme.colorScheme.onSurfaceVariant) } } else { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filtered) { item -> HistoryCard(item, onLoad = { viewModel.loadFromHistory(item) }) } } }
    }
}

@Composable
fun HistoryCard(item: com.strobingn.taxcalc.data.CalculationHistory, onLoad: () -> Unit) {
    Card(modifier = Modifier.clickable { onLoad() }) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text(java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(item.timestamp)), style = MaterialTheme.typography.labelSmall); Badge { Text(item.mode) } }
            Text(item.countyName, fontWeight = FontWeight.Bold)
            Text("${item.taxRate}% rate used at time of calc", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(if (item.mode == "FORWARD") "Subtotal ${formatCurrency(item.inputAmount)} → Tax ${formatCurrency(item.taxAmount)} → Total ${formatCurrency(item.outputAmount)}" else "Total ${formatCurrency(item.inputAmount)} → Tax ${formatCurrency(item.taxAmount)} → Pre-Tax ${formatCurrency(item.outputAmount)}", fontSize = 15.sp)
        }
    }
}

fun formatCurrency(amount: Double): String = NumberFormat.getCurrencyInstance(Locale.US).format(amount)

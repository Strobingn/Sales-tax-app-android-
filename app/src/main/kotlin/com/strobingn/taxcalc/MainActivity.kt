package com.strobingn.taxcalc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strobingn.taxcalc.AppScreen
import com.strobingn.taxcalc.data.County
import com.strobingn.taxcalc.ui.CountiesScreen
import com.strobingn.taxcalc.ui.HistoryScreen
import com.strobingn.taxcalc.ui.theme.TaxCalcTheme
import com.strobingn.taxcalc.utils.formatCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            TaxCalcTheme {
                val viewModel: TaxCalcViewModel = viewModel()
                TaxCalcAppWithNavigation(viewModel = viewModel)
            }
        }
    }
}

// Helper functions
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
}

private fun shareResult(
    context: Context,
    result: CalculationResult,
    mode: CalcMode,
    county: County?
) {
    val rate = county?.taxRate ?: 0.0
    val text = when (mode) {
        CalcMode.FORWARD -> "Forward Calc: Subtotal=${formatCurrency(result.inputAmount)}, Tax=${formatCurrency(result.taxAmount)} (${rate}%), Total=${formatCurrency(result.outputAmount)}"
        CalcMode.REVERSE -> "Reverse Calc: Total=${formatCurrency(result.inputAmount)}, Pre-Tax=${formatCurrency(result.outputAmount)}, Tax=${formatCurrency(result.taxAmount)} (${rate}%)"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Result"))
}

// Calculator Screen Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalcScreen(viewModel: TaxCalcViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val mode by viewModel.mode.collectAsState()
    val input by viewModel.inputAmount.collectAsState()
    val useCustomKeypad by viewModel.useCustomKeypad.collectAsState()
    val result by viewModel.calculationResult.collectAsState()
    val selectedCounty by viewModel.selectedCounty.collectAsState()
    val counties by viewModel.counties.collectAsState()
    
    var showCountySheet by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        
        // Mode selector - Modern segmented look
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ModeChip(
                    selected = mode == CalcMode.FORWARD,
                    onClick = { viewModel.setMode(CalcMode.FORWARD) },
                    label = "Add Tax",
                    modifier = Modifier.weight(1f)
                )
                ModeChip(
                    selected = mode == CalcMode.REVERSE,
                    onClick = { viewModel.setMode(CalcMode.REVERSE) },
                    label = "Remove Tax",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // County selector - Modern design
        Card(
            modifier = Modifier.fillMaxWidth().clickable { showCountySheet = true },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = CardDefaults.outlinedCardBorder()
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

        // Amount input - Modern design
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (mode == CalcMode.FORWARD) "Subtotal Amount" else "Total Amount Paid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                if (!useCustomKeypad) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { viewModel.setInputAmount(it) },
                        label = null,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                        prefix = { Text("$", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold) }
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$${if (input.isEmpty()) "0.00" else input}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (input.isEmpty())
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick amounts
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 25, 50, 100, 500).forEach { amt ->
                val scale = remember { Animatable(1f) }
                AssistChip(
                    onClick = {
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

        // Calculate button - Modern prominent design
        Button(
            onClick = {
                when {
                    selectedCounty == null -> scope.launch { snackbarHostState.showSnackbar("Please select a county first") }
                    input.isEmpty() || input.toDoubleOrNull() == null || input.toDoubleOrNull()!! <= 0 ->
                        scope.launch { snackbarHostState.showSnackbar("Enter a valid amount greater than 0") }
                    else -> viewModel.calculateAndSave()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Default.Calculate, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("CALCULATE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = CardDefaults.outlinedCardBorder()
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
                        CopyableRow("Pre-Tax / Subtotal", formatCurrency(preTaxVal), context, snackbarHostState, scope)
                        CopyableRow("Tax (${selectedCounty?.taxRate ?: 0}%)", formatCurrency(res.taxAmount), context, snackbarHostState, scope)

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

        SnackbarHost(hostState = snackbarHostState)
    }

    if (showCountySheet) {
        CountyPickerSheet(
            counties = counties,
            onSelect = { viewModel.selectCounty(it); showCountySheet = false },
            onDismiss = { showCountySheet = false }
        )
    }
}

// Main App Navigation with Bottom Bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxCalcAppWithNavigation(viewModel: TaxCalcViewModel) {
    val selectedScreen by viewModel.selectedScreen.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                val navItems = listOf(
                    NavItem(AppScreen.CALC, "Home", Icons.Default.Home),
                    NavItem(AppScreen.HISTORY, "History", Icons.Default.History),
                    NavItem(AppScreen.COUNTIES, "Counties", Icons.Default.LocationCity),
                    NavItem(AppScreen.SETTINGS, "Settings", Icons.Default.Settings)
                )
                
                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.label) },
                        selected = selectedScreen == item.screen,
                        onClick = { viewModel.setScreen(item.screen) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedScreen) {
                AppScreen.CALC -> CalcScreen(viewModel)
                AppScreen.HISTORY -> HistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { viewModel.setScreen(AppScreen.CALC) }
                )
                AppScreen.COUNTIES -> CountiesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { viewModel.setScreen(AppScreen.CALC) }
                )
                AppScreen.SETTINGS -> Text("Settings Screen - Coming Soon", modifier = Modifier.padding(16.dp))
                AppScreen.COMPARE -> Text("Compare Screen - Coming Soon", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

// Navigation data class
private data class NavItem(
    val screen: AppScreen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// ModeChip composable - Modern segmented button look
@Composable
fun ModeChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = if (selected) ButtonDefaults.buttonElevation(defaultElevation = 4.dp) else null,
        border = if (!selected) ButtonDefaults.outlinedButtonBorder else null,
        colors = if (selected) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) {
        Text(label, fontWeight = FontWeight.Medium)
    }
}

// CopyableRow composable
@Composable
fun CopyableRow(
    label: String,
    value: String,
    context: Context,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                copyToClipboard(context, value)
                scope.launch { snackbarHostState.showSnackbar("$label copied to clipboard") }
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

// CustomKeyPad composable
@Composable
fun CustomKeyPad(viewModel: TaxCalcViewModel) {
    val buttons = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf(".", "0", "⌫")
    )
    
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Button(
                        onClick = {
                            when (key) {
                                "⌫" -> viewModel.backspaceInput()
                                "." -> viewModel.appendInput(".")
                                else -> viewModel.appendInput(key)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// CountyPickerSheet composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountyPickerSheet(
    counties: List<County>,
    onSelect: (County) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Select County", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            if (counties.isEmpty()) {
                Text("No counties available. Add some first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                counties.forEach { county ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(county) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (county.isFavorite) {
                            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Spacer(Modifier.width(32.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(county.name, style = MaterialTheme.typography.bodyLarge)
                            Text("${county.taxRate}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

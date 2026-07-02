        // Main scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
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
    }

    if (showCountySheet) {
        CountyPickerSheet(
            counties = counties,
            onSelect = { viewModel.selectCounty(it); showCountySheet = false },
            onDismiss = { showCountySheet = false }
        )
    }
}
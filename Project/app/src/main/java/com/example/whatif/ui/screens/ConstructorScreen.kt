package com.example.whatif.ui.screens

import com.example.whatif.viewmodels.ConstructorUiState
import com.example.whatif.viewmodels.ConstructorViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.whatif.models.Frequency
import com.example.whatif.ui.components.ChartLegend
import com.example.whatif.ui.components.ChartSeries
import com.example.whatif.ui.components.LabeledSlider
import com.example.whatif.ui.components.NumberField
import com.example.whatif.ui.components.ProjectionChart
import com.example.whatif.ui.components.ReadoutRow
import com.example.whatif.ui.components.ScrubReadout
import com.example.whatif.ui.components.SectionCard
import com.example.whatif.ui.components.StatBlock
import com.example.whatif.ui.components.WhatIfTopBar
import com.example.whatif.util.formatMoney
import com.example.whatif.util.formatPercent
import com.example.whatif.util.yearsText
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun ConstructorRoute(
    onBack: () -> Unit,
    viewModel: ConstructorViewModel = viewModel(factory = ConstructorViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ConstructorScreen(
        state = state,
        onBack = onBack,
        suggestedName = viewModel::suggestedName,
        onHabitNameChange = viewModel::onHabitNameChange,
        onAmountChange = viewModel::onAmountChange,
        onFrequencyChange = viewModel::onFrequencyChange,
        onYearsChange = viewModel::onYearsChange,
        onReturnChange = viewModel::onReturnChange,
        onInflationChange = viewModel::onInflationChange,
        onSave = viewModel::saveScenario,
    )
}

@Composable
fun ConstructorScreen(
    state: ConstructorUiState,
    onBack: () -> Unit,
    suggestedName: () -> String,
    onHabitNameChange: (String) -> Unit,
    onAmountChange: (Double) -> Unit,
    onFrequencyChange: (Frequency) -> Unit,
    onYearsChange: (Float) -> Unit,
    onReturnChange: (Float) -> Unit,
    onInflationChange: (Float) -> Unit,
    onSave: (name: String) -> Unit,
) {
    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            WhatIfTopBar(
                title = state.editingName ?: "New scenario",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                enabled = state.isFormValid,
                onClick = { showSaveDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text("Save scenario") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HabitCard(
                state = state,
                onHabitNameChange = onHabitNameChange,
                onAmountChange = onAmountChange,
                onFrequencyChange = onFrequencyChange,
            )

            SectionCard(title = "Term and returns") {
                LabeledSlider(
                    label = "Term",
                    valueText = yearsText(state.params.years),
                    value = state.params.years.toFloat(),
                    onValueChange = onYearsChange,
                    valueRange = 1f..40f,
                    steps = 38,
                )
                LabeledSlider(
                    label = "Annual return",
                    valueText = formatPercent(state.params.annualReturnPct),
                    value = state.params.annualReturnPct,
                    onValueChange = { onReturnChange(roundToHalf(it)) },
                    valueRange = 0f..25f,
                    steps = 49,
                )
                LabeledSlider(
                    label = "Annual inflation",
                    valueText = formatPercent(state.params.inflationPct),
                    value = state.params.inflationPct,
                    onValueChange = { onInflationChange(roundToHalf(it)) },
                    valueRange = 0f..20f,
                    steps = 39,
                )
            }

            if (state.isFormValid) {
                ResultCard(state)

                ChartCard(
                    state = state,
                    selectedIndex = selectedIndex?.takeIf { it < state.projection.size },
                    onIndexSelected = { selectedIndex = it },
                )
            }
        }
    }

    if (showSaveDialog) {
        SaveScenarioDialog(
            initialName = state.editingName ?: suggestedName(),
            canOverwrite = state.editingId != null,
            onConfirm = { name ->
                onSave(name)
                showSaveDialog = false
                onBack()
            },
            onDismiss = { showSaveDialog = false },
        )
    }
}

private fun roundToHalf(x: Float): Float = (x * 2).roundToInt() / 2f

@Composable
private fun ResultCard(state: ConstructorUiState) {
    val result = state.lastPoint
    SectionCard {
        Text(
            "In ${yearsText(state.params.years)} you will have",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatMoney(result?.nominal ?: 0.0),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            StatBlock(
                label = "In today's prices",
                value = formatMoney(result?.real ?: 0.0),
                valueColor = MaterialTheme.colorScheme.tertiary,
            )
            StatBlock(label = "You will invest", value = formatMoney(result?.contributed ?: 0.0))
        }
        Text(
            "This is a projection, not a guarantee: actual returns may differ.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChartCard(
    state: ConstructorUiState,
    selectedIndex: Int?,
    onIndexSelected: (Int?) -> Unit,
) {
    val nominalColor = MaterialTheme.colorScheme.primary
    val realColor = MaterialTheme.colorScheme.tertiary

    val series = remember(state.projection, nominalColor, realColor) {
        listOf(
            ChartSeries("Nominal (account balance)", state.projection.map { it.nominal }, nominalColor, filled = true),
            ChartSeries("In today's prices", state.projection.map { it.real }, realColor, dashed = true, filled = true),
        )
    }

    SectionCard(title = "Savings growth") {
        ChartLegend(series)
        ProjectionChart(
            series = series,
            selectedIndex = selectedIndex,
            onIndexSelected = onIndexSelected,
        )
        val point = selectedIndex?.let { state.projection.getOrNull(it) }
        ScrubReadout(
            title = point?.let { "After ${yearsText(it.year)}" },
            rows = point?.let {
                listOf(
                    ReadoutRow("Nominal", formatMoney(it.nominal), nominalColor),
                    ReadoutRow("In today's prices", formatMoney(it.real), realColor),
                    ReadoutRow("Invested", formatMoney(it.contributed), MaterialTheme.colorScheme.outline),
                )
            }.orEmpty(),
        )
    }
}

@Composable
private fun HabitCard(
    state: ConstructorUiState,
    onHabitNameChange: (String) -> Unit,
    onAmountChange: (Double) -> Unit,
    onFrequencyChange: (Frequency) -> Unit,
) {
    var amountText by rememberSaveable(state.editingId) {
        mutableStateOf(if (state.params.amount > 0) state.params.amount.roundToLong().toString() else "")
    }

    SectionCard(title = "Your habit") {
        OutlinedTextField(
            value = state.params.habitName,
            onValueChange = onHabitNameChange,
            label = { Text("What you spend on") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        NumberField(
            label = "Amount per purchase",
            value = amountText,
            onValueChange = {
                amountText = it
                onAmountChange(it.toDoubleOrNull() ?: 0.0)
            },
            suffix = "₸",
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Frequency.entries.forEach { frequency ->
                FilterChip(
                    selected = state.params.frequency == frequency,
                    onClick = { onFrequencyChange(frequency) },
                    label = { Text(frequency.label) },
                )
            }
        }
    }
}


@Composable
private fun SaveScenarioDialog(
    initialName: String,
    canOverwrite: Boolean,
    onConfirm: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save scenario") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(50) },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) {
                Text(if (canOverwrite) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

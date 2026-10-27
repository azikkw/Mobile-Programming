package com.example.whatif.ui.screens

import com.example.whatif.viewmodels.CompareUiState
import com.example.whatif.viewmodels.CompareViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.whatif.models.Scenario
import com.example.whatif.ui.components.ChartLegend
import com.example.whatif.ui.components.ChartSeries
import com.example.whatif.ui.components.EmptyState
import com.example.whatif.ui.components.LabeledSlider
import com.example.whatif.ui.components.ProjectionChart
import com.example.whatif.ui.components.ReadoutRow
import com.example.whatif.ui.components.ScrubReadout
import com.example.whatif.ui.components.SectionCard
import com.example.whatif.ui.components.StatBlock
import com.example.whatif.ui.components.WhatIfTopBar
import com.example.whatif.util.formatMoney
import com.example.whatif.util.formatPercent
import com.example.whatif.util.yearsText
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CompareRoute(
    onBack: () -> Unit,
    viewModel: CompareViewModel = viewModel(factory = CompareViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CompareScreen(state = state, onBack = onBack)
}

@Composable
fun CompareScreen(
    state: CompareUiState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            WhatIfTopBar(
                title = "Compare",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            CompareUiState.Loading -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            CompareUiState.NotFound -> EmptyState(
                title = "Scenario not found",
                message = "One of the scenarios may have been deleted.",
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                action = { Button(onClick = onBack) { Text("Back") } },
            )

            is CompareUiState.Ready -> CompareContent(state, Modifier.padding(padding))
        }
    }
}

@Composable
private fun CompareContent(state: CompareUiState.Ready, modifier: Modifier = Modifier) {
    var showReal by rememberSaveable { mutableStateOf(true) }
    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    var horizon by rememberSaveable { mutableIntStateOf(state.comparison.years) }
    val years = horizon.coerceIn(1, state.comparison.years)

    val colorA = MaterialTheme.colorScheme.primary
    val colorB = MaterialTheme.colorScheme.tertiary

    val comparison = state.comparison
    val pointsA = remember(comparison, years) { comparison.pointsA.take(years + 1) }
    val pointsB = remember(comparison, years) { comparison.pointsB.take(years + 1) }

    val series = remember(pointsA, pointsB, showReal, colorA, colorB) {
        listOf(
            ChartSeries(
                label = state.a.name,
                values = pointsA.map { if (showReal) it.real else it.nominal },
                color = colorA,
                filled = true
            ),
            ChartSeries(
                label = state.b.name,
                values = pointsB.map { if (showReal) it.real else it.nominal },
                color = colorB,
                filled = true
            ),
        )
    }

    val lastA = pointsA.last()
    val lastB = pointsB.last()
    val difference = if (showReal) lastB.real - lastA.real else lastB.nominal - lastA.nominal

    val winner = when {
        difference > 0.5 -> state.b.name
        difference < -0.5 -> state.a.name
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard {
            if (comparison.years > 1) {
                LabeledSlider(
                    label = "Period",
                    valueText = yearsText(years),
                    value = years.toFloat(),
                    onValueChange = { horizon = it.roundToInt() },
                    valueRange = 1f..comparison.years.toFloat(),
                    steps = comparison.years - 2,
                )
            }
            Text(
                "Difference over ${yearsText(years)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatMoney(abs(difference)),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (winner != null) "more with \"$winner\"" else "no difference",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = showReal,
                    onClick = { showReal = true },
                    label = { Text("In today's money") },
                )
                FilterChip(
                    selected = !showReal,
                    onClick = { showReal = false },
                    label = { Text("Nominal") },
                )
            }
        }

        SectionCard(title = "Savings by year") {
            ChartLegend(series)
            ProjectionChart(
                series = series,
                selectedIndex = selectedIndex?.takeIf { it < pointsA.size },
                onIndexSelected = { selectedIndex = it },
            )
            val index = selectedIndex?.takeIf { it < pointsA.size }
            ScrubReadout(
                title = index?.let { "After ${yearsText(it)}" },
                rows = if (index == null) {
                    emptyList()
                } else {
                    listOf(
                        ReadoutRow(state.a.name, formatMoney(series[0].values[index]), colorA),
                        ReadoutRow(state.b.name, formatMoney(series[1].values[index]), colorB),
                    )
                },
            )
        }

        ScenarioSummary(
            scenario = state.a,
            accent = colorA,
            total = pointsA.last().let { if (showReal) it.real else it.nominal },
        )
        ScenarioSummary(
            scenario = state.b,
            accent = colorB,
            total = pointsB.last().let { if (showReal) it.real else it.nominal },
        )
    }
}

@Composable
private fun ScenarioSummary(scenario: Scenario, accent: Color, total: Double) {
    val p = scenario.params
    SectionCard {
        Text(
            scenario.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        Text(
            "${formatMoney(p.amount)} · ${p.frequency.label.lowercase()} · ${yearsText(p.years)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            StatBlock("Return", formatPercent(p.annualReturnPct))
            StatBlock("Inflation", formatPercent(p.inflationPct))
            StatBlock("Total", formatMoney(total), valueColor = accent)
        }
    }
}

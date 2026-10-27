package com.example.whatif.ui.screens

import com.example.whatif.viewmodels.ScenarioItem
import com.example.whatif.viewmodels.ScenariosUiState
import com.example.whatif.viewmodels.ScenariosViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.whatif.ui.components.EmptyState
import com.example.whatif.ui.components.StatBlock
import com.example.whatif.ui.components.WhatIfTopBar
import com.example.whatif.util.formatMoney
import com.example.whatif.util.formatPercent
import com.example.whatif.util.yearsText

@Composable
fun ScenariosRoute(
    onOpenScenario: (Long) -> Unit,
    onCompare: (Long, Long) -> Unit,
    onCreate: () -> Unit,
    viewModel: ScenariosViewModel = viewModel(factory = ScenariosViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    ScenariosScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onToggle = viewModel::onToggleSelection,
        onDelete = viewModel::onDelete,
        onOpen = onOpenScenario,
        onCompare = {
            val ids = state.selectedIds
            if (ids.size == 2) {
                onCompare(ids[0], ids[1])
                viewModel.clearSelection()
            }
        },
        onCreate = onCreate,
    )
}

@Composable
fun ScenariosScreen(
    state: ScenariosUiState,
    snackbarHostState: SnackbarHostState,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onCompare: () -> Unit,
    onCreate: () -> Unit,
) {
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = { WhatIfTopBar(title = "Scenarios") },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when {
                state.canCompare -> ExtendedFloatingActionButton(
                    onClick = onCompare,
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    text = { Text("Compare") },
                )
                state.items.isNotEmpty() -> ExtendedFloatingActionButton(
                    onClick = onCreate,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New scenario") },
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Unit
            state.items.isEmpty() -> EmptyState(
                title = "No scenarios yet",
                message = "Create your first scenario and save it. Then you can compare two options.",
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                action = { Button(onClick = onCreate) { Text("Create scenario") } },
            )
            else -> LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "Tap a card to open it. Tick two scenarios to compare them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(items = state.items, key = { it.scenario.id }) { item ->
                    ScenarioCard(
                        item = item,
                        selected = item.scenario.id in state.selectedIds,
                        onToggle = { onToggle(item.scenario.id) },
                        onOpen = { onOpen(item.scenario.id) },
                        onDelete = { pendingDeleteId = item.scenario.id },
                    )
                }
            }
        }
    }

    pendingDeleteId?.let { id ->
        val name = state.items.firstOrNull { it.scenario.id == id }?.scenario?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete scenario?") },
            text = { Text("\"$name\" will be deleted permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(id)
                    pendingDeleteId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun ScenarioCard(
    item: ScenarioItem,
    selected: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = item.scenario.params
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    item.scenario.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${formatMoney(p.amount)} · ${p.frequency.label.lowercase()} · ${yearsText(p.years)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Return ${formatPercent(p.annualReturnPct)} · inflation ${formatPercent(p.inflationPct)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatBlock("Total", formatMoney(item.finalNominal), valueColor = MaterialTheme.colorScheme.primary)
                    StatBlock("In today's prices", formatMoney(item.finalReal))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete scenario")
            }
        }
    }
}

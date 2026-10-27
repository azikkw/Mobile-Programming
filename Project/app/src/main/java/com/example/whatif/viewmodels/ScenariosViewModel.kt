package com.example.whatif.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.whatif.repository.ScenarioRepository
import com.example.whatif.calculators.ProjectionCalculator
import com.example.whatif.models.Scenario
import com.example.whatif.whatIfApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScenarioItem(
    val scenario: Scenario,
    val finalNominal: Double,
    val finalReal: Double,
)

data class ScenariosUiState(
    val items: List<ScenarioItem> = emptyList(),
    val selectedIds: List<Long> = emptyList(),
    val isLoading: Boolean = true,
) {
    val canCompare: Boolean get() = selectedIds.size == ScenariosViewModel.MAX_SELECTED
}

class ScenariosViewModel(
    private val repository: ScenarioRepository,
) : ViewModel() {

    private val selected = MutableStateFlow<List<Long>>(emptyList())

    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    private val items: Flow<List<ScenarioItem>> = repository.scenarios
        .map { list ->
            list.sortedByDescending { it.id }.map { scenario ->
                val last = ProjectionCalculator.project(scenario.params).last()
                ScenarioItem(scenario, last.nominal, last.real)
            }
        }
        .flowOn(Dispatchers.Default)

    val uiState: StateFlow<ScenariosUiState> = combine(items, selected) { list, sel ->
        ScenariosUiState(
            items = list,
            selectedIds = sel.filter { id -> list.any { it.scenario.id == id } },
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScenariosUiState())

    fun onToggleSelection(id: Long) {
        val current = selected.value
        when {
            id in current -> selected.value = current - id
            current.size >= MAX_SELECTED -> viewModelScope.launch {
                _events.send("You can select only two scenarios to compare")
            }
            else -> selected.value = current + id
        }
    }

    fun onDelete(id: Long) {
        repository.delete(id)
        selected.update { it - id }
    }

    fun clearSelection() {
        selected.value = emptyList()
    }

    companion object {
        const val MAX_SELECTED = 2

        val Factory = viewModelFactory {
            initializer { ScenariosViewModel(whatIfApp().repository) }
        }
    }
}

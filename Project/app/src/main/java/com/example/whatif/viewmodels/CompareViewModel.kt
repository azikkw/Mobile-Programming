package com.example.whatif.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.whatif.repository.ScenarioRepository
import com.example.whatif.models.Comparison
import com.example.whatif.calculators.ComparisonCalculator
import com.example.whatif.models.Scenario
import androidx.navigation.toRoute
import com.example.whatif.navigation.Compare
import com.example.whatif.whatIfApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface CompareUiState {
    data object Loading : CompareUiState
    data object NotFound : CompareUiState
    data class Ready(
        val a: Scenario,
        val b: Scenario,
        val comparison: Comparison,
    ) : CompareUiState
}

class CompareViewModel(
    repository: ScenarioRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Compare>()
    private val idA: Long = route.idA
    private val idB: Long = route.idB

    val uiState: StateFlow<CompareUiState> = repository.scenarios
        .map { list ->
            val a = list.firstOrNull { it.id == idA }
            val b = list.firstOrNull { it.id == idB }
            if (a == null || b == null) {
                CompareUiState.NotFound
            } else {
                CompareUiState.Ready(a, b, ComparisonCalculator.compare(a, b))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompareUiState.Loading)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CompareViewModel(
                    repository = whatIfApp().repository,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}

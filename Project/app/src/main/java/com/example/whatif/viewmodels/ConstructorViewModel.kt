package com.example.whatif.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.whatif.repository.ScenarioRepository
import com.example.whatif.models.Frequency
import com.example.whatif.calculators.ProjectionCalculator
import com.example.whatif.models.ProjectionParams
import com.example.whatif.models.ProjectionPoint
import com.example.whatif.models.Scenario
import androidx.navigation.toRoute
import com.example.whatif.navigation.Constructor
import com.example.whatif.whatIfApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class ConstructorUiState(
    val params: ProjectionParams = ProjectionParams(),
    val projection: List<ProjectionPoint> = emptyList(),
    val editingId: Long? = null,
    val editingName: String? = null,
) {
    val lastPoint: ProjectionPoint? get() = projection.lastOrNull()
    val isFormValid: Boolean get() = params.habitName.isNotBlank() && params.amount > 0
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConstructorViewModel(
    private val repository: ScenarioRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // scenarioId == 0 means "new scenario", and the repository has no scenario with id 0.
    private val initialScenario: Scenario? =
        repository.get(savedStateHandle.toRoute<Constructor>().scenarioId)

    private val params = MutableStateFlow(initialScenario?.params ?: ProjectionParams())
    private val editing = MutableStateFlow(initialScenario?.let { it.id to it.name })

    val uiState: StateFlow<ConstructorUiState> = combine(params, editing) { p, e -> p to e }
        .mapLatest { (p, e) ->
            ConstructorUiState(
                params = p,
                projection = withContext(Dispatchers.Default) { ProjectionCalculator.project(p) },
                editingId = e?.first,
                editingName = e?.second,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConstructorUiState(
                params = params.value,
                projection = ProjectionCalculator.project(params.value),
                editingId = editing.value?.first,
                editingName = editing.value?.second,
            ),
        )

    fun onHabitNameChange(name: String) = params.update { it.copy(habitName = name.take(40)) }
    fun onAmountChange(amount: Double) = params.update { it.copy(amount = amount) }
    fun onFrequencyChange(frequency: Frequency) = params.update { it.copy(frequency = frequency) }
    fun onYearsChange(years: Float) = params.update { it.copy(years = years.roundToInt().coerceIn(1, 40)) }
    fun onReturnChange(pct: Float) = params.update { it.copy(annualReturnPct = pct) }
    fun onInflationChange(pct: Float) = params.update { it.copy(inflationPct = pct) }

    fun suggestedName(): String {
        val p = params.value
        return "${p.habitName.ifBlank { "Habit" }}: ${p.frequency.label.lowercase()}"
    }

    fun saveScenario(name: String) {
        val title = name.trim().ifBlank { suggestedName() }
        val current = editing.value
        val id = current?.first ?: 0L
        val savedId = repository.save(Scenario(id = id, name = title, params = params.value))
        editing.value = savedId to title
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ConstructorViewModel(
                    repository = whatIfApp().repository,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}

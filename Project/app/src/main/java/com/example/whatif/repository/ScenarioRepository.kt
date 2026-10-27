package com.example.whatif.repository

import android.content.Context
import com.example.whatif.models.Frequency
import com.example.whatif.models.ProjectionParams
import com.example.whatif.models.Scenario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

interface ScenarioRepository {
    val scenarios: StateFlow<List<Scenario>>
    fun get(id: Long): Scenario?

    fun save(scenario: Scenario): Long
    fun delete(id: Long)
}

class PrefsScenarioRepository(context: Context) : ScenarioRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())

    override val scenarios: StateFlow<List<Scenario>> = state.asStateFlow()

    override fun get(id: Long): Scenario? = state.value.firstOrNull { it.id == id }

    override fun save(scenario: Scenario): Long {
        val current = state.value
        val saved = if (scenario.id == 0L) {
            scenario.copy(id = (current.maxOfOrNull { it.id } ?: 0L) + 1L)
        } else {
            scenario
        }
        val updated = if (current.any { it.id == saved.id }) {
            current.map { if (it.id == saved.id) saved else it }
        } else {
            current + saved
        }
        commit(updated)
        return saved.id
    }

    override fun delete(id: Long) {
        commit(state.value.filterNot { it.id == id })
    }

    private fun commit(list: List<Scenario>) {
        state.value = list
        prefs.edit { putString(KEY_JSON, encode(list)) }
    }

    private fun load(): List<Scenario> {
        val json = prefs.getString(KEY_JSON, null) ?: return seed()
        return runCatching { decode(json) }.getOrElse { emptyList() }
    }

    private fun seed(): List<Scenario> {
        val daily = Scenario(
            id = 1,
            name = "Coffee every day",
            params = ProjectionParams(habitName = "Coffee", amount = 300.0, frequency = Frequency.DAILY),
        )
        val weekdays = Scenario(
            id = 2,
            name = "Coffee on weekdays only",
            params = ProjectionParams(habitName = "Coffee", amount = 300.0, frequency = Frequency.WEEKDAYS),
        )
        val list = listOf(daily, weekdays)
        prefs.edit { putString(KEY_JSON, encode(list)) }
        return list
    }

    private fun encode(list: List<Scenario>): String {
        val array = JSONArray()
        list.forEach { s ->
            array.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("habit", s.params.habitName)
                    .put("amount", s.params.amount)
                    .put("frequency", s.params.frequency.name)
                    .put("years", s.params.years)
                    .put("return", s.params.annualReturnPct.toDouble())
                    .put("inflation", s.params.inflationPct.toDouble()),
            )
        }
        return array.toString()
    }

    private fun decode(json: String): List<Scenario> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Scenario(
                id = o.getLong("id"),
                name = o.getString("name"),
                params = ProjectionParams(
                    habitName = o.getString("habit"),
                    amount = o.getDouble("amount"),
                    frequency = Frequency.valueOf(o.getString("frequency")),
                    years = o.getInt("years"),
                    annualReturnPct = o.getDouble("return").toFloat(),
                    inflationPct = o.getDouble("inflation").toFloat(),
                ),
            )
        }
    }

    private companion object {
        const val PREFS_NAME = "whatif_scenarios"
        const val KEY_JSON = "scenarios_json"
    }
}

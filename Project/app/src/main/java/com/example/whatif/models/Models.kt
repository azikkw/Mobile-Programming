package com.example.whatif.models

enum class Frequency(val label: String, val timesPerYear: Double) {
    DAILY("Every day", 365.0),
    WEEKDAYS("Weekdays only", 260.0),
    WEEKLY("Once a week", 52.0),
}

data class ProjectionParams(
    val habitName: String = "",
    val amount: Double = 0.0,
    val frequency: Frequency = Frequency.DAILY,
    val years: Int = 5,
    val annualReturnPct: Float = 10f,
    val inflationPct: Float = 8f,
)

data class ProjectionPoint(
    val year: Int,
    val nominal: Double,
    val real: Double,
    val contributed: Double,
)

data class Scenario(
    val id: Long,
    val name: String,
    val params: ProjectionParams,
)

data class Comparison(
    val years: Int,
    val pointsA: List<ProjectionPoint>,
    val pointsB: List<ProjectionPoint>,
)

package com.example.whatif.calculators

import com.example.whatif.models.Comparison
import com.example.whatif.models.Scenario
import kotlin.math.max

object ComparisonCalculator {
    fun compare(a: Scenario, b: Scenario): Comparison {
        val horizon = max(a.params.years, b.params.years)

        return Comparison(
            years = horizon,
            pointsA = ProjectionCalculator.project(a.params.copy(years = horizon)),
            pointsB = ProjectionCalculator.project(b.params.copy(years = horizon)),
        )
    }
}
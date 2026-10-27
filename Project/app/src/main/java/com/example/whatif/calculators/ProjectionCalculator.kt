package com.example.whatif.calculators

import com.example.whatif.models.Comparison
import com.example.whatif.models.ProjectionParams
import com.example.whatif.models.ProjectionPoint
import com.example.whatif.models.Scenario
import kotlin.math.max
import kotlin.math.pow

object ProjectionCalculator {

    fun monthlyContribution(params: ProjectionParams): Double =
        params.amount * params.frequency.timesPerYear / 12.0

    fun project(params: ProjectionParams): List<ProjectionPoint> {
        val monthly = monthlyContribution(params)
        val r = params.annualReturnPct / 100.0 / 12.0
        val inflation = params.inflationPct / 100.0

        return (0..params.years).map { year ->
            val months = year * 12

            val nominal = if (r == 0.0) {
                monthly * months
            } else {
                monthly * ((1 + r).pow(months) - 1) / r
            }

            ProjectionPoint(
                year = year,
                nominal = nominal,
                real = nominal / (1 + inflation).pow(year),
                contributed = monthly * months,
            )
        }
    }

}

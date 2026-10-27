package com.example.whatif.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.whatif.ui.screens.CompareRoute
import com.example.whatif.ui.screens.ConstructorRoute
import com.example.whatif.ui.screens.ScenariosRoute
import kotlinx.serialization.Serializable

@Serializable
object Scenarios

/** [scenarioId] == 0 means "create a new scenario". */
@Serializable
data class Constructor(val scenarioId: Long = 0L)

@Serializable
data class Compare(val idA: Long, val idB: Long)

@Composable
fun WhatIfApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Scenarios) {
        composable<Scenarios> {
            ScenariosRoute(
                onOpenScenario = { id -> navController.navigate(Constructor(id)) },
                onCompare = { a, b -> navController.navigate(Compare(a, b)) },
                onCreate = { navController.navigate(Constructor()) },
            )
        }

        composable<Constructor> {
            ConstructorRoute(onBack = { navController.navigateUp() })
        }

        composable<Compare> {
            CompareRoute(onBack = { navController.navigateUp() })
        }
    }
}

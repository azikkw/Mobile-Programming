package com.example.whatif

import android.app.Application
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.whatif.repository.PrefsScenarioRepository
import com.example.whatif.repository.ScenarioRepository

class WhatIfApplication : Application() {
    val repository: ScenarioRepository by lazy { PrefsScenarioRepository(this) }
}

fun CreationExtras.whatIfApp(): WhatIfApplication = this[APPLICATION_KEY] as WhatIfApplication

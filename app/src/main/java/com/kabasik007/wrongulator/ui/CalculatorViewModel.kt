package com.kabasik007.wrongulator.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.kabasik007.wrongulator.core.CalcKey
import com.kabasik007.wrongulator.core.CalculatorMachine
import com.kabasik007.wrongulator.core.CalculatorState

/** Owns screen state. In production only a verified paid entitlement may enable accuracy. */
class CalculatorViewModel : ViewModel() {
    private val machine = CalculatorMachine()

    var state by mutableStateOf(CalculatorState())
        private set

    var debugProPreview by mutableStateOf(false)
        private set

    fun press(key: CalcKey) {
        state = machine.press(state, key, debugProPreview)
    }

    /** Developer-only preview. Must never be wired to a release build. */
    fun setDebugProPreview(enabled: Boolean) {
        debugProPreview = enabled
        state = CalculatorState()
    }
}

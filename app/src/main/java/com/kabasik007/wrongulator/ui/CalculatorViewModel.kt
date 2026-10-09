package com.kabasik007.wrongulator.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.kabasik007.wrongulator.core.CalcKey
import com.kabasik007.wrongulator.core.CalculatorMachine
import com.kabasik007.wrongulator.core.CalculatorState
import com.kabasik007.wrongulator.core.ResultKind

/** Stateful screen controller; paid entitlements must be verified externally. */
class CalculatorViewModel : ViewModel() {
    private val machine = CalculatorMachine()

    var state by mutableStateOf(CalculatorState())
        private set

    var debugProPreview by mutableStateOf(false)
        private set

    /** Increases only for new incorrect '=' results, not for invalid operations or repeated taps. */
    var paywallTrigger by mutableIntStateOf(0)
        private set

    fun press(key: CalcKey) {
        val old = state
        val next = machine.press(old, key, debugProPreview)
        state = next

        if (!debugProPreview &&
            key == CalcKey.EQUALS &&
            old.operator != null &&
            next !== old &&
            next.resultKind == ResultKind.WRONG
        ) {
            paywallTrigger++
        }
    }

    /** Developer-only feature preview; never activate from a plan-card selection. */
    fun setAccuracyPreviewEnabled(enabled: Boolean) {
        debugProPreview = enabled
        state = CalculatorState()
    }
}

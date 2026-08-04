package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Flüchtiger UI-Zustand des gemeinsamen Inspectors.
 * Er gehört weder zur Karte noch zur Undo-Historie und wird deshalb nicht persistiert.
 */
object InspektorSichtbarkeit {
    var offen: Boolean by mutableStateOf(true)
        private set

    fun öffnen() {
        offen = true
    }

    fun schließen() {
        offen = false
    }
}

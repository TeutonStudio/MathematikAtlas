package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.PrädikatsRechenwegInhalt
import de.TeutonStudio.MathematikRechenSystem.kern.*
import org.junit.Rule
import org.junit.Test

/** Der gemeinsame Compose-Dialog ist ein Overlay, kein zusätzliches AWT-Fenster. */
class PrädikatsRechenwegDesktopTest {
    @get:Rule val compose = createComposeRule()

    @Test fun schrittOeffnetDetailsUndVeralteteDetailsSchliessen() {
        val x = Variable("x")
        val prädikat = Gleichheit(addition(multiplikation(RationaleZahl.von(2), x), RationaleZahl.von(3)), RationaleZahl.von(7))
        val lösung = lösePrädikat(prädikat, listOf(GebundeneMengenVariable(x, ReelleZahlen)))
        val zustand = mutableStateOf(KnotenAuswertungsErgebnis(emptyMap(), schritte = lösung.schritte))
        val zeile = "1. ${lösung.schritte.first().titel}"
        compose.setContent { MaterialTheme { PrädikatsRechenwegInhalt(zustand.value, Modifier.fillMaxSize()) } }

        compose.onNodeWithText(zeile).performClick()
        compose.onNodeWithText("Zurück").assertIsDisplayed()
        compose.runOnIdle { zustand.value = zustand.value.copy(warnungen = listOf("Neue Eingabe")) }
        compose.onNodeWithText("Zurück").assertDoesNotExist()
        compose.onNodeWithText("Neue Eingabe").assertIsDisplayed()

        compose.onNodeWithText(zeile).performClick()
        compose.onNodeWithText("Zurück").performClick()
        compose.onNodeWithText("Zurück").assertDoesNotExist()
        compose.onNodeWithText(zeile).assertIsDisplayed()
    }
}

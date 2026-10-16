package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MethodenQuelleGeraeteTest {
    @get:Rule val compose = createComposeRule()
    @Test fun joinQuelleBietetFesteArgumentanzahlMitValidierung() {
        val kandidat = methodenKnotenQuellen().first { it.id == "operator:ZAHL:zahl.addition" }
        val state = mutableStateOf(konfiguriereMethodenQuelle(MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt.Zero), kandidat.knoten))
        val aktionen = object : KnotenInspektorAktionen {
            override fun knoten(knoten: KnotenDaten) { state.value = knoten }
            override fun parameter(schlüssel: String, wert: String) = Unit
            override fun name(wert: String) = Unit
            override fun eigenschaften(eigenschaften: Map<String, KnotenEigenschaft>) = Unit
            override fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId) = Unit
        }
        compose.setContent { MaterialTheme { Column { MethodenQuellenInspektor.Inhalt(state.value, null, aktionen) } } }
        compose.onNodeWithText("Anzahl Argumente").performTextReplacement("1")
        compose.onNodeWithText("Argumentanzahl übernehmen").assertIsNotEnabled()
        compose.onNodeWithText("Anzahl Argumente").performTextReplacement("4")
        compose.onNodeWithText("Argumentanzahl übernehmen").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(4, methodenQuellKnoten(state.value)!!.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang }) }
    }
    @Test fun quellenDialogSuchtKonfiguriertUndVerwirftEntwurf() {
        val standard = MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt.Zero)
        val state = mutableStateOf(standard)
        val aktionen = object : KnotenInspektorAktionen {
            override fun knoten(knoten: KnotenDaten) { state.value = knoten }
            override fun parameter(schlüssel: String, wert: String) = Unit
            override fun name(wert: String) = Unit
            override fun eigenschaften(eigenschaften: Map<String, KnotenEigenschaft>) = Unit
            override fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId) = Unit
        }
        compose.setContent { MaterialTheme { Column { MethodenQuellenInspektor.Inhalt(state.value, null, aktionen) } } }
        compose.onNodeWithText("Identität").performClick()
        compose.onNodeWithText("Operator suchen").performTextInput("Potenz")
        compose.onAllNodes(hasText("Potenz", substring = false) and hasClickAction()).onLast().performClick()
        compose.onNodeWithText("Abbrechen").performClick()
        compose.runOnIdle { assertEquals(standard, state.value) }
        compose.onNodeWithText("Identität").performClick()
        compose.onNodeWithText("Operator suchen").performTextInput("Potenz")
        compose.onAllNodes(hasText("Potenz", substring = false) and hasClickAction()).onLast().performClick()
        compose.onNodeWithText("Übernehmen").performClick()
        compose.runOnIdle {
            assertNotNull(methodenQuellKnoten(state.value))
            assertEquals(standard.anschlüsse.last().id, state.value.anschlüsse.single().id)
            assertEquals("mathematik.methode", state.value.art)
        }
    }
}

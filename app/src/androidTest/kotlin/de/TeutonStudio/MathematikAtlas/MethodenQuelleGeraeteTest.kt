package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
        val schrift = mutableStateOf(1f)
        val aktionen = object : KnotenInspektorAktionen {
            override fun knoten(knoten: KnotenDaten) { state.value = knoten }
            override fun parameter(schlüssel: String, wert: String) = Unit
            override fun name(wert: String) = Unit
            override fun eigenschaften(eigenschaften: Map<String, KnotenEigenschaft>) = Unit
            override fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId) = Unit
        }
        compose.setContent {
            val dichte = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(dichte.density, schrift.value)) {
                MaterialTheme { Column { MethodenQuellenInspektor.Inhalt(state.value, null, aktionen) } }
            }
        }
        for (faktor in listOf(1f, 2f)) {
            compose.runOnIdle { schrift.value = faktor; state.value = standard }
            compose.onNodeWithText("Identität").performClick()
            compose.onNodeWithText("Operator suchen").performTextInput("Potenz")
            val liste = compose.onNodeWithTag("rechner-operator-liste").fetchSemanticsNode().boundsInRoot
            assertTrue("Trefferliste braucht sichtbaren Platz auch bei Schriftfaktor $faktor", liste.height > 0f)
            compose.onNodeWithTag("rechner-operator-liste").performScrollToNode(hasTestTag("rechner-operator:operator:ZAHL:zahl.potenz"))
            compose.onNodeWithTag("rechner-operator:operator:ZAHL:zahl.potenz").performClick()
            compose.onNodeWithText("Abbrechen").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(standard, state.value) }
            compose.onNodeWithText("Identität").performClick()
            compose.onNodeWithText("Operator suchen").performTextInput("Potenz")
            compose.onNodeWithTag("rechner-operator-liste").performScrollToNode(hasTestTag("rechner-operator:operator:ZAHL:zahl.potenz"))
            compose.onNodeWithTag("rechner-operator:operator:ZAHL:zahl.potenz").performClick()
            compose.onNodeWithText("Übernehmen").performScrollTo().performClick()
            compose.runOnIdle {
                assertNotNull(methodenQuellKnoten(state.value))
                assertEquals(standard.anschlüsse.last().id, state.value.anschlüsse.single().id)
                assertEquals("mathematik.methode", state.value.art)
            }
        }
    }
}

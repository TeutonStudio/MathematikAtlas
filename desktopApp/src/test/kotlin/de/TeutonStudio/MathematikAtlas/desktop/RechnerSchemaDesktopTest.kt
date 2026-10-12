package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.test.assertTrue
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import org.junit.Rule
import org.junit.Test

class RechnerSchemaDesktopTest {
    @get:Rule val compose = createComposeRule()
    @Test fun falkSchemaStehtMittigWennPlatzVorhandenIst() {
        val matrix = Matrix(listOf(listOf(RationaleZahl.Eins)))
        val wert = KnotenAuswertungsErgebnis(emptyMap(), eingänge = linkedMapOf("a" to BedingterWert(matrix), "b" to BedingterWert(matrix)))
        compose.setContent { MaterialTheme { Box(Modifier.width(900.dp)) { MatrixproduktFalkSchemaInhalt(wert) } } }
        val ausschnitt = compose.onNodeWithContentDescription("Falk-Schema Ausschnitt").fetchSemanticsNode().boundsInRoot
        val schema = compose.onNodeWithContentDescription("Falk-Schema").fetchSemanticsNode().boundsInRoot
        assertTrue(schema.width < ausschnitt.width)
        assertTrue(kotlin.math.abs(schema.center.x - ausschnitt.center.x) <= 2f)
    }

    @Test fun ergebniszelleWaehltSkalarproduktUndWechselSetztAuswahlZurueck() {
        fun matrix(vararg werte: Long) = Matrix(listOf(werte.map { RationaleZahl.von(it) }))
        val links = matrix(1, 2)
        val rechts = Matrix(listOf(listOf(RationaleZahl.von(3), RationaleZahl.von(4)),
            listOf(RationaleZahl.von(5), RationaleZahl.von(6))))
        val wert = mutableStateOf(KnotenAuswertungsErgebnis(emptyMap(), eingänge = linkedMapOf(
            "a" to BedingterWert(links), "b" to BedingterWert(rechts), "c" to BedingterWert(rechts))))
        compose.setContent { MaterialTheme { MatrixproduktFalkSchemaInhalt(wert.value) } }
        compose.onNodeWithContentDescription("P2, Zeile 1, Spalte 2").performClick()
        compose.onNodeWithText("P2, Zeile 1, Spalte 2").assertExists()
        compose.onNodeWithContentDescription("P1, Zeile 1, Spalte 2").performClick()
        compose.onNodeWithText("P1, Zeile 1, Spalte 2").assertExists()
        compose.onNodeWithText("k = 2:", substring = true).performClick()
        compose.runOnIdle { wert.value = wert.value.copy(warnungen = listOf("Geändert")) }
        compose.onNodeWithText("P1, Zeile 1, Spalte 1").assertExists()
    }
}

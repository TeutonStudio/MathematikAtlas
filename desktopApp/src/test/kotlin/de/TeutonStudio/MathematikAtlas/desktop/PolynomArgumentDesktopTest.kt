package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PolynomArgumentDesktopTest {
    @Test fun modusUndNamensentwurfWerdenNurGueltigUebernommen()=runDesktopComposeUiTest(width=420,height=700) {
        var aktuell by mutableStateOf(konfigurierePolynomArgument(ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),POLYNOM_ARGUMENT_EINGANG))
        var aufrufe=0
        setContent {MaterialTheme {Column(Modifier.width(360.dp).verticalScroll(rememberScrollState())) {
            InspektorZahlenRechnerEingänge(aktuell) {aktuell=it;aufrufe++}
        }}}
        onNodeWithText("Polynomvariable").assertDoesNotExist()
        onNodeWithText("Intrinsische Variable").performClick()
        runOnIdle {assertEquals(1,aufrufe);assertTrue(aktuell.anschlüsse.none {it.name=="argument"})}
        onNodeWithText("Polynomvariable").performTextClearance()
        onNodeWithText("Variable übernehmen").assertIsNotEnabled()
        runOnIdle {assertEquals("x",aktuell.parameter[ZAHLENRECHNER_VARIABLE]);assertEquals(1,aufrufe)}
        onNodeWithText("Eingang").performClick()
        onNodeWithText("Intrinsische Variable").assertIsEnabled().performClick()
        onNodeWithText("Polynomvariable").performTextReplacement("sin")
        onNodeWithText("Variable übernehmen").performClick()
        runOnIdle {assertEquals("sin",aktuell.parameter[ZAHLENRECHNER_VARIABLE]);assertEquals(4,aufrufe)}
        onNodeWithText("Eingang").performClick()
        onNodeWithText("Polynomvariable").assertDoesNotExist()
        runOnIdle {assertTrue(aktuell.anschlüsse.any {it.name=="argument"});assertEquals(5,aufrufe)}
    }
}

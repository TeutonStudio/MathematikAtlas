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
class ParameterBereichDesktopTest {
    @Test fun tiefengrenzeVerhindertUngueltigenBereichswechselVorUebernahme()=runDesktopComposeUiTest(width=420,height=700) {
        var bereich:WertebereichKonfiguration=WertebereichKonfiguration.Zahl()
        repeat(63) {bereich=WertebereichKonfiguration.Menge(bereich)}
        val basis=konfiguriereAllgemeinenParameterBereich(MathematikKnotenVorlagen.AllgemeinerParameter.erzeuge(GraphPunkt.Zero),bereich)
        var aufrufe=0
        setContent {MaterialTheme {Column(Modifier.width(360.dp).verticalScroll(rememberScrollState())) {
            InspektorAllgemeinerParameter(basis) {aufrufe++}
        }}}
        onAllNodesWithText("Menge").onLast().assertIsNotEnabled()
        onAllNodesWithText("Tupel").onLast().assertIsNotEnabled()
        onAllNodesWithText("Zahl").onLast().assertIsEnabled()
        runOnIdle {assertEquals(0,aufrufe);assertEquals(bereich,WertebereichKonfiguration.vonEigenschaft(basis.eigenschaften[WertebereichKonfiguration.EIGENSCHAFT]))}
    }
    @Test fun defekterBereichWirdNichtAutomatischDurchRersetzt()=runDesktopComposeUiTest(width=420,height=700) {
        val basis=MathematikKnotenVorlagen.AllgemeinerParameter.erzeuge(GraphPunkt.Zero).let {it.copy(
            eigenschaften=it.eigenschaften+(WertebereichKonfiguration.EIGENSCHAFT to KnotenEigenschaft.Text("defekt")))}
        var aktuell by mutableStateOf(basis)
        setContent {MaterialTheme {Column(Modifier.width(360.dp).verticalScroll(rememberScrollState())) {
            InspektorAllgemeinerParameter(aktuell) {aktuell=it}
        }}}
        onNodeWithText("Ein Wertebereich muss ein Objekt sein.").assertExists()
        onNodeWithText("Zahl").assertIsNotSelected()
        onNodeWithText("Parametername").assertIsNotEnabled()
        runOnIdle {assertEquals(basis,aktuell)}
        onNodeWithText("Matrix").performScrollTo().performClick()
        runOnIdle {
            assertEquals(WertebereichKonfiguration.Matrix(),WertebereichKonfiguration.vonEigenschaft(aktuell.eigenschaften[WertebereichKonfiguration.EIGENSCHAFT]))
            assertEquals(basis.anschlüsse.map {it.id},aktuell.anschlüsse.map {it.id})
        }
    }
}

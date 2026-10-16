package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class MeldungsEbenenDesktopTest {
    @Test fun nurAusgewaehlteMeldungLiegtVorAnderenKoerpernUndAbwahlSenktSieAb() = runDesktopComposeUiTest(width = 1100, height = 1000) {
        val eigentümer = KnotenDaten(art = "test", name = "Meldung", position = GraphPunkt(140f, 350f), größe = GraphGröße(300f, 130f))
        val deckend = KnotenDaten(art = "test", name = "Deckend", position = GraphPunkt(100f, 100f), größe = GraphGröße(400f, 250f),
            eigenschaften = mapOf(KNOTEN_HINTERGRUND_EIGENSCHAFT to KnotenEigenschaft.Farbe(0xFF0000FF)))
        var editor by mutableStateOf(KartenEditorZustand(KartenDaten(name = "Ebenen"), GraphPrüfung(AnschlussArtRegister())))
        var schrift by mutableFloatStateOf(1f)
        var dunkel by mutableStateOf(false)
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f, schrift)) {
            MaterialTheme(colorScheme = if (dunkel) darkColorScheme() else lightColorScheme()) {
                KnotenKartenEditor(editor, modifier = Modifier.testTag("karte"), zeigeKnotenInspektor = false)
            }
        } }
        for (umgekehrt in listOf(false, true)) for (zoom in listOf(.75f, 1.5f)) for (font in listOf(1f, 2f)) for (dark in listOf(false, true)) {
            runOnIdle {
                schrift = font; dunkel = dark
                editor = KartenEditorZustand(KartenDaten(name = "Ebenen", knoten = if (umgekehrt) listOf(deckend, eigentümer) else listOf(eigentümer, deckend),
                    ansicht = AnsichtsFenster(zoom = zoom)), GraphPrüfung(AnschlussArtRegister()))
                editor.meldeKnotenMeldungen(eigentümer.id, listOf(KnotenMeldung("Überlappende Meldung", KnotenMeldungsArt.Fehler)))
            }
            waitForIdle()
            val bounds = onNodeWithText("Überlappende Meldung").fetchSemanticsNode().boundsInRoot
            val punkt = Offset(bounds.right - 10f * zoom, bounds.center.y)
            fun istBlau(): Boolean {
                val bild = onNodeWithTag("karte").captureToImage().toPixelMap()
                val farbe = bild[punkt.x.toInt(), punkt.y.toInt()]
                return farbe.blue > .95f && farbe.red < .05f && farbe.green < .05f
            }
            assertTrue(istBlau(), "Nicht ausgewählte Meldung verdeckt Knoten: Reihenfolge=$umgekehrt Zoom=$zoom Schrift=$font")
            onNodeWithTag("karte").performTouchInput { click(punkt) }
            mainClock.advanceTimeBy(600)
            runOnIdle { assertEquals(deckend.id, editor.ausgewählterKnoten); editor.wähleKnoten(eigentümer.id) }
            assertFalse(istBlau(), "Ausgewählte Meldung muss über dem anderen Körper liegen")
            runOnIdle { editor.wähleKnoten(null) }
            assertTrue(istBlau(), "Abwahl muss Meldung wieder absenken")
            runOnIdle { assertFalse(editor.kannRückgängig()) }
        }
    }
    @Test fun aktiveMeldungLiegtBeiMehrfachauswahlZuletztOben() = runDesktopComposeUiTest(width = 800, height = 650) {
        val a = KnotenDaten(art = "test", name = "A", position = GraphPunkt(120f, 300f), größe = GraphGröße(300f, 130f))
        val b = a.copy(id = neueKnotenId(), name = "B")
        val editor = KartenEditorZustand(KartenDaten(name = "Mehrfach", knoten = listOf(a, b)), GraphPrüfung(AnschlussArtRegister()))
        setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) {
            MaterialTheme { KnotenKartenEditor(editor, zeigeKnotenInspektor = false) }
        } }
        runOnIdle {
            editor.meldeKnotenMeldungen(a.id, listOf(KnotenMeldung("Gleicher Text", KnotenMeldungsArt.Fehler)))
            editor.meldeKnotenMeldungen(b.id, listOf(KnotenMeldung("Gleicher Text", KnotenMeldungsArt.Warnung)))
            editor.wähleKnoten(a.id)
        }
        fun farben(): List<androidx.compose.ui.graphics.Color> {
            val bounds = onNodeWithContentDescription("Fehler: Gleicher Text").fetchSemanticsNode().boundsInRoot
            val bild = onAllNodes(isRoot()).onLast().captureToImage().toPixelMap()
            return (bounds.top.toInt() until bounds.bottom.toInt()).flatMap { y ->
                (bounds.left.toInt() until bounds.right.toInt()).map { x -> bild[x, y] }
            }
        }
        val nurA = farben()
        runOnIdle { editor.wähleKnoten(b.id) }
        val nurB = farben()
        assertNotEquals(nurA, nurB)
        runOnIdle { editor.stelleAuswahlWiederHer(setOf(a.id, b.id), a.id) }
        assertEquals(nurA, farben())
        runOnIdle { editor.stelleAuswahlWiederHer(setOf(a.id, b.id), b.id) }
        assertEquals(nurB, farben())
        runOnIdle { assertFalse(editor.kannRückgängig()) }
    }

}

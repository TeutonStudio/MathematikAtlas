package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import androidx.compose.ui.geometry.Offset
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Rule
import org.junit.Test
import kotlin.test.*

class GeometryNodesDesktopTest {
    @get:Rule val compose=createComposeRule()
    private fun node(suffix: String) = GeometryNodesVorlagen.alle.single { it.art==GEOMETRY_NODES_PREFIX+suffix }.erzeuge(GraphPunkt.Zero)
    @Test fun konfigurationValidiertEntwuerfeUndEineUebernahmeIstRueckgaengig() {
        val original=node("points");val editor=KartenEditorZustand(KartenDaten(name="Konfiguration",knoten=listOf(original)),MathematikKartenLaufzeit().graphPrüfung)
        compose.setContent { MaterialTheme { Column(Modifier.size(500.dp,700.dp).verticalScroll(rememberScrollState())) {
            GeometryNodesEinstellungen(editor.karte.knoten.single()) { editor.führeAus(KartenAktion.KnotenErsetzen(it)) }
        } } }
        compose.onNodeWithTag("geometry-param-anzahl").performTextReplacement("-1")
        compose.onNodeWithTag("geometry-apply").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(original,editor.karte.knoten.single()) }
        compose.onNodeWithTag("geometry-param-anzahl").performScrollTo().performTextReplacement("42")
        compose.onNodeWithTag("geometry-apply").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals("42",editor.karte.knoten.single().parameter["anzahl"])
            assertEquals(original.anschlüsse,editor.karte.knoten.single().anschlüsse)
            editor.rückgängig();assertEquals(original,editor.karte.knoten.single())
        }
    }
    @Test fun vorschauZeigtInstanzenUndReagiertAufDrehenUndZuruecksetzen() {
        val cube=GeometrieOperationen.wuerfel(GeometrieVektor3(0.5,0.5,0.5))
        val geo=GeometrieOperationen.aufPunktenInstanzieren(GeometrieOperationen.gitter(3,3,3.0,3.0),cube)
        val ergebnis=KnotenAuswertungsErgebnis(mapOf("geometrie" to BedingterWert(geo)))
        val renderer=GeometryNodesVorschauRenderer { ergebnis };val n=node("preview")
        compose.setContent { MaterialTheme { Box(Modifier.size(650.dp,520.dp)) {
            renderer.Inhalt(n,false,object:KnotenRendererAktionen { override fun eigenschaftenErsetzen(eigenschaften: Map<String,KnotenEigenschaft>) {} })
        } } }
        compose.onNodeWithText("Direkt:",substring=true).assertTextContains("9 Instanzen",substring=true)
        fun bild(): List<Int> {
            val map=compose.onNodeWithTag("geometry-preview").captureToImage().toPixelMap()
            return List(map.width*map.height) { map[it%map.width,it/map.width].toArgb() }
        }
        val vorher=bild()
        System.getenv("GEOMETRY_SCREENSHOT")?.let { pfad ->
            val map=compose.onRoot().captureToImage().toPixelMap();val image=BufferedImage(map.width,map.height,BufferedImage.TYPE_INT_ARGB)
            for(y in 0 until map.height) for(x in 0 until map.width) image.setRGB(x,y,map[x,y].toArgb())
            ImageIO.write(image,"png",File(pfad))
        }
        compose.onNodeWithTag("geometry-preview").performTouchInput { swipe(center,center+androidx.compose.ui.geometry.Offset(90f,40f)) }
        compose.waitForIdle();assertNotEquals(vorher,bild())
        compose.onNodeWithText("Standardansicht").performClick();compose.waitForIdle();assertEquals(vorher,bild())
    }
    @Test fun fehlerAmVorschauknotenBleibtSichtbar() {
        val knoten = node("preview").copy(position = GraphPunkt(50f, 100f), größe = GraphGröße(650f, 520f))
        val editor = KartenEditorZustand(KartenDaten(name = "Fehler", knoten = listOf(knoten)),
            GraphPrüfung(AnschlussArtRegister(emptyList())))
        val renderer=GeometryNodesVorschauRenderer { KnotenAuswertungsErgebnis(emptyMap(),fehler="Instanzen zuerst realisieren") }
        compose.setContent { MaterialTheme {
            KnotenKartenEditor(editor, Modifier.size(900.dp, 750.dp), rendererFür = { renderer }, zeigeKnotenInspektor = false)
        } }
        val fehler = compose.onNodeWithText("Instanzen zuerst realisieren")
        fehler.assertIsDisplayed()
        compose.onAllNodesWithText("Instanzen zuerst realisieren").assertCountEquals(1)
        val körper = compose.onNodeWithContentDescription("Knoten ${knoten.name}, ${knoten.anschlüsse.size} Anschlüsse").fetchSemanticsNode().boundsInRoot
        assertTrue(fehler.fetchSemanticsNode().boundsInRoot.bottom <= körper.top)

    }
    private fun bild(): List<Int> {
        val map=compose.onNodeWithTag("geometry-preview").captureToImage().toPixelMap()
        return List(map.width*map.height) { map[it%map.width,it/map.width].toArgb() }
    }
    private fun zeigeVorschau() {
        val geo=GeometrieOperationen.wuerfel()
        val r=GeometryNodesVorschauRenderer { KnotenAuswertungsErgebnis(mapOf("geometrie" to BedingterWert(geo))) }
        val n=node("preview")
        compose.setContent { MaterialTheme { Surface { Box(Modifier.size(700.dp,650.dp)) {
            r.Inhalt(n,false,object:KnotenRendererAktionen { override fun eigenschaftenErsetzen(eigenschaften: Map<String,KnotenEigenschaft>) {} })
        } } } }
    }
    @Test fun ansichtswechselPanEinpassenUndSichtbarkeitAendernNurDieDarstellung() {
        zeigeVorschau()
        compose.onNodeWithText("XY").performClick();compose.waitForIdle()
        val xy=bild()
        compose.onNodeWithText("Verschieben").assertIsSelected()
        compose.onNodeWithTag("geometry-preview").performTouchInput { swipe(center,center+Offset(80f,40f)) }
        compose.waitForIdle();assertNotEquals(xy,bild())
        compose.onNodeWithText("Einpassen").performClick();compose.waitForIdle();assertEquals(xy,bild())
        compose.onNodeWithText("Punkte",useUnmergedTree=true).performClick();compose.waitForIdle();assertNotEquals(xy,bild())
        compose.onNodeWithText("Punkte",useUnmergedTree=true).performClick();compose.waitForIdle();assertEquals(xy,bild())
        compose.onNodeWithText("Kanten",useUnmergedTree=true).performClick();compose.waitForIdle();assertNotEquals(xy,bild())
    }
    @Test fun mausradZoomUndEinpassenStellenDasGleicheBildWiederHer() {
        zeigeVorschau();val vorher=bild()
        compose.onNodeWithTag("geometry-preview").performMouseInput { moveTo(center);scroll(-2f) }
        compose.waitForIdle();assertNotEquals(vorher,bild())
        compose.onNodeWithText("Einpassen").performClick();compose.waitForIdle();assertEquals(vorher,bild())
    }
    @Test fun zweiFingerZoomenDieVorschauOhneDieKarteZuVerschieben() {
        val geo=GeometrieOperationen.wuerfel()
        val n=node("preview").copy(position=GraphPunkt(80f,60f),größe=GraphGröße(700f,650f))
        val editor=KartenEditorZustand(KartenDaten(name="Viewport",knoten=listOf(n)),MathematikKartenLaufzeit().graphPrüfung)
        val r=GeometryNodesVorschauRenderer { KnotenAuswertungsErgebnis(mapOf("geometrie" to BedingterWert(geo))) }
        compose.setContent { MaterialTheme { KnotenKartenEditor(editor,Modifier.size(1200.dp,850.dp),rendererFür={ r },zeigeKnotenInspektor=false) } }
        compose.waitForIdle();val ansicht=editor.karte.ansicht;val vorher=bild()
        compose.onNodeWithTag("geometry-preview").performTouchInput {
            down(0,center-Offset(30f,0f));down(1,center+Offset(30f,0f))
            moveTo(0,center-Offset(70f,0f));moveTo(1,center+Offset(70f,0f))
            moveTo(0,center-Offset(100f,0f));moveTo(1,center+Offset(100f,0f));up(0);up(1)
        }
        compose.waitForIdle();assertNotEquals(vorher,bild())
        compose.runOnIdle { assertEquals(ansicht,editor.karte.ansicht);assertEquals(n.position,editor.karte.knoten.single().position) }
    }
}

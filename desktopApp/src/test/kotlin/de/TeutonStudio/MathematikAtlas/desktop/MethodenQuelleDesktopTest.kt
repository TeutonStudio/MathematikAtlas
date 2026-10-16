package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.*

class MethodenQuelleDesktopTest {
    @get:Rule val compose = createComposeRule()
    @Test fun joinQuelleBietetFesteArgumentanzahlMitValidierungUndUndo() {
        val atlas = DesktopAtlasZustand(DesktopKartenSpeicher(Files.createTempDirectory("methoden-join-ui-")))
        val kandidat = methodenKnotenQuellen().first { it.id == "operator:ZAHL:zahl.addition" }
        val quelle = konfiguriereMethodenQuelle(MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt.Zero), kandidat.knoten)
        atlas.editor.führeAus(de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion.KnotenEinfügen(quelle))
        compose.setContent { MaterialTheme { Column { DesktopMethodenQuelle(atlas.editor.karte.knoten.single(), atlas) } } }
        compose.onNodeWithText("Anzahl Argumente").performTextReplacement("1")
        compose.onNodeWithText("Argumentanzahl übernehmen").assertIsNotEnabled()
        compose.onNodeWithText("Anzahl Argumente").performTextReplacement("4")
        compose.onNodeWithText("Argumentanzahl übernehmen").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(4, methodenQuellKnoten(atlas.editor.karte.knoten.single())!!.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang })
            atlas.editor.rückgängig()
            assertEquals(quelle, atlas.editor.karte.knoten.single())
            atlas.beendeBerechnungen()
        }
    }
    @Test fun sucheAuswahlAbbruchUndIdentitaetBleibenBedienbar() {
        val atlas = DesktopAtlasZustand(DesktopKartenSpeicher(Files.createTempDirectory("methoden-ui-")))
        val quelle = MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt.Zero)
        atlas.editor.führeAus(de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion.KnotenEinfügen(quelle))
        compose.setContent { MaterialTheme { Column { DesktopMethodenQuelle(atlas.editor.karte.knoten.single(), atlas) } } }
        compose.onNodeWithText("Methodenquelle: Identität").performClick()
        compose.onNodeWithText("Methoden suchen").performTextInput("Potenz")
        compose.onNodeWithTag("methoden-quelle:operator:ZAHL:zahl.potenz").performClick()
        compose.onNodeWithText("Methode auswählen").assertIsDisplayed()
        screenshot("/tmp/methodenquelle-desktop-dialog.png")
        compose.onNodeWithText("Abbrechen").performClick()
        compose.runOnIdle { assertNull(methodenQuellKnoten(atlas.editor.karte.knoten.single())) }
        compose.onNodeWithText("Methodenquelle: Identität").performClick()
        compose.onNodeWithText("Methoden suchen").performTextInput("Potenz")
        compose.onNodeWithTag("methoden-quelle:operator:ZAHL:zahl.potenz").performClick()
        compose.onNodeWithText("Methode übernehmen").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertNull(atlas.meldung, "Quellenübernahme: ${atlas.meldung}")
            assertNull(atlas.konfigurationsPlan, "Unerwartete Verlustbestätigung: ${atlas.konfigurationsPlan}")
            val neu = atlas.editor.karte.knoten.single()
            assertNotNull(methodenQuellKnoten(neu))
            assertEquals(quelle.anschlüsse.last().id, neu.anschlüsse.single().id)
            atlas.editor.rückgängig()
            assertNull(methodenQuellKnoten(atlas.editor.karte.knoten.single()))
            atlas.beendeBerechnungen()
        }
    }
    @Test fun dialogBleibtSchmalNiedrigUndMitDoppelterSchriftErreichbar() {
        val quelle = MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt.Zero)
        val einträge = List(50) { i -> DesktopMethodenEintrag("quelle-$i", if (i == 49) "Letzte Quelle" else "Quelle $i", "Knoten", quelle, signatur = "(a,b) → (wert)") }
        val konfiguration = mutableStateOf(Triple(390 to 600, 1f, false))
        var übernommen = 0
        compose.setContent {
            val (größe, schrift, dunkel) = konfiguration.value
            MaterialTheme(colorScheme = if (dunkel) darkColorScheme() else lightColorScheme()) {
                val dichte = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(dichte.density, schrift)) {
                    DesktopMethodenAuswahlDialog(einträge, "quelle-0", { übernommen++ }, {}, Modifier.width(größe.first.dp).height(größe.second.dp))
                }
            }
        }
        for (größe in listOf(390 to 600, 800 to 350)) for (schrift in listOf(1f, 2f)) for (dunkel in listOf(false, true)) {
            compose.runOnIdle { konfiguration.value = Triple(größe, schrift, dunkel) }
            compose.onNodeWithText("Methode auswählen").assertIsDisplayed()
            compose.onNodeWithText("Methoden suchen").performScrollTo().performTextReplacement("Letzte Quelle")
            compose.onNodeWithTag("methoden-quelle:quelle-49").performClick()
            val dialog = compose.onNodeWithTag("methoden-dialog").fetchSemanticsNode().boundsInRoot
            val liste = compose.onNodeWithTag("methoden-quellenliste").fetchSemanticsNode().boundsInRoot
            assertTrue(liste.height >= 40f, "Die Quellenliste braucht sichtbaren Platz: $größe, $schrift, $liste")
            for (tag in listOf("methoden-uebernehmen", "methoden-abbrechen")) {
                val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
                assertTrue(bounds.top >= dialog.top && bounds.bottom <= dialog.bottom + 1f && bounds.left >= dialog.left && bounds.right <= dialog.right + 1f,
                    "Aktion muss vollständig im Dialog liegen: $größe, $schrift, $tag=$bounds, Dialog=$dialog")
                assertTrue(bounds.top >= liste.bottom - 1f, "Der Footer darf die Quellenliste nicht überlagern.")
            }
            compose.onNodeWithText("Methode übernehmen").assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithText("Abbrechen").assertIsDisplayed()
            if (schrift == 2f && dunkel) screenshot("/tmp/methodenquelle-desktop-${größe.first}x${größe.second}-2x-dunkel.png")
        }
        compose.runOnIdle { assertEquals(8, übernommen) }
    }
    @Test fun identitaetsknotenZeigtFormelUndGemeinsameZeitfusszeile() {
        val knoten = MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt(80f, 60f))
        val laufzeit = MathematikKartenLaufzeit()
        fun editor(größe: GraphGröße, zoom: Float, ausgewählt: Boolean) = KartenEditorZustand(
            KartenDaten(name = "Identität", knoten = listOf(knoten.copy(größe = größe)), ansicht = AnsichtsFenster(zoom = zoom)),
            laufzeit.graphPrüfung,
        ).also { if (ausgewählt) it.wähleKnoten(knoten.id) }
        val editorZustand = mutableStateOf(editor(knoten.größe, 1f, false))
        val schrift = mutableStateOf(1f)
        val messung = mutableStateOf<Long?>(null)
        val basisErgebnis = laufzeit.auswerten(editorZustand.value.karte).knoten.getValue(knoten.id)
        compose.setContent {
            MaterialTheme {
                val dichte = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(dichte.density, schrift.value)) {
                    val basis = MathematikKnotenRenderer(ergebnisFür = { basisErgebnis.copy(auswertungsDauerNanos = messung.value) })
                    // Die Messhüllen delegieren vollständig; Mathematik und Fußzeile bleiben im Produktpfad.
                    val beobachtet = object : KnotenRenderer by basis {
                        @Composable override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
                            Box(Modifier.testTag("identitaet-inhalt")) { basis.Inhalt(knoten, ausgewählt, aktionen) }
                        }
                        @Composable override fun Fußzeile(knoten: KnotenDaten, ausgewählt: Boolean) {
                            Box(Modifier.testTag("identitaet-fusszeile")) { basis.Fußzeile(knoten, ausgewählt) }
                        }
                    }
                    KnotenKartenEditor(editorZustand.value, Modifier.size(900.dp, 650.dp).testTag("identitaet-editor"), rendererFür = { beobachtet }, zeigeKnotenInspektor = false)
                }
            }
        }
        var überprüfteZustände = 0
        for ((größenIndex, größe) in listOf(GraphGröße(245f, 125f), GraphGröße(380f, 240f)).withIndex()) {
            for (zoom in listOf(.75f, 1.25f)) for (font in listOf(1f, 1.6f)) for (ausgewählt in listOf(false, true)) {
                compose.runOnIdle {
                    schrift.value = font
                    messung.value = null
                    editorZustand.value = editor(größe, zoom, ausgewählt)
                }
                compose.onAllNodesWithText("Methode", substring = false).onFirst().assertIsDisplayed()
                compose.onAllNodesWithText("Δzeit", substring = true).assertCountEquals(0)
                val ohneZeit = compose.onNodeWithTag("identitaet-inhalt").fetchSemanticsNode().boundsInRoot
                assertTrue(ohneZeit.width > 0f && ohneZeit.height > 0f)
                val extrem = font == 1.6f && ((größenIndex == 0 && zoom == .75f && ausgewählt) || (größenIndex == 1 && zoom == 1.25f && !ausgewählt))
                val bildName = if (größenIndex == 0) "klein-z075-f16-ausgewaehlt" else "gross-z125-f16-normal"
                if (extrem) screenshot("/tmp/methodenquelle-identitaet-$bildName-ohne-zeit.png")
                compose.runOnIdle { messung.value = 1000000L }
                compose.onNodeWithText("Δzeit", substring = true).assertIsDisplayed()
                val inhalt = compose.onNodeWithTag("identitaet-inhalt").fetchSemanticsNode().boundsInRoot
                val footer = compose.onNodeWithTag("identitaet-fusszeile").fetchSemanticsNode().boundsInRoot
                val zeitText = compose.onNodeWithText("Δzeit", substring = true).fetchSemanticsNode().boundsInRoot
                val editorBounds = compose.onNodeWithTag("identitaet-editor").fetchSemanticsNode().boundsInRoot
                val fall = "Größe=$größe, Zoom=$zoom, Schrift=$font, ausgewählt=$ausgewählt"
                assertTrue(footer.width > 0f && footer.height > 0f, "Fußzeile braucht messbare Fläche: $fall")
                assertTrue(inhalt.bottom <= footer.top + 1f, "Inhalt darf Zeitfußzeile nicht überlagern: $fall, $inhalt, $footer")
                assertTrue(zeitText.top >= footer.top - 1f && zeitText.bottom <= footer.bottom + 1f && zeitText.left >= footer.left - 1f && zeitText.right <= footer.right + 1f,
                    "Zeittext muss vollständig in der Fußzeile liegen: $fall, $zeitText, $footer")
                assertTrue(footer.left >= editorBounds.left && footer.right <= editorBounds.right + 1f && footer.bottom <= editorBounds.bottom + 1f,
                    "Fußzeile muss vollständig im sichtbaren Editor liegen: $fall")
                if (extrem) screenshot("/tmp/methodenquelle-identitaet-$bildName-mit-zeit.png")
                überprüfteZustände += 2
            }
        }
        compose.runOnIdle { assertEquals(32, überprüfteZustände) }
    }
    private fun screenshot(pfad: String) {
        val pixel = compose.onAllNodes(isRoot()).onLast().captureToImage().toPixelMap()
        val bild = BufferedImage(pixel.width, pixel.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pixel.height) for (x in 0 until pixel.width) {
            val c = pixel[x, y]
            bild.setRGB(x, y, ((c.alpha * 255).toInt() shl 24) or ((c.red * 255).toInt() shl 16) or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt())
        }
        ImageIO.write(bild, "png", java.io.File(pfad))
    }
}

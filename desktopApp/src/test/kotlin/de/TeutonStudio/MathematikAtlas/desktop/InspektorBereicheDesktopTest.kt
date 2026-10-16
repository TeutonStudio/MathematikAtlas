package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKnoten.*
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class InspektorBereicheDesktopTest {
    @Test fun zellenBehaltenKlappzustandBeiUmordnungUndKnotenwechselOhneModellaenderung() = runDesktopComposeUiTest(width = 450, height = 1000) {
        val a = AnschlussDaten(name = "a", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("test"))
        val b = a.copy(id = neueAnschlussId(), name = "b", reihenfolge = 1)
        val erster = KnotenDaten(art = "test", name = "Erster", anschlüsse = listOf(a, b))
        val zweiter = erster.copy(id = neueKnotenId(), name = "Zweiter")
        val karte = KartenDaten(name = "Test", knoten = listOf(erster, zweiter))
        val auswahl = mutableStateOf(erster)
        val klappen = InspektorKlappZustand()
        setContent { MaterialTheme { Column(Modifier.width(360.dp).verticalScroll(rememberScrollState())) {
            KnotenInspektorBereiche(karte, auswahl.value, klappen, standarddaten = { Text("Standardinhalt") },
                knotendaten = { Text("Fachinhalt") }, eingangsEinstellungen = { Text("Richtungsoption") }) { Text("Inhalt ${it.name}") }
        } } }
        onNodeWithText("Standardinhalt").assertIsDisplayed()
        onNodeWithText("Ausgangsdaten").assertDoesNotExist()
        onNodeWithText("Inhalt a").assertDoesNotExist()
        onNodeWithText("a").performClick()
        onNodeWithText("Inhalt a").assertIsDisplayed()
        onNodeWithText("Gemeinsame Einstellungen").performClick()
        onNodeWithText("Richtungsoption").assertIsDisplayed()
        runOnIdle { auswahl.value = erster.copy(anschlüsse = listOf(b.copy(reihenfolge = 0), a.copy(reihenfolge = 1))) }
        onNodeWithText("Inhalt a").assertIsDisplayed()
        runOnIdle { auswahl.value = zweiter }
        onNodeWithText("Inhalt a").assertDoesNotExist()
        runOnIdle { auswahl.value = erster }
        onNodeWithText("Inhalt a").assertIsDisplayed()
        onNodeWithText("Eingangsdaten").performClick()
        onNodeWithText("Inhalt a").assertDoesNotExist()
        onNodeWithText("Standarddaten").performClick()
        onNodeWithText("Standardinhalt").assertDoesNotExist()
        assertEquals(listOf(erster, zweiter), karte.knoten)
    }

    @Test fun standarddatenValidierenNameUndFarbeUndUebernehmenFormAtomar() = runDesktopComposeUiTest(width = 420, height = 850) {
        val basis = KnotenDaten(art = "test", name = "Notiz")
        val editor = KartenEditorZustand(KartenDaten(name = "Test", knoten = listOf(basis)), GraphPrüfung(AnschlussArtRegister(emptyList())))
        val dunkel = mutableStateOf(false)
        setContent { MaterialTheme(colorScheme = if (dunkel.value) darkColorScheme() else lightColorScheme()) {
            Column(Modifier.width(310.dp).verticalScroll(rememberScrollState())) {
                KnotenStandardDatenEditor(editor.karte.knoten.single(), KnotenDarstellungsForm.entries.toSet(), übernehmen = { editor.führeAus(KartenAktion.KnotenErsetzen(it)) })
            }
        } }
        onNodeWithText("Knotenname").performTextReplacement(" ")
        onNodeWithText("Name übernehmen").assertIsNotEnabled()
        runOnIdle { assertEquals(basis, editor.karte.knoten.single()) }
        onNodeWithText("Knotenname").performTextReplacement("Neue Notiz")
        onNodeWithText("Name übernehmen").performClick()
        runOnIdle { assertEquals("Neue Notiz", editor.karte.knoten.single().name) }
        onNodeWithText("Hintergrundfarbe · #RRGGBB").performScrollTo().performTextReplacement("GG0011")
        onNodeWithText("Hintergrundfarbe übernehmen").assertIsNotEnabled()
        onNodeWithText("Hintergrundfarbe · #RRGGBB").performTextReplacement("123456")
        onNodeWithText("Hintergrundfarbe übernehmen").performScrollTo().performClick()
        onNodeWithText("Rahmenfarbe · #RRGGBB").performScrollTo().performTextReplacement("ABCDEF")
        onNodeWithText("Rahmenfarbe übernehmen").performScrollTo().performClick()
        onNodeWithText("Raute").performScrollTo().performClick()
        runOnIdle {
            val aktuell = editor.karte.knoten.single()
            assertEquals(KnotenDarstellungsForm.Raute, aktuell.darstellungsForm)
            assertEquals(KnotenEigenschaft.Farbe(0xFF123456), aktuell.eigenschaften[KNOTEN_HINTERGRUND_EIGENSCHAFT])
            assertEquals(KnotenEigenschaft.Farbe(0xFFABCDEF), aktuell.eigenschaften[KNOTEN_RAHMEN_EIGENSCHAFT])
            editor.rückgängig(); assertEquals(KnotenDarstellungsForm.Rechteck, editor.karte.knoten.single().darstellungsForm)
            editor.wiederholen(); dunkel.value = true
        }
        onNodeWithText("Hintergrundfarbe automatisch").performScrollTo().performClick()
        runOnIdle {
            assertFalse(KNOTEN_HINTERGRUND_EIGENSCHAFT in editor.karte.knoten.single().eigenschaften)
            assertNotNull(editor.karte.knoten.single().eigenschaften[KNOTEN_RAHMEN_EIGENSCHAFT])
        }
    }

    @Test fun desktopUebersetzerHatZielNurUnterAusgangsdatenUndErhaeltUndo() = runDesktopComposeUiTest(width = 430, height = 950) {
        val atlas = DesktopAtlasZustand(DesktopKartenSpeicher(Files.createTempDirectory("atlas-inspektor-")))
        val basis = ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        runOnIdle { atlas.editor.ersetzeKarte(KartenDaten(name = "Test", knoten = listOf(basis))); atlas.editor.wähleKnoten(basis.id); atlas.aktualisiereAuswertung() }
        setContent { MaterialTheme { DesktopInspektor(atlas, remember { FocusRequester() }, Modifier.width(360.dp).fillMaxHeight()) } }
        onNodeWithText("Übersetzungsziel").assertDoesNotExist()
        onNodeWithText("Standarddaten").performClick()
        onNodeWithText("Ausgangsdaten").performScrollTo().assertIsDisplayed()
        onNodeWithText("Gemeinsame Einstellungen").performScrollTo().performClick()
        onNodeWithText("Übersetzungsziel").assertIsDisplayed()
        onNodeWithText("Polarkoordinaten").performScrollTo().performClick()
        runOnIdle {
            val neu = atlas.editor.karte.knoten.single()
            assertEquals("polar", übersetzerZiel(neu))
            assertEquals(basis.anschlüsse.map { it.id }, neu.anschlüsse.map { it.id })
            atlas.editor.rückgängig(); atlas.aktualisiereAuswertung()
            assertEquals(übersetzerZiel(basis), übersetzerZiel(atlas.editor.karte.knoten.single()))
            atlas.editor.wiederholen(); atlas.aktualisiereAuswertung()
        }
        onNodeWithText("Gemeinsame Einstellungen").performScrollTo().performClick()
        onNodeWithText("Übersetzungsziel").assertDoesNotExist()
        speichereBild(onRoot(), "/tmp/atlas-inspektor-desktop.png")
        runOnIdle { atlas.beendeBerechnungen() }
    }

    @Test fun schmalerInspectorMitGrosserSchriftHaeltFelderUndAktionenErreichbar() = runDesktopComposeUiTest(width = 320, height = 480) {
        val atlas = DesktopAtlasZustand(DesktopKartenSpeicher(Files.createTempDirectory("atlas-inspektor-schmal-")))
        val note = KnotenDaten(art = "karte.notiz", name = "Notiz", parameter = mapOf("text" to "Hinweis"))
        runOnIdle { atlas.editor.ersetzeKarte(KartenDaten(name = "Test", knoten = listOf(note))); atlas.editor.wähleKnoten(note.id); atlas.aktualisiereAuswertung() }
        setContent { MaterialTheme(colorScheme = darkColorScheme()) {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                DesktopInspektor(atlas, remember { FocusRequester() }, Modifier.width(300.dp).fillMaxHeight())
            }
        } }
        onNodeWithText("Raute").performScrollTo().performClick()
        onNodeWithText("Standarddaten").performScrollTo().performClick()
        onNodeWithText("Eingangsdaten").assertDoesNotExist()
        onNodeWithText("Ausgangsdaten").assertDoesNotExist()
        onNodeWithText("Schriftgröße (sp)").performScrollTo().assertIsDisplayed()
        onNodeWithText("Duplizieren").assertIsDisplayed()
        onNodeWithText("Löschen").assertIsDisplayed()
        runOnIdle {
            assertEquals(KnotenDarstellungsForm.Raute, atlas.editor.karte.knoten.single().darstellungsForm)
            assertNull(atlas.auswertung.knoten[note.id]?.fehler)
            atlas.beendeBerechnungen()
        }
    }

    @Test fun formwechselAktualisiertTouchUndRechtsklickOhneIdOderGroessenaenderung() = runDesktopComposeUiTest(width = 1200, height = 950) {
        val basis = KnotenDaten(art = "karte.notiz", name = "Wechsel", position = GraphPunkt(100f, 100f), größe = GraphGröße(700f, 650f))
        val z = KartenEditorZustand(KartenDaten(name = "Wechsel", knoten = listOf(basis)), GraphPrüfung(AnschlussArtRegister(emptyList())))
        var kontexte = 0
        setContent { MaterialTheme {
            KnotenKartenEditor(z, Modifier.size(1200.dp, 950.dp), rendererFür = { DesktopNotizRenderer }, zeigeKnotenInspektor = false,
                beiKnotenKontext = { kontexte++ })
        } }
        fun körper() = onNodeWithContentDescription("Knoten Wechsel, 0 Anschlüsse")
        körper().performMouseInput { moveTo(Offset(30f, 30f)); click(position = Offset(30f, 30f), button = MouseButton.Secondary); exit() }
        runOnIdle { assertEquals(1, kontexte); z.wähleKnoten(null)
            z.führeAus(KartenAktion.KnotenErsetzen(basis.mitDarstellungsForm(KnotenDarstellungsForm.Ellipse, KnotenDarstellungsForm.entries.toSet()))) }
        körper().performMouseInput { moveTo(Offset(30f, 30f)); advanceEventTime(400); click(position = Offset(30f, 30f), button = MouseButton.Secondary); exit() }
        runOnIdle { assertEquals(1, kontexte); assertNull(z.ausgewählterKnoten) }
        körper().performTouchInput { click(Offset(30f, 30f)) }
        mainClock.advanceTimeBy(400)
        runOnIdle { assertNull(z.ausgewählterKnoten)
            z.führeAus(KartenAktion.KnotenErsetzen(basis)) }
        körper().performTouchInput { click(Offset(30f, 30f)) }
        mainClock.advanceTimeBy(400)
        runOnIdle { assertEquals(basis.id, z.ausgewählterKnoten) }
        onNodeWithContentDescription("Knoten Wechsel vergrößern").performMouseInput {
            moveTo(center); press(); moveBy(Offset(30f, 25f), delayMillis = 300); release(); exit()
        }
        runOnIdle { assertTrue(z.karte.knoten.single().größe.breite > basis.größe.breite)
            z.rückgängig(); assertEquals(basis.größe, z.karte.knoten.single().größe) }
    }

    @Test fun formenHabenLesbarenInhaltUndGleicheTrefferBeiTouchMausZoomUndSchrift() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        fun editor(form: KnotenDarstellungsForm, zoom: Float): KartenEditorZustand {
            val note = KnotenDaten(art = "karte.notiz", name = "Formnotiz", position = GraphPunkt(140f, 200f), größe = GraphGröße(560f, 450f),
                parameter = mapOf("text" to "Lesbarer Inhalt"), eigenschaften = mapOf(KNOTEN_FORM_EIGENSCHAFT to KnotenEigenschaft.Text(form.schlüssel)))
            return KartenEditorZustand(KartenDaten(name = "Form", knoten = listOf(note), ansicht = AnsichtsFenster(zoom = zoom)), GraphPrüfung(AnschlussArtRegister(emptyList())))
        }
        val z = mutableStateOf(editor(KnotenDarstellungsForm.Rechteck, 1f))
        val schrift = mutableStateOf(1f)
        val dunkel = mutableStateOf(false)
        setContent { MaterialTheme(colorScheme = if (dunkel.value) darkColorScheme() else lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides Density(1f, schrift.value)) {
                KnotenKartenEditor(z.value, Modifier.size(1600.dp, 1100.dp), rendererFür = { DesktopNotizRenderer }, zeigeKnotenInspektor = false)
            }
        } }
        for (form in KnotenDarstellungsForm.entries) for (font in listOf(1f, 2f)) {
            runOnIdle { schrift.value = font; dunkel.value = font == 2f; z.value = editor(form, if (font == 2f) .7f else 1f) }
            waitForIdle()
            val knoten = z.value.karte.knoten.single()
            val körper = onNodeWithContentDescription("Knoten Formnotiz, 0 Anschlüsse")
            if (form != KnotenDarstellungsForm.Rechteck) {
                körper.performTouchInput { click(Offset(2f, 2f)) }
                runOnIdle { assertNull(z.value.ausgewählterKnoten, form.toString()) }
            }
            körper.performMouseInput { moveTo(center); advanceEventTime(400); click(); exit() }
            mainClock.advanceTimeBy(400)
            runOnIdle { assertEquals(knoten.id, z.value.ausgewählterKnoten, "$form $font") }
            val layouts = mutableListOf<TextLayoutResult>()
            onNodeWithText("Lesbarer Inhalt", useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.all { !it.didOverflowHeight && !it.didOverflowWidth }, "$form $font $layouts")
            onNodeWithContentDescription("Knoten Formnotiz vergrößern").assertExists()
            if (font == 2f) speichereBild(onRoot(), "/tmp/atlas-notiz-${form.schlüssel}-desktop.png")
        }
        speichereBild(onRoot(), "/tmp/atlas-notiz-form-desktop.png")
    }

    private fun speichereBild(node: SemanticsNodeInteraction, pfad: String) {
        val pixels = node.captureToImage().toPixelMap()
        val bild = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val c = pixels[x, y]
            bild.setRGB(x, y, ((c.alpha * 255).toInt() shl 24) or ((c.red * 255).toInt() shl 16) or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt())
        }
        ImageIO.write(bild, "png", File(pfad))
    }
}

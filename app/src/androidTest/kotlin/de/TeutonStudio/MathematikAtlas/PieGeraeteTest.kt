package de.TeutonStudio.MathematikAtlas

import android.content.Context
import android.content.ContextWrapper
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import de.TeutonStudio.KnotenKartenVerwalter.zustand.AuswahlModus
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikKnoten.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/** Native Android-Stiftereignisse im produktiven Compose-Hitpfad, ohne echte Kartendateien. */
class PieGeraeteTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var atlas: AtlasZustand
    private lateinit var view: View
    private lateinit var layout: PieLayout
    private var downZeit = 0L

    private fun szene() {
        compose.setContent {
            val context = LocalContext.current
            atlas = remember { AtlasZustand(PieTestContext(context)).also {
                it.editor.ersetzeKarte(KartenDaten(name = "Pie-Gerätetest"))
            } }
            view = LocalView.current
            MaterialTheme {
                layout = erinnerePieLayout(atlas)
                Box(Modifier.fillMaxSize().testTag("piehost").pieEingaben(atlas, layout, true)) {
                    KnotenKartenEditor(atlas.editor, rendererFür = atlas::rendererFür, interaktionGesperrt = atlas.pie.aktiv,
                        beiVerbindungsEinfügeAnfrage = { kontext, punkt ->
                            val ansicht = atlas.editor.darstellungsKarte.ansicht
                            val dichte = view.resources.displayMetrics.density
                            atlas.pie.verbindungÖffnen(kontext, Offset(ansicht.verschiebung.x + punkt.x * ansicht.zoom * dichte,
                                ansicht.verschiebung.y + punkt.y * ansicht.zoom * dichte))
                        })
                    PieMenü(atlas, layout)
                }
            }
        }
    }

    private fun stift(action: Int, punkt: Offset, button: Int = 0, id: Int = 17) {
        compose.runOnIdle {
            val zeit = android.os.SystemClock.uptimeMillis()
            if (action == MotionEvent.ACTION_DOWN) downZeit = zeit
            val props = MotionEvent.PointerProperties().apply { this.id = id; toolType = MotionEvent.TOOL_TYPE_STYLUS }
            val coords = MotionEvent.PointerCoords().apply { x = punkt.x; y = punkt.y; pressure = if (action == MotionEvent.ACTION_UP) 0f else 1f }
            val event = MotionEvent.obtain(downZeit, zeit, action, 1, arrayOf(props), arrayOf(coords), 0, button,
                1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0)
            view.dispatchTouchEvent(event); event.recycle()
        }
        compose.waitForIdle()
    }

    private fun prüfeFreieKartenNavigation() {
        val vorher = compose.runOnIdle { atlas.editor.karte.ansicht }
        compose.onNodeWithTag("piehost").performTouchInput { swipe(center, center + Offset(100f, 60f)) }
        compose.runOnIdle {
            assertNotEquals(vorher.verschiebung, atlas.editor.karte.ansicht.verschiebung)
            assertEquals(vorher.zoom, atlas.editor.karte.ansicht.zoom, .001f)
        }
        val zoomVorher = compose.runOnIdle { atlas.editor.karte.ansicht.zoom }
        compose.onNodeWithTag("piehost").performTouchInput {
            val mitte = center
            down(0, mitte - Offset(60f, 0f)); down(1, mitte + Offset(60f, 0f))
            moveTo(0, mitte - Offset(90f, 0f)); moveTo(1, mitte + Offset(90f, 0f))
            moveTo(0, mitte - Offset(120f, 0f)); moveTo(1, mitte + Offset(120f, 0f))
            up(0); up(1)
        }
        compose.runOnIdle { assertTrue(atlas.editor.karte.ansicht.zoom > zoomVorher) }
    }

    private fun startKnoten(richtung: AnschlussRichtung = AnschlussRichtung.Ausgang,
        art: AnschlussArtId = MathematikAnschlussArten.Zahl.id): KnotenDaten = KnotenDaten(
        art = "test", name = "Start", position = GraphPunkt(24f, 80f), größe = GraphGröße(120f, 80f),
        anschlüsse = listOf(AnschlussDaten(name = "start", richtung = richtung, art = art,
            kante = if (richtung == AnschlussRichtung.Ausgang) AnschlussKante.Rechts else AnschlussKante.Links)))

    private fun öffneAnschlussPie(start: KnotenDaten) {
        compose.runOnIdle {
            atlas.editor.ersetzeKarte(KartenDaten(name = "Anschluss", knoten = listOf(start)))
            atlas.editor.beginneVerbindung(AnschlussVerweis(start.id, start.anschlüsse.single().id))
            val kontext = requireNotNull(atlas.editor.übergibVerbindungZurEinfügung())
            atlas.pie.verbindungÖffnen(kontext, Offset(500f, 500f))
        }
        compose.waitUntil(timeoutMillis = 60_000) { !atlas.pie.lädt }
    }

    @Test fun seitentasteImAnschlussdragOeffnetErstBeimAnhebenGefilterteKreise() {
        szene()
        for (button in listOf(MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY)) {
            val start = startKnoten()
            compose.runOnIdle { atlas.editor.ersetzeKarte(KartenDaten(name = "Stift", knoten = listOf(start))) }
            val griff = compose.onNode(hasContentDescription("Start, Anschluss start", substring = true)).fetchSemanticsNode().boundsInRoot.center
            val host = compose.onNodeWithTag("piehost").fetchSemanticsNode().boundsInRoot
            val ziel = Offset(minOf(griff.x + 120f, host.right - 24f), minOf(griff.y + 160f, host.bottom - 24f))
            stift(MotionEvent.ACTION_DOWN, griff)
            stift(MotionEvent.ACTION_MOVE, griff + Offset(50f, 30f), button)
            compose.runOnIdle { assertEquals(PieEbene.Geschlossen, atlas.pie.ebene) }
            stift(MotionEvent.ACTION_MOVE, ziel)
            stift(MotionEvent.ACTION_UP, ziel)
            compose.waitUntil(timeoutMillis = 60_000) { !atlas.pie.lädt }
            compose.runOnIdle {
                assertEquals(PieEbene.Fächer, atlas.pie.ebene)
                assertTrue(atlas.pie.anschlussbezogen)
                assertEquals(ziel.x, atlas.pie.anker.x, 2f); assertEquals(ziel.y, atlas.pie.anker.y, 2f)
                assertTrue(atlas.pie.einträge.isNotEmpty())
                assertTrue(atlas.editor.karte.verbindungen.isEmpty()); assertEquals(1, atlas.editor.karte.knoten.size)
                assertFalse(atlas.editor.kannRückgängig())
                atlas.pie.schließen()
            }
        }
        compose.runOnIdle { atlas.beendeBerechnungen() }
    }

    @Test fun mehrereAnschluesseWerdenAusgewaehltUndAtomarVerbunden() {
        szene(); val start = startKnoten(); öffneAnschlussPie(start)
        val eintrag = compose.runOnIdle { atlas.pie.einträge.first { e ->
            e.vorlage.art == ZAHLENRECHNER_ART && e.vorlage.standardParameter[ZAHLENRECHNER_OPERATOR] == "zahl.potenz"
        } }
        compose.runOnIdle {
            atlas.pie.definitionÖffnen(eintrag); assertEquals(PieEbene.Definition, atlas.pie.ebene)
            atlas.pie.zurück(); assertEquals(PieEbene.Fächer, atlas.pie.ebene)
            atlas.pie.beginneZiehen(eintrag, Offset(600f, 500f), Offset(20f, 20f), view.resources.displayMetrics.density)
            atlas.pie.ablegen()
            assertEquals(PieEbene.Anschluss, atlas.pie.ebene)
            assertTrue(atlas.editor.interaktionAktiv); assertFalse(atlas.editor.kannRückgängig())
        }
        compose.onNodeWithText("Anschluss auswählen").assertIsDisplayed()
        val portName = compose.runOnIdle {
            val ref = atlas.pie.anschlussTreffer.last().anschluss
            atlas.editor.karte.knoten.first { it.id == ref.knotenId }.anschlüsse.first { it.id == ref.anschlussId }.name
        }
        compose.onNodeWithText(portName).performClick()
        compose.onNodeWithText("Verbinden").performClick()
        compose.runOnIdle {
            assertEquals(PieEbene.Geschlossen, atlas.pie.ebene)
            assertEquals(2, atlas.editor.karte.knoten.size); assertEquals(1, atlas.editor.karte.verbindungen.size)
            val edge = atlas.editor.karte.verbindungen.single()
            val neu = atlas.editor.karte.knoten.first { it.id != start.id }
            assertEquals(portName, neu.anschlüsse.first { it.id == edge.zu.anschlussId }.name)
            atlas.editor.rückgängig(); assertEquals(listOf(start), atlas.editor.karte.knoten); assertTrue(atlas.editor.karte.verbindungen.isEmpty())
            atlas.editor.wiederholen(); assertEquals(1, atlas.editor.karte.verbindungen.size)
            atlas.beendeBerechnungen()
        }
    }

    @Test fun dialogabbruchErhaeltAlteKanteUndGruppenOderVerbindungsAuswahl() {
        szene()
        for (gruppeWählen in listOf(false, true)) {
            val start = startKnoten(); val ziel = startKnoten(AnschlussRichtung.Eingang).copy(name = "Altziel")
            val edge = VerbindungDaten(von = AnschlussVerweis(start.id, start.anschlüsse.single().id), zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.single().id))
            val gruppe = VisuelleKnotenGruppeDaten(knotenIds = setOf(start.id, ziel.id), position = GraphPunkt.Zero, größe = GraphGröße(800f, 600f))
            compose.runOnIdle {
                atlas.editor.ersetzeKarte(KartenDaten(name = "Abbruch", knoten = listOf(start, ziel), verbindungen = listOf(edge), visuelleGruppen = listOf(gruppe)))
                atlas.editor.beginneVerbindung(edge.zu)
                atlas.pie.verbindungÖffnen(requireNotNull(atlas.editor.übergibVerbindungZurEinfügung()), Offset(500f, 500f))
                if (gruppeWählen) atlas.editor.wähleGruppe(gruppe.id) else atlas.editor.wähleVerbindung(edge.id)
            }
            compose.waitUntil(timeoutMillis = 60_000) { !atlas.pie.lädt }
            compose.runOnIdle {
                val e = atlas.pie.einträge.first { it.vorlage.art == ZAHLENRECHNER_ART && it.vorlage.standardParameter[ZAHLENRECHNER_OPERATOR] == "zahl.potenz" }
                atlas.pie.beginneZiehen(e, Offset(600f, 500f), Offset.Zero, 1f); atlas.pie.ablegen()
                assertEquals(PieEbene.Anschluss, atlas.pie.ebene)
            }
            compose.onNodeWithText("Abbrechen").performClick()
            compose.runOnIdle {
                assertEquals(PieEbene.Fächer, atlas.pie.ebene)
                assertEquals(listOf(edge), atlas.editor.karte.verbindungen); assertEquals(2, atlas.editor.karte.knoten.size)
                if (gruppeWählen) assertEquals(gruppe.id, atlas.editor.ausgewählteGruppe) else assertEquals(edge.id, atlas.editor.ausgewählteVerbindung)
                assertFalse(atlas.editor.kannRückgängig()); atlas.pie.schließen()
            }
        }
        compose.runOnIdle { atlas.beendeBerechnungen() }
    }

    @Test fun leererAnschlussfilterZeigtLeermeldungUndSchliessenIstVerlustfrei() {
        szene(); val start = startKnoten(art = AnschlussArtId("test.unbekannterTyp")); öffneAnschlussPie(start)
        compose.onNodeWithText("Keine passenden Knoten gefunden").assertIsDisplayed()
        compose.onNodeWithText("Schließen").performClick()
        compose.runOnIdle {
            assertEquals(PieEbene.Geschlossen, atlas.pie.ebene)
            assertEquals(listOf(start), atlas.editor.karte.knoten)
            assertFalse(atlas.editor.kannRückgängig()); atlas.beendeBerechnungen()
        }
    }

    @Test fun geschlossenePieSitzungLaesstVerschiebenUndPinchDurch() {
        szene()
        prüfeFreieKartenNavigation()
        compose.runOnIdle { atlas.beendeBerechnungen() }
    }

    @Test fun navigationIstNachSchliessenAbbruchUndKnotenablageWiederFrei() {
        szene()
        repeat(3) { fall ->
            compose.runOnIdle {
                atlas.pie.schließen()
                atlas.editor.ersetzeKarte(KartenDaten(name = "Navigation-$fall"))
                atlas.pie.öffnen(Offset(250f, 250f))
                if (fall == 0) atlas.pie.schließen()
                else {
                    val index = KonzeptBibliothekRegister.kategorien.indexOfFirst { atlas.pie.für(it).isNotEmpty() }
                    atlas.pie.wählen(index)
                    atlas.pie.beginneZiehen(atlas.pie.einträge.first(), Offset(10f, 10f), Offset.Zero, 1f)
                    if (fall == 1) atlas.pie.schließen() else atlas.pie.ablegen()
                }
                assertFalse(atlas.pie.aktiv)
            }
            compose.waitForIdle()
            prüfeFreieKartenNavigation()
        }
        compose.runOnIdle { atlas.beendeBerechnungen() }
    }

    @Test fun zweitesMenueBleibtAmTipppunktUndPinchTransformiertNurDieAuswahl() {
        szene()
        val mitte = compose.onNodeWithTag("piehost").fetchSemanticsNode().boundsInRoot.center
        val ansicht = compose.runOnIdle {
            atlas.pie.öffnen(mitte)
            val index = KonzeptBibliothekRegister.kategorien.indexOfFirst { atlas.pie.für(it).isNotEmpty() }
            atlas.pie.wählen(index)
            assertEquals(mitte, atlas.pie.position)
            atlas.editor.karte.ansicht
        }
        compose.onNodeWithTag("piehost").performTouchInput {
            down(0, center - Offset(60f, 0f)); down(1, center + Offset(60f, 0f))
            moveTo(0, center - Offset(90f, 0f)); moveTo(1, center + Offset(90f, 0f))
            moveTo(0, center - Offset(120f, 0f)); moveTo(1, center + Offset(120f, 0f))
            up(0)
            // Ein verbleibender Finger setzt die Menügeste fort und fügt nichts ein.
            moveBy(1, Offset(30f, 20f)); up(1)
        }
        compose.runOnIdle {
            assertEquals(PieEbene.Fächer, atlas.pie.ebene)
            assertTrue(atlas.pie.zoom > 1f)
            assertEquals(ansicht, atlas.editor.karte.ansicht)
            assertTrue(atlas.editor.karte.knoten.isEmpty())
            assertTrue(atlas.pie.position.x.isFinite()); assertTrue(atlas.pie.position.y.isFinite())
            atlas.pie.zurück()
            assertEquals(PieEbene.Kategorien, atlas.pie.ebene)
            assertEquals(mitte, atlas.pie.position)
            assertEquals(1f, atlas.pie.zoom, .001f)
            atlas.beendeBerechnungen()
        }
    }

    @Test fun mausradZoomtDieAuswahlAberNichtWaehrendEinesGedruecktenZeigers() {
        szene()
        val mitte = compose.onNodeWithTag("piehost").fetchSemanticsNode().boundsInRoot.center
        val kamera = compose.runOnIdle {
            atlas.pie.öffnen(mitte)
            val index = KonzeptBibliothekRegister.kategorien.indexOfFirst { atlas.pie.für(it).isNotEmpty() }
            atlas.pie.wählen(index)
            atlas.editor.karte.ansicht
        }
        compose.onNodeWithTag("piehost").performMouseInput { moveTo(center); scroll(-2f) }
        val zoom = compose.runOnIdle {
            assertTrue(atlas.pie.zoom > 1f)
            assertEquals(kamera, atlas.editor.karte.ansicht)
            atlas.pie.zoom
        }
        compose.onNodeWithTag("piehost").performMouseInput { press(); scroll(-2f) }
        compose.runOnIdle {
            assertEquals(zoom, atlas.pie.zoom, .001f)
            assertEquals(kamera, atlas.editor.karte.ansicht)
        }
        compose.onNodeWithTag("piehost").performMouseInput { release(); exit() }
        compose.runOnIdle { atlas.beendeBerechnungen() }
    }

    @Test fun herausziehenNachMenueZoomBehaeltDenLokalenGriffUndUndo() {
        szene()
        val bounds = compose.onNodeWithTag("piehost").fetchSemanticsNode().boundsInRoot
        val ziel = bounds.center
        lateinit var größe: GraphGröße
        val kamera = compose.runOnIdle {
            atlas.pie.öffnen(ziel)
            val index = KonzeptBibliothekRegister.kategorien.indexOfFirst { atlas.pie.für(it).isNotEmpty() }
            atlas.pie.wählen(index)
            atlas.pie.transformieren(ziel, Offset.Zero, .5f)
            atlas.editor.karte.ansicht
        }
        compose.waitForIdle()
        compose.runOnIdle {
            größe = GraphGröße(layout.plätze.first().rahmen.width, layout.plätze.first().rahmen.height)
            atlas.pie.verschieben(ziel - (atlas.pie.position + layout.plätze.first().rahmen.center))
        }
        compose.waitForIdle()
        stift(MotionEvent.ACTION_DOWN, ziel)
        stift(MotionEvent.ACTION_MOVE, ziel + Offset(50f, 30f))
        compose.runOnIdle {
            assertEquals(PieEbene.Ziehen, atlas.pie.ebene)
            val dichte = view.resources.displayMetrics.density
            val k = atlas.editor.karte.knoten.single()
            val griff = GraphPunkt(größe.breite / (2f * dichte * kamera.zoom * .5f), größe.höhe / (2f * dichte * kamera.zoom * .5f))
            val erwartet = pieWeltPosition(ziel + Offset(50f, 30f), griff, kamera, dichte)
            assertEquals(erwartet.x, k.position.x, .05f); assertEquals(erwartet.y, k.position.y, .05f)
            assertEquals(kamera, atlas.editor.karte.ansicht)
        }
        stift(MotionEvent.ACTION_UP, ziel + Offset(50f, 30f))
        compose.runOnIdle {
            assertEquals(PieEbene.Geschlossen, atlas.pie.ebene)
            atlas.editor.rückgängig(); assertTrue(atlas.editor.karte.knoten.isEmpty())
            atlas.editor.wiederholen(); assertEquals(1, atlas.editor.karte.knoten.size)
            atlas.beendeBerechnungen()
        }
    }

    @Test fun seitentastenOeffnenExaktAmTippUndNormalerStiftNicht() {
        szene()
        val punkt = Offset(90f, 210f)
        stift(MotionEvent.ACTION_DOWN, punkt); stift(MotionEvent.ACTION_UP, punkt)
        compose.runOnIdle { assertEquals(PieEbene.Geschlossen, atlas.pie.ebene) }
        for (button in listOf(MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY)) {
            stift(MotionEvent.ACTION_DOWN, punkt, button); stift(MotionEvent.ACTION_UP, punkt)
            compose.runOnIdle {
                assertEquals(PieEbene.Kategorien, atlas.pie.ebene)
                assertEquals(punkt, atlas.pie.anker)
                assertTrue(atlas.editor.karte.knoten.isEmpty()); assertFalse(atlas.editor.kannRückgängig())
                atlas.pie.schließen()
            }
        }
    }

    @Test fun derselbeStiftZiehtVorschauOhneNeuesAufsetzenUndUndoEntferntSie() {
        szene()
        val bounds = compose.onNodeWithTag("piehost").fetchSemanticsNode().boundsInRoot
        val ziel = Offset(bounds.width / 2f, bounds.height / 2f)
        stift(MotionEvent.ACTION_DOWN, ziel, MotionEvent.BUTTON_STYLUS_PRIMARY); stift(MotionEvent.ACTION_UP, ziel)
        compose.runOnIdle {
            val index = KonzeptBibliothekRegister.kategorien.indexOfFirst { atlas.pie.für(it).isNotEmpty() && it.id != "eigene-karten" }
            atlas.pie.wählen(index)
        }
        compose.waitForIdle()
        compose.runOnIdle { atlas.pie.verschieben(ziel - (atlas.pie.position + layout.plätze.first().rahmen.center)) }
        compose.waitForIdle()
        stift(MotionEvent.ACTION_DOWN, ziel)
        stift(MotionEvent.ACTION_MOVE, ziel + Offset(50f, 30f))
        compose.runOnIdle {
            assertEquals(PieEbene.Ziehen, atlas.pie.ebene)
            assertEquals(1, atlas.editor.karte.knoten.size)
            assertFalse(atlas.pie.sichtbar); assertTrue(atlas.editor.interaktionAktiv)
        }
        stift(MotionEvent.ACTION_MOVE, ziel + Offset(90f, 60f))
        stift(MotionEvent.ACTION_UP, ziel + Offset(90f, 60f))
        compose.runOnIdle {
            assertEquals(PieEbene.Geschlossen, atlas.pie.ebene)
            assertEquals(1, atlas.editor.karte.knoten.size)
            assertFalse(atlas.editor.interaktionAktiv)
            atlas.editor.rückgängig(); assertTrue(atlas.editor.karte.knoten.isEmpty())
            assertFalse(atlas.editor.kannRückgängig()); atlas.beendeBerechnungen()
        }
    }

    @Test fun abbrechenStelltAuswahlWiederHerUndSpaetereFremdeTransaktionBleibtErhalten() {
        szene()
        compose.runOnIdle {
            val alt = statischeKnotenVorlagen().first().erzeuge(GraphPunkt.Zero)
            atlas.editor.ersetzeKarte(KartenDaten(name = "Abbruch", knoten = listOf(alt)))
            atlas.editor.setzeAuswahlModus(AuswahlModus.Gruppe); atlas.editor.wähleKnoten(alt.id)
            atlas.pie.öffnen(Offset.Zero)
            val k = KonzeptBibliothekRegister.kategorien.first { atlas.pie.für(it).isNotEmpty() }
            val e = atlas.pie.für(k).first()
            atlas.pie.beginneZiehen(e, Offset(300f, 300f), Offset(20f, 20f), 1f)
            assertEquals(1, atlas.editor.ausgewählteKnoten.size)
            atlas.pie.schließen()
            assertEquals(listOf(alt), atlas.editor.karte.knoten); assertEquals(setOf(alt.id), atlas.editor.ausgewählteKnoten)
            atlas.pie.öffnen(Offset.Zero)
            atlas.pie.beginneZiehen(e, Offset(300f, 300f), Offset(20f, 20f), 1f); atlas.pie.ablegen()
            atlas.editor.beginneKnotenZiehen(alt.id)
            atlas.pie.schließen()
            assertTrue(atlas.editor.interaktionAktiv)
            atlas.editor.verwerfeKnotenZiehen(); atlas.beendeBerechnungen()
        }
    }
}

private class PieTestContext(context: Context) : ContextWrapper(context) {
    private val basis = java.io.File(context.cacheDir, "pie-test-${java.util.UUID.randomUUID()}")
    override fun getFilesDir() = java.io.File(basis, "files").apply { mkdirs() }
    override fun getNoBackupFilesDir() = java.io.File(basis, "no-backup").apply { mkdirs() }
}

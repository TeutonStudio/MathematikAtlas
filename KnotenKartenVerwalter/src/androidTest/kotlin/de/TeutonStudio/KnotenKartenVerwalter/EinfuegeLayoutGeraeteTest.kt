package de.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EinfuegeLayoutGeraeteTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ablegenSchafftPlatzUndUndoStelltDenGesamtenStandWiederHer() {
        val art = AnschlussArtId("test")
        fun knoten(name: String, x: Float, y: Float) = KnotenDaten(art = "test", name = name,
            position = GraphPunkt(x, y), größe = GraphGröße(220f, 120f), anschlüsse = listOf(
                AnschlussDaten(name = "in", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art),
                AnschlussDaten(name = "out", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)))
        val a = knoten("A", 60f, 150f); val b = knoten("B", 500f, 150f); val c = knoten("C", 280f, 340f)
        val basis = KartenDaten(name = "Gerät", knoten = listOf(a, b, c), ansicht = AnsichtsFenster(zoom = .5f),
            verbindungen = listOf(VerbindungDaten(von = AnschlussVerweis(a.id, a.anschlüsse[1].id), zu = AnschlussVerweis(b.id, b.anschlüsse[0].id))))
        val editor = KartenEditorZustand(basis, GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(art, "Test")))))
        val schrift = mutableStateOf(1f)
        val dunkel = mutableStateOf(false)
        compose.setContent { MaterialTheme(colorScheme = if (dunkel.value) darkColorScheme() else lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides Density(1f, schrift.value)) {
            KnotenKartenEditor(editor, Modifier.size(800.dp, 600.dp), zeigeKnotenInspektor = false)
        } } }
        for (font in listOf(1f, 2f)) for (zoom in listOf(.5f, 1f)) for (dark in listOf(false, true)) {
            val vorher = basis.copy(ansicht = AnsichtsFenster(zoom = zoom))
            compose.runOnIdle { schrift.value = font; dunkel.value = dark; editor.ersetzeKarte(vorher) }
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Knoten C, 2 Anschlüsse").performTouchInput {
                down(Offset(25f, 15f)); moveBy(Offset(0f, -190f * zoom), delayMillis = 300)
            }
            compose.waitForIdle()
            val abgelegt = editor.karte.knoten.single { it.id == c.id }.position
            compose.onNodeWithContentDescription("Knoten C, 2 Anschlüsse").performTouchInput { up() }
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(2, editor.karte.verbindungen.size)
                val links = editor.darstellungsKarte.knoten.single { it.id == a.id }
                val mitte = editor.darstellungsKarte.knoten.single { it.id == c.id }
                val rechts = editor.darstellungsKarte.knoten.single { it.id == b.id }
                assertTrue(links.position.x + links.größe.breite + 31.9f <= mitte.position.x)
                assertTrue(mitte.position.x + mitte.größe.breite + 31.9f <= rechts.position.x)
                assertEquals(abgelegt, editor.karte.knoten.single { it.id == c.id }.position)
                val nachher = editor.karte
                editor.rückgängig(); assertEquals(vorher, editor.karte); assertFalse(editor.kannRückgängig())
                editor.wiederholen(); assertEquals(nachher, editor.karte)
            }
        }
    }
}

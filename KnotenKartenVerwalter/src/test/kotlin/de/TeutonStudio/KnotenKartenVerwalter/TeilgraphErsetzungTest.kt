package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TeilgraphErsetzungTest {
    private val art = AnschlussArt(AnschlussArtId("test.wert"), "Wert")
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(art)))

    @Test
    fun `teilgraphanalyse trennt innere und grenzverbindungen`() {
        val quelle = knoten("Quelle", ausgang = true)
        val mitteA = knoten("A", eingang = true, ausgang = true)
        val mitteB = knoten("B", eingang = true, ausgang = true)
        val ziel = knoten("Ziel", eingang = true)
        val verbindungen = listOf(
            verbinde(quelle, mitteA),
            verbinde(mitteA, mitteB),
            verbinde(mitteB, ziel),
        )
        val karte = KartenDaten(name = "Grenzen", knoten = listOf(quelle, mitteA, mitteB, ziel), verbindungen = verbindungen)

        val grenzen = karte.analysiereTeilgraph(setOf(mitteA.id, mitteB.id))

        assertEquals(listOf(verbindungen[1]), grenzen.innereVerbindungen)
        assertEquals(listOf(verbindungen[0]), grenzen.eingänge.map { it.verbindung })
        assertEquals(listOf(verbindungen[2]), grenzen.ausgänge.map { it.verbindung })
    }

    @Test
    fun `atomare ersetzung ist ein undo schritt und bewahrt grenz ids`() {
        val quelle = knoten("Quelle", ausgang = true)
        val alt = knoten("Alt", eingang = true, ausgang = true)
        val ziel = knoten("Ziel", eingang = true)
        val eingang = verbinde(quelle, alt)
        val ausgang = verbinde(alt, ziel)
        val vorher = KartenDaten(name = "Ersetzung", knoten = listOf(quelle, alt, ziel), verbindungen = listOf(eingang, ausgang))
        val neu = knoten("Neu", eingang = true, ausgang = true)
        val plan = TeilgraphErsetzungsPlan.aus(
            karte = vorher,
            entfernteKnoten = setOf(alt.id),
            neueKnoten = listOf(neu),
            neueVerbindungen = listOf(
                eingang.copy(zu = ref(neu, AnschlussRichtung.Eingang)),
                ausgang.copy(von = ref(neu, AnschlussRichtung.Ausgang)),
            ),
        )

        val vorschau = vorher.vorschauTeilgraphErsetzen(plan, prüfung)
        assertTrue(vorschau.istGültig, vorschau.fehler.joinToString())
        val editor = KartenEditorZustand(vorher, prüfung)
        editor.führeAus(KartenAktion.TeilgraphErsetzen(plan))

        assertEquals(setOf(eingang.id, ausgang.id), editor.karte.verbindungen.mapTo(mutableSetOf()) { it.id })
        assertTrue(editor.karte.knoten.any { it.id == neu.id })
        editor.rückgängig()
        assertEquals(vorher, editor.karte)
        assertFalse(editor.kannRückgängig())
    }

    @Test
    fun `vorschau lehnt nachtraeglich geaenderte grenze ab`() {
        val quelle = knoten("Quelle", ausgang = true)
        val alt = knoten("Alt", eingang = true, ausgang = true)
        val ziel = knoten("Ziel", eingang = true)
        val rand = verbinde(quelle, alt)
        val vorher = KartenDaten(name = "Veraltet", knoten = listOf(quelle, alt, ziel), verbindungen = listOf(rand))
        val plan = TeilgraphErsetzungsPlan.aus(vorher, setOf(alt.id), listOf(knoten("Neu", eingang = true)), emptyList())
        val geändert = vorher.copy(verbindungen = vorher.verbindungen + verbinde(alt, ziel))

        val vorschau = geändert.vorschauTeilgraphErsetzen(plan, prüfung)

        assertFalse(vorschau.istGültig)
        assertTrue(vorschau.fehler.any { "verändert" in it })
    }

    private fun knoten(name: String, eingang: Boolean = false, ausgang: Boolean = false): KnotenDaten = KnotenDaten(
        art = "test.knoten",
        name = name,
        anschlüsse = buildList {
            if (eingang) add(AnschlussDaten(name = "in", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art.id))
            if (ausgang) add(AnschlussDaten(name = "out", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art.id))
        },
    )

    private fun ref(knoten: KnotenDaten, richtung: AnschlussRichtung) = AnschlussVerweis(
        knoten.id,
        knoten.anschlüsse.single { it.richtung == richtung }.id,
    )

    private fun verbinde(von: KnotenDaten, zu: KnotenDaten) = VerbindungDaten(
        von = ref(von, AnschlussRichtung.Ausgang),
        zu = ref(zu, AnschlussRichtung.Eingang),
    )
}

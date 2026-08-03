package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.findeAnschluss
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuswahlZuKarteTest {
    private val arten = AnschlussArtRegister(MathematikAnschlussArten.alle)

    @Test
    fun `Auswahl materialisiert innere Kanten und Grenzschnittstellen ohne die Quelle zu ändern`() {
        val graph = beispielGraph()
        val quelleVorher = graph.karte

        val vorschau = graph.karte.vorschauFürNeueKarte(setOf(graph.a.id, graph.b.id), arten)
        val neu = vorschau.materialisiere(
            kartenName = "Teilkarte",
            eingangsNamen = listOf("x"),
            ausgangsNamen = listOf("ergebnis"),
        )

        assertEquals(quelleVorher, graph.karte)
        assertEquals(2, vorschau.ausgewählteKnoten.size)
        assertEquals(1, vorschau.innereVerbindungen.size)
        assertEquals(1, vorschau.eingänge.size)
        assertEquals(1, vorschau.ausgänge.size)
        assertEquals(4, neu.knoten.size)
        assertEquals(3, neu.verbindungen.size)
        assertTrue(neu.visuelleGruppen.isEmpty())

        val eingang = neu.knoten.single { it.art == "mathematik.kartenEingang" }
        val ausgang = neu.knoten.single { it.art == "mathematik.kartenAusgang" }
        assertEquals("x", eingang.parameter["name"])
        assertEquals("ergebnis", ausgang.parameter["name"])
        assertEquals(MathematikAnschlussArten.Zahl.id, eingang.anschlüsse.single().art)
        assertEquals(MathematikAnschlussArten.Zahl.id, ausgang.anschlüsse.single().art)

        val neueKnotenIds = neu.knoten.map { it.id }
        val neueAnschlussIds = neu.knoten.flatMap { knoten -> knoten.anschlüsse.map { it.id } }
        val neueVerbindungsIds = neu.verbindungen.map { it.id }
        assertEquals(neueKnotenIds.size, neueKnotenIds.distinct().size)
        assertEquals(neueAnschlussIds.size, neueAnschlussIds.distinct().size)
        assertEquals(neueVerbindungsIds.size, neueVerbindungsIds.distinct().size)
        assertTrue(neueKnotenIds.none { id -> id in setOf(graph.a.id, graph.b.id) })
        assertTrue(neueVerbindungsIds.none { id -> id in graph.karte.verbindungen.map { it.id }.toSet() })
        neu.verbindungen.forEach { verbindung ->
            assertTrue(neu.findeAnschluss(verbindung.von) != null)
            assertTrue(neu.findeAnschluss(verbindung.zu) != null)
        }
    }

    @Test
    fun `JSON Roundtrip bewahrt materialisierte Referenzen`() {
        val graph = beispielGraph()
        val neu = graph.karte
            .vorschauFürNeueKarte(setOf(graph.a.id, graph.b.id), arten)
            .materialisiere("Teilkarte", listOf("x"), listOf("ergebnis"))

        val gelesen = KartenJson.lese(KartenJson.schreibe(neu))

        assertEquals(neu, gelesen)
        gelesen.verbindungen.forEach { verbindung ->
            assertTrue(gelesen.findeAnschluss(verbindung.von) != null)
            assertTrue(gelesen.findeAnschluss(verbindung.zu) != null)
        }
    }

    @Test
    fun `Grenzvorschlag verwendet die effektive Anschlussart`() {
        val zahlQuelle = knoten(
            art = "test.zahlQuelle",
            name = "Zahlquelle",
            ausgang = AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Zahl.id,
            ),
        )
        val abhängig = KnotenDaten(
            art = "test.abhängig",
            name = "Abhängig",
            position = GraphPunkt(300f, 80f),
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "wert",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Objekt.id,
                ),
                AnschlussDaten(
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Objekt.id,
                    artFolgtEingang = "wert",
                ),
            ),
        )
        val ziel = knoten(
            art = "test.ziel",
            name = "Ziel",
            eingang = AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Objekt.id,
            ),
            position = GraphPunkt(600f, 80f),
        )
        val karte = KartenDaten(
            name = "Effektive Art",
            knoten = listOf(zahlQuelle, abhängig, ziel),
            verbindungen = listOf(
                VerbindungDaten(
                    von = ref(zahlQuelle, AnschlussRichtung.Ausgang),
                    zu = ref(abhängig, AnschlussRichtung.Eingang),
                ),
                VerbindungDaten(
                    von = ref(abhängig, AnschlussRichtung.Ausgang),
                    zu = ref(ziel, AnschlussRichtung.Eingang),
                ),
            ),
        )

        val vorschau = karte.vorschauFürNeueKarte(setOf(zahlQuelle.id, abhängig.id), arten)

        assertEquals(MathematikAnschlussArten.Zahl.id, vorschau.ausgänge.single().art)
    }

    @Test
    fun `Leere und doppelte Schnittstellennamen blockieren die Materialisierung`() {
        val graph = beispielGraph(zweiterAusgang = true)
        val vorschau = graph.karte.vorschauFürNeueKarte(setOf(graph.a.id, graph.b.id), arten)

        val fehler = vorschau.validierungsFehler(
            kartenName = " ",
            eingangsNamen = listOf(""),
            ausgangsNamen = listOf("gleich", "gleich"),
        )

        assertEquals(2, vorschau.ausgänge.size)
        assertTrue(fehler.any { "Kartenname" in it })
        assertTrue(fehler.any { "Karteneingänge" in it })
        assertTrue(fehler.any { "eindeutig" in it })
    }

    @Test
    fun `Zwei Materialisierungen erhalten getrennte Kartenidentitäten`() {
        val graph = beispielGraph()
        val vorschau = graph.karte.vorschauFürNeueKarte(setOf(graph.a.id, graph.b.id), arten)

        val erste = vorschau.materialisiere("Erste", listOf("x"), listOf("y"))
        val zweite = vorschau.materialisiere("Zweite", listOf("x"), listOf("y"))

        assertNotEquals(erste.id, zweite.id)
        assertTrue(erste.knoten.map { it.id }.toSet().intersect(zweite.knoten.map { it.id }.toSet()).isEmpty())
    }

    private data class BeispielGraph(
        val karte: KartenDaten,
        val a: KnotenDaten,
        val b: KnotenDaten,
    )

    private fun beispielGraph(zweiterAusgang: Boolean = false): BeispielGraph {
        val quelle = knoten(
            art = "test.quelle",
            name = "Quelle",
            ausgang = ausgang("wert"),
            position = GraphPunkt(20f, 80f),
        )
        val a = knoten(
            art = "test.a",
            name = "A",
            eingang = eingang("a"),
            ausgang = ausgang("wert"),
            position = GraphPunkt(300f, 80f),
        )
        val b = knoten(
            art = "test.b",
            name = "B",
            eingang = eingang("b"),
            ausgang = ausgang("wert"),
            position = GraphPunkt(600f, 80f),
        )
        val ziel1 = knoten(
            art = "test.ziel1",
            name = "Ziel 1",
            eingang = eingang("wert"),
            position = GraphPunkt(900f, 40f),
        )
        val ziel2 = knoten(
            art = "test.ziel2",
            name = "Ziel 2",
            eingang = eingang("wert"),
            position = GraphPunkt(900f, 220f),
        )
        val knoten = buildList {
            addAll(listOf(quelle, a, b, ziel1))
            if (zweiterAusgang) add(ziel2)
        }
        val verbindungen = buildList {
            add(VerbindungDaten(von = ref(quelle, AnschlussRichtung.Ausgang), zu = ref(a, AnschlussRichtung.Eingang)))
            add(VerbindungDaten(von = ref(a, AnschlussRichtung.Ausgang), zu = ref(b, AnschlussRichtung.Eingang)))
            add(VerbindungDaten(von = ref(b, AnschlussRichtung.Ausgang), zu = ref(ziel1, AnschlussRichtung.Eingang)))
            if (zweiterAusgang) {
                add(VerbindungDaten(von = ref(b, AnschlussRichtung.Ausgang), zu = ref(ziel2, AnschlussRichtung.Eingang)))
            }
        }
        return BeispielGraph(KartenDaten(name = "Quelle", knoten = knoten, verbindungen = verbindungen), a, b)
    }

    private fun knoten(
        art: String,
        name: String,
        eingang: AnschlussDaten? = null,
        ausgang: AnschlussDaten? = null,
        position: GraphPunkt = GraphPunkt.Zero,
    ) = KnotenDaten(
        art = art,
        name = name,
        position = position,
        anschlüsse = listOfNotNull(eingang, ausgang),
    )

    private fun eingang(name: String) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Zahl.id,
    )

    private fun ausgang(name: String) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Zahl.id,
    )

    private fun ref(knoten: KnotenDaten, richtung: AnschlussRichtung) = AnschlussVerweis(
        knotenId = knoten.id,
        anschlussId = knoten.anschlüsse.single { it.richtung == richtung }.id,
    )
}

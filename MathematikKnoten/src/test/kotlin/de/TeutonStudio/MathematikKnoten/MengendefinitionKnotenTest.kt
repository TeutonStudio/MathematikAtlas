package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MengendefinitionKnotenTest {
    private val register = MathematikAuswerterRegister()

    @Test
    fun `zusammengesetzter Eintrag erzeugt zwei gepaarte Knoten ohne Direktverbindung`() {
        val paar = erzeugeMengendefinitionsPaar(GraphPunkt(100f, 200f))

        assertEquals(MENGENKONSTRUKTOR_ART, paar.konstruktor.art)
        assertEquals(MENGENDEFINATOR_ART, paar.definator.art)
        assertEquals(paar.paarId, paar.konstruktor.mengendefinitionsPaarId())
        assertEquals(paar.paarId, paar.definator.mengendefinitionsPaarId())
        assertEquals(GraphPunkt(510f, 200f), paar.definator.position)
    }

    @Test
    fun `Konstruktor liefert gebundene reelle Variable mit Paarherkunft`() {
        val paar = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val ergebnis = werteAus(paar.konstruktor)
        val element = ergebnis.ausgaben.getValue("element")

        assertEquals(Variable("x"), element.objekt)
        assertEquals(ReelleZahlen, element.werteVorrat)
        assertEquals(paar.paarId, element.variablenQuellen.single().bindungsId)
        assertFalse(element.variablenQuellen.single().alsMethodenParameter)
    }

    @Test
    fun `Definator bindet ausschließlich das Element seines Konstruktorpaares`() {
        val paar = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val element = werteAus(paar.konstruktor).ausgaben.getValue("element")
        val aussage = Vergleich(element.objekt as Variable, VergleichsArt.Kleiner, RationaleZahl.von(3))
        val aussageWert = element.copy(objekt = aussage)

        val ergebnis = werteAus(paar.definator, mapOf("aussage" to aussageWert))
        val mengeWert = ergebnis.ausgaben.getValue("menge")
        assertIs<DefinierteMenge>(mengeWert.objekt)

        assertEquals("M=\\left\\{x\\in\\mathbb{R}\\mid x < 3\\right\\}", mengeWert.anzeigeLatex())
        assertTrue(ergebnis.ausgaben.getValue("menge").variablenQuellen.isEmpty())
    }

    @Test
    fun `gleichnamiges Element aus anderem Paar wird abgelehnt`() {
        val erstes = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val zweites = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val fremdesElement = werteAus(zweites.konstruktor).ausgaben.getValue("element")
        val aussage = Gleichheit(fremdesElement.objekt, RationaleZahl.Null)

        val fehler = assertFailsWith<IllegalArgumentException> {
            werteAus(erstes.definator, mapOf("aussage" to fremdesElement.copy(objekt = aussage)))
        }
        assertEquals("Die Aussage verwendet das Element des gekoppelten Mengenkonstruktors nicht.", fehler.message)
    }

    @Test
    fun `Aussageelement darf direkt mit Aussageeingang verbunden werden`() {
        val paar = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val elementAusgang = paar.konstruktor.anschlüsse.single()
        val aussageKonstruktor = paar.konstruktor.copy(
            parameter = paar.konstruktor.parameter +
                (MENGENDEFINITION_ELEMENTART to MathematikAnschlussArten.Aussage.id.wert),
            anschlüsse = listOf(elementAusgang.copy(art = MathematikAnschlussArten.Aussage.id)),
        )
        val element = werteAus(aussageKonstruktor).ausgaben.getValue("element")
        assertIs<Gleichheit>(element.objekt)

        val karte = KartenDaten(name = "Test", knoten = listOf(aussageKonstruktor, paar.definator))
        val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))
        val von = AnschlussVerweis(aussageKonstruktor.id, aussageKonstruktor.anschlüsse.single().id)
        val zu = AnschlussVerweis(
            paar.definator.id,
            paar.definator.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }.id,
        )
        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(karte, von, zu))

        val menge = werteAus(paar.definator, mapOf("aussage" to element))
            .ausgaben.getValue("menge")
        assertEquals("M=\\left\\{x\\in\\{\\bot,\\top\\}\\mid x = \\top\\right\\}", menge.anzeigeLatex())
    }

    @Test
    fun `Zahlelement kann nicht direkt an Aussageeingang angeschlossen werden`() {
        val paar = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val karte = KartenDaten(name = "Test", knoten = listOf(paar.konstruktor, paar.definator))
        val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))
        val von = AnschlussVerweis(paar.konstruktor.id, paar.konstruktor.anschlüsse.single().id)
        val zu = AnschlussVerweis(
            paar.definator.id,
            paar.definator.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }.id,
        )

        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfung.prüfe(karte, von, zu))
    }

    private fun werteAus(
        knoten: KnotenDaten,
        eingänge: Map<String, BedingterWert> = emptyMap(),
    ): KnotenAuswertungsErgebnis = requireNotNull(register.finde(knoten.art)).auswerten(
        KnotenAuswertungsKontext(knoten, eingänge, RechenKontext()),
    )
}

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
        assertFalse(MENGENDEFINITION_ELEMENTMENGE in paar.konstruktor.parameter)
    }

    @Test
    fun `Konstruktor liefert gebundene Variable ohne behauptete Obermenge`() {
        val paar = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val ergebnis = werteAus(paar.konstruktor)
        val element = ergebnis.ausgaben.getValue("element")

        assertEquals(Variable("x"), element.objekt)
        assertNull(element.werteVorrat)
        assertTrue(element.reelleVariablen.isEmpty())
        assertIs<FehlendeObermenge>(element.variablenQuellen.single().werteVorrat)
        assertEquals(paar.paarId, element.variablenQuellen.single().bindungsId)
        assertFalse(element.variablenQuellen.single().alsMethodenParameter)
    }

    @Test
    fun `Konstruktor akzeptiert jede registrierte Anschlussart`() {
        val paar = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val ausgang = paar.konstruktor.anschlüsse.single()

        MathematikAnschlussArten.alle.forEach { art ->
            val konstruktor = paar.konstruktor.copy(
                parameter = paar.konstruktor.parameter + (MENGENDEFINITION_ELEMENTART to art.id.wert),
                anschlüsse = listOf(ausgang.copy(art = art.id)),
            )
            val element = werteAus(konstruktor).ausgaben.getValue("element")
            assertEquals(art.id, element.variablenQuellen.single().gebundeneArt)
        }
    }

    @Test
    fun `Definator erzeugt ohne Obermenge eine Prädikatsmenge`() {
        val paar = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val element = werteAus(paar.konstruktor).ausgaben.getValue("element")
        val aussage = Vergleich(element.objekt as Variable, VergleichsArt.Kleiner, RationaleZahl.von(3))
        val aussageWert = element.copy(objekt = aussage)

        val ergebnis = werteAus(paar.definator, mapOf("aussage" to aussageWert))
        val mengeWert = ergebnis.ausgaben.getValue("menge")
        assertIs<PrädikatsMenge>(mengeWert.objekt)

        assertEquals("M=\\left\\{x\\mid x < 3\\right\\}", mengeWert.anzeigeLatex())
        assertTrue(mengeWert.variablenQuellen.isEmpty())
    }

    @Test
    fun `Gleichheit mit reeller Mitgliedschaft wird zur Einzelmenge und Mächtigkeit eins`() {
        val paar = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val element = werteAus(paar.konstruktor).ausgaben.getValue("element")
        val x = element.objekt as Variable
        val zwei = RationaleZahl.von(2)
        val aussage = Konjunktion(listOf(
            ElementBeziehung(x, ReelleZahlen),
            Gleichheit(x, zwei),
        ))

        val mengeWert = werteAus(
            paar.definator,
            mapOf("aussage" to element.copy(objekt = aussage)),
        ).ausgaben.getValue("menge")

        assertEquals(EndlicheMenge(setOf(zwei)), mengeWert.objekt)
        assertEquals(EndlicheMächtigkeit(RationaleZahl.Eins), mächtigkeit(mengeWert.objekt as MengenAusdruck))
        assertEquals("M=\\{2\\}", mengeWert.anzeigeLatex())
    }

    @Test
    fun `Vereinigung lässt sich ohne gemeinsame Obermenge als Prädikat definieren`() {
        val paar = erzeugeMengendefinitionsPaar(GraphPunkt.Zero)
        val ausgang = paar.konstruktor.anschlüsse.single()
        val konstruktor = paar.konstruktor.copy(
            parameter = paar.konstruktor.parameter +
                (MENGENDEFINITION_ELEMENTART to MathematikAnschlussArten.Objekt.id.wert),
            anschlüsse = listOf(ausgang.copy(art = MathematikAnschlussArten.Objekt.id)),
        )
        val element = werteAus(konstruktor).ausgaben.getValue("element")
        val aussage = Disjunktion(listOf(
            ElementBeziehung(element.objekt, MengenParameter("A")),
            ElementBeziehung(element.objekt, MengenParameter("B")),
        ))

        val menge = werteAus(
            paar.definator,
            mapOf("aussage" to element.copy(objekt = aussage)),
        ).ausgaben.getValue("menge")

        assertIs<PrädikatsMenge>(menge.objekt)
        assertEquals(
            "M=\\left\\{x\\mid x \\in A \\lor x \\in B\\right\\}",
            menge.anzeigeLatex(),
        )
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
        assertIs<AussagenParameter>(element.objekt)

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
        assertEquals(EndlicheMenge(setOf(WahrheitsKonstante(true))), menge.objekt)
        assertEquals("M=\\{\\mathcal{Wahr}\\}", menge.anzeigeLatex())
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

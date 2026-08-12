package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypPrüfung
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TopologischeStrukturKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Kardinalität trennt Endlichkeit und Abzählbarkeit`() {
        val endlich = kardinalitaetsVertrag(EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2))))
        val natuerlich = kardinalitaetsVertrag(NatürlicheZahlen)
        val reell = kardinalitaetsVertrag(ReelleZahlen)

        assertEquals(EndlichkeitsStatus.ENDLICH, endlich.endlichkeit)
        assertEquals(AbzaehlbarkeitsStatus.ABZAEHLBAR, endlich.abzaehlbarkeit)
        assertEquals(EndlichkeitsStatus.UNENDLICH, natuerlich.endlichkeit)
        assertEquals(AbzaehlbarkeitsStatus.ABZAEHLBAR, natuerlich.abzaehlbarkeit)
        assertEquals(EndlichkeitsStatus.UNENDLICH, reell.endlichkeit)
        assertEquals(AbzaehlbarkeitsStatus.UEBERABZAEHLBAR, reell.abzaehlbarkeit)
    }

    @Test
    fun `leerer Produktfaktor bleibt auch neben unendlicher Menge endlich`() {
        val produkt = KartesischesProdukt(
            listOf(
                EndlicheMenge(emptySet()),
                ReelleZahlen,
            ),
        )
        val vertrag = kardinalitaetsVertrag(produkt)

        assertEquals(EndlichkeitsStatus.ENDLICH, vertrag.endlichkeit)
        assertEquals(AbzaehlbarkeitsStatus.ABZAEHLBAR, vertrag.abzaehlbarkeit)
    }

    @Test
    fun `Folgenraum über leerer Elementmenge enthält nur leere Folge`() {
        val vertrag = kardinalitaetsVertrag(Folgenraum(EndlicheMenge(emptySet())))

        assertEquals(EndlichkeitsStatus.ENDLICH, vertrag.endlichkeit)
        assertEquals(AbzaehlbarkeitsStatus.ABZAEHLBAR, vertrag.abzaehlbarkeit)
    }

    @Test
    fun `nackte Menge erhält nur intrinsische automatische Adjektive`() {
        val ids = automatischeAdjektive(ReelleZahlen).map { it.eigenschaftId }

        assertEquals(listOf("unendlich", "überabzählbar"), ids)
        assertTrue("offen" !in ids)
        assertTrue("abgeschlossen" !in ids)
    }

    @Test
    fun `topologischer Raum verwendet registrierte Standardtopologie nur explizit im Strukturknoten`() {
        val knoten = TopologischeStrukturKnotenVorlagen.TopologischerRaum.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("menge" to BedingterWert(ReelleZahlen)),
                rechenKontext = RechenKontext(),
            ),
        )

        val raum = assertIs<TopologischerRaum>(ergebnis.ausgaben.getValue("raum").objekt)
        assertEquals(ReelleZahlen, raum.traeger)
        assertIs<StandardTopologie>(raum.topologie)
    }

    @Test
    fun `kanonische Topologie wird für unbekannte Trägermenge nicht geraten`() {
        val knoten = TopologischeStrukturKnotenVorlagen.TopologischerRaum.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("menge" to BedingterWert(BenannteMenge("X"))),
                rechenKontext = RechenKontext(),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertNotNull(ergebnis.fehler)
        assertTrue(ergebnis.fehler!!.contains("keine kanonische Standardtopologie"))
    }

    @Test
    fun `Restklassenring wird nicht als Teilmenge der reellen Zahlen geraten`() {
        assertEquals(
            AussageStatus.UNENTSCHEIDBAR,
            teilMengenStatus(ModuloZahlenraum(5), ReelleZahlen),
        )
    }

    @Test
    fun `Offenheit ohne Raum ist bedingt statt implizit reell`() {
        val intervall = ReellesIntervall(
            links = RationaleZahl.von(0),
            linksOffen = true,
            rechts = RationaleZahl.von(1),
            rechtsOffen = true,
        )
        val knoten = MathematischeEigenschaftKnotenVorlagen.MengenEigenschaft
            .erzeuge(GraphPunkt.Zero)
            .copy(
                parameter = MathematischeEigenschaftKnotenVorlagen.MengenEigenschaft.standardParameter +
                    (EIGENSCHAFT_PARAMETER to "offen"),
            )
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("menge" to BedingterWert(intervall)),
                rechenKontext = RechenKontext(),
            ),
        )

        val aussage = assertIs<EigenschaftsAussage>(ergebnis.ausgaben.getValue("aussage").objekt)
        assertEquals(AussageStatus.BEDINGT, aussage.aussageStatus)
        assertEquals("topologischer-raum-fehlt", aussage.diagnose?.code)
    }

    @Test
    fun `Offenheit wird relativ zur verbundenen Topologie entschieden`() {
        val menge = EndlicheMenge(setOf(RationaleZahl.von(1)))
        val raum = TopologischerRaum(ReelleZahlen, DiskreteTopologie(ReelleZahlen))
        val knoten = konfiguriereMengenEigenschaftKnoten(
            MathematischeEigenschaftKnotenVorlagen.MengenEigenschaft.erzeuge(GraphPunkt.Zero),
            "offen",
        )
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "menge" to BedingterWert(menge),
                    "raum" to BedingterWert(raum),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val aussage = assertIs<EigenschaftsAussage>(ergebnis.ausgaben.getValue("aussage").objekt)
        assertEquals(AussageStatus.BEWIESEN, aussage.aussageStatus)
    }

    @Test
    fun `metrischer Raum exportiert seine induzierte Topologie`() {
        val x = Variable("x")
        val y = Variable("y")
        val d = Methode(
            name = "d",
            parameter = listOf(x, y),
            vorschrift = RationaleZahl.von(0),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val knoten = TopologischeStrukturKnotenVorlagen.MetrischerRaum.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "menge" to BedingterWert(ReelleZahlen),
                    "metrik" to BedingterWert(d),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        assertIs<MetrischerRaum>(ergebnis.ausgaben.getValue("raum").objekt)
        val topologie = assertIs<TopologischerRaum>(ergebnis.ausgaben.getValue("topologie").objekt)
        assertIs<MetrischInduzierteTopologie>(topologie.topologie)
        assertTrue(ergebnis.warnungen.any { it.contains("Metrikaxiome") })
    }

    @Test
    fun `nichtreeller Metrikzielraum wird abgelehnt`() {
        val x = Variable("x")
        val y = Variable("y")
        val d = Methode(
            name = "d",
            parameter = listOf(x, y),
            vorschrift = RationaleZahl.von(0),
            zielMenge = KomplexeZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )

        assertIs<StrukturPruefung.Ungueltig>(pruefeMetrik(ReelleZahlen, d))
    }

    @Test
    fun `Stetigkeit verlangt kompatible effektive Quell- und Zielräume`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val quellRaum = TopologischerRaum(ReelleZahlen, DiskreteTopologie(ReelleZahlen))
        val zielRaum = TopologischerRaum(
            ReelleZahlen,
            StandardTopologie(ReelleZahlen, StandardTopologieKennung.REELL),
        )
        val knoten = konfiguriereMethodenEigenschaftKnoten(
            MathematischeEigenschaftKnotenVorlagen.MethodenEigenschaft.erzeuge(GraphPunkt.Zero),
            "stetig",
        )
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "methode" to BedingterWert(methode),
                    "quellRaum" to BedingterWert(quellRaum),
                    "zielRaum" to BedingterWert(zielRaum),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val aussage = assertIs<EigenschaftsAussage>(ergebnis.ausgaben.getValue("aussage").objekt)
        assertEquals(AussageStatus.BEWIESEN, aussage.aussageStatus)
    }

    @Test
    fun `metrischer Raum ist im Typsystem topologischer Raum`() {
        val pruefung = MathematischeTypen.typSystem.prüfe(
            TypAusdruck.Atom(MathematischeTypen.MetrischerRaum),
            TypAusdruck.Atom(MathematischeTypen.TopologischerRaum),
        )
        assertIs<TypPrüfung.Kompatibel>(pruefung)
    }
}

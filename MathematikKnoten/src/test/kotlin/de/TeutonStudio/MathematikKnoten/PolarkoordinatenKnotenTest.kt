package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*
import de.TeutonStudio.TypSystem.*

class PolarkoordinatenKnotenTest {
    private val laufzeit = MathematikKartenLaufzeit()
    private fun t(vararg werte: Long) = Tupel(werte.map(RationaleZahl::von))
    private fun port(k: KnotenDaten, name: String) = AnschlussVerweis(k.id, k.anschlüsse.single { it.name == name }.id)
    private fun rechner(op: VektorRechnerOperator) = vektorRechnerVorlage(op).erzeuge(GraphPunkt.Zero)
    private fun auswerten(k: KnotenDaten, vararg werte: Pair<String, MathematischesObjekt>): KnotenAuswertungsErgebnis =
        GesamterMathematikAuswerter.erzeugeRegister().finde(k.art)!!.auswerten(KnotenAuswertungsKontext(
            k, werte.associate { it.first to BedingterWert(it.second) }, RechenKontext()))

    @Test fun `Gesamtregister berechnet alle drei Operatoren und R1`() {
        val r = auswerten(rechner(VektorRechnerOperator.RADIUS), "kartesisch" to t(3, 4))
        assertNull(r.fehler)
        assertEquals(RationaleZahl.von(5), r.ausgaben.getValue("radius").objekt)
        val w = auswerten(rechner(VektorRechnerOperator.POLARWINKEL), "kartesisch" to t(0, -2))
        assertEquals(Tupel(listOf(GradWinkel(RationaleZahl.von(270)))), w.ausgaben.getValue("winkel").objekt)
        val s = auswerten(rechner(VektorRechnerOperator.POLARWINKEL), "kartesisch" to t(-3))
        assertEquals(RationaleZahl.von(-1), s.ausgaben.getValue("winkel").objekt)
        val k = rechner(VektorRechnerOperator.KARTESISCH)
        assertEquals(t(0, -2), auswerten(k, "radius" to RationaleZahl.von(2), "winkel" to t(270)).ausgaben.getValue("kartesisch").objekt)
        assertEquals(t(-3), auswerten(k, "radius" to RationaleZahl.von(3), "winkel" to RationaleZahl.von(-1)).ausgaben.getValue("kartesisch").objekt)
        assertNotNull(auswerten(k).fehler)
    }

    @Test fun `Uebersetzer bewahrt Metadaten und unterscheidet flache R1 Rueckrichtung`() {
        val basis = ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        val polar = konfiguriereÜbersetzer(basis, "polar")
        assertEquals(Tupel(listOf(RationaleZahl.von(2), Tupel(listOf(GradWinkel(RationaleZahl.von(270)))))), auswerten(polar, "wert" to t(0, -2)).ausgaben.getValue("ergebnis").objekt)
        val rück = konfiguriereÜbersetzer(polar, "kartesisch")
        assertEquals(t(-2, 0), auswerten(rück, "wert" to t(2, 180)).ausgaben.getValue("ergebnis").objekt)
        val r1 = konfiguriereÜbersetzerEindimensional(rück, true)
        assertEquals(t(-2), auswerten(r1, "wert" to t(2, -1)).ausgaben.getValue("ergebnis").objekt)
        assertEquals(basis.anschlüsse.map { it.id }, r1.anschlüsse.map { it.id })
        val annahmen = setOf<Aussage>(Gleichheit(RationaleZahl.Eins, RationaleZahl.Eins))
        val quelle = VariablenQuelle(KnotenId("quelle"), "a", ReelleZahlen)
        val wert = BedingterWert(t(3, 4), annahmen, variablenQuellen = listOf(quelle))
        val ergebnis = GesamterMathematikAuswerter.erzeugeRegister().finde(polar.art)!!.auswerten(
            KnotenAuswertungsKontext(polar, mapOf("wert" to wert), RechenKontext())).ausgaben.getValue("ergebnis")
        assertEquals(annahmen, ergebnis.annahmen)
        assertEquals(listOf(quelle), ergebnis.variablenQuellen)
        assertEquals(inferiereZielmenge(ergebnis.objekt as MathematischesObjekt), ergebnis.zielMenge)
    }

    @Test fun `Verbundene Rechner sind in R1 und R2 kompatibel und ueberstehen JSON`() {
        for (n in 1..2) {
            var quelle = konfiguriereTupel(MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero), TUPEL_EINZEL_EINGABEN)
            if (n == 1) quelle = quelle.copy(anschlüsse = quelle.anschlüsse.filterNot { it.richtung == AnschlussRichtung.Eingang && it.name == "b" })
            quelle = quelle.copy(parameter = quelle.parameter + mapOf("standardwert.a" to "-3", "standardwert.b" to "4"))
            val radius = rechner(VektorRechnerOperator.RADIUS)
            val winkel = rechner(VektorRechnerOperator.POLARWINKEL)
            val rück = rechner(VektorRechnerOperator.KARTESISCH)
            var karte = KartenDaten(name = "R$n", knoten = listOf(quelle, radius, winkel, rück))
            listOf(port(quelle, "tupel") to port(radius, "kartesisch"), port(quelle, "tupel") to port(winkel, "kartesisch"),
                port(radius, "radius") to port(rück, "radius"), port(winkel, "winkel") to port(rück, "winkel")).forEach { (von, zu) ->
                assertIs<VerbindungsPrüfung.Erlaubt>(laufzeit.graphPrüfung.prüfe(karte, von, zu))
                karte = karte.copy(verbindungen = karte.verbindungen + VerbindungDaten(von = von, zu = zu))
            }
            val art = effektiverMathematikAnschluss(karte, port(winkel, "winkel"))!!.art
            assertEquals(if (n == 1) MathematikAnschlussArten.Zahl.id else MathematikAnschlussArten.Tupel.id, art)
            listOf(karte, MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte))).forEach { k ->
                val ergebnis = laufzeit.auswerten(k)
                assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.toString())
                val ausgabe = ergebnis.knoten.getValue(rück.id).ausgaben.getValue("kartesisch").objekt as Tupel
                assertEquals(n, ausgabe.elemente.size)
                val x = ((DomaenenAuswerter.wert(ausgabe.elemente[0] as ZahlAusdruck) as DomaenenErgebnis.Wert).wert as DomaenenWert.Reell).wert.toDouble()
                assertEquals(-3.0, x, 1e-12)
            }
        }
    }

    @Test fun `Ziel und R1 Modus sind atomar rueckgaengig und persistierbar`() {
        val basis = ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        val vorher = KartenDaten(name = "Undo", knoten = listOf(basis))
        val neu = konfiguriereÜbersetzerEindimensional(konfiguriereÜbersetzer(basis, "kartesisch"), true)
        val plan = planeKonfiguration(vorher, neu, laufzeit.graphPrüfung, laufzeit::auswerten)
        val editor = KartenEditorZustand(vorher, laufzeit.graphPrüfung)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte))
        editor.rückgängig()
        assertEquals(vorher, editor.karte)
        editor.wiederholen()
        val geladen = MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(editor.karte))
        assertTrue(übersetzerEindimensional(geladen.knoten.single()))
        assertEquals("kartesisch", übersetzerZiel(geladen.knoten.single()))
    }

    @Test fun `Dimension bleibt durch Uebersetzer und Rueckrechner fuer Winkelverbraucher erhalten`() {
        val quelle = konfiguriereTupel(MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero), TUPEL_EINZEL_EINGABEN)
        val basis = ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        val polar = konfiguriereÜbersetzer(basis, "polar")
        val rück = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "kartesisch")
        val winkel = rechner(VektorRechnerOperator.POLARWINKEL)
        val radius = rechner(VektorRechnerOperator.RADIUS)
        val rechnerRück = rechner(VektorRechnerOperator.KARTESISCH)
        val winkelDanach = rechner(VektorRechnerOperator.POLARWINKEL)
        val verbraucher = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "polar")
        val karte = KartenDaten(name = "Kette", knoten = listOf(quelle, polar, rück, winkel, radius, rechnerRück, winkelDanach, verbraucher),
            verbindungen = listOf(
                port(quelle, "tupel") to port(polar, "wert"), port(polar, "ergebnis") to port(rück, "wert"),
                port(rück, "ergebnis") to port(winkel, "kartesisch"), port(quelle, "tupel") to port(radius, "kartesisch"),
                port(radius, "radius") to port(rechnerRück, "radius"), port(winkel, "winkel") to port(rechnerRück, "winkel"),
                port(rechnerRück, "kartesisch") to port(winkelDanach, "kartesisch"),
            ).map { (von, zu) -> VerbindungDaten(von = von, zu = zu) })
        for (k in listOf(winkel, winkelDanach)) {
            assertEquals(MathematikAnschlussArten.Tupel.id, effektiverMathematikAnschluss(karte, port(k, "winkel"))!!.art)
            assertIs<VerbindungsPrüfung.Erlaubt>(laufzeit.graphPrüfung.prüfe(karte, port(k, "winkel"), port(verbraucher, "wert")))
        }
        val r1 = konfiguriereÜbersetzerEindimensional(rück, true)
        val r1Karte = karte.copy(knoten = karte.knoten.map { if (it.id == rück.id) r1 else it })
        assertEquals(MathematikAnschlussArten.Zahl.id, effektiverMathematikAnschluss(r1Karte, port(winkel, "winkel"))!!.art)
    }
    @Test fun `leeres Tupel und Zielwechsel erzeugen Diagnose statt Inspectorabsturz`() {
        val quelle = konfiguriereTupel(MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero), TUPEL_EINZEL_EINGABEN).let { it.copy(anschlüsse=it.anschlüsse.filter { p -> p.richtung==AnschlussRichtung.Ausgang }) }
        val basis = ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        for (ziel in listOf("polar","kartesisch","indexmethode","polar")) {
            val übersetzer=konfiguriereÜbersetzer(basis,ziel)
            val karte=KartenDaten(name="Leere Koordinaten",knoten=listOf(quelle,übersetzer),verbindungen=listOf(VerbindungDaten(von=port(quelle,"tupel"),zu=port(übersetzer,"wert"))))
            val quellTyp=assertIs<TypAusdruck.Parameterisiert>(effektiverMathematikAnschluss(karte,port(quelle,"tupel"))!!.vertrag.typ)
            assertEquals(MathematischeTypen.Tupel,quellTyp.konstruktor)
            assertTrue(quellTyp.argumente.isEmpty())
            val zielPort=assertNotNull(effektiverMathematikAnschluss(karte,port(übersetzer,"ergebnis")))
            if(ziel=="polar") assertEquals(TypAusdruck.Unbekannt,zielPort.vertrag.typ)
            assertEquals(basis.anschlüsse.map { it.id },übersetzer.anschlüsse.map { it.id })
            if(ziel!="indexmethode") assertTrue(laufzeit.auswerten(karte).fehler.isNotEmpty())
        }
    }

}

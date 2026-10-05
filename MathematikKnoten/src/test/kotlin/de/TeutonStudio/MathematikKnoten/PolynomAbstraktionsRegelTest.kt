package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.logik.vorschauTeilgraphErsetzen
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKnoten.abstraktion.*
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.zerlegeAlsPolynom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PolynomAbstraktionsRegelTest {
    @Test
    fun `kubisches rechengeflecht wird als tupel und polynom vorgeschlagen und atomar ersetzt`() {
        val laufzeit = MathematikKartenLaufzeit()
        val karte = kubischesPolynom()
        val vorherAuswertung = laufzeit.auswerten(karte)
        assertTrue(vorherAuswertung.fehler.isEmpty(), vorherAuswertung.fehler.joinToString())
        val wurzel = karte.knoten.last()
        val ausdruck = vorherAuswertung.knoten.getValue(wurzel.id).ausgaben.getValue("wert").objekt as ZahlAusdruck
        assertNotNull(zerlegeAlsPolynom(ausdruck, Variable("x")))

        val vorschlag = AbstraktionsRegister().analysiere(
            AbstraktionsKontext(
                karte = karte,
                auswertung = vorherAuswertung,
                graphPrüfung = laufzeit.graphPrüfung,
                auswerten = laufzeit::auswerten,
            ),
        ).firstOrNull()

        assertNotNull(vorschlag)
        assertTrue(vorschlag.ersparnis > 0)
        assertEquals(1, vorschlag.ersetzungsPlan.neueKnoten.count { it.art == "mathematik.tupel" })
        assertEquals(1, vorschlag.ersetzungsPlan.neueKnoten.count {
            it.art == ZAHLENRECHNER_ART && it.parameter[ZAHLENRECHNER_OPERATOR] == ErweiterterZahlenOperator.POLYNOM.stabileId
        })

        val editor = KartenEditorZustand(karte, laufzeit.graphPrüfung)
        editor.führeAus(KartenAktion.TeilgraphErsetzen(vorschlag.ersetzungsPlan))
        assertTrue(laufzeit.auswerten(editor.karte).fehler.isEmpty())
        editor.rückgängig()
        assertEquals(karte, editor.karte)
    }

    @Test
    fun `gemeinsam verwendeter zwischenknoten wird von keinem vorschlag geloescht`() {
        val laufzeit = MathematikKartenLaufzeit()
        val basis = kubischesPolynom()
        val potenz = basis.knoten.first { it.art == "mathematik.potenz" }
        val eins = zahl(1, GraphPunkt(0f, 600f))
        val verbraucher = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt(900f, 600f))
        val erweitert = basis.copy(
            knoten = basis.knoten + eins + verbraucher,
            verbindungen = basis.verbindungen + listOf(
                verbinde(potenz, "wert", verbraucher, "a"),
                verbinde(eins, "wert", verbraucher, "b"),
            ),
        )

        val vorschläge = AbstraktionsRegister().analysiere(
            AbstraktionsKontext(
                karte = erweitert,
                auswertung = laufzeit.auswerten(erweitert),
                graphPrüfung = laufzeit.graphPrüfung,
                auswerten = laufzeit::auswerten,
            ),
        )

        assertTrue(vorschläge.none { potenz.id in it.vorherKnoten })
    }

    @Test
    fun `nachgeschaltete term zu methode schnittstelle bleibt erhalten`() {
        val laufzeit = MathematikKartenLaufzeit()
        val basis = kubischesPolynom()
        val wurzel = basis.knoten.last()
        val termZuMethode = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt(1480f, 240f))
        val grenze = verbinde(wurzel, "wert", termZuMethode, "term")
        val karte = basis.copy(
            knoten = basis.knoten + termZuMethode,
            verbindungen = basis.verbindungen + grenze,
        )
        val vorher = laufzeit.auswerten(karte).knoten.getValue(termZuMethode.id).ausgaben.getValue("methode")
        val vorschlag = AbstraktionsRegister().analysiere(
            AbstraktionsKontext(
                karte = karte,
                auswertung = laufzeit.auswerten(karte),
                graphPrüfung = laufzeit.graphPrüfung,
                auswerten = laufzeit::auswerten,
            ),
        ).first { wurzel.id == it.wurzelKnoten }

        assertTrue(termZuMethode.id !in vorschlag.vorherKnoten)
        assertTrue(vorschlag.ersetzungsPlan.neueVerbindungen.any { it.id == grenze.id })
        val ersetzt = karte.vorschauTeilgraphErsetzen(vorschlag.ersetzungsPlan, laufzeit.graphPrüfung).karte
        assertNotNull(ersetzt)
        val nachher = laufzeit.auswerten(ersetzt).knoten.getValue(termZuMethode.id).ausgaben.getValue("methode")
        assertEquals(vorher, nachher)
    }

    private fun kubischesPolynom(): KartenDaten {
        val x = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt(0f, 0f))
        val drei = zahl(3, GraphPunkt(0f, 120f))
        val zweiKoeffizient = zahl(2, GraphPunkt(0f, 240f))
        val minusSieben = zahl(-7, GraphPunkt(0f, 360f))
        val vier = zahl(4, GraphPunkt(0f, 480f))
        val exponentDrei = zahl(3, GraphPunkt(150f, 80f))
        val exponentZwei = zahl(2, GraphPunkt(150f, 200f))
        val x3 = MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt(300f, 60f))
        val x2 = MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt(300f, 190f))
        val dreiX3 = MathematikKnotenVorlagen.Multiplikation.erzeuge(GraphPunkt(510f, 60f))
        val zweiX2 = MathematikKnotenVorlagen.Multiplikation.erzeuge(GraphPunkt(510f, 190f))
        val minusSiebenX = MathematikKnotenVorlagen.Multiplikation.erzeuge(GraphPunkt(510f, 320f))
        val summeA = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt(740f, 100f))
        val summeB = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt(970f, 180f))
        val summeC = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt(1200f, 240f))
        val knoten = listOf(x, drei, zweiKoeffizient, minusSieben, vier, exponentDrei, exponentZwei, x3, x2, dreiX3, zweiX2, minusSiebenX, summeA, summeB, summeC)
        val verbindungen = listOf(
            verbinde(x, "wert", x3, "basis"), verbinde(exponentDrei, "wert", x3, "exponent"),
            verbinde(x, "wert", x2, "basis"), verbinde(exponentZwei, "wert", x2, "exponent"),
            verbinde(drei, "wert", dreiX3, "a"), verbinde(x3, "wert", dreiX3, "b"),
            verbinde(zweiKoeffizient, "wert", zweiX2, "a"), verbinde(x2, "wert", zweiX2, "b"),
            verbinde(minusSieben, "wert", minusSiebenX, "a"), verbinde(x, "wert", minusSiebenX, "b"),
            verbinde(dreiX3, "wert", summeA, "a"), verbinde(zweiX2, "wert", summeA, "b"),
            verbinde(summeA, "wert", summeB, "a"), verbinde(minusSiebenX, "wert", summeB, "b"),
            verbinde(summeB, "wert", summeC, "a"), verbinde(vier, "wert", summeC, "b"),
        )
        return KartenDaten(name = "3x³ + 2x² - 7x + 4", knoten = knoten, verbindungen = verbindungen)
    }

    private fun zahl(wert: Int, position: GraphPunkt) = MathematikKnotenVorlagen.Zahl.erzeuge(position).copy(
        parameter = mapOf("wert" to wert.toString()),
    )

    private fun verbinde(von: KnotenDaten, ausgang: String, zu: KnotenDaten, eingang: String) = VerbindungDaten(
        von = AnschlussVerweis(von.id, von.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang && it.name == ausgang }.id),
        zu = AnschlussVerweis(zu.id, zu.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang && it.name == eingang }.id),
    )
}

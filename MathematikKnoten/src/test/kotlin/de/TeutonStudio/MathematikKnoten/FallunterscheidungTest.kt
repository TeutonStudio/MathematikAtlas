package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.VariablenQuelle
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FallunterscheidungTest {
    private val auswerter = StandardMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Vorlage ordnet Aussage zwischen Wahr- und Lüge-Eingang an`() {
        val knoten = MathematikKnotenVorlagen.Fall.erzeuge(GraphPunkt.Zero)
        val eingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
        val ausgänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals(listOf("wahr", "aussage", "lüge"), eingänge.map { it.name })
        assertEquals(listOf(MathematikAnschlussArten.Objekt.id, MathematikAnschlussArten.Aussage.id, MathematikAnschlussArten.Objekt.id), eingänge.map { it.art })
        assertEquals(listOf("wert"), ausgänge.map { it.name })
        assertEquals(MathematikAnschlussArten.Objekt.id, ausgänge.single().art)
    }

    @Test
    fun `wahre Aussage wählt ersten Eingang`() {
        val ergebnis = werteAus(
            wahr = BedingterWert(RationaleZahl.von(7)),
            aussage = BedingterWert(WahrheitsKonstante(true)),
            lüge = BedingterWert(EndlicheMenge(setOf(RationaleZahl.Eins))),
        )

        assertEquals(RationaleZahl.von(7), ergebnis.objekt)
    }

    @Test
    fun `falsche Aussage wählt zweiten Eingang`() {
        val falschWert = EndlicheMenge(setOf(RationaleZahl.von(2), RationaleZahl.von(3)))
        val ergebnis = werteAus(
            wahr = BedingterWert(RationaleZahl.von(7)),
            aussage = BedingterWert(WahrheitsKonstante(false)),
            lüge = BedingterWert(falschWert),
        )

        assertEquals(falschWert, ergebnis.objekt)
    }

    @Test
    fun `unentscheidbare Aussage bleibt als Fallausdruck erhalten`() {
        val x = Variable("x")
        val ergebnis = werteAus(
            wahr = BedingterWert(x),
            aussage = BedingterWert(Gleichheit(x, RationaleZahl.Eins)),
            lüge = BedingterWert(RationaleZahl.Null),
        )

        val fall = assertIs<FallAusdruck>(ergebnis.objekt)
        assertEquals(x, fall.wahr)
        assertEquals(Gleichheit(x, RationaleZahl.Eins), fall.aussage)
        assertEquals(RationaleZahl.Null, fall.lüge)
    }

    @Test
    fun `symbolischer Fall wird erst beim Anwenden der Methode entschieden`() {
        val x = Variable("x")
        val quelle = KnotenId("x-quelle")
        val variablenQuelle = VariablenQuelle(quelle, x.name, ReelleZahlen)
        val fallWert = werteAus(
            wahr = BedingterWert(x, werteVorrat = ReelleZahlen, variablenQuellen = listOf(variablenQuelle)),
            aussage = BedingterWert(
                Vergleich(x, VergleichsArt.Größer, RationaleZahl.Null),
                variablenQuellen = listOf(variablenQuelle),
            ),
            lüge = BedingterWert(RationaleZahl.Null),
        )
        val knoten = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero)
        val methode = assertIs<Funktion>(auswerter.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("term" to fallWert),
                rechenKontext = RechenKontext(),
                topologischeReihenfolge = mapOf(quelle to 0),
            ),
        ).ausgaben.getValue("methode").objekt)

        assertEquals(listOf(x), methode.parameter)
        assertEquals(RationaleZahl.von(2), methode.wendeAn(mapOf("x" to RationaleZahl.von(2))).getValue("wert"))
        assertEquals(RationaleZahl.Null, methode.wendeAn(mapOf("x" to RationaleZahl.von(-2))).getValue("wert"))
    }

    private fun werteAus(wahr: BedingterWert, aussage: BedingterWert, lüge: BedingterWert): BedingterWert {
        val knoten = MathematikKnotenVorlagen.Fall.erzeuge(GraphPunkt.Zero)
        return auswerter.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("wahr" to wahr, "aussage" to aussage, "lüge" to lüge),
                RechenKontext(),
            ),
        ).ausgaben.getValue("wert")
    }
}

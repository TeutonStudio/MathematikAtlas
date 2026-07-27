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
import kotlin.test.assertFalse
import kotlin.test.assertIs

class MengenKnotenTest {
    private val register = StandardMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Einzelmenge nimmt beliebiges Objekt auf`() {
        val knoten = MathematikKnotenVorlagen.Einzelmenge.erzeuge(GraphPunkt.Zero)
        val aussage = WahrheitsKonstante(true)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(knoten, mapOf("element" to BedingterWert(aussage)), RechenKontext()),
        )

        assertEquals(EndlicheMenge(setOf(aussage)), ergebnis.ausgaben.getValue("menge").objekt)
    }

    @Test
    fun `Mengenfilter wertet endliche Menge exakt aus`() {
        val x = Variable("x")
        val wahrheitsmenge = EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))
        val methode = Funktion(
            name = "positiv",
            parameter = listOf(x),
            ausgaben = mapOf("aussage" to Vergleich(x, VergleichsArt.Größer, RationaleZahl.Null)),
            zielMengen = mapOf("aussage" to wahrheitsmenge),
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val menge = EndlicheMenge(setOf(RationaleZahl.von(-2), RationaleZahl.Null, RationaleZahl.von(3)))
        val knoten = MathematikKnotenVorlagen.Mengenfilter.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("menge" to BedingterWert(menge), "methode" to BedingterWert(methode)),
                RechenKontext(),
            ),
        )

        assertEquals(EndlicheMenge(setOf(RationaleZahl.von(3))), ergebnis.ausgaben.getValue("menge").objekt)
    }

    @Test
    fun `Mengenfilter bewahrt unendliche Filter symbolisch`() {
        val x = Variable("x")
        val methode = Funktion(
            name = "positiv",
            parameter = listOf(x),
            ausgaben = mapOf("aussage" to Vergleich(x, VergleichsArt.Größer, RationaleZahl.Null)),
            zielMengen = mapOf("aussage" to EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))),
            werteVorräte = mapOf("x" to ReelleZahlen),
        )

        assertIs<GefilterteMenge>(filtereMenge(ReelleZahlen, methode))
    }

    @Test
    fun `Karten-Eingänge werden nicht zu Parametern der inneren Methode`() {
        val kartenEingang = MathematikKnotenVorlagen.KartenEingang.erzeuge(GraphPunkt.Zero).let { knoten ->
            knoten.copy(
                parameter = knoten.parameter + ("name" to "min"),
                anschlüsse = knoten.anschlüsse.map { anschluss ->
                    if (anschluss.richtung == AnschlussRichtung.Ausgang) anschluss.copy(art = MathematikAnschlussArten.Zahl.id) else anschluss
                },
            )
        }
        val minWert = register.finde(kartenEingang.art)!!.auswerten(
            KnotenAuswertungsKontext(kartenEingang, emptyMap(), RechenKontext()),
        ).ausgaben.getValue("wert")
        assertFalse(minWert.variablenQuellen.single().alsMethodenParameter)

        val x = Variable("x")
        val termWert = BedingterWert(
            objekt = addition(listOf(x, minWert.objekt as ZahlAusdruck)),
            reelleVariablen = mapOf("x" to ReelleZahlen, "min" to ReelleZahlen),
            variablenQuellen = listOf(
                VariablenQuelle(KnotenId("x-quelle"), "x", ReelleZahlen),
                minWert.variablenQuellen.single(),
            ),
        )
        val termZuMethode = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero)
        val methode = assertIs<Funktion>(register.finde(termZuMethode.art)!!.auswerten(
            KnotenAuswertungsKontext(
                termZuMethode,
                mapOf("term" to termWert),
                RechenKontext(),
                topologischeReihenfolge = mapOf(KnotenId("x-quelle") to 0, kartenEingang.id to 1),
            ),
        ).ausgaben.getValue("methode").objekt)

        assertEquals(listOf("x"), methode.parameter.map { it.name })
        assertEquals(setOf("x"), methode.werteVorräte.keys)
    }
}

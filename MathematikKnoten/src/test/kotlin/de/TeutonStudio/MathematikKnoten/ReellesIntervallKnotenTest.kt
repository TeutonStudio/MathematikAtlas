package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.wendeAn
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ReellesIntervallKnotenTest {
    private val register = StandardMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Vorlage registriert zwei geordnete Zahleneingänge und einen Mengenausgang`() {
        val knoten = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)

        assertEquals("mathematik.reellesIntervall", knoten.art)
        assertEquals("Reelles Intervall", knoten.name)
        assertEquals(
            listOf("untereGrenze", "obereGrenze"),
            knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }.map { it.name },
        )
        assertEquals(MathematikAnschlussArten.Menge.id, knoten.anschlüsse.single { it.name == "menge" }.art)
        assertFalse(knoten.anschlüsse.any { it.kannSichErweitern })
        assertNotNull(register.finde(knoten.art))
    }

    @Test
    fun `Graphprüfung erlaubt typisierte Kanten und das Ersetzen belegter Eingänge`() {
        val intervall = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        fun quelle(name: String, art: AnschlussArtId) = KnotenDaten(
            art = "test.quelle",
            name = name,
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)),
        )
        val untereQuelle = quelle("unten", MathematikAnschlussArten.Zahl.id)
        val obereQuelle = quelle("oben", MathematikAnschlussArten.Zahl.id)
        val mengenZiel = KnotenDaten(
            art = "test.ziel",
            name = "Mengen-Ziel",
            anschlüsse = listOf(AnschlussDaten(name = "menge", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Menge.id)),
        )
        fun ref(knoten: KnotenDaten, name: String) = AnschlussVerweis(knoten.id, knoten.anschlüsse.single { it.name == name }.id)
        val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))
        val untereZiel = ref(intervall, "untereGrenze")
        val obereZiel = ref(intervall, "obereGrenze")
        val ersteKarte = KartenDaten(name = "Test", knoten = listOf(untereQuelle, obereQuelle, intervall, mengenZiel))

        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(ersteKarte, ref(untereQuelle, "wert"), untereZiel))
        val ersteKante = VerbindungDaten(von = ref(untereQuelle, "wert"), zu = untereZiel)
        val karteMitUntererGrenze = ersteKarte.copy(verbindungen = listOf(ersteKante))
        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(karteMitUntererGrenze, ref(obereQuelle, "wert"), obereZiel))
        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(karteMitUntererGrenze, ref(obereQuelle, "wert"), untereZiel))

        val ersatzKante = VerbindungDaten(von = ref(obereQuelle, "wert"), zu = untereZiel)
        val ersetzt = karteMitUntererGrenze.wendeAn(KartenAktion.VerbindungEinfügen(ersatzKante))
        assertEquals(listOf(ersatzKante), ersetzt.verbindungen)
        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(ersetzt, ref(intervall, "menge"), ref(mengenZiel, "menge")))
    }

    @Test
    fun `Knoten erzeugt ein abgeschlossenes Intervall und normalisiert rationale Grenzfälle`() {
        val knoten = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        val auswerter = register.finde(knoten.art)!!
        fun auswerten(unten: Long, oben: Long) = auswerter.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf("untereGrenze" to BedingterWert(RationaleZahl.von(unten)), "obereGrenze" to BedingterWert(RationaleZahl.von(oben))),
            RechenKontext(),
        )).ausgaben.getValue("menge").objekt

        assertEquals("\\left[1,3\\right]", assertIs<ReellesIntervall>(auswerten(1, 3)).zuLatex())
        assertEquals(LeereMenge, auswerten(3, 1))
        assertEquals(EndlicheMenge(setOf(RationaleZahl.von(2))), auswerten(2, 2))
    }

    @Test
    fun `Knoten erhält symbolische reelle Grenzen ohne Ordnungsannahme`() {
        val knoten = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(knoten.art)!!.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf(
                "untereGrenze" to BedingterWert(Variable("a"), werteVorrat = ReelleZahlen),
                "obereGrenze" to BedingterWert(Variable("b"), werteVorrat = ReelleZahlen),
            ),
            RechenKontext(),
        ))

        assertEquals("\\left[a,b\\right]", assertIs<ReellesIntervall>(ergebnis.ausgaben.getValue("menge").objekt).zuLatex())
        assertEquals(emptySet(), ergebnis.ausgaben.getValue("menge").annahmen)
        assertEquals(mapOf("a" to ReelleZahlen, "b" to ReelleZahlen), ergebnis.ausgaben.getValue("menge").reelleVariablen)
    }

    @Test
    fun `Knoten vereinfacht Grenzen im Rechenkontext`() {
        val knoten = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        val x = Variable("x")
        val nichtNull = Ungleichheit(x, RationaleZahl.Null)
        val ergebnis = register.finde(knoten.art)!!.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf(
                "untereGrenze" to BedingterWert(Division(x, x), setOf(nichtNull), reelleVariablen = mapOf("x" to ReelleZahlen)),
                "obereGrenze" to BedingterWert(RationaleZahl.von(2)),
            ),
            RechenKontext(setOf(nichtNull)),
        ))

        assertEquals("\\left[1,2\\right]", assertIs<ReellesIntervall>(ergebnis.ausgaben.getValue("menge").objekt).zuLatex())
    }

    @Test
    fun `Knoten lehnt nicht nachweisbar reelle Grenzen ab`() {
        val knoten = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        val auswerter = register.finde(knoten.art)!!

        assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(KnotenAuswertungsKontext(
                knoten,
                mapOf("untereGrenze" to BedingterWert(Variable("x")), "obereGrenze" to BedingterWert(RationaleZahl.von(1))),
                RechenKontext(),
            ))
        }
        assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "untereGrenze" to BedingterWert(KomplexeZahl(RationaleZahl.Null, RationaleZahl.Eins)),
                    "obereGrenze" to BedingterWert(RationaleZahl.von(1)),
                ),
                RechenKontext(),
            ))
        }
    }
}

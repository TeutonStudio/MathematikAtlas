package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.VariablenQuelle
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikRechenSystem.kern.DefinierteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Gleichheit
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Matrix
import de.TeutonStudio.MathematikRechenSystem.kern.Potenz
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante
import de.TeutonStudio.MathematikRechenSystem.kern.addition
import de.TeutonStudio.MathematikRechenSystem.kern.multiplikation
import de.TeutonStudio.MathematikRechenSystem.kern.vereinfache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuswertenUndStandardwertDarstellungTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private val kartenAuswerter = KartenAuswerter(register)

    @Test
    fun `Auswerten besitzt einen typfolgenden Term Ein und Ausgang`() {
        val vorlage = alleMathematikKnotenVorlagen().single { it.art == "mathematik.auswerten" }
        val knoten = vorlage.erzeuge(GraphPunkt.Zero)
        val eingang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }
        val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals("term", eingang.name)
        assertEquals("term", ausgang.name)
        assertEquals(MathematikAnschlussArten.Objekt.id, eingang.art)
        assertEquals(MathematikAnschlussArten.Objekt.id, ausgang.art)
        assertEquals("term", ausgang.artFolgtEingang)
        assertTrue(AussagenLogikKnotenVorlagen.alle.none { it.art == "mathematik.auswerten" })
    }

    @Test
    fun `Auswerten vereinfacht Zahl Aussage und Matrix`() {
        val knoten = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val auswerter = requireNotNull(register.finde(knoten.art))
        val potenz = Potenz(RationaleZahl.von(2), RationaleZahl.von(-1))

        val zahl = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("term" to BedingterWert(potenz)),
                RechenKontext(),
            ),
        ).ausgaben.getValue("term").objekt
        assertEquals(vereinfache(potenz), zahl)

        val aussage = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "term" to BedingterWert(
                        Gleichheit(
                            Potenz(RationaleZahl.von(2), RationaleZahl.von(2)),
                            RationaleZahl.von(4),
                        ),
                    ),
                ),
                RechenKontext(),
            ),
        ).ausgaben.getValue("term").objekt
        assertEquals(WahrheitsKonstante(true), aussage)

        val matrix = Matrix(listOf(listOf(potenz)))
        val matrixErgebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("term" to BedingterWert(matrix)),
                RechenKontext(),
            ),
        ).ausgaben.getValue("term").objekt
        assertEquals(Matrix(listOf(listOf(vereinfache(potenz)))), matrixErgebnis)
    }

    @Test
    fun `Auflösen bestimmt endliche leere universelle und symbolische Lösungsmengen`() {
        val knoten = MathematikKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        val auswerter = requireNotNull(register.finde(knoten.art))
        val x = Variable("x")
        fun löse(relation: Gleichheit) = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "relation" to BedingterWert(
                        objekt = relation,
                        variablenQuellen = listOf(VariablenQuelle(knoten.id, "x", ReelleZahlen)),
                    ),
                ),
                RechenKontext(),
            ),
        ).ausgaben.getValue("lösungsmenge").objekt

        assertEquals(
            EndlicheMenge(setOf(RationaleZahl.von(2))),
            löse(Gleichheit(multiplikation(RationaleZahl.von(2), x), RationaleZahl.von(4))),
        )
        assertEquals(
            LeereMenge,
            löse(Gleichheit(x, addition(listOf(x, RationaleZahl.Eins)))),
        )
        assertEquals(ReelleZahlen, löse(Gleichheit(x, x)))
        assertIs<DefinierteMenge>(
            löse(Gleichheit(Potenz(x, RationaleZahl.von(2)), RationaleZahl.von(2))),
        )
    }

    @Test
    fun `Auflösen akzeptiert die leere Menge als gültiges Ergebnis`() {
        val knoten = MathematikKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        val ergebnis = requireNotNull(register.finde(knoten.art)).auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "relation" to BedingterWert(
                        Gleichheit(RationaleZahl.von(1), RationaleZahl.von(2)),
                    ),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(LeereMenge, ergebnis.ausgaben.getValue("lösungsmenge").objekt)
        assertEquals(null, ergebnis.fehler)
    }

    @Test
    fun `historische Anschlussnamen bleiben lesbar`() {
        val auswertenBasis = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val historischAuswerten = auswertenBasis.copy(
            anschlüsse = auswertenBasis.anschlüsse.map { anschluss ->
                when (anschluss.richtung) {
                    AnschlussRichtung.Eingang ->
                        anschluss.copy(name = "objekt", art = MathematikAnschlussArten.Objekt.id)
                    AnschlussRichtung.Ausgang ->
                        anschluss.copy(name = "wert", art = MathematikAnschlussArten.Objekt.id)
                    AnschlussRichtung.Neutral -> anschluss
                }
            },
        )
        val auswertenErgebnis = requireNotNull(register.finde(historischAuswerten.art)).auswerten(
            KnotenAuswertungsKontext(
                historischAuswerten,
                mapOf("objekt" to BedingterWert(RationaleZahl.von(2))),
                RechenKontext(),
            ),
        )
        assertEquals(setOf("wert"), auswertenErgebnis.ausgaben.keys)

        val auflösenBasis = MathematikKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        val historischAuflösen = auflösenBasis.copy(
            anschlüsse = auflösenBasis.anschlüsse.map { anschluss ->
                when (anschluss.richtung) {
                    AnschlussRichtung.Eingang -> anschluss.copy(name = "gleichung")
                    AnschlussRichtung.Ausgang -> anschluss.copy(name = "lösungen")
                    AnschlussRichtung.Neutral -> anschluss
                }
            },
        )
        val auflösenErgebnis = requireNotNull(register.finde(historischAuflösen.art)).auswerten(
            KnotenAuswertungsKontext(
                historischAuflösen,
                mapOf(
                    "gleichung" to BedingterWert(
                        Gleichheit(RationaleZahl.von(1), RationaleZahl.von(2)),
                    ),
                ),
                RechenKontext(),
            ),
        )
        assertEquals(setOf("lösungen"), auflösenErgebnis.ausgaben.keys)
        assertEquals(LeereMenge, auflösenErgebnis.ausgaben.getValue("lösungen").objekt)
    }

    @Test
    fun `Standardwerte erzeugen eine operative LaTeX Darstellung`() {
        val potenz = MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf(
                "standardwert.basis" to "2",
                "standardwert.exponent" to "-1",
            ),
        )
        val ergebnis = kartenAuswerter.auswerten(
            KartenDaten(name = "Standardwert-LaTeX", knoten = listOf(potenz)),
        )
        val wert = ergebnis.knoten.getValue(potenz.id).ausgaben.getValue("wert")

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        assertEquals("\\left(2\\right)^{-1}", wert.anzeigeLatex())
    }
}

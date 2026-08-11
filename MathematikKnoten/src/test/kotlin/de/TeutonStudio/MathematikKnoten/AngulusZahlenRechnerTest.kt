package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AngulusZahlenRechnerTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)

    @Test
    fun `Sinus konsumiert Angulus statt nackter Zahl`() {
        val knoten = konfiguriereZahlenRechner(basis, UniversellerZahlenOperator.SINUS)
        val eingang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }

        assertContains(eingang.zulässigeArten, MathematikAnschlussArten.Angulus.id)
        assertContains(eingang.zulässigeArten, MathematikAnschlussArten.Methode.id)
        assertFalse(MathematikAnschlussArten.Zahl.id in eingang.zulässigeArten)
    }

    @Test
    fun `Arcus Sinus liefert Angulus Anschluss und Wert`() {
        val knoten = konfiguriereZahlenRechner(basis, UniversellerZahlenOperator.ARCSINUS)
        val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
        assertEquals(MathematikAnschlussArten.Angulus.id, ausgang.art)

        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("a" to BedingterWert(RationaleZahl.Null)),
                rechenKontext = RechenKontext(),
            ),
        )
        val winkel = assertIs<Angulus>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(AngulusEinheit.RADIAN, winkel.einheit)
    }

    @Test
    fun `Trigonometrie normalisiert Grad Angulus auf Radian`() {
        val knoten = konfiguriereZahlenRechner(basis, UniversellerZahlenOperator.SINUS)
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "a" to BedingterWert(
                        Angulus(RationaleZahl.von(90), AngulusEinheit.GRAD, listOf("x", "y")),
                    ),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val sinus = assertIs<Sinus>(ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue("\\pi" in sinus.argument.zuLatex())
        assertTrue(ergebnis.warnungen.any { "Radian" in it })
    }

    @Test
    fun `erweiterte Trigonometrie wird auf Angulus normalisiert`() {
        val tangens = konfiguriereErweitertenZahlenRechnerMitAngulus(basis, ErweiterterZahlenOperator.TANGENS)
        val tangensEingang = tangens.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }
        assertContains(tangensEingang.zulässigeArten, MathematikAnschlussArten.Angulus.id)
        assertFalse(MathematikAnschlussArten.Zahl.id in tangensEingang.zulässigeArten)

        val arctan = konfiguriereErweitertenZahlenRechnerMitAngulus(basis, ErweiterterZahlenOperator.ARCTANGENS)
        val arctanAusgang = arctan.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
        assertEquals(MathematikAnschlussArten.Angulus.id, arctanAusgang.art)
    }

    @Test
    fun `Polarform akzeptiert PolarTupel Zahl Angulus`() {
        val knoten = konfiguriereZahlenRechner(
            basis,
            operator = UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
            komplexEingabe = ZAHLENRECHNER_KOMPLEX_TUPEL,
        )
        val eingang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }
        assertEquals(MathematikAnschlussArten.PolarTupel.id, eingang.art)

        val polar = Tupel(
            listOf(
                RationaleZahl.von(2),
                Angulus(RationaleZahl.von(90), AngulusEinheit.GRAD),
            ),
        )
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("tupel" to BedingterWert(polar)),
                rechenKontext = RechenKontext(),
            ),
        )
        assertIs<KomplexeZahl>(ergebnis.ausgaben.getValue("wert").objekt)
    }

    @Test
    fun `Polarform hebt Radiusmethode mit festem Winkel zu Methode`() {
        val x = Variable("x")
        val radius = Methode(
            name = "r",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val knoten = konfiguriereZahlenRechner(
            basis,
            operator = UniversellerZahlenOperator.KOMPLEX_AUS_POLAR,
            komplexEingabe = ZAHLENRECHNER_KOMPLEX_SEPARIERT,
        )
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "a" to BedingterWert(radius),
                    "b" to BedingterWert(Angulus(RationaleZahl.von(90), AngulusEinheit.GRAD)),
                ),
                rechenKontext = RechenKontext(),
            ),
        )
        val methode = assertIs<PolarKomplexMethode>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(KomplexeZahlen, methode.signatur.zielMenge)
    }
}

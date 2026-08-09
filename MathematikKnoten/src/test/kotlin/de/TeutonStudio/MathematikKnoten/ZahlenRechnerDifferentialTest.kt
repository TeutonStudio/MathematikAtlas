package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class ZahlenRechnerDifferentialTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    private fun basis(): KnotenDaten = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)

    private fun kontext(
        knoten: KnotenDaten,
        eingänge: Map<String, BedingterWert>,
    ) = KnotenAuswertungsKontext(
        knoten = knoten,
        eingänge = eingänge,
        rechenKontext = RechenKontext(),
    )

    private fun quadratMethode(): Methode {
        val x = Variable("x")
        return Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = Potenz(x, RationaleZahl.von(2)),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
    }

    @Test
    fun `Ableitungsfunktion verwendet Methoden und Ordnungsanschluss`() {
        val knoten = konfiguriereZahlenRechnerDifferential(
            basis(),
            ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION,
        )
        val eingänge = knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
        val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals(listOf("methode", "ordnung"), eingänge.map { it.name })
        assertEquals(MathematikAnschlussArten.Methode.id, eingänge[0].art)
        assertEquals(MathematikAnschlussArten.Zahl.id, eingänge[1].art)
        assertEquals(MathematikAnschlussArten.Methode.id, ausgang.art)
        assertEquals(
            ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION,
            aktuelleZahlenRechnerDifferentialErgebnisArt(knoten),
        )
    }

    @Test
    fun `erste totale Ableitungsfunktion wird als f Strich ausgegeben`() {
        val knoten = konfiguriereZahlenRechnerDifferential(
            basis(),
            ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION,
        )
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(quadratMethode()),
                    "ordnung" to BedingterWert(RationaleZahl.Eins),
                ),
            ),
        )
        val ableitung = assertIs<Methode>(ergebnis.ausgaben.getValue("wert").objekt)

        assertEquals("f'", ableitung.name)
        assertTrue(ableitung.zuLatex().contains("f'"))
        assertTrue(ergebnis.warnungen.any { it.startsWith("Zielraum:") })
    }

    @Test
    fun `mehrdimensionale totale Ableitung bleibt lineare Methode statt Gradient`() {
        val x = Variable("x")
        val y = Variable("y")
        val f = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )
        val knoten = konfiguriereZahlenRechnerDifferential(
            basis(),
            ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION,
        )
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(f),
                    "ordnung" to BedingterWert(RationaleZahl.Eins),
                ),
            ),
        )
        val ableitung = assertIs<Methode>(ergebnis.ausgaben.getValue("wert").objekt)

        assertEquals("f'", ableitung.name)
        assertTrue(ableitung.zielMenge.zuLatex().contains("\\mathcal L"))
        assertFalse(ableitung.zuLatex().contains("\\nabla"))
    }

    @Test
    fun `partielle Ableitungsfunktion behaelt partielle Notation`() {
        val x = Variable("x")
        val y = Variable("y")
        val f = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = multiplikation(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )
        val basisKnoten = konfiguriereZahlenRechnerDifferential(
            basis(),
            ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION,
        )
        val knoten = basisKnoten.copy(
            parameter = basisKnoten.parameter + mapOf(
                DIFFERENTIAL_OPERATOR_PARAMETER to DifferentialOperator.Partiell(2).operatorId,
                DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER to "2",
            ),
        )
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(f),
                    "ordnung" to BedingterWert(RationaleZahl.Eins),
                ),
            ),
        )
        val ableitung = assertIs<Methode>(ergebnis.ausgaben.getValue("wert").objekt)

        assertEquals("\\partial_{2}f", ableitung.name)
    }

    @Test
    fun `Differential ist eigenes Objekt und partielles Differential nutzt iota Einbettung`() {
        val basisDifferential = konfiguriereZahlenRechnerDifferential(
            basis(),
            ZahlenRechnerDifferentialErgebnisArt.DIFFERENTIAL,
        )
        val knoten = basisDifferential.copy(
            parameter = basisDifferential.parameter + mapOf(
                DIFFERENTIAL_OPERATOR_PARAMETER to DifferentialOperator.Partiell(1).operatorId,
                DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER to "1",
            ),
        )
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(quadratMethode()),
                    "ordnung" to BedingterWert(RationaleZahl.Eins),
                ),
            ),
        )
        val differential = assertIs<MethodenDifferential>(ergebnis.ausgaben.getValue("wert").objekt)

        assertEquals("d_{1}f", differential.zuLatex())
        assertTrue(differential.definitionsLatex().contains("\\iota_{1}"))
    }

    @Test
    fun `historischer Zahlenrechner Differentialzustand bleibt skalar`() {
        val historisch = ZahlenRechnerKnotenVorlagen.alle.single {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.DIFFERENTIAL.stabileId
        }.erzeuge(GraphPunkt.Zero)
        assertNull(aktuelleZahlenRechnerDifferentialErgebnisArt(historisch))

        val x = Variable("x")
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            kontext(
                historisch,
                mapOf("a" to BedingterWert(Potenz(x, RationaleZahl.von(2)))),
            ),
        )
        assertIs<ZahlAusdruck>(ergebnis.ausgaben.getValue("wert").objekt)
    }
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class UniversellerZahlenRechnerKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    private fun kontext(
        knoten: KnotenDaten,
        eingänge: Map<String, BedingterWert>,
    ) = KnotenAuswertungsKontext(
        knoten = knoten,
        eingänge = eingänge,
        karte = KartenDaten(name = "Test", knoten = listOf(knoten)),
        rechenKontext = RechenKontext(),
    )

    @Test
    fun `Katalog enthaelt nur noch universelle Zahlenrechnerzustaende`() {
        val vorlagen = alleMathematikKnotenVorlagen()
        assertTrue(UniversellerZahlenOperator.entries.all { operator ->
            vorlagen.any {
                it.art == ZAHLENRECHNER_ART &&
                    it.standardParameter[ZAHLENRECHNER_OPERATOR] == operator.stabileId
            }
        })
        assertTrue(vorlagen.none { it.art in historischeZahlenRechnerArten })
        assertNotNull(register.finde(ZAHLENRECHNER_ART))
    }

    @Test
    fun `Migration erhaelt Knoten Anschluss und Edge IDs`() {
        val alt = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val a = alt.anschlüsse.first { it.richtung == AnschlussRichtung.Eingang }
        val ausgang = alt.anschlüsse.first { it.richtung == AnschlussRichtung.Ausgang }
        val quelle = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val quellAusgang = quelle.anschlüsse.single()
        val edge = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, quellAusgang.id),
            zu = AnschlussVerweis(alt.id, a.id),
        )
        val karte = KartenDaten(
            name = "Migration",
            knoten = listOf(quelle, alt),
            verbindungen = listOf(edge),
        )

        val migriert = karte.migriereUniversellenZahlenRechner()
        val rechner = migriert.knoten.single { it.id == alt.id }

        assertEquals(ZAHLENRECHNER_ART, rechner.art)
        assertEquals(
            UniversellerZahlenOperator.ADDITION.stabileId,
            rechner.parameter[ZAHLENRECHNER_OPERATOR],
        )
        assertTrue(rechner.anschlüsse.any { it.id == a.id })
        assertTrue(rechner.anschlüsse.any { it.id == ausgang.id && it.name == "wert" })
        assertEquals(edge, migriert.verbindungen.single())
        assertEquals(migriert, migriert.migriereUniversellenZahlenRechner())
    }

    @Test
    fun `Division waehlt Zeilen oder Bruchnotation aus dem Nenner`() {
        val vorlage = ZahlenRechnerKnotenVorlagen.alle.first {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                UniversellerZahlenOperator.DIVISION.stabileId
        }
        val knoten = vorlage.erzeuge(GraphPunkt.Zero)
        val auswerter = register.finde(ZAHLENRECHNER_ART)!!

        val kurz = auswerter.auswerten(
            kontext(
                knoten,
                mapOf(
                    "a" to BedingterWert(
                        RationaleZahl.Eins,
                        werteVorrat = BenannteMenge("Rationale Zahlen", "\\mathbb Q"),
                    ),
                    "b" to BedingterWert(
                        RationaleZahl.von(2),
                        werteVorrat = BenannteMenge("Rationale Zahlen", "\\mathbb Q"),
                    ),
                ),
            ),
        ).ausgaben.getValue("wert")
        assertEquals("1 \\div 2", kurz.latexDarstellung)

        val lang = auswerter.auswerten(
            kontext(
                knoten,
                mapOf(
                    "a" to BedingterWert(
                        Variable("a"),
                        werteVorrat = BenannteMenge("Reelle Zahlen", "\\mathbb R"),
                    ),
                    "b" to BedingterWert(
                        addition(Variable("x"), Variable("y")),
                        werteVorrat = BenannteMenge("Reelle Zahlen", "\\mathbb R"),
                    ),
                ),
            ),
        ).ausgaben.getValue("wert")
        assertTrue(lang.latexDarstellung.orEmpty().startsWith("\\frac"))
    }

    @Test
    fun `Quaternionische Multiplikation behaelt Faktorordnung als Regel`() {
        val definition = ZahlenRechnerDefinition(
            UniversellerZahlenOperator.MULTIPLIKATION,
            ZahlenRechnerBereich.QUATERNION,
        )
        assertTrue(definition.regeln.any { "Faktorordnung" in it && "nicht kommutativ" in it })
        assertEquals("\\cdot\\vert_{\\mathbb H}", definition.latex)
    }

    @Test
    fun `Rundung und Modulo werden fuer rationale Eingaben exakt ausgewertet`() {
        assertEquals(RationaleZahl.von(-2), abrunden(RationaleZahl.von(-3, 2)))
        assertEquals(RationaleZahl.von(-1), aufrunden(RationaleZahl.von(-3, 2)))
        assertEquals(RationaleZahl.von(2), runden(RationaleZahl.von(3, 2)))
        assertEquals(RationaleZahl.von(2), modulo(RationaleZahl.von(17), RationaleZahl.von(5)))
    }

    @Test
    fun `Komplexkonstruktor akzeptiert getrennte Werte und Tupelmodus`() {
        val vorlage = ZahlenRechnerKnotenVorlagen.alle.first {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                UniversellerZahlenOperator.KOMPLEX_AUS_KARTESISCH.stabileId
        }
        val getrennt = vorlage.erzeuge(GraphPunkt.Zero)
        val auswerter = register.finde(ZAHLENRECHNER_ART)!!
        val getrenntErgebnis = auswerter.auswerten(
            kontext(
                getrennt,
                mapOf(
                    "a" to BedingterWert(RationaleZahl.von(2)),
                    "b" to BedingterWert(RationaleZahl.von(3)),
                ),
            ),
        )
        assertEquals(
            KomplexeZahl(RationaleZahl.von(2), RationaleZahl.von(3)),
            getrenntErgebnis.ausgaben.getValue("wert").objekt,
        )

        val tupel = getrennt.copy(
            parameter = getrennt.parameter +
                (ZAHLENRECHNER_KOMPLEX_EINGABE to ZAHLENRECHNER_KOMPLEX_TUPEL),
        )
        val tupelErgebnis = auswerter.auswerten(
            kontext(
                tupel,
                mapOf(
                    "tupel" to BedingterWert(
                        Tupel(listOf(RationaleZahl.von(4), RationaleZahl.von(5))),
                    ),
                ),
            ),
        )
        assertEquals(
            KomplexeZahl(RationaleZahl.von(4), RationaleZahl.von(5)),
            tupelErgebnis.ausgaben.getValue("wert").objekt,
        )
    }

    @Test
    fun `Gradwinkel kann symbolisch oder als Pi durch 180 ausgewertet werden`() {
        val winkel = Variable("x")
        assertEquals("x^{\\circ}", gradWinkelLatex(winkel, false))
        assertEquals("x \\cdot \\pi \\div 180", gradWinkelLatex(winkel, true))
        assertEquals(multiplikation(winkel, GradWinkelEinheit), gradZuBogenmass(winkel))
    }
}

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
        rechenKontext = RechenKontext(),
    )

    private fun divisionsKnoten(): KnotenDaten = ZahlenRechnerKnotenVorlagen.alle.first {
        it.standardParameter[ZAHLENRECHNER_OPERATOR] ==
            UniversellerZahlenOperator.DIVISION.stabileId
    }.erzeuge(GraphPunkt.Zero)

    @Test
    fun `Katalog zeigt einen Zahlenrechner und interner Definitionskatalog alle Operatoren`() {
        val sichtbar = alleMathematikKnotenVorlagen()
        assertEquals(1, sichtbar.count { it.art == ZAHLENRECHNER_ART })
        assertTrue(UniversellerZahlenOperator.entries.all { operator ->
            ZahlenRechnerKnotenVorlagen.alle.any {
                it.standardParameter[ZAHLENRECHNER_OPERATOR] == operator.stabileId
            }
        })
        assertTrue(sichtbar.none { it.art in historischeZahlenRechnerArten })
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
    fun `geladene historische Division bleibt bis zur Seitenwahl offen`() {
        val alt = MathematikKnotenVorlagen.Division.erzeuge(GraphPunkt.Zero)
        val ids = alt.anschlüsse.associate { it.name to it.id }
        val karte = KartenDaten(name = "Alte Division", knoten = listOf(alt))

        val migriert = karte
            .migriereUniversellenZahlenRechner()
            .migriereStrukturierteDivision()
        val offen = migriert.knoten.single()

        assertEquals(ZAHLENRECHNER_ART, offen.art)
        assertEquals("true", offen.parameter[ZAHLENRECHNER_DIVISIONSSEITE_FEHLT])
        assertNull(offen.parameter[ZAHLENRECHNER_DIVISIONSSEITE])
        assertEquals(ids.values.toSet(), offen.anschlüsse.map { it.id }.toSet())
        assertEquals(migriert, migriert.migriereStrukturierteDivision())

        val gewaehlt = konfiguriereDivisionsSeite(offen, DivisionsSeite.LINKS)
        assertEquals("links", gewaehlt.parameter[ZAHLENRECHNER_DIVISIONSSEITE])
        assertEquals("false", gewaehlt.parameter[ZAHLENRECHNER_DIVISIONSSEITE_FEHLT])
        assertEquals(offen.anschlüsse.map { it.id }, gewaehlt.anschlüsse.map { it.id })
    }

    @Test
    fun `kommutative Division wird kanonisch als Bruch dargestellt`() {
        val knoten = divisionsKnoten()
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
        assertEquals("\\frac{1}{2}", kurz.latexDarstellung)
        assertIs<Division>(kurz.objekt)

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
        assertEquals("\\frac{a}{x + y}", lang.latexDarstellung)
    }

    @Test
    fun `Quaterniondivision verwendet die gewaehlte Seite ohne Umordnung`() {
        val auswerter = register.finde(ZAHLENRECHNER_ART)!!
        val q = Variable("q")
        val r = Variable("r")
        val quaternionen = BenannteMenge("Quaternionen", "\\mathbb H")
        val eingänge = mapOf(
            "a" to BedingterWert(q, werteVorrat = quaternionen),
            "b" to BedingterWert(r, werteVorrat = quaternionen),
        )

        val rechts = auswerter.auswerten(
            kontext(
                konfiguriereDivisionsSeite(divisionsKnoten(), DivisionsSeite.RECHTS),
                eingänge,
            ),
        ).ausgaben.getValue("wert")
        val links = auswerter.auswerten(
            kontext(
                konfiguriereDivisionsSeite(divisionsKnoten(), DivisionsSeite.LINKS),
                eingänge,
            ),
        ).ausgaben.getValue("wert")

        val rechtsAusdruck = assertIs<StrukturierteDivision>(rechts.objekt)
        val linksAusdruck = assertIs<StrukturierteDivision>(links.objekt)
        assertEquals(DivisionsSeite.RECHTS, rechtsAusdruck.seite)
        assertEquals(DivisionsSeite.LINKS, linksAusdruck.seite)
        assertEquals(listOf(q, InversesElement(r)), rechtsAusdruck.alsGeordnetesProdukt().faktoren)
        assertEquals(listOf(InversesElement(r), q), linksAusdruck.alsGeordnetesProdukt().faktoren)
        assertEquals("q\\div_{R}\\,r", rechts.latexDarstellung)
        assertEquals("q\\div_{L}\\,r", links.latexDarstellung)
    }

    @Test
    fun `historisch offene Quaterniondivision fordert Inspectorwahl`() {
        val offen = divisionsKnoten().copy(
            parameter = divisionsKnoten().parameter +
                (ZAHLENRECHNER_DIVISIONSSEITE_FEHLT to "true"),
        )
        val quaternionen = BenannteMenge("Quaternionen", "\\mathbb H")
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            kontext(
                offen,
                mapOf(
                    "a" to BedingterWert(Variable("q"), werteVorrat = quaternionen),
                    "b" to BedingterWert(Variable("r"), werteVorrat = quaternionen),
                ),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("Divisionsseite fehlt"))
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

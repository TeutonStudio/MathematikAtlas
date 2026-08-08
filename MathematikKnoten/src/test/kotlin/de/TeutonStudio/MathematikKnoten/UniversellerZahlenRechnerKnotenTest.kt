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
        assertEquals(
            setOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Methode.id),
            rechner.anschlüsse.single { it.id == a.id }.zulässigeArten,
        )
        assertEquals(
            listOf(MathematikAnschlussArten.Methode.id),
            rechner.anschlüsse.single { it.id == ausgang.id }.artPriorisiertEingänge?.prioritäten,
        )
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

    @Test
    fun `R2 Zahlenfunktionen werden punktweise addiert und mit Skalaren gemischt`() {
        val x = Variable("x")
        val y = Variable("y")
        val f = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )
        val u = Variable("u")
        val v = Variable("v")
        val g = Methode(
            name = "g",
            parameter = listOf(u, v),
            vorschrift = multiplikation(u, v),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("u" to ReelleZahlen, "v" to ReelleZahlen),
        )
        val knoten = ZahlenRechnerKnotenVorlagen.alle.single {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.ADDITION.stabileId
        }.erzeuge(GraphPunkt.Zero)
        val auswerter = register.finde(ZAHLENRECHNER_ART)!!

        val summe = assertIs<Methode>(
            auswerter.auswerten(
                kontext(knoten, mapOf("a" to BedingterWert(f), "b" to BedingterWert(g))),
            ).ausgaben.getValue("wert").objekt,
        )
        assertEquals(listOf("x", "y"), summe.parameter.map { it.name })
        assertEquals(addition(addition(x, y), multiplikation(x, y)), summe.vorschrift)
        assertEquals(ReelleZahlen, summe.zielMenge)

        val gemischt = assertIs<Methode>(
            auswerter.auswerten(
                kontext(
                    knoten,
                    mapOf("a" to BedingterWert(f), "b" to BedingterWert(RationaleZahl.von(3))),
                ),
            ).ausgaben.getValue("wert").objekt,
        )
        assertEquals(addition(addition(x, y), RationaleZahl.von(3)), gemischt.vorschrift)
    }

    @Test
    fun `punktweise Zahlenfunktionen schneiden ihre Argumentbereiche komponentenweise`() {
        val x = Variable("x")
        val y = Variable("y")
        val f = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen, "y" to RationaleZahlen),
        )
        val u = Variable("u")
        val v = Variable("v")
        val g = Methode(
            name = "g",
            parameter = listOf(u, v),
            vorschrift = multiplikation(u, v),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("u" to GanzeZahlen, "v" to ReelleZahlen),
        )
        val knoten = ZahlenRechnerKnotenVorlagen.alle.single {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.ADDITION.stabileId
        }.erzeuge(GraphPunkt.Zero)

        val summe = assertIs<Methode>(
            register.finde(ZAHLENRECHNER_ART)!!.auswerten(
                kontext(knoten, mapOf("a" to BedingterWert(f), "b" to BedingterWert(g))),
            ).ausgaben.getValue("wert").objekt,
        )

        assertEquals(GanzeZahlen, summe.werteVorräte.getValue("x"))
        assertEquals(RationaleZahlen, summe.werteVorräte.getValue("y"))
        assertEquals(Tupelraum(listOf(GanzeZahlen, RationaleZahlen)), summe.effektiverWerteVorrat)
    }

    @Test
    fun `Kehrwert einer Zahlenfunktion traegt die Nichtnullbedingung in den Wertevorrat ein`() {
        val x = Variable("x")
        val f = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val knoten = ZahlenRechnerKnotenVorlagen.alle.single {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.KEHRWERT.stabileId
        }.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            kontext(knoten, mapOf("a" to BedingterWert(f))),
        )
        val kehrwert = assertIs<Methode>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(Division(RationaleZahl.Eins, x), kehrwert.vorschrift)
        assertTrue(kehrwert.methodenSignatur().werteVorrat.zuLatex().contains("\\neq 0"))
        assertTrue(ergebnis.warnungen.any { it.startsWith("Definitionsbedingung:") })
    }

    @Test
    fun `punktweise Multiplikation erhaelt quaternionische Faktorordnung`() {
        val x = Variable("x")
        val quaternionen = FundamentalerZahlbereich.QUATERNION.alsMenge()
        val f = Methode("f", listOf(x), Variable("q"), quaternionen, mapOf("x" to ReelleZahlen))
        val g = Methode("g", listOf(x), Variable("r"), quaternionen, mapOf("x" to ReelleZahlen))
        val knoten = ZahlenRechnerKnotenVorlagen.alle.single {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.MULTIPLIKATION.stabileId
        }.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            kontext(knoten, mapOf("a" to BedingterWert(f), "b" to BedingterWert(g))),
        )
        val produkt = assertIs<Methode>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(listOf(Variable("q"), Variable("r")), assertIs<Multiplikation>(produkt.vorschrift).faktoren)
        assertTrue(ergebnis.warnungen.any { "Faktorordnung" in it })
    }

    @Test
    fun `inkompatible Stelligkeit und nichtnumerische Methoden werden diagnostiziert`() {
        val x = Variable("x")
        val y = Variable("y")
        val einstellig = Methode("f", listOf(x), x, ReelleZahlen, mapOf("x" to ReelleZahlen))
        val zweistellig = Methode(
            "g",
            listOf(x, y),
            addition(x, y),
            ReelleZahlen,
            mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
        )
        val nichtNumerisch = Methode(
            "h",
            listOf(x),
            x,
            BenannteMenge("Farben", "\\mathcal F"),
            mapOf("x" to ReelleZahlen),
        )
        val knoten = ZahlenRechnerKnotenVorlagen.alle.single {
            it.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.ADDITION.stabileId
        }.erzeuge(GraphPunkt.Zero)
        val auswerter = register.finde(ZAHLENRECHNER_ART)!!

        val stelligkeitsFehler = assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(
                kontext(knoten, mapOf("a" to BedingterWert(einstellig), "b" to BedingterWert(zweistellig))),
            )
        }
        assertTrue(stelligkeitsFehler.message.orEmpty().contains("Stelligkeit"))

        val typFehler = assertFailsWith<IllegalStateException> {
            auswerter.auswerten(
                kontext(knoten, mapOf("a" to BedingterWert(einstellig), "b" to BedingterWert(nichtNumerisch))),
            )
        }
        assertTrue(typFehler.message.orEmpty().contains("Zielmenge"))
    }
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class RechnerErweiterungen410Test {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `fehlende Mengenoperatoren sind sichtbar und besitzen kanonische Notation`() {
        val sichtbar = sichtbareMengenRechnerOperatoren().toSet()
        assertContains(sichtbar, MengenRechnerOperator.POTENZMENGE)
        assertContains(sichtbar, MengenRechnerOperator.ABBILDUNGSMENGE)
        assertContains(sichtbar, MengenRechnerOperator.FOLGENMENGE)
        assertContains(sichtbar, MengenRechnerOperator.HALBFOLGENMENGE)
        assertContains(sichtbar, MengenRechnerOperator.KLASSIFIZIERTE_MENGE)
        assertEquals("\\mathcal{P}(M)", MengenRechnerOperator.POTENZMENGE.vorschauLatex())
        assertEquals("M^A", MengenRechnerOperator.ABBILDUNGSMENGE.vorschauLatex())
        assertEquals("M^{\\mathbb Z}", MengenRechnerOperator.FOLGENMENGE.vorschauLatex())
        assertEquals("M^{\\mathbb N_0}", MengenRechnerOperator.HALBFOLGENMENGE.vorschauLatex())
        assertEquals("M\\div r", MengenRechnerOperator.KLASSIFIZIERTE_MENGE.vorschauLatex())
        assertEquals(
            "\\mathop{\\Large\\times}\\limits_{i\\in I}A(i)",
            MengenRechnerOperator.ITERIERTES_KARTESISCHES_PRODUKT.vorschauLatex(),
        )
    }

    @Test
    fun `Abbildungs Folgen und Halbfolgenmenge verwenden dieselbe strukturierte Abbildungsmenge`() {
        val m = BenannteMenge("M", "M")
        val a = BenannteMenge("A", "A")
        val abbildung = assertIs<MengenRechnerErgebnis.Wert>(
            MengenRechner.erzeuge(
                MengenRechnerOperator.ABBILDUNGSMENGE,
                listOf(
                    MengenRechnerEingabe("zielmenge", m),
                    MengenRechnerEingabe("argumentmenge", a),
                ),
            ),
        ).menge
        val folge = assertIs<MengenRechnerErgebnis.Wert>(
            MengenRechner.erzeuge(MengenRechnerOperator.FOLGENMENGE, listOf(MengenRechnerEingabe("a", m))),
        ).menge
        val halbfolge = assertIs<MengenRechnerErgebnis.Wert>(
            MengenRechner.erzeuge(MengenRechnerOperator.HALBFOLGENMENGE, listOf(MengenRechnerEingabe("a", m))),
        ).menge

        assertEquals(Abbildungsmenge(m, a), abbildung)
        assertEquals(Abbildungsmenge(m, GanzeZahlen), folge)
        assertEquals(Abbildungsmenge(m, NichtnegativeGanzeZahlenSemantik.menge), halbfolge)
    }

    @Test
    fun `arithmetisches und geometrisches Mittel sind methodenfaehig`() {
        val x = Variable("x")
        val f = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)

        listOf(
            ErweiterterZahlenOperator.ARITHMETISCHES_MITTEL,
            ErweiterterZahlenOperator.GEOMETRISCHES_MITTEL,
        ).forEach { operator ->
            val knoten = konfiguriereErweitertenZahlenRechner(basis, operator)
            val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
                KnotenAuswertungsKontext(
                    knoten = knoten,
                    eingänge = mapOf(
                        "a" to BedingterWert(f),
                        "b" to BedingterWert(RationaleZahl.von(4)),
                    ),
                    rechenKontext = RechenKontext(),
                ),
            )
            assertIs<Methode>(ergebnis.ausgaben.getValue("wert").objekt)
        }
    }

    @Test
    fun `Stammfunktion liefert Methode mit variablem reellen Intervallende`() {
        val t = Variable("t")
        val f = Methode(
            name = "f",
            parameter = listOf(t),
            vorschrift = t,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("t" to ReelleZahlen),
        )
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereErweitertenZahlenRechner(basis, ErweiterterZahlenOperator.STAMMFUNKTION)
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "methode" to BedingterWert(f),
                    "startwert" to BedingterWert(RationaleZahl.Null),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val stammfunktion = assertIs<Methode>(ergebnis.ausgaben.getValue("stammfunktion").objekt)
        assertEquals(listOf("x"), stammfunktion.parameter.map { it.name })
        val integral = assertIs<StrukturiertesIntegral>(stammfunktion.vorschrift)
        val intervall = assertIs<ReellesIntervall>(integral.bereich.komponenten.single())
        assertEquals("x", assertIs<Variable>(intervall.rechts).name)
    }

    @Test
    fun `Polynomvorschau verwendet Koeffizienten und Potenzvektor`() {
        assertEquals(
            "(c_k)_k\\cdot(x^k)_k\\text{ für }k\\in\\mathbb N_0^{\\leq n}",
            ErweiterterZahlenOperator.POLYNOM.vorschauLatex,
        )
    }

    @Test
    fun `alle sechs Rechnerfamilien besitzen Methodenvertrag`() {
        val strukturVorlagen = listOf(
            StrukturFormelRechnerVorlagen.Aussagesatz,
            StrukturFormelRechnerVorlagen.Vektor,
            StrukturFormelRechnerVorlagen.Matrix,
            StrukturFormelRechnerVorlagen.Tensor,
            MengenRechnerKnotenVorlagen.standard,
        )
        strukturVorlagen.forEach { vorlage ->
            val knoten = normalisiereRechnerMethodenAnschluesse(vorlage.erzeuge(GraphPunkt.Zero))
            val methodenfaehige = knoten.anschlüsse.filter {
                it.richtung == AnschlussRichtung.Eingang && MathematikAnschlussArten.Methode.id in it.zulässigeArten
            }
            assertTrue(methodenfaehige.isNotEmpty(), "${vorlage.art} besitzt keinen methodenfähigen Werteingang")
            val ausgang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
            assertNotNull(ausgang, "${vorlage.art} besitzt keinen Ausgang")
            assertNotNull(ausgang.artPriorisiertEingänge, "${vorlage.art} priorisiert den Methodenausgang nicht")
        }

        val zahlen = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        assertTrue(zahlen.anschlüsse.any {
            it.richtung == AnschlussRichtung.Eingang && MathematikAnschlussArten.Methode.id in it.zulässigeArten
        })
        assertTrue(zahlen.anschlüsse.any {
            it.richtung == AnschlussRichtung.Ausgang && it.artPriorisiertEingänge != null
        })
    }

    @Test
    fun `Aussagerechner hebt Praedikate wieder zu Praedikat an`() {
        val x = Variable("x")
        fun praedikat(name: String) = Methode(
            name = name,
            parameter = listOf(x),
            vorschrift = UnentscheidbareAussage("$name(x)", "Testprädikat"),
            zielMenge = WahrheitsMenge,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val knoten = normalisiereRechnerMethodenAnschluesse(
            konfiguriereStrukturRechner(
                StrukturFormelRechnerVorlagen.Aussagesatz.erzeuge(GraphPunkt.Zero),
                StrukturRechnerKnotenFamilie.AUSSAGESATZ,
                "aussage.konjunktion",
            ),
        )
        val ergebnis = register.finde(AussagenSatzRechner.KNOTEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "a" to BedingterWert(praedikat("P")),
                    "b" to BedingterWert(praedikat("Q")),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val p = assertIs<Methode>(ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue(p.istPrädikat())
        assertEquals(WahrheitsMenge, p.zielMenge)
    }
}
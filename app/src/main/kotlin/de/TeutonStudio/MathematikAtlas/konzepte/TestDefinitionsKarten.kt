package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.ErweiterteMathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

/** Ausführbare, unveränderliche Konzeptkarten für die derzeit definierten Grundoperatoren. */
object TestDefinitionsKarten {
    val alle: List<KonzeptDefinition> by lazy {
        listOf(
            zahlenKonzept(),
            additionsKonzept(),
            subtraktionsKonzept(),
            multiplikationsKonzept(),
            kehrwertKonzept(),
            divisionsKonzept(),
        )
    }

    fun finde(id: KonzeptId): KonzeptDefinition? = alle.firstOrNull { it.id == id }

    fun fürKnoten(knoten: KnotenDaten): KonzeptDefinition? = when (knoten.art) {
        "mathematik.zahl" -> finde(KonzeptId("zahl"))
        "mathematik.addition" -> finde(KonzeptId("addition"))
        "mathematik.subtraktion" -> finde(KonzeptId("subtraktion"))
        "mathematik.multiplikation" -> finde(KonzeptId("multiplikation"))
        "mathematik.kehrwert" -> finde(KonzeptId("kehrwert"))
        "mathematik.division" -> finde(KonzeptId("division"))
        else -> null
    }

    private fun zahlenKonzept(): KonzeptDefinition {
        val nullMenge = testKnoten(MathematikKnotenVorlagen.EndlicheMenge, "zahl-null-leermenge", 40f, 40f, mapOf("elemente" to ""))
        val plusOperator = testKnoten(MathematikKnotenVorlagen.AllgemeinerParameter, "zahl-plus-operator", 40f, 210f, mapOf("name" to "+"))
        val plusEinzelmenge = testKnoten(MathematikKnotenVorlagen.Einzelmenge, "zahl-plus-einzelmenge", 350f, 210f)
        val plusMächtigkeit = testKnoten(MathematikKnotenVorlagen.Mächtigkeit, "zahl-plus-maechtigkeit", 670f, 210f)
        val minusOperator = testKnoten(MathematikKnotenVorlagen.AllgemeinerParameter, "zahl-minus-operator", 40f, 390f, mapOf("name" to "−"))
        val minusEinzelmenge = testKnoten(MathematikKnotenVorlagen.Einzelmenge, "zahl-minus-einzelmenge", 350f, 390f)
        val minusMächtigkeit = testKnoten(MathematikKnotenVorlagen.Mächtigkeit, "zahl-minus-maechtigkeit", 670f, 390f)
        val definition = testKarte(
            "konzept-zahl-definition",
            "0 = ∅, 1 = {+}, −1 = {−}",
            listOf(nullMenge, plusOperator, plusEinzelmenge, plusMächtigkeit, minusOperator, minusEinzelmenge, minusMächtigkeit),
            listOf(
                testVerbindung("zahl-definition-1", plusOperator, "wert", plusEinzelmenge, "element"),
                testVerbindung("zahl-definition-2", plusEinzelmenge, "menge", plusMächtigkeit, "menge"),
                testVerbindung("zahl-definition-3", minusOperator, "wert", minusEinzelmenge, "element"),
                testVerbindung("zahl-definition-4", minusEinzelmenge, "menge", minusMächtigkeit, "menge"),
            ),
        )
        val positiverNachfolger = nachfolgerKarte("positiv", "+", "Natürlicher positiver Nachfolger")
        val negativerNachfolger = nachfolgerKarte("negativ", "−", "Natürlicher negativer Nachfolger")
        val zahlbereiche = zahlbereicheKarte()
        return KonzeptDefinition(
            id = KonzeptId("zahl"),
            name = "Zahl",
            beschreibung = "Alternative ganzzahlige Konstruktion: 0 ist die leere Menge; +1 und −1 sind gleichmächtige Einzelmengen ihrer Richtungsoperatoren. Wiederholte Nachfolger erzeugen den positiven beziehungsweise negativen Ast von ℤ.",
            pfad = listOf("Grundlagen", "Zahlen"),
            tags = setOf("Zahl", "Ganze Zahlen", "Nachfolger", "Mächtigkeit", "Peano-Alternative"),
            knotenArten = setOf("mathematik.zahl"),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, definition),
                KonzeptReiter("positiver-nachfolger", "Positiver Nachfolger", KonzeptReiterRolle.Spezialfall, positiverNachfolger),
                KonzeptReiter("negativer-nachfolger", "Negativer Nachfolger", KonzeptReiterRolle.Spezialfall, negativerNachfolger),
                KonzeptReiter("zahlbereiche", "ℕ, ℕ₀ und ℤ", KonzeptReiterRolle.Äquivalenz, zahlbereiche),
            ),
            navigation = standardNavigation("definition", definition.knoten),
        )
    }

    private fun nachfolgerKarte(id: String, operator: String, name: String): KartenDaten {
        val eingang = kartenEingang("zahl-$id-eingang", "x", MathematikAnschlussArten.Menge.id, 30f, 120f)
        val operatorKnoten = testKnoten(MathematikKnotenVorlagen.AllgemeinerParameter, "zahl-$id-operator", 30f, 310f, mapOf("name" to operator))
        val einzelmenge = testKnoten(MathematikKnotenVorlagen.Einzelmenge, "zahl-$id-einzelmenge", 340f, 310f)
        val vereinigung = binärerKnoten(MathematikKnotenVorlagen.Vereinigung, "zahl-$id-vereinigung", 680f, 180f)
        val ausgang = kartenAusgang("zahl-$id-ausgang", "nachfolger", MathematikAnschlussArten.Menge.id, 1010f, 180f)
        return testKarte(
            "konzept-zahl-$id-nachfolger",
            name,
            listOf(eingang, operatorKnoten, einzelmenge, vereinigung, ausgang),
            listOf(
                testVerbindung("zahl-$id-1", operatorKnoten, "wert", einzelmenge, "element"),
                testVerbindung("zahl-$id-2", eingang, "wert", vereinigung, "a"),
                testVerbindung("zahl-$id-3", einzelmenge, "menge", vereinigung, "b"),
                testVerbindung("zahl-$id-4", vereinigung, "menge", ausgang, "wert"),
            ),
        )
    }

    private fun zahlbereicheKarte(): KartenDaten {
        val n = testKnoten(MathematikKnotenVorlagen.NatürlicheZahlen, "zahlbereiche-n", 30f, 60f)
        val null = testKnoten(MathematikKnotenVorlagen.Zahl, "zahlbereiche-null", 30f, 230f, mapOf("wert" to "0"))
        val nullMenge = testKnoten(MathematikKnotenVorlagen.Einzelmenge, "zahlbereiche-nullmenge", 300f, 230f)
        val n0 = binärerKnoten(MathematikKnotenVorlagen.Vereinigung, "zahlbereiche-n0", 610f, 150f)
        val z = testKnoten(MathematikKnotenVorlagen.GanzeZahlen, "zahlbereiche-z", 940f, 150f)
        val nTeilZ = testKnoten(MathematikKnotenVorlagen.TeilOderGleichmenge, "zahlbereiche-n-teil-z", 1260f, 60f)
        val n0TeilZ = testKnoten(MathematikKnotenVorlagen.TeilOderGleichmenge, "zahlbereiche-n0-teil-z", 1260f, 250f)
        return testKarte(
            "konzept-zahl-zahlbereiche",
            "ℕ ⊆ ℤ und ℕ₀ ⊆ ℤ",
            listOf(n, null, nullMenge, n0, z, nTeilZ, n0TeilZ),
            listOf(
                testVerbindung("zahlbereiche-1", null, "wert", nullMenge, "element"),
                testVerbindung("zahlbereiche-2", n, "menge", n0, "a"),
                testVerbindung("zahlbereiche-3", nullMenge, "menge", n0, "b"),
                testVerbindung("zahlbereiche-4", n, "menge", nTeilZ, "links"),
                testVerbindung("zahlbereiche-5", z, "menge", nTeilZ, "rechts"),
                testVerbindung("zahlbereiche-6", n0, "menge", n0TeilZ, "links"),
                testVerbindung("zahlbereiche-7", z, "menge", n0TeilZ, "rechts"),
            ),
        )
    }

    private fun additionsKonzept(): KonzeptDefinition {
        val a = testKnoten(MathematikKnotenVorlagen.Zahl, "addition-a", 40f, 55f, mapOf("wert" to "2"))
        val b = testKnoten(MathematikKnotenVorlagen.Zahl, "addition-b", 40f, 215f, mapOf("wert" to "3"))
        val addition = binärerKnoten(MathematikKnotenVorlagen.Addition, "addition-operator", 330f, 135f)
        val definition = testKarte(
            "konzept-addition-definition",
            "Addition",
            listOf(a, b, addition),
            listOf(
                testVerbindung("addition-a-kante", a, "wert", addition, "a"),
                testVerbindung("addition-b-kante", b, "wert", addition, "b"),
            ),
        )
        val x = testKnoten(MathematikKnotenVorlagen.Zahl, "addition-null-x", 35f, 55f, mapOf("wert" to "7"))
        val nullKnoten = testKnoten(MathematikKnotenVorlagen.Zahl, "addition-null-null", 35f, 225f, mapOf("wert" to "0"))
        val plus = binärerKnoten(MathematikKnotenVorlagen.Addition, "addition-null-plus", 330f, 135f)
        val gleich = testKnoten(MathematikKnotenVorlagen.Gleichheit, "addition-null-gleich", 640f, 135f)
        val sonderfall = testKarte(
            "konzept-addition-null",
            "Neutrales Element der Addition",
            listOf(x, nullKnoten, plus, gleich),
            listOf(
                testVerbindung("addition-null-1", x, "wert", plus, "a"),
                testVerbindung("addition-null-2", nullKnoten, "wert", plus, "b"),
                testVerbindung("addition-null-3", plus, "wert", gleich, "links"),
                testVerbindung("addition-null-4", x, "wert", gleich, "rechts"),
            ),
        )
        return KonzeptDefinition(
            id = KonzeptId("addition"),
            name = "Addition",
            beschreibung = "Verknüpft zwei Zahlen; auf ℤ verbindet sie Schritte desselben oder entgegengesetzten Nachfolgerasts.",
            pfad = listOf("Algebra", "Verknüpfungen"),
            tags = setOf("Addition", "Summe", "Kommutativ"),
            knotenArten = setOf("mathematik.addition"),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, definition),
                KonzeptReiter("neutral-null", "Sonderfall 0", KonzeptReiterRolle.Spezialfall, sonderfall),
            ),
            navigation = standardNavigation("definition", definition.knoten) + standardNavigation("neutral-null", sonderfall.knoten),
            erkundungsFreigaben = listOf(
                KonzeptErkundungsFreigabe("definition", a.id, "wert", "Erster Summand"),
                KonzeptErkundungsFreigabe("definition", b.id, "wert", "Zweiter Summand"),
                KonzeptErkundungsFreigabe("neutral-null", x.id, "wert", "Ausgangswert"),
                KonzeptErkundungsFreigabe("neutral-null", nullKnoten.id, "wert", "Testwert statt 0"),
            ),
        )
    }

    private fun subtraktionsKonzept(): KonzeptDefinition {
        val a = testKnoten(MathematikKnotenVorlagen.Zahl, "subtraktion-a", 35f, 60f, mapOf("wert" to "7"))
        val b = testKnoten(MathematikKnotenVorlagen.Zahl, "subtraktion-b", 35f, 230f, mapOf("wert" to "3"))
        val minus = testKnoten(ErweiterteMathematikKnotenVorlagen.Subtraktion, "subtraktion-operator", 370f, 145f)
        val definition = testKarte(
            "konzept-subtraktion-definition",
            "Subtraktion",
            listOf(a, b, minus),
            listOf(
                testVerbindung("subtraktion-1", a, "wert", minus, "minuend"),
                testVerbindung("subtraktion-2", b, "wert", minus, "subtrahend"),
            ),
        )
        val minusEins = testKnoten(MathematikKnotenVorlagen.Zahl, "subtraktion-minus-eins", 35f, 390f, mapOf("wert" to "-1"))
        val produkt = binärerKnoten(MathematikKnotenVorlagen.Multiplikation, "subtraktion-negation", 370f, 300f)
        val addition = binärerKnoten(MathematikKnotenVorlagen.Addition, "subtraktion-addition", 700f, 180f)
        val ganzzahl = testKarte(
            "konzept-subtraktion-ganzzahl",
            "a − b = a + (−1) · b",
            listOf(a.copy(id = KnotenId("subtraktion-z-a")), b.copy(id = KnotenId("subtraktion-z-b")), minusEins, produkt, addition),
            listOf(
                testVerbindung("subtraktion-z-1", b.copy(id = KnotenId("subtraktion-z-b")), "wert", produkt, "a"),
                testVerbindung("subtraktion-z-2", minusEins, "wert", produkt, "b"),
                testVerbindung("subtraktion-z-3", a.copy(id = KnotenId("subtraktion-z-a")), "wert", addition, "a"),
                testVerbindung("subtraktion-z-4", produkt, "wert", addition, "b"),
            ),
        )
        return KonzeptDefinition(
            id = KonzeptId("subtraktion"),
            name = "Subtraktion",
            beschreibung = "Auf ℤ ist die Subtraktion Addition des additiv Inversen und wechselt bei positiven Subtrahenden in Richtung des negativen Nachfolgerasts.",
            pfad = listOf("Algebra", "Verknüpfungen"),
            tags = setOf("Subtraktion", "Differenz", "Additives Inverses", "Ganze Zahlen"),
            knotenArten = setOf("mathematik.subtraktion"),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, definition),
                KonzeptReiter("ganze-zahlen", "Sonderfall ℤ", KonzeptReiterRolle.Spezialfall, ganzzahl),
            ),
            navigation = standardNavigation("definition", definition.knoten) + standardNavigation("ganze-zahlen", ganzzahl.knoten),
            erkundungsFreigaben = listOf(
                KonzeptErkundungsFreigabe("definition", a.id, "wert", "Minuend"),
                KonzeptErkundungsFreigabe("definition", b.id, "wert", "Subtrahend"),
            ),
        )
    }

    private fun multiplikationsKonzept(): KonzeptDefinition {
        val a = testKnoten(MathematikKnotenVorlagen.Zahl, "multiplikation-a", 40f, 55f, mapOf("wert" to "4"))
        val b = testKnoten(MathematikKnotenVorlagen.Zahl, "multiplikation-b", 40f, 215f, mapOf("wert" to "5"))
        val mal = binärerKnoten(MathematikKnotenVorlagen.Multiplikation, "multiplikation-operator", 330f, 135f)
        val karte = testKarte(
            "konzept-multiplikation-definition",
            "Multiplikation",
            listOf(a, b, mal),
            listOf(
                testVerbindung("multiplikation-1", a, "wert", mal, "a"),
                testVerbindung("multiplikation-2", b, "wert", mal, "b"),
            ),
        )
        return KonzeptDefinition(
            id = KonzeptId("multiplikation"),
            name = "Multiplikation",
            beschreibung = "Multiplikation zweier Faktoren; Vorzeichen folgen der Kombination positiver und negativer Nachfolgeräste.",
            pfad = listOf("Algebra", "Verknüpfungen"),
            tags = setOf("Multiplikation", "Produkt", "Ganze Zahlen"),
            knotenArten = setOf("mathematik.multiplikation"),
            reiter = listOf(KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, karte)),
            navigation = standardNavigation("definition", karte.knoten),
            erkundungsFreigaben = listOf(
                KonzeptErkundungsFreigabe("definition", a.id, "wert", "Erster Faktor"),
                KonzeptErkundungsFreigabe("definition", b.id, "wert", "Zweiter Faktor"),
            ),
        )
    }

    private fun kehrwertKonzept(): KonzeptDefinition {
        val zahl = testKnoten(MathematikKnotenVorlagen.Zahl, "kehrwert-zahl", 40f, 110f, mapOf("wert" to "4"))
        val kehrwert = testKnoten(MathematikKnotenVorlagen.Kehrwert, "kehrwert-operator", 340f, 110f)
        val karte = testKarte(
            "konzept-kehrwert-definition",
            "Kehrwert als Potenz −1",
            listOf(zahl, kehrwert),
            listOf(testVerbindung("kehrwert-1", zahl, "wert", kehrwert, "zahl")),
        )
        return KonzeptDefinition(
            id = KonzeptId("kehrwert"),
            name = "Kehrwert",
            beschreibung = "Der Kehrwert von x ist x⁻¹ und setzt x ≠ 0 voraus.",
            pfad = listOf("Algebra", "Verknüpfungen"),
            tags = setOf("Kehrwert", "Inverse", "Potenz", "Division"),
            knotenArten = setOf("mathematik.kehrwert"),
            reiter = listOf(KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, karte)),
            navigation = standardNavigation("definition", karte.knoten),
            erkundungsFreigaben = listOf(KonzeptErkundungsFreigabe("definition", zahl.id, "wert", "Zahl ungleich 0")),
        )
    }

    private fun divisionsKonzept(): KonzeptDefinition {
        val definition = divisionsDefinitionsKarte()
        val reellReell = einfacheKehrwertDivision("division-reell-reell", "Reelle Division", "Zähler", "Nenner")
        val komplexReell = einfacheKehrwertDivision("division-komplex-reell", "Komplex durch reell", "Dividend", "Divisor")
        val komplexerDivisor = komplexeDivisionMitKonjugierter()
        val reiter = listOf(
            KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, definition),
            KonzeptReiter("reell-reell", "Reell durch reell", KonzeptReiterRolle.Spezialfall, reellReell),
            KonzeptReiter("komplex-reell", "Komplex durch reell", KonzeptReiterRolle.Spezialfall, komplexReell),
            KonzeptReiter("komplexer-divisor", "Komplexer Divisor", KonzeptReiterRolle.Spezialfall, komplexerDivisor),
        )
        return KonzeptDefinition(
            id = KonzeptId("division"),
            name = "Division",
            beschreibung = "Division als Multiplikation mit dem Kehrwert, einschließlich Nullfall und komplexer Rationalisierung.",
            pfad = listOf("Algebra", "Verknüpfungen"),
            tags = setOf("Division", "Quotient", "Kehrwert", "Konjugierte"),
            knotenArten = setOf("mathematik.division"),
            reiter = reiter,
            navigation = buildMap { reiter.forEach { putAll(standardNavigation(it.id, it.karte.knoten)) } },
        )
    }

    private fun divisionsDefinitionsKarte(): KartenDaten {
        val zähler = kartenEingang("division-definition-zähler", "Zähler x", MathematikAnschlussArten.Zahl.id, 30f, 70f)
        val nenner = kartenEingang("division-definition-nenner", "Nenner y", MathematikAnschlussArten.Zahl.id, 30f, 240f)
        val nullErsatz = kartenEingang("division-definition-null-ersatz", "falls Nenner null", MathematikAnschlussArten.Zahl.id, 420f, 610f)
        val nullKnoten = testKnoten(MathematikKnotenVorlagen.Zahl, "division-definition-null", 40f, 430f, mapOf("wert" to "0"))
        val gleichheit = testKnoten(MathematikKnotenVorlagen.Gleichheit, "division-definition-gleich", 360f, 330f)
        val kehrwert = testKnoten(MathematikKnotenVorlagen.Kehrwert, "division-definition-kehrwert", 360f, 190f)
        val produkt = binärerKnoten(MathematikKnotenVorlagen.Multiplikation, "division-definition-produkt", 670f, 105f)
        val fall = testKnoten(MathematikKnotenVorlagen.Fall, "division-definition-fall", 970f, 270f)
        val ausgang = kartenAusgang("division-definition-ausgang", "ergebnis", MathematikAnschlussArten.Zahl.id, 1320f, 290f)
        return testKarte(
            "konzept-division-definition",
            "Divisionskarte",
            listOf(zähler, nenner, nullErsatz, nullKnoten, gleichheit, kehrwert, produkt, fall, ausgang),
            listOf(
                testVerbindung("division-definition-1", nenner, "wert", gleichheit, "links"),
                testVerbindung("division-definition-2", nullKnoten, "wert", gleichheit, "rechts"),
                testVerbindung("division-definition-3", nenner, "wert", kehrwert, "zahl"),
                testVerbindung("division-definition-4", zähler, "wert", produkt, "a"),
                testVerbindung("division-definition-5", kehrwert, "wert", produkt, "b"),
                testVerbindung("division-definition-6", nullErsatz, "wert", fall, "wahr"),
                testVerbindung("division-definition-7", gleichheit, "aussage", fall, "aussage"),
                testVerbindung("division-definition-8", produkt, "wert", fall, "lüge"),
                testVerbindung("division-definition-9", fall, "wert", ausgang, "wert"),
            ),
        )
    }

    private fun einfacheKehrwertDivision(id: String, name: String, linkerName: String, rechterName: String): KartenDaten {
        val links = kartenEingang("$id-links", linkerName, MathematikAnschlussArten.Zahl.id, 40f, 70f)
        val rechts = kartenEingang("$id-rechts", rechterName, MathematikAnschlussArten.Zahl.id, 40f, 250f)
        val kehrwert = testKnoten(MathematikKnotenVorlagen.Kehrwert, "$id-kehrwert", 370f, 250f)
        val produkt = binärerKnoten(MathematikKnotenVorlagen.Multiplikation, "$id-produkt", 690f, 150f)
        val ausgang = kartenAusgang("$id-ausgang", "ergebnis", MathematikAnschlussArten.Zahl.id, 1010f, 150f)
        return testKarte(
            "konzept-$id",
            name,
            listOf(links, rechts, kehrwert, produkt, ausgang),
            listOf(
                testVerbindung("$id-1", rechts, "wert", kehrwert, "zahl"),
                testVerbindung("$id-2", links, "wert", produkt, "a"),
                testVerbindung("$id-3", kehrwert, "wert", produkt, "b"),
                testVerbindung("$id-4", produkt, "wert", ausgang, "wert"),
            ),
        )
    }

    private fun komplexeDivisionMitKonjugierter(): KartenDaten {
        val dividend = kartenEingang("division-komplex-dividend", "Dividend", MathematikAnschlussArten.Zahl.id, 30f, 80f)
        val divisor = kartenEingang("division-komplex-divisor", "Divisor", MathematikAnschlussArten.Zahl.id, 30f, 310f)
        val konjugierte = testKnoten(MathematikKnotenVorlagen.Konjugierte, "division-komplex-konjugierte", 350f, 310f)
        val zählerProdukt = binärerKnoten(MathematikKnotenVorlagen.Multiplikation, "division-komplex-zähler-produkt", 660f, 80f)
        val nennerProdukt = binärerKnoten(MathematikKnotenVorlagen.Multiplikation, "division-komplex-nenner-produkt", 660f, 330f)
        val kehrwert = testKnoten(MathematikKnotenVorlagen.Kehrwert, "division-komplex-kehrwert", 970f, 330f)
        val ergebnisProdukt = binärerKnoten(MathematikKnotenVorlagen.Multiplikation, "division-komplex-ergebnis-produkt", 1260f, 180f)
        val ausgang = kartenAusgang("division-komplex-ausgang", "ergebnis", MathematikAnschlussArten.Zahl.id, 1580f, 180f)
        return testKarte(
            "konzept-division-komplexer-divisor",
            "Division mit komplexem Divisor",
            listOf(dividend, divisor, konjugierte, zählerProdukt, nennerProdukt, kehrwert, ergebnisProdukt, ausgang),
            listOf(
                testVerbindung("division-komplex-1", divisor, "wert", konjugierte, "zahl"),
                testVerbindung("division-komplex-2", dividend, "wert", zählerProdukt, "a"),
                testVerbindung("division-komplex-3", konjugierte, "wert", zählerProdukt, "b"),
                testVerbindung("division-komplex-4", divisor, "wert", nennerProdukt, "a"),
                testVerbindung("division-komplex-5", konjugierte, "wert", nennerProdukt, "b"),
                testVerbindung("division-komplex-6", nennerProdukt, "wert", kehrwert, "zahl"),
                testVerbindung("division-komplex-7", zählerProdukt, "wert", ergebnisProdukt, "a"),
                testVerbindung("division-komplex-8", kehrwert, "wert", ergebnisProdukt, "b"),
                testVerbindung("division-komplex-9", ergebnisProdukt, "wert", ausgang, "wert"),
            ),
        )
    }

    private fun standardNavigation(reiterId: String, knoten: Iterable<KnotenDaten>): Map<KonzeptKnotenSchlüssel, KonzeptId> =
        knoten.mapNotNull { daten ->
            val ziel = when (daten.art) {
                "mathematik.zahl" -> KonzeptId("zahl")
                "mathematik.addition" -> KonzeptId("addition")
                "mathematik.subtraktion" -> KonzeptId("subtraktion")
                "mathematik.multiplikation" -> KonzeptId("multiplikation")
                "mathematik.kehrwert" -> KonzeptId("kehrwert")
                "mathematik.division" -> KonzeptId("division")
                else -> null
            }
            ziel?.let { KonzeptKnotenSchlüssel(reiterId, daten.id) to it }
        }.toMap()

    private fun binärerKnoten(vorlage: KnotenVorlage, id: String, x: Float, y: Float): KnotenDaten =
        testKnoten(vorlage, id, x, y).let { erzeugt ->
            erzeugt.copy(anschlüsse = erzeugt.anschlüsse.filter { it.name in setOf("a", "b", "wert", "menge") })
        }

    private fun kartenEingang(id: String, name: String, art: AnschlussArtId, x: Float, y: Float): KnotenDaten =
        testKnoten(MathematikKnotenVorlagen.KartenEingang, id, x, y, mapOf("name" to name)).copy(
            anschlüsse = listOf(AnschlussDaten(
                id = AnschlussId("$id-wert-0"),
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = art,
            )),
        )

    private fun kartenAusgang(id: String, name: String, art: AnschlussArtId, x: Float, y: Float): KnotenDaten =
        testKnoten(MathematikKnotenVorlagen.KartenAusgang, id, x, y, mapOf("name" to name)).copy(
            anschlüsse = listOf(AnschlussDaten(
                id = AnschlussId("$id-wert-0"),
                name = "wert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = art,
            )),
        )

    private fun testKnoten(
        vorlage: KnotenVorlage,
        id: String,
        x: Float,
        y: Float,
        parameter: Map<String, String> = emptyMap(),
    ): KnotenDaten {
        val erzeugt = vorlage.erzeuge(GraphPunkt(x, y))
        return erzeugt.copy(
            id = KnotenId(id),
            parameter = erzeugt.parameter + parameter,
            anschlüsse = erzeugt.anschlüsse.map { anschluss ->
                anschluss.copy(id = AnschlussId("$id-${anschluss.name}-${anschluss.reihenfolge}"))
            },
        )
    }

    private fun testKarte(
        id: String,
        name: String,
        knoten: List<KnotenDaten>,
        verbindungen: List<VerbindungDaten> = emptyList(),
    ) = KartenDaten(
        id = KartenId(id),
        name = name,
        knoten = knoten,
        verbindungen = verbindungen,
        ansicht = AnsichtsFenster.Standard,
    )

    private fun testVerbindung(
        id: String,
        vonKnoten: KnotenDaten,
        vonName: String,
        zuKnoten: KnotenDaten,
        zuName: String,
    ): VerbindungDaten = VerbindungDaten(
        id = VerbindungsId(id),
        von = AnschlussVerweis(vonKnoten.id, vonKnoten.anschlüsse.first { it.name == vonName }.id),
        zu = AnschlussVerweis(zuKnoten.id, zuKnoten.anschlüsse.first { it.name == zuName }.id),
    )
}

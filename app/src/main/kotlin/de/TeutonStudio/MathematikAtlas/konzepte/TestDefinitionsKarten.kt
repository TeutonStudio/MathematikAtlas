package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

object TestDefinitionsKarten {
    val alle: List<KonzeptDefinition> by lazy {
        listOf(
            zahlenKonzept(),
            additionsKonzept(),
            multiplikationsKonzept(),
            kehrwertKonzept(),
            divisionsKonzept(),
        )
    }

    fun finde(id: KonzeptId): KonzeptDefinition? = alle.firstOrNull { it.id == id }

    fun fürKnoten(knoten: KnotenDaten): KonzeptDefinition? = when (knoten.art) {
        "mathematik.zahl" -> finde(KonzeptId("zahl"))
        "mathematik.addition" -> finde(KonzeptId("addition"))
        "mathematik.multiplikation" -> finde(KonzeptId("multiplikation"))
        "mathematik.kehrwert" -> finde(KonzeptId("kehrwert"))
        "mathematik.division" -> finde(KonzeptId("division"))
        else -> null
    }

    private fun zahlenKonzept(): KonzeptDefinition {
        val zahl = testKnoten(MathematikKnotenVorlagen.Zahl, "zahl-definition", 100f, 90f, mapOf("wert" to "5"))
        val karte = testKarte("konzept-zahl-definition", "Definition der Zahl", listOf(zahl))
        return KonzeptDefinition(
            id = KonzeptId("zahl"),
            name = "Zahl",
            beschreibung = "Testkarte für eine konkrete Zahl und ihren veränderbaren Wert.",
            pfad = listOf("Grundlagen", "Zahlen"),
            tags = setOf("Zahl", "Konstante", "Rechnen"),
            knotenArten = setOf("mathematik.zahl"),
            reiter = listOf(KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, karte)),
            navigation = mapOf(KonzeptKnotenSchlüssel("definition", zahl.id) to KonzeptId("zahl")),
            erkundungsFreigaben = listOf(
                KonzeptErkundungsFreigabe("definition", zahl.id, "wert", "Wert der Zahl"),
            ),
        )
    }

    private fun additionsKonzept(): KonzeptDefinition {
        val a = testKnoten(MathematikKnotenVorlagen.Zahl, "addition-a", 40f, 55f, mapOf("wert" to "2"))
        val b = testKnoten(MathematikKnotenVorlagen.Zahl, "addition-b", 40f, 215f, mapOf("wert" to "3"))
        val addition = testKnoten(MathematikKnotenVorlagen.Addition, "addition-operator", 330f, 135f)
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
        val plus = testKnoten(MathematikKnotenVorlagen.Addition, "addition-null-plus", 330f, 135f)
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
            beschreibung = "Testdefinition der Addition mit einem neutralen Sonderfall.",
            pfad = listOf("Algebra", "Verknüpfungen"),
            tags = setOf("Addition", "Summe", "Rechnen", "Kommutativ"),
            knotenArten = setOf("mathematik.addition"),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, definition),
                KonzeptReiter("neutral-null", "Sonderfall 0", KonzeptReiterRolle.Spezialfall, sonderfall),
            ),
            navigation = mapOf(
                KonzeptKnotenSchlüssel("definition", a.id) to KonzeptId("zahl"),
                KonzeptKnotenSchlüssel("definition", b.id) to KonzeptId("zahl"),
                KonzeptKnotenSchlüssel("definition", addition.id) to KonzeptId("addition"),
                KonzeptKnotenSchlüssel("neutral-null", x.id) to KonzeptId("zahl"),
                KonzeptKnotenSchlüssel("neutral-null", nullKnoten.id) to KonzeptId("zahl"),
                KonzeptKnotenSchlüssel("neutral-null", plus.id) to KonzeptId("addition"),
            ),
            erkundungsFreigaben = listOf(
                KonzeptErkundungsFreigabe("definition", a.id, "wert", "Erster Summand"),
                KonzeptErkundungsFreigabe("definition", b.id, "wert", "Zweiter Summand"),
                KonzeptErkundungsFreigabe("neutral-null", x.id, "wert", "Ausgangswert"),
                KonzeptErkundungsFreigabe("neutral-null", nullKnoten.id, "wert", "Testwert statt 0"),
            ),
        )
    }

    private fun multiplikationsKonzept(): KonzeptDefinition {
        val a = testKnoten(MathematikKnotenVorlagen.Zahl, "multiplikation-a", 40f, 55f, mapOf("wert" to "4"))
        val b = testKnoten(MathematikKnotenVorlagen.Zahl, "multiplikation-b", 40f, 215f, mapOf("wert" to "5"))
        val mal = zweifacheMultiplikation("multiplikation-operator", 330f, 135f)
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
            beschreibung = "Testdefinition der Multiplikation zweier Faktoren.",
            pfad = listOf("Algebra", "Verknüpfungen"),
            tags = setOf("Multiplikation", "Produkt", "Rechnen"),
            knotenArten = setOf("mathematik.multiplikation"),
            reiter = listOf(KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, karte)),
            navigation = standardNavigation("definition", listOf(a, b, mal)),
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
            beschreibung = "Der Kehrwert von x ist die Potenz x⁻¹ und setzt x ≠ 0 voraus.",
            pfad = listOf("Algebra", "Verknüpfungen"),
            tags = setOf("Kehrwert", "Inverse", "Potenz", "Division"),
            knotenArten = setOf("mathematik.kehrwert"),
            reiter = listOf(KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, karte)),
            navigation = standardNavigation("definition", listOf(zahl, kehrwert)),
            erkundungsFreigaben = listOf(
                KonzeptErkundungsFreigabe("definition", zahl.id, "wert", "Zahl ungleich 0"),
            ),
        )
    }

    private fun divisionsKonzept(): KonzeptDefinition {
        val definition = divisionsDefinitionsKarte()
        val reellReell = einfacheKehrwertDivision(
            id = "division-reell-reell",
            name = "Reelle Division",
            linkerName = "Zähler",
            rechterName = "Nenner",
        )
        val komplexReell = einfacheKehrwertDivision(
            id = "division-komplex-reell",
            name = "Komplex durch reell",
            linkerName = "Dividend",
            rechterName = "Divisor",
        )
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
            tags = setOf("Division", "Quotient", "Kehrwert", "Konjugierte", "Rechnen"),
            knotenArten = setOf("mathematik.division"),
            reiter = reiter,
            navigation = buildMap {
                reiter.forEach { konzeptReiter -> putAll(standardNavigation(konzeptReiter.id, konzeptReiter.karte.knoten)) }
            },
        )
    }

    /** x / y = falls y = 0: Ersatz, sonst x · y⁻¹. */
    private fun divisionsDefinitionsKarte(): KartenDaten {
        val zähler = zahlKartenEingang("division-definition-zähler", "Zähler x", 30f, 70f)
        val nenner = zahlKartenEingang("division-definition-nenner", "Nenner y", 30f, 240f)
        val nullErsatz = zahlKartenEingang("division-definition-null-ersatz", "falls Nenner null", 420f, 610f)
        val nullKnoten = testKnoten(MathematikKnotenVorlagen.Zahl, "division-definition-null", 40f, 430f, mapOf("wert" to "0"))
        val gleichheit = testKnoten(MathematikKnotenVorlagen.Gleichheit, "division-definition-gleich", 360f, 330f)
        val kehrwert = testKnoten(MathematikKnotenVorlagen.Kehrwert, "division-definition-kehrwert", 360f, 190f)
        val produkt = zweifacheMultiplikation("division-definition-produkt", 670f, 105f)
        val fall = testKnoten(MathematikKnotenVorlagen.Fall, "division-definition-fall", 970f, 270f)
        val ausgang = zahlKartenAusgang("division-definition-ausgang", "ergebnis", 1320f, 290f)
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

    private fun einfacheKehrwertDivision(
        id: String,
        name: String,
        linkerName: String,
        rechterName: String,
    ): KartenDaten {
        val links = zahlKartenEingang("$id-links", linkerName, 40f, 70f)
        val rechts = zahlKartenEingang("$id-rechts", rechterName, 40f, 250f)
        val kehrwert = testKnoten(MathematikKnotenVorlagen.Kehrwert, "$id-kehrwert", 370f, 250f)
        val produkt = zweifacheMultiplikation("$id-produkt", 690f, 150f)
        val ausgang = zahlKartenAusgang("$id-ausgang", "ergebnis", 1010f, 150f)
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

    /** a / b = (a · conjugate(b)) · (b · conjugate(b))⁻¹; der neue Nenner ist reell. */
    private fun komplexeDivisionMitKonjugierter(): KartenDaten {
        val dividend = zahlKartenEingang("division-komplex-dividend", "Dividend", 30f, 80f)
        val divisor = zahlKartenEingang("division-komplex-divisor", "Divisor", 30f, 310f)
        val konjugierte = testKnoten(MathematikKnotenVorlagen.Konjugierte, "division-komplex-konjugierte", 350f, 310f)
        val zählerProdukt = zweifacheMultiplikation("division-komplex-zähler-produkt", 660f, 80f)
        val nennerProdukt = zweifacheMultiplikation("division-komplex-nenner-produkt", 660f, 330f)
        val kehrwert = testKnoten(MathematikKnotenVorlagen.Kehrwert, "division-komplex-kehrwert", 970f, 330f)
        val ergebnisProdukt = zweifacheMultiplikation("division-komplex-ergebnis-produkt", 1260f, 180f)
        val ausgang = zahlKartenAusgang("division-komplex-ausgang", "ergebnis", 1580f, 180f)
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
        knoten.mapNotNull { knotenDaten ->
            val ziel = when (knotenDaten.art) {
                "mathematik.zahl" -> KonzeptId("zahl")
                "mathematik.addition" -> KonzeptId("addition")
                "mathematik.multiplikation" -> KonzeptId("multiplikation")
                "mathematik.kehrwert" -> KonzeptId("kehrwert")
                "mathematik.division" -> KonzeptId("division")
                else -> null
            }
            ziel?.let { KonzeptKnotenSchlüssel(reiterId, knotenDaten.id) to it }
        }.toMap()

    private fun zweifacheMultiplikation(id: String, x: Float, y: Float): KnotenDaten =
        testKnoten(MathematikKnotenVorlagen.Multiplikation, id, x, y).copy(
            anschlüsse = testKnoten(MathematikKnotenVorlagen.Multiplikation, id, x, y).anschlüsse
                .filter { it.name in setOf("a", "b", "wert") },
        )

    private fun zahlKartenEingang(id: String, name: String, x: Float, y: Float): KnotenDaten =
        testKnoten(MathematikKnotenVorlagen.KartenEingang, id, x, y, mapOf("name" to name)).copy(
            anschlüsse = listOf(AnschlussDaten(
                id = AnschlussId("$id-wert-0"),
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Zahl.id,
            )),
        )

    private fun zahlKartenAusgang(id: String, name: String, x: Float, y: Float): KnotenDaten =
        testKnoten(MathematikKnotenVorlagen.KartenAusgang, id, x, y, mapOf("name" to name)).copy(
            anschlüsse = listOf(AnschlussDaten(
                id = AnschlussId("$id-wert-0"),
                name = "wert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Zahl.id,
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

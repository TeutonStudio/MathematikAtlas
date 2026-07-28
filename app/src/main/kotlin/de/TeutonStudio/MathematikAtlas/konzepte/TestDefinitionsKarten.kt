package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

object TestDefinitionsKarten {
    val alle: List<KonzeptDefinition> by lazy {
        listOf(
            zahlenKonzept(),
            additionsKonzept(),
            multiplikationsKonzept(),
            divisionsKonzept(),
        )
    }

    fun finde(id: KonzeptId): KonzeptDefinition? = alle.firstOrNull { it.id == id }

    fun fürKnoten(knoten: KnotenDaten): KonzeptDefinition? = when (knoten.art) {
        "mathematik.zahl" -> finde(KonzeptId("zahl"))
        "mathematik.addition" -> finde(KonzeptId("addition"))
        "mathematik.multiplikation" -> finde(KonzeptId("multiplikation"))
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
        val mal = testKnoten(MathematikKnotenVorlagen.Multiplikation, "multiplikation-operator", 330f, 135f)
            .copy(anschlüsse = MathematikKnotenVorlagen.Multiplikation.anschlüsse
                .filter { it.name in setOf("a", "b", "wert") }
                .map { it.copy(id = AnschlussId("multiplikation-${it.name}")) })
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
            navigation = mapOf(
                KonzeptKnotenSchlüssel("definition", a.id) to KonzeptId("zahl"),
                KonzeptKnotenSchlüssel("definition", b.id) to KonzeptId("zahl"),
                KonzeptKnotenSchlüssel("definition", mal.id) to KonzeptId("multiplikation"),
            ),
            erkundungsFreigaben = listOf(
                KonzeptErkundungsFreigabe("definition", a.id, "wert", "Erster Faktor"),
                KonzeptErkundungsFreigabe("definition", b.id, "wert", "Zweiter Faktor"),
            ),
        )
    }

    private fun divisionsKonzept(): KonzeptDefinition {
        val dividend = testKnoten(MathematikKnotenVorlagen.Zahl, "division-dividend", 40f, 55f, mapOf("wert" to "8"))
        val divisor = testKnoten(MathematikKnotenVorlagen.Zahl, "division-divisor", 40f, 215f, mapOf("wert" to "2"))
        val division = testKnoten(MathematikKnotenVorlagen.Division, "division-operator", 330f, 135f)
        val karte = testKarte(
            "konzept-division-definition",
            "Division",
            listOf(dividend, divisor, division),
            listOf(
                testVerbindung("division-1", dividend, "wert", division, "dividend"),
                testVerbindung("division-2", divisor, "wert", division, "divisor"),
            ),
        )
        return KonzeptDefinition(
            id = KonzeptId("division"),
            name = "Division",
            beschreibung = "Testdefinition der Division einschließlich des veränderbaren Divisors.",
            pfad = listOf("Algebra", "Verknüpfungen"),
            tags = setOf("Division", "Quotient", "Rechnen"),
            knotenArten = setOf("mathematik.division"),
            reiter = listOf(KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, karte)),
            navigation = mapOf(
                KonzeptKnotenSchlüssel("definition", dividend.id) to KonzeptId("zahl"),
                KonzeptKnotenSchlüssel("definition", divisor.id) to KonzeptId("zahl"),
                KonzeptKnotenSchlüssel("definition", division.id) to KonzeptId("division"),
            ),
            erkundungsFreigaben = listOf(
                KonzeptErkundungsFreigabe("definition", dividend.id, "wert", "Dividend"),
                KonzeptErkundungsFreigabe("definition", divisor.id, "wert", "Divisor"),
            ),
        )
    }

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

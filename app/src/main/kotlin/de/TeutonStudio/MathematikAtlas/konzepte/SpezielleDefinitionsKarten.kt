package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

/** Erhält fachliche Zusatzreiter, die bereits vor v2.3.9 Bestandteil des Konzeptsystems waren. */
internal object SpezielleDefinitionsKarten {
    fun zahl(): KonzeptDefinition {
        val leer = knoten(MathematikKnotenVorlagen.EndlicheMenge, "zahl-leer", 30f, 30f, mapOf("elemente" to ""))
        val plus = knoten(MathematikKnotenVorlagen.AllgemeinerParameter, "zahl-plus", 30f, 190f, mapOf("name" to "+"))
        val plusMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "zahl-plus-menge", 330f, 190f)
        val plusMacht = knoten(MathematikKnotenVorlagen.Mächtigkeit, "zahl-plus-macht", 620f, 190f)
        val minus = knoten(MathematikKnotenVorlagen.AllgemeinerParameter, "zahl-minus", 30f, 350f, mapOf("name" to "−"))
        val minusMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "zahl-minus-menge", 330f, 350f)
        val minusMacht = knoten(MathematikKnotenVorlagen.Mächtigkeit, "zahl-minus-macht", 620f, 350f)
        val definition = karte(
            "konzept-zahl-definition",
            "0 = ∅, 1 = {+}, −1 = {−}",
            listOf(leer, plus, plusMenge, plusMacht, minus, minusMenge, minusMacht),
            listOf(
                verbindung("zahl-d1", plus, "wert", plusMenge, "element"),
                verbindung("zahl-d2", plusMenge, "menge", plusMacht, "menge"),
                verbindung("zahl-d3", minus, "wert", minusMenge, "element"),
                verbindung("zahl-d4", minusMenge, "menge", minusMacht, "menge"),
            ),
        )
        return KonzeptDefinition(
            id = KonzeptId("zahl"),
            name = "Zahl",
            beschreibung = "0 ist die leere Menge. +1 und −1 sind gleichmächtige Einzelmengen ihrer Richtungsoperatoren; ihre Nachfolger erzeugen die beiden Äste von ℤ.",
            pfad = listOf("Grundlagen", "Zahlen"),
            tags = setOf("Zahl", "Nachfolger", "Ganze Zahlen", "Mächtigkeit"),
            knotenArten = setOf("mathematik.zahl"),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, definition),
                KonzeptReiter("positiver-nachfolger", "Positiver Nachfolger", KonzeptReiterRolle.Spezialfall, nachfolger("positiv", "+")),
                KonzeptReiter("negativer-nachfolger", "Negativer Nachfolger", KonzeptReiterRolle.Spezialfall, nachfolger("negativ", "−")),
                KonzeptReiter("zahlbereiche", "ℕ, ℕ₀ und ℤ", KonzeptReiterRolle.Äquivalenz, zahlbereiche()),
            ),
        )
    }

    fun subtraktion(vorlage: KnotenVorlage): KonzeptDefinition {
        val a = knoten(MathematikKnotenVorlagen.Zahl, "sub-z-a", 30f, 50f, mapOf("wert" to "7"))
        val b = knoten(MathematikKnotenVorlagen.Zahl, "sub-z-b", 30f, 210f, mapOf("wert" to "3"))
        val minusEins = knoten(MathematikKnotenVorlagen.Zahl, "sub-minus-eins", 30f, 370f, mapOf("wert" to "-1"))
        val mal = binär(MathematikKnotenVorlagen.Multiplikation, "sub-mal", 350f, 280f)
        val plus = binär(MathematikKnotenVorlagen.Addition, "sub-plus", 690f, 150f)
        val ganzeZahlen = karte(
            "konzept-sub-z",
            "a + (−1)·b",
            listOf(a, b, minusEins, mal, plus),
            listOf(
                verbindung("subz-1", b, "wert", mal, "a"),
                verbindung("subz-2", minusEins, "wert", mal, "b"),
                verbindung("subz-3", a, "wert", plus, "a"),
                verbindung("subz-4", mal, "wert", plus, "b"),
            ),
        )
        return KonzeptDefinition(
            id = KonzeptId("subtraktion"),
            name = vorlage.name,
            beschreibung = "Subtraktion ist Addition des additiv Inversen.",
            pfad = listOf("Algebra", "Verknüpfungen"),
            tags = setOf("Subtraktion", "Differenz", "Addition", "additives Inverses"),
            knotenArten = setOf(vorlage.art),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, TestDefinitionsKarten.definitionsKarte(vorlage, 0)),
                KonzeptReiter("ganze-zahlen", "Sonderfall ℤ", KonzeptReiterRolle.Spezialfall, ganzeZahlen),
            ),
        )
    }

    private fun nachfolger(id: String, operator: String): KartenDaten {
        val x = schnittstelle("nachfolger-$id-x", "x", MathematikAnschlussArten.Menge.id, true, 30f, 100f)
        val op = knoten(MathematikKnotenVorlagen.AllgemeinerParameter, "nachfolger-$id-op", 30f, 280f, mapOf("name" to operator))
        val singleton = knoten(MathematikKnotenVorlagen.Einzelmenge, "nachfolger-$id-singleton", 340f, 280f)
        val vereinigt = binär(MathematikKnotenVorlagen.Vereinigung, "nachfolger-$id-union", 660f, 160f)
        val aus = schnittstelle("nachfolger-$id-aus", "nachfolger", MathematikAnschlussArten.Menge.id, false, 990f, 160f)
        return karte(
            "konzept-zahl-$id",
            if (id == "positiv") "x ∪ {+}" else "x ∪ {−}",
            listOf(x, op, singleton, vereinigt, aus),
            listOf(
                verbindung("nach-$id-1", op, "wert", singleton, "element"),
                verbindung("nach-$id-2", x, "wert", vereinigt, "a"),
                verbindung("nach-$id-3", singleton, "menge", vereinigt, "b"),
                verbindung("nach-$id-4", vereinigt, "menge", aus, "wert"),
            ),
        )
    }

    private fun zahlbereiche(): KartenDaten {
        val n = knoten(MathematikKnotenVorlagen.NatürlicheZahlen, "bereiche-n", 30f, 40f)
        val nullAlsLeereMenge = knoten(MathematikKnotenVorlagen.EndlicheMenge, "bereiche-null", 30f, 210f, mapOf("elemente" to ""))
        val nullMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "bereiche-nullmenge", 300f, 210f)
        val n0 = binär(MathematikKnotenVorlagen.Vereinigung, "bereiche-n0", 600f, 120f)
        val z = knoten(MathematikKnotenVorlagen.GanzeZahlen, "bereiche-z", 920f, 120f)
        val nTeil = knoten(MathematikKnotenVorlagen.TeilOderGleichmenge, "bereiche-n-teil", 1230f, 40f)
        val n0Teil = knoten(MathematikKnotenVorlagen.TeilOderGleichmenge, "bereiche-n0-teil", 1230f, 230f)
        return karte(
            "konzept-zahl-bereiche",
            "ℕ ⊆ ℤ und ℕ₀ ⊆ ℤ",
            listOf(n, nullAlsLeereMenge, nullMenge, n0, z, nTeil, n0Teil),
            listOf(
                verbindung("bereiche-1", nullAlsLeereMenge, "menge", nullMenge, "element"),
                verbindung("bereiche-2", n, "menge", n0, "a"),
                verbindung("bereiche-3", nullMenge, "menge", n0, "b"),
                verbindung("bereiche-4", n, "menge", nTeil, "links"),
                verbindung("bereiche-5", z, "menge", nTeil, "rechts"),
                verbindung("bereiche-6", n0, "menge", n0Teil, "links"),
                verbindung("bereiche-7", z, "menge", n0Teil, "rechts"),
            ),
        )
    }

    private fun knoten(
        vorlage: KnotenVorlage,
        id: String,
        x: Float,
        y: Float,
        parameter: Map<String, String> = emptyMap(),
    ): KnotenDaten {
        val basis = vorlage.erzeuge(GraphPunkt(x, y))
        return basis.copy(
            id = KnotenId(id),
            parameter = basis.parameter + parameter,
            anschlüsse = basis.anschlüsse.map { it.copy(id = AnschlussId("$id-${it.name}-${it.reihenfolge}")) },
        )
    }

    private fun binär(vorlage: KnotenVorlage, id: String, x: Float, y: Float): KnotenDaten {
        val basis = knoten(vorlage, id, x, y)
        return basis.copy(anschlüsse = basis.anschlüsse.filter { it.name in setOf("a", "b", "wert", "menge") })
    }

    private fun schnittstelle(
        id: String,
        name: String,
        art: AnschlussArtId,
        eingang: Boolean,
        x: Float,
        y: Float,
    ): KnotenDaten = KnotenDaten(
        id = KnotenId(id),
        art = if (eingang) TestDefinitionsKarten.KONZEPT_EINGANG_ART else TestDefinitionsKarten.KONZEPT_AUSGANG_ART,
        name = name,
        position = GraphPunkt(x, y),
        größe = GraphGröße(260f, 92f),
        anschlüsse = listOf(AnschlussDaten(
            id = AnschlussId("$id-wert"),
            name = "wert",
            richtung = if (eingang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang,
            kante = if (eingang) AnschlussKante.Rechts else AnschlussKante.Links,
            art = art,
        )),
        parameter = mapOf("typ" to art.wert, "variabel" to "false", "folgtEingang" to ""),
    )

    private fun karte(
        id: String,
        name: String,
        knoten: List<KnotenDaten>,
        verbindungen: List<VerbindungDaten>,
    ) = KartenDaten(id = KartenId(id), name = name, knoten = knoten, verbindungen = verbindungen)

    private fun verbindung(
        id: String,
        von: KnotenDaten,
        vonName: String,
        zu: KnotenDaten,
        zuName: String,
    ) = VerbindungDaten(
        id = VerbindungsId(id),
        von = AnschlussVerweis(von.id, von.anschlüsse.first { it.name == vonName }.id),
        zu = AnschlussVerweis(zu.id, zu.anschlüsse.first { it.name == zuName }.id),
    )
}

package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

/** Erhält fachliche Zusatzreiter, die bereits vor v2.3.9 Bestandteil des Konzeptsystems waren. */
internal object SpezielleDefinitionsKarten {
    fun zahl(): KonzeptDefinition {
        val leer = knoten(MathematikKnotenVorlagen.EndlicheMenge, "zahl-leer", 30f, 40f, mapOf("elemente" to ""))
        val nullAusgang = schnittstelle("zahl-null-aus", "0 = ∅", MathematikAnschlussArten.Menge.id, false, 780f, 40f)

        val plus = knoten(MathematikKnotenVorlagen.AllgemeinerParameter, "zahl-plus", 30f, 210f, mapOf("name" to "+"))
        val plusMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "zahl-plus-menge", 340f, 210f)
        val einsAusgang = schnittstelle("zahl-eins-aus", "1 = {+}", MathematikAnschlussArten.Menge.id, false, 780f, 210f)

        val minus = knoten(MathematikKnotenVorlagen.AllgemeinerParameter, "zahl-minus", 30f, 380f, mapOf("name" to "−"))
        val minusMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "zahl-minus-menge", 340f, 380f)
        val minusEinsAusgang = schnittstelle("zahl-minus-eins-aus", "−1 = {−}", MathematikAnschlussArten.Menge.id, false, 780f, 380f)

        val definition = karte(
            "konzept-zahl-definition",
            "0 = ∅, 1 = {+}, −1 = {−}",
            listOf(leer, nullAusgang, plus, plusMenge, einsAusgang, minus, minusMenge, minusEinsAusgang),
            listOf(
                verbindung("zahl-d0", leer, "menge", nullAusgang, "wert"),
                verbindung("zahl-d1", plus, "wert", plusMenge, "element"),
                verbindung("zahl-d2", plusMenge, "menge", einsAusgang, "wert"),
                verbindung("zahl-d3", minus, "wert", minusMenge, "element"),
                verbindung("zahl-d4", minusMenge, "menge", minusEinsAusgang, "wert"),
            ),
        )
        return KonzeptDefinition(
            id = KonzeptId("zahl"),
            name = "Zahl",
            beschreibung = "Die elementaren Zahlrepräsentanten sind 0 = ∅, 1 = {+} und −1 = {−}. Die Mengen selbst sind die Zahlen; ihre Mächtigkeit ist nicht ihre Definition.",
            pfad = listOf("Grundlagen", "Zahlen"),
            tags = setOf("Zahl", "Null", "Eins", "Minus Eins", "Vorzeichen", "Mengen"),
            knotenArten = setOf("mathematik.zahl"),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, definition),
                KonzeptReiter("zahlbereiche", "ℕ, ℕ₀ und ℤ", KonzeptReiterRolle.Äquivalenz, zahlbereiche()),
            ),
        )
    }

    fun ganzeZahlen(vorlage: KnotenVorlage): KonzeptDefinition = KonzeptDefinition(
        id = KonzeptId("ganzezahlen"),
        name = vorlage.name,
        beschreibung = "Die ganzen Zahlen bestehen aus 0 sowie zwei kumulativen Ästen. Positive Zahlen beginnen bei 1 = {+}, negative bei −1 = {−}; jeder nächste Wert vereinigt die bisherige Zahl mit der Einzelmenge ihres Vorgängers.",
        pfad = listOf("Grundlagen", "Zahlen", "Ganze Zahlen"),
        tags = setOf("Ganze Zahlen", "Vorzeichen", "Nachfolger", "Vorgänger", "Mengen"),
        knotenArten = setOf(vorlage.art),
        reiter = listOf(
            KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, ganzeZahlenDefinition()),
            KonzeptReiter("ganze-beispiele", "Beispiele", KonzeptReiterRolle.Beispiel, vorzeichenBeispiele()),
            KonzeptReiter("positiver-nachfolger", "Positiver Nachfolger", KonzeptReiterRolle.Spezialfall, vorzeichenNachfolger("positiv")),
            KonzeptReiter("negativer-nachfolger", "Negativer Nachfolger", KonzeptReiterRolle.Spezialfall, vorzeichenNachfolger("negativ")),
        ),
    )

    fun natürlicheZahlen(vorlage: KnotenVorlage): KonzeptDefinition = KonzeptDefinition(
        id = KonzeptId("natuerlichezahlen"),
        name = vorlage.name,
        beschreibung = "Die natürlichen Zahlen verwenden die von-Neumann-Realisierung des Peano-Nachfolgers: 0 = ∅ und n+1 = n ∪ {n}. Daher gilt 1 = {0} und 2 = {1,0}.",
        pfad = listOf("Grundlagen", "Zahlen", "Natürliche Zahlen"),
        tags = setOf("Natürliche Zahlen", "Peano", "von Neumann", "Nachfolger", "Mengen"),
        knotenArten = setOf(vorlage.art),
        reiter = listOf(
            KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, natürlicherNachfolger()),
            KonzeptReiter("beispiele", "Beispiele", KonzeptReiterRolle.Beispiel, natürlicheBeispiele()),
        ),
    )

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

    private fun ganzeZahlenDefinition(): KartenDaten = karte(
        id = "konzept-ganze-zahlen-definition",
        name = "ℤ aus 0 und zwei kumulativen Vorzeichenästen",
        knoten = listOf(
            regelHinweis(
                id = "ganze-definition-null",
                name = "Null",
                regel = "0 = ∅",
                x = 40f,
                y = 40f,
            ),
            regelHinweis(
                id = "ganze-definition-positiv",
                name = "Positiver Ast",
                regel = "Basis 1 = {+}; für n ≥ 1 gilt n+1 = n ∪ {n}.",
                x = 40f,
                y = 250f,
            ),
            regelHinweis(
                id = "ganze-definition-negativ",
                name = "Negativer Ast",
                regel = "Basis −1 = {−}; für n ≤ −1 gilt n−1 = n ∪ {n}.",
                x = 520f,
                y = 250f,
            ),
        ),
        verbindungen = emptyList(),
    )

    private fun vorzeichenBeispiele(): KartenDaten = karte(
        id = "konzept-zahl-ganze-beispiele",
        name = "3 = {2,1,+} und −6 = {−5,−4,−3,−2,−1,−}",
        knoten = listOf(
            regelHinweis(
                id = "ganze-beispiel-positiv",
                name = "Positive ganze Zahl",
                regel = "3 = {2,1,+}; allgemein enthält n alle positiven Vorgänger bis 1 sowie das Grundelement +.",
                x = 40f,
                y = 80f,
            ),
            regelHinweis(
                id = "ganze-beispiel-negativ",
                name = "Negative ganze Zahl",
                regel = "−6 = {−5,−4,−3,−2,−1,−}; allgemein enthält −n alle negativen Vorgänger bis −1 sowie das Grundelement −.",
                x = 520f,
                y = 80f,
            ),
        ),
        verbindungen = emptyList(),
    )

    private fun vorzeichenNachfolger(richtung: String): KartenDaten {
        val n = schnittstelle("nachfolger-$richtung-n", "n", MathematikAnschlussArten.Menge.id, true, 30f, 120f)
        val nMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "nachfolger-$richtung-n-menge", 350f, 260f)
        val vereinigt = binär(MathematikKnotenVorlagen.Vereinigung, "nachfolger-$richtung-union", 680f, 120f)
        val formel = if (richtung == "positiv") {
            "n+1 = n ∪ {n}, Basis 1 = {+}"
        } else {
            "n−1 = n ∪ {n}, Basis −1 = {−}"
        }
        val aus = schnittstelle("nachfolger-$richtung-aus", formel, MathematikAnschlussArten.Menge.id, false, 1030f, 120f)
        return karte(
            "konzept-zahl-$richtung",
            formel,
            listOf(n, nMenge, vereinigt, aus),
            listOf(
                verbindung("nach-$richtung-1", n, "wert", nMenge, "element"),
                verbindung("nach-$richtung-2", n, "wert", vereinigt, "a"),
                verbindung("nach-$richtung-3", nMenge, "menge", vereinigt, "b"),
                verbindung("nach-$richtung-4", vereinigt, "menge", aus, "wert"),
            ),
        )
    }

    private fun natürlicheBeispiele(): KartenDaten {
        val nullMenge = knoten(MathematikKnotenVorlagen.EndlicheMenge, "nat-null", 30f, 80f, mapOf("elemente" to ""))
        val eins = knoten(MathematikKnotenVorlagen.Einzelmenge, "nat-eins", 340f, 80f)
        val einsMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "nat-eins-menge", 650f, 250f)
        val zwei = binär(MathematikKnotenVorlagen.Vereinigung, "nat-zwei", 650f, 80f)
        val nullAusgang = schnittstelle("nat-null-aus", "0 = ∅", MathematikAnschlussArten.Menge.id, false, 1010f, 20f)
        val einsAusgang = schnittstelle("nat-eins-aus", "1 = {0}", MathematikAnschlussArten.Menge.id, false, 1010f, 150f)
        val zweiAusgang = schnittstelle("nat-zwei-aus", "2 = {1,0}", MathematikAnschlussArten.Menge.id, false, 1010f, 280f)
        return karte(
            "konzept-zahl-natürliche-beispiele",
            "0 = ∅, 1 = {0}, 2 = {1,0}",
            listOf(nullMenge, eins, einsMenge, zwei, nullAusgang, einsAusgang, zweiAusgang),
            listOf(
                verbindung("nat-b-1", nullMenge, "menge", eins, "element"),
                verbindung("nat-b-2", eins, "menge", einsMenge, "element"),
                verbindung("nat-b-3", eins, "menge", zwei, "a"),
                verbindung("nat-b-4", einsMenge, "menge", zwei, "b"),
                verbindung("nat-b-5", nullMenge, "menge", nullAusgang, "wert"),
                verbindung("nat-b-6", eins, "menge", einsAusgang, "wert"),
                verbindung("nat-b-7", zwei, "menge", zweiAusgang, "wert"),
            ),
        )
    }

    private fun natürlicherNachfolger(): KartenDaten {
        val n = schnittstelle("nat-nachfolger-n", "n", MathematikAnschlussArten.Menge.id, true, 30f, 120f)
        val nMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "nat-nachfolger-einzelmenge", 350f, 260f)
        val vereinigt = binär(MathematikKnotenVorlagen.Vereinigung, "nat-nachfolger-union", 680f, 120f)
        val aus = schnittstelle("nat-nachfolger-aus", "n+1 = n ∪ {n}, Basis 0 = ∅", MathematikAnschlussArten.Menge.id, false, 1030f, 120f)
        return karte(
            "konzept-zahl-natürlicher-nachfolger",
            "n+1 = n ∪ {n}, Basis 0 = ∅",
            listOf(n, nMenge, vereinigt, aus),
            listOf(
                verbindung("nat-n-1", n, "wert", nMenge, "element"),
                verbindung("nat-n-2", n, "wert", vereinigt, "a"),
                verbindung("nat-n-3", nMenge, "menge", vereinigt, "b"),
                verbindung("nat-n-4", vereinigt, "menge", aus, "wert"),
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

    private fun regelHinweis(id: String, name: String, regel: String, x: Float, y: Float) = KnotenDaten(
        id = KnotenId(id),
        art = TestDefinitionsKarten.KONZEPT_REGEL_ART,
        name = name,
        position = GraphPunkt(x, y),
        größe = GraphGröße(420f, 180f),
        anschlüsse = emptyList(),
        parameter = mapOf(
            "regel" to regel,
            "knotenArt" to "Mengendefinition einer Zahl",
            "kategorie" to "Grundlagen: Zahlen",
        ),
    )

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

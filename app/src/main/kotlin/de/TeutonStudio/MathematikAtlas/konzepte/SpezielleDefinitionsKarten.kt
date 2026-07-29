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
            beschreibung = "Vorzeichen-Ganzzahlen werden als Mengen aufgebaut: 0 = ∅, 1 = {+}, −1 = {−}, positive Nachfolger als {n,+} und negative Nachfolger als {n,−}. Natürliche Zahlen verwenden getrennt die von-Neumann-Konstruktion 0 = ∅ und n+1 = n ∪ {n}.",
            pfad = listOf("Grundlagen", "Zahlen"),
            tags = setOf("Zahl", "Nachfolger", "Ganze Zahlen", "Natürliche Zahlen", "Peano", "von Neumann", "Mengen"),
            knotenArten = setOf("mathematik.zahl"),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, definition),
                KonzeptReiter("positiver-nachfolger", "Positiver Nachfolger", KonzeptReiterRolle.Spezialfall, vorzeichenNachfolger("positiv", "+")),
                KonzeptReiter("negativer-nachfolger", "Negativer Nachfolger", KonzeptReiterRolle.Spezialfall, vorzeichenNachfolger("negativ", "−")),
                KonzeptReiter("natürliche-beispiele", "Natürliche Zahlen", KonzeptReiterRolle.Beispiel, natürlicheBeispiele()),
                KonzeptReiter("natürlicher-nachfolger", "Natürlicher Nachfolger", KonzeptReiterRolle.Spezialfall, natürlicherNachfolger()),
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

    private fun vorzeichenNachfolger(id: String, operator: String): KartenDaten {
        val n = schnittstelle("nachfolger-$id-n", "n", MathematikAnschlussArten.Menge.id, true, 30f, 80f)
        val nMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "nachfolger-$id-n-menge", 340f, 80f)
        val op = knoten(MathematikKnotenVorlagen.AllgemeinerParameter, "nachfolger-$id-op", 30f, 280f, mapOf("name" to operator))
        val opMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "nachfolger-$id-op-menge", 340f, 280f)
        val vereinigt = binär(MathematikKnotenVorlagen.Vereinigung, "nachfolger-$id-union", 660f, 170f)
        val formel = if (id == "positiv") "n+1 = {n,+}" else "n−1 = {n,−}"
        val aus = schnittstelle("nachfolger-$id-aus", formel, MathematikAnschlussArten.Menge.id, false, 1010f, 170f)
        return karte(
            "konzept-zahl-$id",
            formel,
            listOf(n, nMenge, op, opMenge, vereinigt, aus),
            listOf(
                verbindung("nach-$id-1", n, "wert", nMenge, "element"),
                verbindung("nach-$id-2", op, "wert", opMenge, "element"),
                verbindung("nach-$id-3", nMenge, "menge", vereinigt, "a"),
                verbindung("nach-$id-4", opMenge, "menge", vereinigt, "b"),
                verbindung("nach-$id-5", vereinigt, "menge", aus, "wert"),
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
        val zweiAusgang = schnittstelle("nat-zwei-aus", "2 = {0,1}", MathematikAnschlussArten.Menge.id, false, 1010f, 280f)
        return karte(
            "konzept-zahl-natürliche-beispiele",
            "0 = ∅, 1 = {0}, 2 = {0,1}",
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
        val aus = schnittstelle("nat-nachfolger-aus", "n+1 = n ∪ {n}", MathematikAnschlussArten.Menge.id, false, 1030f, 120f)
        return karte(
            "konzept-zahl-natürlicher-nachfolger",
            "n+1 = n ∪ {n}",
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

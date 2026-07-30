package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

/** Praktische, selbstbezugsfreie Definitionskarten der Division. */
internal object DivisionDefinitionsKarten {
    val konzept: KonzeptDefinition by lazy {
        KonzeptDefinition(
            id = KonzeptId("division"),
            name = "Division",
            beschreibung = "Division mit Nullfall, Vorzeichennormalisierung sowie kartesischer und polarer Behandlung komplexer Zahlen.",
            pfad = listOf("Rechnen", "Division"),
            tags = setOf("Division", "Quotient", "Kehrwert", "komplex", "kartesisch", "polar"),
            knotenArten = setOf(MathematikKnotenVorlagen.Division.art),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, definition()),
                KonzeptReiter("positiver-nenner", "Nenner > 0", KonzeptReiterRolle.Spezialfall, positiverNenner()),
                KonzeptReiter("negativer-nenner", "Nenner < 0", KonzeptReiterRolle.Spezialfall, negativerNenner()),
                KonzeptReiter(
                    id = "komplexer-nenner",
                    titel = "Komplexer Nenner",
                    rolle = KonzeptReiterRolle.Spezialfall,
                    karte = komplexerNennerKartesisch(),
                    darstellungsVarianten = mapOf(KomplexDarstellung.Polar to komplexerNennerPolar()),
                ),
                KonzeptReiter(
                    id = "komplexer-zaehler",
                    titel = "Komplexer Zähler",
                    rolle = KonzeptReiterRolle.Spezialfall,
                    karte = komplexerZählerKartesisch(),
                    darstellungsVarianten = mapOf(KomplexDarstellung.Polar to komplexerZählerPolar()),
                ),
            ),
        )
    }

    /** D(a,b,n) = falls b=0: n, sonst a·b⁻¹. */
    private fun definition(): KartenDaten {
        val zähler = eingang("division-definition-zaehler", "Zähler a", 30f, 60f)
        val nenner = eingang("division-definition-nenner", "Nenner b", 30f, 240f)
        val nullwert = eingang("division-definition-nullwert", "Falls Nenner 0", 30f, 480f)
        val nullKnoten = zahl("division-definition-null", "0", 330f, 420f)
        val gleich = knoten(MathematikKnotenVorlagen.Gleichheit, "division-definition-gleich", 620f, 330f)
        val kehrwert = knoten(MathematikKnotenVorlagen.Kehrwert, "division-definition-kehrwert", 620f, 180f)
        val produkt = binäreMultiplikation("division-definition-produkt", 920f, 100f)
        val fall = knoten(MathematikKnotenVorlagen.Fall, "division-definition-fall", 1220f, 270f)
        val ausgang = ausgang("division-definition-ausgang", "Ergebnis", 1570f, 300f)
        return karte(
            "division-definition",
            "Division mit Nullfall",
            listOf(zähler, nenner, nullwert, nullKnoten, gleich, kehrwert, produkt, fall, ausgang),
            listOf(
                verbinde("d1", nenner, "wert", gleich, "links"),
                verbinde("d2", nullKnoten, "wert", gleich, "rechts"),
                verbinde("d3", nenner, "wert", kehrwert, "zahl"),
                verbinde("d4", zähler, "wert", produkt, "a"),
                verbinde("d5", kehrwert, "wert", produkt, "b"),
                verbinde("d6", nullwert, "wert", fall, "wahr"),
                verbinde("d7", gleich, "aussage", fall, "aussage"),
                verbinde("d8", produkt, "wert", fall, "lüge"),
                verbinde("d9", fall, "wert", ausgang, "wert"),
            ),
        )
    }

    /** Für b>0 bleibt nur a·b⁻¹. */
    private fun positiverNenner(): KartenDaten {
        val zähler = eingang("division-positiv-zaehler", "Zähler a", 30f, 80f)
        val nenner = eingang("division-positiv-nenner", "Nenner b > 0", 30f, 270f)
        val kehrwert = knoten(MathematikKnotenVorlagen.Kehrwert, "division-positiv-kehrwert", 390f, 270f)
        val produkt = binäreMultiplikation("division-positiv-produkt", 720f, 150f)
        val ausgang = ausgang("division-positiv-ausgang", "Ergebnis", 1050f, 160f)
        return karte(
            "division-positiver-nenner",
            "Division bei positivem reellem Nenner",
            listOf(zähler, nenner, kehrwert, produkt, ausgang),
            listOf(
                verbinde("p1", nenner, "wert", kehrwert, "zahl"),
                verbinde("p2", zähler, "wert", produkt, "a"),
                verbinde("p3", kehrwert, "wert", produkt, "b"),
                verbinde("p4", produkt, "wert", ausgang, "wert"),
            ),
        )
    }

    /** a/b = (-a)/(-b), falls b<0. */
    private fun negativerNenner(): KartenDaten {
        val zähler = eingang("division-negativ-zaehler", "Zähler a", 30f, 70f)
        val nenner = eingang("division-negativ-nenner", "Nenner b < 0", 30f, 310f)
        val minusEins = zahl("division-negativ-minus-eins", "-1", 310f, 500f)
        val zählerNegiert = binäreMultiplikation("division-negativ-zaehler-negiert", 430f, 70f)
        val nennerNegiert = binäreMultiplikation("division-negativ-nenner-negiert", 430f, 310f)
        val kehrwert = knoten(MathematikKnotenVorlagen.Kehrwert, "division-negativ-kehrwert", 780f, 310f)
        val produkt = binäreMultiplikation("division-negativ-produkt", 1090f, 170f)
        val ausgang = ausgang("division-negativ-ausgang", "Ergebnis", 1420f, 180f)
        return karte(
            "division-negativer-nenner",
            "Vorzeichen zum Zähler verschieben",
            listOf(zähler, nenner, minusEins, zählerNegiert, nennerNegiert, kehrwert, produkt, ausgang),
            listOf(
                verbinde("n1", zähler, "wert", zählerNegiert, "a"),
                verbinde("n2", minusEins, "wert", zählerNegiert, "b"),
                verbinde("n3", nenner, "wert", nennerNegiert, "a"),
                verbinde("n4", minusEins, "wert", nennerNegiert, "b"),
                verbinde("n5", nennerNegiert, "wert", kehrwert, "zahl"),
                verbinde("n6", zählerNegiert, "wert", produkt, "a"),
                verbinde("n7", kehrwert, "wert", produkt, "b"),
                verbinde("n8", produkt, "wert", ausgang, "wert"),
            ),
        )
    }

    /** a/b = (a·conj(b))·(b·conj(b))⁻¹. */
    private fun komplexerNennerKartesisch(): KartenDaten {
        val zähler = eingang("division-kn-k-zaehler", "Zähler a", 30f, 80f)
        val nenner = eingang("division-kn-k-nenner", "Nenner b ∈ ℂ∖ℝ", 30f, 360f)
        val konjugierte = knoten(MathematikKnotenVorlagen.Konjugierte, "division-kn-k-konjugierte", 360f, 360f)
        val zählerProdukt = binäreMultiplikation("division-kn-k-zaehler-produkt", 690f, 90f)
        val nennerProdukt = binäreMultiplikation("division-kn-k-nenner-produkt", 690f, 360f)
        val kehrwert = knoten(MathematikKnotenVorlagen.Kehrwert, "division-kn-k-kehrwert", 1030f, 360f)
        val produkt = binäreMultiplikation("division-kn-k-produkt", 1320f, 190f)
        val ausgang = ausgang("division-kn-k-ausgang", "Ergebnis", 1650f, 200f)
        return karte(
            "division-komplexer-nenner-kartesisch",
            "Komplexen Nenner kartesisch rationalisieren",
            listOf(zähler, nenner, konjugierte, zählerProdukt, nennerProdukt, kehrwert, produkt, ausgang),
            listOf(
                verbinde("knk1", nenner, "wert", konjugierte, "zahl"),
                verbinde("knk2", zähler, "wert", zählerProdukt, "a"),
                verbinde("knk3", konjugierte, "wert", zählerProdukt, "b"),
                verbinde("knk4", nenner, "wert", nennerProdukt, "a"),
                verbinde("knk5", konjugierte, "wert", nennerProdukt, "b"),
                verbinde("knk6", nennerProdukt, "wert", kehrwert, "zahl"),
                verbinde("knk7", zählerProdukt, "wert", produkt, "a"),
                verbinde("knk8", kehrwert, "wert", produkt, "b"),
                verbinde("knk9", produkt, "wert", ausgang, "wert"),
            ),
        )
    }

    /** In Polarform werden Radien dividiert und Winkel modulo 2π subtrahiert. */
    private fun komplexerNennerPolar(): KartenDaten {
        val zähler = eingang("division-kn-p-zaehler", "Zähler a", 30f, 70f)
        val nenner = eingang("division-kn-p-nenner", "Nenner b ∈ ℂ∖ℝ", 30f, 420f)
        val radiusA = knoten(MathematikKnotenVorlagen.KomplexerRadius, "division-kn-p-radius-a", 350f, 50f)
        val winkelA = knoten(MathematikKnotenVorlagen.Winkel, "division-kn-p-winkel-a", 350f, 190f)
        val radiusB = knoten(MathematikKnotenVorlagen.KomplexerRadius, "division-kn-p-radius-b", 350f, 390f)
        val winkelB = knoten(MathematikKnotenVorlagen.Winkel, "division-kn-p-winkel-b", 350f, 530f)
        val minusEins = zahl("division-kn-p-minus-eins", "-1", 660f, 690f)
        val radiusKehrwert = knoten(MathematikKnotenVorlagen.Kehrwert, "division-kn-p-radius-kehrwert", 680f, 390f)
        val radiusProdukt = binäreMultiplikation("division-kn-p-radius-produkt", 980f, 110f)
        val winkelNegiert = binäreMultiplikation("division-kn-p-winkel-negiert", 680f, 530f)
        val winkelDifferenz = binäreAddition("division-kn-p-winkel-differenz", 1000f, 440f)
        val tupel = binäresTupel("division-kn-p-tupel", 1300f, 230f)
        val komplex = komplexAusTupel("division-kn-p-komplex", "polar", 1600f, 230f)
        val ausgang = ausgang("division-kn-p-ausgang", "Ergebnis", 1910f, 240f)
        return karte(
            "division-komplexer-nenner-polar",
            "Komplexe Division in Polarform (Winkel modulo 2π)",
            listOf(zähler, nenner, radiusA, winkelA, radiusB, winkelB, minusEins, radiusKehrwert, radiusProdukt, winkelNegiert, winkelDifferenz, tupel, komplex, ausgang),
            listOf(
                verbinde("knp1", zähler, "wert", radiusA, "zahl"),
                verbinde("knp2", zähler, "wert", winkelA, "zahl"),
                verbinde("knp3", nenner, "wert", radiusB, "zahl"),
                verbinde("knp4", nenner, "wert", winkelB, "zahl"),
                verbinde("knp5", radiusB, "wert", radiusKehrwert, "zahl"),
                verbinde("knp6", radiusA, "wert", radiusProdukt, "a"),
                verbinde("knp7", radiusKehrwert, "wert", radiusProdukt, "b"),
                verbinde("knp8", winkelB, "wert", winkelNegiert, "a"),
                verbinde("knp9", minusEins, "wert", winkelNegiert, "b"),
                verbinde("knp10", winkelA, "wert", winkelDifferenz, "a"),
                verbinde("knp11", winkelNegiert, "wert", winkelDifferenz, "b"),
                verbinde("knp12", radiusProdukt, "wert", tupel, "a"),
                verbinde("knp13", winkelDifferenz, "wert", tupel, "b"),
                verbinde("knp14", tupel, "tupel", komplex, "tupel"),
                verbinde("knp15", komplex, "zahl", ausgang, "wert"),
            ),
            zoom = .56f,
        )
    }

    /** (x+iy)/b = x/b + i·y/b für b>0. */
    private fun komplexerZählerKartesisch(): KartenDaten {
        val zähler = eingang("division-kz-k-zaehler", "Zähler a ∈ ℂ∖ℝ", 30f, 180f)
        val nenner = eingang("division-kz-k-nenner", "Nenner b > 0", 30f, 520f)
        val realteil = knoten(MathematikKnotenVorlagen.Realteil, "division-kz-k-realteil", 360f, 100f)
        val imaginärteil = knoten(MathematikKnotenVorlagen.Imaginärteil, "division-kz-k-imaginaerteil", 360f, 300f)
        val kehrwert = knoten(MathematikKnotenVorlagen.Kehrwert, "division-kz-k-kehrwert", 360f, 520f)
        val realQuotient = binäreMultiplikation("division-kz-k-real-quotient", 710f, 100f)
        val imaginärQuotient = binäreMultiplikation("division-kz-k-imaginaer-quotient", 710f, 330f)
        val tupel = binäresTupel("division-kz-k-tupel", 1050f, 210f)
        val komplex = komplexAusTupel("division-kz-k-komplex", "kartesisch", 1360f, 210f)
        val ausgang = ausgang("division-kz-k-ausgang", "Ergebnis", 1670f, 220f)
        return karte(
            "division-komplexer-zaehler-kartesisch",
            "Komplexen Zähler kartesisch zerlegen",
            listOf(zähler, nenner, realteil, imaginärteil, kehrwert, realQuotient, imaginärQuotient, tupel, komplex, ausgang),
            listOf(
                verbinde("kzk1", zähler, "wert", realteil, "zahl"),
                verbinde("kzk2", zähler, "wert", imaginärteil, "zahl"),
                verbinde("kzk3", nenner, "wert", kehrwert, "zahl"),
                verbinde("kzk4", realteil, "wert", realQuotient, "a"),
                verbinde("kzk5", kehrwert, "wert", realQuotient, "b"),
                verbinde("kzk6", imaginärteil, "wert", imaginärQuotient, "a"),
                verbinde("kzk7", kehrwert, "wert", imaginärQuotient, "b"),
                verbinde("kzk8", realQuotient, "wert", tupel, "a"),
                verbinde("kzk9", imaginärQuotient, "wert", tupel, "b"),
                verbinde("kzk10", tupel, "tupel", komplex, "tupel"),
                verbinde("kzk11", komplex, "zahl", ausgang, "wert"),
            ),
        )
    }

    /** In Polarform wird bei positivem reellem Nenner nur der Radius geteilt. */
    private fun komplexerZählerPolar(): KartenDaten {
        val zähler = eingang("division-kz-p-zaehler", "Zähler a ∈ ℂ∖ℝ", 30f, 160f)
        val nenner = eingang("division-kz-p-nenner", "Nenner b > 0", 30f, 480f)
        val radius = knoten(MathematikKnotenVorlagen.KomplexerRadius, "division-kz-p-radius", 360f, 100f)
        val winkel = knoten(MathematikKnotenVorlagen.Winkel, "division-kz-p-winkel", 360f, 300f)
        val kehrwert = knoten(MathematikKnotenVorlagen.Kehrwert, "division-kz-p-kehrwert", 360f, 500f)
        val radiusQuotient = binäreMultiplikation("division-kz-p-radius-quotient", 720f, 120f)
        val tupel = binäresTupel("division-kz-p-tupel", 1050f, 220f)
        val komplex = komplexAusTupel("division-kz-p-komplex", "polar", 1360f, 220f)
        val ausgang = ausgang("division-kz-p-ausgang", "Ergebnis", 1670f, 230f)
        return karte(
            "division-komplexer-zaehler-polar",
            "Komplexen Zähler polar teilen",
            listOf(zähler, nenner, radius, winkel, kehrwert, radiusQuotient, tupel, komplex, ausgang),
            listOf(
                verbinde("kzp1", zähler, "wert", radius, "zahl"),
                verbinde("kzp2", zähler, "wert", winkel, "zahl"),
                verbinde("kzp3", nenner, "wert", kehrwert, "zahl"),
                verbinde("kzp4", radius, "wert", radiusQuotient, "a"),
                verbinde("kzp5", kehrwert, "wert", radiusQuotient, "b"),
                verbinde("kzp6", radiusQuotient, "wert", tupel, "a"),
                verbinde("kzp7", winkel, "wert", tupel, "b"),
                verbinde("kzp8", tupel, "tupel", komplex, "tupel"),
                verbinde("kzp9", komplex, "zahl", ausgang, "wert"),
            ),
        )
    }

    private fun eingang(id: String, name: String, x: Float, y: Float): KnotenDaten =
        knoten(MathematikKnotenVorlagen.KartenEingang, id, x, y, parameter = mapOf("name" to name), anschlussArt = MathematikAnschlussArten.Zahl.id)

    private fun ausgang(id: String, name: String, x: Float, y: Float): KnotenDaten =
        knoten(MathematikKnotenVorlagen.KartenAusgang, id, x, y, parameter = mapOf("name" to name), anschlussArt = MathematikAnschlussArten.Zahl.id)

    private fun zahl(id: String, wert: String, x: Float, y: Float): KnotenDaten =
        knoten(MathematikKnotenVorlagen.Zahl, id, x, y, parameter = mapOf("wert" to wert))

    private fun binäreMultiplikation(id: String, x: Float, y: Float): KnotenDaten =
        knoten(MathematikKnotenVorlagen.Multiplikation, id, x, y, anschlussNamen = setOf("a", "b", "wert"))

    private fun binäreAddition(id: String, x: Float, y: Float): KnotenDaten =
        knoten(MathematikKnotenVorlagen.Addition, id, x, y, anschlussNamen = setOf("a", "b", "wert"))

    private fun binäresTupel(id: String, x: Float, y: Float): KnotenDaten =
        knoten(MathematikKnotenVorlagen.Tupel, id, x, y, anschlussNamen = setOf("a", "b", "tupel"))

    private fun komplexAusTupel(id: String, modus: String, x: Float, y: Float): KnotenDaten =
        knoten(MathematikKnotenVorlagen.KomplexAusTupel, id, x, y, parameter = mapOf("modus" to modus))

    private fun knoten(
        vorlage: KnotenVorlage,
        id: String,
        x: Float,
        y: Float,
        parameter: Map<String, String> = emptyMap(),
        anschlussNamen: Set<String>? = null,
        anschlussArt: AnschlussArtId? = null,
    ): KnotenDaten {
        val erzeugt = vorlage.erzeuge(GraphPunkt(x, y))
        return erzeugt.copy(
            id = KnotenId(id),
            anschlüsse = erzeugt.anschlüsse
                .filter { anschlussNamen == null || it.name in anschlussNamen }
                .map { anschluss ->
                    anschluss.copy(
                        id = AnschlussId("$id-${anschluss.richtung.name.lowercase()}-${anschluss.name}"),
                        art = anschlussArt ?: anschluss.art,
                    )
                },
            parameter = erzeugt.parameter + parameter,
        )
    }

    private fun verbinde(
        id: String,
        von: KnotenDaten,
        vonName: String,
        zu: KnotenDaten,
        zuName: String,
    ) = VerbindungDaten(
        id = VerbindungsId("division-$id"),
        von = AnschlussVerweis(von.id, von.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang && it.name == vonName }.id),
        zu = AnschlussVerweis(zu.id, zu.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang && it.name == zuName }.id),
    )

    private fun karte(
        id: String,
        name: String,
        knoten: List<KnotenDaten>,
        verbindungen: List<VerbindungDaten>,
        zoom: Float = .65f,
    ) = KartenDaten(
        id = KartenId(id),
        name = name,
        knoten = knoten,
        verbindungen = verbindungen,
        ansicht = AnsichtsFenster(zoom = zoom),
    )
}

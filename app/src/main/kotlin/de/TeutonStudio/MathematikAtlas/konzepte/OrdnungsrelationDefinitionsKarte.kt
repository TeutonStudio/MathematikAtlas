package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

internal object OrdnungsrelationDefinitionsKarten {
    private val reihenfolge = listOf("kleiner", "kleinerGleich", "größer", "größerGleich")

    fun katalogKonzept(varianten: List<KnotenVorlage>): KonzeptDefinition {
        val geordnet = reihenfolge.map { relation ->
            varianten.single { it.standardParameter["relation"] == relation }
        }
        return KonzeptDefinition(
            id = KonzeptId("ordnungsrelation"),
            name = "Ordnungsrelation",
            beschreibung = "Vergleicht zwei reelle Zahlterme mit einer auswählbaren strikten oder nichtstrikten Ordnungsrelation.",
            pfad = listOf("Aussagen", "Zahlenprädikate"),
            tags = geordnet.flatMap { listOf(it.name, it.kategorie, it.art, it.standardParameter.getValue("relation")) }.toSet(),
            knotenArten = setOf(MathematikKnotenVorlagen.ORDNUNGSRELATION_ART),
            reiter = geordnet.mapIndexed { index, vorlage ->
                KonzeptReiter(
                    id = if (index == 0) "definition" else "relation-${vorlage.standardParameter.getValue("relation")}",
                    titel = vorlage.name,
                    rolle = if (index == 0) KonzeptReiterRolle.Definition else KonzeptReiterRolle.Äquivalenz,
                    karte = ordnungsrelationDefinitionsKarte(vorlage, index),
                )
            },
        )
    }

    fun fürKnoten(knoten: KnotenDaten): KonzeptDefinition {
        val relation = normalisiere(knoten.parameter["relation"])
        val vorlage = vorlageFür(relation)
        return KonzeptDefinition(
            id = KonzeptId("ordnungsrelation-$relation"),
            name = "Ordnungsrelation: ${vorlage.name}",
            beschreibung = vorlage.beschreibung,
            pfad = listOf("Aussagen", "Zahlenprädikate"),
            tags = setOf(vorlage.name, vorlage.kategorie, vorlage.art, relation),
            knotenArten = setOf(MathematikKnotenVorlagen.ORDNUNGSRELATION_ART),
            reiter = listOf(
                KonzeptReiter(
                    id = "definition",
                    titel = "Definition",
                    rolle = KonzeptReiterRolle.Definition,
                    karte = ordnungsrelationDefinitionsKarte(vorlage, 0),
                ),
            ),
        )
    }

    fun normalisiere(relation: String?): String = when (relation) {
        "kleiner", "kleinerGleich", "größer", "größerGleich" -> relation
        else -> "kleiner"
    }

    private fun vorlageFür(relation: String): KnotenVorlage = when (relation) {
        "kleiner" -> MathematikKnotenVorlagen.Kleiner
        "kleinerGleich" -> MathematikKnotenVorlagen.KleinerGleich
        "größer" -> MathematikKnotenVorlagen.Größer
        "größerGleich" -> MathematikKnotenVorlagen.GrößerGleich
        else -> error("Unbekannte Ordnungsrelation '$relation'.")
    }
}

/**
 * Selbstbezugsfreie Definition über Quadrate:
 * x < a  genau dann, wenn ein c ∈ R\\{0} mit x + c² = a existiert;
 * die nichtstrikte Variante erlaubt c = 0. Größer-Relationen vertauschen die Seiten.
 */
internal fun ordnungsrelationDefinitionsKarte(
    vorlage: KnotenVorlage,
    variantenIndex: Int,
): KartenDaten {
    val relation = OrdnungsrelationDefinitionsKarten.normalisiere(vorlage.standardParameter["relation"])
    val strikt = relation == "kleiner" || relation == "größer"
    val linksIstKleiner = relation == "kleiner" || relation == "kleinerGleich"
    val prefix = "definition-ordnungsrelation-$relation-$variantenIndex"

    val links = ordnungsDokumentationsEingang(prefix, "links", GraphPunkt(20f, 110f), 0)
    val rechts = ordnungsDokumentationsEingang(prefix, "rechts", GraphPunkt(20f, 530f), 1)
    val c = ordnungsVorlagenKnoten(
        prefix,
        "variable-c",
        MathematikKnotenVorlagen.Variable,
        GraphPunkt(280f, 330f),
        mapOf("name" to "c", "werteVorrat" to "R"),
    )
    val zwei = ordnungsVorlagenKnoten(
        prefix,
        "zahl-zwei",
        MathematikKnotenVorlagen.Zahl,
        GraphPunkt(280f, 500f),
        mapOf("wert" to "2"),
    )
    val quadrat = ordnungsVorlagenKnoten(prefix, "quadrat", MathematikKnotenVorlagen.Potenz, GraphPunkt(560f, 360f))
    val addition = ordnungsVorlagenKnoten(prefix, "addition", MathematikKnotenVorlagen.Addition, GraphPunkt(850f, 240f))
    val gleichheit = ordnungsVorlagenKnoten(prefix, "gleichheit", MathematikKnotenVorlagen.Gleichheit, GraphPunkt(1150f, 300f))
    val aussageZuMethode = ordnungsVorlagenKnoten(
        prefix,
        "aussage-zu-methode",
        MathematikKnotenVorlagen.AussageZuMethode,
        GraphPunkt(1440f, 300f),
        mapOf("name" to "P", "argumentReihenfolge" to "c"),
    )
    val reelleZahlen = ordnungsVorlagenKnoten(prefix, "reelle-zahlen", MathematikKnotenVorlagen.ReelleZahlen, GraphPunkt(1430f, 70f))
    val nullKnoten = ordnungsVorlagenKnoten(
        prefix,
        "zahl-null",
        MathematikKnotenVorlagen.Zahl,
        GraphPunkt(1430f, 570f),
        mapOf("wert" to "0"),
    )
    val einzelNull = ordnungsVorlagenKnoten(prefix, "einzel-null", MathematikKnotenVorlagen.Einzelmenge, GraphPunkt(1710f, 570f))
    val differenz = ordnungsVorlagenKnoten(prefix, "ohne-null", MathematikKnotenVorlagen.Differenz, GraphPunkt(1990f, 130f))
    val iteration = ordnungsVorlagenKnoten(prefix, "iteration", MathematikKnotenVorlagen.IterierteDisjunktion, GraphPunkt(2290f, 300f))
    val ausgang = ordnungsVorlagenKnoten(
        prefix,
        "karten-ausgang",
        MathematikKnotenVorlagen.KartenAusgang,
        GraphPunkt(2610f, 300f),
        mapOf("name" to "aussage"),
    )

    val knoten = buildList {
        addAll(listOf(links, rechts, c, zwei, quadrat, addition, gleichheit, aussageZuMethode, reelleZahlen))
        if (strikt) addAll(listOf(nullKnoten, einzelNull, differenz))
        addAll(listOf(iteration, ausgang))
    }

    val verbindungen = buildList {
        fun verbinde(von: KnotenDaten, vonName: String, zu: KnotenDaten, zuName: String, kennung: String) {
            add(
                VerbindungDaten(
                    id = VerbindungsId("$prefix-$kennung"),
                    von = AnschlussVerweis(von.id, von.ordnungsAnschluss(vonName, AnschlussRichtung.Ausgang).id),
                    zu = AnschlussVerweis(zu.id, zu.ordnungsAnschluss(zuName, AnschlussRichtung.Eingang).id),
                ),
            )
        }

        verbinde(c, "wert", quadrat, "basis", "quadrat-basis")
        verbinde(zwei, "wert", quadrat, "exponent", "quadrat-exponent")
        if (linksIstKleiner) {
            verbinde(links, "wert", addition, "a", "addition-links")
            verbinde(quadrat, "wert", addition, "b", "addition-quadrat")
            verbinde(addition, "wert", gleichheit, "links", "gleichheit-summe")
            verbinde(rechts, "wert", gleichheit, "rechts", "gleichheit-rechts")
        } else {
            verbinde(rechts, "wert", addition, "a", "addition-rechts")
            verbinde(quadrat, "wert", addition, "b", "addition-quadrat")
            verbinde(addition, "wert", gleichheit, "links", "gleichheit-summe")
            verbinde(links, "wert", gleichheit, "rechts", "gleichheit-links")
        }
        verbinde(gleichheit, "aussage", aussageZuMethode, "term", "methode-term")
        verbinde(aussageZuMethode, "methode", iteration, "methode", "iteration-methode")
        if (strikt) {
            verbinde(reelleZahlen, "menge", differenz, "links", "differenz-reelle")
            verbinde(nullKnoten, "wert", einzelNull, "element", "einzel-null")
            verbinde(einzelNull, "menge", differenz, "rechts", "differenz-null")
            verbinde(differenz, "menge", iteration, "indexmenge", "iteration-index")
        } else {
            verbinde(reelleZahlen, "menge", iteration, "indexmenge", "iteration-index")
        }
        verbinde(iteration, "aussage", ausgang, "wert", "karten-ausgang")
    }

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition von ${vorlage.name}",
        knoten = knoten,
        verbindungen = verbindungen,
    )
}

private fun ordnungsDokumentationsEingang(
    prefix: String,
    name: String,
    position: GraphPunkt,
    index: Int,
): KnotenDaten {
    val id = KnotenId("$prefix-eingang-$index")
    return KnotenDaten(
        id = id,
        art = TestDefinitionsKarten.KONZEPT_EINGANG_ART,
        name = name,
        position = position,
        größe = GraphGröße(220f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("${id.wert}-wert"),
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Zahl.id,
            ),
        ),
        parameter = mapOf(
            "typ" to MathematikAnschlussArten.Zahl.id.wert,
            "variabel" to "false",
            "folgtEingang" to "",
        ),
    )
}

private fun ordnungsVorlagenKnoten(
    prefix: String,
    kennung: String,
    vorlage: KnotenVorlage,
    position: GraphPunkt,
    parameter: Map<String, String> = emptyMap(),
): KnotenDaten {
    val id = KnotenId("$prefix-$kennung")
    return vorlage.erzeuge(position).copy(
        id = id,
        anschlüsse = vorlage.anschlüsse.mapIndexed { index, anschluss ->
            anschluss.copy(id = AnschlussId("${id.wert}-anschluss-$index"))
        },
        parameter = vorlage.standardParameter + parameter,
    )
}

private fun KnotenDaten.ordnungsAnschluss(name: String, richtung: AnschlussRichtung): AnschlussDaten =
    anschlüsse.single { it.name == name && it.richtung == richtung }

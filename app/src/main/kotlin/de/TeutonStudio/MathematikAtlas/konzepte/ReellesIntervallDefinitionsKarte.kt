package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

/** Azyklische, selbstbezugfreie Definitionskarte des reellen Intervalls. */
internal fun reellesIntervallDefinitionsKarte(
    vorlage: KnotenVorlage,
    variantenIndex: Int,
): KartenDaten {
    val prefix = "definition-reelles-intervall-$variantenIndex"

    val links = dokumentationsEingang(prefix, "links", MathematikAnschlussArten.Zahl.id, GraphPunkt(20f, 40f), 0)
    val linksOffen = dokumentationsEingang(
        prefix,
        "linksOffen",
        MathematikAnschlussArten.Aussage.id,
        GraphPunkt(20f, 230f),
        1,
        vorschauWahrheitswert = false,
    )
    val rechts = dokumentationsEingang(prefix, "rechts", MathematikAnschlussArten.Zahl.id, GraphPunkt(20f, 610f), 2)
    val rechtsOffen = dokumentationsEingang(
        prefix,
        "rechtsOffen",
        MathematikAnschlussArten.Aussage.id,
        GraphPunkt(20f, 800f),
        3,
        vorschauWahrheitswert = false,
    )

    val x = vorlagenKnoten(
        prefix,
        "variable-x",
        MathematikKnotenVorlagen.Variable,
        GraphPunkt(240.20312f, 374.42264f),
        mapOf("name" to "x", "werteVorrat" to "R"),
    )
    val linksKleiner = vorlagenKnoten(prefix, "links-kleiner", MathematikKnotenVorlagen.Kleiner, GraphPunkt(560f, 40f))
    val linksKleinerGleich = vorlagenKnoten(prefix, "links-kleiner-gleich", MathematikKnotenVorlagen.KleinerGleich, GraphPunkt(560f, 220f))
    val rechtsKleiner = vorlagenKnoten(prefix, "rechts-kleiner", MathematikKnotenVorlagen.Kleiner, GraphPunkt(560f, 610f))
    val rechtsKleinerGleich = vorlagenKnoten(prefix, "rechts-kleiner-gleich", MathematikKnotenVorlagen.KleinerGleich, GraphPunkt(560f, 790f))

    val linkerFall = vorlagenKnoten(prefix, "linker-fall", MathematikKnotenVorlagen.Fall, GraphPunkt(850f, 140f))
    val rechterFall = vorlagenKnoten(prefix, "rechter-fall", MathematikKnotenVorlagen.Fall, GraphPunkt(850f, 690f))
    val konjunktion = vorlagenKnoten(prefix, "konjunktion", MathematikKnotenVorlagen.Konjunktion, GraphPunkt(1160f, 420f))
    val termZuMethode = vorlagenKnoten(
        prefix,
        "term-zu-methode",
        MathematikKnotenVorlagen.TermZuMethode,
        GraphPunkt(1533.1195f, 395.10303f),
        mapOf("name" to "f", "argumentReihenfolge" to ""),
    )
    val reelleZahlen = vorlagenKnoten(
        prefix,
        "reelle-zahlen",
        MathematikKnotenVorlagen.ReelleZahlen,
        GraphPunkt(1568.4684f, 262.04926f),
    )
    val mengenfilter = vorlagenKnoten(
        prefix,
        "mengenfilter",
        MathematikKnotenVorlagen.Mengenfilter,
        GraphPunkt(1971.1384f, 325.91025f),
    )
    val ausgang = vorlagenKnoten(
        prefix,
        "karten-ausgang",
        MathematikKnotenVorlagen.KartenAusgang,
        GraphPunkt(2362.3706f, 318.3821f),
        mapOf("name" to "menge"),
    )

    val knoten = listOf(
        links,
        linksOffen,
        rechts,
        rechtsOffen,
        linksKleiner,
        linksKleinerGleich,
        rechtsKleiner,
        rechtsKleinerGleich,
        linkerFall,
        rechterFall,
        konjunktion,
        ausgang,
        x,
        termZuMethode,
        mengenfilter,
        reelleZahlen,
    )

    val verbindungen = buildList {
        fun verbinde(
            von: KnotenDaten,
            vonName: String,
            zu: KnotenDaten,
            zuName: String,
            kennung: String,
        ) {
            add(
                VerbindungDaten(
                    id = VerbindungsId("$prefix-$kennung"),
                    von = AnschlussVerweis(von.id, von.anschluss(vonName, AnschlussRichtung.Ausgang).id),
                    zu = AnschlussVerweis(zu.id, zu.anschluss(zuName, AnschlussRichtung.Eingang).id),
                ),
            )
        }

        verbinde(links, "wert", linksKleiner, "links", "links-kleiner-links")
        verbinde(x, "wert", linksKleiner, "rechts", "links-kleiner-x")
        verbinde(links, "wert", linksKleinerGleich, "links", "links-kleiner-gleich-links")
        verbinde(x, "wert", linksKleinerGleich, "rechts", "links-kleiner-gleich-x")

        verbinde(x, "wert", rechtsKleiner, "links", "rechts-kleiner-x")
        verbinde(rechts, "wert", rechtsKleiner, "rechts", "rechts-kleiner-rechts")
        verbinde(x, "wert", rechtsKleinerGleich, "links", "rechts-kleiner-gleich-x")
        verbinde(rechts, "wert", rechtsKleinerGleich, "rechts", "rechts-kleiner-gleich-rechts")

        verbinde(linksKleiner, "aussage", linkerFall, "wahr", "linker-fall-wahr")
        verbinde(linksOffen, "wert", linkerFall, "aussage", "linker-fall-aussage")
        verbinde(linksKleinerGleich, "aussage", linkerFall, "lüge", "linker-fall-luege")

        verbinde(rechtsKleiner, "aussage", rechterFall, "wahr", "rechter-fall-wahr")
        verbinde(rechtsOffen, "wert", rechterFall, "aussage", "rechter-fall-aussage")
        verbinde(rechtsKleinerGleich, "aussage", rechterFall, "lüge", "rechter-fall-luege")

        verbinde(linkerFall, "wert", konjunktion, "a", "konjunktion-links")
        verbinde(rechterFall, "wert", konjunktion, "b", "konjunktion-rechts")
        verbinde(konjunktion, "aussage", termZuMethode, "term", "term-zu-methode")
        verbinde(termZuMethode, "methode", mengenfilter, "methode", "mengenfilter-methode")
        verbinde(reelleZahlen, "menge", mengenfilter, "menge", "mengenfilter-reelle-zahlen")
        verbinde(mengenfilter, "menge", ausgang, "wert", "karten-ausgang")
    }

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition von ${vorlage.name}",
        knoten = knoten,
        verbindungen = verbindungen,
    )
}

private fun dokumentationsEingang(
    prefix: String,
    name: String,
    art: AnschlussArtId,
    position: GraphPunkt,
    index: Int,
    vorschauWahrheitswert: Boolean? = null,
): KnotenDaten {
    val id = KnotenId("$prefix-eingang-$index")
    return KnotenDaten(
        id = id,
        art = TestDefinitionsKarten.KONZEPT_EINGANG_ART,
        name = name,
        position = position,
        größe = GraphGröße(240f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("${id.wert}-wert"),
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = art,
            ),
        ),
        parameter = buildMap {
            put("typ", art.wert)
            put("variabel", "false")
            put("folgtEingang", "")
            vorschauWahrheitswert?.let { put("vorschauWert", it.toString()) }
        },
    )
}

private fun vorlagenKnoten(
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

private fun KnotenDaten.anschluss(name: String, richtung: AnschlussRichtung): AnschlussDaten =
    anschlüsse.single { it.name == name && it.richtung == richtung }

package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.ITERIERTE_SUMME_TUPEL_MODUS
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

internal val ITERIERTE_KONZEPT_ARTEN: Set<String> = setOf(
    MathematikKnotenVorlagen.IterierteSumme.art,
    MathematikKnotenVorlagen.IteriertesProdukt.art,
    MathematikKnotenVorlagen.ITERIERTE_AUSSAGENVERKNÜPFUNG_ART,
    MathematikKnotenVorlagen.IterierteVereinigung.art,
    MathematikKnotenVorlagen.IterierterSchnitt.art,
    MathematikKnotenVorlagen.IteriertesKartesischesProdukt.art,
)

internal fun iteriertesOperatorKonzept(varianten: List<KnotenVorlage>): KonzeptDefinition {
    val konzeptVarianten = varianten.filterNot { vorlage ->
        vorlage.standardParameter["eingabeModus"] == ITERIERTE_SUMME_TUPEL_MODUS
    }
    require(konzeptVarianten.isNotEmpty())
    val erste = konzeptVarianten.first()
    val mehrereVarianten = konzeptVarianten.size > 1
    val reiter = buildList {
        konzeptVarianten.forEachIndexed { index, vorlage ->
            val namensPrefix = if (mehrereVarianten) "${vorlage.name}: " else ""
            add(
                KonzeptReiter(
                    id = if (index == 0) "definition" else "definition-${iterierterSlug(vorlage.name)}-$index",
                    titel = "${namensPrefix}Definition",
                    rolle = if (index == 0) KonzeptReiterRolle.Definition else KonzeptReiterRolle.Spezialfall,
                    karte = iterierteDefinitionsKarte(vorlage, index * 10),
                ),
            )
            add(
                KonzeptReiter(
                    id = "${iterierterSlug(vorlage.name)}-leere-indexmenge-$index",
                    titel = "${namensPrefix}Leere Indexmenge",
                    rolle = KonzeptReiterRolle.Spezialfall,
                    karte = iterierteOperatorSpezialfallKarte(
                        vorlage,
                        index,
                        IterierterSpezialfall.LeereIndexmenge,
                    ),
                ),
            )
            add(
                KonzeptReiter(
                    id = "${iterierterSlug(vorlage.name)}-endlicher-spezialfall-$index",
                    titel = "${namensPrefix}Endlicher Spezialfall",
                    rolle = KonzeptReiterRolle.Beispiel,
                    karte = iterierteOperatorSpezialfallKarte(
                        vorlage,
                        index,
                        IterierterSpezialfall.EndlicheIndexmenge,
                    ),
                ),
            )
        }
    }

    return KonzeptDefinition(
        id = KonzeptId(iterierterSlug(erste.art.removePrefix("mathematik."))),
        name = konzeptVarianten.joinToString(" / ") { it.name },
        beschreibung = konzeptVarianten.map(KnotenVorlage::beschreibung).distinct().joinToString(" "),
        pfad = erste.kategorie.split(':').map(String::trim).filter(String::isNotBlank),
        tags = konzeptVarianten.flatMap { listOf(it.name, it.kategorie, it.art) }.toSet(),
        knotenArten = setOf(erste.art),
        reiter = reiter,
    )
}

private fun iterierterSlug(text: String): String = text.lowercase()
    .replace("ä", "ae")
    .replace("ö", "oe")
    .replace("ü", "ue")
    .replace("ß", "ss")
    .map { if (it.isLetterOrDigit()) it else '-' }
    .joinToString("")
    .replace(Regex("-+"), "-")
    .trim('-')

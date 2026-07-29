package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

/**
 * Vollständiger Definitionskartenkatalog aller fest registrierten Knotenarten.
 *
 * Definitionskarten verwenden absichtlich keine auswählbaren Mathematikknoten:
 * Der Anschlussvertrag wird durch interne Dokumentationsknoten beschrieben. Dadurch
 * sind direkter Selbstbezug und zirkuläre Definitionsabhängigkeiten konstruktiv ausgeschlossen.
 */
object TestDefinitionsKarten {
    internal const val KONZEPT_REGEL_ART = "konzept.regel"
    internal const val KONZEPT_EINGANG_ART = "konzept.eingang"
    internal const val KONZEPT_AUSGANG_ART = "konzept.ausgang"

    private val festeVorlagen: List<KnotenVorlage> by lazy {
        MathematikKnotenVorlagen.alle +
            ErweiterteMathematikKnotenVorlagen.alle +
            GeometrieKnotenVorlagen.alle
    }

    val alle: List<KonzeptDefinition> by lazy {
        festeVorlagen
            .groupBy { it.art }
            .values
            .map(::konzeptFür)
            .sortedWith(compareBy<KonzeptDefinition> { it.pfad.joinToString("/") }.thenBy { it.name })
            .also { katalog ->
                val fehler = prüfe(katalog)
                require(fehler.isEmpty()) {
                    fehler.joinToString(prefix = "Ungültiger Definitionskartenkatalog:\n- ", separator = "\n- ")
                }
            }
    }

    private val nachId: Map<KonzeptId, KonzeptDefinition> by lazy { alle.associateBy { it.id } }
    private val nachArt: Map<KnotenArtId, KonzeptDefinition> by lazy {
        alle.flatMap { konzept -> konzept.knotenArten.map { art -> art to konzept } }.toMap()
    }

    fun finde(id: KonzeptId): KonzeptDefinition? = nachId[id]

    fun fürKnoten(knoten: KnotenDaten): KonzeptDefinition? = nachArt[knoten.art]

    internal fun validierungsFehler(): List<String> = prüfe(alle)

    private fun konzeptFür(varianten: List<KnotenVorlage>): KonzeptDefinition {
        val erste = varianten.first()
        return when (erste.art) {
            "mathematik.zahl" -> SpezielleDefinitionsKarten.zahl()
            "mathematik.subtraktion" -> SpezielleDefinitionsKarten.subtraktion(erste)
            else -> generischesKonzept(varianten)
        }
    }

    private fun generischesKonzept(varianten: List<KnotenVorlage>): KonzeptDefinition {
        val erste = varianten.first()
        val reiter = varianten.mapIndexed { index, vorlage ->
            KonzeptReiter(
                id = if (index == 0) "definition" else "variante-${slug(vorlage.name)}",
                titel = if (varianten.size == 1) "Definition" else vorlage.name,
                rolle = if (index == 0) KonzeptReiterRolle.Definition else KonzeptReiterRolle.Spezialfall,
                karte = definitionsKarte(vorlage, index),
            )
        }
        return KonzeptDefinition(
            id = KonzeptId(slug(erste.art.toString().removePrefix("mathematik."))),
            name = varianten.joinToString(" / ") { it.name },
            beschreibung = varianten.joinToString(" ") { it.beschreibung }.eindeutigeSätze(),
            pfad = erste.kategorie.split(':').map { it.trim() }.filter { it.isNotBlank() },
            tags = varianten.flatMap { listOf(it.name, it.kategorie, it.art.toString()) }.toSet(),
            knotenArten = setOf(erste.art),
            reiter = reiter,
        )
    }

    internal fun definitionsKarte(vorlage: KnotenVorlage, variantenIndex: Int): KartenDaten {
        val prefix = "definition-${slug(vorlage.art.toString())}-${slug(vorlage.name)}-$variantenIndex"
        val eingänge = vorlage.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
        val ausgänge = vorlage.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Ausgang }
            .sortedBy { it.reihenfolge }
        val regel = regelKnoten(prefix, vorlage, maxOf(eingänge.size, ausgänge.size, 1))
        val eingangsKnoten = eingänge.mapIndexed { index, anschluss ->
            schnittstellenKnoten(prefix, anschluss, index, eingang = true)
        }
        val ausgangsKnoten = ausgänge.mapIndexed { index, anschluss ->
            schnittstellenKnoten(prefix, anschluss, index, eingang = false)
        }
        val regelEingänge = regel.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
        val regelAusgänge = regel.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Ausgang }
            .sortedBy { it.reihenfolge }
        val verbindungen = buildList {
            eingangsKnoten.forEachIndexed { index, quelle ->
                add(VerbindungDaten(
                    id = VerbindungsId("$prefix-e-$index"),
                    von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                    zu = AnschlussVerweis(regel.id, regelEingänge[index].id),
                ))
            }
            ausgangsKnoten.forEachIndexed { index, ziel ->
                add(VerbindungDaten(
                    id = VerbindungsId("$prefix-a-$index"),
                    von = AnschlussVerweis(regel.id, regelAusgänge[index].id),
                    zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.single().id),
                ))
            }
        }
        return KartenDaten(
            id = KartenId(prefix),
            name = "Definition von ${vorlage.name}",
            knoten = eingangsKnoten + regel + ausgangsKnoten,
            verbindungen = verbindungen,
        )
    }

    private fun regelKnoten(prefix: String, vorlage: KnotenVorlage, zeilen: Int): KnotenDaten = KnotenDaten(
        id = KnotenId("$prefix-regel"),
        art = KONZEPT_REGEL_ART,
        name = vorlage.name,
        position = GraphPunkt(370f, 55f),
        größe = GraphGröße(380f, maxOf(150f, 88f + zeilen * 30f)),
        anschlüsse = vorlage.anschlüsse.mapIndexed { index, anschluss ->
            anschluss.copy(id = AnschlussId("$prefix-regel-$index"))
        },
        parameter = mapOf(
            "regel" to vorlage.beschreibung,
            "knotenArt" to vorlage.art.toString(),
            "kategorie" to vorlage.kategorie,
        ),
    )

    private fun schnittstellenKnoten(
        prefix: String,
        anschluss: AnschlussDaten,
        index: Int,
        eingang: Boolean,
    ): KnotenDaten {
        val id = "$prefix-${if (eingang) "eingang" else "ausgang"}-$index"
        return KnotenDaten(
            id = KnotenId(id),
            art = if (eingang) KONZEPT_EINGANG_ART else KONZEPT_AUSGANG_ART,
            name = anschluss.name,
            position = GraphPunkt(if (eingang) 30f else 810f, 55f + index * 118f),
            größe = GraphGröße(260f, 92f),
            anschlüsse = listOf(AnschlussDaten(
                id = AnschlussId("$id-wert"),
                name = "wert",
                richtung = if (eingang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang,
                kante = if (eingang) AnschlussKante.Rechts else AnschlussKante.Links,
                art = anschluss.art,
            )),
            parameter = mapOf(
                "typ" to anschluss.art.wert,
                "variabel" to anschluss.kannSichErweitern.toString(),
                "folgtEingang" to anschluss.artFolgtEingang.orEmpty(),
            ),
        )
    }

    private fun prüfe(katalog: List<KonzeptDefinition>): List<String> = buildList {
        val festeArten = festeVorlagen.map { it.art }.toSet()
        val abgedeckteArten = katalog.flatMap { it.knotenArten }.toSet()
        (festeArten - abgedeckteArten).forEach { add("Fehlende Definitionskarte für $it") }
        (abgedeckteArten - festeArten).forEach { add("Unbekannte definierte Knotenart $it") }

        katalog.groupBy { it.id }.filterValues { it.size > 1 }.keys.forEach {
            add("Doppelte Konzept-ID $it")
        }
        katalog.flatMap { konzept -> konzept.knotenArten.map { art -> art to konzept.id } }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.distinct().size > 1 }
            .forEach { (art, ids) -> add("Knotenart $art ist mehreren Konzepten zugeordnet: ${ids.distinct()}") }

        val ids = katalog.map { it.id }.toSet()
        katalog.forEach { konzept ->
            konzept.reiter.forEach { reiter ->
                reiter.karte.knoten.filter { it.art in konzept.knotenArten }.forEach { knoten ->
                    add("Selbstbezug in ${konzept.id}/${reiter.id}: ${knoten.id}")
                }
            }
            konzept.navigation.values.filterNot { it in ids }.forEach {
                add("Fehlendes Navigationsziel $it in ${konzept.id}")
            }
        }

        val abhängigkeiten = katalog.associate { konzept ->
            val verwendet = konzept.reiter
                .flatMap { it.karte.knoten }
                .map { it.art }
                .filter { it in festeArten }
                .toSet()
            konzept.knotenArten.single() to (verwendet - konzept.knotenArten)
        }
        findeZyklus(abhängigkeiten)?.let {
            add("Zirkuläre Definitionsabhängigkeit: ${it.joinToString(" -> ")}")
        }
    }

    private fun findeZyklus(graph: Map<KnotenArtId, Set<KnotenArtId>>): List<KnotenArtId>? {
        val aktiv = linkedSetOf<KnotenArtId>()
        val erledigt = mutableSetOf<KnotenArtId>()

        fun besuche(art: KnotenArtId): List<KnotenArtId>? {
            if (art in aktiv) {
                val pfad = aktiv.toList()
                return pfad.drop(pfad.indexOf(art)) + art
            }
            if (art in erledigt) return null
            aktiv += art
            graph[art].orEmpty().forEach { abhängig ->
                besuche(abhängig)?.let { return it }
            }
            aktiv -= art
            erledigt += art
            return null
        }

        graph.keys.forEach { art -> besuche(art)?.let { return it } }
        return null
    }

    private fun slug(text: String): String = text.lowercase()
        .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .replace(Regex("-+"), "-")
        .trim('-')

    private fun String.eindeutigeSätze(): String = split(Regex("(?<=\\.)\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" ")
}

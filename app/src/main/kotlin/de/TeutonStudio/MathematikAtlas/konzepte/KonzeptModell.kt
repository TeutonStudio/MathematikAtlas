package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.GeometrieKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen

@JvmInline
value class KonzeptId(val wert: String) {
    override fun toString(): String = wert
}

enum class KonzeptReiterRolle { Definition, Spezialfall, Beispiel, Äquivalenz }

data class KonzeptReiter(
    val id: String,
    val titel: String,
    val rolle: KonzeptReiterRolle,
    val karte: KartenDaten,
)

data class KonzeptDefinition(
    val id: KonzeptId,
    val name: String,
    val beschreibung: String,
    val pfad: List<String>,
    val tags: Set<String>,
    val knotenArten: Set<KnotenArtId>,
    val reiter: List<KonzeptReiter>,
) {
    init {
        require(reiter.count { it.rolle == KonzeptReiterRolle.Definition } == 1) {
            "$name benötigt genau einen Definitionsreiter."
        }
    }
}

object TestDefinitionsKarten {
    const val KONZEPT_REGEL_ART = "konzept.regel"
    const val KONZEPT_EINGANG_ART = "konzept.eingang"
    const val KONZEPT_AUSGANG_ART = "konzept.ausgang"

    val festeVorlagen: List<KnotenVorlage> by lazy {
        (MathematikKnotenVorlagen.alle + MengenraumKnotenVorlagen.alle + GeometrieKnotenVorlagen.alle)
            .distinctBy { it.art to it.name }
    }

    val alle: List<KonzeptDefinition> by lazy {
        festeVorlagen
            .groupBy(KnotenVorlage::art)
            .values
            .map(::konzeptFür)
            .sortedWith(compareBy<KonzeptDefinition> { it.pfad.joinToString("/") }.thenBy { it.name })
            .also { katalog ->
                val fehler = validierungsFehler(katalog)
                require(fehler.isEmpty()) { fehler.joinToString(prefix = "Ungültiger Konzeptkatalog:\n- ", separator = "\n- ") }
            }
    }

    private val nachArt: Map<KnotenArtId, KonzeptDefinition> by lazy {
        alle.flatMap { konzept -> konzept.knotenArten.map { art -> art to konzept } }.toMap()
    }

    fun fürKnoten(knoten: KnotenDaten): KonzeptDefinition? = nachArt[knoten.art]

    fun validierungsFehler(): List<String> = validierungsFehler(alle)

    private fun konzeptFür(varianten: List<KnotenVorlage>): KonzeptDefinition {
        val erste = varianten.first()
        val reiter = varianten.mapIndexed { index, vorlage ->
            KonzeptReiter(
                id = if (index == 0) "definition" else "variante-${slug(vorlage.name)}-$index",
                titel = if (varianten.size == 1) "Definition" else vorlage.name,
                rolle = if (index == 0) KonzeptReiterRolle.Definition else KonzeptReiterRolle.Spezialfall,
                karte = definitionsKarte(vorlage, index),
            )
        }
        return KonzeptDefinition(
            id = KonzeptId(slug(erste.art.removePrefix("mathematik."))),
            name = varianten.joinToString(" / ") { it.name },
            beschreibung = varianten.map(KnotenVorlage::beschreibung).distinct().joinToString(" "),
            pfad = erste.kategorie.split(':').map(String::trim).filter(String::isNotBlank),
            tags = varianten.flatMap { listOf(it.name, it.kategorie, it.art) }.toSet(),
            knotenArten = setOf(erste.art),
            reiter = reiter,
        )
    }

    internal fun definitionsKarte(vorlage: KnotenVorlage, variantenIndex: Int): KartenDaten {
        val prefix = "definition-${slug(vorlage.art)}-${slug(vorlage.name)}-$variantenIndex"
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
                add(
                    VerbindungDaten(
                        id = VerbindungsId("$prefix-e-$index"),
                        von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                        zu = AnschlussVerweis(regel.id, regelEingänge[index].id),
                    ),
                )
            }
            ausgangsKnoten.forEachIndexed { index, ziel ->
                add(
                    VerbindungDaten(
                        id = VerbindungsId("$prefix-a-$index"),
                        von = AnschlussVerweis(regel.id, regelAusgänge[index].id),
                        zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.single().id),
                    ),
                )
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
        größe = GraphGröße(390f, maxOf(150f, 88f + zeilen * 30f)),
        anschlüsse = vorlage.anschlüsse.mapIndexed { index, anschluss ->
            anschluss.copy(id = AnschlussId("$prefix-regel-$index"))
        },
        parameter = mapOf(
            "regel" to vorlage.beschreibung,
            "knotenArt" to vorlage.art,
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
            position = GraphPunkt(if (eingang) 30f else 830f, 55f + index * 118f),
            größe = GraphGröße(260f, 92f),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("$id-wert"),
                    name = "wert",
                    richtung = if (eingang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang,
                    kante = if (eingang) AnschlussKante.Rechts else AnschlussKante.Links,
                    art = anschluss.art,
                ),
            ),
            parameter = mapOf(
                "typ" to anschluss.art.wert,
                "variabel" to anschluss.kannSichErweitern.toString(),
                "folgtEingang" to anschluss.artFolgtEingang.orEmpty(),
            ),
        )
    }

    private fun validierungsFehler(katalog: List<KonzeptDefinition>): List<String> = buildList {
        val festeArten = festeVorlagen.map(KnotenVorlage::art).toSet()
        val abgedeckteArten = katalog.flatMap(KonzeptDefinition::knotenArten).toSet()
        (festeArten - abgedeckteArten).forEach { add("Fehlende Definitionskarte für $it") }
        (abgedeckteArten - festeArten).forEach { add("Unbekannte Definitionskarte für $it") }
        katalog.groupBy(KonzeptDefinition::id).filterValues { it.size > 1 }.keys.forEach {
            add("Doppelte Konzept-ID $it")
        }
        katalog.forEach { konzept ->
            konzept.reiter.forEach { reiter ->
                reiter.karte.knoten.filter { it.art in konzept.knotenArten }.forEach { knoten ->
                    add("Selbstbezug in ${konzept.id}/${reiter.id}: ${knoten.id}")
                }
            }
        }
    }

    private fun slug(text: String): String = text.lowercase()
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .replace(Regex("-+"), "-")
        .trim('-')
}

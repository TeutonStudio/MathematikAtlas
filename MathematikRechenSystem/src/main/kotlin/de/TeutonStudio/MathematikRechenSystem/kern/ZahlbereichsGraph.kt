package de.TeutonStudio.MathematikRechenSystem.kern

@JvmInline
value class ZahlbereichsId(val wert: String) {
    init {
        require(wert.isNotBlank()) { "Eine Zahlbereichs-ID darf nicht leer sein." }
    }

    override fun toString(): String = wert
}

data class ZahlbereichsKnoten(
    val id: ZahlbereichsId,
    val latex: String,
    val name: String,
)

enum class BereichsRelationArt {
    TEILMENGE,
    KANONISCHE_EINBETTUNG,
    ISOMORPHE_NORMALISIERUNG,
    DARSTELLUNG,
    HYPERERWEITERUNG,
}

data class BereichsRelation(
    val quelle: ZahlbereichsId,
    val ziel: ZahlbereichsId,
    val art: BereichsRelationArt,
    val verlustfrei: Boolean = true,
    val kanonisch: Boolean = true,
    val voraussetzungen: Set<String> = emptySet(),
    val adapterId: String? = null,
)

data class EinbettungsPfad(
    val start: ZahlbereichsId,
    val ziel: ZahlbereichsId,
    val relationen: List<BereichsRelation>,
) {
    val voraussetzungen: Set<String> = relationen.flatMapTo(linkedSetOf()) { it.voraussetzungen }
}

enum class GemeinsamerBereichStatus {
    EINDEUTIG,
    MEHRDEUTIG,
    NICHT_VORHANDEN,
}

data class GemeinsamerBereichErgebnis(
    val status: GemeinsamerBereichStatus,
    val bereich: ZahlbereichsId? = null,
    val alternativen: List<ZahlbereichsId> = emptyList(),
    val pfade: Map<ZahlbereichsId, List<EinbettungsPfad>> = emptyMap(),
    val voraussetzungen: Set<String> = emptySet(),
) {
    init {
        when (status) {
            GemeinsamerBereichStatus.EINDEUTIG -> require(bereich != null && alternativen.isEmpty())
            GemeinsamerBereichStatus.MEHRDEUTIG -> require(bereich == null && alternativen.size >= 2)
            GemeinsamerBereichStatus.NICHT_VORHANDEN -> require(bereich == null && alternativen.isEmpty())
        }
    }
}

class ZahlbereichsGraph(
    knoten: Iterable<ZahlbereichsKnoten>,
    relationen: Iterable<BereichsRelation>,
) {
    private val knotenListe = knoten.toList()
    private val knotenNachId: Map<ZahlbereichsId, ZahlbereichsKnoten> = knotenListe.associateBy { it.id }
    private val relationenListe: List<BereichsRelation> = relationen.toList()
    private val ausgehend: Map<ZahlbereichsId, List<BereichsRelation>> = relationenListe.groupBy { it.quelle }

    init {
        require(knotenNachId.isNotEmpty()) { "Ein Zahlbereichsgraph benötigt mindestens einen Knoten." }
        require(knotenNachId.size == knotenListe.size) { "Zahlbereichs-IDs müssen eindeutig sein." }
        relationenListe.forEach { relation ->
            require(relation.quelle in knotenNachId) { "Unbekannte Quelle ${relation.quelle}." }
            require(relation.ziel in knotenNachId) { "Unbekanntes Ziel ${relation.ziel}." }
        }
    }

    fun knoten(id: ZahlbereichsId): ZahlbereichsKnoten? = knotenNachId[id]

    fun relationen(): List<BereichsRelation> = relationenListe.toList()

    fun istAutomatischNutzbar(relation: BereichsRelation): Boolean =
        relation.verlustfrei && relation.kanonisch && relation.art != BereichsRelationArt.DARSTELLUNG

    fun kuerzesterAutomatischerPfad(
        quelle: ZahlbereichsId,
        ziel: ZahlbereichsId,
    ): EinbettungsPfad? {
        if (quelle !in knotenNachId || ziel !in knotenNachId) return null
        if (quelle == ziel) return EinbettungsPfad(quelle, ziel, emptyList())

        val besucht = mutableSetOf(quelle)
        val warteschlange = ArrayDeque<Pair<ZahlbereichsId, List<BereichsRelation>>>()
        warteschlange.add(quelle to emptyList())

        while (warteschlange.isNotEmpty()) {
            val (aktuell, pfad) = warteschlange.removeFirst()
            for (relation in ausgehend[aktuell].orEmpty().filter(::istAutomatischNutzbar)) {
                if (!besucht.add(relation.ziel)) continue
                val neuerPfad = pfad + relation
                if (relation.ziel == ziel) return EinbettungsPfad(quelle, ziel, neuerPfad)
                warteschlange.add(relation.ziel to neuerPfad)
            }
        }
        return null
    }

    fun istAutomatischErreichbar(
        quelle: ZahlbereichsId,
        ziel: ZahlbereichsId,
    ): Boolean = kuerzesterAutomatischerPfad(quelle, ziel) != null

    fun gemeinsameMinimaleZielbereiche(
        bereiche: Iterable<ZahlbereichsId>,
    ): GemeinsamerBereichErgebnis {
        val quellen = bereiche.toList()
        require(quellen.isNotEmpty()) { "Mindestens ein Ausgangsbereich ist erforderlich." }
        require(quellen.all { it in knotenNachId }) { "Alle Ausgangsbereiche müssen registriert sein." }

        val pfadeNachZiel = knotenNachId.keys.mapNotNull { kandidat ->
            val pfade = quellen.map { quelle -> kuerzesterAutomatischerPfad(quelle, kandidat) }
            if (pfade.all { it != null }) kandidat to pfade.filterNotNull() else null
        }.toMap()

        if (pfadeNachZiel.isEmpty()) {
            return GemeinsamerBereichErgebnis(GemeinsamerBereichStatus.NICHT_VORHANDEN)
        }

        val minimaleZiele = pfadeNachZiel.keys.filter { kandidat ->
            pfadeNachZiel.keys.none { anderes ->
                anderes != kandidat && istAutomatischErreichbar(anderes, kandidat)
            }
        }.sortedBy { it.wert }

        val ausgewaehltePfade = minimaleZiele.associateWith { pfadeNachZiel.getValue(it) }
        val voraussetzungen = ausgewaehltePfade.values
            .flatten()
            .flatMapTo(linkedSetOf()) { it.voraussetzungen }

        return if (minimaleZiele.size == 1) {
            GemeinsamerBereichErgebnis(
                status = GemeinsamerBereichStatus.EINDEUTIG,
                bereich = minimaleZiele.single(),
                pfade = ausgewaehltePfade,
                voraussetzungen = voraussetzungen,
            )
        } else {
            GemeinsamerBereichErgebnis(
                status = GemeinsamerBereichStatus.MEHRDEUTIG,
                alternativen = minimaleZiele,
                pfade = ausgewaehltePfade,
                voraussetzungen = voraussetzungen,
            )
        }
    }
}

object ZahlbereichsIds {
    val NATUERLICH_POSITIV = ZahlbereichsId("N")
    val NATUERLICH_MIT_NULL = ZahlbereichsId("N0")
    val GANZ = ZahlbereichsId("Z")
    val RATIONAL = ZahlbereichsId("Q")
    val REELL = ZahlbereichsId("R")
    val KOMPLEX = ZahlbereichsId("C")
    val QUATERNION = ZahlbereichsId("H")
    val GAUSS_GANZ = ZahlbereichsId("Z[i]")
    val REELL_ADJUNGIERT_I = ZahlbereichsId("R[i]")
    val HYPER_REELL = ZahlbereichsId("*R")
    val HYPER_KOMPLEX = ZahlbereichsId("*C")
    val KOMPLEX_ALS_M2_REELL = ZahlbereichsId("darstellung.C.M2R")
    val QUATERNION_ALS_M2_KOMPLEX = ZahlbereichsId("darstellung.H.M2C")
}

data class ZahlbereichsDarstellung(
    val id: String,
    val quelle: ZahlbereichsId,
    val ziel: ZahlbereichsId,
    val definitionsLatex: String,
)

object StandardZahlbereichsGraph {
    private fun knoten(id: ZahlbereichsId, latex: String, name: String) = ZahlbereichsKnoten(id, latex, name)

    val knoten: List<ZahlbereichsKnoten> = listOf(
        knoten(ZahlbereichsIds.NATUERLICH_POSITIV, "\\mathbb N", "Positive natürliche Zahlen"),
        knoten(ZahlbereichsIds.NATUERLICH_MIT_NULL, "\\mathbb N_0", "Nichtnegative natürliche Zahlen"),
        knoten(ZahlbereichsIds.GANZ, "\\mathbb Z", "Ganze Zahlen"),
        knoten(ZahlbereichsIds.RATIONAL, "\\mathbb Q", "Rationale Zahlen"),
        knoten(ZahlbereichsIds.REELL, "\\mathbb R", "Reelle Zahlen"),
        knoten(ZahlbereichsIds.KOMPLEX, "\\mathbb C", "Komplexe Zahlen"),
        knoten(ZahlbereichsIds.QUATERNION, "\\mathbb H", "Hamilton-Quaternionen"),
        knoten(ZahlbereichsIds.GAUSS_GANZ, "\\mathbb Z[i]", "Gaußsche ganze Zahlen"),
        knoten(ZahlbereichsIds.REELL_ADJUNGIERT_I, "\\mathbb R[i]", "Reelle Körperadjunktion mit i"),
        knoten(ZahlbereichsIds.HYPER_REELL, "{}^*\\mathbb R", "Hyperreelle Zahlen"),
        knoten(ZahlbereichsIds.HYPER_KOMPLEX, "{}^*\\mathbb C", "Hyperkomplexe Zahlen"),
        knoten(ZahlbereichsIds.KOMPLEX_ALS_M2_REELL, "M_2(\\mathbb R)", "Reelle 2×2-Matrixdarstellung komplexer Zahlen"),
        knoten(ZahlbereichsIds.QUATERNION_ALS_M2_KOMPLEX, "M_2(\\mathbb C)", "Komplexe 2×2-Matrixdarstellung der Quaternionen"),
    )

    val relationen: List<BereichsRelation> = listOf(
        BereichsRelation(ZahlbereichsIds.NATUERLICH_POSITIV, ZahlbereichsIds.NATUERLICH_MIT_NULL, BereichsRelationArt.TEILMENGE),
        BereichsRelation(ZahlbereichsIds.NATUERLICH_MIT_NULL, ZahlbereichsIds.GANZ, BereichsRelationArt.TEILMENGE),
        BereichsRelation(ZahlbereichsIds.GANZ, ZahlbereichsIds.RATIONAL, BereichsRelationArt.KANONISCHE_EINBETTUNG),
        BereichsRelation(ZahlbereichsIds.RATIONAL, ZahlbereichsIds.REELL, BereichsRelationArt.KANONISCHE_EINBETTUNG),
        BereichsRelation(ZahlbereichsIds.REELL, ZahlbereichsIds.KOMPLEX, BereichsRelationArt.KANONISCHE_EINBETTUNG),
        BereichsRelation(ZahlbereichsIds.KOMPLEX, ZahlbereichsIds.QUATERNION, BereichsRelationArt.KANONISCHE_EINBETTUNG),
        BereichsRelation(ZahlbereichsIds.GAUSS_GANZ, ZahlbereichsIds.KOMPLEX, BereichsRelationArt.TEILMENGE),
        BereichsRelation(
            ZahlbereichsIds.REELL_ADJUNGIERT_I,
            ZahlbereichsIds.KOMPLEX,
            BereichsRelationArt.ISOMORPHE_NORMALISIERUNG,
            adapterId = "zahlbereich.normalisierung.Ri.C",
        ),
        BereichsRelation(
            ZahlbereichsIds.REELL,
            ZahlbereichsIds.HYPER_REELL,
            BereichsRelationArt.HYPERERWEITERUNG,
            adapterId = "zahlbereich.hyper.R",
        ),
        BereichsRelation(
            ZahlbereichsIds.KOMPLEX,
            ZahlbereichsIds.HYPER_KOMPLEX,
            BereichsRelationArt.HYPERERWEITERUNG,
            adapterId = "zahlbereich.hyper.C",
        ),
        BereichsRelation(
            ZahlbereichsIds.HYPER_REELL,
            ZahlbereichsIds.HYPER_KOMPLEX,
            BereichsRelationArt.KANONISCHE_EINBETTUNG,
            adapterId = "zahlbereich.einbettung.*R.*C",
        ),
        BereichsRelation(
            ZahlbereichsIds.KOMPLEX,
            ZahlbereichsIds.KOMPLEX_ALS_M2_REELL,
            BereichsRelationArt.DARSTELLUNG,
            kanonisch = false,
            adapterId = "zahlbereich.darstellung.C.M2R",
        ),
        BereichsRelation(
            ZahlbereichsIds.QUATERNION,
            ZahlbereichsIds.QUATERNION_ALS_M2_KOMPLEX,
            BereichsRelationArt.DARSTELLUNG,
            kanonisch = false,
            adapterId = "zahlbereich.darstellung.H.M2C",
        ),
    )

    val graph = ZahlbereichsGraph(knoten, relationen)

    val darstellungen: List<ZahlbereichsDarstellung> = listOf(
        ZahlbereichsDarstellung(
            id = "zahlbereich.darstellung.C.M2R",
            quelle = ZahlbereichsIds.KOMPLEX,
            ziel = ZahlbereichsIds.KOMPLEX_ALS_M2_REELL,
            definitionsLatex = "a+bi\\longleftrightarrow\\begin{pmatrix}a&-b\\\\b&a\\end{pmatrix}",
        ),
        ZahlbereichsDarstellung(
            id = "zahlbereich.darstellung.H.M2C",
            quelle = ZahlbereichsIds.QUATERNION,
            ziel = ZahlbereichsIds.QUATERNION_ALS_M2_KOMPLEX,
            definitionsLatex = "a+bi+cj+dk\\longleftrightarrow\\begin{pmatrix}a+bi&c+di\\\\-c+di&a-bi\\end{pmatrix}",
        ),
    )
}

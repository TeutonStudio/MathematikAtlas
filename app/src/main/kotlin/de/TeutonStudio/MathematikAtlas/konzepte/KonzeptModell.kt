package de.TeutonStudio.MathematikAtlas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.TeutonStudio.KnotenKartenVerwalter.daten.*

@JvmInline
value class KonzeptId(val wert: String) {
    override fun toString(): String = wert
}

enum class KonzeptReiterRolle {
    Definition,
    Spezialfall,
    Äquivalenz,
    Beispiel,
}

data class KonzeptReiter(
    val id: String,
    val titel: String,
    val rolle: KonzeptReiterRolle,
    val karte: KartenDaten,
)

data class KonzeptKnotenSchlüssel(
    val reiterId: String,
    val knotenId: KnotenId,
)

data class KonzeptErkundungsFreigabe(
    val reiterId: String,
    val knotenId: KnotenId,
    val parameter: String,
    val beschriftung: String,
)

data class KonzeptDefinition(
    val id: KonzeptId,
    val name: String,
    val beschreibung: String,
    val pfad: List<String>,
    val tags: Set<String>,
    val knotenArten: Set<KnotenArtId>,
    val reiter: List<KonzeptReiter>,
    val navigation: Map<KonzeptKnotenSchlüssel, KonzeptId> = emptyMap(),
    val erkundungsFreigaben: List<KonzeptErkundungsFreigabe> = emptyList(),
) {
    init {
        require(reiter.isNotEmpty()) { "Ein Konzept benötigt mindestens einen Reiter." }
        require(reiter.count { it.rolle == KonzeptReiterRolle.Definition } == 1) {
            "Ein Konzept benötigt genau einen Definitionsreiter."
        }
        require(reiter.map { it.id }.distinct().size == reiter.size) { "Reiter-IDs müssen eindeutig sein." }
    }

    val sortierteReiter: List<KonzeptReiter>
        get() = reiter.sortedWith(compareBy<KonzeptReiter> { it.rolle != KonzeptReiterRolle.Definition }.thenBy { it.titel })

    fun reiter(id: String): KonzeptReiter = reiter.firstOrNull { it.id == id } ?: sortierteReiter.first()
}

data class KonzeptNavigationsEintrag(
    val konzeptId: KonzeptId,
    val reiterId: String,
    val ausgewählterKnoten: KnotenId? = null,
    val parameterÄnderungen: Map<String, Map<KnotenId, Map<String, String>>> = emptyMap(),
)

@Stable
class KonzeptSitzung {
    var pfad by mutableStateOf<List<KonzeptNavigationsEintrag>>(emptyList())
        private set

    val istAktiv: Boolean get() = pfad.isNotEmpty()
    val aktuellerEintrag: KonzeptNavigationsEintrag? get() = pfad.lastOrNull()
    val aktuellesKonzept: KonzeptDefinition? get() = aktuellerEintrag?.let { TestDefinitionsKarten.finde(it.konzeptId) }
    val aktiverReiter: KonzeptReiter? get() = aktuellesKonzept?.reiter(aktuellerEintrag?.reiterId.orEmpty())

    fun öffne(id: KonzeptId) {
        val konzept = TestDefinitionsKarten.finde(id) ?: return
        pfad = listOf(neuerEintrag(konzept))
    }

    fun navigiere(id: KonzeptId) {
        val konzept = TestDefinitionsKarten.finde(id) ?: return
        if (pfad.lastOrNull()?.konzeptId == id) return
        if (pfad.size >= 32) return
        pfad = pfad + neuerEintrag(konzept)
    }

    fun navigiereÜber(knoten: KnotenDaten) {
        val eintrag = aktuellerEintrag ?: return
        val explizit = TestDefinitionsKarten.finde(eintrag.konzeptId)
            ?.navigation
            ?.get(KonzeptKnotenSchlüssel(eintrag.reiterId, knoten.id))
        val ziel = explizit ?: TestDefinitionsKarten.fürKnoten(knoten)?.id ?: return
        navigiere(ziel)
    }

    fun springeZu(index: Int) {
        if (index !in pfad.indices) return
        pfad = pfad.take(index + 1)
    }

    fun schließe() {
        pfad = emptyList()
    }

    fun wähleReiter(id: String) {
        val eintrag = aktuellerEintrag ?: return
        val konzept = aktuellesKonzept ?: return
        if (konzept.reiter.none { it.id == id }) return
        ersetzeLetzten(eintrag.copy(reiterId = id, ausgewählterKnoten = null))
    }

    fun wähleKnoten(id: KnotenId?) {
        val eintrag = aktuellerEintrag ?: return
        ersetzeLetzten(eintrag.copy(ausgewählterKnoten = id))
    }

    fun setzeParameter(knotenId: KnotenId, schlüssel: String, wert: String) {
        val eintrag = aktuellerEintrag ?: return
        val konzept = aktuellesKonzept ?: return
        val erlaubt = konzept.erkundungsFreigaben.any {
            it.reiterId == eintrag.reiterId && it.knotenId == knotenId && it.parameter == schlüssel
        }
        if (!erlaubt) return

        val reiterÄnderungen = eintrag.parameterÄnderungen[eintrag.reiterId].orEmpty()
        val knotenÄnderungen = reiterÄnderungen[knotenId].orEmpty() + (schlüssel to wert)
        val neueReiterÄnderungen = reiterÄnderungen + (knotenId to knotenÄnderungen)
        ersetzeLetzten(
            eintrag.copy(
                parameterÄnderungen = eintrag.parameterÄnderungen + (eintrag.reiterId to neueReiterÄnderungen),
            ),
        )
    }

    fun setzeAktuellenReiterZurück() {
        val eintrag = aktuellerEintrag ?: return
        ersetzeLetzten(
            eintrag.copy(
                parameterÄnderungen = eintrag.parameterÄnderungen - eintrag.reiterId,
                ausgewählterKnoten = null,
            ),
        )
    }

    fun aktuelleKarte(): KartenDaten? {
        val eintrag = aktuellerEintrag ?: return null
        val basis = aktiverReiter?.karte ?: return null
        val änderungen = eintrag.parameterÄnderungen[eintrag.reiterId].orEmpty()
        return basis.copy(
            knoten = basis.knoten.map { knoten ->
                val parameter = änderungen[knoten.id].orEmpty()
                if (parameter.isEmpty()) knoten else knoten.copy(parameter = knoten.parameter + parameter)
            },
        )
    }

    fun freigabenFürAuswahl(): List<KonzeptErkundungsFreigabe> {
        val eintrag = aktuellerEintrag ?: return emptyList()
        val ausgewählt = eintrag.ausgewählterKnoten ?: return emptyList()
        return aktuellesKonzept?.erkundungsFreigaben.orEmpty().filter {
            it.reiterId == eintrag.reiterId && it.knotenId == ausgewählt
        }
    }

    private fun neuerEintrag(konzept: KonzeptDefinition): KonzeptNavigationsEintrag =
        KonzeptNavigationsEintrag(
            konzeptId = konzept.id,
            reiterId = konzept.sortierteReiter.first().id,
        )

    private fun ersetzeLetzten(eintrag: KonzeptNavigationsEintrag) {
        if (pfad.isEmpty()) return
        pfad = pfad.dropLast(1) + eintrag
    }
}

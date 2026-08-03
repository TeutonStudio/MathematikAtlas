package de.TeutonStudio.MathematikRechenSystem.kern

/** Semantische Herkunft einer Variablen; der sichtbare Name ist ausdrücklich keine Identität. */
enum class FormelVariablenArt { FREI, GEBUNDEN, EXTERN }

data class FormelVariablenQuelle(
    val id: String,
    val anzeigeName: String,
    val art: FormelVariablenArt,
    val typ: FormelTyp,
    val werteVorrat: MengenAusdruck? = null,
) {
    init {
        require(id.isNotBlank())
        require(anzeigeName.isNotBlank())
    }
}

enum class FormelQuantorArt { FUER_ALLE, ES_EXISTIERT }

data class FormelQuantorBindung(
    val bindungsId: String,
    val quantor: FormelQuantorArt,
    val quelleId: String,
    /** Wurzel des lexikalischen Gültigkeitsbereichs. */
    val geltungsbereichAusdruckId: String,
) {
    init {
        require(bindungsId.isNotBlank())
        require(quelleId.isNotBlank())
        require(geltungsbereichAusdruckId.isNotBlank())
    }
}

data class FormelPraedikatsVertrag(
    val id: String,
    val name: String,
    val argumentQuellenIds: List<String>,
    val zielTyp: FormelTyp = FormelTyp.AUSSAGE,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(argumentQuellenIds.distinct().size == argumentQuellenIds.size)
        require(zielTyp == FormelTyp.AUSSAGE) {
            "Ein Prädikat muss die Zielmenge {Wahr, Falsch} beziehungsweise den Typ AUSSAGE besitzen."
        }
    }
}

/**
 * Verlustarmer Bindungsvertrag über dem gemeinsamen Ausdrucksmodell.
 *
 * `variablenVorkommen` ordnet jede Variable über ihre Ausdrucks-ID einer stabilen
 * Quelle zu. Dadurch dürfen verschiedene Quellen denselben sichtbaren Namen tragen.
 */
data class GebundeneFormel(
    val wurzel: FormelAusdruck,
    val quellen: List<FormelVariablenQuelle>,
    val variablenVorkommen: Map<String, String>,
    val quantoren: List<FormelQuantorBindung> = emptyList(),
    val praedikate: List<FormelPraedikatsVertrag> = emptyList(),
)

sealed interface FormelBindungsPruefung {
    data object Gueltig : FormelBindungsPruefung
    data class Ungueltig(val gruende: List<String>) : FormelBindungsPruefung
}

object FormelBindungsPruefer {
    fun pruefe(formel: GebundeneFormel): FormelBindungsPruefung {
        val gruende = mutableListOf<String>()
        val quellen = formel.quellen.associateBy { it.id }
        if (quellen.size != formel.quellen.size) gruende += "Variablenquellen besitzen doppelte IDs."
        val ausdrucke = formel.wurzel.ausdruckeNachId(gruende)

        formel.variablenVorkommen.forEach { (ausdruckId, quelleId) ->
            if (ausdrucke[ausdruckId] !is FormelAusdruck.Variable) {
                gruende += "Das Vorkommen $ausdruckId bezeichnet keine Variable."
            }
            if (quelleId !in quellen) gruende += "Die Variablenquelle $quelleId fehlt."
        }
        ausdrucke.values.filterIsInstance<FormelAusdruck.Variable>().forEach { variable ->
            if (variable.id !in formel.variablenVorkommen) {
                gruende += "Die Variable ${variable.id} besitzt keine stabile Quelle."
            }
        }

        val bindungsIds = formel.quantoren.map { it.bindungsId }
        if (bindungsIds.distinct().size != bindungsIds.size) gruende += "Quantorbindungen besitzen doppelte IDs."
        val quantorenNachQuelle = formel.quantoren.groupBy { it.quelleId }
        formel.quantoren.forEach { bindung ->
            val quelle = quellen[bindung.quelleId]
            if (quelle == null) {
                gruende += "Die Quantorquelle ${bindung.quelleId} fehlt."
                return@forEach
            }
            if (quelle.art != FormelVariablenArt.GEBUNDEN) {
                gruende += "Quantor ${bindung.bindungsId} bindet keine als GEBUNDEN markierte Quelle."
            }
            val geltungsbereich = ausdrucke[bindung.geltungsbereichAusdruckId]
            if (geltungsbereich == null) {
                gruende += "Der Gültigkeitsbereich ${bindung.geltungsbereichAusdruckId} fehlt."
                return@forEach
            }
            val enthalten = geltungsbereich.ausdrucksIds()
            formel.variablenVorkommen
                .filterValues { it == bindung.quelleId }
                .keys
                .filterNot { it in enthalten }
                .forEach { vorkommen ->
                    gruende += "Das gebundene Vorkommen $vorkommen liegt außerhalb des Gültigkeitsbereichs."
                }
        }
        formel.quellen.forEach { quelle ->
            when (quelle.art) {
                FormelVariablenArt.GEBUNDEN -> if (quantorenNachQuelle[quelle.id].orEmpty().size != 1) {
                    gruende += "Die gebundene Quelle ${quelle.id} benötigt genau einen Quantor."
                }
                FormelVariablenArt.FREI,
                FormelVariablenArt.EXTERN,
                -> if (quelle.id in quantorenNachQuelle) {
                    gruende += "Die ${quelle.art}-Quelle ${quelle.id} darf nicht durch einen Quantor gebunden werden."
                }
            }
        }

        val praedikatsIds = formel.praedikate.map { it.id }
        if (praedikatsIds.distinct().size != praedikatsIds.size) gruende += "Prädikate besitzen doppelte IDs."
        formel.praedikate.forEach { vertrag ->
            vertrag.argumentQuellenIds.filterNot { it in quellen }.forEach { quelleId ->
                gruende += "Prädikat ${vertrag.id} verweist auf die fehlende Quelle $quelleId."
            }
        }

        return if (gruende.isEmpty()) FormelBindungsPruefung.Gueltig
        else FormelBindungsPruefung.Ungueltig(gruende.distinct())
    }
}

private fun FormelAusdruck.ausdruckeNachId(gruende: MutableList<String>): Map<String, FormelAusdruck> {
    val ergebnis = linkedMapOf<String, FormelAusdruck>()
    val aktiv = mutableSetOf<String>()
    fun besuche(ausdruck: FormelAusdruck) {
        val vorhanden = ergebnis[ausdruck.id]
        if (vorhanden != null && vorhanden != ausdruck) {
            gruende += "Ausdrucks-ID ${ausdruck.id} bezeichnet mehrere Strukturen."
            return
        }
        if (!aktiv.add(ausdruck.id)) {
            gruende += "Zyklische Ausdrucksstruktur bei ${ausdruck.id}."
            return
        }
        ergebnis[ausdruck.id] = ausdruck
        if (ausdruck is FormelAusdruck.Operation) {
            ausdruck.argumente.sortedBy { it.position }.forEach { besuche(it.ausdruck) }
        }
        aktiv.remove(ausdruck.id)
    }
    besuche(this)
    return ergebnis
}

private fun FormelAusdruck.ausdrucksIds(): Set<String> = buildSet {
    fun besuche(ausdruck: FormelAusdruck) {
        if (!add(ausdruck.id)) return
        if (ausdruck is FormelAusdruck.Operation) {
            ausdruck.argumente.forEach { besuche(it.ausdruck) }
        }
    }
    besuche(this@ausdrucksIds)
}

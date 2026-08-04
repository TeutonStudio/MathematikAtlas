package de.TeutonStudio.MathematikKnoten.enzyklopädie

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen
import de.TeutonStudio.MathematikKnoten.konzeptknoten.KonzeptKnotenRegister

class EnzyklopädieRegister private constructor(
    val alle: List<WissensEintrag>,
) {
    private val nachId: Map<WissensId, WissensEintrag> = alle.associateBy(WissensEintrag::id)
    private val nachAlias: Map<String, WissensEintrag> = buildMap {
        alle.forEach { eintrag ->
            eintrag.aliase.forEach { alias -> put(alias, eintrag) }
        }
    }
    private val nachKnotenArt: Map<KnotenArtId, List<WissensEintrag>> =
        alle.flatMap { eintrag -> eintrag.knotenArten.map { art -> art to eintrag } }
            .groupBy({ it.first }, { it.second })

    fun finde(id: WissensId): WissensEintrag? = nachId[id]

    fun finde(idOderAlias: String): WissensEintrag? =
        nachId[WissensId(idOderAlias)] ?: nachAlias[idOderAlias]

    fun fürKnotenArt(art: KnotenArtId): List<WissensEintrag> = nachKnotenArt[art].orEmpty()

    fun inFachPfad(pfad: FachPfad): List<WissensEintrag> =
        alle.filter { pfad in it.fachPfade }

    fun suche(text: String): List<WissensEintrag> {
        val teile = text.normalisierteSuchteile()
        if (teile.isEmpty()) return alle
        return alle.mapNotNull { eintrag ->
            val suchraum = eintrag.alleSuchtexte.joinToString(" ").lowercase()
            val treffer = teile.count(suchraum::contains)
            if (treffer == 0) null else eintrag to treffer
        }.sortedWith(
            compareByDescending<Pair<WissensEintrag, Int>> { it.second }
                .thenBy { it.first.titel }
                .thenBy { it.first.id.wert },
        ).map(Pair<WissensEintrag, Int>::first)
    }

    fun validierungsFehler(): List<String> = buildList {
        alle.groupBy(WissensEintrag::id).filterValues { it.size > 1 }.keys.forEach {
            add("Doppelte Wissens-ID: $it")
        }
        val bekannteIds = alle.mapTo(mutableSetOf(), WissensEintrag::id)
        alle.forEach { eintrag ->
            (eintrag.voraussetzungen - bekannteIds).forEach {
                add("${eintrag.id}: unbekannte Voraussetzung $it")
            }
            eintrag.beziehungen.filter { it.ziel !in bekannteIds }.forEach {
                add("${eintrag.id}: unbekanntes Beziehungsziel ${it.ziel}")
            }
            if (eintrag.verfügbarkeit == WissensVerfügbarkeit.Verfügbar && eintrag.primäreDefinition == null) {
                add("${eintrag.id}: keine primäre Definition")
            }
        }
        addAll(RechnerFamilienKatalog.validierungsFehler())
        RechnerFamilienKatalog.alle.filter { operator -> finde(operator.wissensId) == null }.forEach {
            add("${it.stabileId}: unbekannte Wissens-ID ${it.wissensId}")
        }
    }

    companion object {
        fun ausVorlagen(vorlagen: List<KnotenVorlage>): EnzyklopädieRegister {
            val register = EnzyklopädieRegister(KonzeptKnotenRegister.erstelle(vorlagen))
            val fehler = register.validierungsFehler()
            require(fehler.isEmpty()) {
                fehler.joinToString(prefix = "Ungültiges Enzyklopädie-Register:\n- ", separator = "\n- ")
            }
            return register
        }
    }
}

object MathematikEnzyklopädie {
    val standard: EnzyklopädieRegister by lazy {
        EnzyklopädieRegister.ausVorlagen(alleMathematikDefinitionsVorlagen())
    }
}

private fun String.normalisierteSuchteile(): List<String> =
    trim().lowercase().split(Regex("\\s+")).filter(String::isNotBlank)

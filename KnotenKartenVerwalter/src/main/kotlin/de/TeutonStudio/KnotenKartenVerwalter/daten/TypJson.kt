package de.TeutonStudio.KnotenKartenVerwalter.daten

import org.json.JSONArray
import org.json.JSONObject

internal fun AnschlussVertrag.zuJson(): JSONObject = JSONObject().apply {
    put("typ", typ.zuJson())
    if (anforderungen.isNotEmpty()) {
        put("anforderungen", JSONArray().apply {
            anforderungen.sortedWith(compareBy<TypAnforderung> { it.art.name }.thenBy { it.id.wert }).forEach { anforderung ->
                put(JSONObject().apply {
                    put("art", anforderung.art.name)
                    put("id", anforderung.id.wert)
                })
            }
        })
    }
}

internal fun anschlussVertragVonJson(json: JSONObject?): AnschlussVertrag {
    if (json == null) return AnschlussVertrag()
    val anforderungen = json.optJSONArray("anforderungen")?.let { array ->
        buildSet {
            for (index in 0 until array.length()) {
                val eintrag = array.optJSONObject(index) ?: continue
                val art = runCatching { enumValueOf<TypAnforderungsArt>(eintrag.optString("art")) }.getOrNull() ?: continue
                val id = eintrag.optString("id").takeIf(String::isNotBlank) ?: continue
                add(TypAnforderung(art, TypId(id)))
            }
        }
    }.orEmpty()
    return AnschlussVertrag(
        typ = typAusdruckVonJson(json.optJSONObject("typ")) ?: TypAusdruck.Unbekannt,
        anforderungen = anforderungen,
    )
}

internal fun TypAusdruck.zuJson(): JSONObject = JSONObject().apply {
    when (this@zuJson) {
        TypAusdruck.Beliebig -> put("art", "beliebig")
        TypAusdruck.Unbekannt -> put("art", "unbekannt")
        is TypAusdruck.Atom -> { put("art", "atom"); put("id", id.wert) }
        is TypAusdruck.Variable -> { put("art", "variable"); put("id", id.wert) }
        is TypAusdruck.Parameterisiert -> {
            put("art", "parameterisiert")
            put("konstruktor", konstruktor.wert)
            put("argumente", JSONArray().apply { argumente.forEach { put(it.zuJson()) } })
        }
        is TypAusdruck.Vereinigung -> {
            put("art", "vereinigung")
            put("alternativen", JSONArray().apply { alternativen.forEach { put(it.zuJson()) } })
        }
    }
}

internal fun typAusdruckVonJson(json: JSONObject?): TypAusdruck? {
    if (json == null) return null
    return when (json.optString("art")) {
        "beliebig" -> TypAusdruck.Beliebig
        "unbekannt" -> TypAusdruck.Unbekannt
        "atom" -> json.optString("id").takeIf(String::isNotBlank)?.let { TypAusdruck.Atom(TypId(it)) }
        "variable" -> json.optString("id").takeIf(String::isNotBlank)?.let { TypAusdruck.Variable(TypVariablenId(it)) }
        "parameterisiert" -> {
            val konstruktor = json.optString("konstruktor").takeIf(String::isNotBlank) ?: return null
            val argumente = typListeVonJson(json.optJSONArray("argumente"))
            argumente.takeIf { it.isNotEmpty() }?.let { TypAusdruck.Parameterisiert(TypId(konstruktor), it) }
        }
        "vereinigung" -> typListeVonJson(json.optJSONArray("alternativen")).takeIf { it.isNotEmpty() }?.let(TypAusdruck::Vereinigung)
        else -> null
    }
}

private fun typListeVonJson(array: JSONArray?): List<TypAusdruck> {
    if (array == null) return emptyList()
    return buildList {
        for (index in 0 until array.length()) typAusdruckVonJson(array.optJSONObject(index))?.let(::add)
    }
}

internal fun TypInferenzRegel.zuJson(): JSONObject = JSONObject().apply {
    when (val regel = this@zuJson) {
        is TypInferenzRegel.FolgtEingang -> { put("art", "folgtEingang"); put("eingang", regel.eingang) }
        is TypInferenzRegel.GemeinsameOberart -> { put("art", "gemeinsameOberart"); put("eingänge", JSONArray(regel.eingänge)) }
        is TypInferenzRegel.VereinigungAusEingängen -> { put("art", "vereinigungAusEingängen"); put("eingänge", JSONArray(regel.eingänge)) }
        is TypInferenzRegel.AbbildungVonEingang -> {
            put("art", "abbildungVonEingang")
            put("eingang", regel.eingang)
            put("fälle", JSONArray().apply {
                regel.fälle.forEach { fall -> put(JSONObject().put("von", fall.von.zuJson()).put("zu", fall.zu.zuJson())) }
            })
        }
        is TypInferenzRegel.Priorisierung -> {
            put("art", "priorisierung")
            put("eingänge", JSONArray(regel.eingänge))
            put("prioritäten", JSONArray().apply { regel.prioritäten.forEach { put(it.zuJson()) } })
        }
        is TypInferenzRegel.TupelAusEingängen -> {
            put("art", "tupelAusEingängen")
            put("eingänge", JSONArray(regel.eingänge))
            put("konstruktor", regel.konstruktor.wert)
        }
        is TypInferenzRegel.KomponenteAusEingang -> {
            put("art", "komponenteAusEingang")
            put("eingang", regel.eingang)
            put("index", regel.index)
            regel.konstruktor?.let { put("konstruktor", it.wert) }
        }
    }
}

internal fun typInferenzVonJson(json: JSONObject?): TypInferenzRegel? {
    if (json == null) return null
    return when (json.optString("art")) {
        "folgtEingang" -> json.optString("eingang").takeIf(String::isNotBlank)?.let(TypInferenzRegel::FolgtEingang)
        "gemeinsameOberart" -> stringListe(json.optJSONArray("eingänge")).takeIf { it.isNotEmpty() }?.let(TypInferenzRegel::GemeinsameOberart)
        "vereinigungAusEingängen" -> stringListe(json.optJSONArray("eingänge")).takeIf { it.isNotEmpty() }?.let(TypInferenzRegel::VereinigungAusEingängen)
        "abbildungVonEingang" -> {
            val eingang = json.optString("eingang").takeIf(String::isNotBlank) ?: return null
            val array = json.optJSONArray("fälle") ?: return null
            val fälle = buildList {
                for (index in 0 until array.length()) {
                    val fall = array.optJSONObject(index) ?: continue
                    val von = typAusdruckVonJson(fall.optJSONObject("von")) ?: continue
                    val zu = typAusdruckVonJson(fall.optJSONObject("zu")) ?: continue
                    add(TypAbbildungsFall(von, zu))
                }
            }
            fälle.takeIf { it.isNotEmpty() }?.let { TypInferenzRegel.AbbildungVonEingang(eingang, it) }
        }
        "priorisierung" -> {
            val eingänge = stringListe(json.optJSONArray("eingänge"))
            val prioritäten = typListeVonJson(json.optJSONArray("prioritäten"))
            if (eingänge.isNotEmpty() && prioritäten.isNotEmpty()) TypInferenzRegel.Priorisierung(eingänge, prioritäten) else null
        }
        "tupelAusEingängen" -> {
            val eingänge = stringListe(json.optJSONArray("eingänge"))
            val konstruktor = json.optString("konstruktor", "mathematik.tupel").takeIf(String::isNotBlank) ?: "mathematik.tupel"
            eingänge.takeIf { it.isNotEmpty() }?.let { TypInferenzRegel.TupelAusEingängen(it, TypId(konstruktor)) }
        }
        "komponenteAusEingang" -> {
            val eingang = json.optString("eingang").takeIf(String::isNotBlank) ?: return null
            val index = json.optInt("index", -1)
            if (index < 0) return null
            val konstruktor = json.optString("konstruktor").takeIf(String::isNotBlank)?.let(::TypId)
            TypInferenzRegel.KomponenteAusEingang(eingang, index, konstruktor)
        }
        else -> null
    }
}

private fun stringListe(array: JSONArray?): List<String> =
    if (array == null) emptyList() else List(array.length()) { index -> array.optString(index) }.filter(String::isNotBlank)

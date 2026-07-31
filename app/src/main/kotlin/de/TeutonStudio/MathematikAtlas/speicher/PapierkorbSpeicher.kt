package de.TeutonStudio.MathematikAtlas.speicher

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class PapierkorbArt { Karte, Ordner }

data class PapierkorbEintrag(
    val id: String = UUID.randomUUID().toString(),
    val art: PapierkorbArt,
    val name: String,
    val ursprünglicherPfad: List<String>,
    val kartenPfade: Map<KartenId, List<String>>,
    val ordnerPfade: Set<List<String>> = emptySet(),
    val gelöschtAm: Long = System.currentTimeMillis(),
) {
    val kartenIds: Set<KartenId> get() = kartenPfade.keys
}

class PapierkorbSpeicher(context: Context) {
    private val datei = File(File(context.filesDir, "MathematikAtlas"), "papierkorb.json")

    fun liste(): List<PapierkorbEintrag> = lade().sortedByDescending(PapierkorbEintrag::gelöschtAm)

    fun kartenIds(): Set<KartenId> = lade().flatMapTo(mutableSetOf()) { it.kartenIds }

    fun legeAb(eintrag: PapierkorbEintrag) {
        val bestehend = lade().filterNot { alt ->
            alt.id == eintrag.id || alt.kartenIds.any { it in eintrag.kartenIds }
        }
        speichere(bestehend + eintrag)
    }

    fun entferne(eintragId: String) {
        speichere(lade().filterNot { it.id == eintragId })
    }

    fun entferneKarten(kartenIds: Set<KartenId>) {
        if (kartenIds.isEmpty()) return
        speichere(lade().mapNotNull { eintrag ->
            val verbleibend = eintrag.kartenPfade.filterKeys { it !in kartenIds }
            when {
                verbleibend.isNotEmpty() -> eintrag.copy(kartenPfade = verbleibend)
                eintrag.art == PapierkorbArt.Ordner && eintrag.ordnerPfade.isNotEmpty() -> eintrag.copy(kartenPfade = emptyMap())
                else -> null
            }
        })
    }

    private fun lade(): List<PapierkorbEintrag> = runCatching {
        if (!datei.exists()) return@runCatching emptyList()
        val json = JSONObject(datei.readText())
        val einträge = json.optJSONArray("einträge") ?: JSONArray()
        List(einträge.length()) { index -> eintragVonJson(einträge.getJSONObject(index)) }
    }.getOrDefault(emptyList())

    private fun speichere(einträge: List<PapierkorbEintrag>) {
        datei.parentFile?.mkdirs()
        val temporär = File(datei.parentFile, "${datei.name}.tmp")
        temporär.writeText(JSONObject().apply {
            put("formatVersion", 1)
            put("einträge", JSONArray().apply { einträge.forEach { put(it.zuJson()) } })
        }.toString(2))
        if (!temporär.renameTo(datei)) {
            temporär.copyTo(datei, overwrite = true)
            temporär.delete()
        }
    }

    private fun PapierkorbEintrag.zuJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("art", art.name)
        put("name", name)
        put("ursprünglicherPfad", ursprünglicherPfad.zuJson())
        put("gelöschtAm", gelöschtAm)
        put("kartenPfade", JSONObject().apply {
            kartenPfade.forEach { (kartenId, pfad) -> put(kartenId.wert, pfad.zuJson()) }
        })
        put("ordnerPfade", JSONArray().apply { ordnerPfade.forEach { put(it.zuJson()) } })
    }

    private fun eintragVonJson(json: JSONObject): PapierkorbEintrag {
        val kartenJson = json.optJSONObject("kartenPfade") ?: JSONObject()
        val kartenPfade = kartenJson.keys().asSequence().associate { id ->
            KartenId(id) to kartenJson.optJSONArray(id).zuPfad()
        }
        val ordnerJson = json.optJSONArray("ordnerPfade") ?: JSONArray()
        return PapierkorbEintrag(
            id = json.optString("id").takeIf(String::isNotBlank) ?: UUID.randomUUID().toString(),
            art = runCatching { PapierkorbArt.valueOf(json.optString("art")) }.getOrDefault(PapierkorbArt.Karte),
            name = json.optString("name", "Gelöschtes Element"),
            ursprünglicherPfad = json.optJSONArray("ursprünglicherPfad").zuPfad(),
            kartenPfade = kartenPfade,
            ordnerPfade = List(ordnerJson.length()) { ordnerJson.optJSONArray(it).zuPfad() }.filter(List<String>::isNotEmpty).toSet(),
            gelöschtAm = json.optLong("gelöschtAm", System.currentTimeMillis()),
        )
    }
}

private fun List<String>.zuJson(): JSONArray = JSONArray().apply { this@zuJson.forEach(::put) }
private fun JSONArray?.zuPfad(): List<String> = if (this == null) emptyList() else List(length()) { optString(it) }.map(String::trim).filter(String::isNotEmpty)

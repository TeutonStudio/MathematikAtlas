package de.TeutonStudio.MathematikAtlas.speicher

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class KartenOrdnung(
    val ordner: Set<List<String>> = emptySet(),
    val kartenOrdner: Map<KartenId, List<String>> = emptyMap(),
) {
    fun ordnerFür(kartenId: KartenId): List<String> = kartenOrdner[kartenId].orEmpty()

    fun kartenUnter(pfad: List<String>): Set<KartenId> {
        val normalisiert = normalisierePfad(pfad)
        return kartenOrdner.filterValues { it.beginntMit(normalisiert) }.keys
    }

    fun ordnerUnter(pfad: List<String>): Set<List<String>> {
        val normalisiert = normalisierePfad(pfad)
        return ordner.filter { it.beginntMit(normalisiert) }.toSet()
    }

    fun mitOrdner(pfad: List<String>): KartenOrdnung {
        val normalisiert = normalisierePfad(pfad)
        if (normalisiert.isEmpty()) return this
        return copy(ordner = ordner + normalisiert.präfixe()).normalisiert()
    }

    fun mitOrdnern(pfade: Iterable<List<String>>): KartenOrdnung =
        pfade.fold(this) { aktuell, pfad -> aktuell.mitOrdner(pfad) }

    fun mitKarteInOrdner(kartenId: KartenId, pfad: List<String>): KartenOrdnung {
        val normalisiert = normalisierePfad(pfad)
        val zuordnung = if (normalisiert.isEmpty()) kartenOrdner - kartenId else kartenOrdner + (kartenId to normalisiert)
        return copy(ordner = ordner + normalisiert.präfixe(), kartenOrdner = zuordnung).normalisiert()
    }

    fun mitKartenInOrdnern(zuordnungen: Map<KartenId, List<String>>): KartenOrdnung =
        zuordnungen.entries.fold(this) { aktuell, (id, pfad) -> aktuell.mitKarteInOrdner(id, pfad) }

    fun verschiebeOrdner(von: List<String>, nach: List<String>): KartenOrdnung {
        val quelle = normalisierePfad(von)
        val ziel = normalisierePfad(nach)
        require(quelle.isNotEmpty()) { "Der Stammordner kann nicht verschoben werden." }
        require(ziel.isNotEmpty()) { "Ein Ordner benötigt einen Namen." }
        if (quelle == ziel) return this
        require(!ziel.beginntMit(quelle)) { "Ein Ordner kann nicht in sich selbst verschoben werden." }

        fun ersetze(pfad: List<String>): List<String> =
            if (pfad.beginntMit(quelle)) ziel + pfad.drop(quelle.size) else pfad

        return KartenOrdnung(
            ordner = ordner.map(::ersetze).toSet() + ziel.präfixe(),
            kartenOrdner = kartenOrdner.mapValues { (_, pfad) -> ersetze(pfad) },
        ).normalisiert()
    }

    fun kannOrdnerLöschen(pfad: List<String>): Boolean {
        val normalisiert = normalisierePfad(pfad)
        if (normalisiert.isEmpty()) return false
        val hatUnterordner = ordner.any { it.size > normalisiert.size && it.beginntMit(normalisiert) }
        val enthältKarten = kartenOrdner.values.any { it.beginntMit(normalisiert) }
        return !hatUnterordner && !enthältKarten
    }

    fun ohneOrdner(pfad: List<String>): KartenOrdnung {
        val normalisiert = normalisierePfad(pfad)
        if (!kannOrdnerLöschen(normalisiert)) return this
        return copy(ordner = ordner.filterNot { it == normalisiert }.toSet()).normalisiert()
    }

    fun ohneKarten(kartenIds: Set<KartenId>): KartenOrdnung =
        copy(kartenOrdner = kartenOrdner.filterKeys { it !in kartenIds }).normalisiert()

    fun ohneOrdnerBaum(pfad: List<String>): KartenOrdnung {
        val normalisiert = normalisierePfad(pfad)
        if (normalisiert.isEmpty()) return this
        val kartenIds = kartenUnter(normalisiert)
        return KartenOrdnung(
            ordner = ordner.filterNot { it.beginntMit(normalisiert) }.toSet(),
            kartenOrdner = kartenOrdner.filterKeys { it !in kartenIds },
        ).normalisiert()
    }

    fun normalisiert(): KartenOrdnung {
        val saubereZuordnung = kartenOrdner.mapValues { (_, pfad) -> normalisierePfad(pfad) }.filterValues { it.isNotEmpty() }
        val saubereOrdner = (ordner.map(::normalisierePfad).filter { it.isNotEmpty() } +
            saubereZuordnung.values).flatMap { it.präfixe() }.toSet()
        return KartenOrdnung(saubereOrdner, saubereZuordnung)
    }
}

class KartenOrdnungSpeicher(context: Context) {
    private val datei = File(File(context.filesDir, "MathematikAtlas"), "karten-ordnung.json")

    fun lade(): KartenOrdnung = runCatching {
        if (!datei.exists()) return@runCatching KartenOrdnung()
        val json = JSONObject(datei.readText())
        val ordner = json.optJSONArray("ordner").zuPfade().toSet()
        val kartenJson = json.optJSONObject("karten") ?: JSONObject()
        val karten = kartenJson.keys().asSequence().associate { id ->
            KartenId(id) to (kartenJson.optJSONArray(id)?.zuPfad().orEmpty())
        }
        KartenOrdnung(ordner, karten).normalisiert()
    }.getOrDefault(KartenOrdnung())

    fun speichere(ordnung: KartenOrdnung) {
        val normalisiert = ordnung.normalisiert()
        val json = JSONObject().apply {
            put("formatVersion", 1)
            put("ordner", JSONArray().apply {
                normalisiert.ordner.sortedMitPfad().forEach { put(it.zuJson()) }
            })
            put("karten", JSONObject().apply {
                normalisiert.kartenOrdner.entries.sortedBy { it.key.wert }.forEach { (id, pfad) -> put(id.wert, pfad.zuJson()) }
            })
        }
        datei.parentFile?.mkdirs()
        val temporär = File(datei.parentFile, "${datei.name}.tmp")
        temporär.writeText(json.toString(2))
        if (!temporär.renameTo(datei)) {
            temporär.copyTo(datei, overwrite = true)
            temporär.delete()
        }
    }
}

fun parseOrdnerPfad(text: String): List<String> = normalisierePfad(text.split('/', '\\'))
fun formatiereOrdnerPfad(pfad: List<String>): String = normalisierePfad(pfad).joinToString("/")

private fun normalisierePfad(pfad: List<String>): List<String> = pfad.map(String::trim).filter(String::isNotEmpty)
private fun List<String>.präfixe(): List<List<String>> = indices.map { take(it + 1) }
private fun List<String>.beginntMit(präfix: List<String>): Boolean = size >= präfix.size && take(präfix.size) == präfix
private fun List<String>.zuJson(): JSONArray = JSONArray().apply { this@zuJson.forEach { put(it) } }
private fun JSONArray.zuPfad(): List<String> = List(length()) { index -> optString(index) }.let(::normalisierePfad)
private fun JSONArray?.zuPfade(): List<List<String>> = if (this == null) emptyList() else List(length()) { optJSONArray(it)?.zuPfad().orEmpty() }
private fun Iterable<List<String>>.sortedMitPfad(): List<List<String>> = sortedWith(compareBy({ it.size }, { it.joinToString("\u0000") }))

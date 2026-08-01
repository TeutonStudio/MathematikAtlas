package de.TeutonStudio.MathematikAtlas.speicher

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.UUID

enum class FreigabeArt { Karte, Sammlung }

data class FreigabeQuelle(
    val herausgeberId: ProfilId,
    val herausgeberPseudonym: String,
    val ressourcenId: String,
    val freigabeVersion: Long,
    val ressourcenName: String,
    val art: FreigabeArt,
)

data class GelesenesFreigabePaket(
    val quelle: FreigabeQuelle,
    val wurzeln: List<KartenVerweis>,
    val karten: List<KartenDaten>,
    val kartenPfade: Map<KartenId, List<String>>,
    val ordnerPfade: Set<List<String>>,
)

data class ImportierteFreigabe(
    val lokaleKartenId: KartenId,
    val ursprünglicheKartenId: KartenId,
    val ursprünglicheVersion: Int,
    val quelle: FreigabeQuelle,
)

object KartenFreigabePaket {
    private const val FORMAT_VERSION = 1
    private const val MAXIMALE_KARTEN = 1_000
    private const val MAXIMALE_TEXTLÄNGE = 10_000_000

    fun istFreigabePaket(text: String): Boolean = runCatching {
        JSONObject(text).optInt("paketFormatVersion", 0) > 0
    }.getOrDefault(false)

    fun erstelle(
        name: String,
        art: FreigabeArt,
        wurzelKarten: List<KartenDaten>,
        ordnung: KartenOrdnung,
        sammlungsPfad: List<String>? = null,
        profil: LokalesProfil,
        lade: (KartenVerweis) -> KartenDaten?,
    ): String {
        require(wurzelKarten.isNotEmpty()) { "Eine Freigabe benötigt mindestens eine Karte." }
        val wurzeln = wurzelKarten.map { KartenVerweis(it.id, it.version) }
        val karten = abhängigkeiten(wurzeln, lade)
        val wurzelIds = wurzelKarten.mapTo(mutableSetOf()) { it.id }
        val basisPfad = sammlungsPfad.orEmpty()
        val abhängigkeitenPfad = listOf("$name – Abhängigkeiten")
        val kartenPfade = karten.values.associate { karte ->
            val pfad = if (karte.id in wurzelIds) {
                ordnung.ordnerFür(karte.id).drop(basisPfad.size)
            } else {
                abhängigkeitenPfad
            }
            karte.id to pfad
        }
        val ordnerPfade = buildSet {
            if (art == FreigabeArt.Sammlung) {
                ordnung.ordnerUnter(basisPfad).forEach { pfad ->
                    val relativ = pfad.drop(basisPfad.size)
                    if (relativ.isNotEmpty()) add(relativ)
                }
            }
            if (karten.values.any { it.id !in wurzelIds }) add(abhängigkeitenPfad)
            kartenPfade.values.filter(List<String>::isNotEmpty).forEach { pfad ->
                pfad.indices.forEach { index -> add(pfad.take(index + 1)) }
            }
        }
        val schlüssel = when (art) {
            FreigabeArt.Karte -> "karte:${wurzelKarten.single().id.wert}"
            FreigabeArt.Sammlung -> "sammlung:${formatiereOrdnerPfad(basisPfad)}"
        }
        val ressourcenId = UUID.nameUUIDFromBytes(
            "${profil.id.wert}|$schlüssel".toByteArray(StandardCharsets.UTF_8),
        ).toString()
        val quelle = FreigabeQuelle(
            herausgeberId = profil.id,
            herausgeberPseudonym = profil.pseudonym,
            ressourcenId = ressourcenId,
            freigabeVersion = System.currentTimeMillis(),
            ressourcenName = name,
            art = art,
        )

        return JSONObject().apply {
            put("paketFormatVersion", FORMAT_VERSION)
            put("quelle", quelle.zuJson())
            put("wurzeln", JSONArray().apply { wurzeln.forEach { put(it.zuJson()) } })
            put("karten", JSONArray().apply {
                karten.entries.sortedWith(compareBy({ it.key.kartenId.wert }, { it.key.version }))
                    .forEach { (_, karte) -> put(JSONObject(KartenJson.schreibe(karte))) }
            })
            put("kartenPfade", JSONObject().apply {
                kartenPfade.entries.sortedBy { it.key.wert }.forEach { (id, pfad) -> put(id.wert, pfad.zuJson()) }
            })
            put("ordnerPfade", JSONArray().apply {
                ordnerPfade.sortedWith(compareBy({ it.size }, { it.joinToString("\u0000") })).forEach { put(it.zuJson()) }
            })
        }.toString(2)
    }

    fun lese(text: String): GelesenesFreigabePaket {
        require(text.length <= MAXIMALE_TEXTLÄNGE) { "Das Freigabepaket ist zu groß." }
        val json = JSONObject(text)
        require(json.optInt("paketFormatVersion", 0) == FORMAT_VERSION) {
            "Diese Freigabepaket-Version wird nicht unterstützt."
        }
        val quelleJson = json.getJSONObject("quelle")
        val quelle = FreigabeQuelle(
            herausgeberId = ProfilId(quelleJson.getString("herausgeberId")),
            herausgeberPseudonym = quelleJson.optString("herausgeberPseudonym", "Unbekannt"),
            ressourcenId = quelleJson.getString("ressourcenId"),
            freigabeVersion = quelleJson.getLong("freigabeVersion"),
            ressourcenName = quelleJson.optString("ressourcenName", "Freigabe"),
            art = FreigabeArt.valueOf(quelleJson.optString("art", FreigabeArt.Karte.name)),
        )
        val kartenJson = json.getJSONArray("karten")
        require(kartenJson.length() in 1..MAXIMALE_KARTEN) { "Das Freigabepaket enthält eine ungültige Kartenanzahl." }
        val karten = List(kartenJson.length()) { index -> KartenJson.lese(kartenJson.getJSONObject(index).toString()) }
        val vorhandeneVerweise = karten.mapTo(mutableSetOf()) { KartenVerweis(it.id, it.version) }
        val wurzelJson = json.getJSONArray("wurzeln")
        val wurzeln = List(wurzelJson.length()) { index -> wurzelJson.getJSONObject(index).zuVerweis() }
        require(wurzeln.isNotEmpty() && wurzeln.all { it in vorhandeneVerweise }) {
            "Das Freigabepaket besitzt keine gültige Wurzelkarte."
        }
        karten.forEach { karte ->
            karte.knoten.flatMap(KnotenDaten::alleKartenVerweise).forEach { verweis ->
                require(verweis in vorhandeneVerweise) {
                    "Die Abhängigkeit ${verweis.kartenId} v${verweis.version} fehlt im Freigabepaket."
                }
            }
        }
        val pfadeJson = json.optJSONObject("kartenPfade") ?: JSONObject()
        val kartenPfade = pfadeJson.keys().asSequence().associate { id ->
            KartenId(id) to pfadeJson.optJSONArray(id).zuPfad()
        }
        val ordnerJson = json.optJSONArray("ordnerPfade") ?: JSONArray()
        val ordnerPfade = List(ordnerJson.length()) { index -> ordnerJson.optJSONArray(index).zuPfad() }
            .filter(List<String>::isNotEmpty)
            .toSet()
        return GelesenesFreigabePaket(quelle, wurzeln, karten, kartenPfade, ordnerPfade)
    }

    private fun abhängigkeiten(
        wurzeln: List<KartenVerweis>,
        lade: (KartenVerweis) -> KartenDaten?,
    ): Map<KartenVerweis, KartenDaten> {
        val offen = ArrayDeque(wurzeln)
        val ergebnis = linkedMapOf<KartenVerweis, KartenDaten>()
        while (offen.isNotEmpty()) {
            val verweis = offen.removeFirst()
            if (verweis in ergebnis) continue
            val karte = requireNotNull(lade(verweis)) {
                "Die benötigte Karte ${verweis.kartenId} v${verweis.version} wurde nicht gefunden."
            }
            ergebnis[verweis] = karte
            karte.knoten.flatMap(KnotenDaten::alleKartenVerweise).forEach(offen::addLast)
        }
        return ergebnis
    }

    private fun FreigabeQuelle.zuJson(): JSONObject = JSONObject().apply {
        put("herausgeberId", herausgeberId.wert)
        put("herausgeberPseudonym", herausgeberPseudonym)
        put("ressourcenId", ressourcenId)
        put("freigabeVersion", freigabeVersion)
        put("ressourcenName", ressourcenName)
        put("art", art.name)
    }

    private fun KartenVerweis.zuJson(): JSONObject = JSONObject().apply {
        put("kartenId", kartenId.wert)
        put("version", version)
    }

    private fun JSONObject.zuVerweis() = KartenVerweis(KartenId(getString("kartenId")), getInt("version"))
}

class FreigabeQuellenSpeicher(context: Context) {
    private val datei = File(File(context.filesDir, "MathematikAtlas"), "freigabe-quellen.json")

    fun liste(): List<ImportierteFreigabe> = runCatching {
        if (!datei.exists()) return@runCatching emptyList()
        val json = JSONObject(datei.readText()).optJSONArray("karten") ?: JSONArray()
        List(json.length()) { index -> json.getJSONObject(index).zuImportierterFreigabe() }
    }.getOrDefault(emptyList())

    fun speichere(neueEinträge: List<ImportierteFreigabe>) {
        val zusammengeführt = (liste().filterNot { alt -> neueEinträge.any { it.lokaleKartenId == alt.lokaleKartenId } } + neueEinträge)
            .sortedBy { it.lokaleKartenId.wert }
        datei.parentFile?.mkdirs()
        val temporär = File(datei.parentFile, "${datei.name}.tmp")
        temporär.writeText(JSONObject().apply {
            put("formatVersion", 1)
            put("karten", JSONArray().apply { zusammengeführt.forEach { put(it.zuJson()) } })
        }.toString(2))
        if (!temporär.renameTo(datei)) {
            temporär.copyTo(datei, overwrite = true)
            temporär.delete()
        }
    }

    fun entferne(kartenIds: Set<KartenId>) {
        if (kartenIds.isEmpty()) return
        val verbleibend = liste().filterNot { it.lokaleKartenId in kartenIds }
        datei.parentFile?.mkdirs()
        datei.writeText(JSONObject().apply {
            put("formatVersion", 1)
            put("karten", JSONArray().apply { verbleibend.forEach { put(it.zuJson()) } })
        }.toString(2))
    }

    private fun ImportierteFreigabe.zuJson(): JSONObject = JSONObject().apply {
        put("lokaleKartenId", lokaleKartenId.wert)
        put("ursprünglicheKartenId", ursprünglicheKartenId.wert)
        put("ursprünglicheVersion", ursprünglicheVersion)
        put("quelle", JSONObject().apply {
            put("herausgeberId", quelle.herausgeberId.wert)
            put("herausgeberPseudonym", quelle.herausgeberPseudonym)
            put("ressourcenId", quelle.ressourcenId)
            put("freigabeVersion", quelle.freigabeVersion)
            put("ressourcenName", quelle.ressourcenName)
            put("art", quelle.art.name)
        })
    }

    private fun JSONObject.zuImportierterFreigabe(): ImportierteFreigabe {
        val q = getJSONObject("quelle")
        return ImportierteFreigabe(
            lokaleKartenId = KartenId(getString("lokaleKartenId")),
            ursprünglicheKartenId = KartenId(getString("ursprünglicheKartenId")),
            ursprünglicheVersion = getInt("ursprünglicheVersion"),
            quelle = FreigabeQuelle(
                herausgeberId = ProfilId(q.getString("herausgeberId")),
                herausgeberPseudonym = q.optString("herausgeberPseudonym", "Unbekannt"),
                ressourcenId = q.getString("ressourcenId"),
                freigabeVersion = q.getLong("freigabeVersion"),
                ressourcenName = q.optString("ressourcenName", "Freigabe"),
                art = FreigabeArt.valueOf(q.optString("art", FreigabeArt.Karte.name)),
            ),
        )
    }
}

fun sichererFreigabeDateiname(name: String): String = name.trim()
    .replace(Regex("[^\\p{L}\\p{N}._ -]+"), "-")
    .replace(Regex("\\s+"), " ")
    .trim(' ', '.', '-')
    .ifBlank { "Mathematik-Atlas" }
    .take(80) + ".matlas"

private fun List<String>.zuJson(): JSONArray = JSONArray().apply { this@zuJson.forEach(::put) }
private fun JSONArray?.zuPfad(): List<String> = if (this == null) emptyList() else List(length()) { optString(it) }.map(String::trim).filter(String::isNotEmpty)

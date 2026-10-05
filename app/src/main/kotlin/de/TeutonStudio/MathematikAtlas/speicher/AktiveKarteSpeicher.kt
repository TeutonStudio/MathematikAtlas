package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import org.json.JSONObject
import java.io.File

/**
 * Flüchtige App-Navigation, bewusst getrennt von versionierten Karteninhalten.
 * Eine defekte oder veraltete Referenz ist kein Persistenzfehler der Karte.
 */
class AktiveKarteSpeicher(private val basis: File) {
    private val datei = File(basis, DATEINAME)

    fun lade(): KartenId? = runCatching {
        if (!datei.exists()) return@runCatching null
        JSONObject(datei.readText()).optString("kartenId").trim().takeIf(String::isNotBlank)?.let(::KartenId)
    }.getOrNull()

    fun speichere(id: KartenId) {
        datei.parentFile?.mkdirs()
        val temporär = File(datei.parentFile, "${datei.name}.tmp")
        temporär.writeText(JSONObject().apply {
            put("formatVersion", FORMAT_VERSION)
            put("kartenId", id.wert)
        }.toString())
        if (!temporär.renameTo(datei)) {
            temporär.copyTo(datei, overwrite = true)
            temporär.delete()
        }
    }

    fun löschen() {
        if (datei.exists()) datei.delete()
    }

    companion object {
        private const val DATEINAME = "aktive-karte.json"
        private const val FORMAT_VERSION = 1
    }
}

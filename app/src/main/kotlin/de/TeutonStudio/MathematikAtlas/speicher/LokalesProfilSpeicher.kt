package de.TeutonStudio.MathematikAtlas.speicher

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.util.UUID

@JvmInline
value class ProfilId(val wert: String)

data class LokalesProfil(
    val id: ProfilId,
    val pseudonym: String,
    val lieblingsFarbe: ProfilFarbe = ProfilFarbe.Standard,
)

class LokalesProfilSpeicher(context: Context) {
    private val datei = File(File(context.filesDir, "MathematikAtlas"), "profil.json")

    fun lade(): LokalesProfil {
        val profil = runCatching {
            if (!datei.exists()) return@runCatching neuesProfil()
            val json = JSONObject(datei.readText())
            LokalesProfil(
                id = ProfilId(json.optString("id").takeIf(String::isNotBlank) ?: UUID.randomUUID().toString()),
                pseudonym = normalisierePseudonym(json.optString("pseudonym")),
                lieblingsFarbe = ProfilFarbe.parse(json.optString("lieblingsFarbe")) ?: ProfilFarbe.Standard,
            )
        }.getOrElse { neuesProfil() }
        veröffentliche(profil)
        return profil
    }

    fun speichere(profil: LokalesProfil): LokalesProfil {
        val normalisiert = profil.copy(
            pseudonym = normalisierePseudonym(profil.pseudonym),
            lieblingsFarbe = ProfilFarbe.parse(profil.lieblingsFarbe.rgbHex) ?: ProfilFarbe.Standard,
        )
        datei.parentFile?.mkdirs()
        val temporär = File(datei.parentFile, "${datei.name}.tmp")
        temporär.writeText(JSONObject().apply {
            put("formatVersion", AKTUELLE_FORMAT_VERSION)
            put("id", normalisiert.id.wert)
            put("pseudonym", normalisiert.pseudonym)
            put("lieblingsFarbe", normalisiert.lieblingsFarbe.rgbHex)
        }.toString(2))
        if (!temporär.renameTo(datei)) {
            temporär.copyTo(datei, overwrite = true)
            temporär.delete()
        }
        veröffentliche(normalisiert)
        return normalisiert
    }

    private fun neuesProfil(): LokalesProfil = speichere(
        LokalesProfil(
            id = ProfilId(UUID.randomUUID().toString()),
            pseudonym = "Mathematikfreund",
            lieblingsFarbe = ProfilFarbe.Standard,
        ),
    )

    private fun normalisierePseudonym(text: String): String =
        text.trim().replace(Regex("\\s+"), " ").take(40).ifBlank { "Mathematikfreund" }

    private fun veröffentliche(profil: LokalesProfil) {
        _profilAenderungen.value = profil
    }

    companion object {
        private const val AKTUELLE_FORMAT_VERSION = 2
        private val _profilAenderungen = MutableStateFlow<LokalesProfil?>(null)

        /** Gemeinsame beobachtbare Profilquelle der laufenden App-Sitzung. */
        val profilAenderungen: StateFlow<LokalesProfil?> = _profilAenderungen.asStateFlow()
    }
}

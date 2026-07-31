package de.TeutonStudio.MathematikAtlas.speicher

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

@JvmInline
value class ProfilId(val wert: String)

data class LokalesProfil(
    val id: ProfilId,
    val pseudonym: String,
)

class LokalesProfilSpeicher(context: Context) {
    private val datei = File(File(context.filesDir, "MathematikAtlas"), "profil.json")

    fun lade(): LokalesProfil = runCatching {
        if (!datei.exists()) return@runCatching neuesProfil()
        val json = JSONObject(datei.readText())
        LokalesProfil(
            id = ProfilId(json.optString("id").takeIf(String::isNotBlank) ?: UUID.randomUUID().toString()),
            pseudonym = normalisierePseudonym(json.optString("pseudonym")),
        )
    }.getOrElse { neuesProfil() }

    fun speichere(profil: LokalesProfil): LokalesProfil {
        val normalisiert = profil.copy(pseudonym = normalisierePseudonym(profil.pseudonym))
        datei.parentFile?.mkdirs()
        val temporär = File(datei.parentFile, "${datei.name}.tmp")
        temporär.writeText(JSONObject().apply {
            put("formatVersion", 1)
            put("id", normalisiert.id.wert)
            put("pseudonym", normalisiert.pseudonym)
        }.toString(2))
        if (!temporär.renameTo(datei)) {
            temporär.copyTo(datei, overwrite = true)
            temporär.delete()
        }
        return normalisiert
    }

    private fun neuesProfil(): LokalesProfil = speichere(
        LokalesProfil(
            id = ProfilId(UUID.randomUUID().toString()),
            pseudonym = "Mathematikfreund",
        ),
    )

    private fun normalisierePseudonym(text: String): String =
        text.trim().replace(Regex("\\s+"), " ").take(40).ifBlank { "Mathematikfreund" }
}

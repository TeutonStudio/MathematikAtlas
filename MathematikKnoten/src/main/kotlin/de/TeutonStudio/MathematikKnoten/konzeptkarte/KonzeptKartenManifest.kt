package de.TeutonStudio.MathematikKnoten.konzeptkarte

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import org.json.JSONObject
import java.security.MessageDigest

@JvmInline
value class KonzeptKartenId(val wert: String) {
    init { require(wert.isNotBlank()) }
    override fun toString(): String = wert
}

data class KonzeptKartenManifestEintrag(
    val id: KonzeptKartenId,
    val datei: String,
    val formatVersion: Int = KartenDatenJson.FORMAT_VERSION,
    val sha256: String? = null,
) {
    init {
        require(datei.endsWith(".json"))
        require(formatVersion > 0)
        require(sha256 == null || sha256.matches(Regex("[0-9a-f]{64}")))
    }
}

data class KonzeptKartenManifest(
    val einträge: List<KonzeptKartenManifestEintrag>,
) {
    init {
        require(einträge.map { it.id }.distinct().size == einträge.size) { "Doppelte Konzeptkarten-ID im Manifest." }
        require(einträge.map { it.datei }.distinct().size == einträge.size) { "Doppelte Konzeptkarten-Datei im Manifest." }
    }

    private val nachId = einträge.associateBy(KonzeptKartenManifestEintrag::id)
    fun finde(id: KonzeptKartenId): KonzeptKartenManifestEintrag? = nachId[id]
}

object KonzeptKartenManifestJson {
    const val MANIFEST_VERSION = 1

    fun lese(text: String): KonzeptKartenManifest {
        val json = JSONObject(text)
        require(json.getInt("manifestVersion") == MANIFEST_VERSION) {
            "Unbekannte Konzeptkarten-Manifestversion ${json.optInt("manifestVersion", -1)}."
        }
        val karten = json.getJSONArray("karten")
        return KonzeptKartenManifest(
            List(karten.length()) { index ->
                val eintrag = karten.getJSONObject(index)
                KonzeptKartenManifestEintrag(
                    id = KonzeptKartenId(eintrag.getString("id")),
                    datei = eintrag.getString("datei"),
                    formatVersion = eintrag.getInt("formatVersion"),
                    sha256 = eintrag.optString("sha256").takeIf(String::isNotBlank),
                )
            },
        )
    }
}

fun interface KonzeptKartenQuelle {
    fun lese(pfad: String): String?
}

sealed interface KonzeptKartenLadeErgebnis {
    data class Erfolg(val karte: KartenDaten) : KonzeptKartenLadeErgebnis
    data class Fehler(val code: String, val nachricht: String) : KonzeptKartenLadeErgebnis
}

class KonzeptKartenLader(
    private val quelle: KonzeptKartenQuelle,
    private val manifest: KonzeptKartenManifest,
) {
    fun lade(id: KonzeptKartenId): KonzeptKartenLadeErgebnis {
        val eintrag = manifest.finde(id)
            ?: return KonzeptKartenLadeErgebnis.Fehler("unbekannte_karte", "Keine Konzeptkarte mit ID $id registriert.")
        val pfad = "$ASSET_BASISPFAD/${eintrag.datei}"
        val text = quelle.lese(pfad)
            ?: return KonzeptKartenLadeErgebnis.Fehler("asset_fehlt", "Das Konzeptkarten-Asset $pfad fehlt.")
        if (eintrag.sha256 != null && text.sha256() != eintrag.sha256) {
            return KonzeptKartenLadeErgebnis.Fehler("pruefsumme", "Die Prüfsumme von $pfad stimmt nicht mit dem Manifest überein.")
        }
        val formatVersion = runCatching { KartenDatenJson.formatVersion(text) }.getOrElse {
            return KonzeptKartenLadeErgebnis.Fehler("ungueltiges_json", "Die Formatversion von $pfad kann nicht gelesen werden: ${it.message}")
        }
        if (formatVersion != eintrag.formatVersion) {
            return KonzeptKartenLadeErgebnis.Fehler(
                "formatversion",
                "$pfad besitzt Formatversion $formatVersion, erwartet wird ${eintrag.formatVersion}.",
            )
        }
        val karte = runCatching { KartenDatenJson.lese(text) }.getOrElse {
            return KonzeptKartenLadeErgebnis.Fehler("ungueltige_karte", "$pfad ist keine gültige Karte: ${it.message}")
        }
        if (karte.id.wert != id.wert) {
            return KonzeptKartenLadeErgebnis.Fehler(
                "karten_id",
                "$pfad enthält Karten-ID ${karte.id}; erwartet wird $id.",
            )
        }
        return KonzeptKartenLadeErgebnis.Erfolg(karte)
    }

    fun validierungsFehler(): List<String> = manifest.einträge.mapNotNull { eintrag ->
        when (val ergebnis = lade(eintrag.id)) {
            is KonzeptKartenLadeErgebnis.Erfolg -> null
            is KonzeptKartenLadeErgebnis.Fehler -> "${eintrag.id}: ${ergebnis.nachricht}"
        }
    }
}

fun KonzeptKartenQuelle.ladeManifest(): Result<KonzeptKartenManifest> = runCatching {
    val text = requireNotNull(lese(MANIFEST_ASSET_PFAD)) {
        "Das Konzeptkarten-Manifest $MANIFEST_ASSET_PFAD fehlt."
    }
    KonzeptKartenManifestJson.lese(text)
}

const val ASSET_BASISPFAD = "de/TeutonStudio/MathematikKnoten/konzeptkarte"
const val MANIFEST_ASSET_PFAD = "$ASSET_BASISPFAD/index.json"

private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray(Charsets.UTF_8))
    .joinToString("") { byte -> "%02x".format(byte) }

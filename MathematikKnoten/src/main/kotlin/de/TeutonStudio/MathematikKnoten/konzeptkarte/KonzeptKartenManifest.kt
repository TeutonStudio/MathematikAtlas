package de.TeutonStudio.MathematikKnoten.konzeptkarte

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
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

fun interface KonzeptKartenQuelle {
    fun lese(pfad: String): String?
}

sealed interface KonzeptKartenLadeErgebnis {
    data class Erfolg(val karte: KartenDaten) : KonzeptKartenLadeErgebnis
    data class Fehler(val code: String, val nachricht: String) : KonzeptKartenLadeErgebnis
}

class KonzeptKartenLader(
    private val quelle: KonzeptKartenQuelle,
    private val manifest: KonzeptKartenManifest = StandardKonzeptKartenManifest.manifest,
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

object StandardKonzeptKartenManifest {
    val manifest = KonzeptKartenManifest(
        listOf(
            KonzeptKartenManifestEintrag(KonzeptKartenId("konzept.zahlkonstante"), "zahlkonstante-v1.json"),
            KonzeptKartenManifestEintrag(KonzeptKartenId("konzept.mengenkonstante"), "mengenkonstante-v1.json"),
            KonzeptKartenManifestEintrag(KonzeptKartenId("konzept.term-zu-methode"), "term-zu-methode-v1.json"),
            KonzeptKartenManifestEintrag(KonzeptKartenId("konzept.zahlenrechner"), "zahlenrechner-v1.json"),
            KonzeptKartenManifestEintrag(KonzeptKartenId("konzept.tensorrechner"), "tensorrechner-v1.json"),
        ),
    )
}

const val ASSET_BASISPFAD = "de/TeutonStudio/MathematikKnoten/konzeptkarte"

private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray(Charsets.UTF_8))
    .joinToString("") { byte -> "%02x".format(byte) }

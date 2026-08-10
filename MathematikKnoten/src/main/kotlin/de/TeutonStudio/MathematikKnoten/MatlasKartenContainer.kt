package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writer für das versionierte, nicht ausführbare `.matlas`-Kartenformat.
 *
 * Der Container besteht in Formatversion 1 ausschließlich aus einem Manifest
 * und der kanonischen Karten-JSON. Alle Einträge besitzen feste relative Pfade
 * und reproduzierbare Zeitstempel, damit identische Kartendaten keinen
 * zufälligen ZIP-Metadatenmüll erzeugen.
 */
object MatlasKartenContainer {
    const val FORMAT_ID = "mathematik-atlas"
    const val FORMAT_VERSION = 1
    const val MIME_TYPE = "application/vnd.teutonstudio.mathematik-atlas"
    const val DATEI_ENDUNG = ".matlas"
    const val MANIFEST_DATEI = "manifest.json"
    const val KARTEN_DATEI = "karte.json"

    private const val ZIP_ZEITSTEMPEL_1980 = 315_532_800_000L

    /** Erzeugt einen vollständigen `.matlas`-Container im Speicher. */
    fun schreibe(
        karte: KartenDaten,
        erstellerVersion: String,
    ): ByteArray {
        val kartenJson = MathematikKartenCodec.schreibe(karte).toByteArray(StandardCharsets.UTF_8)
        val manifest = manifest(karte, erstellerVersion, kartenJson).toByteArray(StandardCharsets.UTF_8)
        val dateien = listOf(
            MANIFEST_DATEI to manifest,
            KARTEN_DATEI to kartenJson,
        )

        return ByteArrayOutputStream().use { ziel ->
            ZipOutputStream(ziel, StandardCharsets.UTF_8).use { zip ->
                zip.setLevel(Deflater.BEST_COMPRESSION)
                dateien.forEach { (pfad, inhalt) ->
                    require(!pfad.startsWith('/') && ".." !in pfad.split('/')) {
                        "Ein .matlas-Eintrag muss einen sicheren relativen Pfad besitzen: $pfad"
                    }
                    val eintrag = ZipEntry(pfad).apply {
                        time = ZIP_ZEITSTEMPEL_1980
                        comment = null
                        extra = null
                    }
                    zip.putNextEntry(eintrag)
                    zip.write(inhalt)
                    zip.closeEntry()
                }
            }
            ziel.toByteArray()
        }
    }

    /**
     * Schreibt zunächst in eine temporäre Datei im Zielordner und ersetzt das
     * Ziel anschließend atomar, soweit das Dateisystem `ATOMIC_MOVE` unterstützt.
     */
    fun schreibeAtomar(
        ziel: Path,
        karte: KartenDaten,
        erstellerVersion: String,
    ): Path {
        val absolut = ziel.toAbsolutePath().normalize()
        val ordner = absolut.parent ?: error("Für .matlas ist ein Zielordner erforderlich.")
        Files.createDirectories(ordner)
        val temporaer = Files.createTempFile(ordner, ".${absolut.fileName}.", ".tmp")
        try {
            Files.write(
                temporaer,
                schreibe(karte, erstellerVersion),
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
            runCatching {
                Files.move(
                    temporaer,
                    absolut,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            }.getOrElse {
                Files.move(temporaer, absolut, StandardCopyOption.REPLACE_EXISTING)
            }
            return absolut
        } finally {
            Files.deleteIfExists(temporaer)
        }
    }

    private fun manifest(
        karte: KartenDaten,
        erstellerVersion: String,
        kartenJson: ByteArray,
    ): String {
        val checksumme = sha256(kartenJson)
        return buildString {
            append("{\n")
            append("  \"format\": ").append(JSONObject.quote(FORMAT_ID)).append(",\n")
            append("  \"formatVersion\": ").append(FORMAT_VERSION).append(",\n")
            append("  \"erstellerVersion\": ").append(JSONObject.quote(erstellerVersion.trim().ifBlank { "unbekannt" })).append(",\n")
            append("  \"karte\": {\n")
            append("    \"id\": ").append(JSONObject.quote(karte.id.wert)).append(",\n")
            append("    \"titel\": ").append(JSONObject.quote(karte.name)).append(",\n")
            append("    \"version\": ").append(karte.version).append("\n")
            append("  },\n")
            append("  \"dateien\": [\n")
            append("    {\"pfad\": ").append(JSONObject.quote(KARTEN_DATEI))
            append(", \"rolle\": \"karte\", \"sha256\": ").append(JSONObject.quote(checksumme))
            append(", \"bytes\": ").append(kartenJson.size).append("}\n")
            append("  ]\n")
            append("}\n")
        }
    }

    internal fun sha256(inhalt: ByteArray): String {
        val hex = "0123456789abcdef"
        val digest = MessageDigest.getInstance("SHA-256").digest(inhalt)
        return buildString(digest.size * 2) {
            digest.forEach { byte ->
                val wert = byte.toInt() and 0xff
                append(hex[wert ushr 4])
                append(hex[wert and 0x0f])
            }
        }
    }
}

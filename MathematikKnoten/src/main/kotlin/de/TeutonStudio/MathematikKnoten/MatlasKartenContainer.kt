package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
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
    private const val MAXIMALE_CONTAINER_BYTES = 16 * 1024 * 1024

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

    /** Liest und validiert einen nicht ausführbaren Container vollständig im Speicher. */
    fun lese(container: ByteArray): KartenDaten {
        require(container.size <= MAXIMALE_CONTAINER_BYTES) { "Der .matlas-Container ist zu groß." }
        val dateien = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(container), StandardCharsets.UTF_8).use { zip ->
            var eintrag = zip.nextEntry
            while (eintrag != null) {
                val pfad = eintrag.name
                require(!eintrag.isDirectory && !pfad.startsWith('/') && ".." !in pfad.split('/')) {
                    "Der .matlas-Container enthält einen unsicheren Pfad."
                }
                require(pfad in setOf(MANIFEST_DATEI, KARTEN_DATEI)) { "Unbekannter .matlas-Eintrag: $pfad" }
                require(pfad !in dateien) { "Doppelter .matlas-Eintrag: $pfad" }
                val inhalt = zip.leseBegrenzt(MAXIMALE_CONTAINER_BYTES)
                dateien[pfad] = inhalt
                zip.closeEntry()
                eintrag = zip.nextEntry
            }
        }
        require(dateien.keys == setOf(MANIFEST_DATEI, KARTEN_DATEI)) {
            "Der .matlas-Container ist unvollständig."
        }
        val manifest = JSONObject(dateien.getValue(MANIFEST_DATEI).toString(StandardCharsets.UTF_8))
        require(manifest.getString("format") == FORMAT_ID) { "Unbekanntes .matlas-Format." }
        require(manifest.getInt("formatVersion") == FORMAT_VERSION) { "Nicht unterstützte .matlas-Version." }
        val kartenJson = dateien.getValue(KARTEN_DATEI)
        val beschreibung = manifest.getJSONArray("dateien").let { dateienArray ->
            require(dateienArray.length() == 1) { "Das .matlas-Manifest besitzt unerwartete Dateien." }
            dateienArray.getJSONObject(0)
        }
        require(beschreibung.getString("pfad") == KARTEN_DATEI && beschreibung.getString("rolle") == "karte") {
            "Das .matlas-Manifest referenziert keine Karte."
        }
        require(beschreibung.getInt("bytes") == kartenJson.size) { "Die .matlas-Dateigröße stimmt nicht." }
        require(beschreibung.getString("sha256") == sha256(kartenJson)) { "Die .matlas-Prüfsumme stimmt nicht." }
        val karte = MathematikKartenCodec.importiere(kartenJson.toString(StandardCharsets.UTF_8))
        val manifestKarte = manifest.getJSONObject("karte")
        require(manifestKarte.getString("id") == karte.id.wert && manifestKarte.getInt("version") == karte.version) {
            "Manifest und Kartendaten widersprechen sich."
        }
        return karte
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

    private fun ZipInputStream.leseBegrenzt(maximaleBytes: Int): ByteArray {
        val ziel = ByteArrayOutputStream(minOf(maximaleBytes, 64 * 1024))
        val puffer = ByteArray(8 * 1024)
        var gesamt = 0
        while (true) {
            val gelesen = read(puffer)
            if (gelesen < 0) break
            gesamt += gelesen
            require(gesamt <= maximaleBytes) { "Ein .matlas-Eintrag ist zu groß." }
            ziel.write(puffer, 0, gelesen)
        }
        return ziel.toByteArray()
    }
}

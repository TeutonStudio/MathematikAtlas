package de.TeutonStudio.MathematikAtlas.speicher

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

/** Rollbackjournal: erst eine vollständige, geprüfte Sicherung erlaubt Bestandsänderungen. */
internal class KartenImportTransaktionen(private val basis: File) {
    private val transaktionen = File(basis, "import-transaktionen")
    private val ziele = listOf("karten", "sicherungen", "karten-ordnung.json", "freigabe-quellen.json", "karten-herkunft.json")
    val wiederherstellungsFehler = mutableListOf<String>()

    init {
        transaktionen.listFiles().orEmpty().filter(File::isDirectory).forEach { ordner ->
            runCatching {
                val journalDatei = File(ordner, "journal.json")
                // Vor der Veröffentlichung des Journals darf noch kein Import begonnen haben.
                if (!journalDatei.exists()) {
                    error("Importjournal fehlt; Bestand und Sicherung werden unverändert erhalten.")
                } else {
                    val journal = JSONObject(journalDatei.readText())
                    if (journal.getString("status") == "ABGESCHLOSSEN") {
                        check(ordner.deleteRecursively()) { "Abgeschlossene Importsicherung konnte nicht entfernt werden." }
                    } else stelleWiederHer(ordner, journal)
                }
            }.onFailure {
                wiederherstellungsFehler += "Importsicherung ${ordner.name} bleibt erhalten: ${it.message}"
            }
        }
    }

    @Synchronized
    fun <T> ausführen(block: () -> T): T {
        check(wiederherstellungsFehler.isEmpty()) { wiederherstellungsFehler.joinToString("\n") }
        check(basis.isDirectory || basis.mkdirs()) { "Kartenverzeichnis konnte nicht angelegt werden." }
        check(transaktionen.isDirectory || transaktionen.mkdirs()) { "Importverzeichnis konnte nicht angelegt werden." }
        val ordner = File(transaktionen, UUID.randomUUID().toString())
        val sicherung = File(ordner, "vorher")
        check(sicherung.mkdirs())
        val vorhanden = ziele.filter { File(basis, it).exists() }
        try {
            vorhanden.forEach { kopiere(File(basis, it), File(sicherung, it)) }
            val journal = JSONObject().apply {
                put("formatVersion", 1)
                put("status", "VORBEREITET")
                put("vorhanden", JSONArray(vorhanden))
                put("pruefsummen", JSONObject().apply {
                    vorhanden.forEach { put(it, prüfsumme(File(sicherung, it))) }
                })
            }
            // Atomare Veröffentlichung; ohne gültiges Journal wird block niemals aufgerufen.
            schreibeJournal(ordner, journal)
            return try {
                val ergebnis = block()
                schreibeJournal(ordner, JSONObject(journal.toString()).put("status", "ABGESCHLOSSEN"))
                ordner.deleteRecursively()
                ergebnis
            } catch (ursache: Throwable) {
                try { stelleWiederHer(ordner, journal) } catch (rollback: Throwable) {
                    wiederherstellungsFehler += "Importwiederherstellung fehlgeschlagen: ${rollback.message}"
                    ursache.addSuppressed(rollback)
                }
                throw ursache
            }
        } catch (ursache: Throwable) {
            if (!File(ordner, "journal.json").exists()) ordner.deleteRecursively()
            throw ursache
        }
    }

    private fun stelleWiederHer(ordner: File, journal: JSONObject) {
        require(journal.getInt("formatVersion") == 1 && journal.getString("status") == "VORBEREITET") {
            "Unbekanntes oder beschädigtes Importjournal."
        }
        val einträge = journal.getJSONArray("vorhanden")
        val vorhanden = List(einträge.length()) { einträge.getString(it) }
        require(vorhanden.distinct().size == vorhanden.size && vorhanden.all { it in ziele }) {
            "Ungültige Ziele im Importjournal."
        }
        val hashes = journal.getJSONObject("pruefsummen")
        val sicherung = File(ordner, "vorher")
        // Alle Sicherungen validieren, bevor auch nur eine Bestandsdatei verändert wird.
        vorhanden.forEach { name ->
            require(File(sicherung, name).exists() && prüfsumme(File(sicherung, name)) == hashes.getString(name)) {
                "Unvollständige oder beschädigte Sicherung für $name."
            }
        }
        ziele.forEach { name ->
            val ziel = File(basis, name)
            check(!ziel.exists() || ziel.deleteRecursively()) { "Wiederherstellungsziel $name konnte nicht ersetzt werden." }
            if (name in vorhanden) kopiere(File(sicherung, name), ziel)
        }
        // Die Sicherung bleibt bis zum vollständigen Erfolg vorhanden: Wiederholung ist idempotent.
        check(ordner.deleteRecursively()) { "Importsicherung konnte nach Wiederherstellung nicht entfernt werden." }
    }

    private fun schreibeJournal(ordner: File, journal: JSONObject) {
        val temporär = File(ordner, "journal.tmp")
        FileOutputStream(temporär).use { stream ->
            stream.write(journal.toString().toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
        check(temporär.renameTo(File(ordner, "journal.json"))) { "Importjournal konnte nicht atomar geschrieben werden." }
    }

    private fun kopiere(quelle: File, ziel: File) {
        if (quelle.isDirectory) {
            check(quelle.copyRecursively(ziel, overwrite = true)) { "Importsicherung konnte nicht vollständig kopiert werden." }
        } else {
            ziel.parentFile?.mkdirs()
            quelle.copyTo(ziel, overwrite = true)
        }
    }

    private fun prüfsumme(datei: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val dateien = if (datei.isDirectory) datei.walkTopDown().toList().sortedBy { it.relativeTo(datei).invariantSeparatorsPath } else listOf(datei)
        dateien.forEach { eintrag ->
            require(!java.nio.file.Files.isSymbolicLink(eintrag.toPath())) { "Symbolische Links sind in Importsicherungen nicht erlaubt." }
            val name = if (datei.isDirectory) eintrag.relativeTo(datei).invariantSeparatorsPath else ""
            digest.update((name + if (eintrag.isDirectory) "/\u0000" else "\u0000").toByteArray())
            if (eintrag.isFile) eintrag.inputStream().use { stream ->
                val puffer = ByteArray(8192)
                while (true) { val n = stream.read(puffer); if (n < 0) break; digest.update(puffer, 0, n) }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}

package de.TeutonStudio.MathematikAtlas.speicher

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Sichert die von einem Import gemeinsam veränderten app-lokalen Dateien.
 * Ein beim nächsten Start gefundenes, nicht abgeschlossenes Journal wird auf
 * den Vorzustand zurückgerollt.
 */
internal class KartenImportTransaktionen(private val basis: File) {
    private val transaktionen = File(basis, "import-transaktionen")
    private val ziele = listOf(
        "karten",
        "sicherungen",
        "karten-ordnung.json",
        "freigabe-quellen.json",
        "karten-herkunft.json",
    )

    init {
        stelleOffeneWiederHer()
    }

    fun <T> ausführen(block: () -> T): T {
        basis.mkdirs()
        transaktionen.mkdirs()
        val ordner = File(transaktionen, UUID.randomUUID().toString())
        val sicherung = File(ordner, "vorher")
        sicherung.mkdirs()
        val vorhanden = ziele.filter { File(basis, it).exists() }
        vorhanden.forEach { name -> kopiere(File(basis, name), File(sicherung, name)) }
        File(ordner, "journal.json").writeText(JSONObject().apply {
            put("status", "VORBEREITET")
            put("vorhanden", JSONArray(vorhanden))
        }.toString())

        return try {
            val ergebnis = block()
            File(ordner, "journal.json").writeText(JSONObject().apply {
                put("status", "ABGESCHLOSSEN")
                put("vorhanden", JSONArray(vorhanden))
            }.toString())
            ordner.deleteRecursively()
            ergebnis
        } catch (ursache: Throwable) {
            stelleWiederHer(ordner)
            throw ursache
        }
    }

    private fun stelleOffeneWiederHer() {
        transaktionen.listFiles().orEmpty().filter(File::isDirectory).forEach { ordner ->
            val status = runCatching {
                JSONObject(File(ordner, "journal.json").readText()).optString("status")
            }.getOrDefault("")
            if (status == "ABGESCHLOSSEN") ordner.deleteRecursively() else stelleWiederHer(ordner)
        }
    }

    private fun stelleWiederHer(ordner: File) {
        val journal = runCatching { JSONObject(File(ordner, "journal.json").readText()) }.getOrNull()
        val vorhanden = journal?.optJSONArray("vorhanden")?.let { array ->
            List(array.length()) { array.getString(it) }.toSet()
        }.orEmpty()
        val sicherung = File(ordner, "vorher")
        ziele.forEach { name ->
            val ziel = File(basis, name)
            if (ziel.exists()) ziel.deleteRecursively()
            if (name in vorhanden) kopiere(File(sicherung, name), ziel)
        }
        ordner.deleteRecursively()
    }

    private fun kopiere(quelle: File, ziel: File) {
        if (quelle.isDirectory) {
            quelle.copyRecursively(ziel, overwrite = true)
        } else {
            ziel.parentFile?.mkdirs()
            quelle.copyTo(ziel, overwrite = true)
        }
    }
}

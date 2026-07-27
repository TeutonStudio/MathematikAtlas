package de.TeutonStudio.MathematikAtlas.speicher

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import java.io.File

class KartenSpeicher(context: Context) {
    private val basis = File(context.filesDir, "MathematikAtlas")
    private val kartenOrdner = File(basis, "karten")
    private val sicherungsOrdner = File(basis, "sicherungen")

    init { kartenOrdner.mkdirs(); sicherungsOrdner.mkdirs() }

    fun liste(archivierteEinschließen: Boolean = false): List<KartenDaten> = kartenOrdner.listFiles().orEmpty()
        .filter { it.isDirectory }
        .mapNotNull { ordner ->
            ordner.listFiles { f -> f.name.matches(Regex("v\\d+\\.json")) }.orEmpty()
                .maxByOrNull { versionAusDatei(it) }
                ?.let(::leseDatei)
        }
        .filter { archivierteEinschließen || !it.archiviert }
        .sortedBy { it.name.lowercase() }

    fun lade(verweis: KartenVerweis): KartenDaten? = leseDatei(dateiFür(verweis.kartenId, verweis.version))
    fun ladeAktuell(id: KartenId): KartenDaten? = File(kartenOrdner, id.wert)
        .listFiles { f -> f.name.matches(Regex("v\\d+\\.json")) }.orEmpty()
        .maxByOrNull(::versionAusDatei)?.let(::leseDatei)

    fun speichere(karte: KartenDaten): KartenDaten {
        val zielVersion = if (versionWirdVerwendet(KartenVerweis(karte.id, karte.version))) {
            maxOf(karte.version + 1, höchsteVersion(karte.id) + 1)
        } else karte.version
        val zuSpeichern = if (zielVersion == karte.version) karte else karte.copy(version = zielVersion, erstelltAm = System.currentTimeMillis())
        val datei = dateiFür(zuSpeichern.id, zuSpeichern.version)
        datei.parentFile?.mkdirs()
        if (datei.exists()) datei.copyTo(File(sicherungsOrdner, "${zuSpeichern.id.wert}-v${zuSpeichern.version}-${System.currentTimeMillis()}.json"), overwrite = true)
        val temporär = File(datei.parentFile, "${datei.name}.tmp")
        temporär.writeText(KartenJson.schreibe(zuSpeichern))
        if (!temporär.renameTo(datei)) { temporär.copyTo(datei, overwrite = true); temporär.delete() }
        return zuSpeichern
    }

    fun importiere(text: String): KartenDaten {
        val gelesen = KartenJson.lese(text)
        val version = maxOf(gelesen.version, höchsteVersion(gelesen.id) + 1)
        return speichere(gelesen.copy(version = version, erstelltAm = System.currentTimeMillis()))
    }

    fun exportiere(karte: KartenDaten) = KartenJson.schreibe(karte)

    fun archiviere(karte: KartenDaten): KartenDaten = speichere(karte.copy(archiviert = true))

    fun versionWirdVerwendet(verweis: KartenVerweis): Boolean = alleVersionen().any { karte ->
        karte.id != verweis.kartenId && karte.knoten.any { it.kartenVerweis == verweis }
    }

    fun verwendungen(verweis: KartenVerweis): List<KartenDaten> = alleVersionen().filter { karte -> karte.knoten.any { it.kartenVerweis == verweis } }.toList()

    private fun alleVersionen(): Sequence<KartenDaten> = kartenOrdner.walkTopDown().filter { it.isFile && it.name.matches(Regex("v\\d+\\.json")) }.mapNotNull(::leseDatei)
    private fun höchsteVersion(id: KartenId): Int = File(kartenOrdner, id.wert).listFiles().orEmpty().maxOfOrNull(::versionAusDatei) ?: 0
    private fun dateiFür(id: KartenId, version: Int) = File(File(kartenOrdner, id.wert), "v$version.json")
    private fun versionAusDatei(file: File) = file.name.removePrefix("v").removeSuffix(".json").toIntOrNull() ?: 0
    private fun leseDatei(file: File): KartenDaten? = runCatching { if (file.exists()) KartenJson.lese(file.readText()) else null }.getOrNull()
}

package de.TeutonStudio.MathematikAtlas.desktop

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import java.nio.file.*
import kotlin.io.path.*

class DesktopKartenSpeicher(
    val basis: Path = standardDatenVerzeichnis(),
) {
    private val kartenVerzeichnis = basis.resolve("karten")
    private val aktuellDatei = basis.resolve("aktuelle-karte.txt")

    init { kartenVerzeichnis.createDirectories() }

    fun ladeAktuell(): KartenDaten? {
        val pfad = aktuellDatei.takeIf { it.exists() }?.readText()?.trim()?.takeIf(String::isNotBlank)?.let(basis::resolve)
        return pfad?.takeIf { it.isRegularFile() }?.let(::ladeDatei)
            ?: alleDateien().maxByOrNull { it.getLastModifiedTime().toMillis() }?.let(::ladeDatei)
    }

    fun lade(verweis: KartenVerweis): KartenDaten? = kartenVerzeichnis
        .resolve(verweis.kartenId.wert)
        .resolve("v${verweis.version}.json")
        .takeIf { it.isRegularFile() }
        ?.let(::ladeDatei)

    fun speichere(karte: KartenDaten): KartenDaten {
        val ordner = kartenVerzeichnis.resolve(karte.id.wert).also { it.createDirectories() }
        val vorhandeneVersion = Files.list(ordner).use { dateien ->
            dateien.toList().map { it.fileName.toString() }
                .filter { it.matches(Regex("v\\d+\\.json")) }
                .map { it.removePrefix("v").removeSuffix(".json").toInt() }
                .maxOrNull() ?: 0
        }
        val version = if (vorhandeneVersion == 0) karte.version.coerceAtLeast(1) else maxOf(vorhandeneVersion + 1, karte.version)
        val gespeichert = karte.copy(version = version)
        val ziel = ordner.resolve("v$version.json")
        atomarSchreiben(ziel, KartenDatenJson.schreibe(gespeichert))
        atomarSchreiben(aktuellDatei, basis.relativize(ziel).toString())
        return gespeichert
    }

    fun importiere(text: String): KartenDaten = speichere(KartenDatenJson.lese(text))
    fun exportiere(karte: KartenDaten): String = KartenDatenJson.schreibe(karte)

    private fun ladeDatei(pfad: Path): KartenDaten? = runCatching { KartenDatenJson.lese(pfad.readText()) }.getOrNull()

    private fun alleDateien(): List<Path> = if (!kartenVerzeichnis.exists()) emptyList() else
        Files.walk(kartenVerzeichnis).use { dateien -> dateien.filter { it.isRegularFile() && it.extension == "json" }.toList() }

    private fun atomarSchreiben(ziel: Path, text: String) {
        ziel.parent?.createDirectories()
        val temporär = ziel.resolveSibling(".${ziel.fileName}.tmp")
        temporär.writeText(text)
        runCatching {
            Files.move(temporär, ziel, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(temporär, ziel, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        fun standardDatenVerzeichnis(): Path {
            val xdg = System.getenv("XDG_DATA_HOME")?.trim()?.takeIf(String::isNotEmpty)
            val wurzel = xdg?.let(Paths::get) ?: Paths.get(System.getProperty("user.home"), ".local", "share")
            return wurzel.resolve("MathematikAtlas")
        }
    }
}

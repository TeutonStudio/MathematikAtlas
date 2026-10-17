package de.TeutonStudio.MathematikAtlas.speicher

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikKartenCodec
import de.TeutonStudio.MathematikKnoten.MathematikKartenMigrationen
import de.TeutonStudio.MathematikKnoten.MatlasKartenContainer
import org.json.JSONObject
import java.io.File

data class KartenVerwendung(
    val verwendendeKarte: KartenDaten,
    val verwendeterVerweis: KartenVerweis,
)

class KartenSpeicher(private val context: Context) {
    private val basis = File(context.filesDir, "MathematikAtlas")
    private val kartenOrdner = File(basis, "karten")
    private val sicherungsOrdner = File(basis, "sicherungen")
    private val papierkorbSpeicher = PapierkorbSpeicher(context)
    private val ordnungsSpeicher = KartenOrdnungSpeicher(context)
    private val freigabeQuellenSpeicher = FreigabeQuellenSpeicher(context)
    private val herkunftSpeicher = KartenHerkunftSpeicher(context)
    private val herkunftAuflösung = KartenHerkunftAuflösung(context)
    private val importTransaktionen = KartenImportTransaktionen(basis)

    val importWiederherstellungsFehler: List<String> get() = importTransaktionen.wiederherstellungsFehler.toList()

    init {
        kartenOrdner.mkdirs()
        sicherungsOrdner.mkdirs()
    }

    fun liste(archivierteEinschließen: Boolean = false): List<KartenDaten> {
        val papierkorbIds = if (archivierteEinschließen) emptySet() else papierkorbSpeicher.kartenIds()
        return kartenOrdner.listFiles().orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { ordner ->
                ordner.listFiles { f -> f.name.matches(Regex("v\\d+\\.json")) }.orEmpty()
                    .sortedByDescending(::versionAusDatei)
                    .firstNotNullOfOrNull(::leseDatei)
            }
            .filter { archivierteEinschließen || (!it.archiviert && it.id !in papierkorbIds) }
            .sortedBy { it.name.lowercase() }
    }

    fun lade(verweis: KartenVerweis): KartenDaten? = leseDatei(
        dateiFür(verweis.kartenId, verweis.version),
    )

    fun ladeAktuell(id: KartenId): KartenDaten? = kartenOrdnerFür(id)
        .listFiles { f -> f.name.matches(Regex("v\\d+\\.json")) }
        .orEmpty()
        .sortedByDescending(::versionAusDatei)
        .firstNotNullOfOrNull(::leseDatei)

    fun speichere(karte: KartenDaten): KartenDaten {
        val normalisiert = MathematikKartenMigrationen.vorSpeichern(karte)
        val verweis = KartenVerweis(normalisiert.id, normalisiert.version)
        val zielVersion = if (mussOriginalrevisionBewahren(dateiFür(verweis.kartenId, verweis.version), verweis) || versionWirdVerwendet(verweis)) {
            Math.addExact(maxOf(normalisiert.version, höchsteVersion(normalisiert.id)), 1)
        } else {
            normalisiert.version
        }
        val zuSpeichern = if (zielVersion == normalisiert.version) {
            normalisiert
        } else {
            normalisiert.copy(version = zielVersion, erstelltAm = System.currentTimeMillis())
        }
        speichereExakt(zuSpeichern)
        return zuSpeichern
    }

    fun importiere(text: String): KartenDaten {
        require(text.toByteArray(Charsets.UTF_8).size <= SichererKartenAbruf.MAXIMALE_GRÖSSE) { "Die Importdatei ist größer als 16 MiB." }
        val paket = if (KartenFreigabePaket.istFreigabePaket(text)) KartenFreigabePaket.lese(text) else null
        val karte = if (paket == null) MathematikKartenCodec.importiere(text) else null
        karte?.let(::prüfeImportKarte)
        return importTransaktionen.ausführen {
            if (paket != null) {
                importierePaket(paket)
            } else {
                val gelesen = requireNotNull(karte)
                val version = maxOf(gelesen.version, höchsteVersion(gelesen.id) + 1)
                val gespeichert = speichere(gelesen.copy(version = version, erstelltAm = System.currentTimeMillis()))
                herkunftSpeicher.markiereFremd(setOf(gespeichert.id))
                gespeichert
            }
        }
    }

    fun importiere(inhalt: ByteArray): KartenDaten {
        require(inhalt.size <= SichererKartenAbruf.MAXIMALE_GRÖSSE) { "Die Importdatei ist größer als 16 MiB." }
        val istZip = inhalt.size >= 4 && inhalt[0] == 'P'.code.toByte() && inhalt[1] == 'K'.code.toByte()
        if (!istZip) return importiere(inhalt.toString(Charsets.UTF_8))
        val karte = MatlasKartenContainer.lese(inhalt)
        prüfeImportKarte(karte)
        return importTransaktionen.ausführen {
            val version = maxOf(karte.version, höchsteVersion(karte.id) + 1)
            val gespeichert = speichere(karte.copy(version = version, erstelltAm = System.currentTimeMillis()))
            herkunftSpeicher.markiereFremd(setOf(gespeichert.id))
            gespeichert
        }
    }

    fun herkunftFür(id: KartenId): KartenHerkunft = herkunftAuflösung.fürKarte(id)

    fun fremdVerwalteteOrdner(): Set<List<String>> = herkunftAuflösung.fremdVerwalteteOrdner()

    fun exportiere(karte: KartenDaten) = KartenJson.schreibe(karte)

    fun erstelleFreigabePaket(
        name: String,
        art: FreigabeArt,
        wurzelKarten: List<KartenDaten>,
        ordnung: KartenOrdnung,
        sammlungsPfad: List<String>? = null,
    ): String = KartenFreigabePaket.erstelle(
        name = name,
        art = art,
        wurzelKarten = wurzelKarten,
        ordnung = ordnung,
        sammlungsPfad = sammlungsPfad,
        profil = LokalesProfilSpeicher(context).lade(),
        lade = ::lade,
    )

    fun archiviere(karte: KartenDaten): KartenDaten = speichere(karte.copy(archiviert = true))

    fun papierkorbEinträge(): List<PapierkorbEintrag> = papierkorbSpeicher.liste()

    fun legeInPapierkorb(eintrag: PapierkorbEintrag) = papierkorbSpeicher.legeAb(eintrag)

    fun entfernePapierkorbEintrag(eintragId: String) = papierkorbSpeicher.entferne(eintragId)

    fun blockierendeVerwendungen(kartenIds: Set<KartenId>): List<KartenVerwendung> {
        if (kartenIds.isEmpty()) return emptyList()
        return alleVersionen()
            .filter { it.id !in kartenIds }
            .flatMap { karte ->
                karte.knoten.asSequence().flatMap { knoten ->
                    knoten.alleKartenVerweise().asSequence()
                        .filter { it.kartenId in kartenIds }
                        .map { KartenVerwendung(karte, it) }
                }
            }
            .distinctBy {
                Triple(it.verwendendeKarte.id, it.verwendendeKarte.version, it.verwendeterVerweis)
            }
            .toList()
    }

    fun löscheEndgültig(kartenIds: Set<KartenId>): List<KartenVerwendung> {
        val blockierend = blockierendeVerwendungen(kartenIds)
        if (blockierend.isNotEmpty()) return blockierend
        kartenIds.forEach { id ->
            kartenOrdnerFür(id).deleteRecursively()
            sicherungsOrdner.listFiles().orEmpty()
                .filter { it.name.startsWith("${id.wert}-") }
                .forEach(File::delete)
        }
        papierkorbSpeicher.entferneKarten(kartenIds)
        freigabeQuellenSpeicher.entferne(kartenIds)
        herkunftSpeicher.entferne(kartenIds)
        return emptyList()
    }

    fun versionWirdVerwendet(verweis: KartenVerweis): Boolean = alleVersionen().any { karte ->
        karte.id != verweis.kartenId && karte.knoten.any { verweis in it.alleKartenVerweise() }
    }

    fun verwendungen(verweis: KartenVerweis): List<KartenDaten> = alleVersionen()
        .filter { karte -> karte.knoten.any { verweis in it.alleKartenVerweise() } }
        .toList()

    private fun importierePaket(paket: GelesenesFreigabePaket): KartenDaten {
        val vorhandeneQuellen = freigabeQuellenSpeicher.liste().filter {
            it.quelle.herausgeberId == paket.quelle.herausgeberId &&
                it.quelle.ressourcenId == paket.quelle.ressourcenId
        }
        val bekannteIds = vorhandeneQuellen.associate {
            it.ursprünglicheKartenId to it.lokaleKartenId
        }
        val idAbbildung = paket.karten
            .map(KartenDaten::id)
            .distinct()
            .associateWith { ursprünglicheId ->
                bekannteIds[ursprünglicheId]
                    ?: ursprünglicheId.takeIf { ladeAktuell(it) == null }
                    ?: neueKartenId()
            }
        val remappteKarten = paket.karten.map { karte ->
            val migriert = MathematikKartenMigrationen.nachLaden(karte)
            migriert.copy(
                id = idAbbildung.getValue(migriert.id),
                knoten = migriert.knoten.map { knoten ->
                    knoten.copy(
                        kartenVerweis = knoten.kartenVerweis?.remappe(idAbbildung),
                        eingangsKartenVerweise = knoten.eingangsKartenVerweise.mapValues { (_, verweis) ->
                            verweis.remappe(idAbbildung)
                        },
                    )
                },
            )
        }
        remappteKarten
            .sortedWith(compareBy({ it.id.wert }, { it.version }))
            .forEach(::speichereExakt)

        val paketOrdnung = ordnungsSpeicher.lade()
        val importStamm = eindeutigerImportPfad(
            paketOrdnung,
            listOf(
                "Freigaben",
                "${paket.quelle.ressourcenName} – ${paket.quelle.herausgeberPseudonym}",
            ),
        )
        val ordner = paket.ordnerPfade.map { importStamm + it }
        val zuordnungen = paket.kartenPfade.mapNotNull { (ursprünglicheId, pfad) ->
            idAbbildung[ursprünglicheId]?.let { lokaleId ->
                lokaleId to (importStamm + pfad)
            }
        }.toMap()
        ordnungsSpeicher.speichere(
            paketOrdnung
                .mitOrdner(importStamm)
                .mitOrdnern(ordner)
                .mitKartenInOrdnern(zuordnungen),
        )

        val quellenEinträge = paket.karten.groupBy(KartenDaten::id).map {
            (ursprünglicheId, versionen) ->
            ImportierteFreigabe(
                lokaleKartenId = idAbbildung.getValue(ursprünglicheId),
                ursprünglicheKartenId = ursprünglicheId,
                ursprünglicheVersion = versionen.maxOf(KartenDaten::version),
                quelle = paket.quelle,
            )
        }
        freigabeQuellenSpeicher.speichere(quellenEinträge)
        if (paket.quelle.herausgeberId != LokalesProfilSpeicher(context).lade().id) {
            herkunftSpeicher.markiereFremd(
                kartenIds = idAbbildung.values.toSet(),
                ordner = buildSet {
                    add(importStamm)
                    addAll(ordner)
                },
            )
        }

        val wurzel = paket.wurzeln.first()
        val lokaleWurzelId = idAbbildung.getValue(wurzel.kartenId)
        return requireNotNull(lade(KartenVerweis(lokaleWurzelId, wurzel.version))) {
            "Die importierte Wurzelkarte konnte nicht geladen werden."
        }
    }

    private fun KartenVerweis.remappe(
        idAbbildung: Map<KartenId, KartenId>,
    ): KartenVerweis = copy(kartenId = idAbbildung[kartenId] ?: kartenId)

    private fun eindeutigerImportPfad(
        ordnung: KartenOrdnung,
        basisPfad: List<String>,
    ): List<String> {
        if (basisPfad !in ordnung.ordner) return basisPfad
        var nummer = 2
        while (basisPfad.dropLast(1) + "${basisPfad.last()} $nummer" in ordnung.ordner) {
            nummer += 1
        }
        return basisPfad.dropLast(1) + "${basisPfad.last()} $nummer"
    }

    private fun speichereExakt(karte: KartenDaten) {
        val datei = dateiFür(karte.id, karte.version)
        datei.parentFile?.mkdirs()
        if (datei.exists()) {
            datei.copyTo(
                File(
                    sicherungsOrdner,
                    "${karte.id.wert}-v${karte.version}-${System.currentTimeMillis()}.json",
                ),
                overwrite = true,
            )
        }
        val temporär = File(datei.parentFile, "${datei.name}.tmp")
        temporär.writeText(KartenJson.schreibe(karte))
        if (!temporär.renameTo(datei)) {
            temporär.copyTo(datei, overwrite = true)
            temporär.delete()
        }
    }

    private fun alleVersionen(): Sequence<KartenDaten> = kartenOrdner.walkTopDown()
        .filter { it.isFile && it.name.matches(Regex("v\\d+\\.json")) }
        .mapNotNull(::leseDatei)

    private fun höchsteVersion(id: KartenId): Int = kartenOrdnerFür(id)
        .listFiles()
        .orEmpty()
        .maxOfOrNull(::versionAusDatei)
        ?: 0

    private fun kartenOrdnerFür(id: KartenId): File {
        prüfeKartenId(id)
        return File(kartenOrdner, id.wert)
    }

    private fun dateiFür(id: KartenId, version: Int) = File(
        kartenOrdnerFür(id),
        "v$version.json",
    )

    private fun versionAusDatei(file: File) = file.name
        .removePrefix("v")
        .removeSuffix(".json")
        .toIntOrNull()
        ?: 0

    private fun leseDatei(file: File): KartenDaten? = runCatching {
        if (file.exists()) MathematikKartenCodec.lade(file.readText()) else null
    }.getOrNull()
}

/** Nur der gespeicherte Originalzustand entscheidet; gewöhnliche aktuelle Bearbeitungen bleiben unverändert. */
internal fun mussOriginalrevisionBewahren(datei:File, erwartet:KartenVerweis):Boolean {
    if (!datei.exists()) return false
    return runCatching {
        val text=datei.readText()
        val original=KartenDatenJson.lese(text)
        original.id!=erwartet.kartenId || original.version!=erwartet.version ||
            KartenDatenJson.formatVersion(text)!=KartenDatenJson.FORMAT_VERSION ||
            MathematikKartenMigrationen.vorSpeichern(MathematikKartenCodec.lade(text))!=original
    }.getOrDefault(true)
}

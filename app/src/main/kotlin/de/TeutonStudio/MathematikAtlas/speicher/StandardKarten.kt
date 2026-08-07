package de.TeutonStudio.MathematikAtlas.speicher

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikAtlas.BeispielKarten
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

private const val STANDARDKARTEN_ASSET_BASIS =
    "de/TeutonStudio/MathematikAtlas/standardkarten"
private const val STANDARDKARTEN_MANIFEST = "$STANDARDKARTEN_ASSET_BASIS/manifest.json"

enum class StandardKartenStatus {
    AKTIV,
    BENUTZERGEÄNDERT,
    GELÖSCHT,
}

data class InstallierteStandardKarte(
    val sourceId: String,
    val sourceVersion: String,
    val sourceHash: String,
    val lokaleKartenId: KartenId,
    val installierterHash: String,
    val status: StandardKartenStatus,
)

data class StandardKartenInstallationsBericht(
    val installiert: Int = 0,
    val aktualisiert: Int = 0,
    val übersprungen: Int = 0,
    val alsGeändertErkannt: Int = 0,
    val alsGelöschtErkannt: Int = 0,
)

private data class StandardKartenEintrag(
    val sourceId: String,
    val cardId: KartenId,
    val folder: List<String>,
    val title: String,
    val path: String,
    val sourceHash: String,
    val requiredNodeTypes: Set<String>,
    val dependsOn: List<String>,
)

private data class StandardKartenPaket(
    val version: String,
    val entries: List<StandardKartenEintrag>,
    val karten: Map<KartenId, KartenDaten>,
)

internal class StandardKartenProvenienzSpeicher(
    private val datei: File,
) {
    constructor(context: Context) : this(
        File(File(context.filesDir, "MathematikAtlas"), "standardkarten-quellen.json"),
    )

    fun liste(): List<InstallierteStandardKarte> = runCatching {
        if (!datei.exists()) return@runCatching emptyList()
        val json = JSONObject(datei.readText())
        val karten = json.optJSONArray("karten") ?: JSONArray()
        List(karten.length()) { index ->
            val eintrag = karten.getJSONObject(index)
            InstallierteStandardKarte(
                sourceId = eintrag.getString("sourceId"),
                sourceVersion = eintrag.optString("sourceVersion", ""),
                sourceHash = eintrag.optString("sourceHash", ""),
                lokaleKartenId = KartenId(eintrag.getString("lokaleKartenId")),
                installierterHash = eintrag.optString("installierterHash", ""),
                status = runCatching {
                    StandardKartenStatus.valueOf(eintrag.optString("status"))
                }.getOrDefault(StandardKartenStatus.AKTIV),
            )
        }
    }.getOrDefault(emptyList())

    fun speichere(einträge: Collection<InstallierteStandardKarte>) {
        datei.parentFile?.mkdirs()
        val temporär = File(datei.parentFile, "${datei.name}.tmp")
        temporär.writeText(JSONObject().apply {
            put("formatVersion", 1)
            put("karten", JSONArray().apply {
                einträge.sortedBy(InstallierteStandardKarte::sourceId).forEach { eintrag ->
                    put(JSONObject().apply {
                        put("sourceId", eintrag.sourceId)
                        put("sourceVersion", eintrag.sourceVersion)
                        put("sourceHash", eintrag.sourceHash)
                        put("lokaleKartenId", eintrag.lokaleKartenId.wert)
                        put("installierterHash", eintrag.installierterHash)
                        put("status", eintrag.status.name)
                    })
                }
            })
        }.toString(2))
        if (!temporär.renameTo(datei)) {
            temporär.copyTo(datei, overwrite = true)
            temporär.delete()
        }
    }
}

internal class StandardKartenInstaller(
    private val context: Context,
    private val speicher: KartenSpeicher,
    private val bekannteKnotenArten: Set<String>,
) {
    private val provenienz = StandardKartenProvenienzSpeicher(context)
    private val ordnungsSpeicher = KartenOrdnungSpeicher(context)

    fun installiere(): StandardKartenInstallationsBericht {
        migriereUnveränderteLegacyBeispiele()

        val paket = lesePaket()
        var status = provenienz.liste().associateByTo(linkedMapOf()) { it.sourceId }
        var bericht = StandardKartenInstallationsBericht()

        // Fehlende lokale Karten werden zu Tombstones. Nutzeränderungen werden anhand
        // des zuletzt installierten normalisierten Inhalts erkannt und nie überschrieben.
        status = status.mapValuesTo(linkedMapOf()) { (_, alt) ->
            val lokal = speicher.ladeAktuell(alt.lokaleKartenId)
            when {
                alt.status == StandardKartenStatus.GELÖSCHT -> alt
                lokal == null -> {
                    bericht = bericht.copy(alsGelöschtErkannt = bericht.alsGelöschtErkannt + 1)
                    alt.copy(status = StandardKartenStatus.GELÖSCHT)
                }
                alt.installierterHash.isNotBlank() &&
                    standardKartenInhaltsHash(lokal) != alt.installierterHash -> {
                    bericht = bericht.copy(alsGeändertErkannt = bericht.alsGeändertErkannt + 1)
                    alt.copy(status = StandardKartenStatus.BENUTZERGEÄNDERT)
                }
                else -> alt
            }
        }

        val lokaleIds = paket.entries.associate { eintrag ->
            eintrag.sourceId to (status[eintrag.sourceId]?.lokaleKartenId ?: neueKartenId())
        }.toMutableMap()

        // Quellkarten referenzieren sich über ihre stabile cardId. Lokal wird diese auf
        // die nutzereigene ID und die tatsächlich installierte Version abgebildet.
        val sourceIdFürCardId = paket.entries.associate { it.cardId to it.sourceId }
        val lokaleVerweise = paket.entries.associate { eintrag ->
            val id = lokaleIds.getValue(eintrag.sourceId)
            val version = speicher.ladeAktuell(id)?.version ?: 1
            eintrag.sourceId to KartenVerweis(id, version)
        }.toMutableMap()

        var ordnung = ordnungsSpeicher.lade()

        for (eintrag in paket.entries) {
            val alt = status[eintrag.sourceId]
            if (alt?.status == StandardKartenStatus.GELÖSCHT ||
                alt?.status == StandardKartenStatus.BENUTZERGEÄNDERT
            ) {
                bericht = bericht.copy(übersprungen = bericht.übersprungen + 1)
                continue
            }

            if (!bekannteKnotenArten.containsAll(eintrag.requiredNodeTypes)) {
                bericht = bericht.copy(übersprungen = bericht.übersprungen + 1)
                continue
            }

            val abhängigkeitenVerfügbar = eintrag.dependsOn.all { sourceId ->
                val zustand = status[sourceId]
                val verweis = lokaleVerweise[sourceId]
                zustand?.status != StandardKartenStatus.GELÖSCHT &&
                    verweis != null &&
                    speicher.lade(verweis) != null
            }
            if (!abhängigkeitenVerfügbar) {
                bericht = bericht.copy(übersprungen = bericht.übersprungen + 1)
                continue
            }

            val quelle = requireNotNull(paket.karten[eintrag.cardId]) {
                "Standardkarte '${eintrag.sourceId}' fehlt im Paket."
            }
            val lokaleId = lokaleIds.getValue(eintrag.sourceId)
            val aktuell = alt?.let { speicher.ladeAktuell(it.lokaleKartenId) }

            if (alt != null && aktuell != null && alt.sourceHash == eintrag.sourceHash) {
                lokaleVerweise[eintrag.sourceId] = KartenVerweis(aktuell.id, aktuell.version)
                continue
            }

            val remappt = quelle.remappeStandardKartenVerweise(
                eigeneLokaleId = lokaleId,
                sourceIdFürCardId = sourceIdFürCardId,
                lokaleVerweise = lokaleVerweise,
                lokaleVersion = aktuell?.version ?: 1,
            )
            val gespeichert = speicher.speichere(remappt)
            lokaleVerweise[eintrag.sourceId] = KartenVerweis(gespeichert.id, gespeichert.version)

            status[eintrag.sourceId] = InstallierteStandardKarte(
                sourceId = eintrag.sourceId,
                sourceVersion = paket.version,
                sourceHash = eintrag.sourceHash,
                lokaleKartenId = gespeichert.id,
                installierterHash = standardKartenInhaltsHash(gespeichert),
                status = StandardKartenStatus.AKTIV,
            )

            if (alt == null) {
                ordnung = ordnung.mitKarteInOrdner(gespeichert.id, eintrag.folder)
                bericht = bericht.copy(installiert = bericht.installiert + 1)
            } else {
                bericht = bericht.copy(aktualisiert = bericht.aktualisiert + 1)
            }
        }

        provenienz.speichere(status.values)
        ordnungsSpeicher.speichere(ordnung)
        return bericht
    }

    private fun lesePaket(): StandardKartenPaket {
        val text = context.assets.open(STANDARDKARTEN_MANIFEST)
            .bufferedReader()
            .use { it.readText() }
        val json = JSONObject(text)
        require(json.optInt("standardKartenFormatVersion", 0) == 1) {
            "Nicht unterstützte Standardkarten-Paketversion."
        }
        val entriesJson = json.getJSONArray("entries")
        val entries = List(entriesJson.length()) { index ->
            val e = entriesJson.getJSONObject(index)
            StandardKartenEintrag(
                sourceId = e.getString("sourceId"),
                cardId = KartenId(e.getString("cardId")),
                folder = e.getJSONArray("folder").zuStringListe(),
                title = e.getString("title"),
                path = e.getString("path"),
                sourceHash = e.getString("sourceHash"),
                requiredNodeTypes = e.getJSONArray("requiredNodeTypes").zuStringListe().toSet(),
                dependsOn = e.optJSONArray("dependsOn").zuStringListe(),
            )
        }
        require(entries.map { it.sourceId }.distinct().size == entries.size) {
            "Standardkarten-sourceIds müssen eindeutig sein."
        }
        require(entries.map { it.cardId }.distinct().size == entries.size) {
            "Standardkarten-cardIds müssen eindeutig sein."
        }

        val karten = entries.associate { eintrag ->
            require(!eintrag.path.startsWith("/") && ".." !in eintrag.path.split('/')) {
                "Ungültiger Standardkartenpfad '${eintrag.path}'."
            }
            val kartenText = context.assets
                .open("$STANDARDKARTEN_ASSET_BASIS/${eintrag.path}")
                .bufferedReader()
                .use { it.readText() }
            val karte = KartenJson.lese(kartenText)
            require(karte.id == eintrag.cardId) {
                "Standardkarte '${eintrag.sourceId}' besitzt die falsche cardId ${karte.id.wert}."
            }
            eintrag.cardId to karte
        }
        return StandardKartenPaket(
            version = json.getString("version"),
            entries = entries,
            karten = karten,
        )
    }

    /**
     * Entfernt die historischen fünf Beispielkarten nur dann, wenn der komplette
     * Satz strukturell noch dem ausgelieferten Seed entspricht, nicht im Papierkorb
     * liegt, nicht verschoben wurde und von keiner fremden Karte referenziert wird.
     * Bei jedem Zweifel gewinnt der Nutzerinhalt.
     */
    private fun migriereUnveränderteLegacyBeispiele() {
        val marker = File(File(context.filesDir, "MathematikAtlas"), "legacy-beispiele-v332.json")
        if (marker.exists()) return

        val aktuell = speicher.liste(archivierteEinschließen = true)
        val erwartete = BeispielKarten.alle()
        val ordnung = ordnungsSpeicher.lade()
        val papierkorbIds = speicher.papierkorbEinträge()
            .flatMapTo(mutableSetOf()) { it.kartenIds }

        val treffer = erwartete.mapNotNull { vorlage ->
            val signatur = legacyBeispielSignatur(vorlage)
            aktuell.singleOrNull { kandidat ->
                !kandidat.archiviert &&
                    kandidat.id !in papierkorbIds &&
                    ordnung.ordnerFür(kandidat.id).isEmpty() &&
                    legacyBeispielSignatur(kandidat) == signatur
            }
        }

        val ids = treffer.mapTo(linkedSetOf()) { it.id }
        val vollständigUnverändert = treffer.size == erwartete.size &&
            ids.size == erwartete.size &&
            speicher.blockierendeVerwendungen(ids).isEmpty()

        var ergebnis = "beibehalten"
        if (vollständigUnverändert && speicher.löscheEndgültig(ids).isEmpty()) {
            ordnungsSpeicher.speichere(ordnung.ohneKarten(ids))
            ergebnis = "migriert"
        }

        marker.parentFile?.mkdirs()
        marker.writeText(JSONObject().apply {
            put("formatVersion", 1)
            put("status", ergebnis)
        }.toString(2))
    }
}

internal fun standardKartenInhaltsHash(karte: KartenDaten): String {
    val normalisiert = karte.copy(
        version = 1,
        erstelltAm = 0L,
        archiviert = false,
        ansicht = AnsichtsFenster.Standard,
    )
    val bytes = KartenJson.schreibe(normalisiert).toByteArray(Charsets.UTF_8)
    return MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}

private fun KartenDaten.remappeStandardKartenVerweise(
    eigeneLokaleId: KartenId,
    sourceIdFürCardId: Map<KartenId, String>,
    lokaleVerweise: Map<String, KartenVerweis>,
    lokaleVersion: Int,
): KartenDaten {
    fun remappe(verweis: KartenVerweis): KartenVerweis {
        val sourceId = sourceIdFürCardId[verweis.kartenId] ?: return verweis
        return lokaleVerweise[sourceId] ?: verweis
    }

    return copy(
        id = eigeneLokaleId,
        version = lokaleVersion,
        erstelltAm = System.currentTimeMillis(),
        knoten = knoten.map { knoten ->
            knoten.copy(
                kartenVerweis = knoten.kartenVerweis?.let(::remappe),
                eingangsKartenVerweise = knoten.eingangsKartenVerweise.mapValues { (_, verweis) ->
                    remappe(verweis)
                },
            )
        },
    )
}

private fun legacyBeispielSignatur(karte: KartenDaten): String {
    val knotenIndex = karte.knoten.withIndex().associate { it.value.id to it.index }
    fun anschlussIndex(ref: AnschlussVerweis): Int {
        val knoten = karte.knoten[knotenIndex.getValue(ref.knotenId)]
        return knoten.anschlüsse.indexOfFirst { it.id == ref.anschlussId }
    }

    val knoten = karte.knoten.map { k ->
        buildString {
            append(k.art).append('|').append(k.name).append('|')
            append(k.position.x).append(',').append(k.position.y).append('|')
            append(k.größe.breite).append(',').append(k.größe.höhe).append('|')
            k.parameter.toSortedMap().forEach { (key, value) ->
                append(key).append('=').append(value).append(';')
            }
            append('|')
            k.anschlüsse.forEach { a ->
                append(a.name).append(':')
                    .append(a.richtung.name).append(':')
                    .append(a.kante.name).append(':')
                    .append(a.art.wert).append(':')
                    .append(a.reihenfolge).append(':')
                    .append(a.kannSichErweitern).append(';')
            }
            append("|ref=").append(k.kartenVerweis != null)
        }
    }

    val kanten = karte.verbindungen.map { v ->
        val vonKnoten = knotenIndex.getValue(v.von.knotenId)
        val zuKnoten = knotenIndex.getValue(v.zu.knotenId)
        "$vonKnoten:${anschlussIndex(v.von)}>$zuKnoten:${anschlussIndex(v.zu)}"
    }.sorted()

    return buildString {
        append(karte.name).append('\n')
        knoten.forEach { append(it).append('\n') }
        kanten.forEach { append(it).append('\n') }
    }
}

private fun JSONArray?.zuStringListe(): List<String> =
    if (this == null) emptyList() else List(length()) { index -> getString(index) }

package de.TeutonStudio.MathematikAtlas.speicher

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class KartenHerkunft(val titel: String) {
    EIGEN("Eigene Karten"),
    VORDEFINIERT("Vordefinierte Karten"),
    FREMD("Fremdnutzerkarten"),
}

internal data class ExterneKartenHerkunft(
    val fremdeKarten: Set<KartenId> = emptySet(),
    val fremdeOrdner: Set<List<String>> = emptySet(),
)

internal class KartenHerkunftSpeicher(
    private val datei: File,
) {
    constructor(context: Context) : this(
        File(File(context.filesDir, "MathematikAtlas"), "karten-herkunft.json"),
    )

    fun lade(): ExterneKartenHerkunft = runCatching {
        if (!datei.exists()) return@runCatching ExterneKartenHerkunft()
        val json = JSONObject(datei.readText())
        val karten = json.optJSONArray("fremdeKarten") ?: JSONArray()
        val ordner = json.optJSONArray("fremdeOrdner") ?: JSONArray()
        ExterneKartenHerkunft(
            fremdeKarten = List(karten.length()) { KartenId(karten.getString(it)) }.toSet(),
            fremdeOrdner = List(ordner.length()) { index ->
                val pfad = ordner.getJSONArray(index)
                List(pfad.length()) { pfad.getString(it) }
            }.filter(List<String>::isNotEmpty).toSet(),
        )
    }.getOrDefault(ExterneKartenHerkunft())

    fun markiereFremd(
        kartenIds: Set<KartenId>,
        ordner: Set<List<String>> = emptySet(),
    ) {
        val bisher = lade()
        speichere(
            bisher.copy(
                fremdeKarten = bisher.fremdeKarten + kartenIds,
                fremdeOrdner = bisher.fremdeOrdner + ordner.filter(List<String>::isNotEmpty),
            ),
        )
    }

    fun entferne(kartenIds: Set<KartenId>) {
        if (kartenIds.isEmpty()) return
        val bisher = lade()
        speichere(bisher.copy(fremdeKarten = bisher.fremdeKarten - kartenIds))
    }

    private fun speichere(herkunft: ExterneKartenHerkunft) {
        datei.parentFile?.mkdirs()
        val temporär = File(datei.parentFile, "${datei.name}.tmp")
        temporär.writeText(JSONObject().apply {
            put("formatVersion", 1)
            put("fremdeKarten", JSONArray().apply {
                herkunft.fremdeKarten.sortedBy(KartenId::wert).forEach { put(it.wert) }
            })
            put("fremdeOrdner", JSONArray().apply {
                herkunft.fremdeOrdner
                    .sortedWith(compareBy({ it.size }, { it.joinToString("\u0000") }))
                    .forEach { pfad -> put(JSONArray().apply { pfad.forEach(::put) }) }
            })
        }.toString(2))
        if (!temporär.renameTo(datei)) {
            temporär.copyTo(datei, overwrite = true)
            temporär.delete()
        }
    }
}

internal class KartenHerkunftAuflösung(context: Context) {
    private val standard = StandardKartenProvenienzSpeicher(context)
    private val freigaben = FreigabeQuellenSpeicher(context)
    private val externe = KartenHerkunftSpeicher(context)
    private val profil = LokalesProfilSpeicher(context)

    fun fürKarte(id: KartenId): KartenHerkunft {
        if (standard.liste().any { it.lokaleKartenId == id && it.status != StandardKartenStatus.GELÖSCHT }) {
            return KartenHerkunft.VORDEFINIERT
        }
        freigaben.liste().firstOrNull { it.lokaleKartenId == id }?.let { importiert ->
            return if (importiert.quelle.herausgeberId == profil.lade().id) {
                KartenHerkunft.EIGEN
            } else {
                KartenHerkunft.FREMD
            }
        }
        return if (id in externe.lade().fremdeKarten) KartenHerkunft.FREMD else KartenHerkunft.EIGEN
    }

    fun fremdVerwalteteOrdner(): Set<List<String>> = externe.lade().fremdeOrdner +
        standard.liste().flatMap { eintrag -> (1..eintrag.ordner.size).map(eintrag.ordner::take) }.toSet()
}

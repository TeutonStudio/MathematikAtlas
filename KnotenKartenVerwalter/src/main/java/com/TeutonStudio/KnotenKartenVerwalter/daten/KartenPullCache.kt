package com.TeutonStudio.KnotenKartenVerwalter.daten

data class KnotenCacheEintrag(
    val knotenId: String,
    val signatur: String,
    val daten: Map<String, String> = emptyMap(),
    val fehler: String? = null,
    val gueltig: Boolean = true,
)

data class KartenCacheDaten(
    val version: Int = 1,
    val knoten: Map<String, KnotenCacheEintrag> = emptyMap(),
) {
    fun eintrag(knotenId: String): KnotenCacheEintrag? = knoten[knotenId]
}

fun interface KnotenPullAuswertung {
    fun berechne(
        karte: KarteDaten,
        knoten: KnotenDaten,
        eingangsCaches: List<KnotenCacheEintrag>,
        signatur: String,
        vorherigerCache: KnotenCacheEintrag?,
    ): KnotenCacheEintrag
}

object StandardKnotenPullAuswertung : KnotenPullAuswertung {
    override fun berechne(
        karte: KarteDaten,
        knoten: KnotenDaten,
        eingangsCaches: List<KnotenCacheEintrag>,
        signatur: String,
        vorherigerCache: KnotenCacheEintrag?,
    ): KnotenCacheEintrag {
        if (vorherigerCache?.signatur == signatur) return vorherigerCache

        val daten = buildMap {
            put("name", knoten.name)
            put("art", knoten.art)
            knoten.data["wert"]?.toString()?.let { put("wert", it) }
            knoten.data["variable"]?.toString()?.let { put("variable", it) }
            knoten.data["operator"]?.toString()?.let { put("operator", it) }
            knoten.data["formel"]?.toString()?.let { put("formel", it) }
            knoten.data["kurzform"]?.toString()?.let { put("kurzform", it) }
            if (eingangsCaches.isNotEmpty()) {
                put("eingang", eingangsCaches.joinToString("|") { it.daten["wert"] ?: it.daten["kurzform"] ?: it.signatur })
            }
        }

        return KnotenCacheEintrag(
            knotenId = knoten.id,
            signatur = signatur,
            daten = daten,
            fehler = eingangsCaches.firstNotNullOfOrNull { it.fehler },
            gueltig = eingangsCaches.all { it.gueltig },
        )
    }
}

fun KarteDaten.mitAktualisiertemPullCache(
    auswertung: KnotenPullAuswertung = StandardKnotenPullAuswertung,
): KarteDaten {
    if (knoten.isEmpty()) return copy(cache = KartenCacheDaten())

    val knotenIds = knoten.mapTo(mutableSetOf()) { it.id }
    val ausgehendeVerbindungen = verbindungen
        .filter { it.quellKnotenId in knotenIds && it.zielKnotenId in knotenIds }
        .groupBy { it.quellKnotenId }
    val eingehendeVerbindungen = verbindungen
        .filter { it.quellKnotenId in knotenIds && it.zielKnotenId in knotenIds }
        .groupBy { it.zielKnotenId }
    val eingangsGrad = knotenIds.associateWith { eingehendeVerbindungen[it].orEmpty().size }.toMutableMap()
    val reihenfolge = mutableListOf<KnotenDaten>()
    val warteschlange = ArrayDeque(knoten.filter { eingangsGrad.getValue(it.id) == 0 }.map { it.id })
    val knotenNachId = knoten.associateBy { it.id }

    while (warteschlange.isNotEmpty()) {
        val knotenId = warteschlange.removeFirst()
        val aktuellerKnoten = knotenNachId[knotenId] ?: continue
        reihenfolge += aktuellerKnoten
        ausgehendeVerbindungen[knotenId].orEmpty().forEach { verbindung ->
            eingangsGrad[verbindung.zielKnotenId] = eingangsGrad.getValue(verbindung.zielKnotenId) - 1
            if (eingangsGrad.getValue(verbindung.zielKnotenId) == 0) {
                warteschlange.addLast(verbindung.zielKnotenId)
            }
        }
    }

    reihenfolge += knoten.filterNot { kandidat -> reihenfolge.any { it.id == kandidat.id } }

    val neuerCache = mutableMapOf<String, KnotenCacheEintrag>()
    reihenfolge.forEach { aktuellerKnoten ->
        val eingangsCaches = eingehendeVerbindungen[aktuellerKnoten.id].orEmpty()
            .mapNotNull { verbindung -> neuerCache[verbindung.quellKnotenId] ?: cache.eintrag(verbindung.quellKnotenId) }
        val signatur = aktuellerKnoten.pullSignatur(eingehendeVerbindungen[aktuellerKnoten.id].orEmpty(), eingangsCaches)
        val vorher = cache.eintrag(aktuellerKnoten.id)
        neuerCache[aktuellerKnoten.id] = if (vorher?.signatur == signatur) {
            vorher
        } else {
            auswertung.berechne(this, aktuellerKnoten, eingangsCaches, signatur, vorher)
        }
    }

    return copy(cache = KartenCacheDaten(knoten = neuerCache))
}

private fun KnotenDaten.pullSignatur(
    eingehendeVerbindungen: List<VerbindungDaten>,
    eingangsCaches: List<KnotenCacheEintrag>,
): String {
    val datenSignatur = data.entries
        .sortedBy { it.key }
        .joinToString(",") { "${it.key}=${it.value}" }
    val verbindungsSignatur = eingehendeVerbindungen
        .sortedWith(compareBy<VerbindungDaten> { it.zielAnschlussId }.thenBy { it.quellKnotenId }.thenBy { it.quellAnschlussId })
        .joinToString(",") { "${it.quellKnotenId}.${it.quellAnschlussId}->${it.zielAnschlussId}:${it.id}:${it.fehler.orEmpty()}" }
    val eingangsSignatur = eingangsCaches
        .sortedBy { it.knotenId }
        .joinToString(",") { "${it.knotenId}:${it.signatur}:${it.gueltig}:${it.fehler.orEmpty()}" }
    return listOf(name, art, datenSignatur, verbindungsSignatur, eingangsSignatur).joinToString("#")
}

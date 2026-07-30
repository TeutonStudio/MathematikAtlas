package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArt
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId

class AnschlussArtRegister(arten: Iterable<AnschlussArt> = emptyList()) {
    private val nachId = linkedMapOf<AnschlussArtId, AnschlussArt>()

    init { arten.forEach(::registriere) }

    fun registriere(art: AnschlussArt) { nachId[art.id] = art }
    fun finde(id: AnschlussArtId): AnschlussArt? = nachId[id]
    fun alle(): List<AnschlussArt> = nachId.values.toList()

    fun istUnterart(von: AnschlussArtId, erwartet: AnschlussArtId): Boolean {
        var aktuell: AnschlussArtId? = von
        val besucht = mutableSetOf<AnschlussArtId>()
        while (aktuell != null && besucht.add(aktuell)) {
            if (aktuell == erwartet) return true
            aktuell = nachId[aktuell]?.elternArt
        }
        return false
    }

    /** Liefert die spezifischste Art, die Oberart aller angegebenen Arten ist. */
    fun gemeinsameOberart(arten: Iterable<AnschlussArtId>): AnschlussArtId? {
        val eindeutig = arten.distinct()
        if (eindeutig.isEmpty()) return null

        val kandidaten = mutableListOf<AnschlussArtId>()
        var aktuell: AnschlussArtId? = eindeutig.first()
        val besucht = mutableSetOf<AnschlussArtId>()
        while (aktuell != null && besucht.add(aktuell)) {
            kandidaten += aktuell
            aktuell = nachId[aktuell]?.elternArt
        }
        return kandidaten.firstOrNull { kandidat -> eindeutig.all { art -> istUnterart(art, kandidat) } }
    }
}

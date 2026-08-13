package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/**
 * Öffentliche Kartenanschlüsse übernehmen den bereits bekannten semantischen Wertvertrag
 * ihrer internen Schnittstellenkomponente. Interne Typinferenzregeln werden dagegen nicht
 * nach außen kopiert, weil deren Anschlussnamen nur innerhalb der referenzierten Karte gelten.
 */
internal fun öffentlicheKartenAnschlüsseMitVertrag(
    karte: KartenDaten,
    interneArt: String,
    richtung: AnschlussRichtung,
    kante: AnschlussKante,
): List<AnschlussDaten> {
    val basis = öffentlicheKartenAnschlüsse(karte, interneArt, richtung, kante)
    val quelleNachName = linkedMapOf<String, AnschlussDaten>()
    karte.knoten.asSequence()
        .filter { it.art == interneArt }
        .forEach { intern ->
            val name = öffentlicherKartenName(intern)
            val wert = intern.anschlüsse.firstOrNull { it.name == "wert" } ?: return@forEach
            quelleNachName.putIfAbsent(name, wert)
        }

    return basis.map { öffentlich ->
        val intern = quelleNachName[öffentlich.name] ?: return@map öffentlich
        öffentlich.copy(vertrag = intern.vertrag)
    }
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/**
 * Ein gemeinsamer ID-Vertrag für alle Rechnerfamilien. Nur dieselbe semantische
 * Rolle oder ausdrücklich deklarierte historische Namen dürfen eine ID übernehmen.
 * Die Position ist weder Rolle noch Migration und wird deshalb nie verwendet.
 */
internal fun erhalteRechnerAnschlussIds(
    bisher: List<AnschlussDaten>,
    gewünscht: List<AnschlussDaten>,
    aliases: (AnschlussDaten) -> Set<String> = { emptySet() },
): List<AnschlussDaten> {
    val verbraucht = mutableSetOf<AnschlussId>()
    return gewünscht.map { soll ->
        val namen = listOf(soll.name) + aliases(soll)
        val kandidat = namen.firstNotNullOfOrNull { name -> bisher.firstOrNull { alt ->
            alt.id !in verbraucht && alt.richtung == soll.richtung && alt.name == name &&
                kompatibleRechnerArten(alt, soll)
        } }
        if (kandidat != null) verbraucht += kandidat.id
        soll.copy(id = kandidat?.id ?: neueAnschlussId())
    }
}

private fun kompatibleRechnerArten(a: AnschlussDaten, b: AnschlussDaten): Boolean {
    if (a.art == b.art) return true
    fun konkreteArten(p: AnschlussDaten): Set<AnschlussArtId> = p.zulässigeArten.ifEmpty { setOf(p.art) }
    val offen = setOf(MathematikAnschlussArten.Objekt.id, MathematikAnschlussArten.AtlasWert.id)
    if ((a.art in offen && a.zulässigeArten.isEmpty()) || (b.art in offen && b.zulässigeArten.isEmpty())) return true
    return konkreteArten(a).intersect(konkreteArten(b)).isNotEmpty()
}

/** Aliasauflösung aus genau der ausgewählten kanonischen Signatur. */
internal fun erhalteRechnerAnschlussIds(
    bisher: List<AnschlussDaten>,
    gewünscht: List<AnschlussDaten>,
    familie: RechnerFamilie,
    operatorId: String,
    signaturId: String = "standard",
): List<AnschlussDaten> {
    val vertrag = (RechnerOperatorRegistry.finde(familie, operatorId) as? RechnerOperatorAuflösung.Bekannt)?.vertrag
    val rollen = vertrag?.signaturen?.singleOrNull { it.id == signaturId }?.anschlüsse.orEmpty()
    return erhalteRechnerAnschlussIds(bisher, gewünscht) { port ->
        rollen.singleOrNull { it.name == port.name && it.richtung == port.richtung }?.aliases.orEmpty()
    }
}

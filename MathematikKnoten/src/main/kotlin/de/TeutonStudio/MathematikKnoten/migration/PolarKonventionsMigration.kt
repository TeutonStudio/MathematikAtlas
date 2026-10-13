package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.PolarKonvention

/** Explizite historische Semantik; die Auswertung errät keine Konvention aus Laufzeitwerten. */
fun KartenDaten.migrierePolarKonventionen():KartenDaten {
    val nachId=knoten.associateBy { it.id }
    fun quelle(k:KnotenDaten,port:AnschlussDaten):KnotenDaten? = verbindungen.firstOrNull { it.zu.knotenId==k.id && it.zu.anschlussId==port.id }?.von?.knotenId?.let(nachId::get)
    fun richtungsQuelle(k:KnotenDaten,besucht:Set<KnotenId> = emptySet()):Boolean {
        if(k.id in besucht) return false
        if(k.art=="mathematik.variable" && variablenStruktur(k)==VARIABLE_STRUKTUR_WINKEL) return true
        if(k.art==MathematikKnotenVorlagen.Tupel.art) {
            val ports=k.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
            return ports.lastOrNull()?.let { quelle(k,it) }?.let { richtungsQuelle(it,besucht+k.id) }==true
        }
        return false
    }
    return copy(knoten=knoten.map { k ->
        if(k.art!=ÜBERSETZER_ART) k else {
            var parameter=k.parameter
            if(ÜBERSETZER_POLAR_KONVENTION !in parameter) {
                val port=k.anschlüsse.firstOrNull { it.richtung==AnschlussRichtung.Eingang && it.name=="winkel" }
                    ?: k.anschlüsse.firstOrNull { it.richtung==AnschlussRichtung.Eingang && it.name=="wert" }
                val signiert=port?.let { quelle(k,it) }?.let { richtungsQuelle(it) }==true
                parameter=parameter+(ÜBERSETZER_POLAR_KONVENTION to if(signiert) PolarKonvention.SIGNED.name else PolarKonvention.CLASSIC.name)
            }
            if("methode.tupelEinzelausgabe" !in parameter) parameter=parameter+("methode.tupelEinzelausgabe" to "false")
            k.copy(parameter=parameter)
        }
    })
}

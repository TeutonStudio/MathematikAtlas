package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val DEFINITIONSMENGE_DOPPELPUNKT_DARSTELLUNG = "@mathematik.definitionsmenge.doppelpunkt"

data class BedingterWert(
    val objekt: MathematischesObjekt,
    val annahmen: Set<Aussage> = emptySet(),
    /** Metadaten einer öffentlichen Methodenausgabe, kein zweiter Rückgabewert. */
    val zielMenge: MengenAusdruck? = null,
    /** Definitionsmenge einer Variable; relevant beim Aufbau einer Methode. */
    val werteVorrat: MengenAusdruck? = null,
    /** Laufzeitmetadaten für Variablen, deren Wertebereich nachweisbar reell ist. */
    val reelleVariablen: Map<String, MengenAusdruck> = emptyMap(),
    /** Nichtpersistierte Herkunft der freien Variablen eines aus dem Graphen abgeleiteten Werts. */
    val variablenQuellen: List<VariablenQuelle> = emptyList(),
    /** Pfadgebundene Darstellung; verändert das mathematische Objekt ausdrücklich nicht. */
    val latexDarstellung: String? = null,
)

/** Verwendet eine gesetzte Darstellungsoptimierung, andernfalls die mathematische Standarddarstellung. */
fun BedingterWert.anzeigeLatex(): String = when {
    latexDarstellung == DEFINITIONSMENGE_DOPPELPUNKT_DARSTELLUNG && objekt is DefinierteMenge ->
        objekt.zuDoppelpunktLatex()
    else -> latexDarstellung?.takeIf { it.isNotBlank() } ?: objekt.zuLatex()
}

data class VariablenQuelle(
    val knotenId: KnotenId,
    val name: String,
    val werteVorrat: MengenAusdruck,
    /** Nur echte Parameterknoten werden in die Signatur einer mit „Term zu Methode“ erzeugten Methode übernommen. */
    val alsMethodenParameter: Boolean = true,
)

/** Konservativer Laufzeitnachweis für die Zulässigkeit reeller Zahloperationen. */
fun BedingterWert.istNachweisbarReell(): Boolean = (objekt as? ZahlAusdruck)?.let { ausdruck ->
    istNachweisbarReell(ausdruck, { variable ->
        val vorrat = reelleVariablen[variable.name] ?: if (ausdruck == variable) werteVorrat else null
        vorrat in setOf(NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen)
    }, annahmen)
} ?: false

fun reelleVariablen(werte: Iterable<BedingterWert>): Map<String, MengenAusdruck> = buildMap {
    werte.forEach { wert ->
        putAll(wert.reelleVariablen)
        (wert.objekt as? Variable)?.let { variable -> wert.werteVorrat?.let { put(variable.name, it) } }
    }
}

data class KnotenAuswertungsErgebnis(
    val ausgaben: Map<String, BedingterWert>,
    val schritte: List<UmformungsSchritt> = emptyList(),
    val fehler: String? = null,
    /** Die beim Auswerten tatsächlich verwendeten Eingabewerte, auch für die Knotendarstellung. */
    val eingänge: Map<String, BedingterWert> = emptyMap(),
)

data class KartenAuswertungsErgebnis(
    val knoten: Map<KnotenId, KnotenAuswertungsErgebnis>,
    val fehler: List<String>,
)

data class KnotenAuswertungsKontext(
    val knoten: KnotenDaten,
    val eingänge: Map<String, BedingterWert>,
    val rechenKontext: RechenKontext,
    /** Deterministische Kahn-Reihenfolge der aktuellen Karte für abgeleitete Argumentlisten. */
    val topologischeReihenfolge: Map<KnotenId, Int> = emptyMap(),
)

fun interface MathematikKnotenAuswerter {
    fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis
}

class MathematikAuswerterRegister {
    private val auswerter = linkedMapOf<KnotenArtId, MathematikKnotenAuswerter>()
    fun registriere(art: KnotenArtId, wert: MathematikKnotenAuswerter) { auswerter[art] = wert }
    fun finde(art: KnotenArtId): MathematikKnotenAuswerter? = when (art) {
        "mathematik.reellesIntervall" -> ReellesIntervallAuswerter
        else -> auswerter[art]
    }
    fun arten(): Set<KnotenArtId> = auswerter.keys
}

fun interface KartenQuelle {
    fun lade(verweis: KartenVerweis): KartenDaten?
}

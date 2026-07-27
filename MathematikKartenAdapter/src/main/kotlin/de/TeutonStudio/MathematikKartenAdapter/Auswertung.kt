package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

data class BedingterWert(
    val objekt: MathematischesObjekt,
    val annahmen: Set<Aussage> = emptySet(),
)

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
)

fun interface MathematikKnotenAuswerter {
    fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis
}

class MathematikAuswerterRegister {
    private val auswerter = linkedMapOf<KnotenArtId, MathematikKnotenAuswerter>()
    fun registriere(art: KnotenArtId, wert: MathematikKnotenAuswerter) { auswerter[art] = wert }
    fun finde(art: KnotenArtId): MathematikKnotenAuswerter? = auswerter[art]
    fun arten(): Set<KnotenArtId> = auswerter.keys
}

fun interface KartenQuelle {
    fun lade(verweis: KartenVerweis): KartenDaten?
}

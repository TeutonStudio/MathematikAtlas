package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val DEFINITIONSMENGE_DOPPELPUNKT_DARSTELLUNG = "@mathematik.definitionsmenge.doppelpunkt"

data class ArgumentIdentität(
    val quelle: String,
    val rolle: String,
)

enum class ArgumentQuellenArt { Wert, Aussage }

data class BedingterWert(
    val objekt: MathematischesObjekt,
    val annahmen: Set<Aussage> = emptySet(),
    /** Metadaten einer öffentlichen Methodenausgabe, kein zweiter Rückgabewert. */
    val zielMenge: MengenAusdruck? = null,
    /** Definitionsmenge einer Variable; relevant beim Aufbau einer Methode. */
    val werteVorrat: MengenAusdruck? = null,
    /** Laufzeitmetadaten für Variablen, deren Wertebereich nachweisbar reell ist. */
    val reelleVariablen: Map<String, MengenAusdruck> = emptyMap(),
    /**
     * Nichtpersistierte Herkunft aller freien Methodenargumente.
     *
     * Der historische Name bleibt aus Quellkompatibilitätsgründen bestehen. Die
     * Einträge unterscheiden nun gewöhnliche Werte- und Aussageargumente und
     * besitzen eine stabile semantische Identität.
     */
    val variablenQuellen: List<VariablenQuelle> = emptyList(),
    /** Pfadgebundene Darstellung; verändert das mathematische Objekt ausdrücklich nicht. */
    val latexDarstellung: String? = null,
    /** Gemeinsame Anschlussart der Elemente einer mengenwertigen Ausgabe. */
    val elementArt: AnschlussArtId? = null,
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
    /** Semantische Bindung eines gekoppelten Konstruktor-/Definator-Paares. */
    val bindungsId: String? = null,
    /** Sichtbarer Name oder Rolle des durch die Bindung erzeugten Objekts. */
    val bindungsName: String? = null,
    /** Anschlussart des gebundenen Elements; relevant für nichtnumerische Elemente. */
    val gebundeneArt: AnschlussArtId? = null,
    /** Nichtpersistierter Ausgangswert einer Bindung, etwa Indexmenge oder neutrales Element. */
    val bindungsWert: MathematischesObjekt? = null,
    /** Ordnung des ersten Auftretens in der Methodensignatur. */
    val reihenfolge: Int = 0,
    /** Aussageargumente erscheinen in gemischten Prädikatssignaturen als Einermengen. */
    val argumentArt: ArgumentQuellenArt = ArgumentQuellenArt.Wert,
    /** Vollständige symbolische Aussage eines Aussagearguments. */
    val aussage: Aussage? = null,
) {
    val identität: ArgumentIdentität = ArgumentIdentität(
        quelle = bindungsId ?: knotenId.wert,
        rolle = bindungsName ?: name,
    )
}

/** Stabile Deduplizierung beim ersten Auftreten unter Erhalt der fachlichen Reihenfolge. */
fun Iterable<VariablenQuelle>.geordnetEindeutig(): List<VariablenQuelle> = buildList {
    val gesehen = mutableSetOf<ArgumentIdentität>()
    this@geordnetEindeutig
        .sortedWith(compareBy<VariablenQuelle>({ it.reihenfolge }, { it.identität.quelle }, { it.identität.rolle }))
        .forEach { quelle -> if (gesehen.add(quelle.identität)) add(quelle) }
}

/** Wandelt Adapterquellen in das Android-freie Prädikatsmodell des Rechenkerns um. */
fun BedingterWert.prädikatsArgumente(): List<PrädikatsArgument> = variablenQuellen
    .geordnetEindeutig()
    .filter { it.alsMethodenParameter }
    .map { quelle ->
        when (quelle.argumentArt) {
            ArgumentQuellenArt.Aussage -> PrädikatsArgument.AussageWert(
                name = quelle.name,
                latex = quelle.aussage?.zuLatex() ?: quelle.name,
                identität = "${quelle.identität.quelle}:${quelle.identität.rolle}",
            )
            ArgumentQuellenArt.Wert -> PrädikatsArgument.Wert(
                name = quelle.name,
                werteVorrat = quelle.werteVorrat,
                identität = "${quelle.identität.quelle}:${quelle.identität.rolle}",
            )
        }
    }

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
    /** Feldbezogene Konfigurationsfehler, indiziert durch stabile Element-IDs. */
    val elementFehler: Map<String, String> = emptyMap(),
    /** Nichtpersistierte Hinweise, etwa über zusammengeführte Mengenelemente. */
    val warnungen: List<String> = emptyList(),
)

data class KartenAuswertungsErgebnis(
    val knoten: Map<KnotenId, KnotenAuswertungsErgebnis>,
    private val basisFehler: List<String>,
) {
    /**
     * Defensiver Kartenfehler für doppelte offene Prädikatsnamen.
     *
     * Identische Methodenobjekte, die lediglich durch weitere Knoten gereicht
     * werden, zählen einmal. Verschiedene Definitionen mit demselben Namen
     * werden nicht automatisch umbenannt.
     */
    val fehler: List<String> = basisFehler + knoten.values
        .flatMap { it.ausgaben.values }
        .mapNotNull { it.objekt as? Methode }
        .filter { it.istOffenesPrädikat() }
        .eindeutigNachIdentität()
        .groupBy { it.name.trim() }
        .filter { (name, methoden) -> name.isNotEmpty() && methoden.size > 1 }
        .keys
        .sorted()
        .map { name -> "Der Prädikatsname '$name' wird innerhalb der Karte mehrfach definiert." }
}


private fun Iterable<Methode>.eindeutigNachIdentität(): List<Methode> {
    val gesehen = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<Methode, Boolean>(),
    )
    return filter(gesehen::add)
}

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
        KARTEN_EINGANG_ART -> KartenEingangAuswerter
        "mathematik.reellesIntervall" -> ReellesIntervallAuswerter
        MENGENKONSTRUKTOR_ART -> MengenkonstruktorAuswerter
        MENGENDEFINATOR_ART -> MengendefinatorAuswerter
        FALTUNGSKONSTRUKTOR_ART -> FaltungskonstruktorAuswerter
        FALTUNGSDEFINATOR_ART -> FaltungsdefinatorAuswerter
        METHODEN_ANWENDUNG_ART -> MethodenAnwendungAuswerter
        METHODEN_ZIELMENGE_ART -> MethodenZielmengeAuswerter
        else -> auswerter[art]
    }
    fun arten(): Set<KnotenArtId> = auswerter.keys + setOf(
        KARTEN_EINGANG_ART,
        MENGENKONSTRUKTOR_ART,
        MENGENDEFINATOR_ART,
        FALTUNGSKONSTRUKTOR_ART,
        FALTUNGSDEFINATOR_ART,
        METHODEN_ANWENDUNG_ART,
        METHODEN_ZIELMENGE_ART,
    )
}

fun interface KartenQuelle {
    fun lade(verweis: KartenVerweis): KartenDaten?
}

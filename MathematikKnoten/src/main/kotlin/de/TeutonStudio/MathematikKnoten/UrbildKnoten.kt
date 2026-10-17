package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswertungsErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.AnschlussVertrag
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypPrüfung

const val URBILD_EINGABEFORM = "urbild.eingabeform"
const val URBILD_EINZELWERT = "einzelwert"
const val URBILD_ZIELMENGE = "zielmenge"
const val URBILD_VERTRAGSFEHLER = "urbild.vertragsfehler"

fun istUrbildKnoten(knoten: KnotenDaten): Boolean = knoten.art == MengenRechner.KNOTEN_ART &&
    MengenRechnerOperator.vonIdOderNull(knoten.parameter[MENGENRECHNER_OPERATOR_PARAMETER]) == MengenRechnerOperator.URBILD

fun urbildEingabeform(knoten: KnotenDaten): String = knoten.parameter[URBILD_EINGABEFORM] ?: URBILD_EINZELWERT

fun knotenUrbildDiagnose(knoten: KnotenDaten): String? =
    knoten.parameter[URBILD_VERTRAGSFEHLER] ?: if (urbildEingabeform(knoten) !in setOf(URBILD_EINZELWERT, URBILD_ZIELMENGE))
        "Unbekannte Urbild-Eingabeform: ${urbildEingabeform(knoten)}" else null

fun konfiguriereUrbildEingabeform(knoten: KnotenDaten, eingabeform: String): KnotenDaten {
    require(istUrbildKnoten(knoten))
    require(eingabeform in setOf(URBILD_EINZELWERT, URBILD_ZIELMENGE))
    val vorher = knoten.anschlüsse.singleOrNull { it.name == "ziel" && it.richtung == AnschlussRichtung.Eingang }
    val zielArt = if (eingabeform == URBILD_ZIELMENGE) MathematikAnschlussArten.Menge.id
        else if (urbildEingabeform(knoten) == URBILD_EINZELWERT) vorher?.art ?: MathematikAnschlussArten.Zahl.id
        else MathematikAnschlussArten.Zahl.id
    val basis = knoten.copy(parameter = (knoten.parameter - URBILD_VERTRAGSFEHLER) + (URBILD_EINGABEFORM to eingabeform))
    return basis.copy(anschlüsse = urbildAnschluesse(basis).map { port ->
        if (port.name == "ziel" && port.richtung == AnschlussRichtung.Eingang)
            port.copy(art = zielArt, vertrag = if (eingabeform == urbildEingabeform(knoten)) port.vertrag else AnschlussVertrag()) else port
    })
}

internal fun urbildAnschluesse(knoten: KnotenDaten?): List<AnschlussDaten> {
    fun port(name: String, richtung: AnschlussRichtung, art: AnschlussArtId, reihe: Int) =
        AnschlussDaten(name = name, richtung = richtung,
            kante = if (richtung == AnschlussRichtung.Eingang) AnschlussKante.Links else AnschlussKante.Rechts,
            art = art, reihenfolge = reihe).let { neu ->
            knoten?.anschlüsse?.singleOrNull { it.name == name && it.richtung == richtung }?.let { alt ->
                neu.copy(id = alt.id, semantischeId = alt.semantischeId, vertrag = alt.vertrag,
                    art = if (name == "ziel") alt.art else neu.art)
            } ?: neu
        }
    return listOf(
        port("methode", AnschlussRichtung.Eingang, MathematikAnschlussArten.Methode.id, 0),
        port("ziel", AnschlussRichtung.Eingang,
            if (knoten != null && urbildEingabeform(knoten) == URBILD_ZIELMENGE) MathematikAnschlussArten.Menge.id
            else MathematikAnschlussArten.Zahl.id, 1),
        port("menge", AnschlussRichtung.Ausgang, MathematikAnschlussArten.Menge.id, 0),
    )
}

/** Gleicher Typableitungspfad für beide Plattformen und die bestätigbare Konfigurationsvorschau. */
fun synchronisiereUrbildAnschluesse(
    karte: KartenDaten,
    auswertung: KartenAuswertungsErgebnis,
    prüfung: GraphPrüfung,
    verlustVorschau: Boolean = false,
): KartenDaten {
    var neu = karte
    for (knoten in karte.knoten.filter(::istUrbildKnoten)) {
        if (RECHNER_MIGRATION_DIAGNOSE in knoten.parameter) continue
        val methodenPort = knoten.anschlüsse.singleOrNull { it.name == "methode" && it.richtung == AnschlussRichtung.Eingang } ?: continue
        val quelle = karte.verbindungen.singleOrNull { it.zu == AnschlussVerweis(knoten.id, methodenPort.id) }?.von
        val quellPort = quelle?.let { q -> karte.knoten.singleOrNull { it.id == q.knotenId }?.anschlüsse?.singleOrNull { it.id == q.anschlussId } }
        val methode = if (quelle != null) auswertung.knoten[quelle.knotenId]?.ausgaben?.get(quellPort?.name)?.objekt as? Methode
            else auswertung.knoten[knoten.id]?.eingänge?.get("methode")?.objekt as? Methode
        val signatur = (methode as? SignaturtragendeMethode)?.signatur
        val ergebnisTyp = signatur?.let { urbildAnschlussTyp(it.ergebnisse.singleOrNull()?.typ ?: it.ergebnisTyp) }
        val mengenModus = urbildEingabeform(knoten) == URBILD_ZIELMENGE
        val typ = if (mengenModus) ergebnisTyp?.let { TypAusdruck.Parameterisiert(MathematischeTypen.Menge, listOf(it)) }
            ?: TypAusdruck.Unbekannt else ergebnisTyp ?: TypAusdruck.Unbekannt
        val art = if (mengenModus) MathematikAnschlussArten.Menge.id
            else if (methode == null && quelle == null) MathematikAnschlussArten.Zahl.id
            else ergebnisTyp?.let(::variablenAnschlussArt) ?: MathematikAnschlussArten.Objekt.id
        val alt = knoten.anschlüsse.singleOrNull { it.name == "ziel" && it.richtung == AnschlussRichtung.Eingang } ?: continue
        val ziel = alt.copy(art = art, vertrag = AnschlussVertrag(typ), zulässigeArten = emptySet())
        val lokalInkompatibel = alt.art != ziel.art ||
            alt.vertrag.typ != TypAusdruck.Unbekannt && typ != TypAusdruck.Unbekannt &&
                (MathematischeTypen.typSystem.prüfe(alt.vertrag.typ, typ) is TypPrüfung.Inkompatibel)
        fun lokal(key: String) = key == "standardwert.ziel" || key.startsWith("standardwert.ziel.")
        fun fallback(key: String) = key == "ziel" || key.startsWith("ziel@karte:")
        val lokale = knoten.parameter.keys.any(::lokal) || knoten.eingangsKartenVerweise.keys.any(::fallback)
        var kandidat = knoten.copy(anschlüsse = knoten.anschlüsse.map { if (it.id == alt.id) ziel else it },
            parameter = knoten.parameter - URBILD_VERTRAGSFEHLER)
        val probe = neu.copy(knoten = neu.knoten.map { if (it.id == knoten.id) kandidat else it })
        val inkompatibel = prüfung.inkompatibleVerbindungen(probe).filter { it.zu == AnschlussVerweis(knoten.id, ziel.id) }
        if (verlustVorschau) {
            if (lokalInkompatibel && lokale) kandidat = kandidat.copy(parameter = kandidat.parameter.filterKeys { !lokal(it) },
                eingangsKartenVerweise = kandidat.eingangsKartenVerweise.filterKeys { !fallback(it) })
            neu = neu.copy(verbindungen = neu.verbindungen - inkompatibel.toSet())
        } else if (lokalInkompatibel && lokale || inkompatibel.isNotEmpty()) {
            // Implizite Laufzeitsynchronisierung darf gespeicherte Quellen nicht löschen.
            if (lokalInkompatibel && lokale) kandidat = knoten
            kandidat = kandidat.copy(parameter = kandidat.parameter +
                (URBILD_VERTRAGSFEHLER to "Der Methodenergebnistyp hat sich geändert. Zielanschluss über die Eingabeform neu konfigurieren; betroffene Quellen werden vorab angezeigt."))
        }
        if (methode != null && (signatur == null || ergebnisTyp == TypAusdruck.Unbekannt) || quelle != null && methode == null) kandidat = kandidat.copy(
            parameter = kandidat.parameter + (URBILD_VERTRAGSFEHLER to "Die verbundene Methode besitzt keine verfügbare Ergebnissignatur."))
        neu = neu.copy(knoten = neu.knoten.map { if (it.id == knoten.id) kandidat else it })
    }
    return neu
}

/** Der ausgeblendete historische Platzhalter hatte genau den Mengeneingang a. */
fun KartenDaten.migriereUrbildKnoten(): KartenDaten = copy(knoten = knoten.map { alt ->
    if (!istUrbildKnoten(alt) || RECHNER_MIGRATION_DIAGNOSE in alt.parameter || URBILD_EINGABEFORM in alt.parameter) return@map alt
    val eingänge = alt.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
    val a = eingänge.singleOrNull()
    val ausgänge = alt.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }
    val rollenkollision = alt.parameter.keys.any { it == "standardwert.ziel" || it.startsWith("standardwert.ziel.") } ||
        alt.eingangsKartenVerweise.keys.any { it == "ziel" || it.startsWith("ziel@karte:") }
    if (rollenkollision || ausgänge.size != 1 || ausgänge.single().name != "menge" ||
        ausgänge.single().art != MathematikAnschlussArten.Menge.id || a == null || a.name != "a" || a.art != MathematikAnschlussArten.Menge.id &&
        !(a.art == MathematikAnschlussArten.AtlasWert.id && a.zulässigeArten == setOf(MathematikAnschlussArten.Menge.id, MathematikAnschlussArten.Methode.id)))
        return@map alt.copy(parameter = alt.parameter + (RECHNER_MIGRATION_DIAGNOSE to "Historische Urbildanschlüsse sind nicht eindeutig zuordenbar."))
    val basis = alt.copy(parameter = alt.parameter.mapKeys { (key, _) ->
        if (key == "standardwert.a" || key.startsWith("standardwert.a.")) key.replaceFirst("standardwert.a", "standardwert.ziel") else key
    } + (URBILD_EINGABEFORM to URBILD_ZIELMENGE), eingangsKartenVerweise = alt.eingangsKartenVerweise.mapKeys { (key, _) ->
        if (key == "a" || key.startsWith("a@karte:")) key.replaceFirst("a", "ziel") else key
    }, anschlüsse = alt.anschlüsse.map { if (it.id == a.id) it.copy(name = "ziel", art = MathematikAnschlussArten.Menge.id, zulässigeArten = emptySet()) else it })
    basis.copy(anschlüsse = urbildAnschluesse(basis))
})

/** Zahlenquellen tragen den gemeinsamen Zahlvertrag; Zahlbereiche prüft der Kern. */
private fun urbildAnschlussTyp(typ: TypAusdruck): TypAusdruck = when (typ) {
    is TypAusdruck.Atom -> if (MathematischeTypen.istAtomUntertyp(typ.id, MathematischeTypen.Zahl))
        TypAusdruck.Atom(MathematischeTypen.Zahl) else typ
    is TypAusdruck.Parameterisiert -> typ.copy(argumente = typ.argumente.map(::urbildAnschlussTyp))
    is TypAusdruck.Vereinigung -> typ.copy(alternativen = typ.alternativen.map(::urbildAnschlussTyp).distinct())
    else -> typ
}

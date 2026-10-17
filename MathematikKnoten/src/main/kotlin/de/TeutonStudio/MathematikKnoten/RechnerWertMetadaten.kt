package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Bereiche gehören zu Quellenidentitäten; sichtbare Namen sind nur eindeutige Legacy-Aliase. */
internal fun Iterable<BedingterWert>.rechnerQuellBereiche(): Map<String, MengenAusdruck> {
    val werte = toList()
    val quellen = werte.flatMap { it.variablenQuellen }.distinctBy { it.identität }
    val identitäten = quellen.map { it.name to (it.bindungsId ?: it.knotenId.wert) } +
        werte.mapNotNull { (it.objekt as? MethodenParameter)?.let { p -> p.name to (p.bindungsId ?: p.name) } }
    val mehrdeutigeNamen = identitäten.groupBy({ it.first }, { it.second })
        .filterValues { it.distinct().size > 1 }.keys
    return buildMap {
        werte.forEach { wert -> putAll(wert.reelleVariablen.filterKeys { it !in mehrdeutigeNamen }) }
        quellen.forEach { quelle ->
            put(quelle.bindungsId ?: quelle.knotenId.wert, quelle.werteVorrat)
            if (quelle.name !in mehrdeutigeNamen) put(quelle.name, quelle.werteVorrat)
        }
        werte.forEach { wert ->
            (wert.objekt as? MethodenParameter)?.let { p -> (wert.werteVorrat ?: wert.zielMenge)?.let { menge ->
                put(p.bindungsId ?: p.name, menge)
                if (p.name !in mehrdeutigeNamen) put(p.name, menge)
            } }
        }
    }
}

/** Eine Berechnung verändert den Wert; gültige Eingangszeugnisse bleiben als Herkunft erhalten. */
internal fun KnotenAuswertungsKontext.rechnerErgebnisWert(objekt: AtlasWert, bedingungen: Set<Aussage> = emptySet(),
    zielMenge: MengenAusdruck? = null, übersetzungen: ÜbersetzungsMetadaten = ÜbersetzungsMetadaten()): BedingterWert = BedingterWert(
    objekt = objekt,
    annahmen = eingänge.values.flatMap { it.annahmen }.toSet() + bedingungen,
    zielMenge = zielMenge,
    reelleVariablen = eingänge.values.rechnerQuellBereiche().filterValues { nachgewieseneZahlenGrundmenge(it)?.fundamentalerZahlbereichOderNull()?.let { b -> b <= FundamentalerZahlbereich.REELL } == true },
    variablenQuellen = eingänge.values.flatMap { it.variablenQuellen }.geordnetEindeutig(),
    übersetzungen = übersetzungen.copy(herkunft = (übersetzungen.herkunft + eingänge.values
        .flatMap { it.übersetzungen.alsHerkunft().herkunft }).distinct()),
)

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikKartenAdapter.methodenErgebnisArtSchlüssel

private val ALTE_METHODEN_ANSCHLUSSARTEN = setOf(
    "mathematik.funktion",
    "mathematik.funktion.zahl",
    "mathematik.funktion.aussage",
    "mathematik.funktion.menge",
    "mathematik.funktion.vektor.spalte",
    "mathematik.funktion.vektor.zeile",
)

private fun alteMethodenErgebnisArt(art: String): String? = when (art) {
    "mathematik.funktion.zahl" -> "mathematik.zahl"
    "mathematik.funktion.aussage" -> "mathematik.aussage"
    "mathematik.funktion.menge" -> "mathematik.menge"
    "mathematik.funktion.vektor.spalte" -> "mathematik.vektor.spalte"
    "mathematik.funktion.vektor.zeile" -> "mathematik.vektor.zeile"
    else -> null
}

/**
 * Verlustfreie Lade-Migration auf den einzigen Methodenanschluss.
 *
 * Knoten-, Anschluss- und Verbindungs-IDs bleiben unverändert. Die ehemalige
 * Methodenunterart wird als semantischer Ergebnisvertrag am jeweiligen
 * Anschlussnamen bewahrt. Die Methode ist idempotent.
 */
internal fun KartenDaten.migriereMethodenAnschlüsse(): KartenDaten {
    var verändert = false
    val neueKnoten = knoten.map { knoten ->
        var parameter = knoten.parameter
        val neueAnschlüsse = knoten.anschlüsse.map { anschluss ->
            val alteArt = anschluss.art.wert
            if (alteArt !in ALTE_METHODEN_ANSCHLUSSARTEN) anschluss
            else {
                verändert = true
                alteMethodenErgebnisArt(alteArt)?.let { ergebnisArt ->
                    val schlüssel = methodenErgebnisArtSchlüssel(anschluss.name)
                    if (parameter[schlüssel] != ergebnisArt) {
                        parameter = parameter + (schlüssel to ergebnisArt)
                    }
                }
                anschluss.copy(art = AnschlussArtId("mathematik.methode"))
            }
        }
        if (neueAnschlüsse == knoten.anschlüsse && parameter == knoten.parameter) knoten
        else knoten.copy(anschlüsse = neueAnschlüsse, parameter = parameter)
    }
    return if (verändert || neueKnoten != knoten) copy(knoten = neueKnoten) else this
}

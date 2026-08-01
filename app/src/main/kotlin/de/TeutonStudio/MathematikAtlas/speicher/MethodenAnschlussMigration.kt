package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten

private val ALTE_METHODEN_ANSCHLUSSARTEN = setOf(
    "mathematik.funktion",
    "mathematik.funktion.zahl",
    "mathematik.funktion.aussage",
    "mathematik.funktion.menge",
    "mathematik.funktion.vektor.spalte",
    "mathematik.funktion.vektor.zeile",
)

/**
 * Verlustfreie Lade-Migration auf den einzigen Methodenanschluss.
 *
 * Knoten-, Anschluss- und Verbindungs-IDs bleiben unverändert. Dadurch bleiben
 * sämtliche bestehenden Verbindungen gültig; nur die fachliche Anschlussart
 * wird normalisiert. Die Funktion ist idempotent.
 */
internal fun KartenDaten.migriereMethodenAnschlüsse(): KartenDaten {
    var verändert = false
    val neueKnoten = knoten.map { knoten ->
        val neueAnschlüsse = knoten.anschlüsse.map { anschluss ->
            if (anschluss.art.wert !in ALTE_METHODEN_ANSCHLUSSARTEN) anschluss
            else {
                verändert = true
                anschluss.copy(art = AnschlussArtId("mathematik.methode"))
            }
        }
        if (neueAnschlüsse == knoten.anschlüsse) knoten else knoten.copy(anschlüsse = neueAnschlüsse)
    }
    return if (verändert) copy(knoten = neueKnoten) else this
}

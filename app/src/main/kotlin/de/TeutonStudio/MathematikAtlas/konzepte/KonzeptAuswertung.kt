package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante

/**
 * Wertet ausführbare Definitionskarten mit symbolischen Vorschauwerten aus.
 * Generische Dokumentationskarten bleiben reine Dokumentation.
 */
internal fun KartenAuswerter.werteKonzeptKarteAus(karte: KartenDaten): KartenAuswertungsErgebnis {
    val enthältNichtAuswertbareDokumentation = karte.knoten.any { knoten ->
        knoten.art.startsWith("konzept.") && knoten.art != KonzeptKnotenArten.EINGANG
    }
    if (enthältNichtAuswertbareDokumentation) {
        return KartenAuswertungsErgebnis(emptyMap(), emptyList())
    }

    val vorgaben = karte.knoten
        .filter { it.art == KonzeptKnotenArten.EINGANG }
        .associate { knoten ->
            val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
            val methodenErgebnisArt = karte.inferiereMethodenErgebnisArt(knoten, ausgang)
            knoten.id to mapOf(ausgang.name to knoten.vorschauWert(ausgang, methodenErgebnisArt))
        }

    return auswerten(karte, vorgaben)
}

private fun KartenDaten.inferiereMethodenErgebnisArt(
    knoten: KnotenDaten,
    ausgang: AnschlussDaten,
): String? {
    if (ausgang.art.wert != "mathematik.methode") return null
    knoten.parameter[methodenErgebnisArtSchlüssel(ausgang.name)]?.let { return it }
    val quelle = AnschlussVerweis(knoten.id, ausgang.id)
    return verbindungen.asSequence()
        .filter { it.von == quelle }
        .mapNotNull { verbindung ->
            val zielKnoten = this.knoten.firstOrNull { it.id == verbindung.zu.knotenId } ?: return@mapNotNull null
            val zielAnschluss = zielKnoten.anschlüsse.firstOrNull { it.id == verbindung.zu.anschlussId }
                ?: return@mapNotNull null
            zielKnoten.parameter[methodenErgebnisArtSchlüssel(zielAnschluss.name)]
                ?: zielKnoten.parameter[METHODEN_ANWENDUNG_ERGEBNIS_ART]
        }
        .distinct()
        .singleOrNull()
}

private fun KnotenDaten.vorschauWert(
    ausgang: AnschlussDaten,
    methodenErgebnisArt: String?,
): BedingterWert {
    val parameterName = name.trim().ifBlank { "eingang" }
    val aussagenVorschau = if (ausgang.art == MathematikAnschlussArten.Aussage.id) {
        parameter["vorschauWert"]
            ?.toBooleanStrictOrNull()
            ?.let(::WahrheitsKonstante)
    } else {
        null
    }
    return symbolischerEingangswert(
        art = ausgang.art,
        name = parameterName,
        knotenId = id,
        aussagenVorschau = aussagenVorschau,
        methodenErgebnisArt = methodenErgebnisArt,
    )
}

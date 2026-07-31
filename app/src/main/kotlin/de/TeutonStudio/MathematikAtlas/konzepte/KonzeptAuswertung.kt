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
        knoten.art.startsWith("konzept.") && knoten.art != TestDefinitionsKarten.KONZEPT_EINGANG_ART
    }
    if (enthältNichtAuswertbareDokumentation) {
        return KartenAuswertungsErgebnis(emptyMap(), emptyList())
    }

    val vorgaben = karte.knoten
        .filter { it.art == TestDefinitionsKarten.KONZEPT_EINGANG_ART }
        .associate { knoten ->
            val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
            knoten.id to mapOf(ausgang.name to knoten.vorschauWert(ausgang))
        }

    return auswerten(karte, vorgaben)
}

private fun KnotenDaten.vorschauWert(ausgang: AnschlussDaten): BedingterWert {
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
    )
}

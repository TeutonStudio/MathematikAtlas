package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.Matrix
import de.TeutonStudio.MathematikRechenSystem.kern.SpaltenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.UmformungsTabelle
import de.TeutonStudio.MathematikRechenSystem.kern.UmformungsVerlauf

internal data class AuswertungsTabellenKonfiguration(
    val spaltenNamen: List<String>,
    val rechteSeitenSpalten: Int,
)

internal fun auswertungsTabellenKonfiguration(
    modus: String?,
    variablenText: String?,
    eingabeSpalten: Int?,
    verlaufsSpalten: Int,
    hatRechteSeiteEingang: Boolean,
): AuswertungsTabellenKonfiguration {
    require(verlaufsSpalten > 0)
    val normalisierterModus = modus.orEmpty().trim().lowercase()
    val istInverse = normalisierterModus == "inverse" ||
        normalisierterModus.contains("gauss-jordan") ||
        normalisierterModus.contains("gauß-jordan")
    val rechteSeitenSpalten = when {
        hatRechteSeiteEingang && eingabeSpalten != null ->
            (verlaufsSpalten - eingabeSpalten).coerceIn(0, verlaufsSpalten)
        istInverse -> (verlaufsSpalten / 2).takeIf { verlaufsSpalten % 2 == 0 } ?: 0
        normalisierterModus in setOf("linearessystem", "lineares system", "loesen", "lösen") ->
            1.takeIf { verlaufsSpalten > 1 } ?: 0
        else -> 0
    }
    val koeffizientenSpalten = verlaufsSpalten - rechteSeitenSpalten
    val konfigurierteVariablen = variablenText.orEmpty()
        .split(',')
        .map(String::trim)
        .filter(String::isNotBlank)
        .takeIf { it.size == koeffizientenSpalten && it.distinct().size == it.size }
    val variablen = konfigurierteVariablen
        ?: List(koeffizientenSpalten) { index -> "x_${index + 1}" }
    val rechteSeite = when {
        rechteSeitenSpalten == 0 -> emptyList()
        istInverse -> List(rechteSeitenSpalten) { index -> "e_${index + 1}" }
        rechteSeitenSpalten == 1 -> listOf("b")
        else -> List(rechteSeitenSpalten) { index -> "b_${index + 1}" }
    }
    return AuswertungsTabellenKonfiguration(
        spaltenNamen = variablen + rechteSeite,
        rechteSeitenSpalten = rechteSeitenSpalten,
    )
}

internal fun umformungsTabelleOderNull(
    knoten: KnotenDaten,
    ergebnis: KnotenAuswertungsErgebnis?,
): UmformungsTabelle? {
    val schritte = ergebnis?.schritte.orEmpty()
    val eingabe = ergebnis?.eingänge?.get("objekt")?.objekt as? Matrix
    val start = schritte.firstOrNull()?.vorher as? Matrix ?: eingabe ?: return null
    val ende = schritte.lastOrNull()?.nachher as? Matrix
        ?: (ergebnis?.ausgaben?.get("wert")?.objekt as? Matrix)
        ?: start
    if (start.spaltenAnzahl != ende.spaltenAnzahl) return null
    val hatRechteSeite = ergebnis?.eingänge?.get("rechteSeite")?.objekt is SpaltenVektor
    val konfiguration = auswertungsTabellenKonfiguration(
        modus = knoten.parameter["gaussModus"],
        variablenText = knoten.parameter["variablen"],
        eingabeSpalten = eingabe?.spaltenAnzahl,
        verlaufsSpalten = start.spaltenAnzahl,
        hatRechteSeiteEingang = hatRechteSeite,
    )
    return runCatching {
        UmformungsVerlauf(start, schritte, ende).alsMatrixTabelle(
            spaltenNamen = konfiguration.spaltenNamen,
            rechteSeitenSpalten = konfiguration.rechteSeitenSpalten,
        )
    }.getOrNull()
}

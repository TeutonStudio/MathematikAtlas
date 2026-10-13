package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixRechner

/** Rollen ändern sich, stabile Identitäten und bestehende Ausgangsverbindungen bleiben erhalten. */
fun KartenDaten.migriereMatrixZerlegenZumÜbersetzer(): KartenDaten = copy(knoten = knoten.map { k ->
    if (k.art != MatrixRechner.KNOTEN_ART || k.parameter[RECHNER_OPERATOR_PARAMETER] != "matrix.zerlegen") k
    else k.copy(art = ÜBERSETZER_ART,
        eigenschaften = k.eigenschaften + (ÜBERSETZER_ZIEL to KnotenEigenschaft.Text("matrixzerlegen")),
        parameter = k.parameter.filterKeys { it != RECHNER_OPERATOR_PARAMETER && it != "standardwert.matrix" } +
            (k.parameter["standardwert.matrix"]?.let { mapOf("standardwert.wert" to it) } ?: emptyMap()),
        eingangsKartenVerweise = k.eingangsKartenVerweise.mapKeys { if (it.key == "matrix") "wert" else it.key },
        anschlüsse = k.anschlüsse.map { p -> if (p.richtung == AnschlussRichtung.Eingang && p.name == "matrix")
            p.copy(name = "wert", art = MathematikAnschlussArten.Objekt.id, zulässigeArten = setOf(MathematikAnschlussArten.Matrix.id)) else p })
})

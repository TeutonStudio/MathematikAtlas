package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Vorkonfigurierte Katalogeinträge; alle erzeugen weiterhin dieselbe stabile Zahlenrechner-Art. */
object ErweiterteZahlenRechnerKnotenVorlagen {
    val alle: List<KnotenVorlage> = ErweiterterZahlenOperator.entries.map { operator ->
        KnotenVorlage(
            art = ZAHLENRECHNER_ART,
            name = operator.titel,
            kategorie = "Rechnen: Zahlenrechner",
            beschreibung = "Universeller Zahlenrechner im Zustand ${operator.titel}; im Inspector auch als CAS-Formel bearbeitbar.",
            standardGröße = GraphGröße(270f, 145f),
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "a",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Zahl.id,
                ),
                AnschlussDaten(
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Zahl.id,
                ),
            ),
            standardParameter = mapOf(
                ZAHLENRECHNER_OPERATOR to operator.stabileId,
                ZAHLENRECHNER_GRADWINKEL to "false",
                ZAHLENRECHNER_GRAD_AUSWERTEN to "true",
            ),
        )
    }
}

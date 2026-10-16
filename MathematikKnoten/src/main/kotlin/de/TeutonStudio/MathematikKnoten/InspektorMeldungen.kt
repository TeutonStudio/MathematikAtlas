package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis

fun inspektorMeldungen(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, lokale: List<KnotenMeldung> = emptyList()): List<KnotenMeldung> =
    (lokale + listOfNotNull(ergebnis?.fehler, knoten.parameter[RECHNER_MIGRATION_DIAGNOSE], ergebnis?.übersetzungsDiagnose?.beschreibung)
        .map { KnotenMeldung(it, KnotenMeldungsArt.Fehler) } +
        ergebnis?.warnungen.orEmpty().map { KnotenMeldung(it, KnotenMeldungsArt.Warnung) } +
        ergebnis?.elementFehler.orEmpty().map { (id, text) -> KnotenMeldung("$id: $text", KnotenMeldungsArt.Fehler) }).normalisierteMeldungen()

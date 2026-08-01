package de.TeutonStudio.KnotenKartenVerwalter.daten

/** Sämtliche direkte Kartenabhängigkeiten eines Knotens, einschließlich Eingangs-Fallbacks. */
fun KnotenDaten.alleKartenVerweise(): List<KartenVerweis> =
    (listOfNotNull(kartenVerweis) + eingangsKartenVerweise.values)
        .distinct()

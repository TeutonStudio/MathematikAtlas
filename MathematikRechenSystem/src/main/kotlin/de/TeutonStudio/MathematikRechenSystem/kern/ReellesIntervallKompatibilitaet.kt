package de.TeutonStudio.MathematikRechenSystem.kern

/** Quellcode-Kompatibilität innerhalb des Rechenkerns; keine Karten- oder Anschlussmigration. */
@Deprecated("Verwende links.", ReplaceWith("links"))
val ReellesIntervall.untereGrenze: ZahlAusdruck get() = links

/** Quellcode-Kompatibilität innerhalb des Rechenkerns; keine Karten- oder Anschlussmigration. */
@Deprecated("Verwende rechts.", ReplaceWith("rechts"))
val ReellesIntervall.obereGrenze: ZahlAusdruck get() = rechts

/** Geschlossene Kurzform für bestehende interne Aufrufer. */
@Deprecated(
    "Gib die Offenheit beider Grenzen ausdrücklich an.",
    ReplaceWith("reellesIntervall(untereGrenze, false, obereGrenze, false, kontext)"),
)
fun reellesIntervall(
    untereGrenze: ZahlAusdruck,
    obereGrenze: ZahlAusdruck,
    kontext: RechenKontext = RechenKontext(),
): MengenAusdruck = reellesIntervall(untereGrenze, false, obereGrenze, false, kontext)

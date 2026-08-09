package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Quellkompatibilität für alte Knoten-/Auswerterpfade nach Entfernung des
 * eigenständigen Zahlenoperators `Norm`.
 *
 * Der Alias ist kein Enum-Eintrag und erscheint daher weder im Operatorregister
 * noch in Auswahl, Katalog oder Formel-Tastatur. Alte Norm-Pfade verwenden die
 * kanonische 0-Distanz (`Betrag`).
 */
@Deprecated(
    message = "Norm ist im Zahlenrechner redundant zur 0-Distanz.",
    replaceWith = ReplaceWith("UniversellerZahlenOperator.BETRAG"),
)
val UniversellerZahlenOperator.Companion.NORM: UniversellerZahlenOperator
    get() = UniversellerZahlenOperator.BETRAG

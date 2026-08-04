package de.TeutonStudio.MathematikAtlas.speicher

/**
 * Bereichsprüfung für noch nicht vollständig geparste Dezimalkanäle.
 * Ein fehlender Wert liegt definitionsgemäß in keinem gültigen Kanalbereich.
 */
internal operator fun ClosedFloatingPointRange<Double>.contains(wert: Double?): Boolean =
    wert != null && wert >= start && wert <= endInclusive

package de.TeutonStudio.MathematikAtlas.speicher

/** Kotlin-versionsstabile Hexadezimalprüfung für persistierte Profilfarben. */
internal fun Char.isHexDigit(): Boolean = digitToIntOrNull(radix = 16) != null

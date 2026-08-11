package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck

/**
 * Atlas-UI-Helfer für benannte Methodenargumenträume.
 *
 * [Methode.werteVorräte] ist absichtlich eine nach Parameternamen geordnete Map.
 * Die Drag-Vorschau interessiert nur der mathematische Raum des jeweiligen
 * Eintrags, nicht sein interner Parametername.
 */
internal fun Map.Entry<String, MengenAusdruck>.zuLatex(): String = value.zuLatex()

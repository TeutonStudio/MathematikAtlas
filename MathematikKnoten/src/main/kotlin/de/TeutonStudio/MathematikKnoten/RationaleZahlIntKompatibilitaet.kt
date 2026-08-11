package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl

/** Kleine Adapterüberladung für Größen aus Kotlin-Collections. */
internal fun RationaleZahl.Companion.von(ganzzahl: Int): RationaleZahl = von(ganzzahl.toLong())

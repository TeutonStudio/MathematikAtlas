package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.padding as composePadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Kompatibilitätsüberladung für den aktuellen Master-Stand der Dialogwerkzeugleiste.
 * Die verwendete Compose-Version bietet keine einparametrigen `vertical`-/`end`-Padding-Aufrufe.
 * Sobald die Toolbar selbst auf die vollständigen Padding-Argumente umgestellt ist, kann diese
 * kleine Brücke wieder entfallen.
 */
internal fun Modifier.padding(
    vertical: Dp? = null,
    end: Dp? = null,
): Modifier = composePadding(
    start = 0.dp,
    top = vertical ?: 0.dp,
    end = end ?: 0.dp,
    bottom = vertical ?: 0.dp,
)

package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll as composeVerticalScroll
import androidx.compose.ui.Modifier

/** Lokaler Importanker, damit der JSON-Editor die Foundation-Scroll-Erweiterung eindeutig auflöst. */
internal fun Modifier.verticalScroll(state: ScrollState): Modifier = composeVerticalScroll(state)

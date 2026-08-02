package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll as composeVerticalScroll
import androidx.compose.ui.Modifier

/** Lokale Brücke, damit die Profilansicht ohne einen zweiten Scrollcontainer importiert werden kann. */
internal fun Modifier.verticalScroll(state: ScrollState): Modifier =
    this.composeVerticalScroll(state)

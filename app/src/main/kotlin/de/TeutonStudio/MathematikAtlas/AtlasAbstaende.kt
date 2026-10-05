package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gemeinsame kompakte Abstandsskala der Android-App.
 *
 * Die Grundwerte werden auch einzeln angeboten, damit Speziallayouts dieselbe
 * Skala verwenden können, ohne ihre semantische Bedeutung zu verlieren.
 */
internal data class AtlasAbstände(
    val haarlinie: Dp,
    val winzig: Dp,
    val eng: Dp,
    val standard: Dp,
    val bereich: Dp,
    val inhalt: Dp,
    val dialog: Dp,
    val weit: Dp,
) {
    val liste: PaddingValues
        get() = PaddingValues(horizontal = standard, vertical = eng)

    val bereichInnen: PaddingValues
        get() = PaddingValues(bereich)

    val dialogInnen: PaddingValues
        get() = PaddingValues(horizontal = inhalt, vertical = bereich)

    val aktionsLeiste: PaddingValues
        get() = PaddingValues(horizontal = inhalt, vertical = standard)

    companion object {
        val Kompakt = AtlasAbstände(
            haarlinie = 2.dp,
            winzig = 4.dp,
            eng = 6.dp,
            standard = 8.dp,
            bereich = 12.dp,
            inhalt = 16.dp,
            dialog = 20.dp,
            weit = 24.dp,
        )
    }
}

internal val LocalAtlasAbstände = staticCompositionLocalOf { AtlasAbstände.Kompakt }

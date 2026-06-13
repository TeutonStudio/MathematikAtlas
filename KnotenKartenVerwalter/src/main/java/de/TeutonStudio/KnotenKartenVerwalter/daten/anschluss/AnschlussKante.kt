package de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AnschlussKante {
    Links,
    Rechts,
    Oben,
    Unten;

    public fun istVertikal(): Boolean = this == Links || this == Rechts
    public fun istHorizontal(): Boolean = this == Oben || this == Unten

    public fun radius(radius: Dp = (2.5f).dp): Dp = radius(this,radius)

    public fun <A> wertFür(
        links: A,
        rechts: A,
        oben: A,
        unten: A,
    ): A = wertFür(this,links,rechts,oben,unten)

    public fun tangente(länge: Float = 1f): Offset = wertFür(
        Offset(-1f, 0f),
        Offset(1f, 0f),
        Offset(0f, -1f),
        Offset(0f, 1f),
    ) * länge

    public fun gegenüber() = wertFür(
        Rechts,
        Links,
        Unten,
        Oben,
    )

    public fun alignment(): Alignment = wertFür(
        Alignment.CenterStart,
        Alignment.CenterEnd,
        Alignment.TopCenter,
        Alignment.BottomCenter,
    )

    public companion object {
        private fun <A> wertFür(
            kante: AnschlussKante,
            links: A,
            rechts: A,
            oben: A,
            unten: A,
        ): A = when(kante) {
            Links -> links
            Rechts -> rechts
            Oben -> oben
            Unten -> unten
        }

        public fun Modifier.fillMaxKante(kante: AnschlussKante,@FloatRange fraction: Float = 1f): Modifier = if (kante.istVertikal()) fillMaxHeight(fraction) else fillMaxWidth(fraction)

        public fun Modifier.offsetKante(kante: AnschlussKante, offset: Dp = 0.dp) = if(kante.istVertikal()) offset(x=offset) else offset(y=offset)

        private fun radius(kante: AnschlussKante, radius: Dp = (2.5f).dp): Dp = if (kante == AnschlussKante.Rechts || kante == AnschlussKante.Unten) radius else -radius

    }
}


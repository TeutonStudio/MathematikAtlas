package com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss

import androidx.compose.ui.Alignment

enum class AnschlussKante {
    Links,
    Rechts,
    Oben,
    Unten;

    public fun istVertikal(): Boolean = this == Links || this == Rechts
    public fun istHorizontal(): Boolean = this == Oben || this == Unten

    public fun <A> wertFür(
        links: A,
        rechts: A,
        oben: A,
        unten: A,
    ): A = when(this) {
        Links -> links
        Rechts -> rechts
        Oben -> oben
        Unten -> unten
    }

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
}


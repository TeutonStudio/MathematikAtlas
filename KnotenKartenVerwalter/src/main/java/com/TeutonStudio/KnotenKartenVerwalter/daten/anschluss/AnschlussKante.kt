package com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss

import androidx.compose.ui.Alignment

enum class AnschlussKante {
    Links,
    Rechts,
    Oben,
    Unten;

    public fun istVertikal(): Boolean = this == AnschlussKante.Links || this == AnschlussKante.Rechts
    public fun istHorizontal(): Boolean = this == AnschlussKante.Oben || this == AnschlussKante.Unten

    public fun <A> wertFür(
        links: A,
        rechts: A,
        oben: A,
        unten: A,
    ): A = when(this) {
        AnschlussKante.Links -> links
        AnschlussKante.Rechts -> rechts
        AnschlussKante.Oben -> oben
        AnschlussKante.Unten -> unten
    }

    public fun alignment(): Alignment = wertFür(
        Alignment.CenterStart,
        Alignment.CenterEnd,
        Alignment.TopCenter,
        Alignment.BottomCenter,
    )
}


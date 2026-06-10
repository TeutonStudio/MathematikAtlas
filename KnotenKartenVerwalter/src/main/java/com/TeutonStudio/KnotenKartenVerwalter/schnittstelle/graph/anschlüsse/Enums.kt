package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.ui.Alignment

enum class AnschlussRichtung {
    Eingang,
    Ausgang,
}

public fun AnschlussRichtung?.istEingang(): Boolean = this == AnschlussRichtung.Eingang
public fun AnschlussRichtung?.istAusgang(): Boolean = this == AnschlussRichtung.Ausgang


enum class AnschlussKante {
    Links,
    Rechts,
    Oben,
    Unten,
}

public fun <A> AnschlussKante.wertFür(
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

public fun AnschlussKante.istVertikal(): Boolean = this == AnschlussKante.Links || this == AnschlussKante.Rechts
public fun AnschlussKante.istHorizontal(): Boolean = this == AnschlussKante.Oben || this == AnschlussKante.Unten



public fun AnschlussKante.alignment(): Alignment = when(this) {
    AnschlussKante.Links -> Alignment.CenterStart
    AnschlussKante.Rechts -> Alignment.CenterEnd
    AnschlussKante.Oben -> Alignment.TopCenter
    AnschlussKante.Unten -> Alignment.BottomCenter
}

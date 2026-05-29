package com.TeutonStudio.KnotenKartenVerwalter.daten

enum class AnschlussRichtung {
    Eingang,
    Ausgang,
}

sealed class AnschlussDaten(
    open val id: String,
    open val label: String,
    open val richtung: AnschlussRichtung,
)

data class EingangDaten(
    override val id: String,
    override val label: String,
) : AnschlussDaten(
    id = id,
    label = label,
    richtung = AnschlussRichtung.Eingang,
)

data class AusgangDaten(
    override val id: String,
    override val label: String,
) : AnschlussDaten(
    id = id,
    label = label,
    richtung = AnschlussRichtung.Ausgang,
)

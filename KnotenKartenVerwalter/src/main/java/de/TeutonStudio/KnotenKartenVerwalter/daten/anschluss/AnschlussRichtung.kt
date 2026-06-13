package de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss


enum class AnschlussRichtung {
    Eingang,
    Ausgang;

    public fun istEingang(): Boolean = this == AnschlussRichtung.Eingang
    public fun istAusgang(): Boolean = this == AnschlussRichtung.Ausgang
}

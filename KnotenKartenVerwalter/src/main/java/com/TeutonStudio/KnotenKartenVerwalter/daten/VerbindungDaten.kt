package com.TeutonStudio.KnotenKartenVerwalter.daten

data class VerbindungDaten(
    val id: String,
    val quellKnotenId: String,
    val quellAnschlussId: String,
    val zielKnotenId: String,
    val zielAnschlussId: String,
    val label: String? = null,
    val typ: String = "default",
    val ausgewaehlt: Boolean = false,
)

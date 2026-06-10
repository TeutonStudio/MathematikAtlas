package com.TeutonStudio.KnotenKartenVerwalter.daten

abstract class GraphDaten(
    open val id: String,
) {
    abstract val klasse: String?
}
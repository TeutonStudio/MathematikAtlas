package com.TeutonStudio.KnotenKartenVerwalter.daten.fix


sealed interface GraphDaten {
    val id: String
    val klasse: String?
}
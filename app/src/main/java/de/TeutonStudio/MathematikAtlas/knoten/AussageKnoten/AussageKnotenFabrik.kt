package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenKonstruktor

@Suppress("UNCHECKED_CAST")
val AussageKnotenFabrik: KnotenFabrik = mapOf(
    definition.KNOTEN_ART to ::definition as KnotenKonstruktor,
    operator.KNOTEN_ART to ::operator as KnotenKonstruktor,
    auswerten.KNOTEN_ART to ::auswerten as KnotenKonstruktor,
)
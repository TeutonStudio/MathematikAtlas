package de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenKonstruktor

@Suppress("UNCHECKED_CAST")
val MengenKnotenFabrik: KnotenFabrik = mapOf(
    definition.KNOTEN_ART to ::definition as KnotenKonstruktor,
    operator.KNOTEN_ART to ::operator as KnotenKonstruktor,
    relation.KNOTEN_ART to ::relation as KnotenKonstruktor,
    auswerten.KNOTEN_ART to ::auswerten as KnotenKonstruktor,
    unbekannt.KNOTEN_ART to ::unbekannt as KnotenKonstruktor,
)

package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAusgabeDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenEingabeDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenKonstruktor

@Suppress("UNCHECKED_CAST")
val AussageKnotenFabrik: KnotenFabrik = mapOf(
    definition.KNOTEN_ART to ::definition as KnotenKonstruktor,
    operator.KNOTEN_ART to ::operator as KnotenKonstruktor,
    auswerten.KNOTEN_ART to ::auswerten as KnotenKonstruktor,
)
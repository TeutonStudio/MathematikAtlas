package de.TeutonStudio.MathematikAtlas.knoten

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenKonstruktor
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageKnotenFabrik
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.auswerten
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.definition
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.operator


@Suppress("UNCHECKED_CAST")
val MatheKnotenFabrik: KnotenFabrik = mapOf<KnotenArt, KnotenKonstruktor>().plus(AussageKnotenFabrik)
package de.TeutonStudio.MathematikAtlas.knoten

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenKonstruktor
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageKnotenFabrik
import de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten.MengenKnotenFabrik
import de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten.ZahlenKnotenFabrik


@Suppress("UNCHECKED_CAST")
val MatheKnotenFabrik: KnotenFabrik =
    mapOf<KnotenArt, KnotenKonstruktor>() + AussageKnotenFabrik + ZahlenKnotenFabrik + MengenKnotenFabrik

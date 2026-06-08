package com.TeutonStudio.MathematikAtlas

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.idReferenz


public fun testKarte(): KarteDaten = KarteDaten(
    id = "test-karte",
    name = "Graph Testkarte",
    knoten = listOf(
        KnotenDaten(
            id = "eingabe",
            name = "Eingabe",
            position = Offset(80f, 120f),
            anschlüsse = mutableMapOf(
                AusgangDaten("out1", AnschlussKante.Rechts,"Ausgang 1") to 0,
                AusgangDaten("out2", AnschlussKante.Rechts,"Ausgang 2") to 1
            )
        ),
        KnotenDaten(
            id = "mitte",
            name = "Mitte",
            position = Offset(360f, 170f),
            anschlüsse = mutableMapOf(
                AusgangDaten("out", AnschlussKante.Rechts,"Ausgang 1") to 0,
                EingangDaten("in", AnschlussKante.Links,"Eingang 1") to 0,
                AnschlussDaten("top1", AnschlussKante.Oben,"Test Oben 1") to 0,
                AnschlussDaten("top2", AnschlussKante.Oben,"Test Oben 2") to 1,
                AnschlussDaten("top3", AnschlussKante.Oben,"Test Oben 3") to 2,
                AnschlussDaten("bot1", AnschlussKante.Unten,"Test Unten 1") to 0,
                AnschlussDaten("bot2", AnschlussKante.Unten,"Test Unten 2") to 1,
            )
        ),
        AusgabeDaten(
            id = "ausgabe",
            name = "Ausgabe",
            position = Offset(660f, 120f),
            anschlussLabel = mutableMapOf(
                AnschlussKante.Links to ("Eingang 1" to 0)
            ),
            ausgewaehlt = true
        ),
    ),
    verbindungen = listOf(
        VerbindungDaten(
            id = "v-eingabe-mitte",
            ids = idReferenz("eingabe" to "mitte","out1" to "in"),
        ),
        VerbindungDaten(
            id = "v-mitte-ausgabe",
            ids = idReferenz("mitte" to "ausgabe","out" to AusgabeDaten.id("ausgabe",0)),
        ),
    ),
)
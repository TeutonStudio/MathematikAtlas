package com.TeutonStudio.MathematikAtlas

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAusgabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten


public fun testKarte(): KarteDaten = KarteDaten(
    id = "test-karte",
    name = "Graph Testkarte",
    initialKnoten = listOf(
        KnotenDaten(
            id = "eingabe",
            name = "Eingabe",
            position = Offset(80f, 120f),
            anschlüsse = mutableMapOf(
                AusgangDaten("out1", AnschlussKante.Rechts, "Ausgang 1") to 0,
                AusgangDaten("out2", AnschlussKante.Rechts, "Ausgang 2") to 1
            )
        ),
        KnotenDaten(
            id = "mitte",
            name = "Mitte",
            position = Offset(360f, 170f),
            anschlüsse = mutableMapOf(
                AusgangDaten("out", AnschlussKante.Rechts,"Ausgang 1") to 0,
                EingangDaten("in", AnschlussKante.Links, "Eingang 1") to 0,
                AnschlussDaten("top1", AnschlussKante.Oben, "Test Oben 1") to 0,
                AnschlussDaten("top2", AnschlussKante.Oben,"Test Oben 2") to 1,
                AnschlussDaten("top3", AnschlussKante.Oben,"Test Oben 3") to 2,
                AnschlussDaten("bot1", AnschlussKante.Unten,"Test Unten 1") to 0,
                AnschlussDaten("bot2", AnschlussKante.Unten,"Test Unten 2") to 1,
            )
        ),
        KnotenAusgabeDaten(
            id = "ausgabe",
            name = "Ausgabe",
            position = Offset(660f, 120f),
            anschlussLabel = mutableMapOf(
                AnschlussKante.Links to ("Eingang 1" to 0)
            )
        ),
    ),
    initialVerbindungen = listOf(
        VerbindungDaten(
            id = "v-eingabe-mitte",
            ids = IDEhe("eingabe","mitte","out1","in"),
        ),
        VerbindungDaten(
            id = "v-mitte-ausgabe",
            ids = IDEhe("mitte","ausgabe","out",KnotenAusgabeDaten.id("ausgabe",0)),
        ),
    ),
)
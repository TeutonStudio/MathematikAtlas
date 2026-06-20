package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.geometry.Offset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.daten.vordefiniert.BasisKnotenDaten


/*
public fun testKarte(): KarteDaten = KarteDaten(
    id = "test-karte",
    name = "Graph Testkarte",
    initialKnoten = listOf(
        BasisKnotenDaten(
            id = "eingabe",
            name = "Eingabe",
            position = Offset(80f, 120f),
            anschlüsse = mutableMapOf(
                AusgangDaten("out1", Kante.Rechts, "Ausgang 1") to 0,
                AusgangDaten("out2", Kante.Rechts, "Ausgang 2") to 1
            )
        ),
        BasisKnotenDaten(
            id = "mitte",
            name = "Mitte",
            position = Offset(360f, 170f),
            anschlüsse = mutableMapOf(
                AusgangDaten("out",AnschlussKante.Rechts,"Ausgang 1") to 0,
                EingangDaten("in",AnschlussKante.Links,"Eingang 1") to 0,
                AnschlussDaten("top1",AnschlussKante.Oben,"Test Oben 1") to 0,
                AnschlussDaten("top2",AnschlussKante.Oben,"Test Oben 2") to 1,
                AnschlussDaten("top3",AnschlussKante.Oben,"Test Oben 3") to 2,
                AnschlussDaten("bot1",AnschlussKante.Unten,"Test Unten 1") to 0,
                AnschlussDaten("bot2",AnschlussKante.Unten,"Test Unten 2") to 1,
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
            ids = GraphDatenVerbindung.IDEhe("eingabe", "mitte", "out1", "in"),
        ),
        VerbindungDaten(
            id = "v-mitte-ausgabe",
            ids = GraphDatenVerbindung.IDEhe(
                "mitte",
                "ausgabe",
                "out",
                KnotenAusgabeDaten.id("ausgabe", 0)
            ),
        ),
    ),
)*/

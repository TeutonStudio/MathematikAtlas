package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.geometry.Offset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.vordefiniert.BasisDatenVerbindung
import de.TeutonStudio.MathematikAtlas.karten.AussageKarteDaten
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.definition
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.definition.AussageDefinitionDaten
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.operator
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.operator.AussageOperatorDatenBasis
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageDefinition
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageAuswerten

fun aussageTestKarte(): AussageKarteDaten {
    /*
     * A = Wahr
     * B = Lüge
     * C = Wahr
     *
     * Ergebnis:
     * (A UND B) ODER C
     */

    val aussageA = AussageDefinition(
        id = "aussage-a",
        name = "A",
        initialWahr = true,
    ).apply {
        position = Offset(
            x = 80f,
            y = 80f,
        )
    }

    val aussageB = AussageDefinition(
        id = "aussage-b",
        name = "B",
        initialWahr = false,
    ).apply {
        position = Offset(
            x = 80f,
            y = 260f,
        )
    }

    val aussageC = AussageDefinition(
        id = "aussage-c",
        name = "C",
        initialWahr = true,
    ).apply {
        position = Offset(
            x = 380f,
            y = 420f,
        )
    }

    val undKnoten = AussageOperatorDatenBasis(
        id = "operator-und",
        name = "UND",
    ).apply {
        position = Offset(
            x = 380f,
            y = 160f,
        )

        data[operator.OPERATOR_SCHLÜSSEL] = operator.AussagenVerknüpfung.UND.name
    }

    val oderKnoten = AussageOperatorDatenBasis(
        id = "operator-oder",
        name = "ODER",
    ).apply {
        position = Offset(
            x = 680f,
            y = 260f,
        )

        data[operator.OPERATOR_SCHLÜSSEL] =
            operator.AussagenVerknüpfung.ODER.name
    }

    val auswertenKnoten = AussageAuswerten(
        id = "aussage-auswerten",
        name = "Auswerten",
    ).apply {
        position = Offset(
            x = 980f,
            y = 260f,
        )
    }

    /*
     * Anschluss-IDs werden aus den tatsächlich erzeugten Daten gelesen.
     * Damit hängt die Testkarte nicht an nachgebauten ID-Strings.
     */

    val ausgangA = aussageA.anschlüsse.filterIsInstance<GraphDatenAnschluss.gerichteteGDA>().filter { it.richtung == Richtung.Ausgang }.single()
    val ausgangB = aussageB.anschlüsse.filterIsInstance<GraphDatenAnschluss.gerichteteGDA>().filter { it.richtung == Richtung.Ausgang }.single()
    val ausgangC = aussageC.anschlüsse.filterIsInstance<GraphDatenAnschluss.gerichteteGDA>().filter { it.richtung == Richtung.Ausgang }.single()

    val undEingänge = undKnoten.anschlüsse.filterIsInstance<GraphDatenAnschluss.gerichteteGDA>().filter { it.richtung == Richtung.Eingang }
        .sortedBy {
            undKnoten.anschlussIdx[it.id]
                ?: Int.MAX_VALUE
        }

    val undAusgang = undKnoten.anschlüsse.filterIsInstance<GraphDatenAnschluss.gerichteteGDA>().filter { it.richtung == Richtung.Ausgang }.single()

    val oderEingänge = oderKnoten.anschlüsse.filterIsInstance<GraphDatenAnschluss.gerichteteGDA>().filter { it.richtung == Richtung.Eingang }
        .sortedBy {
            oderKnoten.anschlussIdx[it.id]
                ?: Int.MAX_VALUE
        }

    val oderAusgang = oderKnoten.anschlüsse.filterIsInstance<GraphDatenAnschluss.gerichteteGDA>().filter { it.richtung == Richtung.Ausgang }.single()

    val auswertenEingang = auswertenKnoten.anschlüsse.filterIsInstance<GraphDatenAnschluss.gerichteteGDA>().filter { it.richtung == Richtung.Eingang }.single()

    require(undEingänge.size == 2) {
        "Der UND-Knoten benötigt genau zwei Eingänge"
    }

    require(oderEingänge.size == 2) {
        "Der ODER-Knoten benötigt genau zwei Eingänge"
    }

    return AussageKarteDaten(
        id = "aussage-testkarte",
        name = "Aussagenlogik – Testkarte",

        initialKnoten = listOf(
            aussageA,
            aussageB,
            aussageC,
            undKnoten,
            oderKnoten,
            auswertenKnoten,
        ),

        initialVerbindungen = listOf(
            /*
             * A -> UND Eingang 0
             */
            BasisDatenVerbindung(
                id = "verbindung-a-und",
                ids = GraphDatenVerbindung.IDEhe(
                    knotenIdMann = aussageA.id,
                    knotenIdWeib = undKnoten.id,
                    anschlussIdMann = ausgangA.id,
                    anschlussIdWeib = undEingänge[0].id,
                ),
                label = "", fehler = null,
            ),

            /*
             * B -> UND Eingang 1
             */
            BasisDatenVerbindung(
                id = "verbindung-b-und",
                ids = GraphDatenVerbindung.IDEhe(
                    knotenIdMann = aussageB.id,
                    knotenIdWeib = undKnoten.id,
                    anschlussIdMann = ausgangB.id,
                    anschlussIdWeib = undEingänge[1].id,
                ),
                label = "", fehler = null,
            ),

            /*
             * UND -> ODER Eingang 0
             */
            BasisDatenVerbindung(
                id = "verbindung-und-oder",
                ids = GraphDatenVerbindung.IDEhe(
                    knotenIdMann = undKnoten.id,
                    knotenIdWeib = oderKnoten.id,
                    anschlussIdMann = undAusgang.id,
                    anschlussIdWeib = oderEingänge[0].id,
                ),
                label = "", fehler = null,
            ),

            /*
             * C -> ODER Eingang 1
             */
            BasisDatenVerbindung(
                id = "verbindung-c-oder",
                ids = GraphDatenVerbindung.IDEhe(
                    knotenIdMann = aussageC.id,
                    knotenIdWeib = oderKnoten.id,
                    anschlussIdMann = ausgangC.id,
                    anschlussIdWeib = oderEingänge[1].id,
                ),
                label = "", fehler = null,
            ),

            /*
             * ODER -> Auswerten
             */
            BasisDatenVerbindung(
                id = "verbindung-oder-auswerten",
                ids = GraphDatenVerbindung.IDEhe(
                    knotenIdMann = oderKnoten.id,
                    knotenIdWeib = auswertenKnoten.id,
                    anschlussIdMann = oderAusgang.id,
                    anschlussIdWeib = auswertenEingang.id,
                ),
                label = "", fehler = null,
            ),
        ),
    )
}
package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.geometry.Offset
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.MathematikAtlas.karten.AussageKarteDaten
import de.TeutonStudio.MathematikAtlas.knoten.AussageAuswertenDaten
import de.TeutonStudio.MathematikAtlas.knoten.AussageDefinitionDaten
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.operator
import de.TeutonStudio.MathematikAtlas.knoten.AussageOperatorDaten

fun aussageTestKarte(): AussageKarteDaten {
    /*
     * A = Wahr
     * B = Lüge
     * C = Wahr
     *
     * Ergebnis:
     * (A UND B) ODER C
     */

    val aussageA = AussageDefinitionDaten(
        id = "aussage-a",
        name = "A",
        initialWahr = true,
    ).apply {
        position = Offset(
            x = 80f,
            y = 80f,
        )
    }

    val aussageB = AussageDefinitionDaten(
        id = "aussage-b",
        name = "B",
        initialWahr = false,
    ).apply {
        position = Offset(
            x = 80f,
            y = 260f,
        )
    }

    val aussageC = AussageDefinitionDaten(
        id = "aussage-c",
        name = "C",
        initialWahr = true,
    ).apply {
        position = Offset(
            x = 380f,
            y = 420f,
        )
    }

    val undKnoten = AussageOperatorDaten(
        id = "operator-und",
        name = "UND",
    ).apply {
        position = Offset(
            x = 380f,
            y = 160f,
        )

        data[operator.OPERATOR_SCHLÜSSEL] = operator.AussagenVerknüpfung.UND.name
    }

    val oderKnoten = AussageOperatorDaten(
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

    val auswertenKnoten = AussageAuswertenDaten(
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

    val ausgangA = aussageA.anschlüsse
        .filterIsInstance<AusgangDaten>()
        .single()

    val ausgangB = aussageB.anschlüsse
        .filterIsInstance<AusgangDaten>()
        .single()

    val ausgangC = aussageC.anschlüsse
        .filterIsInstance<AusgangDaten>()
        .single()

    val undEingänge = undKnoten.anschlüsse
        .filterIsInstance<EingangDaten>()
        .sortedBy {
            undKnoten.anschlussIdx[it.id]
                ?: Int.MAX_VALUE
        }

    val undAusgang = undKnoten.anschlüsse
        .filterIsInstance<AusgangDaten>()
        .single()

    val oderEingänge = oderKnoten.anschlüsse
        .filterIsInstance<EingangDaten>()
        .sortedBy {
            oderKnoten.anschlussIdx[it.id]
                ?: Int.MAX_VALUE
        }

    val oderAusgang = oderKnoten.anschlüsse
        .filterIsInstance<AusgangDaten>()
        .single()

    val auswertenEingang = auswertenKnoten.anschlüsse
        .filterIsInstance<EingangDaten>()
        .single()

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
            VerbindungDaten(
                id = "verbindung-a-und",
                ids = IDEhe(
                    knotenIdMann = aussageA.id,
                    knotenIdWeib = undKnoten.id,
                    anschlussIdMann = ausgangA.id,
                    anschlussIdWeib = undEingänge[0].id,
                ),
            ),

            /*
             * B -> UND Eingang 1
             */
            VerbindungDaten(
                id = "verbindung-b-und",
                ids = IDEhe(
                    knotenIdMann = aussageB.id,
                    knotenIdWeib = undKnoten.id,
                    anschlussIdMann = ausgangB.id,
                    anschlussIdWeib = undEingänge[1].id,
                ),
            ),

            /*
             * UND -> ODER Eingang 0
             */
            VerbindungDaten(
                id = "verbindung-und-oder",
                ids = IDEhe(
                    knotenIdMann = undKnoten.id,
                    knotenIdWeib = oderKnoten.id,
                    anschlussIdMann = undAusgang.id,
                    anschlussIdWeib = oderEingänge[0].id,
                ),
            ),

            /*
             * C -> ODER Eingang 1
             */
            VerbindungDaten(
                id = "verbindung-c-oder",
                ids = IDEhe(
                    knotenIdMann = aussageC.id,
                    knotenIdWeib = oderKnoten.id,
                    anschlussIdMann = ausgangC.id,
                    anschlussIdWeib = oderEingänge[1].id,
                ),
            ),

            /*
             * ODER -> Auswerten
             */
            VerbindungDaten(
                id = "verbindung-oder-auswerten",
                ids = IDEhe(
                    knotenIdMann = oderKnoten.id,
                    knotenIdWeib = auswertenKnoten.id,
                    anschlussIdMann = oderAusgang.id,
                    anschlussIdWeib = auswertenEingang.id,
                ),
            ),
        ),
    )
}
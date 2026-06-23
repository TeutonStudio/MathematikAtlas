package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.geometry.Offset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Richtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.vordefiniert.BasisDatenVerbindung
import de.TeutonStudio.MathematikAtlas.karten.MatheKarte
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageAuswerten
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.AussageDefinition
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.OperatorDaten
import de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten.operator as AussageOperator
import de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten.MengenAuswertenDaten
import de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten.MengenDefinitionDaten
import de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten.MengenOperatorDaten
import de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten.MengenRelationDaten
import de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten.operator as MengenOperator
import de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten.relation as MengenRelation
import de.TeutonStudio.MathematikAtlas.knoten.MengenKnoten.unbekannt as MengenUnbekannt
import de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten.ZahlenAuswertenDaten
import de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten.ZahlenDefinitionDaten
import de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten.ZahlenOperatorDaten
import de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten.ZahlenRelationDaten
import de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten.operator as ZahlenOperator
import de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten.relation as ZahlenRelation
import de.TeutonStudio.MathematikAtlas.knoten.ZahlenKnoten.unbekannt as ZahlenUnbekannt

fun matheTestKarte(): MatheKarte.MatheKarteDaten {
    val aussageA = AussageDefinition(
        id = "aussage-a",
        name = "A",
        initialWahr = true,
    ).at(80f, 80f)

    val aussageB = AussageDefinition(
        id = "aussage-b",
        name = "B",
        initialWahr = false,
    ).at(80f, 230f)

    val aussageOperator = OperatorDaten(
        id = "aussage-operator-und",
        name = "Aussage UND",
    ).at(360f, 150f).apply {
        data[AussageOperator.OPERATOR_SCHLÜSSEL] = AussageOperator.AussagenVerknüpfung.UND.name
        aktualisiereCache()
    }

    val aussageAuswerten = AussageAuswerten(
        id = "aussage-auswerten",
        name = "Aussage auswerten",
    ).at(660f, 150f)

    val zahl2 = ZahlenDefinitionDaten(
        id = "zahl-2",
        name = "Zahl 2",
        initialWert = "2",
    ).at(80f, 420f)

    val zahl3 = ZahlenDefinitionDaten(
        id = "zahl-3",
        name = "Zahl 3",
        initialWert = "3",
    ).at(80f, 570f)

    val zahlUnbekannt = ZahlenDefinitionDaten(
        id = "zahl-unbekannt",
        name = "Unbekannte Zahl",
        initialWert = "?",
    ).at(80f, 720f).apply {
        klasse = ZahlenUnbekannt.KNOTEN_ART
    }

    val zahlOperator = ZahlenOperatorDaten(
        id = "zahl-operator-plus",
        name = "Zahlenoperator",
    ).at(360f, 495f).apply {
        setzeOperator(ZahlenOperator.ZahlenVerknuepfung.ADDITION)
    }

    val zahlRelation = ZahlenRelationDaten(
        id = "zahl-relation",
        name = "Zahlenrelation",
    ).at(660f, 495f).apply {
        setzeRelation(ZahlenRelation.ZahlenRelation.KLEINER)
    }

    val zahlAuswerten = ZahlenAuswertenDaten(
        id = "zahl-auswerten",
        name = "Zahl auswerten",
    ).at(660f, 650f)

    val zahlRelationAuswerten = AussageAuswerten(
        id = "zahl-relation-auswerten",
        name = "Zahlenrelation auswerten",
    ).at(960f, 495f)

    val mengeN = MengenDefinitionDaten(
        id = "menge-n",
        name = "Menge N",
        initialLatex = "\\mathbb{N}",
    ).at(80f, 910f)

    val mengeZ = MengenDefinitionDaten(
        id = "menge-z",
        name = "Menge Z",
        initialLatex = "\\mathbb{Z}",
    ).at(80f, 1060f)

    val mengeUnbekannt = MengenDefinitionDaten(
        id = "menge-unbekannt",
        name = "Unbekannte Menge",
        initialLatex = "?",
    ).at(80f, 1210f).apply {
        klasse = MengenUnbekannt.KNOTEN_ART
    }

    val mengeOperator = MengenOperatorDaten(
        id = "menge-operator-vereinigung",
        name = "Mengenoperator",
    ).at(360f, 985f).apply {
        setzeOperator(MengenOperator.MengenVerknuepfung.VEREINIGUNG)
    }

    val mengeRelation = MengenRelationDaten(
        id = "menge-relation",
        name = "Mengenrelation",
    ).at(660f, 985f).apply {
        setzeRelation(MengenRelation.MengenRelation.TEILMENGE)
    }

    val mengeAuswerten = MengenAuswertenDaten(
        id = "menge-auswerten",
        name = "Menge auswerten",
    ).at(660f, 1140f)

    val mengeRelationAuswerten = AussageAuswerten(
        id = "menge-relation-auswerten",
        name = "Mengenrelation auswerten",
    ).at(960f, 985f)

    val knoten = listOf(
        aussageA,
        aussageB,
        aussageOperator,
        aussageAuswerten,
        zahl2,
        zahl3,
        zahlUnbekannt,
        zahlOperator,
        zahlRelation,
        zahlAuswerten,
        zahlRelationAuswerten,
        mengeN,
        mengeZ,
        mengeUnbekannt,
        mengeOperator,
        mengeRelation,
        mengeAuswerten,
        mengeRelationAuswerten,
    )

    return MatheKarte.MatheKarteDaten(
        id = "mathe-testkarte",
        name = "Mathe Testkarte - alle Knoten",
        initialKnoten = knoten,
        initialVerbindungen = listOf(
            verbindung("aussage-a-und", aussageA, aussageOperator, 0),
            verbindung("aussage-b-und", aussageB, aussageOperator, 1),
            verbindung("aussage-und-auswerten", aussageOperator, aussageAuswerten, 0),

            verbindung("zahl-2-plus", zahl2, zahlOperator, 0),
            verbindung("zahl-3-plus", zahl3, zahlOperator, 1),
            verbindung("zahl-plus-auswerten", zahlOperator, zahlAuswerten, 0),
            verbindung("zahl-2-relation", zahl2, zahlRelation, 0),
            verbindung("zahl-plus-relation", zahlOperator, zahlRelation, 1),
            verbindung("zahl-relation-auswerten", zahlRelation, zahlRelationAuswerten, 0),

            verbindung("menge-n-operator", mengeN, mengeOperator, 0),
            verbindung("menge-z-operator", mengeZ, mengeOperator, 1),
            verbindung("menge-operator-auswerten", mengeOperator, mengeAuswerten, 0),
            verbindung("menge-n-relation", mengeN, mengeRelation, 0),
            verbindung("menge-operator-relation", mengeOperator, mengeRelation, 1),
            verbindung("menge-relation-auswerten", mengeRelation, mengeRelationAuswerten, 0),
        ),
    )
}

private fun <T : GraphDatenKnoten> T.at(x: Float, y: Float): T = apply {
    position = Offset(x, y)
}

private fun verbindung(
    id: String,
    von: GraphDatenKnoten,
    nach: GraphDatenKnoten,
    eingangIndex: Int,
): BasisDatenVerbindung =
    BasisDatenVerbindung(
        id = "verbindung-$id",
        ids = GraphDatenVerbindung.IDEhe(
            knotenIdMann = von.id,
            knotenIdWeib = nach.id,
            anschlussIdMann = von.ausgang().id,
            anschlussIdWeib = nach.eingang(eingangIndex).id,
        ),
    )

private fun GraphDatenKnoten.ausgang(): GraphDatenAnschluss =
    anschlüsse
        .filterIsInstance<GraphDatenAnschluss.gerichteteGDA>()
        .single { it.richtung == Richtung.Ausgang }

private fun GraphDatenKnoten.eingang(index: Int): GraphDatenAnschluss =
    anschlüsse
        .filterIsInstance<GraphDatenAnschluss.gerichteteGDA>()
        .filter { it.richtung == Richtung.Eingang }
        .sortedBy { anschlussIdx[it.id] ?: Int.MAX_VALUE }
        .getOrElse(index) {
            error("Knoten '$id' besitzt keinen Eingang mit Index $index")
        }

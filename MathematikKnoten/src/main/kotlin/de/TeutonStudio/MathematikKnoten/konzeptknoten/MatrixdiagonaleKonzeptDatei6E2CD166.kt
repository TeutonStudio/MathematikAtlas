package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MatrixdiagonaleKonzeptDatei6E2CD166 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.matrixdiagonale|Matrixdiagonale|diagonalArt=hauptdiagonale")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.matrixdiagonale|diagonalArt=hauptdiagonale|Matrixdiagonale"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Matrixdiagonale",
            kurzbeschreibung = "Liest die Haupt- oder rechts oben verankerte Nebendiagonale als kartesisches Tupel.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "matrizen")),
            suchbegriffe = setOf("Liest die Haupt- oder rechts oben verankerte Nebendiagonale als kartesisches Tupel.", "Matrixdiagonale", "Matrizen", "diagonalArt", "diagonale", "hauptdiagonale", "mathematik.matrix", "mathematik.matrixdiagonale", "mathematik.tupel", "matrix"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.matrixdiagonale"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MatrixproduktKonzeptDatei0369ED39 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.matrixProdukt|Matrixprodukt|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.matrixProdukt|Matrixprodukt"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Matrixprodukt",
            kurzbeschreibung = "Multipliziert zwei kompatible Matrizen.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen"), FachPfad.von("lineare-algebra", "matrizen")),
            suchbegriffe = setOf("Matrixprodukt", "Multipliziert zwei kompatible Matrizen.", "Rechnen", "a", "b", "mathematik.matrix", "mathematik.matrixProdukt", "matrix"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.matrixProdukt"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

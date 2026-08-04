package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MatrixInvertierenKonzeptDateiA1A8093F : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.matrixInvertieren|Matrix invertieren|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.matrixInvertieren|Matrix invertieren"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Matrix invertieren",
            kurzbeschreibung = "Invertiert eine rationale quadratische Matrix exakt.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen"), FachPfad.von("lineare-algebra", "matrizen")),
            suchbegriffe = setOf("Invertiert eine rationale quadratische Matrix exakt.", "Matrix invertieren", "Rechnen", "inverse", "mathematik.matrix", "mathematik.matrixInvertieren", "matrix"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.matrixInvertieren"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

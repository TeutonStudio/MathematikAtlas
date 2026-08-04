package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MatrixrechnerKonzeptDatei50E21D48 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.matrixrechner|Matrixrechner|operator=matrix.addition")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.matrixrechner|operator=matrix.addition|Matrixrechner"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Matrixrechner",
            kurzbeschreibung = "Einheitlicher Matrixrechner mit operatorabhängigen Anschlüssen und typisiertem CAS-Formelmodus.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen"), FachPfad.von("lineare-algebra", "matrizen")),
            suchbegriffe = setOf("Einheitlicher Matrixrechner mit operatorabhängigen Anschlüssen und typisiertem CAS-Formelmodus.", "Lineare Algebra: Matrizen", "Matrixrechner", "a", "b", "mathematik.matrix", "mathematik.matrixrechner", "matrix.addition", "operator", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.matrixrechner"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

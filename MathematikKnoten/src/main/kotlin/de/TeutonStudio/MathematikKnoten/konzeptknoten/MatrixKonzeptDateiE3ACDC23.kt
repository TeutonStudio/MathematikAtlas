package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MatrixKonzeptDateiE3ACDC23 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.matrix|Matrix|breite=2;erzeugungsArt=einzelEingaben;höhe=2")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.matrix|breite=2|erzeugungsArt=einzelEingaben|höhe=2|Matrix"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Matrix",
            kurzbeschreibung = "Matrix aus Einzelwerten, Zeilen, Spalten oder einer zweistelligen Indexfunktion.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "matrizen")),
            suchbegriffe = setOf("2", "Matrix", "Matrix aus Einzelwerten, Zeilen, Spalten oder einer zweistelligen Indexfunktion.", "Matrizen", "breite", "eintrag_0_0", "eintrag_0_1", "eintrag_1_0", "eintrag_1_1", "einzelEingaben", "erzeugungsArt", "höhe", "mathematik.matrix", "mathematik.zahl", "matrix"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.matrix"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

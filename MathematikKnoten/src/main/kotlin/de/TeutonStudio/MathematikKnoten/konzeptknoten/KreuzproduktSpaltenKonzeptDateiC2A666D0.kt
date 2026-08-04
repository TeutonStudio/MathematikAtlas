package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object KreuzproduktSpaltenKonzeptDateiC2A666D0 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.kreuzproduktSpalte|Kreuzprodukt (Spalten)|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.kreuzproduktSpalte|Kreuzprodukt (Spalten)"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Kreuzprodukt (Spalten)",
            kurzbeschreibung = "Kreuzprodukt reeller 3-Spaltenvektoren.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("Kreuzprodukt (Spalten)", "Kreuzprodukt reeller 3-Spaltenvektoren.", "Vektoren", "a", "b", "mathematik.kreuzproduktSpalte", "mathematik.vektor.spalte", "vektor"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.kreuzproduktSpalte"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

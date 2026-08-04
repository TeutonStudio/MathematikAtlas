package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object SpaltenmethodeDifferentierenKonzeptDateiE0E646C8 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.spaltenMethodeDifferentieren|Spaltenmethode differentieren|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.spaltenMethodeDifferentieren|Spaltenmethode differentieren"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Spaltenmethode differentieren",
            kurzbeschreibung = "Differentiert die Komponenten einer Spaltenvektormethode.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("Abbildungen", "Differentiert die Komponenten einer Spaltenvektormethode.", "Spaltenmethode differentieren", "mathematik.methode", "mathematik.spaltenMethodeDifferentieren", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.spaltenMethodeDifferentieren"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

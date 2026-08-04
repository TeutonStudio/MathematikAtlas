package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object AussagenmethodeAnwendenKonzeptDatei3E9CB1B7 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.methodenAnwendung|Aussagenmethode anwenden|methodenAnwendung.ergebnisArt=mathematik.aussage")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.methodenAnwendung|methodenAnwendung.ergebnisArt=mathematik.aussage|Aussagenmethode anwenden"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Aussagenmethode anwenden",
            kurzbeschreibung = "Wendet eine einwertige Methode auf ein Argument an.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("Aussagenmethode anwenden", "Methoden", "Wendet eine einwertige Methode auf ein Argument an.", "argument", "mathematik.aussage", "mathematik.methode", "mathematik.methodenAnwendung", "mathematik.objekt", "methode", "methodenAnwendung.ergebnisArt", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.methodenAnwendung"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

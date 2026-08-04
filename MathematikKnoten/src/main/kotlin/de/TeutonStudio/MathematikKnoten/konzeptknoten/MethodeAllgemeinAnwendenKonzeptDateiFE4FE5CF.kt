package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MethodeAllgemeinAnwendenKonzeptDateiFE4FE5CF : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.methodenAnwendung|Methode allgemein anwenden|methodenAnwendung.ergebnisArt=mathematik.objekt")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.methodenAnwendung|methodenAnwendung.ergebnisArt=mathematik.objekt|Methode allgemein anwenden"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Methode allgemein anwenden",
            kurzbeschreibung = "Wendet eine einwertige Methode auf ein Argument an.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("Methode allgemein anwenden", "Methoden", "Wendet eine einwertige Methode auf ein Argument an.", "argument", "mathematik.methode", "mathematik.methodenAnwendung", "mathematik.objekt", "methode", "methodenAnwendung.ergebnisArt", "wert"),
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

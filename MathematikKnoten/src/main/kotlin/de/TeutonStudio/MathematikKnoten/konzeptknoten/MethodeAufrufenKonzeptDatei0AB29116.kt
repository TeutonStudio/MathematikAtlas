package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MethodeAufrufenKonzeptDatei0AB29116 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.methodeAufrufen|Methode aufrufen|festeEingänge=2;methodenAnwendung.ergebnisArt=mathematik.objekt")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.methodeAufrufen|festeEingänge=2|methodenAnwendung.ergebnisArt=mathematik.objekt|Methode aufrufen"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Methode aufrufen",
            kurzbeschreibung = "Wendet eine Methode geordnet auf konkrete oder symbolische Argumente an.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("2", "Methode aufrufen", "Methoden", "Wendet eine Methode geordnet auf konkrete oder symbolische Argumente an.", "argument1", "argument2", "festeEingänge", "mathematik.methode", "mathematik.methodeAufrufen", "mathematik.objekt", "methode", "methodenAnwendung.ergebnisArt", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.methodeAufrufen"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

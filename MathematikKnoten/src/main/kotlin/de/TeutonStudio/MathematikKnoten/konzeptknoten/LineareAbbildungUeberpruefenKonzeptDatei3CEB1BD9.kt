package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object LineareAbbildungUeberpruefenKonzeptDatei3CEB1BD9 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.begriff.lineareAbbildung|Lineare Abbildung überprüfen|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.begriff.lineareAbbildung|Lineare Abbildung überprüfen"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Lineare Abbildung überprüfen",
            kurzbeschreibung = "Prüft eine Methode zwischen zwei nachgewiesenen Vektorräumen auf Additivität und Homogenität.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("Lineare Abbildung überprüfen", "Lineare Algebra: Begriffe", "Prüft eine Methode zwischen zwei nachgewiesenen Vektorräumen auf Additivität und Homogenität.", "aussage", "definitionsraum", "mathematik.aussage", "mathematik.begriff.lineareAbbildung", "mathematik.methode", "methode", "zielraum"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.begriff.lineareAbbildung"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

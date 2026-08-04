package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object ZeilenvektorKonzeptDateiCD2FAD60 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.zeilenVektor|Zeilenvektor|festeEingänge=2;operatorAnzeige=wert")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.zeilenVektor|festeEingänge=2|operatorAnzeige=wert|Zeilenvektor"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Zeilenvektor",
            kurzbeschreibung = "Zeilenvektor aus dynamischen Zahl-Eingängen.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("2", "Vektoren", "Zeilenvektor", "Zeilenvektor aus dynamischen Zahl-Eingängen.", "a", "b", "festeEingänge", "mathematik.vektor.zeile", "mathematik.zahl", "mathematik.zeilenVektor", "operatorAnzeige", "vektor", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.zeilenVektor"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

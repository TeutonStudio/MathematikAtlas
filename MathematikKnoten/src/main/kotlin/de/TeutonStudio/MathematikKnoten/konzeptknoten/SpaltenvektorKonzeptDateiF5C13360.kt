package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object SpaltenvektorKonzeptDateiF5C13360 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.vektor|Spaltenvektor|festeEingänge=2;operatorAnzeige=wert")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.vektor|festeEingänge=2|operatorAnzeige=wert|Spaltenvektor"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Spaltenvektor",
            kurzbeschreibung = "Spaltenvektor aus dynamischen Zahl-Eingängen.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("2", "Spaltenvektor", "Spaltenvektor aus dynamischen Zahl-Eingängen.", "Vektoren", "a", "b", "festeEingänge", "mathematik.vektor", "mathematik.vektor.spalte", "mathematik.zahl", "operatorAnzeige", "vektor", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.vektor"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

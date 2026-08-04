package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MengenfilterKonzeptDatei50031874 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.mengenfilter|Mengenfilter|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.mengenfilter|Mengenfilter"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Mengenfilter",
            kurzbeschreibung = "Behält genau die Elemente einer Menge, für die eine einstellige Methode eine wahre Aussage liefert.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Behält genau die Elemente einer Menge, für die eine einstellige Methode eine wahre Aussage liefert.", "Mengen", "Mengenfilter", "mathematik.menge", "mathematik.mengenfilter", "mathematik.methode", "menge", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.mengenfilter"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

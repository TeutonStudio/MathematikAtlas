package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object VereinigungKonzeptDatei0C0E4F66 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.vereinigung|Vereinigung|festeEingänge=2;operatorAnzeige=wert")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.vereinigung|festeEingänge=2|operatorAnzeige=wert|Vereinigung"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Vereinigung",
            kurzbeschreibung = "Vereinigt zwei Mengen.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengenoperationen")),
            suchbegriffe = setOf("2", "Mengen", "Vereinigt zwei Mengen.", "Vereinigung", "a", "b", "festeEingänge", "mathematik.menge", "mathematik.vereinigung", "menge", "operatorAnzeige", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.vereinigung"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

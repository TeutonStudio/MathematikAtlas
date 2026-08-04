package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object KartesischesProduktKonzeptDatei4F5DFA58 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.kartesischesProdukt|Kartesisches Produkt|festeEingänge=2;operatorAnzeige=wert")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.kartesischesProdukt|festeEingänge=2|operatorAnzeige=wert|Kartesisches Produkt"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Kartesisches Produkt",
            kurzbeschreibung = "Bildet das kartesische Produkt von zwei oder mehr Mengen.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengenoperationen")),
            suchbegriffe = setOf("2", "Bildet das kartesische Produkt von zwei oder mehr Mengen.", "Kartesisches Produkt", "Mengen", "a", "b", "festeEingänge", "mathematik.kartesischesProdukt", "mathematik.menge", "menge", "operatorAnzeige", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.kartesischesProdukt"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

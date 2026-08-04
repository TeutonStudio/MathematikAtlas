package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object IteriertesKartesischesProduktKonzeptDateiF534B8DE : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.iteriertesKartesischesProdukt|Iteriertes kartesisches Produkt|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.iteriertesKartesischesProdukt|Iteriertes kartesisches Produkt"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Iteriertes kartesisches Produkt",
            kurzbeschreibung = "Bildet das kartesische Produkt der Mengenwerte einer Methode über einer Indexmenge.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengenoperationen")),
            suchbegriffe = setOf("Bildet das kartesische Produkt der Mengenwerte einer Methode über einer Indexmenge.", "Iteriertes kartesisches Produkt", "Mengen", "indexmenge", "mathematik.iteriertesKartesischesProdukt", "mathematik.menge", "mathematik.methode", "menge", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.iteriertesKartesischesProdukt"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

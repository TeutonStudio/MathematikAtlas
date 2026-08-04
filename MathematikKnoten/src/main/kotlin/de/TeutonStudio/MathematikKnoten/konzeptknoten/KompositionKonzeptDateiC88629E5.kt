package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object KompositionKonzeptDateiC88629E5 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.komposition|Komposition|festeEingänge=2;operatorAnzeige=wert")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.komposition|festeEingänge=2|operatorAnzeige=wert|Komposition"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Komposition",
            kurzbeschreibung = "Komponiert zwei oder mehr Methoden in sichtbarer Reihenfolge und prüft jeden Übergang semantisch.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("2", "Abbildungen", "Komponiert zwei oder mehr Methoden in sichtbarer Reihenfolge und prüft jeden Übergang semantisch.", "Komposition", "außen", "festeEingänge", "innen", "mathematik.komposition", "mathematik.methode", "methode", "operatorAnzeige", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.komposition"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

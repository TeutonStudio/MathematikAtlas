package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MaechtigkeitKonzeptDateiD03E6986 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.mächtigkeit|Mächtigkeit|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.mächtigkeit|Mächtigkeit"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Mächtigkeit",
            kurzbeschreibung = "Bestimmt endlich, abzählbar unendlich oder überabzählbar.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Bestimmt endlich, abzählbar unendlich oder überabzählbar.", "Mengen", "Mächtigkeit", "mathematik.menge", "mathematik.mächtigkeit", "mathematik.objekt", "menge", "mächtigkeit"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.mächtigkeit"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

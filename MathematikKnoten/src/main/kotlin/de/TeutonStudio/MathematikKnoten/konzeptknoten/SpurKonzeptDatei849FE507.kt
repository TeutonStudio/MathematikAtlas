package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object SpurKonzeptDatei849FE507 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.spur|Spur|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.spur|Spur"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Spur",
            kurzbeschreibung = "Summiert die Hauptdiagonale einer quadratischen Matrix.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "matrizen")),
            suchbegriffe = setOf("Matrizen", "Spur", "Summiert die Hauptdiagonale einer quadratischen Matrix.", "mathematik.matrix", "mathematik.spur", "mathematik.zahl", "matrix", "spur"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.spur"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

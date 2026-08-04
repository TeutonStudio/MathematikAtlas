package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object OffeneMengeKonzeptDatei75C049CE : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("geplant.topologie.offene-menge")
    override val varianten: Set<VariantenId> = emptySet()

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Offene Menge",
            kurzbeschreibung = "Grundbegriff der Topologie; eine erzeugbare Knotenvorlage ist noch nicht registriert.",
            fachPfade = setOf(FachPfad.von("topologie", "grundbegriffe")),
            suchbegriffe = setOf("Topologie", "Umgebung", "offene Menge"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Geplant,
            reifegrad = WissensReifegrad.Entwurf,
            knotenArten = emptySet(),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

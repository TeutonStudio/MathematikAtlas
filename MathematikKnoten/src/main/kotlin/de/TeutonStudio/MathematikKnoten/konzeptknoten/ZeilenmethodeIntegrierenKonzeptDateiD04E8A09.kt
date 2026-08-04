package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object ZeilenmethodeIntegrierenKonzeptDateiD04E8A09 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.zeilenMethodeIntegrieren|Zeilenmethode integrieren|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.zeilenMethodeIntegrieren|Zeilenmethode integrieren"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Zeilenmethode integrieren",
            kurzbeschreibung = "Integriert die Komponenten einer Zeilenvektormethode.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "differential-integral"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("Abbildungen", "Integriert die Komponenten einer Zeilenvektormethode.", "Zeilenmethode integrieren", "mathematik.methode", "mathematik.zeilenMethodeIntegrieren", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.zeilenMethodeIntegrieren"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

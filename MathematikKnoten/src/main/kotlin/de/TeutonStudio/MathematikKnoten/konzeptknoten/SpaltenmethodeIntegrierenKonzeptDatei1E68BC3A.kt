package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object SpaltenmethodeIntegrierenKonzeptDatei1E68BC3A : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.spaltenMethodeIntegrieren|Spaltenmethode integrieren|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.spaltenMethodeIntegrieren|Spaltenmethode integrieren"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Spaltenmethode integrieren",
            kurzbeschreibung = "Integriert die Komponenten einer Spaltenvektormethode.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "differential-integral"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("Abbildungen", "Integriert die Komponenten einer Spaltenvektormethode.", "Spaltenmethode integrieren", "mathematik.methode", "mathematik.spaltenMethodeIntegrieren", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.spaltenMethodeIntegrieren"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

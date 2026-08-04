package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MethodeIntegrierenKonzeptDatei0D61CF49 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.methodenIntegrieren|Methode integrieren|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.methodenIntegrieren|Methode integrieren"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Methode integrieren",
            kurzbeschreibung = "Integriert eine skalare Methode bei integralfähigem Wertevorrat.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "differential-integral"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("Abbildungen", "Integriert eine skalare Methode bei integralfähigem Wertevorrat.", "Methode integrieren", "mathematik.methode", "mathematik.methodenIntegrieren", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.methodenIntegrieren"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

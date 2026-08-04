package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object MethodeDifferentierenKonzeptDatei6C112126 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.methodenDifferentieren|Methode differentieren|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.methodenDifferentieren|Methode differentieren"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Methode differentieren",
            kurzbeschreibung = "Differentiert eine skalare Methode bei differentialfähigem Wertevorrat.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("Abbildungen", "Differentiert eine skalare Methode bei differentialfähigem Wertevorrat.", "Methode differentieren", "mathematik.methode", "mathematik.methodenDifferentieren", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.methodenDifferentieren"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

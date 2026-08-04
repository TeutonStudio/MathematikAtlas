package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object AllgemeinerParameterKonzeptDatei5724C3EC : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.allgemeinerParameter|Allgemeiner Parameter|name=a")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.allgemeinerParameter|name=a|Allgemeiner Parameter"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Allgemeiner Parameter",
            kurzbeschreibung = "Freier, nicht auf Zahlterme beschränkter Parameter für allgemeine Methoden.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("Abbildungen", "Allgemeiner Parameter", "Freier, nicht auf Zahlterme beschränkter Parameter für allgemeine Methoden.", "a", "mathematik.allgemeinerParameter", "mathematik.objekt", "name", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.allgemeinerParameter"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

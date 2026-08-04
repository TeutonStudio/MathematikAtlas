package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object NegationKonzeptDatei3ED8965D : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.negation|Negation|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.negation|Negation"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Negation",
            kurzbeschreibung = "Kehrt den Wahrheitswert einer Aussage mit ¬ um.",
            fachPfade = setOf(FachPfad.von("logik", "aussagen")),
            suchbegriffe = setOf("Aussagen: Aussagenlogik", "Kehrt den Wahrheitswert einer Aussage mit ¬ um.", "Negation", "aussage", "mathematik.aussage", "mathematik.negation"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.negation"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

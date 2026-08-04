package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object DisjunktKonzeptDateiF321CB4A : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.disjunkt|Disjunkt|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.disjunkt|Disjunkt"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Disjunkt",
            kurzbeschreibung = "Prüft, ob zwei Mengen keinen gemeinsamen Wert besitzen.",
            fachPfade = setOf(FachPfad.von("logik", "aussagen"), FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Aussagen: Mengenprädikate", "Disjunkt", "Prüft, ob zwei Mengen keinen gemeinsamen Wert besitzen.", "aussage", "links", "mathematik.aussage", "mathematik.disjunkt", "mathematik.menge", "rechts"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.disjunkt"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

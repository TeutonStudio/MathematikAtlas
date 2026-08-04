package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object AufloesenKonzeptDatei2E01E8D3 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.gleichungLösen|Auflösen|variable=x")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.gleichungLösen|variable=x|Auflösen"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Auflösen",
            kurzbeschreibung = "Bestimmt die Lösungsmenge einer Relation. Exakt lösbare Relationen werden berechnet; andernfalls bleibt die Lösungsmenge symbolisch definiert.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen")),
            suchbegriffe = setOf("Algebra", "Auflösen", "Bestimmt die Lösungsmenge einer Relation. Exakt lösbare Relationen werden berechnet; andernfalls bleibt die Lösungsmenge symbolisch definiert.", "lösungsmenge", "mathematik.aussage", "mathematik.gleichungLösen", "mathematik.menge", "relation", "variable", "x"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.gleichungLösen"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

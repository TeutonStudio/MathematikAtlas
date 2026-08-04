package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object ZufallsvariableKonzeptDatei70790B00 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("geplant.stochastik.zufallsvariable")
    override val varianten: Set<VariantenId> = emptySet()

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Zufallsvariable",
            kurzbeschreibung = "Messbare Abbildung eines Wahrscheinlichkeitsraums; noch nicht als Knoten verfügbar.",
            fachPfade = setOf(FachPfad.von("stochastik", "grundbegriffe")),
            suchbegriffe = setOf("Stochastik", "Wahrscheinlichkeit", "Zufallsvariable"),
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

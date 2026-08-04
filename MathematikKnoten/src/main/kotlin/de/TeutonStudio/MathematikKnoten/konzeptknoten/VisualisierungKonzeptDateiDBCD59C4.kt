package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object VisualisierungKonzeptDateiDBCD59C4 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.visualisierung|Visualisierung|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.visualisierung|Visualisierung"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Visualisierung",
            kurzbeschreibung = "Stellt Mengen als numerische Approximation in R² oder R³ dar und reicht sie unverändert weiter.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen")),
            suchbegriffe = setOf("Stellt Mengen als numerische Approximation in R² oder R³ dar und reicht sie unverändert weiter.", "Visualisierung", "mathematik.menge", "mathematik.visualisierung", "menge"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.visualisierung"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

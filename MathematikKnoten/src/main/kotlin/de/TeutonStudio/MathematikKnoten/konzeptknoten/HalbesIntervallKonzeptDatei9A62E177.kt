package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object HalbesIntervallKonzeptDatei9A62E177 : ExpliziteKonzeptDatei {
    override val id = WissensId("mathematik.halbesIntervall|Halbes Intervall|")
    override val varianten = setOf(MathematikKnotenVorlagen.HalbesIntervall.stabileVariantenId())
    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passende = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Halbes Intervall",
            kurzbeschreibung = "Reeller Halbstrahl mit einseitiger offener oder geschlossener Grenze.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Halbes Intervall", "Halbstrahl", "Untergrenze", "Obergrenze", "offen", "geschlossen"),
            aliase = setOf("Reeller Halbstrahl"),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.halbesIntervall"),
            varianten = passende.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passende,
            karten = StatischeKonzeptKarten.alle.filter { it.id.startsWith("mathematik.halbesIntervall|") },
        )
    }
}

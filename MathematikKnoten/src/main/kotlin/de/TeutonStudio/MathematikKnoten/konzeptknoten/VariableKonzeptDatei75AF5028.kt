package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object VariableKonzeptDatei75AF5028 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.variable|Variable|name=x;werteVorrat=R")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.variable|name=x|werteVorrat=R|Variable"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Variable",
            kurzbeschreibung = "Freie mathematische Variable mit im Inspector definiertem Wertevorrat.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen")),
            suchbegriffe = setOf("Freie mathematische Variable mit im Inspector definiertem Wertevorrat.", "R", "Rechnen", "Variable", "mathematik.variable", "mathematik.zahl", "name", "wert", "werteVorrat", "x"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.variable"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

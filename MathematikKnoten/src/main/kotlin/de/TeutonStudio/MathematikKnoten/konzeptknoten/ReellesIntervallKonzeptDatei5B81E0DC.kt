package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object ReellesIntervallKonzeptDatei5B81E0DC : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.reellesIntervall|Reelles Intervall|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.reellesIntervall|Reelles Intervall"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Reelles Intervall",
            kurzbeschreibung = "Bildet ein reelles Intervall aus zwei nachweisbar reellen Grenzen und Aussagen über offene Randpunkte.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Bildet ein reelles Intervall aus zwei nachweisbar reellen Grenzen und Aussagen über offene Randpunkte.", "Mengen", "Reelles Intervall", "links", "linksOffen", "mathematik.aussage", "mathematik.menge", "mathematik.reellesIntervall", "mathematik.zahl", "menge", "rechts", "rechtsOffen"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.reellesIntervall"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

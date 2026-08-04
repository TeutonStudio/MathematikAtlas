package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object TeilOderGleichmengeKonzeptDatei51777C6D : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.teilOderGleichmenge|Teil- oder Gleichmenge|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.teilOderGleichmenge|Teil- oder Gleichmenge"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Teil- oder Gleichmenge",
            kurzbeschreibung = "Prüft ⊆.",
            fachPfade = setOf(FachPfad.von("logik", "aussagen"), FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Aussagen: Mengenprädikate", "Prüft ⊆.", "Teil- oder Gleichmenge", "aussage", "links", "mathematik.aussage", "mathematik.menge", "mathematik.teilOderGleichmenge", "rechts"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.teilOderGleichmenge"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

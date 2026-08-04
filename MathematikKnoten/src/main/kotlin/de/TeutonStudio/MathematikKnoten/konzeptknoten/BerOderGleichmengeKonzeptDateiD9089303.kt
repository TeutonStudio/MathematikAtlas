package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object BerOderGleichmengeKonzeptDateiD9089303 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.überOderGleichmenge|Über- oder Gleichmenge|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.überOderGleichmenge|Über- oder Gleichmenge"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Über- oder Gleichmenge",
            kurzbeschreibung = "Prüft ⊇.",
            fachPfade = setOf(FachPfad.von("logik", "aussagen"), FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Aussagen: Mengenprädikate", "Prüft ⊇.", "aussage", "links", "mathematik.aussage", "mathematik.menge", "mathematik.überOderGleichmenge", "rechts", "Über- oder Gleichmenge"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.überOderGleichmenge"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

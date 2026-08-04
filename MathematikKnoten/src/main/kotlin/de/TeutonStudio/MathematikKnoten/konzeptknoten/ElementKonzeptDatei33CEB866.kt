package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object ElementKonzeptDatei33CEB866 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.element|Element|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.element|Element"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Element",
            kurzbeschreibung = "Prüft, ob ein Objekt Element einer Menge ist.",
            fachPfade = setOf(FachPfad.von("logik", "aussagen"), FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Aussagen: Mengenprädikate", "Element", "Prüft, ob ein Objekt Element einer Menge ist.", "aussage", "links", "mathematik.aussage", "mathematik.element", "mathematik.menge", "mathematik.objekt", "rechts"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.element"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

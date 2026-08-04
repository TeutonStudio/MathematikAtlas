package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object QuivalenzKonzeptDatei8816493A : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.äquivalenz|Äquivalenz|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.äquivalenz|Äquivalenz"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Äquivalenz",
            kurzbeschreibung = "Bildet A ⇔ B.",
            fachPfade = setOf(FachPfad.von("logik", "aussagen")),
            suchbegriffe = setOf("Aussage", "Bildet A ⇔ B.", "a", "aussage", "b", "mathematik.aussage", "mathematik.äquivalenz", "Äquivalenz"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.äquivalenz"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

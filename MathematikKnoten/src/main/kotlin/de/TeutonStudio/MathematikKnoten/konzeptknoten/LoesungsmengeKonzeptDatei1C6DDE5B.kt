package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object LoesungsmengeKonzeptDatei1C6DDE5B : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.lösungsmenge|Lösungsmenge|automatisch=true;grundmengen=R;variablen=")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.lösungsmenge|automatisch=true|grundmengen=R|variablen=|Lösungsmenge"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Lösungsmenge",
            kurzbeschreibung = "Bildet eine symbolische Menge aller Variablenwerte, die eine Aussage erfüllen.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Bildet eine symbolische Menge aller Variablenwerte, die eine Aussage erfüllen.", "Lösungsmenge", "Mengen", "R", "automatisch", "bedingung", "grundmengen", "mathematik.aussage", "mathematik.lösungsmenge", "mathematik.menge", "menge", "true", "variablen"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.lösungsmenge"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object KartenEingangKonzeptDatei9196A9B8 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.kartenEingang|Karten-Eingang|name=x")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.kartenEingang|name=x|Karten-Eingang"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Karten-Eingang",
            kurzbeschreibung = "Öffentlicher Parameter einer wiederverwendbaren Karte.",
            fachPfade = setOf(FachPfad.von("eigene-karten")),
            suchbegriffe = setOf("Eigene Karten", "Karten-Eingang", "mathematik.kartenEingang", "mathematik.objekt", "name", "wert", "x", "Öffentlicher Parameter einer wiederverwendbaren Karte."),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.kartenEingang"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

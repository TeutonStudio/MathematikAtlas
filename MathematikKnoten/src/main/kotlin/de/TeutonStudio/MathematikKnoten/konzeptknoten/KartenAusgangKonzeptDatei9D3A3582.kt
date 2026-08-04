package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object KartenAusgangKonzeptDatei9D3A3582 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.kartenAusgang|Karten-Ausgang|name=ergebnis")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.kartenAusgang|name=ergebnis|Karten-Ausgang"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Karten-Ausgang",
            kurzbeschreibung = "Öffentliche Ausgabe einer wiederverwendbaren Karte.",
            fachPfade = setOf(FachPfad.von("eigene-karten")),
            suchbegriffe = setOf("Eigene Karten", "Karten-Ausgang", "ergebnis", "mathematik.kartenAusgang", "mathematik.objekt", "name", "wert", "Öffentliche Ausgabe einer wiederverwendbaren Karte."),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.kartenAusgang"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

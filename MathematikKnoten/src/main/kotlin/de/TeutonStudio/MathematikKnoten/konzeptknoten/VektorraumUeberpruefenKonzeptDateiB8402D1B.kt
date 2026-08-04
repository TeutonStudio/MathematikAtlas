package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object VektorraumUeberpruefenKonzeptDateiB8402D1B : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.begriff.vektorraum|Vektorraum überprüfen|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.begriff.vektorraum|Vektorraum überprüfen"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Vektorraum überprüfen",
            kurzbeschreibung = "Prüft Trägermenge, Addition und skalare Multiplikation gegen die Vektorraumaxiome.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("Lineare Algebra: Begriffe", "Prüft Trägermenge, Addition und skalare Multiplikation gegen die Vektorraumaxiome.", "Vektorraum überprüfen", "addition", "aussage", "mathematik.aussage", "mathematik.begriff.vektorraum", "mathematik.menge", "mathematik.methode", "menge", "skalareMultiplikation"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.begriff.vektorraum"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

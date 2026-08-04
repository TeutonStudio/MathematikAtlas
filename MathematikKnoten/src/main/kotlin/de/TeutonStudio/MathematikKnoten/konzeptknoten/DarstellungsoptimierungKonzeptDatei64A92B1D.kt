package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object DarstellungsoptimierungKonzeptDatei64A92B1D : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.darstellungsoptimierung|Darstellungsoptimierung|latex=u")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.darstellungsoptimierung|latex=u|Darstellungsoptimierung"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Darstellungsoptimierung",
            kurzbeschreibung = "Reicht einen beliebigen Wert unverändert weiter und ersetzt seine nachfolgende LaTeX-Darstellung.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen")),
            suchbegriffe = setOf("Darstellung", "Darstellungsoptimierung", "Reicht einen beliebigen Wert unverändert weiter und ersetzt seine nachfolgende LaTeX-Darstellung.", "latex", "mathematik.darstellungsoptimierung", "mathematik.objekt", "u", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.darstellungsoptimierung"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

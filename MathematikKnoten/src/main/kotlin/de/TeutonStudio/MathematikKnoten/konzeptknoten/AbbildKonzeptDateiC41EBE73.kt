package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object AbbildKonzeptDateiC41EBE73 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.abbild|Abbild|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.abbild|Bildmenge"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Bildmenge",
            kurzbeschreibung = "Bestimmt die Bildmenge einer Menge unter einer einwertigen Methode: f(M) = { f(x) : x ∈ M }.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen"), FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Bildmenge", "Abbild", "f(M)", "Bestimmt die Bildmenge einer Menge unter einer einwertigen Methode.", "Mengen", "mathematik.abbild", "mathematik.menge", "mathematik.methode", "menge", "methode"),
            aliase = setOf("Abbild"),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.abbild"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

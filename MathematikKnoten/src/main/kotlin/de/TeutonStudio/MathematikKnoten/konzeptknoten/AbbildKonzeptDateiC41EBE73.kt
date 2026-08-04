package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object AbbildKonzeptDateiC41EBE73 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.abbild|Abbild|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.abbild|Abbild"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Abbild",
            kurzbeschreibung = "Bildet eine Menge mit einer einwertigen Methode ab: f[M] = { f(x) : x ∈ M }.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen"), FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Abbild", "Bildet eine Menge mit einer einwertigen Methode ab: f[M] = { f(x) : x ∈ M }.", "Mengen", "mathematik.abbild", "mathematik.menge", "mathematik.methode", "menge", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.abbild"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

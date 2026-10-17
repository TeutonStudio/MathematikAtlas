package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object ZeilenmethodeDifferentierenKonzeptDatei0958FE38 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.zeilenMethodeDifferentieren|Zeilenmethode differentieren|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.methodenDifferentieren|methodenAnforderung.methode.ergebnisArt=mathematik.vektor.zeile|orientierung=zeile|Vektormethode differentieren"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Zeilenmethode differentieren",
            kurzbeschreibung = "Differentiert die Komponenten einer Zeilenvektormethode.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("Abbildungen", "Differentiert die Komponenten einer Zeilenvektormethode.", "Zeilenmethode differentieren", "mathematik.methode", "mathematik.zeilenMethodeDifferentieren", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.methodenDifferentieren"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object KreuzproduktZeilenKonzeptDatei4F9C6B31 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.kreuzproduktZeile|Kreuzprodukt (Zeilen)|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.vektorrechner|achse=1|integral.massModus=AUTO|integral.massSymbol=\\mu|metrik=metrik.l2|operator=vektor.kreuzprodukt|orientierung=zeile|strukturAusgabe=tupel|Kreuzprodukt (Zeilen)"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Kreuzprodukt (Zeilen)",
            kurzbeschreibung = "Kreuzprodukt reeller 3-Zeilenvektoren.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("Kreuzprodukt (Zeilen)", "Kreuzprodukt reeller 3-Zeilenvektoren.", "Vektoren", "a", "b", "mathematik.kreuzproduktZeile", "mathematik.vektor.zeile", "vektor"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.vektorrechner"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

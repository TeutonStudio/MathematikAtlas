package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object VektorZuPolynomKonzeptDatei9DB43D81 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.vektorZuPolynom|Vektor zu Polynom|variable=x")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.vektorZuPolynom|variable=x|Vektor zu Polynom"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Vektor zu Polynom",
            kurzbeschreibung = "Liest einen Vektor als Koeffizienten c₀, …, cₙ eines Polynoms.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("Liest einen Vektor als Koeffizienten c₀, …, cₙ eines Polynoms.", "Vektor zu Polynom", "Vektoren", "mathematik.vektor", "mathematik.vektorZuPolynom", "mathematik.zahl", "variable", "vektor", "wert", "x"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.vektorZuPolynom"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

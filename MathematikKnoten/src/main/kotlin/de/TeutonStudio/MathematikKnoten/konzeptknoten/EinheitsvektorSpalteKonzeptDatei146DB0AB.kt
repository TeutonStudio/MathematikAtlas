package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object EinheitsvektorSpalteKonzeptDatei146DB0AB : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.einheitsSpalte|Einheitsvektor (Spalte)|standardwert.dimension=3;standardwert.position=0")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.einheitsSpalte|standardwert.dimension=3|standardwert.position=0|Einheitsvektor (Spalte)"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Einheitsvektor (Spalte)",
            kurzbeschreibung = "Erzeugt einen Standardbasisvektor aus nullbasierter Position und positiver Dimension.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("0", "3", "Einheitsvektor (Spalte)", "Erzeugt einen Standardbasisvektor aus nullbasierter Position und positiver Dimension.", "Vektoren", "dimension", "mathematik.einheitsSpalte", "mathematik.vektor.spalte", "mathematik.zahl", "position", "standardwert.dimension", "standardwert.position", "vektor"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.einheitsSpalte"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

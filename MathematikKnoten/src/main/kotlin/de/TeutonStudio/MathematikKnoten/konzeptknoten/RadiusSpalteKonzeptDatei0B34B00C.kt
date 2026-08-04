package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object RadiusSpalteKonzeptDatei0B34B00C : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.vektorRadiusSpalte|Radius (Spalte)|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.vektorRadiusSpalte|Radius (Spalte)"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Radius (Spalte)",
            kurzbeschreibung = "Euklidische Norm eines Spaltenvektors.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("Euklidische Norm eines Spaltenvektors.", "Radius (Spalte)", "Vektoren", "mathematik.vektor.spalte", "mathematik.vektorRadiusSpalte", "mathematik.zahl", "vektor", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.vektorRadiusSpalte"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

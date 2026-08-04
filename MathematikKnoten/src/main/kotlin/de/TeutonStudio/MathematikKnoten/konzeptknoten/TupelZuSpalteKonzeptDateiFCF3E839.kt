package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object TupelZuSpalteKonzeptDateiFCF3E839 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.tupelZuSpalte|Tupel zu Spalte|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.tupelZuSpalte|Tupel zu Spalte"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Tupel zu Spalte",
            kurzbeschreibung = "Erzeugt aus einem Zahlentupel einen Spaltenvektor.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("Erzeugt aus einem Zahlentupel einen Spaltenvektor.", "Tupel zu Spalte", "Vektoren", "mathematik.tupel", "mathematik.tupelZuSpalte", "mathematik.vektor.spalte", "tupel", "vektor"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.tupelZuSpalte"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

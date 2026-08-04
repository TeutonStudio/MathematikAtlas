package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object TupelZuZeileKonzeptDatei74C8B1FF : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.tupelZuZeile|Tupel zu Zeile|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.tupelZuZeile|Tupel zu Zeile"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Tupel zu Zeile",
            kurzbeschreibung = "Erzeugt aus einem Zahlentupel einen Zeilenvektor.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("Erzeugt aus einem Zahlentupel einen Zeilenvektor.", "Tupel zu Zeile", "Vektoren", "mathematik.tupel", "mathematik.tupelZuZeile", "mathematik.vektor.zeile", "tupel", "vektor"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.tupelZuZeile"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

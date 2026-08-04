package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object RadiusZeileKonzeptDatei63B931F8 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.vektorRadiusZeile|Radius (Zeile)|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.vektorRadiusZeile|Radius (Zeile)"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Radius (Zeile)",
            kurzbeschreibung = "Euklidische Norm eines Zeilenvektors.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("Euklidische Norm eines Zeilenvektors.", "Radius (Zeile)", "Vektoren", "mathematik.vektor.zeile", "mathematik.vektorRadiusZeile", "mathematik.zahl", "vektor", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.vektorRadiusZeile"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

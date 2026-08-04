package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object EinzelmengeKonzeptDateiE95AFB12 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.einzelmenge|Einzelmenge|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.einzelmenge|Einzelmenge"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Einzelmenge",
            kurzbeschreibung = "Bildet aus einem beliebigen mathematischen Objekt die Menge, die genau dieses Element enthält.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Bildet aus einem beliebigen mathematischen Objekt die Menge, die genau dieses Element enthält.", "Einzelmenge", "Mengen", "element", "mathematik.einzelmenge", "mathematik.menge", "mathematik.objekt", "menge"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.einzelmenge"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

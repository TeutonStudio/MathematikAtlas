package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object TensorproduktKonzeptDatei21C3C57C : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.tensorprodukt|Tensorprodukt|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.tensorprodukt|Tensorprodukt"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Tensorprodukt",
            kurzbeschreibung = "Verkettet die Achsen zweier tensorartig betrachteter Objekte in fester Links-rechts-Reihenfolge.",
            fachPfade = setOf(FachPfad.von("lineare-algebra", "tensoren")),
            suchbegriffe = setOf("Tensoren", "Tensorprodukt", "Verkettet die Achsen zweier tensorartig betrachteter Objekte in fester Links-rechts-Reihenfolge.", "links", "mathematik.objekt", "mathematik.tensorprodukt", "rechts", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.tensorprodukt"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

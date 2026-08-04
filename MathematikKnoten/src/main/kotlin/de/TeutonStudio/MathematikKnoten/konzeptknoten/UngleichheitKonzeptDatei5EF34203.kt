package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object UngleichheitKonzeptDatei5EF34203 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.ungleichheit|Ungleichheit|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.ungleichheit|Ungleichheit"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Ungleichheit",
            kurzbeschreibung = "Prüft, ob zwei mathematische Objekte verschieden sind.",
            fachPfade = setOf(FachPfad.von("logik", "praedikate")),
            suchbegriffe = setOf("Aussagen: Aussagenprädikate", "Prüft, ob zwei mathematische Objekte verschieden sind.", "Ungleichheit", "aussage", "links", "mathematik.aussage", "mathematik.objekt", "mathematik.ungleichheit", "rechts"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.ungleichheit"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

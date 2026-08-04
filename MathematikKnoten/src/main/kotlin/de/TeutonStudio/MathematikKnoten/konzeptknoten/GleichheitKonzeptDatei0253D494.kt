package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object GleichheitKonzeptDatei0253D494 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.gleichheit|Gleichheit|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.gleichheit|Gleichheit"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Gleichheit",
            kurzbeschreibung = "Vergleicht zwei mathematische Objekte.",
            fachPfade = setOf(FachPfad.von("logik", "praedikate")),
            suchbegriffe = setOf("Aussagen: Aussagenprädikate", "Gleichheit", "Vergleicht zwei mathematische Objekte.", "aussage", "links", "mathematik.aussage", "mathematik.gleichheit", "mathematik.objekt", "rechts"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.gleichheit"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

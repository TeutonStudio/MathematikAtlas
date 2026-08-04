package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object ZahlkonstanteKonzeptDatei6894F709 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("konzept.zahlkonstante")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.zahl|wert=2|Zahl"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Zahlkonstante",
            kurzbeschreibung = "Exakte Zahlkonstante mit kanonischer rationaler Darstellung und typisiertem Zahlenausgang.",
            fachPfade = setOf(FachPfad.von("algebra", "zahlen")),
            suchbegriffe = setOf("2", "Exakte ganze oder rationale Zahl.", "Konstante", "Rechnen", "Zahl", "ganz", "mathematik.zahl", "rational", "wert"),
            aliase = setOf("mathematik.zahl|Zahl|wert=2"),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.zahl"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

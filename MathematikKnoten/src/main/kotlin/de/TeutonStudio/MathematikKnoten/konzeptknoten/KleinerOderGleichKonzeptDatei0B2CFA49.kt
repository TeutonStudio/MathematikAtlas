package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object KleinerOderGleichKonzeptDatei0B2CFA49 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.ordnungsrelation|Kleiner oder gleich|relation=kleinerGleich")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.ordnungsrelation|relation=kleinerGleich|Kleiner oder gleich"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Kleiner oder gleich",
            kurzbeschreibung = "Vergleicht zwei Zahlterme mit ≤.",
            fachPfade = setOf(FachPfad.von("logik", "praedikate")),
            suchbegriffe = setOf("Aussagen: Zahlenprädikate", "Kleiner oder gleich", "Vergleicht zwei Zahlterme mit ≤.", "aussage", "kleinerGleich", "links", "mathematik.aussage", "mathematik.ordnungsrelation", "mathematik.zahl", "rechts", "relation"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.ordnungsrelation"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

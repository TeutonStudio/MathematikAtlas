package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object GroesserOderGleichKonzeptDateiEA21E5BD : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.ordnungsrelation|Größer oder gleich|relation=größerGleich")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.ordnungsrelation|relation=größerGleich|Größer oder gleich"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Größer oder gleich",
            kurzbeschreibung = "Vergleicht zwei Zahlterme mit ≥.",
            fachPfade = setOf(FachPfad.von("logik", "praedikate")),
            suchbegriffe = setOf("Aussagen: Zahlenprädikate", "Größer oder gleich", "Vergleicht zwei Zahlterme mit ≥.", "aussage", "größerGleich", "links", "mathematik.aussage", "mathematik.ordnungsrelation", "mathematik.zahl", "rechts", "relation"),
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

package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object GroesserKonzeptDatei44D01B8F : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.ordnungsrelation|Größer|relation=größer")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.ordnungsrelation|relation=größer|Größer"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Größer",
            kurzbeschreibung = "Vergleicht zwei Zahlterme mit >.",
            fachPfade = setOf(FachPfad.von("logik", "praedikate")),
            suchbegriffe = setOf("Aussagen: Zahlenprädikate", "Größer", "Vergleicht zwei Zahlterme mit >.", "aussage", "größer", "links", "mathematik.aussage", "mathematik.ordnungsrelation", "mathematik.zahl", "rechts", "relation"),
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

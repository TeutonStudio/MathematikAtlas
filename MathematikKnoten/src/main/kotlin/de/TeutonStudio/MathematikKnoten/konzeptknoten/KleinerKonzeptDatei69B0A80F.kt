package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object KleinerKonzeptDatei69B0A80F : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.ordnungsrelation|Kleiner|relation=kleiner")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.ordnungsrelation|relation=kleiner|Kleiner"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Kleiner",
            kurzbeschreibung = "Vergleicht zwei Zahlterme mit <.",
            fachPfade = setOf(FachPfad.von("logik", "praedikate")),
            suchbegriffe = setOf("Aussagen: Zahlenprädikate", "Kleiner", "Vergleicht zwei Zahlterme mit <.", "aussage", "kleiner", "links", "mathematik.aussage", "mathematik.ordnungsrelation", "mathematik.zahl", "rechts", "relation"),
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

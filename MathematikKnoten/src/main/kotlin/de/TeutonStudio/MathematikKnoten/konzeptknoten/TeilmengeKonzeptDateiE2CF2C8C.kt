package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object TeilmengeKonzeptDateiE2CF2C8C : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.teilmenge|Teilmenge|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.teilmenge|Teilmenge"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Teilmenge",
            kurzbeschreibung = "Prüft die echte Teilmengenbeziehung.",
            fachPfade = setOf(FachPfad.von("logik", "aussagen"), FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Aussagen: Mengenprädikate", "Prüft die echte Teilmengenbeziehung.", "Teilmenge", "aussage", "links", "mathematik.aussage", "mathematik.menge", "mathematik.teilmenge", "rechts"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.teilmenge"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

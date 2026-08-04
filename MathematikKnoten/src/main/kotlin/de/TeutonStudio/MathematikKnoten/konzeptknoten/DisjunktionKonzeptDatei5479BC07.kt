package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object DisjunktionKonzeptDatei5479BC07 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.disjunktion|Disjunktion|festeEingänge=2;operatorAnzeige=wert")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.disjunktion|festeEingänge=2|operatorAnzeige=wert|Disjunktion"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Disjunktion",
            kurzbeschreibung = "Verknüpft zwei oder mehr Aussagen mit ∨.",
            fachPfade = setOf(FachPfad.von("logik", "aussagen")),
            suchbegriffe = setOf("2", "Aussage", "Disjunktion", "Verknüpft zwei oder mehr Aussagen mit ∨.", "a", "aussage", "b", "festeEingänge", "mathematik.aussage", "mathematik.disjunktion", "operatorAnzeige", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.disjunktion"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

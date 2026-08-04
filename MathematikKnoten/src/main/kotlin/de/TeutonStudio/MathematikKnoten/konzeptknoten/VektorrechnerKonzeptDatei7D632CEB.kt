package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object VektorrechnerKonzeptDatei7D632CEB : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.vektorrechner|Vektorrechner|operator=vektor.addition")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.vektorrechner|operator=vektor.addition|Vektorrechner"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Vektorrechner",
            kurzbeschreibung = "Einheitlicher Vektorrechner mit operatorabhängigen Anschlüssen und typisiertem CAS-Formelmodus.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen"), FachPfad.von("lineare-algebra", "vektoren")),
            suchbegriffe = setOf("Einheitlicher Vektorrechner mit operatorabhängigen Anschlüssen und typisiertem CAS-Formelmodus.", "Lineare Algebra", "Vektorrechner", "links", "mathematik.vektor", "mathematik.vektorrechner", "operator", "rechts", "vektor.addition", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.vektorrechner"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

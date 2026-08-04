package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object AussagesatzKonzeptDatei306805CB : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.aussagensatz|Aussagesatz|operator=aussage.negation")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.aussagensatz|operator=aussage.negation|Aussagesatz"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Aussagesatz",
            kurzbeschreibung = "Einheitlicher Aussagesatz mit operatorabhängigen Anschlüssen und typisiertem CAS-Formelmodus.",
            fachPfade = setOf(FachPfad.von("logik", "aussagen")),
            suchbegriffe = setOf("Aussagen: Aussagenlogik", "Aussagesatz", "Einheitlicher Aussagesatz mit operatorabhängigen Anschlüssen und typisiertem CAS-Formelmodus.", "aussage", "aussage.negation", "mathematik.aussage", "mathematik.aussagensatz", "operator", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.aussagensatz"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

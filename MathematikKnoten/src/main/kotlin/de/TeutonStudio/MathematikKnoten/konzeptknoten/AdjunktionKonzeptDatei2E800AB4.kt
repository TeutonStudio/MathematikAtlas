package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object AdjunktionKonzeptDatei2E800AB4 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.adjunktion|Adjunktion|festeEingänge=2;logikSemantik=xor;operatorAnzeige=wert")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.adjunktion|festeEingänge=2|logikSemantik=xor|operatorAnzeige=wert|Adjunktion"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Adjunktion",
            kurzbeschreibung = "Bildet das ausschließende Oder: (a ∨ b) ∧ ¬(a ∧ b).",
            fachPfade = setOf(FachPfad.von("logik", "aussagen")),
            suchbegriffe = setOf("2", "Adjunktion", "Aussagen: Aussagenlogik", "Bildet das ausschließende Oder: (a ∨ b) ∧ ¬(a ∧ b).", "a", "aussage", "b", "festeEingänge", "logikSemantik", "mathematik.adjunktion", "mathematik.aussage", "operatorAnzeige", "wert", "xor"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.adjunktion"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

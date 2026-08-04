package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object KonjunktionKonzeptDatei433D2483 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.konjunktion|Konjunktion|festeEingänge=2;operatorAnzeige=wert")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.konjunktion|festeEingänge=2|operatorAnzeige=wert|Konjunktion"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Konjunktion",
            kurzbeschreibung = "Verknüpft zwei oder mehr Aussagen mit ∧.",
            fachPfade = setOf(FachPfad.von("logik", "aussagen")),
            suchbegriffe = setOf("2", "Aussage", "Konjunktion", "Verknüpft zwei oder mehr Aussagen mit ∧.", "a", "aussage", "b", "festeEingänge", "mathematik.aussage", "mathematik.konjunktion", "operatorAnzeige", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.konjunktion"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

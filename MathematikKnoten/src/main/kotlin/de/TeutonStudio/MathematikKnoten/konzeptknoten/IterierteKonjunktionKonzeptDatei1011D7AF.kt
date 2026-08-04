package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object IterierteKonjunktionKonzeptDatei1011D7AF : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.iterierteAussagenverknüpfung|Iterierte Konjunktion|operator=konjunktion")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.iterierteAussagenverknüpfung|operator=konjunktion|Iterierte Konjunktion"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Iterierte Konjunktion",
            kurzbeschreibung = "Verknüpft die Aussagenwerte einer Methode über einer Indexmenge mit ∧.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen"), FachPfad.von("logik", "aussagen")),
            suchbegriffe = setOf("Iterierte Konjunktion", "Operatoren", "Verknüpft die Aussagenwerte einer Methode über einer Indexmenge mit ∧.", "aussage", "indexmenge", "konjunktion", "mathematik.aussage", "mathematik.iterierteAussagenverknüpfung", "mathematik.menge", "mathematik.methode", "methode", "operator"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.iterierteAussagenverknüpfung"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

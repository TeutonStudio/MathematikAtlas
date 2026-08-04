package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object IterierteDisjunktionKonzeptDatei85044197 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.iterierteAussagenverknüpfung|Iterierte Disjunktion|operator=disjunktion")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.iterierteAussagenverknüpfung|operator=disjunktion|Iterierte Disjunktion"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Iterierte Disjunktion",
            kurzbeschreibung = "Verknüpft die Aussagenwerte einer Methode über einer Indexmenge mit ∨.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen"), FachPfad.von("logik", "aussagen")),
            suchbegriffe = setOf("Iterierte Disjunktion", "Operatoren", "Verknüpft die Aussagenwerte einer Methode über einer Indexmenge mit ∨.", "aussage", "disjunktion", "indexmenge", "mathematik.aussage", "mathematik.iterierteAussagenverknüpfung", "mathematik.menge", "mathematik.methode", "methode", "operator"),
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

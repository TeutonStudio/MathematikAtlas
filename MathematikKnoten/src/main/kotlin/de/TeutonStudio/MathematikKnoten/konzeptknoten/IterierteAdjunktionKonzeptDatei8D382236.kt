package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object IterierteAdjunktionKonzeptDatei8D382236 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.iterierteAussagenverknüpfung|Iterierte Adjunktion|logikSemantik=xor;operator=adjunktion")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.iterierteAussagenverknüpfung|logikSemantik=xor|operator=adjunktion|Iterierte Adjunktion"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Iterierte Adjunktion",
            kurzbeschreibung = "Verknüpft die Aussagenwerte einer Methode über einer Indexmenge als iteriertes ausschließendes Oder.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen"), FachPfad.von("logik", "aussagen")),
            suchbegriffe = setOf("Iterierte Adjunktion", "Operatoren", "Verknüpft die Aussagenwerte einer Methode über einer Indexmenge als iteriertes ausschließendes Oder.", "adjunktion", "aussage", "indexmenge", "logikSemantik", "mathematik.aussage", "mathematik.iterierteAussagenverknüpfung", "mathematik.menge", "mathematik.methode", "methode", "operator", "xor"),
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

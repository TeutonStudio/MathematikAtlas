package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object TermZuMethodeKonzeptDateiEF3062BD : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("konzept.term-zu-methode")
    private val altePrädikatVariante = VariantenId("mathematik.termZuMethode|argumentReihenfolge=|name=P|Aussage zu Methode")
    override val varianten: Set<VariantenId> = setOf(
        VariantenId("mathematik.termZuMethode|argumentReihenfolge=|name=P|Aussage zu Prädikat"),
        VariantenId("mathematik.termZuMethode|argumentReihenfolge=|name=f|Term zu Methode"),
    )

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        val aktuelleVarianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()
        return WissensEintrag(
            id = id,
            titel = "Term zu Methode",
            kurzbeschreibung = "Bindet freie Parameter eines Terms und stellt den Term als typisierte Methode mit Zielmenge bereit.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf(
                "Abbildung",
                "Aussage zu Prädikat",
                "Aussage zu Methode",
                "Erzeugt aus einem allgemeinen Term eine Methode mit automatisch abgeleiteten Variablen und Zielmenge.",
                "Erzeugt aus einer Aussage ein typisiertes Prädikat.",
                "Lambda", "Methode", "Methoden", "P", "Parameterbindung", "Prädikat", "Term", "Term zu Methode",
                "argumentReihenfolge", "f", "mathematik.aussage", "mathematik.methode", "mathematik.objekt",
                "mathematik.termZuMethode", "methode", "name", "term",
            ),
            aliase = setOf(
                "mathematik.termZuMethode|Aussage zu Prädikat|argumentReihenfolge=;name=P",
                "mathematik.termZuMethode|Aussage zu Methode|argumentReihenfolge=;name=P",
                "mathematik.termZuMethode|Term zu Methode|argumentReihenfolge=;name=f",
            ),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.termZuMethode"),
            varianten = aktuelleVarianten,
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(aktuelleVarianten + altePrädikatVariante),
        )
    }
}

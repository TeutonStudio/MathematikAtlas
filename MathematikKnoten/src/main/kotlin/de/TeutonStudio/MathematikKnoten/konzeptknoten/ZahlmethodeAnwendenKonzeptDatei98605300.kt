package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object ZahlmethodeAnwendenKonzeptDatei98605300 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.methodenAnwendung|Zahlmethode anwenden|methodenAnwendung.ergebnisArt=mathematik.zahl")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.methodeAufrufen|festeEingänge=1|methodenAnwendung.ergebnisArt=mathematik.zahl|methodenAufruf.argumentprojektion=separiert|methodenAufruf.ergebnisprojektion=direkt|methodenAufruf.projektionFest=true|Zahlmethode anwenden"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Zahlmethode anwenden",
            kurzbeschreibung = "Wendet eine einwertige Methode auf ein Argument an.",
            fachPfade = setOf(FachPfad.von("algebra", "methoden"), FachPfad.von("analysis", "funktionen")),
            suchbegriffe = setOf("Methoden", "Wendet eine einwertige Methode auf ein Argument an.", "Zahlmethode anwenden", "argument", "mathematik.methode", "mathematik.methodenAnwendung", "mathematik.objekt", "mathematik.zahl", "methode", "methodenAnwendung.ergebnisArt", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.methodeAufrufen"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

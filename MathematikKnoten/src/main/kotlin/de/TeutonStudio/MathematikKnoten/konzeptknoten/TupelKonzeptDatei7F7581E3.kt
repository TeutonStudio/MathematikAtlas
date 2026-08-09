package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object TupelKonzeptDatei7F7581E3 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.tupel|Tupel|erzeugungsArt=einzelEingaben;festeEingänge=2;operatorAnzeige=wert")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.tupel|erzeugungsArt=einzelEingaben|festeEingänge=2|operatorAnzeige=wert|Tupel"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Tupel",
            kurzbeschreibung = "Geordnetes Tupel aus einzelnen Zahlen oder einer Indexmethode.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen")),
            suchbegriffe = setOf("1", "2", "Dimension", "Geordnetes Tupel aus einzelnen Zahlen oder einer Indexmethode.", "Indexmethode", "Tupel", "Zahlen", "a", "b", "dimension", "einzelEingaben", "erzeugungsArt", "festeEingänge", "mathematik.methode", "mathematik.tupel", "mathematik.zahl", "methode", "operatorAnzeige", "tupel", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.tupel"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

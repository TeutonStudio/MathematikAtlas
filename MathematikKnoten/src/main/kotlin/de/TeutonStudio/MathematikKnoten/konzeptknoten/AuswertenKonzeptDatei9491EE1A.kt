package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object AuswertenKonzeptDatei9491EE1A : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.auswerten|Auswerten|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.auswerten|Auswerten"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Auswerten",
            kurzbeschreibung = "Vereinfacht einen mathematischen Term typ-erhaltend, etwa eine Zahl, Aussage, Matrix, einen Vektor, ein Tupel oder eine Menge.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen")),
            suchbegriffe = setOf("Auswerten", "Steuerung", "Vereinfacht einen mathematischen Term typ-erhaltend, etwa eine Zahl, Aussage, Matrix, einen Vektor, ein Tupel oder eine Menge.", "mathematik.auswerten", "mathematik.objekt", "term"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.auswerten"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

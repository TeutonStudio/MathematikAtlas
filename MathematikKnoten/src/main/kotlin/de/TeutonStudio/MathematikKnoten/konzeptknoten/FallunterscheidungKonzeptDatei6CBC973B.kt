package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object FallunterscheidungKonzeptDatei6CBC973B : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.fall|Fallunterscheidung|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.fall|Fallunterscheidung"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Fallunterscheidung",
            kurzbeschreibung = "Wählt abhängig vom Wahrheitswert einer Aussage einen von zwei Werten aus.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen")),
            suchbegriffe = setOf("Fallunterscheidung", "Steuerung", "Wählt abhängig vom Wahrheitswert einer Aussage einen von zwei Werten aus.", "aussage", "lüge", "mathematik.aussage", "mathematik.fall", "mathematik.objekt", "wahr", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.fall"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

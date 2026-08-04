package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object TransponierenKonzeptDateiC5B2771D : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.transponieren|Transponieren|achsenPermutation=1,0")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.transponieren|achsenPermutation=1,0|Transponieren"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Transponieren",
            kurzbeschreibung = "Transponiert Vektoren, Matrizen und Tensoren typabhängig.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen")),
            suchbegriffe = setOf("1,0", "Lineare Algebra", "Transponieren", "Transponiert Vektoren, Matrizen und Tensoren typabhängig.", "achsenPermutation", "mathematik.objekt", "mathematik.transponieren", "wert"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.transponieren"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

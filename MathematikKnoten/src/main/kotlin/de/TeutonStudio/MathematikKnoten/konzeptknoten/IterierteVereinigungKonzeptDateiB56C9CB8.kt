package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object IterierteVereinigungKonzeptDateiB56C9CB8 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.iterierteVereinigung|Iterierte Vereinigung|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.iterierteVereinigung|Iterierte Vereinigung"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Iterierte Vereinigung",
            kurzbeschreibung = "Vereinigt die Mengenwerte einer Methode über einer Indexmenge.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengenoperationen")),
            suchbegriffe = setOf("Iterierte Vereinigung", "Mengen", "Vereinigt die Mengenwerte einer Methode über einer Indexmenge.", "indexmenge", "mathematik.iterierteVereinigung", "mathematik.menge", "mathematik.methode", "menge", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.iterierteVereinigung"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

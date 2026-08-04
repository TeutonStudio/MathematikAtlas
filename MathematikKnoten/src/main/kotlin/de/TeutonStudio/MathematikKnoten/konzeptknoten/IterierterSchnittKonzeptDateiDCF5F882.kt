package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object IterierterSchnittKonzeptDateiDCF5F882 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.iterierterSchnitt|Iterierter Schnitt|")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.iterierterSchnitt|Iterierter Schnitt"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Iterierter Schnitt",
            kurzbeschreibung = "Schneidet Mengenwerte; die Grundmenge stammt aus der Zielmenge der Methode.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengenoperationen")),
            suchbegriffe = setOf("Iterierter Schnitt", "Mengen", "Schneidet Mengenwerte; die Grundmenge stammt aus der Zielmenge der Methode.", "indexmenge", "mathematik.iterierterSchnitt", "mathematik.menge", "mathematik.methode", "menge", "methode"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.iterierterSchnitt"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

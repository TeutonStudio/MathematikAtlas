package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object EndlicheMengeKonzeptDatei8A0B2B23 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("mathematik.endlicheMenge|Endliche Menge|elementKonfiguration=v2:bWF0aGVtYXRpay56YWhs|z:c3RhbmRhcmQtMQ:bWF0aGVtYXRpay56YWhs:MQ|z:c3RhbmRhcmQtMg:bWF0aGVtYXRpay56YWhs:Mg|z:c3RhbmRhcmQtMw:bWF0aGVtYXRpay56YWhs:Mw")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.endlicheMenge|elementKonfiguration=v2:bWF0aGVtYXRpay56YWhs|z:c3RhbmRhcmQtMQ:bWF0aGVtYXRpay56YWhs:MQ|z:c3RhbmRhcmQtMg:bWF0aGVtYXRpay56YWhs:Mg|z:c3RhbmRhcmQtMw:bWF0aGVtYXRpay56YWhs:Mw|Endliche Menge"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Endliche Menge",
            kurzbeschreibung = "Endliche Menge aus einer geordnet bearbeitbaren Liste typisierter Elemente.",
            fachPfade = setOf(FachPfad.von("mengenlehre", "mengen")),
            suchbegriffe = setOf("Endliche Menge", "Endliche Menge aus einer geordnet bearbeitbaren Liste typisierter Elemente.", "Mengen", "elementKonfiguration", "mathematik.endlicheMenge", "mathematik.menge", "menge", "v2:bWF0aGVtYXRpay56YWhs|z:c3RhbmRhcmQtMQ:bWF0aGVtYXRpay56YWhs:MQ|z:c3RhbmRhcmQtMg:bWF0aGVtYXRpay56YWhs:Mg|z:c3RhbmRhcmQtMw:bWF0aGVtYXRpay56YWhs:Mw"),
            aliase = emptySet(),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.endlicheMenge"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

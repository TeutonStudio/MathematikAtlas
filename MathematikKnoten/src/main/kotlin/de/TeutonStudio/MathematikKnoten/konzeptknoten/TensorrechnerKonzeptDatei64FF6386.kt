package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.StatischeKonzeptKarten

internal object TensorrechnerKonzeptDatei64FF6386 : ExpliziteKonzeptDatei {
    override val id: WissensId = WissensId("konzept.tensorrechner")
    override val varianten: Set<VariantenId> = setOf(VariantenId("mathematik.tensorrechner|operator=tensor.addition|Tensorrechner"))

    override fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag {
        val passendeVorlagen = vorlagen.nachVarianten(varianten)
        return WissensEintrag(
            id = id,
            titel = "Tensorrechner",
            kurzbeschreibung = "Universeller Rechner für komponentenweise, indexbezogene und formverändernde Tensoroperationen.",
            fachPfade = setOf(FachPfad.von("algebra", "operationen"), FachPfad.von("lineare-algebra", "tensoren")),
            suchbegriffe = setOf("Achsenpermutation", "Einheitlicher Tensorrechner mit operatorabhängigen Anschlüssen und typisiertem CAS-Formelmodus.", "Kontraktion", "Matrix", "Tensor", "Tensoren", "Tensorrechner", "Vektor", "links", "mathematik.tensor", "mathematik.tensorrechner", "operator", "rechts", "tensor.addition", "wert"),
            aliase = setOf("mathematik.tensorrechner|Tensorrechner|operator=tensor.addition"),
            verfügbarkeit = WissensVerfügbarkeit.Verfügbar,
            reifegrad = WissensReifegrad.Geprüft,
            knotenArten = setOf("mathematik.tensorrechner"),
            varianten = passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet(),
            knotenVorlagen = passendeVorlagen,
            karten = StatischeKonzeptKarten.fürVarianten(passendeVorlagen.map(KnotenVorlage::stabileVariantenId).toSet()),
        )
    }
}

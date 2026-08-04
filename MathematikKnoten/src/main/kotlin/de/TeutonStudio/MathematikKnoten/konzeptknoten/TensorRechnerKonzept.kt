package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId
import de.TeutonStudio.MathematikRechenSystem.kern.TensorRechner

object TensorRechnerKonzept {
    val id = WissensId("konzept.tensorrechner")

    fun passt(vorlage: KnotenVorlage): Boolean = vorlage.art == TensorRechner.KNOTEN_ART

    fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag = gruppiertesVorlagenKonzept(
        id = id,
        titel = "Tensorrechner",
        beschreibung = "Universeller Rechner für komponentenweise, indexbezogene und formverändernde Tensoroperationen.",
        vorlagen = vorlagen.filter(::passt),
        generatorId = "konzeptkarte.tensorrechner",
        assetDatei = "tensorrechner-v1.json",
        zusätzlicheSuchbegriffe = setOf("Tensor", "Matrix", "Vektor", "Kontraktion", "Achsenpermutation"),
    )
}

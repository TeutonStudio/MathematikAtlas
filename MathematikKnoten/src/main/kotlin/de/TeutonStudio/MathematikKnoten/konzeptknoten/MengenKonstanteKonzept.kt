package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.MENGEN_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId

object MengenKonstanteKonzept {
    val id = WissensId("konzept.mengenkonstante")

    fun passt(vorlage: KnotenVorlage): Boolean = vorlage.art == MENGEN_KNOTEN_ART

    fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag = gruppiertesVorlagenKonzept(
        id = id,
        titel = "Mengenkonstante",
        beschreibung = "Konsolidierter Mengenknoten für Grundmengen und versionsfeste benutzerdefinierte Mengenkarten.",
        vorlagen = vorlagen.filter(::passt),
        generatorId = "konzeptkarte.mengenkonstante",
        assetDatei = "mengenkonstante-v1.json",
        zusätzlicheSuchbegriffe = setOf("Grundmenge", "Zahlbereich", "Menge", "Mengenkonstante"),
    )
}

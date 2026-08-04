package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId

object TermZuMethodeKonzept {
    val id = WissensId("konzept.term-zu-methode")

    fun passt(vorlage: KnotenVorlage): Boolean =
        "termzumethode" in vorlage.art.lowercase() ||
            "term zu methode" in vorlage.name.lowercase()

    fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag = gruppiertesVorlagenKonzept(
        id = id,
        titel = "Term zu Methode",
        beschreibung = "Bindet freie Parameter eines Terms und stellt den Term als typisierte Methode mit Zielmenge bereit.",
        vorlagen = vorlagen.filter(::passt),
        generatorId = "konzeptkarte.term-zu-methode",
        assetDatei = "term-zu-methode-v1.json",
        zusätzlicheSuchbegriffe = setOf("Methode", "Abbildung", "Parameterbindung", "Lambda", "Term"),
    )
}

package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId

object ZahlenRechnerKonzept {
    val id = WissensId("konzept.zahlenrechner")

    fun passt(vorlage: KnotenVorlage): Boolean = vorlage.art == ZAHLENRECHNER_ART

    fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag = gruppiertesVorlagenKonzept(
        id = id,
        titel = "Zahlenrechner",
        beschreibung = "Universeller Rechner für Zahlenoperatoren mit operatorabhängigen Anschlüssen, Definitionsbereichen und Formeldarstellung.",
        vorlagen = vorlagen.filter(::passt),
        generatorId = "konzeptkarte.zahlenrechner",
        zusätzlicheSuchbegriffe = setOf("CAS", "Formel", "Zahlenoperator", "Rechenoperation"),
    )
}

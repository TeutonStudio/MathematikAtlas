package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId

object ZahlKonstanteKonzept {
    val id = WissensId("konzept.zahlkonstante")

    fun passt(vorlage: KnotenVorlage): Boolean = vorlage.art == "mathematik.zahl"

    fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag = gruppiertesVorlagenKonzept(
        id = id,
        titel = "Zahlkonstante",
        beschreibung = "Exakte Zahlkonstante mit kanonischer rationaler Darstellung und typisiertem Zahlenausgang.",
        vorlagen = vorlagen.filter(::passt),
        generatorId = "konzeptkarte.zahlkonstante",
        zusätzlicheSuchbegriffe = setOf("Zahl", "Konstante", "rational", "ganz"),
    )
}

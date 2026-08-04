package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.MathematikKnoten.enzyklopädie.FachKatalog
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensVerfügbarkeit

object GeplanteKonzepte {
    val OffeneMenge = WissensEintrag(
        id = WissensId("geplant.topologie.offene-menge"),
        titel = "Offene Menge",
        kurzbeschreibung = "Grundbegriff der Topologie; eine erzeugbare Knotenvorlage ist noch nicht registriert.",
        fachPfade = setOf(FachKatalog.TopologieGrundbegriffe),
        suchbegriffe = setOf("offene Menge", "Topologie", "Umgebung"),
        verfügbarkeit = WissensVerfügbarkeit.Geplant,
    )

    val Zufallsvariable = WissensEintrag(
        id = WissensId("geplant.stochastik.zufallsvariable"),
        titel = "Zufallsvariable",
        kurzbeschreibung = "Messbare Abbildung eines Wahrscheinlichkeitsraums; noch nicht als Knoten verfügbar.",
        fachPfade = setOf(FachKatalog.StochastikGrundbegriffe),
        suchbegriffe = setOf("Zufallsvariable", "Stochastik", "Wahrscheinlichkeit"),
        verfügbarkeit = WissensVerfügbarkeit.Geplant,
    )

    val alle: List<WissensEintrag> = listOf(OffeneMenge, Zufallsvariable)
}

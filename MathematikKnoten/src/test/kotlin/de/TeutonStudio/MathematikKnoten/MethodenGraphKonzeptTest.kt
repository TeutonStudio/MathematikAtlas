package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.enzyklopädie.FachKatalog
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensKartenRolle
import de.TeutonStudio.MathematikKnoten.konzeptknoten.KonzeptKnotenRegister
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MethodenGraphKonzeptTest {
    @Test
    fun `graph besitzt eine primaere definitionskarte`() {
        val eintrag = KonzeptKnotenRegister.erstelle(alleMathematikDefinitionsVorlagen())
            .single { wissen ->
                wissen.knotenVorlagen.any { vorlage -> vorlage.art == METHODEN_GRAPH_KNOTEN_ART }
            }

        assertTrue(
            eintrag.karten.any { karte ->
                karte.primär && karte.rolle == WissensKartenRolle.Definition
            },
        )
        assertEquals(
            setOf(FachKatalog.AnalysisFunktionen, FachKatalog.MengenlehreKonstruktionen),
            eintrag.fachPfade,
        )
    }

    @Test
    fun `graphdefinition bleibt unter den fachlichen suchbegriffen auffindbar`() {
        val eintrag = KonzeptKnotenRegister.erstelle(alleMathematikDefinitionsVorlagen())
            .single { wissen ->
                wissen.knotenVorlagen.any { vorlage -> vorlage.art == METHODEN_GRAPH_KNOTEN_ART }
            }
        val suchtexte = eintrag.alleSuchtexte.map(String::lowercase).toSet()

        listOf(
            "graph",
            "funktionsgraph",
            "graph einer funktion",
            "graph einer methode",
            "γ_f",
        ).forEach { suchtext ->
            assertTrue(
                suchtexte.any { text -> suchtext in text },
                "Definitions-/Konzeptmetadaten enthalten '$suchtext' nicht.",
            )
        }
    }
}

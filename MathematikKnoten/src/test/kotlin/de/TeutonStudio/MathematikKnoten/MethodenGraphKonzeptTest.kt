package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.enzyklopädie.FachKatalog
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensKartenReferenz
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensKartenRolle
import de.TeutonStudio.MathematikKnoten.konzeptknoten.KonzeptKnotenRegister
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MethodenGraphKonzeptTest {
    @Test
    fun `graph besitzt eine explizite primaere definitionskarte`() {
        val eintrag = graphEintrag()
        val definition = eintrag.karten.single { karte ->
            karte.primär && karte.rolle == WissensKartenRolle.Definition
        }
        val asset = assertIs<WissensKartenReferenz.Asset>(definition)

        assertEquals("mathematik.methodenGraph|Graph|.definition", asset.id)
        assertEquals("karte-methodengraph-definition-v7.json", asset.datei)
        assertEquals(7, asset.formatVersion)
        assertEquals(
            setOf(FachKatalog.AnalysisFunktionen, FachKatalog.MengenlehreKonstruktionen),
            eintrag.fachPfade,
        )
    }

    @Test
    fun `graphdefinition bleibt unter den fachlichen suchbegriffen auffindbar`() {
        val suchtexte = graphEintrag().alleSuchtexte.map(String::lowercase).toSet()

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

    private fun graphEintrag() = KonzeptKnotenRegister.erstelle(alleMathematikDefinitionsVorlagen())
        .single { wissen ->
            wissen.knotenVorlagen.any { vorlage -> vorlage.art == METHODEN_GRAPH_KNOTEN_ART }
        }
}

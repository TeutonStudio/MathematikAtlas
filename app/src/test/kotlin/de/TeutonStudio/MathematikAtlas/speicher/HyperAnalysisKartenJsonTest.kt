package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.HYPER_ERWEITERUNGSART_PARAMETER
import de.TeutonStudio.MathematikKnoten.HYPER_GROESSENKLASSE_PARAMETER
import de.TeutonStudio.MathematikKnoten.HYPER_MODELL_ID_PARAMETER
import de.TeutonStudio.MathematikKnoten.HYPER_STANDARDTEIL_PARAMETER
import de.TeutonStudio.MathematikKnoten.HYPER_WERT_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.HYPER_WERT_NAME_PARAMETER
import de.TeutonStudio.MathematikKnoten.HyperAnalysisKnotenVorlagen
import de.TeutonStudio.MathematikRechenSystem.kern.HyperErweiterungsArt
import de.TeutonStudio.MathematikRechenSystem.kern.HyperGroessenKlasse
import de.TeutonStudio.MathematikRechenSystem.kern.KanonischesHyperModell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HyperAnalysisKartenJsonTest {
    @Test
    fun `Hyperwert roundtrippt Modell Klasse Standardteil und Handles`() {
        val knoten = HyperAnalysisKnotenVorlagen.HyperWert.erzeuge(GraphPunkt.Zero).copy(
            parameter = HyperAnalysisKnotenVorlagen.HyperWert.standardParameter + mapOf(
                HYPER_WERT_NAME_PARAMETER to "h_0",
                HYPER_GROESSENKLASSE_PARAMETER to HyperGroessenKlasse.ENDLICH.name,
                HYPER_STANDARDTEIL_PARAMETER to "5/3",
            ),
        )
        val karte = KartenDaten(name = "Hyperwert", knoten = listOf(knoten))

        val text = KartenJson.schreibe(karte)
        val gelesen = KartenJson.lese(text).knoten.single()

        assertEquals(HYPER_WERT_KNOTEN_ART, gelesen.art)
        assertEquals("h_0", gelesen.parameter[HYPER_WERT_NAME_PARAMETER])
        assertEquals(HyperGroessenKlasse.ENDLICH.name, gelesen.parameter[HYPER_GROESSENKLASSE_PARAMETER])
        assertEquals("5/3", gelesen.parameter[HYPER_STANDARDTEIL_PARAMETER])
        assertEquals(KanonischesHyperModell.modell.id.wert, gelesen.parameter[HYPER_MODELL_ID_PARAMETER])
        assertEquals(knoten.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
        assertFalse(text.contains("filterAxiome"))
        assertFalse(text.contains("\\mathcal U"))
    }

    @Test
    fun `Hypererweiterungsart bleibt Kartenparameter`() {
        val knoten = HyperAnalysisKnotenVorlagen.HyperErweiterung.erzeuge(GraphPunkt.Zero).copy(
            parameter = HyperAnalysisKnotenVorlagen.HyperErweiterung.standardParameter +
                (HYPER_ERWEITERUNGSART_PARAMETER to HyperErweiterungsArt.METHODE.name),
        )

        val gelesen = KartenJson.lese(
            KartenJson.schreibe(KartenDaten(name = "Erweiterung", knoten = listOf(knoten))),
        ).knoten.single()

        assertEquals(HyperErweiterungsArt.METHODE.name, gelesen.parameter[HYPER_ERWEITERUNGSART_PARAMETER])
        assertEquals(KanonischesHyperModell.modell.id.wert, gelesen.parameter[HYPER_MODELL_ID_PARAMETER])
    }

    @Test
    fun `historischer Hyperwert wird beim Lesen ohne Filtermaterialisierung migriert`() {
        val alt = KnotenDaten(
            art = "mathematik.hyperReelleZahl",
            name = "Alt",
            position = GraphPunkt.Zero,
            parameter = mapOf("name" to "H"),
        )
        val roh = KartenDatenJson.schreibe(KartenDaten(name = "Altkarte", knoten = listOf(alt)))

        val gelesen = KartenJson.lese(roh).knoten.single()

        assertEquals(HYPER_WERT_KNOTEN_ART, gelesen.art)
        assertEquals("H", gelesen.parameter[HYPER_WERT_NAME_PARAMETER])
        assertTrue(gelesen.parameter.containsKey(HYPER_MODELL_ID_PARAMETER))
    }
}

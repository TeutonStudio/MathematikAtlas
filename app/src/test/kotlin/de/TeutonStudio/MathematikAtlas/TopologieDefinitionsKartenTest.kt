package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.EIGENSCHAFT_PARAMETER
import de.TeutonStudio.MathematikKnoten.MathematischeEigenschaftKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematischeEigenschaftRegister
import de.TeutonStudio.MathematikKnoten.TopologischeStrukturKnotenVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TopologieDefinitionsKartenTest {
    @Test
    fun `topologischer Raum besitzt die geforderten Definitionsreiter`() {
        val knoten = TopologischeStrukturKnotenVorlagen.TopologischerRaum.erzeuge(GraphPunkt.Zero)
        val konzept = assertNotNull(topologieKonzeptFürKnoten(knoten))

        assertEquals(
            listOf(
                "Definition",
                "Topologieaxiome",
                "Diskrete Topologie",
                "Indiskrete Topologie",
                "Standardtopologien",
                "Teilraumtopologie",
                "Produkttopologie",
                "Metrisch induzierte Topologie",
                "Beispiele",
            ),
            konzept.reiter.map { it.titel },
        )
    }

    @Test
    fun `metrischer Raum besitzt die geforderten Definitionsreiter`() {
        val knoten = TopologischeStrukturKnotenVorlagen.MetrischerRaum.erzeuge(GraphPunkt.Zero)
        val konzept = assertNotNull(topologieKonzeptFürKnoten(knoten))

        assertEquals(
            listOf(
                "Definition",
                "Metrikaxiome",
                "Offene Kugeln",
                "Induzierte Topologie",
                "Stetigkeit",
                "Standardmetriken",
                "Beispiele",
            ),
            konzept.reiter.map { it.titel },
        )
    }

    @Test
    fun `abzaehlbar liegt in Kardinalitaet und nicht im Topologiereiter`() {
        val knoten = MathematischeEigenschaftKnotenVorlagen.MengenEigenschaft.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf(EIGENSCHAFT_PARAMETER to MathematischeEigenschaftRegister.Abzaehlbar.id),
        )
        val konzept = assertNotNull(topologieKonzeptFürKnoten(knoten))
        val kardinalitaet = konzept.reiter.first { it.titel == "Kardinalität" }
        val topologie = konzept.reiter.first { it.titel == "Topologie" }

        assertTrue(konzept.pfad.last() == "Kardinalität")
        assertTrue(kardinalitaet.karte.knoten.single().parameter.values.any { "abzählbar" in it })
        assertFalse(topologie.karte.knoten.single().parameter.values.any { "abzählbar" in it })
    }

    @Test
    fun `Bibliothek akzeptiert neue Topologiepfade`() {
        val vorlagen = listOf(
            TopologischeStrukturKnotenVorlagen.TopologischerRaum,
            TopologischeStrukturKnotenVorlagen.MetrischerRaum,
            MathematischeEigenschaftKnotenVorlagen.MethodenEigenschaft,
            MathematischeEigenschaftKnotenVorlagen.MengenEigenschaft.copy(
                standardParameter = mapOf(EIGENSCHAFT_PARAMETER to MathematischeEigenschaftRegister.Abzaehlbar.id),
            ),
        )
        val eintraege = KonzeptBibliothekRegister.erstelle(vorlagen)

        assertTrue(KonzeptBibliothekRegister.validierungsFehler(eintraege).isEmpty())
        assertTrue(eintraege.any { listOf("mengenlehre", "topologie", "raeume") in it.kategoriePfade })
        assertTrue(eintraege.any { listOf("topologie", "abbildungen") in it.kategoriePfade })
        assertTrue(eintraege.any { listOf("mengenlehre", "eigenschaften", "kardinalitaet") in it.kategoriePfade })
    }
}

package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.MathematikAtlas.speicher.KartenOrdnung
import de.TeutonStudio.MathematikAtlas.speicher.KartenHerkunft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KartenListenModellTest {
    private val wurzel = KartenDaten(id = KartenId("wurzel"), name = "Wurzelkarte")
    private val algebra = KartenDaten(id = KartenId("algebra"), name = "Algebra")
    private val matrizen = KartenDaten(id = KartenId("matrizen"), name = "Matrizen")
    private val analysis = KartenDaten(id = KartenId("analysis"), name = "Analysis")

    private val ordnung = KartenOrdnung()
        .mitKarteInOrdner(algebra.id, listOf("Algebra"))
        .mitKarteInOrdner(matrizen.id, listOf("Algebra", "Lineare Algebra"))
        .mitKarteInOrdner(analysis.id, listOf("Analysis"))

    private val karten = listOf(wurzel, algebra, matrizen, analysis)

    @Test
    fun `eingeklappter Ordner blendet alle Nachkommen aus`() {
        val einträge = kartenListenEinträge(
            karten = karten,
            ordnung = ordnung,
            eingeklappteOrdner = setOf(listOf("Algebra")),
        )

        assertTrue(einträge.any { it is KartenListenEintrag.Ordner && it.pfad == listOf("Algebra") })
        assertFalse(einträge.any { it is KartenListenEintrag.Ordner && it.pfad == listOf("Algebra", "Lineare Algebra") })
        assertFalse(einträge.any { it is KartenListenEintrag.Karte && it.karte.id == algebra.id })
        assertFalse(einträge.any { it is KartenListenEintrag.Karte && it.karte.id == matrizen.id })
    }

    @Test
    fun `Geschwister und Wurzelkarten bleiben bei eingeklapptem Ordner sichtbar`() {
        val einträge = kartenListenEinträge(
            karten = karten,
            ordnung = ordnung,
            eingeklappteOrdner = setOf(listOf("Algebra")),
        )

        assertTrue(einträge.any { it is KartenListenEintrag.Ordner && it.pfad == listOf("Analysis") })
        assertTrue(einträge.any { it is KartenListenEintrag.Karte && it.karte.id == analysis.id })
        assertTrue(einträge.any { it is KartenListenEintrag.Karte && it.karte.id == wurzel.id })
    }

    @Test
    fun `erneutes Öffnen stellt Nachkommen in korrekter Tiefe wieder her`() {
        val einträge = kartenListenEinträge(karten, ordnung)
        val lineareAlgebra = einträge.filterIsInstance<KartenListenEintrag.Ordner>()
            .single { it.pfad == listOf("Algebra", "Lineare Algebra") }
        val matrixKarte = einträge.filterIsInstance<KartenListenEintrag.Karte>()
            .single { it.karte.id == matrizen.id }

        assertEquals(1, lineareAlgebra.tiefe)
        assertEquals(2, matrixKarte.tiefe)
    }

    @Test
    fun `Herkunftsfilter zeigt nur Karten und notwendige Ordner der Auswahl`() {
        val einträge = kartenListenEinträge(
            karten,
            ordnung,
            herkunft = mapOf(
                wurzel.id to KartenHerkunft.EIGEN,
                algebra.id to KartenHerkunft.VORDEFINIERT,
                matrizen.id to KartenHerkunft.FREMD,
                analysis.id to KartenHerkunft.EIGEN,
            ),
            filter = KartenHerkunft.FREMD,
        )

        assertEquals(listOf(matrizen.id), einträge.filterIsInstance<KartenListenEintrag.Karte>().map { it.karte.id })
        assertEquals(
            setOf(listOf("Algebra"), listOf("Algebra", "Lineare Algebra")),
            einträge.filterIsInstance<KartenListenEintrag.Ordner>().map { it.pfad }.toSet(),
        )
    }

    @Test
    fun `fremd verwaltete leere Ordner erscheinen nicht unter eigenen Karten`() {
        val einträge = kartenListenEinträge(
            karten = listOf(wurzel),
            ordnung = KartenOrdnung().mitOrdner(listOf("Freigaben", "Ada")),
            fremdVerwalteteOrdner = setOf(listOf("Freigaben")),
        )

        assertTrue(einträge.none { it is KartenListenEintrag.Ordner })
        assertTrue(einträge.any { it is KartenListenEintrag.Karte && it.karte.id == wurzel.id })
    }
}

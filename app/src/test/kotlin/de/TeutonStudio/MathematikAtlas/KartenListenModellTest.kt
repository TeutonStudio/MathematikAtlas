package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.VisuelleKnotenGruppeDaten
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
    @Test fun `Standardordner bleiben im Eigenfilter verborgen`() {
        val liste = kartenListenEinträge(listOf(algebra), ordnung,
            herkunft = mapOf(algebra.id to KartenHerkunft.VORDEFINIERT))
        assertFalse(liste.any { it is KartenListenEintrag.Ordner && it.pfad.first() == "Algebra" })
    }

    @Test fun `eigene Karte bleibt in fremd verwaltetem Ordner erreichbar`() {
        val liste = kartenListenEinträge(listOf(matrizen), ordnung,
            fremdVerwalteteOrdner = setOf(listOf("Algebra")))
        assertTrue(liste.any { it is KartenListenEintrag.Karte && it.karte.id == matrizen.id })
        assertEquals(setOf(listOf("Algebra"), listOf("Algebra", "Lineare Algebra")),
            liste.filterIsInstance<KartenListenEintrag.Ordner>().filter { it.pfad.first() == "Algebra" }.map { it.pfad }.toSet())
    }

    @Test fun `gemeinsamer Ordner trennt Karten nach Herkunft und bewahrt leere eigene Ordner`() {
        val gemeinsam = KartenOrdnung().mitKarteInOrdner(algebra.id, listOf("Gemeinsam"))
            .mitKarteInOrdner(matrizen.id, listOf("Gemeinsam")).mitOrdner(listOf("Leer"))
        val herkunft = mapOf(algebra.id to KartenHerkunft.VORDEFINIERT, matrizen.id to KartenHerkunft.EIGEN)
        val eigene = kartenListenEinträge(listOf(algebra, matrizen), gemeinsam, herkunft = herkunft)
        assertEquals(listOf(matrizen.id), eigene.filterIsInstance<KartenListenEintrag.Karte>().map { it.karte.id })
        assertTrue(eigene.any { it is KartenListenEintrag.Ordner && it.pfad == listOf("Leer") })
        val standard = kartenListenEinträge(listOf(algebra, matrizen), gemeinsam, herkunft = herkunft, filter = KartenHerkunft.VORDEFINIERT)
        assertEquals(listOf(algebra.id), standard.filterIsInstance<KartenListenEintrag.Karte>().map { it.karte.id })
        assertFalse(standard.any { it is KartenListenEintrag.Ordner && it.pfad == listOf("Leer") })
    }

    @Test fun `Ordnernamen allein bestimmen keine Herkunft`() {
        val liste = kartenListenEinträge(listOf(wurzel), KartenOrdnung().mitOrdner(listOf("Standardkarten")))
        assertTrue(liste.any { it is KartenListenEintrag.Ordner && it.pfad == listOf("Standardkarten") })
    }

    @Test fun `explizit eigener leerer Unterordner bleibt im verwalteten Baum sichtbar und persistent`() {
        val temp = java.nio.file.Files.createTempDirectory("eigene-ordner").toFile()
        try {
            val pfad = listOf("Freigaben", "Meine Notizen")
            val speicher = de.TeutonStudio.MathematikAtlas.speicher.KartenOrdnungSpeicher(java.io.File(temp, "ordnung.json"))
            speicher.speichere(KartenOrdnung().mitEigenemOrdner(pfad))
            val geladen = speicher.lade()
            assertEquals(setOf(pfad), geladen.eigeneOrdner)
            val liste = kartenListenEinträge(emptyList(), geladen, fremdVerwalteteOrdner = setOf(listOf("Freigaben")))
            assertTrue(liste.any { it is KartenListenEintrag.Ordner && it.pfad == pfad })
            val verschoben = geladen.verschiebeOrdner(pfad, listOf("Privat", "Notizen"))
            assertEquals(setOf(listOf("Privat", "Notizen")), verschoben.eigeneOrdner)
            assertTrue(verschoben.ohneOrdnerBaum(listOf("Privat")).eigeneOrdner.isEmpty())
        } finally { temp.deleteRecursively() }
    }

    @Test fun `verschobener leerer Legacyordner wird explizit eigener Unterordner`() {
        val ziel = listOf("Freigaben", "Meine Notizen")
        val legacy = KartenOrdnung().mitOrdner(listOf("Notizen"))
        val verschoben = legacy.verschiebeOrdner(listOf("Notizen"), ziel).mitEigenemOrdner(ziel)
        val liste = kartenListenEinträge(emptyList(), verschoben,
            fremdVerwalteteOrdner = setOf(listOf("Freigaben")))
        assertTrue(liste.any { it is KartenListenEintrag.Ordner && it.pfad == ziel })
    }


    @Test fun `Gruppen erscheinen unter Karten hierarchisch und einzeln zuklappbar`() {
        val eltern=VisuelleKnotenGruppeDaten(titel="Eltern")
        val kind=VisuelleKnotenGruppeDaten(titel="Kind",elternGruppenId=eltern.id)
        val karte=wurzel.copy(visuelleGruppen=listOf(kind,eltern))
        val offen=kartenListenEinträge(listOf(karte),KartenOrdnung())
        val gruppen=offen.filterIsInstance<KartenListenEintrag.Gruppe>()
        assertEquals(listOf(eltern.id,kind.id),gruppen.map{it.gruppe.id})
        assertEquals(listOf(1,2),gruppen.map{it.tiefe})
        val zu=kartenListenEinträge(listOf(karte),KartenOrdnung(),eingeklappteGruppen=setOf(gruppen.first().schlüssel))
        assertEquals(listOf(eltern.id),zu.filterIsInstance<KartenListenEintrag.Gruppe>().map{it.gruppe.id})
    }
}

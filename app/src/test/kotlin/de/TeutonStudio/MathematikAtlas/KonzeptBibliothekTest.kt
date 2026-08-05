package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.MathematikKnoten.GeometrieKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KonzeptBibliothekTest {
    @Test
    fun `feste Fachhierarchie enthält alle Hauptbereiche und höchstens drei Ebenen`() {
        val hauptbereiche = KonzeptBibliothekRegister.kategorien.map { it.bezeichnung }.toSet()
        assertTrue(
            setOf(
                "Analysis", "Lineare Algebra", "Geometrie", "Mengenlehre", "Logik", "Algebra",
                "Topologie", "Stochastik", "Eigene Karten",
            ).all { it in hauptbereiche },
        )
        assertFalse("Karteneingänge" in hauptbereiche)
        assertFalse("Kartenausgänge" in hauptbereiche)

        val vorlage = alleMathematikDefinitionsVorlagen().first { it.art == "mathematik.spur" }
        val einträge = KonzeptBibliothekRegister.erstelle(listOf(vorlage))
        assertTrue(einträge.flatMap { it.kategoriePfade }.all { it.size in 1..3 })
        assertEquals(emptyList(), KonzeptBibliothekRegister.validierungsFehler(einträge))
    }

    @Test
    fun `Kartenschnittstellen erscheinen unter Eigene Karten und bleiben suchbar`() {
        val einträge = KonzeptBibliothekRegister.erstelle(
            listOf(
                MathematikKnotenVorlagen.KartenEingang,
                MathematikKnotenVorlagen.KartenAusgang,
            ),
        ).filter { it.vorlage != null }

        assertEquals(2, einträge.size)
        assertTrue(einträge.all { it.kategoriePfade == listOf(listOf("eigene-karten")) })
        assertEquals(
            setOf("Karten-Eingang"),
            einträge.filter { it.passt(KonzeptBibliothekFilter(suchtext = "Eingang")) }.map { it.titel }.toSet(),
        )
        assertEquals(
            setOf("Karten-Ausgang"),
            einträge.filter { it.passt(KonzeptBibliothekFilter(suchtext = "Ausgang")) }.map { it.titel }.toSet(),
        )
        assertEquals(
            setOf("Karten-Eingang"),
            einträge.filter { it.passt(KonzeptBibliothekFilter(suchtext = "Karten-Eingang")) }.map { it.titel }.toSet(),
        )
        assertEquals(
            setOf("Karten-Ausgang"),
            einträge.filter { it.passt(KonzeptBibliothekFilter(suchtext = "Karten-Ausgang")) }.map { it.titel }.toSet(),
        )
        assertEquals(emptyList(), KonzeptBibliothekRegister.validierungsFehler(einträge))
    }

    @Test
    fun `Skalarprodukt erscheint in linearer Algebra und Geometrie`() {
        val vorlage = alleMathematikDefinitionsVorlagen()
            .first { it.art == "mathematik.begriff.skalarprodukt" }
        val eintrag = KonzeptBibliothekRegister.erstelle(listOf(vorlage))
            .single { it.vorlage == vorlage }

        assertTrue(listOf("lineare-algebra", "skalarprodukte") in eintrag.kategoriePfade)
        assertTrue(listOf("geometrie", "grundobjekte") in eintrag.kategoriePfade)
    }

    @Test
    fun `Anschlussfilter prüft Richtung und Typ`() {
        val vorlage = alleMathematikDefinitionsVorlagen().first { it.art == "mathematik.spur" }
        val matrix = vorlage.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }.art
        val zahl = vorlage.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.art
        val eintrag = KonzeptBibliothekRegister.erstelle(listOf(vorlage)).single { it.vorlage == vorlage }

        assertTrue(eintrag.passt(KonzeptBibliothekFilter(erforderlicherEingang = matrix, erforderlicherAusgang = zahl)))
        assertFalse(eintrag.passt(KonzeptBibliothekFilter(erforderlicherEingang = zahl)))
    }

    @Test
    fun `App Erweiterungen ergänzen das kanonische Register ohne Absturz`() {
        val geometrie = GeometrieKnotenVorlagen.Raum
        val mengenraum = MengenraumKnotenVorlagen.Potenzmenge
        val einträge = KonzeptBibliothekRegister.erstelle(
            alleMathematikDefinitionsVorlagen() + listOf(geometrie, mengenraum),
        )

        assertTrue(
            einträge.any {
                it.vorlage == geometrie && listOf("geometrie", "grundobjekte") in it.kategoriePfade
            },
        )
        assertTrue(
            einträge.any {
                it.vorlage == mengenraum && listOf("mengenlehre", "mengen") in it.kategoriePfade
            },
        )
        assertEquals(emptyList(), KonzeptBibliothekRegister.validierungsFehler(einträge))
    }

    @Test
    fun `geplante Konzepte bleiben sichtbar aber nicht einfügbar`() {
        val einträge = KonzeptBibliothekRegister.erstelle(emptyList())
        val geplante = einträge.filter { it.verfügbarkeit == KonzeptVerfügbarkeit.Geplant }

        assertTrue(geplante.isNotEmpty())
        assertTrue(geplante.all { !it.istEinfügbar && it.vorlage == null })
    }

    @Test
    fun `ungültiger gespeicherter Modus fällt auf Standard zurück`() {
        assertEquals(KnotenWählerModus.Standard, KnotenWählerModus.aus("unbekannt"))
        assertEquals(KnotenWählerModus.Konzeptbibliothek, KnotenWählerModus.aus("concept-library"))
    }
}

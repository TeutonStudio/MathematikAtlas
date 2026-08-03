package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.*

class KonzeptBibliothekTest {
    @Test
    fun `feste Fachhierarchie enthält alle Hauptbereiche und höchstens drei Ebenen`() {
        val hauptbereiche = KonzeptBibliothekRegister.kategorien.map { it.bezeichnung }.toSet()
        assertTrue(
            setOf(
                "Analysis", "Lineare Algebra", "Geometrie", "Mengenlehre", "Logik", "Algebra",
                "Topologie", "Stochastik", "Eigene Karten", "Karteneingänge", "Kartenausgänge",
            ).all { it in hauptbereiche },
        )

        val einträge = KonzeptBibliothekRegister.erstelle(listOf(testVorlage()))
        assertTrue(einträge.flatMap { it.kategoriePfade }.all { it.size in 1..3 })
        assertEquals(emptyList(), KonzeptBibliothekRegister.validierungsFehler(einträge))
    }

    @Test
    fun `Skalarprodukt erscheint in linearer Algebra und Geometrie`() {
        val vorlage = testVorlage(
            art = "mathematik.skalarprodukt",
            name = "Standardskalarprodukt",
            kategorie = "Operatoren",
        )
        val eintrag = KonzeptBibliothekRegister.erstelle(listOf(vorlage))
            .single { it.vorlage == vorlage }

        assertTrue(listOf("lineare-algebra", "skalarprodukte") in eintrag.kategoriePfade)
        assertTrue(listOf("geometrie", "grundobjekte") in eintrag.kategoriePfade)
    }

    @Test
    fun `Anschlussfilter prüft Richtung und Typ`() {
        val matrix = AnschlussArtId("mathematik.matrix")
        val zahl = AnschlussArtId("mathematik.zahl")
        val vorlage = testVorlage(
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "matrix",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = matrix,
                ),
                AnschlussDaten(
                    name = "spur",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = zahl,
                ),
            ),
        )
        val eintrag = KonzeptBibliothekRegister.erstelle(listOf(vorlage)).single { it.vorlage == vorlage }

        assertTrue(eintrag.passt(KonzeptBibliothekFilter(erforderlicherEingang = matrix, erforderlicherAusgang = zahl)))
        assertFalse(eintrag.passt(KonzeptBibliothekFilter(erforderlicherEingang = zahl)))
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

    private fun testVorlage(
        art: String = "mathematik.test",
        name: String = "Test",
        kategorie: String = "Operatoren",
        anschlüsse: List<AnschlussDaten> = listOf(
            AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = AnschlussArtId("mathematik.zahl"),
            ),
        ),
    ) = KnotenVorlage(
        art = art,
        name = name,
        kategorie = kategorie,
        beschreibung = "Testbeschreibung",
        anschlüsse = anschlüsse,
    )
}

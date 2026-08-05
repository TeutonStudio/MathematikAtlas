package de.TeutonStudio.MathematikAtlas

import kotlin.test.Test
import kotlin.test.assertEquals

class KonzeptBibliothekTrefferTest {
    @Test
    fun `aktive Suche entfernt Fachgebiete ohne passende Konzepte`() {
        val kategorien = listOf(
            KonzeptKategorie("analysis", "Analysis"),
            KonzeptKategorie("lineare-algebra", "Lineare Algebra"),
            KonzeptKategorie("geometrie", "Geometrie"),
        )
        val einträge = listOf(
            eintrag("ableitung", "Ableitung", listOf("analysis", "differential-integral")),
            eintrag("matrix", "Matrix", listOf("lineare-algebra", "matrizen")),
            eintrag("dreieck", "Dreieck", listOf("geometrie", "grundobjekte")),
        )

        val treffer = hauptkategorieTreffer(
            kategorien = kategorien,
            einträge = einträge,
            filter = KonzeptBibliothekFilter(suchtext = "matrix"),
        )

        assertEquals(listOf("lineare-algebra"), treffer.map { it.kategorie.id })
        assertEquals(listOf(1), treffer.map { it.anzahl })
    }

    @Test
    fun `Trefferzahl wird aus allen passenden Unterkategorien aggregiert`() {
        val kategorien = listOf(KonzeptKategorie("lineare-algebra", "Lineare Algebra"))
        val einträge = listOf(
            eintrag("matrix", "Matrix", listOf("lineare-algebra", "matrizen")),
            eintrag("vektor", "Vektor", listOf("lineare-algebra", "vektoren")),
            eintrag("ableitung", "Ableitung", listOf("analysis", "differential-integral")),
        )

        val treffer = hauptkategorieTreffer(
            kategorien = kategorien,
            einträge = einträge,
            filter = KonzeptBibliothekFilter(),
        )

        assertEquals(2, treffer.single().anzahl)
    }

    @Test
    fun `ungefilterte erste Ebene behaelt auch leere Fachgebiete`() {
        val kategorien = listOf(
            KonzeptKategorie("analysis", "Analysis"),
            KonzeptKategorie("topologie", "Topologie"),
        )

        val treffer = hauptkategorieTreffer(
            kategorien = kategorien,
            einträge = listOf(eintrag("ableitung", "Ableitung", listOf("analysis"))),
            filter = KonzeptBibliothekFilter(),
        )

        assertEquals(listOf("analysis", "topologie"), treffer.map { it.kategorie.id })
        assertEquals(listOf(1, 0), treffer.map { it.anzahl })
    }

    private fun eintrag(
        id: String,
        titel: String,
        pfad: List<String>,
    ): KonzeptBibliothekEintrag = KonzeptBibliothekEintrag(
        id = id,
        titel = titel,
        beschreibung = "$titel Beschreibung",
        kategoriePfade = listOf(pfad),
        suchbegriffe = setOf(titel),
        verfügbarkeit = KonzeptVerfügbarkeit.Geplant,
    )
}

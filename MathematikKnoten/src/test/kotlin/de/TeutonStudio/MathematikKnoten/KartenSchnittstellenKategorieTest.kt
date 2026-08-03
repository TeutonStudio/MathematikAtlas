package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import kotlin.test.Test
import kotlin.test.assertEquals

class KartenSchnittstellenKategorieTest {
    @Test
    fun `Karten Eingang und Ausgang gehören ausschließlich zu Eigene Karten`() {
        assertEquals("Eigene Karten", MathematikKnotenVorlagen.KartenEingang.kategorie)
        assertEquals("Eigene Karten", MathematikKnotenVorlagen.KartenAusgang.kategorie)
        assertEquals(
            listOf("mathematik.kartenEingang", "mathematik.kartenAusgang"),
            MathematikKnotenVorlagen.alle
                .filter { it.kategorie == "Eigene Karten" }
                .map { it.art },
        )
    }

    @Test
    fun `beide Kartenschnittstellen sind genau einmal im produktiven Katalog`() {
        assertEquals(1, MathematikKnotenVorlagen.alle.count { it === MathematikKnotenVorlagen.KartenEingang })
        assertEquals(1, MathematikKnotenVorlagen.alle.count { it === MathematikKnotenVorlagen.KartenAusgang })
    }

    @Test
    fun `Umkategorisierung bewahrt Art IDs und Anschlussverträge`() {
        val eingang = MathematikKnotenVorlagen.KartenEingang
        val ausgang = MathematikKnotenVorlagen.KartenAusgang

        assertEquals("mathematik.kartenEingang", eingang.art)
        assertEquals("mathematik.kartenAusgang", ausgang.art)
        assertEquals(listOf("wert"), eingang.anschlüsse.map { it.name })
        assertEquals(listOf(AnschlussRichtung.Ausgang), eingang.anschlüsse.map { it.richtung })
        assertEquals(listOf(MathematikAnschlussArten.Objekt.id), eingang.anschlüsse.map { it.art })
        assertEquals(listOf("wert"), ausgang.anschlüsse.map { it.name })
        assertEquals(listOf(AnschlussRichtung.Eingang), ausgang.anschlüsse.map { it.richtung })
        assertEquals(listOf(MathematikAnschlussArten.Objekt.id), ausgang.anschlüsse.map { it.art })
    }
}

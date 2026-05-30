package com.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenControllerZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenAendern
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.mitAktualisiertemPullCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KartenPullCacheTest {
    @Test
    fun pullCacheWirdInAbhaengigkeitsReihenfolgeBerechnet() {
        val karte = beispielKarte().mitAktualisiertemPullCache()

        val quelle = requireNotNull(karte.cache.eintrag("a"))
        val ziel = requireNotNull(karte.cache.eintrag("b"))

        assertEquals("2", quelle.daten["wert"])
        assertTrue(requireNotNull(ziel.daten["eingang"]).contains("2"))
    }

    @Test
    fun unveraenderteCacheEintraegeWerdenWiederverwendet() {
        val karte = beispielKarte().mitAktualisiertemPullCache()
        val geaendert = karte.copy(
            knoten = karte.knoten.map { knoten ->
                if (knoten.id == "b") knoten.copy(name = "B neu") else knoten
            },
        ).mitAktualisiertemPullCache()

        assertSame(karte.cache.eintrag("a"), geaendert.cache.eintrag("a"))
        assertNotSame(karte.cache.eintrag("b"), geaendert.cache.eintrag("b"))
    }

    @Test
    fun controllerAktualisiertCacheNachVerbindungserstellung() {
        val karte = KarteDaten(
            id = "karte",
            name = "Karte",
            knoten = listOf(
                KnotenDaten("a", "A", position = Offset.Zero, data = mapOf("wert" to "5")),
                KnotenDaten("b", "B", position = Offset(100f, 0f), data = mapOf("operator" to "+")),
            ),
        ).mitAktualisiertemPullCache()

        val controller = KartenControllerZustand(karte)
            .fuehreAus(VerbindungErstellen(VerbindungDaten("ab", "a", "out", "b", "in")))

        assertTrue(requireNotNull(controller.karte.cache.eintrag("b")?.daten?.get("eingang")).contains("5"))
    }

    @Test
    fun controllerKannPullCacheDeaktivieren() {
        val karte = beispielKarte()
        val controller = KartenControllerZustand(karte, pullCacheAktiv = false)
            .fuehreAus(KnotenAendern(karte.knoten.first().copy(name = "A neu")))

        assertTrue(controller.karte.cache.knoten.isEmpty())
    }

    private fun beispielKarte() = KarteDaten(
        id = "karte",
        name = "Karte",
        knoten = listOf(
            KnotenDaten("a", "A", position = Offset.Zero, data = mapOf("wert" to "2")),
            KnotenDaten("b", "B", position = Offset(100f, 0f), data = mapOf("operator" to "+")),
        ),
        verbindungen = listOf(
            VerbindungDaten("ab", "a", "out", "b", "in"),
        ),
    )
}

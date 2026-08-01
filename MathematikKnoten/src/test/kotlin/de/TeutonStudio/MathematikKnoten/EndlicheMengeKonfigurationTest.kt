package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class EndlicheMengeKonfigurationTest {
    @Test
    fun `Konfiguration bleibt beim Codec-Roundtrip erhalten`() {
        val erwartet = EndlicheMengeKonfiguration(
            einträge = listOf(
                EndlicheMengeEintrag("z", MathematikAnschlussArten.Zahl.id.wert, EndlicheMengeQuelle.ZahlLiteral("1/2")),
                EndlicheMengeEintrag("t", MathematikAnschlussArten.Tupel.id.wert, EndlicheMengeQuelle.TupelLiteral(listOf("2", "3i"))),
                EndlicheMengeEintrag("m", MathematikAnschlussArten.Menge.id.wert, EndlicheMengeQuelle.Konstante("menge.n")),
            ),
        ).mitErkannterGemeinsamerArt()
        val knoten = knoten(erwartet)
        assertEquals(erwartet, leseEndlicheMengeKonfiguration(knoten).konfiguration)
    }

    @Test
    fun `alte Kommaliste wird geordnet migriert`() {
        val gelesen = leseEndlicheMengeKonfiguration(
            KnotenDaten(
                art = "mathematik.endlicheMenge",
                name = "M",
                parameter = mapOf(ENDLICHE_MENGE_ALT_PARAMETER to "1, 2, 3"),
            ),
        )
        assertTrue(gelesen.altformat)
        assertEquals(listOf("1", "2", "3"), gelesen.konfiguration.einträge.map {
            (it.quelle as EndlicheMengeQuelle.ZahlLiteral).wert
        })
    }

    @Test
    fun `Zahlparser unterstützt rationale dezimale und komplexe Werte`() {
        assertEquals(RationaleZahl.von(1, 2), parseEndlicheMengeZahl("1/2"))
        assertEquals(RationaleZahl.von(5, 4), parseEndlicheMengeZahl("1.25"))
        assertEquals(
            KomplexeZahl(RationaleZahl.von(2), RationaleZahl.von(-3)),
            parseEndlicheMengeZahl("2-3i"),
        )
    }

    @Test
    fun `gemischte Menge behält Anzeigereihenfolge und erkennt Objekt als Oberart`() {
        val config = EndlicheMengeKonfiguration(
            einträge = listOf(
                EndlicheMengeEintrag("z", MathematikAnschlussArten.Zahl.id.wert, EndlicheMengeQuelle.ZahlLiteral("2")),
                EndlicheMengeEintrag("m", MathematikAnschlussArten.Menge.id.wert, EndlicheMengeQuelle.Konstante("menge.n")),
            ),
        ).mitErkannterGemeinsamerArt()
        val ergebnis = auswerten(config)
        val wert = ergebnis.ausgaben.getValue("menge")
        assertIs<EndlicheMenge>(wert.objekt)
        assertEquals("\\{2, \\mathbb{N}\\}", wert.latexDarstellung)
        assertEquals(MathematikAnschlussArten.Objekt.id, wert.elementArt)
    }

    @Test
    fun `leere Liste ergibt die leere Menge`() {
        val ergebnis = auswerten(EndlicheMengeKonfiguration())
        assertSame(LeereMenge, ergebnis.ausgaben.getValue("menge").objekt)
        assertEquals("\\varnothing", ergebnis.ausgaben.getValue("menge").latexDarstellung)
    }

    @Test
    fun `Duplikate werden semantisch zusammengeführt und gemeldet`() {
        val config = EndlicheMengeKonfiguration(
            einträge = listOf(
                EndlicheMengeEintrag("a", MathematikAnschlussArten.Zahl.id.wert, EndlicheMengeQuelle.ZahlLiteral("2")),
                EndlicheMengeEintrag("b", MathematikAnschlussArten.Zahl.id.wert, EndlicheMengeQuelle.ZahlLiteral("2/1")),
            ),
        )
        val normalisiert = normalisiereEndlicheMengeKonfiguration(config)
        assertEquals(1, normalisiert.konfiguration.einträge.size)
        assertTrue(normalisiert.warnungen.isNotEmpty())
        val ergebnis = auswerten(config)
        assertEquals(1, assertIs<EndlicheMenge>(ergebnis.ausgaben.getValue("menge").objekt).elemente.size)
        assertTrue(ergebnis.warnungen.isNotEmpty())
    }

    @Test
    fun `ungültige Elemente liefern einen Fehler an ihrer stabilen ID`() {
        val config = EndlicheMengeKonfiguration(
            einträge = listOf(
                EndlicheMengeEintrag("kaputt", MathematikAnschlussArten.Zahl.id.wert, EndlicheMengeQuelle.ZahlLiteral("nicht-zahl")),
            ),
        )
        val ergebnis = auswerten(config)
        assertNotNull(ergebnis.elementFehler["kaputt"])
        assertNotNull(ergebnis.fehler)
    }

    private fun knoten(config: EndlicheMengeKonfiguration) = KnotenDaten(
        art = "mathematik.endlicheMenge",
        name = "M",
        parameter = mapOf(ENDLICHE_MENGE_KONFIGURATION_PARAMETER to config.zuParameter()),
    )

    private fun auswerten(config: EndlicheMengeKonfiguration) = EndlicheMengeAuswerter.auswerten(
        KnotenAuswertungsKontext(knoten(config), emptyMap(), RechenKontext()),
    )
}

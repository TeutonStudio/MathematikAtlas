package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.*
import kotlin.test.Test
import kotlin.test.assertEquals

class AlgebraischePotenzKartenJsonTest {
    @Test
    fun `Potenzknoten roundtrippt Ordnung Strukturmodus und Anschluss IDs`() {
        val basis = AlgebraischePotenzKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero)
        val knoten = basis.copy(
            parameter = basis.parameter + mapOf(
                POTENZ_ORDNUNG_PARAMETER to "12",
                POTENZ_STRUKTUR_MODUS_PARAMETER to PotenzStrukturModus.EXPLIZIT.name,
                POTENZ_STRUKTUR_ID_PARAMETER to "test.struktur",
            ),
        )
        val gelesen = KartenJson.lese(
            KartenJson.schreibe(KartenDaten(name = "Potenz", knoten = listOf(knoten))),
        ).knoten.single()

        assertEquals(ALGEBRAISCHE_POTENZ_KNOTEN_ART, gelesen.art)
        assertEquals("12", gelesen.parameter[POTENZ_ORDNUNG_PARAMETER])
        assertEquals(PotenzStrukturModus.EXPLIZIT.name, gelesen.parameter[POTENZ_STRUKTUR_MODUS_PARAMETER])
        assertEquals("test.struktur", gelesen.parameter[POTENZ_STRUKTUR_ID_PARAMETER])
        assertEquals(knoten.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }

    @Test
    fun `historischer Strukturpotenzknoten wird beim Lesen migriert`() {
        val alt = KnotenDaten(
            art = "mathematik.potenzStrukturell",
            name = "Alt",
            position = GraphPunkt.Zero,
            anschlüsse = AlgebraischePotenzKnotenVorlagen.Potenz.anschlüsse,
            parameter = mapOf("exponent" to "7"),
        )
        val roh = KartenDatenJson.schreibe(KartenDaten(name = "Alt", knoten = listOf(alt)))

        val gelesen = KartenJson.lese(roh).knoten.single()

        assertEquals(ALGEBRAISCHE_POTENZ_KNOTEN_ART, gelesen.art)
        assertEquals("7", gelesen.parameter[POTENZ_ORDNUNG_PARAMETER])
        assertEquals(PotenzStrukturModus.AUTO.name, gelesen.parameter[POTENZ_STRUKTUR_MODUS_PARAMETER])
        assertEquals(alt.anschlüsse.map { it.id }, gelesen.anschlüsse.map { it.id })
    }

    @Test
    fun `historische Zahlenpotenz bleibt fuer Zahlenrechnermigration unberuehrt`() {
        val alt = KnotenDaten(
            art = "mathematik.potenz",
            name = "Zahlenpotenz",
            position = GraphPunkt.Zero,
        )
        val roh = KartenDatenJson.schreibe(KartenDaten(name = "Zahl", knoten = listOf(alt)))

        val gelesen = KartenJson.lese(roh).knoten.single()

        assertEquals("mathematik.potenz", gelesen.art)
    }
}

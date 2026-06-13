package de.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenControllerZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.KartenLayoutAnwenden
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.StandardKartenLayout
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KartenLayoutTest {
    @Test
    fun standardlayoutOrdnetAbhaengigkeitenInSpaltenAn() {
        val layout = StandardKartenLayout(
            start = Offset(10f, 20f),
            spaltenAbstand = 100f,
            zeilenAbstand = 50f,
        )

        val karte = layout.berechneLayout(beispielKarte())

        assertEquals(Offset(10f, 20f), karte.knoten.first { it.id == "a" }.position)
        assertEquals(Offset(110f, 20f), karte.knoten.first { it.id == "b" }.position)
        assertEquals(Offset(210f, 20f), karte.knoten.first { it.id == "d" }.position)
        assertEquals(Offset(10f, 70f), karte.knoten.first { it.id == "c" }.position)
        assertEquals(listOf("ab", "bd"), karte.verbindungen.map { it.id })
    }

    @Test
    fun layoutCommandIstRueckgaengigMachbar() {
        val vorher = beispielKarte()
        val controller = KartenControllerZustand(vorher)
            .fuehreAus(KartenLayoutAnwenden(StandardKartenLayout(start = Offset.Zero)))

        assertTrue(controller.kannRueckgaengig)
        assertEquals(vorher.knoten.map { it.position }, controller.rueckgaengig().karte.knoten.map { it.position })
    }

    private fun beispielKarte() = KarteDaten(
        id = "karte",
        name = "Karte",
        knoten = listOf(
            KnotenDaten("a", "A", position = Offset(400f, 400f)),
            KnotenDaten("b", "B", position = Offset(300f, 300f)),
            KnotenDaten("c", "C", position = Offset(200f, 200f)),
            KnotenDaten("d", "D", position = Offset(100f, 100f)),
        ),
        verbindungen = listOf(
            VerbindungDaten("ab", "a", "out", "b", "in"),
            VerbindungDaten("bd", "b", "out", "d", "in"),
        ),
    )
}

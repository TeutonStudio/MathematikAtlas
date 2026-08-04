package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.*

class ZahlenRechnerFormelEingangPersistenzTest {
    @Test
    fun `Formeleingänge bleiben nach dem Ersetzen im Editor sichtbar`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val formel = konfiguriereZahlenRechnerFormel(basis, "x^2+y")
        val zustand = KartenEditorZustand(
            startKarte = KartenDaten(name = "Test", knoten = listOf(basis)),
            prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)),
        )

        zustand.führeAus(KartenAktion.KnotenErsetzen(formel))

        val eingänge = zustand.karte.knoten.single().anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
        assertEquals(listOf("x", "y"), eingänge.map { it.name })
        assertTrue(eingänge.none { it.dynamischErzeugt })
    }
}

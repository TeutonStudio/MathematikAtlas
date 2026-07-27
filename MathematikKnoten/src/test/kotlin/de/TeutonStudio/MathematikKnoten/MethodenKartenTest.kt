package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.KartenQuelle
import de.TeutonStudio.MathematikRechenSystem.kern.Funktion
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MethodenKartenTest {
    @Test
    fun `Karten-Ausgang übergibt Zielmenge an exportierte Methode`() {
        val eingang = MathematikKnotenVorlagen.KartenEingang.erzeuge(GraphPunkt.Zero)
        val ausgang = MathematikKnotenVorlagen.KartenAusgang.erzeuge(GraphPunkt.Zero)
        val zielmenge = MathematikKnotenVorlagen.ReelleZahlen.erzeuge(GraphPunkt.Zero)
        val intern = KarteBauer("Identität")
            .knoten(eingang, ausgang, zielmenge)
            .verbinde(eingang, "wert", ausgang, "wert")
            .verbinde(zielmenge, "menge", ausgang, "zielmenge")
            .baue()
        val methode = KnotenDaten(
            art = "methode.${intern.id.wert}", name = "Identität", position = GraphPunkt.Zero,
            anschlüsse = listOf(AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.ZahlFunktion.id)),
            kartenVerweis = KartenVerweis(intern.id, intern.version),
        )

        val ergebnis = KartenAuswerter(
            StandardMathematikAuswerter.erzeugeRegister(),
            KartenQuelle { if (it == KartenVerweis(intern.id, intern.version)) intern else null },
        ).auswerten(KartenDaten(name = "Außen", knoten = listOf(methode)))

        val funktion = assertIs<Funktion>(ergebnis.knoten.getValue(methode.id).ausgaben.getValue("methode").objekt)
        assertEquals(ReelleZahlen, funktion.einzigeZielMenge)
    }

    private class KarteBauer(name: String) {
        private var karte = KartenDaten(name = name)
        fun knoten(vararg knoten: KnotenDaten) = apply { karte = karte.copy(knoten = karte.knoten + knoten) }
        fun verbinde(von: KnotenDaten, vonName: String, zu: KnotenDaten, zuName: String) = apply {
            karte = karte.copy(verbindungen = karte.verbindungen + VerbindungDaten(
                von = AnschlussVerweis(von.id, von.anschlüsse.first { it.name == vonName }.id),
                zu = AnschlussVerweis(zu.id, zu.anschlüsse.first { it.name == zuName }.id),
            ))
        }
        fun baue() = karte
    }
}

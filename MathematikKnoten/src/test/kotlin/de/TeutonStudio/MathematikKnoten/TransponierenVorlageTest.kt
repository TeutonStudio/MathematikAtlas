package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import kotlin.test.*

class TransponierenVorlageTest {
    @Test fun `Dialog listet nur den Universalknoten`() {
        val vorlagen = MathematikKnotenVorlagen.alle.filter { it.art.startsWith("mathematik.transponier") }
        assertEquals(listOf("mathematik.transponieren"), vorlagen.map { it.art })
    }

    @Test fun `Vorlage begrenzt Eingang und bildet Ausgangstypen ab`() {
        val knoten = MathematikKnotenVorlagen.Transponieren.erzeuge(de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt.Zero)
        val eingang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }
        val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals(
            setOf(
                MathematikAnschlussArten.SpaltenVektor.id,
                MathematikAnschlussArten.ZeilenVektor.id,
                MathematikAnschlussArten.Matrix.id,
                MathematikAnschlussArten.Tensor.id,
            ),
            eingang.zulässigeArten,
        )
        assertEquals(MathematikAnschlussArten.ZeilenVektor.id, ausgang.artAbbildungVonEingang?.abbildung?.get(MathematikAnschlussArten.SpaltenVektor.id))
        assertEquals(MathematikAnschlussArten.SpaltenVektor.id, ausgang.artAbbildungVonEingang?.abbildung?.get(MathematikAnschlussArten.ZeilenVektor.id))
    }
}

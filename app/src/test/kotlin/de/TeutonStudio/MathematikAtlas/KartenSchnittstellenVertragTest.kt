package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeTypen
import de.TeutonStudio.TypSystem.AnschlussVertrag
import de.TeutonStudio.TypSystem.TypAusdruck
import kotlin.test.Test
import kotlin.test.assertEquals

class KartenSchnittstellenVertragTest {
    @Test
    fun `oeffentlicher Karteneingang bewahrt semantischen Methodenvertrag`() {
        val methodenTyp = TypAusdruck.Parameterisiert(
            MathematischeTypen.Methode,
            listOf(
                TypAusdruck.Parameterisiert(
                    MathematischeTypen.Tupel,
                    listOf(TypAusdruck.Atom(MathematischeTypen.Reell)),
                ),
                TypAusdruck.Atom(MathematischeTypen.Komplex),
            ),
        )
        val vertrag = AnschlussVertrag(methodenTyp)
        val intern = KnotenDaten(
            art = "mathematik.kartenEingang",
            name = "Methode",
            parameter = mapOf("name" to "f"),
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Methode.id,
                    vertrag = vertrag,
                ),
            ),
        )

        val öffentlich = öffentlicheKartenAnschlüsseMitVertrag(
            KartenDaten(name = "Innere Karte", knoten = listOf(intern)),
            "mathematik.kartenEingang",
            AnschlussRichtung.Eingang,
            AnschlussKante.Links,
        ).single()

        assertEquals("f", öffentlich.name)
        assertEquals(MathematikAnschlussArten.Methode.id, öffentlich.art)
        assertEquals(vertrag, öffentlich.vertrag)
    }
}

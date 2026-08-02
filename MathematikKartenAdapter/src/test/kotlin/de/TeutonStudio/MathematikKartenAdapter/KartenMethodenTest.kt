package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class KartenMethodenTest {
    @Test fun `Kartenmethode fasst mehrere Ausgänge zu einem Ergebnistupel zusammen`() {
        val zahlArt = AnschlussArtId("mathematik.zahl")
        fun eingang(id: String, name: String, y: Float) = KnotenDaten(
            id = KnotenId(id), art = "mathematik.kartenEingang", name = name, position = GraphPunkt(0f, y),
            parameter = mapOf("name" to name),
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = zahlArt)),
        )
        fun ausgang(id: String, name: String, y: Float) = KnotenDaten(
            id = KnotenId(id), art = "mathematik.kartenAusgang", name = name, position = GraphPunkt(400f, y),
            parameter = mapOf("name" to name),
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = zahlArt)),
        )
        val x = eingang("x", "x", 0f)
        val y = eingang("y", "y", 100f)
        val summe = ausgang("summe", "summe", 0f)
        val erster = ausgang("erster", "erster", 100f)
        val intern = KartenDaten(
            id = KartenId("intern"), name = "Paar", version = 1,
            knoten = listOf(x, y, summe, erster),
            verbindungen = listOf(
                VerbindungDaten(von = AnschlussVerweis(x.id, x.anschlüsse.single().id), zu = AnschlussVerweis(summe.id, summe.anschlüsse.single().id)),
                VerbindungDaten(von = AnschlussVerweis(x.id, x.anschlüsse.single().id), zu = AnschlussVerweis(erster.id, erster.anschlüsse.single().id)),
            ),
        )
        val methode = KnotenDaten(
            art = "methode.intern", name = "f", kartenVerweis = KartenVerweis(intern.id, intern.version),
            anschlüsse = listOf(AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId("mathematik.methode"))),
        )
        val register = MathematikAuswerterRegister().apply {
            registriere("mathematik.kartenAusgang") { kontext ->
                val wert = kontext.eingänge.getValue("wert")
                KnotenAuswertungsErgebnis(mapOf("wert" to wert.copy(zielMenge = ReelleZahlen)))
            }
        }

        val ergebnis = KartenAuswerter(register, KartenQuelle { if (it == methode.kartenVerweis) intern else null })
            .auswerten(KartenDaten(name = "Außen", knoten = listOf(methode)))
        val funktion = assertIs<Methode>(ergebnis.knoten.getValue(methode.id).ausgaben.getValue("methode").objekt)

        assertEquals(listOf("x", "y"), funktion.parameter.map { it.name })
        assertEquals(listOf("wert"), funktion.ausgabeNamen)
        assertEquals(ReelleZahlen, funktion.zielMenge)
        assertEquals(Tupel(listOf(Variable("x"), Variable("x"))), funktion.vorschrift)
        assertEquals(Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), funktion.zielMenge)
    }
}

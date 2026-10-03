package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class BildmengeSynchronisierungTest {
    @Test
    fun `Einzelmengenmodus erzeugt genau eine Menge je Methodenargument`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val knoten = MathematikKnotenVorlagen.Abbild.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf(BILDMENGE_ARGUMENT_MODUS to BILDMENGE_MODUS_EINZELMENGEN),
        )
        val auswertung = KartenAuswertungsErgebnis(
            knoten = mapOf(
                knoten.id to KnotenAuswertungsErgebnis(
                    ausgaben = emptyMap(),
                    eingänge = mapOf("methode" to BedingterWert(methode)),
                ),
            ),
            fehler = emptyList(),
        )

        val einmal = synchronisiereBildmengenAnschlüsse(KartenDaten(name = "Test", knoten = listOf(knoten)), auswertung)
        val zweimal = synchronisiereBildmengenAnschlüsse(einmal, auswertung)
        val ergebnis = zweimal.knoten.single()
        val eingänge = ergebnis.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }

        assertEquals(
            listOf("methode", bildmengeArgumentName(0), bildmengeArgumentName(1)),
            eingänge.sortedBy { it.reihenfolge }.map { it.name },
        )
        assertTrue(eingänge.drop(1).all { it.art == MathematikAnschlussArten.Menge.id })
        assertEquals(
            einmal.knoten.single().anschlüsse.map { it.id },
            zweimal.knoten.single().anschlüsse.map { it.id },
        )
    }

    @Test
    fun `Wechsel zum Produktmodus entfernt Kanten der Einzelmengen`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val basis = MathematikKnotenVorlagen.Abbild.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf(BILDMENGE_ARGUMENT_MODUS to BILDMENGE_MODUS_EINZELMENGEN),
        )
        val auswertung = KartenAuswertungsErgebnis(
            mapOf(
                basis.id to KnotenAuswertungsErgebnis(
                    ausgaben = emptyMap(),
                    eingänge = mapOf("methode" to BedingterWert(methode)),
                ),
            ),
            emptyList(),
        )
        val einzel = synchronisiereBildmengenAnschlüsse(KartenDaten(name = "Test", knoten = listOf(basis)), auswertung)
        val bild = einzel.knoten.single()
        val argument = bild.anschlüsse.first { it.name == bildmengeArgumentName(0) }
        val quelle = KnotenDaten(
            art = "test.menge",
            name = "Menge",
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "menge",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Menge.id,
                ),
            ),
        )
        val mitKante = einzel.copy(
            knoten = listOf(quelle, bild.copy(
                parameter = bild.parameter + (BILDMENGE_ARGUMENT_MODUS to BILDMENGE_MODUS_PRODUKT),
            )),
            verbindungen = listOf(
                VerbindungDaten(
                    von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                    zu = AnschlussVerweis(bild.id, argument.id),
                ),
            ),
        )

        val produkt = synchronisiereBildmengenAnschlüsse(mitKante, auswertung)
        val produktKnoten = produkt.knoten.first { it.id == bild.id }

        assertEquals(
            listOf("menge", "methode"),
            produktKnoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
                .sortedBy { it.reihenfolge }
                .map { it.name },
        )
        assertTrue(produkt.verbindungen.isEmpty())
    }
}

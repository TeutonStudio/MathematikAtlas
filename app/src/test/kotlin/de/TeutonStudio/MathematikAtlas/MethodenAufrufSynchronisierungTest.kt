package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.FaltungsKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MethodenAufrufSynchronisierungTest {
    private val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))

    @Test
    fun `einstellige Zahlmethode erzeugt genau einen typisierten Argumentanschluss`() {
        val knoten = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero)
        val x = Variable("x")
        val methode = Funktion(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to addition(x, RationaleZahl.Eins)),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        val synchronisiert = synchronisiere(knoten, methode)
        val ergebnisKnoten = synchronisiert.knoten.single()
        val argumente = ergebnisKnoten.argumente()

        assertEquals(1, argumente.size)
        assertEquals("argument-0", argumente.single().name)
        assertEquals(MathematikAnschlussArten.Zahl.id, argumente.single().art)
        assertEquals(MathematikAnschlussArten.Zahl.id, ergebnisKnoten.wertAusgang().art)
        assertEquals("1", ergebnisKnoten.parameter[METHODEN_AUFRUF_STELLIGKEIT])
        assertEquals("x", ergebnisKnoten.parameter["${METHODEN_AUFRUF_PARAMETER_PREFIX}0.name"])
        assertEquals(ReelleZahlen.zuLatex(), ergebnisKnoten.parameter["${METHODEN_AUFRUF_PARAMETER_PREFIX}0.werteVorrat"])
        assertEquals(ReelleZahlen.zuLatex(), ergebnisKnoten.parameter[METHODEN_AUFRUF_ZIELMENGE])
    }

    @Test
    fun `mehrstellige Methode erhält Anschluss IDs indexstabil und in Parameterreihenfolge`() {
        val knoten = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero)
        val ursprünglicheIds = knoten.argumente().map { it.id }
        val x = Variable("x")
        val menge = MengenParameter("A")
        val methode = Funktion(
            name = "g",
            parameter = listOf(x, menge),
            ausgaben = mapOf("wert" to menge),
            zielMengen = mapOf("wert" to Potenzmenge(ReelleZahlen)),
            werteVorräte = mapOf(x.name to ReelleZahlen, menge.name to Potenzmenge(ReelleZahlen)),
        )

        val einmal = synchronisiere(knoten, methode)
        val zweimal = synchronisiere(einmal.knoten.single(), methode)
        val argumente = zweimal.knoten.single().argumente()

        assertEquals(listOf("argument-0", "argument-1"), argumente.map { it.name })
        assertEquals(listOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Menge.id), argumente.map { it.art })
        assertEquals(ursprünglicheIds, argumente.map { it.id })
        assertEquals(einmal.knoten.single().anschlüsse.map { it.id }, zweimal.knoten.single().anschlüsse.map { it.id })
        assertEquals(MathematikAnschlussArten.Menge.id, zweimal.knoten.single().wertAusgang().art)
    }

    @Test
    fun `nullstellige Methode entfernt Argumentanschlüsse`() {
        val knoten = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero)
        val methode = Funktion(
            name = "c",
            parameter = emptyList(),
            ausgaben = mapOf("wert" to RationaleZahl.von(7)),
            zielMengen = mapOf("wert" to GanzeZahlen),
        )

        val ergebnisKnoten = synchronisiere(knoten, methode).knoten.single()

        assertTrue(ergebnisKnoten.argumente().isEmpty())
        assertEquals("0", ergebnisKnoten.parameter[METHODEN_AUFRUF_STELLIGKEIT])
        assertEquals(MathematikAnschlussArten.Zahl.id, ergebnisKnoten.wertAusgang().art)
    }

    @Test
    fun `unbekannter Vertrag fällt auf einen allgemeinen erweiterbaren Anschluss zurück`() {
        val knoten = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero)
        val karte = KartenDaten(name = "Test", knoten = listOf(knoten))

        val synchronisiert = synchronisiereMethodenAufrufe(
            karte,
            KartenAuswertungsErgebnis(emptyMap(), emptyList()),
            prüfung,
        ).knoten.single()

        assertEquals(1, synchronisiert.argumente().size)
        assertTrue(synchronisiert.argumente().single().kannSichErweitern)
        assertEquals(MathematikAnschlussArten.Objekt.id, synchronisiert.argumente().single().art)
        assertEquals(MathematikAnschlussArten.Objekt.id, synchronisiert.wertAusgang().art)
        assertFalse(METHODEN_AUFRUF_STELLIGKEIT in synchronisiert.parameter)
    }

    private fun synchronisiere(knoten: KnotenDaten, methode: Funktion): KartenDaten {
        val karte = KartenDaten(name = "Test", knoten = listOf(knoten))
        val ergebnis = KnotenAuswertungsErgebnis(
            ausgaben = emptyMap(),
            eingänge = mapOf("methode" to BedingterWert(methode)),
        )
        return synchronisiereMethodenAufrufe(
            karte,
            KartenAuswertungsErgebnis(mapOf(knoten.id to ergebnis), emptyList()),
            prüfung,
        )
    }

    private fun KnotenDaten.argumente() = anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }
        .sortedBy { it.reihenfolge }

    private fun KnotenDaten.wertAusgang() = anschlüsse.single {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "wert"
    }
}

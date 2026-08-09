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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MethodenAufrufSynchronisierungTest {
    private val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))

    @Test
    fun `einstellige Zahlmethode erzeugt genau einen typisierten Argumentanschluss`() {
        val knoten = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero)
        val x = Variable("x")
        val methode = Methode(
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
        assertEquals(AnschlussId("${knoten.id.wert}:methodenAufruf:argument:0"), argumente.single().id)
        assertEquals(MathematikAnschlussArten.Zahl.id, argumente.single().art)
        assertEquals(MathematikAnschlussArten.Zahl.id, ergebnisKnoten.wertAusgang().art)
        assertEquals("1", ergebnisKnoten.parameter[METHODEN_AUFRUF_STELLIGKEIT])
        assertEquals("x", ergebnisKnoten.parameter["${METHODEN_AUFRUF_PARAMETER_PREFIX}0.name"])
        assertEquals(ReelleZahlen.zuLatex(), ergebnisKnoten.parameter["${METHODEN_AUFRUF_PARAMETER_PREFIX}0.werteVorrat"])
        assertEquals(ReelleZahlen.zuLatex(), ergebnisKnoten.parameter[METHODEN_AUFRUF_ZIELMENGE])
        assertEquals(METHODEN_ERGEBNISPROJEKTION_DIREKT, ergebnisKnoten.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION])
    }

    @Test
    fun `mehrstellige Methode erhält deterministische Anschluss IDs und Parameterreihenfolge`() {
        val knoten = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero)
        val ursprünglicheIds = knoten.argumente().map { it.id }
        val x = Variable("x")
        val menge = MengenParameter("A")
        val methode = Methode(
            name = "g",
            parameter = listOf(x, menge),
            ausgaben = mapOf("wert" to menge),
            zielMengen = mapOf("wert" to Potenzmenge(ReelleZahlen)),
            werteVorräte = mapOf(x.name to ReelleZahlen, menge.name to Potenzmenge(ReelleZahlen)),
        )

        val einmal = synchronisiere(knoten, methode)
        val zweimal = synchronisiere(einmal.knoten.single(), methode)
        val argumente = zweimal.knoten.single().argumente()
        val erwarteteIds = listOf(
            AnschlussId("${knoten.id.wert}:methodenAufruf:argument:0"),
            AnschlussId("${knoten.id.wert}:methodenAufruf:argument:1"),
        )

        assertEquals(listOf("argument-0", "argument-1"), argumente.map { it.name })
        assertEquals(listOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Menge.id), argumente.map { it.art })
        assertNotEquals(ursprünglicheIds, argumente.map { it.id })
        assertEquals(erwarteteIds, argumente.map { it.id })
        assertEquals(einmal.knoten.single().anschlüsse.map { it.id }, zweimal.knoten.single().anschlüsse.map { it.id })
        assertEquals(MathematikAnschlussArten.Menge.id, zweimal.knoten.single().wertAusgang().art)
    }

    @Test
    fun `bestehende Argumentkante wird auf deterministische Anschluss ID migriert`() {
        val knoten = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero)
        val altesArgument = knoten.argumente().first()
        val quelle = KnotenDaten(
            id = KnotenId("quelle"),
            art = "test.zahl",
            name = "Zahl",
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("quelle-wert"),
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Zahl.id,
                ),
            ),
        )
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
            zu = AnschlussVerweis(knoten.id, altesArgument.id),
        )
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val ergebnis = KnotenAuswertungsErgebnis(
            ausgaben = emptyMap(),
            eingänge = mapOf("methode" to BedingterWert(methode)),
        )
        val karte = KartenDaten(name = "Test", knoten = listOf(quelle, knoten), verbindungen = listOf(verbindung))

        val synchronisiert = synchronisiereMethodenAufrufe(
            karte,
            KartenAuswertungsErgebnis(mapOf(knoten.id to ergebnis), emptyList()),
            prüfung,
        )

        assertEquals(1, synchronisiert.verbindungen.size)
        assertEquals(
            AnschlussId("${knoten.id.wert}:methodenAufruf:argument:0"),
            synchronisiert.verbindungen.single().zu.anschlussId,
        )
    }

    @Test
    fun `nullstellige Methode entfernt Argumentanschlüsse`() {
        val knoten = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero)
        val methode = Methode(
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
    fun `unbekannter Vertrag erzwingt Argumenttupel und Ergebnistupel`() {
        val knoten = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero)
        val karte = KartenDaten(name = "Test", knoten = listOf(knoten))

        val synchronisiert = synchronisiereMethodenAufrufe(
            karte,
            KartenAuswertungsErgebnis(emptyMap(), emptyList()),
            prüfung,
        ).knoten.single()

        assertEquals(listOf("argument-0"), synchronisiert.argumente().map { it.name })
        assertEquals(MathematikAnschlussArten.Tupel.id, synchronisiert.argumente().single().art)
        assertFalse(synchronisiert.argumente().single().kannSichErweitern)
        assertEquals(MathematikAnschlussArten.Tupel.id, synchronisiert.wertAusgang().art)
        assertEquals(METHODEN_ARGUMENTPROJEKTION_TUPEL, synchronisiert.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION])
        assertEquals(METHODEN_ERGEBNISPROJEKTION_TUPEL, synchronisiert.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION])
        assertFalse(METHODEN_AUFRUF_STELLIGKEIT in synchronisiert.parameter)
    }

    @Test
    fun `nicht tupelige Zielmenge kann als Einertupel projiziert werden`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val quelle = methodenQuelle(
            argumentProjektion = METHODEN_ARGUMENTPROJEKTION_SEPARIERT,
            ergebnisProjektion = METHODEN_ERGEBNISPROJEKTION_TUPEL,
        )
        val aufruf = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt(300f, 0f))
        val verbindung = methodenVerbindung(quelle, aufruf)
        val auswertung = KnotenAuswertungsErgebnis(
            ausgaben = emptyMap(),
            eingänge = mapOf("methode" to BedingterWert(methode)),
        )

        val synchronisiert = synchronisiereMethodenAufrufe(
            KartenDaten(name = "Test", knoten = listOf(quelle, aufruf), verbindungen = listOf(verbindung)),
            KartenAuswertungsErgebnis(mapOf(aufruf.id to auswertung), emptyList()),
            prüfung,
        ).knoten.single { it.id == aufruf.id }

        assertEquals(MathematikAnschlussArten.Tupel.id, synchronisiert.wertAusgang().art)
        assertEquals(METHODEN_ERGEBNISPROJEKTION_TUPEL, synchronisiert.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION])
        assertEquals(Tupelraum(listOf(ReelleZahlen)).zuLatex(), synchronisiert.parameter[METHODEN_AUFRUF_ZIELMENGE])
    }

    @Test
    fun `bereits tupelige Zielmenge bleibt auch ohne explizite Projektion ein Tupelausgang`() {
        val x = Variable("x")
        val ziel = Tupelraum(listOf(ReelleZahlen))
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to Tupel(listOf(x))),
            zielMengen = mapOf("wert" to ziel),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val ergebnisKnoten = synchronisiere(FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero), methode)
            .knoten.single()

        assertEquals(MathematikAnschlussArten.Tupel.id, ergebnisKnoten.wertAusgang().art)
        assertEquals(ziel.zuLatex(), ergebnisKnoten.parameter[METHODEN_AUFRUF_ZIELMENGE])
        assertEquals(METHODEN_ERGEBNISPROJEKTION_TUPEL, ergebnisKnoten.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION])
    }

    @Test
    fun `Methodenargumente folgen im separierten Modus der Projektion des Methodenausgangs`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to addition(x, y)),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val quelle = methodenQuelle(METHODEN_ARGUMENTPROJEKTION_SEPARIERT)
        val argumentKnoten = FaltungsKnotenVorlagen.MethodenArgumente.erzeuge(GraphPunkt(300f, 0f))
        val verbindung = methodenVerbindung(quelle, argumentKnoten)
        val auswertung = KnotenAuswertungsErgebnis(
            ausgaben = emptyMap(),
            eingänge = mapOf("methode" to BedingterWert(methode)),
        )

        val synchronisiert = synchronisiereMethodenAufrufe(
            KartenDaten(name = "Test", knoten = listOf(quelle, argumentKnoten), verbindungen = listOf(verbindung)),
            KartenAuswertungsErgebnis(mapOf(argumentKnoten.id to auswertung), emptyList()),
            prüfung,
        ).knoten.single { it.id == argumentKnoten.id }
        val ausgänge = synchronisiert.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Ausgang }
            .sortedBy { it.reihenfolge }

        assertEquals(listOf("x", "y", "dimension"), ausgänge.map { it.name })
        assertEquals(
            listOf(MathematikAnschlussArten.Objekt.id, MathematikAnschlussArten.Objekt.id, MathematikAnschlussArten.Zahl.id),
            ausgänge.map { it.art },
        )
        assertEquals(METHODEN_ARGUMENTPROJEKTION_SEPARIERT, synchronisiert.parameter[METHODEN_ARGUMENTE_PROJEKTION])
    }

    @Test
    fun `Methodenargumente bleiben bei unbekannter Signatur unabhängig von Quellprojektion ein Tupel`() {
        val quelle = methodenQuelle(METHODEN_ARGUMENTPROJEKTION_SEPARIERT)
        val argumentKnoten = FaltungsKnotenVorlagen.MethodenArgumente.erzeuge(GraphPunkt(300f, 0f))
        val verbindung = methodenVerbindung(quelle, argumentKnoten)

        val synchronisiert = synchronisiereMethodenAufrufe(
            KartenDaten(name = "Test", knoten = listOf(quelle, argumentKnoten), verbindungen = listOf(verbindung)),
            KartenAuswertungsErgebnis(emptyMap(), emptyList()),
            prüfung,
        ).knoten.single { it.id == argumentKnoten.id }
        val ausgänge = synchronisiert.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals(listOf("argumente"), ausgänge.map { it.name })
        assertEquals(MathematikAnschlussArten.Tupel.id, ausgänge.single().art)
        assertEquals(METHODEN_ARGUMENTPROJEKTION_TUPEL, synchronisiert.parameter[METHODEN_ARGUMENTE_PROJEKTION])
    }

    private fun synchronisiere(knoten: KnotenDaten, methode: Methode): KartenDaten {
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

    private fun methodenQuelle(
        argumentProjektion: String,
        ergebnisProjektion: String = METHODEN_ERGEBNISPROJEKTION_DIREKT,
    ): KnotenDaten {
        val id = KnotenId("methoden-quelle")
        return KnotenDaten(
            id = id,
            art = "test.methode",
            name = "Methode",
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("methoden-quelle-methode"),
                    name = "methode",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Methode.id,
                ),
            ),
            parameter = mapOf(
                methodenAusgangArgumentprojektionSchlüssel("methode") to argumentProjektion,
                methodenAusgangErgebnisprojektionSchlüssel("methode") to ergebnisProjektion,
            ),
        )
    }

    private fun methodenVerbindung(quelle: KnotenDaten, ziel: KnotenDaten): VerbindungDaten {
        val von = quelle.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
        val zu = ziel.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang && it.name == "methode" }
        return VerbindungDaten(
            von = AnschlussVerweis(quelle.id, von.id),
            zu = AnschlussVerweis(ziel.id, zu.id),
        )
    }

    private fun KnotenDaten.argumente() = anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }
        .sortedBy { it.reihenfolge }

    private fun KnotenDaten.wertAusgang() = anschlüsse.single {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "wert"
    }
}

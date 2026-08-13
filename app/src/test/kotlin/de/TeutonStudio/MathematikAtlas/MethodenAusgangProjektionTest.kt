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
import kotlin.test.assertTrue

class MethodenAusgangProjektionTest {
    private val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))

    @Test
    fun `Tupelprojektion des Methodenaufrufs erzeugt genau einen Tupelanschluss`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to addition(x, y)),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val (karte, aufruf) = verbundeneKarte(methode, METHODEN_ARGUMENTPROJEKTION_TUPEL)

        val synchronisiert = synchronisiereMethodenAufrufe(
            karte,
            auswertungFür(aufruf, methode),
            prüfung,
        )
        val argumente = synchronisiert.knoten.single { it.id == aufruf.id }.argumente()

        assertEquals(1, argumente.size)
        assertEquals(MathematikAnschlussArten.Tupel.id, argumente.single().art)
        assertEquals(METHODEN_ARGUMENTPROJEKTION_TUPEL, synchronisiert.knoten.single { it.id == aufruf.id }.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION])
        assertEquals("2", synchronisiert.knoten.single { it.id == aufruf.id }.parameter[METHODEN_AUFRUF_STELLIGKEIT])
    }

    @Test
    fun `separierte Projektion des Methodenaufrufs erzeugt einen Anschluss je Argument`() {
        val x = Variable("x")
        val y = MengenParameter("A")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to Potenzmenge(ReelleZahlen)),
        )
        val (karte, aufruf) = verbundeneKarte(methode, METHODEN_ARGUMENTPROJEKTION_SEPARIERT)

        val synchronisiert = synchronisiereMethodenAufrufe(
            karte,
            auswertungFür(aufruf, methode),
            prüfung,
        )
        val argumente = synchronisiert.knoten.single { it.id == aufruf.id }.argumente()

        assertEquals(2, argumente.size)
        assertEquals(listOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Menge.id), argumente.map { it.art })
    }

    @Test
    fun `nullstellige Methode erhaelt auch bei Tupelprojektion keinen Argumentanschluss`() {
        val methode = Methode(
            name = "c",
            parameter = emptyList(),
            ausgaben = mapOf("wert" to RationaleZahl.von(7)),
            zielMengen = mapOf("wert" to GanzeZahlen),
        )
        val (karte, aufruf) = verbundeneKarte(methode, METHODEN_ARGUMENTPROJEKTION_TUPEL)

        val synchronisiert = synchronisiereMethodenAufrufe(
            karte,
            auswertungFür(aufruf, methode),
            prüfung,
        )

        assertTrue(synchronisiert.knoten.single { it.id == aufruf.id }.argumente().isEmpty())
    }

    @Test
    fun `Projektionsschluessel ist pro Methodenausgang stabil`() {
        val quelle = methodenQuelle(METHODEN_ARGUMENTPROJEKTION_TUPEL)
        assertEquals(
            "methodenAusgang.methode.argumentprojektion",
            methodenAusgangArgumentprojektionSchlüssel("methode"),
        )
        assertEquals(METHODEN_ARGUMENTPROJEKTION_TUPEL, quelle.methodenAusgangArgumentprojektion("methode"))
        assertEquals(METHODEN_ARGUMENTPROJEKTION_SEPARIERT, quelle.methodenAusgangArgumentprojektion("andererAusgang"))
    }

    private fun verbundeneKarte(methode: Methode, projektion: String): Pair<KartenDaten, KnotenDaten> {
        val quelle = methodenQuelle(projektion)
        val aufruf = FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero).let { erzeugt ->
            erzeugt.copy(
                parameter = erzeugt.parameter +
                    (METHODEN_AUFRUF_ARGUMENTPROJEKTION to projektion),
            )
        }
        val methodenEingang = aufruf.anschlüsse.single {
            it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
        }
        val methodenAusgang = quelle.anschlüsse.single()
        val verbindung = VerbindungDaten(
            von = AnschlussVerweis(quelle.id, methodenAusgang.id),
            zu = AnschlussVerweis(aufruf.id, methodenEingang.id),
        )
        return KartenDaten(
            name = "Methodenprojektion",
            knoten = listOf(quelle, aufruf),
            verbindungen = listOf(verbindung),
        ) to aufruf
    }

    private fun methodenQuelle(projektion: String) = KnotenDaten(
        id = KnotenId("methoden-quelle"),
        art = "test.methodenQuelle",
        name = "Methodenquelle",
        parameter = mapOf(methodenAusgangArgumentprojektionSchlüssel("methode") to projektion),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("methoden-quelle:methode"),
                name = "methode",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Methode.id,
            ),
        ),
    )

    private fun auswertungFür(aufruf: KnotenDaten, methode: Methode) = KartenAuswertungsErgebnis(
        knoten = mapOf(
            aufruf.id to KnotenAuswertungsErgebnis(
                ausgaben = emptyMap(),
                eingänge = mapOf("methode" to BedingterWert(methode)),
            ),
        ),
        basisFehler = emptyList(),
    )

    private fun KnotenDaten.argumente() = anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }
        .sortedBy { it.reihenfolge }
}

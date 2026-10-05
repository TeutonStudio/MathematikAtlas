package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.TupelOperationKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TupelAuflösenSynchronisierungTest {
    private val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))

    @Test
    fun `Ausgänge wachsen schrumpfen und behalten IDs positionsstabil`() {
        val auflöser = TupelOperationKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        val basis = KartenDaten(name = "Test", knoten = listOf(auflöser))

        val mitZwei = synchronisiereTupelAuflöser(
            basis,
            auswertung(auflöser, Tupel(listOf(RationaleZahl.von(1), WahrheitsKonstante(true)))),
            prüfung,
        )
        val ausgängeZwei = mitZwei.knoten.single().ausgänge()
        assertEquals(2, ausgängeZwei.size)
        assertEquals(MathematikAnschlussArten.Zahl.id, ausgängeZwei[0].art)
        assertEquals(MathematikAnschlussArten.Aussage.id, ausgängeZwei[1].art)

        val mitDrei = synchronisiereTupelAuflöser(
            mitZwei,
            auswertung(
                mitZwei.knoten.single(),
                Tupel(listOf(RationaleZahl.von(4), WahrheitsKonstante(false), Tupel(emptyList()))),
            ),
            prüfung,
        )
        val ausgängeDrei = mitDrei.knoten.single().ausgänge()
        assertEquals(3, ausgängeDrei.size)
        assertEquals(ausgängeZwei[0].id, ausgängeDrei[0].id)
        assertEquals(ausgängeZwei[1].id, ausgängeDrei[1].id)
        assertEquals(MathematikAnschlussArten.Tupel.id, ausgängeDrei[2].art)

        val mitEinem = synchronisiereTupelAuflöser(
            mitDrei,
            auswertung(mitDrei.knoten.single(), Tupel(listOf(RationaleZahl.von(9)))),
            prüfung,
        )
        val ausgängeEins = mitEinem.knoten.single().ausgänge()
        assertEquals(1, ausgängeEins.size)
        assertEquals(ausgängeZwei[0].id, ausgängeEins.single().id)
    }

    @Test
    fun `Typwechsel entfernt eine inkompatibel gewordene Verbindung`() {
        val auflöser = TupelOperationKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        val basis = KartenDaten(name = "Test", knoten = listOf(auflöser))
        val synchronisiert = synchronisiereTupelAuflöser(
            basis,
            auswertung(auflöser, Tupel(listOf(RationaleZahl.von(1)))),
            prüfung,
        )
        val auflöserNeu = synchronisiert.knoten.single()
        val ausgang = auflöserNeu.ausgänge().single()
        val ziel = KnotenDaten(
            art = "test.ziel",
            name = "Ziel",
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "zahl",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Zahl.id,
                ),
            ),
        )
        val verbunden = synchronisiert.copy(
            knoten = listOf(auflöserNeu, ziel),
            verbindungen = listOf(
                VerbindungDaten(
                    von = AnschlussVerweis(auflöserNeu.id, ausgang.id),
                    zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.single().id),
                ),
            ),
        )

        val nachTypwechsel = synchronisiereTupelAuflöser(
            verbunden,
            auswertung(auflöserNeu, Tupel(listOf(WahrheitsKonstante(true)))),
            prüfung,
        )

        assertEquals(MathematikAnschlussArten.Aussage.id, nachTypwechsel.knoten.first().ausgänge().single().art)
        assertTrue(nachTypwechsel.verbindungen.isEmpty())
    }

    @Test
    fun `Temporärer Auswertungsfehler behält letzten erfolgreichen Handlevertrag`() {
        val auflöser = TupelOperationKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        val basis = KartenDaten(name = "Test", knoten = listOf(auflöser))
        val synchronisiert = synchronisiereTupelAuflöser(
            basis,
            auswertung(auflöser, Tupel(listOf(RationaleZahl.von(1), RationaleZahl.von(2)))),
            prüfung,
        )
        val vorher = synchronisiert.knoten.single().ausgänge().map { it.id to it.art }
        val fehler = KartenAuswertungsErgebnis(
            mapOf(auflöser.id to KnotenAuswertungsErgebnis(emptyMap(), fehler = "temporär")),
            emptyList(),
        )

        val danach = synchronisiereTupelAuflöser(synchronisiert, fehler, prüfung)

        assertEquals(vorher, danach.knoten.single().ausgänge().map { it.id to it.art })
    }

    @Test
    fun `Matrixzerleger erzeugt stabile Zeilen IDs und orientierte Arten`() {
        val basis = StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val zerleger = konfiguriereStrukturRechner(
            basis,
            StrukturRechnerKnotenFamilie.MATRIX,
            MatrixRechnerOperator.ZERLEGEN.stabileId,
        )
        val matrix = Matrix(List(2) { List(3) { RationaleZahl.Eins } })
        val karte = KartenDaten(name = "Matrix", knoten = listOf(zerleger))
        val einmal = synchronisiereStrukturZerleger(
            karte,
            strukturAuswertung(zerleger, "matrix", matrix),
            prüfung,
        )
        val zweimal = synchronisiereStrukturZerleger(
            einmal,
            strukturAuswertung(einmal.knoten.single(), "matrix", matrix),
            prüfung,
        )
        assertEquals(listOf("zeile1", "zeile2"), einmal.knoten.single().ausgänge().map { it.name })
        assertTrue(einmal.knoten.single().ausgänge().all { it.art == MathematikAnschlussArten.ZeilenVektor.id })
        assertEquals(
            einmal.knoten.single().ausgänge().map { it.id },
            zweimal.knoten.single().ausgänge().map { it.id },
        )
    }

    @Test
    fun `symbolischer Vektorvertrag synchronisiert genau den Methodenausgang`() {
        val zerleger = vektorRechnerVorlage(VektorRechnerOperator.ZERLEGEN).erzeuge(GraphPunkt.Zero)
        val quelle = TypisiertesElement(
            "v",
            MathematikAnschlussArten.SpaltenVektor.id.wert,
            strukturForm = listOf(Variable("n")),
        )
        val ergebnis = synchronisiereStrukturZerleger(
            KartenDaten(name = "Vektor", knoten = listOf(zerleger)),
            strukturAuswertung(zerleger, "struktur", quelle),
            prüfung,
        )
        assertEquals(listOf("methode"), ergebnis.knoten.single().ausgänge().map { it.name })
        assertEquals(MathematikAnschlussArten.Methode.id, ergebnis.knoten.single().ausgänge().single().art)
        val erneut = synchronisiereStrukturZerleger(
            ergebnis,
            strukturAuswertung(ergebnis.knoten.single(), "struktur", quelle),
            prüfung,
        )
        assertEquals(ergebnis.knoten.single().ausgänge().single().id, erneut.knoten.single().ausgänge().single().id)
    }

    private fun auswertung(knoten: KnotenDaten, tupel: Tupel) = KartenAuswertungsErgebnis(
        mapOf(
            knoten.id to KnotenAuswertungsErgebnis(
                ausgaben = emptyMap(),
                eingänge = mapOf("tupel" to BedingterWert(tupel)),
            ),
        ),
        emptyList(),
    )

    private fun strukturAuswertung(
        knoten: KnotenDaten,
        eingang: String,
        objekt: MathematischesObjekt,
    ) = KartenAuswertungsErgebnis(
        mapOf(
            knoten.id to KnotenAuswertungsErgebnis(
                ausgaben = emptyMap(),
                eingänge = mapOf(eingang to BedingterWert(objekt)),
            ),
        ),
        emptyList(),
    )

    private fun KnotenDaten.ausgänge() = anschlüsse
        .filter { it.richtung == AnschlussRichtung.Ausgang }
        .sortedBy { it.reihenfolge }
}

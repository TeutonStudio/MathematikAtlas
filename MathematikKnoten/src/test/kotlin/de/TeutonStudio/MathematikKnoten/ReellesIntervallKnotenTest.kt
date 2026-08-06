package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ReellesIntervallKnotenTest {
    private val register = StandardMathematikAuswerter.erzeugeRegister()
    private fun z(wert: Long) = RationaleZahl.von(wert)

    @Test
    fun `Vorlage registriert Zahl Aussage Zahl Aussage und einen Mengenausgang`() {
        val knoten = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        val eingänge = knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }

        assertEquals("mathematik.reellesIntervall", knoten.art)
        assertEquals(listOf("links", "linksOffen", "rechts", "rechtsOffen"), eingänge.map { it.name })
        assertEquals(
            listOf(
                MathematikAnschlussArten.Zahl.id,
                MathematikAnschlussArten.Aussage.id,
                MathematikAnschlussArten.Zahl.id,
                MathematikAnschlussArten.Aussage.id,
            ),
            eingänge.map { it.art },
        )
        assertEquals(MathematikAnschlussArten.Menge.id, knoten.anschlüsse.single { it.name == "menge" }.art)
        assertFalse(knoten.anschlüsse.any { it.kannSichErweitern })
        assertNotNull(register.finde(knoten.art))
    }

    @Test
    fun `Graphprüfung akzeptiert die vier typisierten Eingänge`() {
        val intervall = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        fun quelle(name: String, art: AnschlussArtId) = KnotenDaten(
            art = "test.quelle",
            name = name,
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = art,
                ),
            ),
        )
        fun ref(knoten: KnotenDaten, name: String) = AnschlussVerweis(
            knoten.id,
            knoten.anschlüsse.single { it.name == name }.id,
        )

        val zahl = quelle("Zahl", MathematikAnschlussArten.Zahl.id)
        val aussage = quelle("Aussage", MathematikAnschlussArten.Aussage.id)
        val karte = KartenDaten(name = "Test", knoten = listOf(zahl, aussage, intervall))
        val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))

        listOf("links", "rechts").forEach { name ->
            assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(karte, ref(zahl, "wert"), ref(intervall, name)))
        }
        listOf("linksOffen", "rechtsOffen").forEach { name ->
            assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(karte, ref(aussage, "wert"), ref(intervall, name)))
        }
    }

    @Test
    fun `fehlende Offenheits Aussagen bedeuten geschlossen`() {
        val ergebnis = auswerten(mapOf("links" to BedingterWert(z(1)), "rechts" to BedingterWert(z(3))))

        assertEquals(
            "{}^{1\\leq}\\mathbb{R}^{\\leq3}",
            assertIs<ReellesIntervall>(ergebnis).zuLatex(),
        )
    }

    @Test
    fun `wahre Offenheits Aussagen öffnen die jeweilige Grenze`() {
        val beideOffen = auswerten(
            mapOf(
                "links" to BedingterWert(z(1)),
                "linksOffen" to BedingterWert(WahrheitsKonstante(true)),
                "rechts" to BedingterWert(z(3)),
                "rechtsOffen" to BedingterWert(WahrheitsKonstante(true)),
            ),
        )
        val nurRechtsOffen = auswerten(
            mapOf(
                "links" to BedingterWert(z(1)),
                "linksOffen" to BedingterWert(WahrheitsKonstante(false)),
                "rechts" to BedingterWert(z(3)),
                "rechtsOffen" to BedingterWert(WahrheitsKonstante(true)),
            ),
        )

        assertEquals("{}^{1<}\\mathbb{R}^{<3}", assertIs<ReellesIntervall>(beideOffen).zuLatex())
        assertEquals("{}^{1\\leq}\\mathbb{R}^{<3}", assertIs<ReellesIntervall>(nurRechtsOffen).zuLatex())
    }

    @Test
    fun `unentscheidbare Offenheit erzeugt einen Auswertungsfehler`() {
        val fehler = assertFailsWith<IllegalStateException> {
            auswerten(
                mapOf(
                    "links" to BedingterWert(z(1)),
                    "linksOffen" to BedingterWert(UnentscheidbareAussage("L", "Testsystem")),
                    "rechts" to BedingterWert(z(3)),
                ),
            )
        }

        assertEquals(
            "Die Aussage am Eingang „links offen?“ konnte nicht entschieden werden. Unentscheidbar in Testsystem",
            fehler.message,
        )
    }

    @Test
    fun `alte Anschlussnamen werden ohne Migration abgelehnt`() {
        assertFailsWith<IllegalStateException> {
            auswerten(
                mapOf(
                    "untereGrenze" to BedingterWert(z(1)),
                    "obereGrenze" to BedingterWert(z(3)),
                ),
            )
        }
    }

    @Test
    fun `nicht nachweisbar reelle Grenzen bleiben unzulässig`() {
        assertFailsWith<IllegalArgumentException> {
            auswerten(
                mapOf(
                    "links" to BedingterWert(Variable("x")),
                    "rechts" to BedingterWert(z(1)),
                ),
            )
        }
    }

    private fun auswerten(eingänge: Map<String, BedingterWert>): MathematischesObjekt {
        val knoten = MathematikKnotenVorlagen.ReellesIntervall.erzeuge(GraphPunkt.Zero)
        return register.finde(knoten.art)!!
            .auswerten(KnotenAuswertungsKontext(knoten, eingänge, RechenKontext()))
            .ausgaben
            .getValue("menge")
            .objekt
    }
}

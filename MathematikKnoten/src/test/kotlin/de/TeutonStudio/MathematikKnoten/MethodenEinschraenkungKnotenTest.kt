package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MethodenEinschraenkungKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private val x = Variable("x")

    private fun methode(bereich: MengenAusdruck = ReelleZahlen) = Methode(
        name = "f",
        parameter = listOf(x),
        vorschrift = x,
        zielMenge = ReelleZahlen,
        werteVorräte = mapOf("x" to bereich),
    )

    private fun kontext(
        knoten: KnotenDaten,
        eingaben: Map<String, BedingterWert>,
    ) = KnotenAuswertungsKontext(
        knoten = knoten,
        eingänge = eingaben,
        rechenKontext = RechenKontext(),
    )

    @Test
    fun `Vorlagenkatalog enthaelt sichtbaren Restriktionsknoten`() {
        val vorlage = alleMathematikKnotenVorlagen().single {
            it.art == METHODEN_EINSCHRAENKUNG_KNOTEN_ART
        }

        assertEquals(setOf("methode", "menge"), vorlage.anschlüsse.filter {
            it.richtung == AnschlussRichtung.Eingang
        }.map { it.name }.toSet())
        assertEquals("methode", vorlage.anschlüsse.single {
            it.richtung == AnschlussRichtung.Ausgang
        }.name)
    }

    @Test
    fun `bewiesene endliche Teilmenge aendert nur den Wertevorrat`() {
        val bereich = EndlicheMenge(setOf(RationaleZahl.Eins, RationaleZahl.von(2)))
        val teilmenge = EndlicheMenge(setOf(RationaleZahl.Eins))
        val knoten = MethodenEinschraenkungKnotenVorlagen.Einschraenkung.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(METHODEN_EINSCHRAENKUNG_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode(bereich)),
                    "menge" to BedingterWert(teilmenge),
                ),
            ),
        )
        val eingeschraenkt = assertIs<Methode>(ergebnis.ausgaben.getValue("methode").objekt)

        assertEquals(teilmenge, eingeschraenkt.werteVorräte.getValue("x"))
        assertEquals(ReelleZahlen, eingeschraenkt.zielMenge)
        assertTrue(ergebnis.ausgaben.getValue("methode").annahmen.isEmpty())
    }

    @Test
    fun `offene Teilmengenbeziehung bleibt als Annahme am Ausgang`() {
        val intervall = ReellesIntervall(
            RationaleZahl.Null,
            false,
            RationaleZahl.Eins,
            false,
        )
        val knoten = MethodenEinschraenkungKnotenVorlagen.Einschraenkung.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(METHODEN_EINSCHRAENKUNG_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode()),
                    "menge" to BedingterWert(intervall),
                ),
            ),
        )

        assertTrue(ergebnis.ausgaben.getValue("methode").annahmen.any { it is TeilmengenBeziehung })
        assertTrue(ergebnis.warnungen.any { it.contains("offene Voraussetzung") })
    }

    @Test
    fun `nachweislich falsche Teilmenge erzeugt Fehler`() {
        val bereich = EndlicheMenge(setOf(RationaleZahl.Eins))
        val fremd = EndlicheMenge(setOf(RationaleZahl.von(2)))
        val knoten = MethodenEinschraenkungKnotenVorlagen.Einschraenkung.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(METHODEN_EINSCHRAENKUNG_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode(bereich)),
                    "menge" to BedingterWert(fremd),
                ),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("keine Teilmenge"))
    }

    @Test
    fun `historische Restriktionsarten migrieren idempotent`() {
        val alt = KnotenDaten(
            art = "mathematik.restriktion",
            name = "Alt",
            position = GraphPunkt.Zero,
            anschlüsse = MethodenEinschraenkungKnotenVorlagen.Einschraenkung.anschlüsse,
        )
        val karte = KartenDaten(name = "Alt", knoten = listOf(alt))

        val migriert = karte.migriereMethodenEinschraenkungKnoten()

        assertEquals(METHODEN_EINSCHRAENKUNG_KNOTEN_ART, migriert.knoten.single().art)
        assertEquals(alt.anschlüsse.map { it.id }, migriert.knoten.single().anschlüsse.map { it.id })
        assertEquals(migriert, migriert.migriereMethodenEinschraenkungKnoten())
    }
}

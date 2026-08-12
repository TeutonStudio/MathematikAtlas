package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RestriktionsKnotenTest {
    private fun menge(vararg werte: Long): MengenAusdruck =
        if (werte.isEmpty()) LeereMenge else EndlicheMenge(werte.map { RationaleZahl.von(it) }.toSet())

    private fun methode(name: String, domain: MengenAusdruck, wert: Long, ziel: MengenAusdruck): MathematischeMethode {
        val x = Variable("x")
        return Methode(
            name = name,
            parameter = listOf(x),
            vorschrift = RationaleZahl.von(wert),
            zielMenge = ziel,
            werteVorräte = mapOf(x.name to domain),
        )
    }

    @Test
    fun `Katalog besitzt getrennte Restriktions- und Bereichsanpassungsvorlage`() {
        val restriktion = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)
        val anpassung = RestriktionsKnotenVorlagen.Bereichsanpassung.erzeuge(GraphPunkt.Zero)

        assertEquals(RESTRIKTIONS_KNOTEN_ART, restriktion.art)
        assertEquals(RESTRIKTIONS_KNOTEN_ART, anpassung.art)
        assertEquals(METHODEN_BEREICHS_OPERATOR_RESTRIKTION, restriktion.methodenBereichsOperator())
        assertEquals(METHODEN_BEREICHS_OPERATOR_ANPASSUNG, anpassung.methodenBereichsOperator())
        assertEquals("Methodenrestriktion", RestriktionsKnotenVorlagen.Restriktion.name)
        assertEquals("Methoden-Bereichsanpassung", RestriktionsKnotenVorlagen.Bereichsanpassung.name)
        assertEquals(
            listOf("methode", "menge"),
            restriktion.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.map { it.name },
        )
        assertTrue(restriktion.anschlüsse.none { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) })
    }

    @Test
    fun `Restriktionsauswerter liefert ausschliesslich f restr M und behaelt Zielmenge`() {
        val ziel = menge(0, 1, 2)
        val basis = methode("f", menge(0, 1, 2), 1, ziel)
        val m = menge(0, 2)
        val knoten = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)
        val auswerter = auswerter()

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(basis), "menge" to BedingterWert(m)),
                rechenKontext = RechenKontext(),
            ),
        )

        val restriktion = ergebnis.ausgaben.getValue("methode").objekt as Methode
        assertEquals(m, restriktion.methodenSignatur().werteVorrat)
        assertEquals(ziel, restriktion.methodenSignatur().zielMenge)
        assertEquals("f\\vert_{${m.zuLatex()}}", ergebnis.ausgaben.getValue("methode").latexDarstellung)
    }

    @Test
    fun `Restriktion ausserhalb der Definitionsmenge wird abgelehnt statt erweitert`() {
        val basis = methode("f", menge(0), 0, menge(0))
        val knoten = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)

        val ergebnis = auswerter().auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(basis), "menge" to BedingterWert(menge(0, 1))),
                rechenKontext = RechenKontext(),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler?.contains("Ungültige Restriktion") == true)
    }

    @Test
    fun `unvollstaendige reine Restriktion erzeugt keinen Ergaenzungseingang`() {
        val basis = methode("f", menge(0), 0, menge(0))
        val knoten = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)
        val synchron = synchronisiereRestriktionsAnschlüsse(
            KartenDaten(name = "Test", knoten = listOf(knoten)),
            auswertung(knoten, mapOf("methode" to BedingterWert(basis), "menge" to BedingterWert(menge(0, 1)))),
        ).knoten.single()

        assertTrue(synchron.anschlüsse.none { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) })
    }

    @Test
    fun `unvollstaendige Bereichsanpassung erzeugt genau einen freien Ergaenzungseingang`() {
        val ziel = menge(0, 1, 2)
        val basis = methode("f", menge(0), 0, ziel)
        val m = menge(0, 1, 2)
        val knoten = RestriktionsKnotenVorlagen.Bereichsanpassung.erzeuge(GraphPunkt.Zero)

        val synchron = synchronisiereRestriktionsAnschlüsse(
            KartenDaten(name = "Test", knoten = listOf(knoten)),
            auswertung(knoten, mapOf("methode" to BedingterWert(basis), "menge" to BedingterWert(m))),
        ).knoten.single()

        val ergänzungen = synchron.anschlüsse.filter { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) }
        assertEquals(1, ergänzungen.size)
        assertTrue(ergänzungen.single().dynamischErzeugt)
        assertEquals(2, ergänzungen.single().reihenfolge)
    }

    @Test
    fun `Bereichsanpassung rendert niemals als Restriktion`() {
        val ziel = menge(0, 1)
        val basis = methode("f", menge(0), 0, ziel)
        val g = methode("g", menge(1), 1, ziel)
        val knoten = RestriktionsKnotenVorlagen.Bereichsanpassung.erzeuge(GraphPunkt.Zero).copy(
            anschlüsse = RestriktionsKnotenVorlagen.Bereichsanpassung.erzeuge(GraphPunkt.Zero).anschlüsse +
                ergänzungsAnschluss("ergänzung.0", 2),
        )

        val ergebnis = auswerter().auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "methode" to BedingterWert(basis),
                    "menge" to BedingterWert(menge(0, 1)),
                    "ergänzung.0" to BedingterWert(g),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val latex = assertNotNull(ergebnis.ausgaben["methode"]?.latexDarstellung)
        assertTrue(latex.contains("Bereichsanpassung"))
        assertTrue(!latex.contains("\\vert_"))
    }

    @Test
    fun `Reihenfolge der dynamischen Anschluesse bestimmt erste passende Methode`() {
        val ziel = menge(0, 1, 2)
        val basis = methode("f", menge(0), 0, ziel)
        val g1 = methode("g_1", menge(1), 1, ziel)
        val g2 = methode("g_2", menge(1), 2, ziel)
        val grund = RestriktionsKnotenVorlagen.Bereichsanpassung.erzeuge(GraphPunkt.Zero)
        val e1 = ergänzungsAnschluss("ergänzung.0", 3)
        val e2 = ergänzungsAnschluss("ergänzung.1", 2)
        val knoten = grund.copy(anschlüsse = grund.anschlüsse + e1 + e2)

        val ergebnis = auswerter().auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "methode" to BedingterWert(basis),
                    "menge" to BedingterWert(menge(0, 1)),
                    e1.name to BedingterWert(g1),
                    e2.name to BedingterWert(g2),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val methode = ergebnis.ausgaben.getValue("methode").objekt as Methode
        assertEquals(RationaleZahl.von(2), methode.wendeAn(listOf(RationaleZahl.von(1))))
    }

    @Test
    fun `freier Ergaenzungseingang behaelt ID bei reiner Neuauswertung`() {
        val ziel = menge(0, 1, 2)
        val basis = methode("f", menge(0), 0, ziel)
        val m = menge(0, 1, 2)
        val grund = RestriktionsKnotenVorlagen.Bereichsanpassung.erzeuge(GraphPunkt.Zero)
        val eingänge = mapOf("methode" to BedingterWert(basis), "menge" to BedingterWert(m))

        val einmal = synchronisiereRestriktionsAnschlüsse(
            KartenDaten(name = "Test", knoten = listOf(grund)),
            auswertung(grund, eingänge),
        ).knoten.single()
        val freieId = einmal.anschlüsse.single { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) }.id

        val zweimal = synchronisiereRestriktionsAnschlüsse(
            KartenDaten(name = "Test", knoten = listOf(einmal)),
            auswertung(einmal, eingänge),
        ).knoten.single()

        assertEquals(
            freieId,
            zweimal.anschlüsse.single { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) }.id,
        )
        assertEquals(einmal, zweimal)
    }

    private fun auswerter(): KnotenAuswerter {
        val register = MathematikAuswerterRegister().apply { registriereRestriktionsKnoten() }
        return assertNotNull(register.finde(RESTRIKTIONS_KNOTEN_ART))
    }

    private fun ergänzungsAnschluss(name: String, reihenfolge: Int) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Methode.id,
        reihenfolge = reihenfolge,
        dynamischErzeugt = true,
    )

    private fun auswertung(
        knoten: KnotenDaten,
        eingänge: Map<String, BedingterWert>,
    ) = KartenAuswertungsErgebnis(
        knoten = mapOf(
            knoten.id to KnotenAuswertungsErgebnis(
                ausgaben = emptyMap(),
                eingänge = eingänge,
            ),
        ),
        basisFehler = emptyList(),
    )
}

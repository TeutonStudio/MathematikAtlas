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

    private fun methode(name: String, domain: MengenAusdruck, wert: Long, ziel: MengenAusdruck): Methode {
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
    fun `Vorlage besitzt Methode Menge und Methodenausgang ohne vorauseilende Ergaenzung`() {
        val knoten = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)
        assertEquals(RESTRIKTIONS_KNOTEN_ART, knoten.art)
        assertEquals("Abbildungen", RestriktionsKnotenVorlagen.Restriktion.kategorie)
        assertEquals(
            listOf("methode", "menge"),
            knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.map { it.name },
        )
        assertEquals(
            listOf("methode"),
            knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }.map { it.name },
        )
    }

    @Test
    fun `Auswerter liefert f restr M und behaelt Zielmenge`() {
        val ziel = menge(0, 1, 2)
        val basis = methode("f", menge(0, 1, 2), 1, ziel)
        val m = menge(0, 2)
        val knoten = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)
        val register = MathematikAuswerterRegister().apply { registriereRestriktionsKnoten() }
        val auswerter = assertNotNull(register.finde(RESTRIKTIONS_KNOTEN_ART))

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(basis), "menge" to BedingterWert(m)),
                rechenKontext = RechenKontext(),
            ),
        )

        val restriktion = ergebnis.ausgaben.getValue("methode").objekt as Methode
        assertEquals(m, restriktion.effektiverWerteVorrat)
        assertEquals(ziel, restriktion.zielMenge)
        assertEquals("f\\vert_{${m.zuLatex()}}", ergebnis.ausgaben.getValue("methode").latexDarstellung)
    }

    @Test
    fun `unvollstaendige Abdeckung erzeugt genau einen freien Ergaenzungseingang`() {
        val ziel = menge(0, 1, 2)
        val basis = methode("f", menge(0), 0, ziel)
        val m = menge(0, 1, 2)
        val knoten = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)
        val auswertung = auswertung(knoten, mapOf("methode" to BedingterWert(basis), "menge" to BedingterWert(m)))

        val synchron = synchronisiereRestriktionsAnschlüsse(
            KartenDaten(name = "Test", knoten = listOf(knoten)),
            auswertung,
        ).knoten.single()

        val ergänzungen = synchron.anschlüsse.filter { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) }
        assertEquals(1, ergänzungen.size)
        assertTrue(ergänzungen.single().dynamischErzeugt)
        assertEquals(2, ergänzungen.single().reihenfolge)
    }

    @Test
    fun `vollstaendige Abdeckung erzeugt keinen Ergaenzungseingang`() {
        val ziel = menge(0, 1)
        val basis = methode("f", menge(0, 1), 0, ziel)
        val m = menge(0, 1)
        val knoten = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)

        val synchron = synchronisiereRestriktionsAnschlüsse(
            KartenDaten(name = "Test", knoten = listOf(knoten)),
            auswertung(knoten, mapOf("methode" to BedingterWert(basis), "menge" to BedingterWert(m))),
        ).knoten.single()

        assertTrue(synchron.anschlüsse.none { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) })
    }

    @Test
    fun `verbundene Ergaenzung behaelt ID und bei weiterem Rest entsteht genau ein neuer Eingang`() {
        val ziel = menge(0, 1, 2)
        val basis = methode("f", menge(0), 0, ziel)
        val g1 = methode("g_1", menge(1), 1, ziel)
        val m = menge(0, 1, 2)
        val grund = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)
        val ergänzung = AnschlussDaten(
            name = "${RESTRIKTIONS_ERGÄNZUNG_PREFIX}0",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Methode.id,
            reihenfolge = 2,
            dynamischErzeugt = true,
        )
        val knoten = grund.copy(anschlüsse = grund.anschlüsse + ergänzung)
        val quelle = KnotenDaten(
            art = "test.methode",
            name = "Quelle",
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "methode",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Methode.id,
                ),
            ),
        )
        val karte = KartenDaten(
            name = "Test",
            knoten = listOf(quelle, knoten),
            verbindungen = listOf(
                VerbindungDaten(
                    von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                    zu = AnschlussVerweis(knoten.id, ergänzung.id),
                ),
            ),
        )

        val synchron = synchronisiereRestriktionsAnschlüsse(
            karte,
            auswertung(
                knoten,
                mapOf(
                    "methode" to BedingterWert(basis),
                    "menge" to BedingterWert(m),
                    ergänzung.name to BedingterWert(g1),
                ),
            ),
        ).knoten.first { it.id == knoten.id }

        val ergänzungen = synchron.anschlüsse.filter { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) }
        assertEquals(2, ergänzungen.size)
        assertEquals(ergänzung.id, ergänzungen.first().id)
        assertTrue(ergänzungen[1].id != ergänzung.id)
    }

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

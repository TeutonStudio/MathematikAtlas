package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VektorRechnerKnotenKonfigurationTest {
    @Test
    fun `Vektorfeldintegral hat nur Vektorfeld und Menge als sichtbare Eingaenge`() {
        val anschluesse = vektorRechnerAnschluesse(VektorRechnerOperator.VEKTORFELD_INTEGRIEREN)
        val eingaenge = anschluesse.filter { it.richtung == AnschlussRichtung.Eingang }

        assertEquals(listOf("vektorfeld", "menge"), eingaenge.sortedBy { it.reihenfolge }.map { it.name })
        assertEquals(1, anschluesse.count { it.richtung == AnschlussRichtung.Ausgang })
    }

    @Test
    fun `Zerlegen bietet vor bekannter Struktur die endliche Indexmethode an`() {
        val anschluesse = vektorRechnerAnschluesse(VektorRechnerOperator.ZERLEGEN)

        assertEquals(2, anschluesse.size)
        assertEquals("struktur", anschluesse.single { it.richtung == AnschlussRichtung.Eingang }.name)
        assertEquals("methode", anschluesse.single { it.richtung == AnschlussRichtung.Ausgang }.name)
        assertEquals(MathematikAnschlussArten.Methode.id, anschluesse.single { it.richtung == AnschlussRichtung.Ausgang }.art)
    }

    @Test
    fun `Legacy Tupelaufloeser wird mit Anschluss IDs in Zerlegen migriert`() {
        val eingangId = AnschlussId("legacy-in")
        val ausgangId = AnschlussId("legacy-out")
        val alt = KnotenDaten(
            art = TUPEL_AUFLÖSEN_ART,
            name = "Tupel auflösen",
            anschlüsse = listOf(
                AnschlussDaten(
                    id = eingangId,
                    name = "tupel",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Tupel.id,
                ),
                AnschlussDaten(
                    id = ausgangId,
                    name = "element-1",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Zahl.id,
                ),
            ),
        )

        val migriert = KartenDaten(name = "Legacy", knoten = listOf(alt))
            .migriereLegacyVektorStrukturKnoten()
            .knoten.single()

        assertEquals(VektorRechner.KNOTEN_ART, migriert.art)
        assertEquals(VektorRechnerOperator.ZERLEGEN.stabileId, migriert.parameter[VEKTOR_RECHNER_OPERATOR])
        assertTrue(migriert.anschlüsse.any { it.id == eingangId && it.name == "struktur" })
        assertTrue(migriert.anschlüsse.any { it.id == ausgangId && it.name == "element1" })
    }
    @Test fun `Zerlegen bleibt bei wiederholter Konfiguration und Migration idempotent`() {
        val initial = vektorRechnerVorlage(VektorRechnerOperator.ZERLEGEN).erzeuge(GraphPunkt.Zero)
        val einmal = konfiguriereVektorRechner(initial, VektorRechnerOperator.ZERLEGEN)
        var aktuell = einmal
        repeat(4) { aktuell = konfiguriereVektorRechner(aktuell, VektorRechnerOperator.ZERLEGEN) }
        assertEquals(einmal.anschlüsse, aktuell.anschlüsse)
        assertEquals(1, aktuell.anschlüsse.count { it.name == "methode" })
        val karte = KartenDaten(name = "Zerlegen", knoten = listOf(aktuell))
        assertEquals(MathematikKartenMigrationen.nachLaden(karte), MathematikKartenMigrationen.nachLaden(MathematikKartenMigrationen.nachLaden(karte)))
    }

    @Test fun `Editor bietet nach zwei belegten Vektoroperanden einen fachlich benannten Reserveeingang`() {
        listOf(VektorRechnerOperator.ADDITION, VektorRechnerOperator.ZUSAMMENFUEHREN).forEach { op ->
            val rechner = konfiguriereVektorRechner(vektorRechnerVorlage(op).erzeuge(GraphPunkt.Zero), op)
            val quellen = List(3) { KnotenDaten(art = "test.quelle", name = "Spalte", anschlüsse = listOf(AnschlussDaten(
                name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.SpaltenVektor.id))) }
            val eingänge = rechner.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
            val karte = KartenDaten(name = "Variadik", knoten = listOf(rechner) + quellen, verbindungen = quellen.take(2).zip(eingänge).map { (q, e) ->
                VerbindungDaten(von = AnschlussVerweis(q.id, q.anschlüsse.single().id), zu = AnschlussVerweis(rechner.id, e.id))
            })
            val arten = AnschlussArtRegister(MathematikAnschlussArten.alle)
            val editor = KartenEditorZustand(karte, GraphPrüfung(arten, erzeugeMathematikKartenTypSystem(arten)))
            editor.beginneVerbindung(AnschlussVerweis(quellen[2].id, quellen[2].anschlüsse.single().id))
            val erweitert = editor.karte.knoten.first { it.id == rechner.id }
            val reserve = erweitert.anschlüsse.single { it.dynamischErzeugt }
            assertEquals(if (op == VektorRechnerOperator.ADDITION) "vektor.3" else "element.3", reserve.name)
            assertEquals(erweitert.anschlüsse.map { it.id }.toSet(), konfiguriereVektorRechner(erweitert, op).anschlüsse.map { it.id }.toSet())
        }
    }

    @Test fun `unbekannte Rechneroperatoren ueberstehen die gesamte Lademigration`() {
        listOf(VektorRechner.KNOTEN_ART to VEKTOR_RECHNER_OPERATOR,
            de.TeutonStudio.MathematikRechenSystem.kern.TensorRechner.KNOTEN_ART to TENSOR_OPERATION_ID).forEach { (art, key) ->
            val knoten = KnotenDaten(art = art, name = "Zukunft", parameter = mapOf(key to "future.operation"), anschlüsse = listOf(
                AnschlussDaten(name = "historisch", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id)))
            val geladen = MathematikKartenMigrationen.nachLaden(KartenDaten(name = "Unbekannt", knoten = listOf(knoten))).knoten.single()
            assertEquals("future.operation", geladen.parameter[key])
            assertEquals(knoten.anschlüsse.map { it.id }, geladen.anschlüsse.map { it.id })
        }
    }

    @Test fun `unäre Rolle bewahrt ersten Operanden in beide Richtungen und historische Namen`() {
        val basis = vektorRechnerVorlage(VektorRechnerOperator.ADDITION).erzeuge(GraphPunkt.Zero)
        for (namen in listOf(listOf("vektor.1", "vektor.2"), listOf("a", "b"), listOf("links", "rechts"))) {
            val alt = basis.copy(anschlüsse = basis.anschlüsse.map { p ->
                when (p.name) { "vektor.1" -> p.copy(name = namen[0]); "vektor.2" -> p.copy(name = namen[1]); else -> p }
            })
            val erster = alt.anschlüsse.first { it.name == namen[0] }
            val norm = konfiguriereVektorRechner(alt, VektorRechnerOperator.NORM)
            assertEquals(erster.id, norm.anschlüsse.single { it.name == "vektor" }.id)
            val zurück = konfiguriereVektorRechner(norm, VektorRechnerOperator.ADDITION)
            assertEquals(erster.id, zurück.anschlüsse.single { it.name == "vektor.1" }.id)
            assertTrue(zurück.anschlüsse.single { it.name == "vektor.2" }.id != erster.id)
        }
        val gehoben = basis.copy(anschlüsse = basis.anschlüsse.map { p -> if (p.name == "vektor.1")
            p.copy(art = MathematikAnschlussArten.AtlasWert.id, zulässigeArten = p.zulässigeArten + MathematikAnschlussArten.Methode.id) else p })
        val norm = konfiguriereVektorRechner(gehoben, VektorRechnerOperator.NORM)
        assertEquals(gehoben.anschlüsse.first { it.name == "vektor.1" }.id, norm.anschlüsse.first { it.name == "vektor" }.id)
        assertTrue(MathematikAnschlussArten.Methode.id in norm.anschlüsse.first { it.name == "vektor" }.zulässigeArten)
    }

}

package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import kotlin.test.*

class TypAbbildungAnschlussTest {
    private val objekt = AnschlussArt(AnschlussArtId("objekt"), "Objekt")
    private val vektor = AnschlussArt(AnschlussArtId("vektor"), "Vektor", objekt.id)
    private val spalte = AnschlussArt(AnschlussArtId("spalte"), "Spalte", vektor.id)
    private val zeile = AnschlussArt(AnschlussArtId("zeile"), "Zeile", vektor.id)
    private val matrix = AnschlussArt(AnschlussArtId("matrix"), "Matrix", objekt.id)
    private val zahl = AnschlussArt(AnschlussArtId("zahl"), "Zahl", objekt.id)
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(objekt, vektor, spalte, zeile, matrix, zahl)))

    @Test fun `Ausgang bildet Spaltenart auf Zeilenart ab`() {
        val quelle = quelle(spalte.id)
        val transponieren = transponieren()
        val karte = karte(quelle, transponieren)

        assertEquals(zeile.id, prüfung.effektiveArt(karte, ref(transponieren, AnschlussRichtung.Ausgang)))
    }

    @Test fun `Unverbundener Ausgang bleibt allgemeines Objekt`() {
        val transponieren = transponieren()
        assertEquals(objekt.id, prüfung.effektiveArt(KartenDaten(name = "Test", knoten = listOf(transponieren)), ref(transponieren, AnschlussRichtung.Ausgang)))
    }

    @Test fun `Nicht zugelassene Eingangsart wird abgelehnt`() {
        val quelle = quelle(zahl.id)
        val transponieren = transponieren()
        val karte = KartenDaten(name = "Test", knoten = listOf(quelle, transponieren))

        assertIs<VerbindungsPrüfung.Abgelehnt>(
            prüfung.prüfe(karte, ref(quelle, AnschlussRichtung.Ausgang), ref(transponieren, AnschlussRichtung.Eingang)),
        )
    }

    @Test fun `Typwechsel wird bei inkompatibler Folgeedge abgelehnt`() {
        val matrixQuelle = quelle(matrix.id)
        val spaltenQuelle = quelle(spalte.id)
        val transponieren = transponieren()
        val matrixZiel = ziel(matrix.id)
        val karte = karte(matrixQuelle, transponieren).copy(
            knoten = listOf(matrixQuelle, spaltenQuelle, transponieren, matrixZiel),
            verbindungen = karte(matrixQuelle, transponieren).verbindungen + VerbindungDaten(
                von = ref(transponieren, AnschlussRichtung.Ausgang),
                zu = ref(matrixZiel, AnschlussRichtung.Eingang),
            ),
        )

        val abgelehnt = assertIs<VerbindungsPrüfung.Abgelehnt>(
            prüfung.prüfe(
                karte,
                ref(spaltenQuelle, AnschlussRichtung.Ausgang),
                ref(transponieren, AnschlussRichtung.Eingang),
            ),
        )
        assertTrue(abgelehnt.grund.contains("abhängigen Ausgang"))
    }

    private fun karte(quelle: KnotenDaten, transponieren: KnotenDaten) = KartenDaten(
        name = "Test",
        knoten = listOf(quelle, transponieren),
        verbindungen = listOf(VerbindungDaten(
            von = ref(quelle, AnschlussRichtung.Ausgang),
            zu = ref(transponieren, AnschlussRichtung.Eingang),
        )),
    )

    private fun quelle(art: AnschlussArtId) = KnotenDaten(
        art = "test.quelle",
        name = "Quelle",
        anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)),
    )

    private fun ziel(art: AnschlussArtId) = KnotenDaten(
        art = "test.ziel",
        name = "Ziel",
        anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art)),
    )

    private fun transponieren() = KnotenDaten(
        art = "test.transponieren",
        name = "Transponieren",
        anschlüsse = listOf(
            AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = objekt.id,
                zulässigeArten = setOf(spalte.id, zeile.id, matrix.id),
            ),
            AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = objekt.id,
                artAbbildungVonEingang = AnschlussArtAbbildung(
                    "wert",
                    mapOf(spalte.id to zeile.id, zeile.id to spalte.id, matrix.id to matrix.id),
                ),
            ),
        ),
    )

    private fun ref(knoten: KnotenDaten, richtung: AnschlussRichtung) = AnschlussVerweis(
        knoten.id,
        knoten.anschlüsse.single { it.richtung == richtung }.id,
    )
}

package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import kotlin.test.*

class AbhängigeAnschlussArtTest {
    private val objekt = AnschlussArt(AnschlussArtId("objekt"), "Objekt")
    private val zahl = AnschlussArt(AnschlussArtId("zahl"), "Zahl", objekt.id)
    private val menge = AnschlussArt(AnschlussArtId("menge"), "Menge", objekt.id)
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(objekt, zahl, menge)))

    @Test fun `Ausgang übernimmt Art der verbundenen Eingangsquelle`() {
        val quelle = quelle("zahl", zahl.id)
        val alias = alias()
        val aliasEingang = ref(alias, AnschlussRichtung.Eingang)
        val aliasAusgang = ref(alias, AnschlussRichtung.Ausgang)
        val karte = KartenDaten(
            name = "Test",
            knoten = listOf(quelle, alias),
            verbindungen = listOf(VerbindungDaten(von = ref(quelle, AnschlussRichtung.Ausgang), zu = aliasEingang)),
        )

        assertEquals(zahl.id, prüfung.effektiveArt(karte, aliasAusgang))
    }

    @Test fun `Abhängiger Zahl-Ausgang darf an Zahl-Eingang`() {
        val quelle = quelle("zahl", zahl.id)
        val alias = alias()
        val ziel = ziel("ziel", zahl.id)
        val aliasEingang = ref(alias, AnschlussRichtung.Eingang)
        val aliasAusgang = ref(alias, AnschlussRichtung.Ausgang)
        val karte = KartenDaten(
            name = "Test",
            knoten = listOf(quelle, alias, ziel),
            verbindungen = listOf(VerbindungDaten(von = ref(quelle, AnschlussRichtung.Ausgang), zu = aliasEingang)),
        )

        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(karte, aliasAusgang, ref(ziel, AnschlussRichtung.Eingang)))
    }

    @Test fun `Typwechsel wird abgelehnt wenn Folge-Verbindung inkompatibel würde`() {
        val zahlQuelle = quelle("zahl", zahl.id)
        val mengenQuelle = quelle("menge", menge.id)
        val alias = alias()
        val ziel = ziel("ziel", zahl.id)
        val aliasEingang = ref(alias, AnschlussRichtung.Eingang)
        val aliasAusgang = ref(alias, AnschlussRichtung.Ausgang)
        val karte = KartenDaten(
            name = "Test",
            knoten = listOf(zahlQuelle, mengenQuelle, alias, ziel),
            verbindungen = listOf(
                VerbindungDaten(von = ref(zahlQuelle, AnschlussRichtung.Ausgang), zu = aliasEingang),
                VerbindungDaten(von = aliasAusgang, zu = ref(ziel, AnschlussRichtung.Eingang)),
            ),
        )

        val abgelehnt = assertIs<VerbindungsPrüfung.Abgelehnt>(
            prüfung.prüfe(karte, ref(mengenQuelle, AnschlussRichtung.Ausgang), aliasEingang),
        )
        assertTrue(abgelehnt.grund.contains("abhängigen Ausgang"))
    }

    private fun quelle(name: String, art: AnschlussArtId) = KnotenDaten(
        art = "test.quelle",
        name = name,
        anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art)),
    )

    private fun ziel(name: String, art: AnschlussArtId) = KnotenDaten(
        art = "test.ziel",
        name = name,
        anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art)),
    )

    private fun alias() = KnotenDaten(
        art = "test.alias",
        name = "Alias",
        anschlüsse = listOf(
            AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = objekt.id),
            AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = objekt.id, artFolgtEingang = "wert"),
        ),
    )

    private fun ref(knoten: KnotenDaten, richtung: AnschlussRichtung) = AnschlussVerweis(
        knoten.id,
        knoten.anschlüsse.single { it.richtung == richtung }.id,
    )
}

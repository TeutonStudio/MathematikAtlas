package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import kotlin.test.*

class AbhängigeAnschlussArtTest {
    private val objekt = AnschlussArt(AnschlussArtId("objekt"), "Objekt")
    private val zahl = AnschlussArt(AnschlussArtId("zahl"), "Zahl", objekt.id)
    private val menge = AnschlussArt(AnschlussArtId("menge"), "Menge", objekt.id)
    private val aussage = AnschlussArt(AnschlussArtId("aussage"), "Aussage", objekt.id)
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(objekt, zahl, menge, aussage)))

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

    @Test fun `Vereinigung gleicher Zweigarten bleibt spezifisch`() {
        val a = quelle("a", zahl.id)
        val b = quelle("b", zahl.id)
        val fall = fall()
        val karte = KartenDaten(
            name = "Test",
            knoten = listOf(a, b, fall),
            verbindungen = listOf(
                VerbindungDaten(von = ref(a, AnschlussRichtung.Ausgang), zu = ref(fall, "wahr", AnschlussRichtung.Eingang)),
                VerbindungDaten(von = ref(b, AnschlussRichtung.Ausgang), zu = ref(fall, "lüge", AnschlussRichtung.Eingang)),
            ),
        )

        assertEquals(zahl.id, prüfung.effektiveArt(karte, ref(fall, AnschlussRichtung.Ausgang)))
    }

    @Test fun `Vereinigung verschiedener Zweigarten verwendet gemeinsame Oberart`() {
        val a = quelle("a", zahl.id)
        val b = quelle("b", menge.id)
        val fall = fall()
        val karte = KartenDaten(
            name = "Test",
            knoten = listOf(a, b, fall),
            verbindungen = listOf(
                VerbindungDaten(von = ref(a, AnschlussRichtung.Ausgang), zu = ref(fall, "wahr", AnschlussRichtung.Eingang)),
                VerbindungDaten(von = ref(b, AnschlussRichtung.Ausgang), zu = ref(fall, "lüge", AnschlussRichtung.Eingang)),
            ),
        )

        assertEquals(objekt.id, prüfung.effektiveArt(karte, ref(fall, AnschlussRichtung.Ausgang)))
    }

    @Test fun `Zweiter Zweig darf bestehenden Fallausgang nicht inkompatibel verbreitern`() {
        val zahlQuelle = quelle("zahl", zahl.id)
        val mengenQuelle = quelle("menge", menge.id)
        val fall = fall()
        val zahlZiel = ziel("zahl-ziel", zahl.id)
        val karte = KartenDaten(
            name = "Test",
            knoten = listOf(zahlQuelle, mengenQuelle, fall, zahlZiel),
            verbindungen = listOf(
                VerbindungDaten(von = ref(zahlQuelle, AnschlussRichtung.Ausgang), zu = ref(fall, "wahr", AnschlussRichtung.Eingang)),
                VerbindungDaten(von = ref(fall, AnschlussRichtung.Ausgang), zu = ref(zahlZiel, AnschlussRichtung.Eingang)),
            ),
        )

        val abgelehnt = assertIs<VerbindungsPrüfung.Abgelehnt>(
            prüfung.prüfe(karte, ref(mengenQuelle, AnschlussRichtung.Ausgang), ref(fall, "lüge", AnschlussRichtung.Eingang)),
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

    private fun fall() = KnotenDaten(
        art = "test.fall",
        name = "Fall",
        anschlüsse = listOf(
            AnschlussDaten(name = "wahr", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = objekt.id, reihenfolge = 0),
            AnschlussDaten(name = "aussage", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = aussage.id, reihenfolge = 1),
            AnschlussDaten(name = "lüge", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = objekt.id, reihenfolge = 2),
            AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = objekt.id, artVereinigtEingänge = listOf("wahr", "lüge")),
        ),
    )

    private fun ref(knoten: KnotenDaten, richtung: AnschlussRichtung) = AnschlussVerweis(
        knoten.id,
        knoten.anschlüsse.single { it.richtung == richtung }.id,
    )

    private fun ref(knoten: KnotenDaten, name: String, richtung: AnschlussRichtung) = AnschlussVerweis(
        knoten.id,
        knoten.anschlüsse.single { it.richtung == richtung && it.name == name }.id,
    )
}

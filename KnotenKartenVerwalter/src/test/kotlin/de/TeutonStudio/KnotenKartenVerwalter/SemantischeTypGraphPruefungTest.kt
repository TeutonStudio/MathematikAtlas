package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.TypSystem.*
import kotlin.test.*

class SemantischeTypGraphPruefungTest {
    private val objektArt = AnschlussArt(AnschlussArtId("mathematik.objekt"), "Objekt")
    private val zahlArt = AnschlussArt(AnschlussArtId("mathematik.zahl"), "Zahl", objektArt.id)
    private val register = AnschlussArtRegister(listOf(objektArt, zahlArt))

    private val n = TypId("N")
    private val z = TypId("Z")
    private val r = TypId("R")
    private val c = TypId("C")
    private val typSystem = StandardTypSystem(
        istAtomUntertyp = { von, erwartet ->
            if (von.wert.startsWith("mathematik.") || erwartet.wert.startsWith("mathematik.")) {
                register.istUnterart(AnschlussArtId(von.wert), AnschlussArtId(erwartet.wert))
            } else {
                val eltern = mapOf(n to z, z to r, r to c)
                var aktuell: TypId? = von
                var passt = false
                while (aktuell != null) {
                    if (aktuell == erwartet) {
                        passt = true
                        break
                    }
                    aktuell = eltern[aktuell]
                }
                passt
            }
        },
        konstruktoren = listOf(TypKonstruktorDefinition(TypId("typ.tupel"))),
    )
    private val prüfung = GraphPrüfung(register, typSystem)

    @Test
    fun `gleiche Anschlussart kann semantisch inkompatibel sein`() {
        val quelle = knoten("q", AnschlussRichtung.Ausgang, TypAusdruck.Atom(c))
        val ziel = knoten("z", AnschlussRichtung.Eingang, TypAusdruck.Atom(r))
        val karte = KartenDaten(name = "Typen", knoten = listOf(quelle, ziel))

        assertIs<VerbindungsPrüfung.Abgelehnt>(prüfung.prüfe(karte, ref(quelle), ref(ziel)))
    }

    @Test
    fun `numerischer Untertyp darf semantisch in Obertyp fliessen`() {
        val quelle = knoten("q", AnschlussRichtung.Ausgang, TypAusdruck.Atom(n))
        val ziel = knoten("z", AnschlussRichtung.Eingang, TypAusdruck.Atom(r))
        val karte = KartenDaten(name = "Typen", knoten = listOf(quelle, ziel))

        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(karte, ref(quelle), ref(ziel)))
    }

    @Test
    fun `zielvereinigung akzeptiert passende Alternative`() {
        val quelle = knoten("q", AnschlussRichtung.Ausgang, TypAusdruck.Atom(n))
        val ziel = knoten(
            "z",
            AnschlussRichtung.Eingang,
            TypAusdruck.Vereinigung(listOf(TypAusdruck.Atom(r), TypAusdruck.Atom(c))),
        )
        val karte = KartenDaten(name = "Typen", knoten = listOf(quelle, ziel))

        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung.prüfe(karte, ref(quelle), ref(ziel)))
    }

    @Test
    fun `typ folgt verbundenem Eingang ohne Anschlussart zu veraendern`() {
        val quelle = knoten("q", AnschlussRichtung.Ausgang, TypAusdruck.Atom(r))
        val ein = AnschlussDaten(
            name = "ein",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = zahlArt.id,
        )
        val aus = AnschlussDaten(
            name = "aus",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = zahlArt.id,
            typInferenz = TypInferenzRegel.FolgtEingang("ein"),
        )
        val operator = KnotenDaten(art = "test.operator", name = "Operator", anschlüsse = listOf(ein, aus))
        val karte = KartenDaten(
            name = "Typen",
            knoten = listOf(quelle, operator),
            verbindungen = listOf(VerbindungDaten(von = ref(quelle), zu = AnschlussVerweis(operator.id, ein.id))),
        )

        assertEquals(TypAusdruck.Atom(r), prüfung.effektiverTyp(karte, AnschlussVerweis(operator.id, aus.id)))
        assertEquals(zahlArt.id, aus.art)
    }

    private fun knoten(name: String, richtung: AnschlussRichtung, typ: TypAusdruck) = KnotenDaten(
        art = "test",
        name = name,
        anschlüsse = listOf(
            AnschlussDaten(
                name = "wert",
                richtung = richtung,
                kante = if (richtung == AnschlussRichtung.Eingang) AnschlussKante.Links else AnschlussKante.Rechts,
                art = zahlArt.id,
                vertrag = AnschlussVertrag(typ),
            ),
        ),
    )

    private fun ref(knoten: KnotenDaten): AnschlussVerweis =
        AnschlussVerweis(knoten.id, knoten.anschlüsse.single().id)
}

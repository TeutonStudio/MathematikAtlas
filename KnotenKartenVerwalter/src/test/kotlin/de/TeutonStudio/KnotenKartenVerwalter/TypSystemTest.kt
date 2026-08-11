package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import org.junit.Assert.*
import org.junit.Test

class TypSystemTest {
    private val objekt = TypId("test.objekt")
    private val zahl = TypId("test.zahl")
    private val menge = TypId("test.menge")
    private val r = TypId("test.R")
    private val c = TypId("test.C")
    private val methode = TypId("test.methode")
    private val tupel = TypId("test.tupel")

    private fun system(): StandardTypSystem {
        val register = TypRegister().apply {
            registriereAtom(objekt)
            registriereAtom(zahl, listOf(objekt))
            registriereAtom(menge, listOf(objekt))
            registriereAtom(r, listOf(zahl))
            registriereAtom(c, listOf(zahl))
            registriereKonstruktor(
                TypKonstruktorBeschreibung(tupel, standardVarianz = TypVarianz.Kovariant),
                listOf(objekt),
            )
            registriereKonstruktor(
                TypKonstruktorBeschreibung(
                    methode,
                    argumentVarianzen = listOf(TypVarianz.Kontravariant, TypVarianz.Kovariant),
                ),
                listOf(objekt),
            )
        }
        return StandardTypSystem(register)
    }

    @Test
    fun unbekannt_ist_nicht_beliebig() {
        val typen = system()
        assertEquals(TypPrüfung.Unbestimmt, typen.prüfe(TypAusdruck.Unbekannt, TypAusdruck.Atom(r)))
        assertEquals(TypPrüfung.Unbestimmt, typen.prüfe(TypAusdruck.Beliebig, TypAusdruck.Atom(r)))
        assertEquals(TypPrüfung.Kompatibel, typen.prüfe(TypAusdruck.Atom(r), TypAusdruck.Beliebig))
    }

    @Test
    fun vereinigung_akzeptiert_eine_passende_alternative_aber_quellvereinigung_muss_vollstaendig_passen() {
        val typen = system()
        val ziel = TypAusdruck.Vereinigung(listOf(TypAusdruck.Atom(r), TypAusdruck.Atom(menge)))
        assertEquals(TypPrüfung.Kompatibel, typen.prüfe(TypAusdruck.Atom(r), ziel))

        val quelle = TypAusdruck.Vereinigung(listOf(TypAusdruck.Atom(r), TypAusdruck.Atom(menge)))
        assertTrue(typen.prüfe(quelle, TypAusdruck.Atom(zahl)) is TypPrüfung.Inkompatibel)
    }

    @Test
    fun vereinigung_entfernt_redundante_untertypen() {
        val typen = system()
        val normalisiert = typen.normalisiere(
            TypAusdruck.Vereinigung(listOf(TypAusdruck.Atom(r), TypAusdruck.Atom(zahl))),
        )
        assertEquals(TypAusdruck.Atom(zahl), normalisiert)
    }

    @Test
    fun tupel_sind_komponentenweise_kovariant() {
        val typen = system()
        val konkret = TypAusdruck.Parameterisiert(tupel, listOf(TypAusdruck.Atom(r), TypAusdruck.Atom(r)))
        val allgemein = TypAusdruck.Parameterisiert(tupel, listOf(TypAusdruck.Atom(zahl), TypAusdruck.Atom(zahl)))
        assertEquals(TypPrüfung.Kompatibel, typen.prüfe(konkret, allgemein))
        assertTrue(typen.prüfe(allgemein, konkret) is TypPrüfung.Inkompatibel)
    }

    @Test
    fun methoden_pruefen_argument_kontravariant_und_ergebnis_kovariant() {
        val typen = system()
        val breiteMethode = TypAusdruck.Parameterisiert(
            methode,
            listOf(TypAusdruck.Atom(objekt), TypAusdruck.Atom(r)),
        )
        val erwarteteMethode = TypAusdruck.Parameterisiert(
            methode,
            listOf(TypAusdruck.Atom(r), TypAusdruck.Atom(zahl)),
        )
        assertEquals(TypPrüfung.Kompatibel, typen.prüfe(breiteMethode, erwarteteMethode))
        assertTrue(typen.prüfe(erwarteteMethode, breiteMethode) is TypPrüfung.Inkompatibel)
    }

    @Test
    fun graphpruefung_lehnt_semantisch_inkompatible_typen_trotz_gleicher_anschlusskategorie_ab() {
        val objektArt = AnschlussArt(AnschlussArtId("test.objekt"), "Objekt")
        val zahlArt = AnschlussArt(AnschlussArtId("test.zahl"), "Zahl", objektArt.id)
        val arten = AnschlussArtRegister(listOf(objektArt, zahlArt))
        val typenRegister = TypRegister().apply {
            registriereAtom(TypId(objektArt.id.wert))
            registriereAtom(TypId(zahlArt.id.wert), listOf(TypId(objektArt.id.wert)))
            registriereAtom(r, listOf(TypId(zahlArt.id.wert)))
            registriereAtom(c, listOf(TypId(zahlArt.id.wert)))
        }
        val prüfung = GraphPrüfung(arten, StandardTypSystem(typenRegister))

        val ausgang = AnschlussDaten(
            id = AnschlussId("ausgang"),
            name = "Ausgang",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = zahlArt.id,
            vertrag = AnschlussVertrag(TypAusdruck.Atom(c)),
        )
        val eingang = AnschlussDaten(
            id = AnschlussId("eingang"),
            name = "Eingang",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = zahlArt.id,
            vertrag = AnschlussVertrag(TypAusdruck.Atom(r)),
        )
        val quelle = KnotenDaten(id = KnotenId("q"), art = "test", name = "Quelle", anschlüsse = listOf(ausgang))
        val ziel = KnotenDaten(id = KnotenId("z"), art = "test", name = "Ziel", anschlüsse = listOf(eingang))
        val karte = KartenDaten(name = "Typprüfung", knoten = listOf(quelle, ziel))

        val ergebnis = prüfung.prüfe(
            karte,
            AnschlussVerweis(quelle.id, ausgang.id),
            AnschlussVerweis(ziel.id, eingang.id),
        )
        assertTrue(ergebnis is VerbindungsPrüfung.Abgelehnt)
    }

    @Test
    fun semantische_tupelinferenz_verwendet_stabile_eingangsnamen() {
        val objektArt = AnschlussArt(AnschlussArtId("test.objekt"), "Objekt")
        val arten = AnschlussArtRegister(listOf(objektArt))
        val register = TypRegister().apply {
            registriereAtom(TypId(objektArt.id.wert))
            registriereAtom(r, listOf(TypId(objektArt.id.wert)))
            registriereAtom(c, listOf(TypId(objektArt.id.wert)))
            registriereKonstruktor(TypKonstruktorBeschreibung(tupel, TypVarianz.Kovariant), listOf(TypId(objektArt.id.wert)))
        }
        val prüfung = GraphPrüfung(arten, StandardTypSystem(register))

        fun quelle(id: String, typ: TypId) = KnotenDaten(
            id = KnotenId(id),
            art = "quelle",
            name = id,
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("$id-out"),
                    name = "Ausgang",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = objektArt.id,
                    vertrag = AnschlussVertrag(TypAusdruck.Atom(typ)),
                ),
            ),
        )
        val q1 = quelle("q1", r)
        val q2 = quelle("q2", c)
        val a = AnschlussDaten(AnschlussId("a"), "a", AnschlussRichtung.Eingang, AnschlussKante.Links, objektArt.id)
        val b = AnschlussDaten(AnschlussId("b"), "b", AnschlussRichtung.Eingang, AnschlussKante.Links, objektArt.id, reihenfolge = 1)
        val out = AnschlussDaten(
            id = AnschlussId("tuple-out"),
            name = "Tupel",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = objektArt.id,
            typInferenz = TypInferenzRegel.TupelAusEingängen(listOf("a", "b"), tupel),
        )
        val kombi = KnotenDaten(id = KnotenId("k"), art = "kombi", name = "Kombi", anschlüsse = listOf(a, b, out))
        val karte = KartenDaten(
            name = "Tupel",
            knoten = listOf(q1, q2, kombi),
            verbindungen = listOf(
                VerbindungDaten(von = AnschlussVerweis(q1.id, q1.anschlüsse.single().id), zu = AnschlussVerweis(kombi.id, a.id)),
                VerbindungDaten(von = AnschlussVerweis(q2.id, q2.anschlüsse.single().id), zu = AnschlussVerweis(kombi.id, b.id)),
            ),
        )

        assertEquals(
            TypAusdruck.Parameterisiert(tupel, listOf(TypAusdruck.Atom(r), TypAusdruck.Atom(c))),
            prüfung.effektiverTyp(karte, AnschlussVerweis(kombi.id, out.id)),
        )
    }
}

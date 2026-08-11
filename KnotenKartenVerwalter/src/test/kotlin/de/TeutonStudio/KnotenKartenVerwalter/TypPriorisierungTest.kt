package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import org.junit.Assert.assertEquals
import org.junit.Test

class TypPriorisierungTest {
    @Test
    fun priorisierung_waehlt_die_erste_passende_semantische_prioritaet() {
        val objektArt = AnschlussArt(AnschlussArtId("test.objekt"), "Objekt")
        val arten = AnschlussArtRegister(listOf(objektArt))
        val allgemein = TypId("test.allgemein")
        val speziell = TypId("test.speziell")
        val register = TypRegister().apply {
            registriereAtom(TypId(objektArt.id.wert))
            registriereAtom(allgemein, listOf(TypId(objektArt.id.wert)))
            registriereAtom(speziell, listOf(allgemein))
        }
        val pruefung = GraphPrüfung(arten, StandardTypSystem(register))

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

        val q1 = quelle("q1", speziell)
        val q2 = quelle("q2", allgemein)
        val a = AnschlussDaten(AnschlussId("a"), "a", AnschlussRichtung.Eingang, AnschlussKante.Links, objektArt.id)
        val b = AnschlussDaten(AnschlussId("b"), "b", AnschlussRichtung.Eingang, AnschlussKante.Links, objektArt.id, reihenfolge = 1)
        val ausgang = AnschlussDaten(
            id = AnschlussId("out"),
            name = "Ergebnis",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = objektArt.id,
            typInferenz = TypInferenzRegel.Priorisierung(
                eingänge = listOf("a", "b"),
                prioritäten = listOf(TypAusdruck.Atom(speziell), TypAusdruck.Atom(allgemein)),
            ),
        )
        val kombi = KnotenDaten(
            id = KnotenId("k"),
            art = "kombi",
            name = "Kombi",
            anschlüsse = listOf(a, b, ausgang),
        )
        val karte = KartenDaten(
            name = "Priorisierung",
            knoten = listOf(q1, q2, kombi),
            verbindungen = listOf(
                VerbindungDaten(
                    von = AnschlussVerweis(q1.id, q1.anschlüsse.single().id),
                    zu = AnschlussVerweis(kombi.id, a.id),
                ),
                VerbindungDaten(
                    von = AnschlussVerweis(q2.id, q2.anschlüsse.single().id),
                    zu = AnschlussVerweis(kombi.id, b.id),
                ),
            ),
        )

        assertEquals(
            TypAusdruck.Atom(speziell),
            pruefung.effektiverTyp(karte, AnschlussVerweis(kombi.id, ausgang.id)),
        )
    }

    @Test
    fun priorisierung_bleibt_im_format_8_roundtrip_erhalten() {
        val regel = TypInferenzRegel.Priorisierung(
            eingänge = listOf("links", "rechts"),
            prioritäten = listOf(
                TypAusdruck.Atom(TypId("test.a")),
                TypAusdruck.Atom(TypId("test.b")),
            ),
        )
        val anschluss = AnschlussDaten(
            id = AnschlussId("out"),
            name = "Ausgang",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = AnschlussArtId("test.objekt"),
            typInferenz = regel,
        )
        val karte = KartenDaten(
            id = KartenId("karte"),
            name = "Priorisierung",
            erstelltAm = 0L,
            knoten = listOf(
                KnotenDaten(id = KnotenId("k"), art = "test", name = "Test", anschlüsse = listOf(anschluss)),
            ),
        )

        val gelesen = KartenDatenJson.lese(KartenDatenJson.schreibe(karte))
        assertEquals(regel, gelesen.knoten.single().anschlüsse.single().typInferenz)
    }
}

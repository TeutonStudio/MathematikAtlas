package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.*

class KartenDatenTypJsonTest {
    @Test fun `Format 8 erhält Typvertrag und Inferenz`() {
        val anschluss = AnschlussDaten(
            name = "wert",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = AnschlussArtId("mathematik.methode"),
            vertrag = AnschlussVertrag(
                typ = TypAusdruck.Parameterisiert(
                    TypId("math.methode"),
                    listOf(
                        TypAusdruck.Parameterisiert(
                            TypKernIds.Tupel,
                            listOf(TypAusdruck.Atom(TypId("math.zahl.reell"))),
                        ),
                        TypAusdruck.Vereinigung(
                            listOf(
                                TypAusdruck.Atom(TypId("math.zahl.reell")),
                                TypAusdruck.Atom(TypId("math.zahl.komplex")),
                            ),
                        ),
                    ),
                ),
                anforderungen = listOf(
                    TypAnforderung.Struktur("topologie"),
                    TypAnforderung.Axiom("stetigkeit"),
                ),
            ),
            typInferenz = TypInferenzRegel.Priorisierung(
                eingänge = listOf("a", "b"),
                prioritäten = listOf(TypAusdruck.Atom(TypId("math.methode"))),
            ),
        )
        val karte = KartenDaten(
            name = "Typen",
            knoten = listOf(KnotenDaten(art = "test", name = "Knoten", anschlüsse = listOf(anschluss))),
        )

        val json = KartenDatenJson.schreibe(karte)
        val geladen = KartenDatenJson.lese(json)
        val geladenAnschluss = geladen.knoten.single().anschlüsse.single()

        assertEquals(8, KartenDatenJson.formatVersion(json))
        assertEquals(anschluss.vertrag, geladenAnschluss.vertrag)
        assertEquals(anschluss.typInferenz, geladenAnschluss.typInferenz)
    }

    @Test fun `Format 7 ohne Typfelder bleibt lesbar`() {
        val karte = KartenDaten(
            name = "Alt",
            knoten = listOf(
                KnotenDaten(
                    art = "test",
                    name = "Alt",
                    anschlüsse = listOf(
                        AnschlussDaten(
                            name = "wert",
                            richtung = AnschlussRichtung.Ausgang,
                            kante = AnschlussKante.Rechts,
                            art = AnschlussArtId("objekt"),
                        ),
                    ),
                ),
            ),
        )
        val format8 = KartenDatenJson.schreibe(karte)
        val format7 = format8
            .replace("\"formatVersion\": 8", "\"formatVersion\": 7")
            .replace(Regex(",?\\s*\"vertrag\"\\s*:\\s*\\{\\s*\"typ\"\\s*:\\s*\\{\\s*\"art\"\\s*:\\s*\"unbekannt\"\\s*}\\s*,\\s*\"anforderungen\"\\s*:\\s*\\[\\s*]\\s*}"), "")

        val geladen = KartenDatenJson.lese(format7)

        assertEquals(TypAusdruck.Unbekannt, geladen.knoten.single().anschlüsse.single().vertrag.typ)
    }
}

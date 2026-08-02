package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KartenWahrheitstabellenTest {
    private val register = AnschlussArtRegister(MathematikAnschlussArten.alle)

    @Test
    fun `erkennt fest versionierten Karten-Knoten mit mehreren Aussageausgaengen`() {
        val karte = testKarte(
            version = 7,
            eingänge = listOf("A" to MathematikAnschlussArten.Aussage.id),
            ausgänge = listOf(
                "gültig" to MathematikAnschlussArten.Aussage.id,
                "widerspruch" to MathematikAnschlussArten.Aussage.id,
                "anzahl" to MathematikAnschlussArten.Zahl.id,
            ),
        )
        val knoten = gruppenKnoten(karte)

        val quelle = assertNotNull(ermittleKartenWahrheitstabellenQuelle(register, knoten, karte))

        assertEquals(7, quelle.verweis.version)
        assertEquals(listOf("gültig", "widerspruch"), quelle.aussageAusgänge.map { it.name })
        assertEquals(listOf("anzahl"), quelle.weitereAusgänge.map { it.name })
    }

    @Test
    fun `Karte ohne Aussageausgang besitzt keine Wahrheitstabelle`() {
        val karte = testKarte(
            eingänge = listOf("x" to MathematikAnschlussArten.Zahl.id),
            ausgänge = listOf("wert" to MathematikAnschlussArten.Zahl.id),
        )

        assertNull(ermittleKartenWahrheitstabellenQuelle(register, gruppenKnoten(karte), karte))
    }

    @Test
    fun `Karte ohne freie logische Eingaenge besitzt genau eine Zeile`() {
        assertEquals(BigInteger.ONE, kartenWahrheitstabellenZeilenAnzahl(0))
        assertEquals(BigInteger.valueOf(8), kartenWahrheitstabellenZeilenAnzahl(3))
    }

    @Test
    fun `mehrstelliges Tabellenpraedikat gilt nur fuer das definierte Argumenttupel`() {
        val methode = erzeugeTabellenPrädikat(
            name = "P",
            definitionsMengen = listOf(BenannteMenge("M"), ReelleZahlen),
            argumente = listOf(AllgemeinerParameter("a"), RationaleZahl.von(2)),
            wert = true,
        )
        val parameter = methode.parameter

        val treffer = methode.wendeAn(
            mapOf(
                parameter[0].name to AllgemeinerParameter("a"),
                parameter[1].name to RationaleZahl.von(2),
            ),
        ) as Aussage
        val andererFall = methode.wendeAn(
            mapOf(
                parameter[0].name to AllgemeinerParameter("b"),
                parameter[1].name to RationaleZahl.von(2),
            ),
        ) as Aussage

        assertEquals(Wahrheitswert.Wahr, treffer.entscheide().wahrheitswert)
        assertNull(andererFall.entscheide().wahrheitswert)
    }

    private fun testKarte(
        version: Int = 1,
        eingänge: List<Pair<String, AnschlussArtId>>,
        ausgänge: List<Pair<String, AnschlussArtId>>,
    ): KartenDaten {
        val knoten = eingänge.mapIndexed { index, (name, art) ->
            KnotenDaten(
                id = KnotenId("eingang-$index"),
                art = "mathematik.kartenEingang",
                name = name,
                parameter = mapOf("name" to name),
                anschlüsse = listOf(
                    AnschlussDaten(
                        id = AnschlussId("eingang-$index-wert"),
                        name = "wert",
                        richtung = AnschlussRichtung.Ausgang,
                        kante = AnschlussKante.Rechts,
                        art = art,
                    ),
                ),
            )
        } + ausgänge.mapIndexed { index, (name, art) ->
            KnotenDaten(
                id = KnotenId("ausgang-$index"),
                art = "mathematik.kartenAusgang",
                name = name,
                parameter = mapOf("name" to name),
                anschlüsse = listOf(
                    AnschlussDaten(
                        id = AnschlussId("ausgang-$index-wert"),
                        name = "wert",
                        richtung = AnschlussRichtung.Eingang,
                        kante = AnschlussKante.Links,
                        art = art,
                    ),
                ),
            )
        }
        return KartenDaten(id = KartenId("logik-karte"), name = "Logikkarte", version = version, knoten = knoten)
    }

    private fun gruppenKnoten(karte: KartenDaten): KnotenDaten {
        val anschlüsse = karte.knoten.mapIndexed { index, intern ->
            val art = intern.anschlüsse.single().art
            if (intern.art == "mathematik.kartenEingang") {
                AnschlussDaten(
                    id = AnschlussId("aussen-eingang-$index"),
                    name = intern.parameter.getValue("name"),
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = art,
                    reihenfolge = index,
                )
            } else {
                AnschlussDaten(
                    id = AnschlussId("aussen-ausgang-$index"),
                    name = intern.parameter.getValue("name"),
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = art,
                    reihenfolge = index,
                )
            }
        }
        return KnotenDaten(
            id = KnotenId("gruppe"),
            art = "gruppe.${karte.id.wert}",
            name = karte.name,
            anschlüsse = anschlüsse,
            kartenVerweis = KartenVerweis(karte.id, karte.version),
        )
    }
}

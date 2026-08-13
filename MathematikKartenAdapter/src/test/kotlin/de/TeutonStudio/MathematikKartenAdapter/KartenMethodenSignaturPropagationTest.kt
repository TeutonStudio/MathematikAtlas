package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class KartenMethodenSignaturPropagationTest {
    @Test
    fun `referenzierte Karte bewahrt deklarierte Signatur eines freien Methodeneingangs`() {
        val methodenArt = AnschlussArtId("mathematik.methode")
        val eingang = KnotenDaten(
            id = KnotenId("methode-eingang"),
            art = KARTEN_EINGANG_ART,
            name = "f",
            parameter = mapOf(
                "name" to "f",
                KARTEN_METHODEN_SIGNATUR_AKTIV to "true",
                KARTEN_METHODEN_ARGUMENT_ANZAHL to "2",
                kartenMethodenArgumentNameSchlüssel(0) to "x",
                kartenMethodenArgumentWerteVorratSchlüssel(0) to "R",
                kartenMethodenArgumentNameSchlüssel(1) to "n",
                kartenMethodenArgumentWerteVorratSchlüssel(1) to "Z",
                KARTEN_METHODEN_ZIELMENGE to "C",
            ),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("methode-eingang-wert"),
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = methodenArt,
                ),
            ),
        )
        val ausgang = KnotenDaten(
            id = KnotenId("methode-ausgang"),
            art = "mathematik.kartenAusgang",
            name = "resultat",
            parameter = mapOf("name" to "resultat"),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("methode-ausgang-wert"),
                    name = "wert",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = methodenArt,
                ),
            ),
        )
        val intern = KartenDaten(
            id = KartenId("signatur-intern"),
            name = "Signatur intern",
            version = 1,
            knoten = listOf(eingang, ausgang),
            verbindungen = listOf(
                VerbindungDaten(
                    von = AnschlussVerweis(eingang.id, eingang.anschlüsse.single().id),
                    zu = AnschlussVerweis(ausgang.id, ausgang.anschlüsse.single().id),
                ),
            ),
        )
        val gruppe = KnotenDaten(
            id = KnotenId("gruppe"),
            art = "gruppe.${intern.id.wert}",
            name = "Gruppe",
            kartenVerweis = KartenVerweis(intern.id, intern.version),
        )
        val register = MathematikAuswerterRegister().apply {
            registriere("mathematik.kartenAusgang") { kontext ->
                KnotenAuswertungsErgebnis(mapOf("wert" to kontext.eingänge.getValue("wert")))
            }
        }

        val ergebnis = KartenAuswerter(
            register,
            KartenQuelle { if (it == gruppe.kartenVerweis) intern else null },
        ).auswerten(KartenDaten(name = "Außen", knoten = listOf(gruppe)))

        val methode = assertIs<SignaturtragendeMethode>(
            ergebnis.knoten.getValue(gruppe.id).ausgaben.getValue("resultat").objekt,
        )
        assertEquals(listOf("x", "n"), methode.signatur.argumente.map { it.parameter.name })
        assertEquals(listOf(ReelleZahlen, GanzeZahlen), methode.signatur.argumente.map { it.werteVorrat })
        assertEquals(KomplexeZahlen, methode.signatur.zielMenge)
        assertFalse(methode is MathematischAuswertbareMethode)
    }
}

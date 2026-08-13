package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KartenEingangMethodensignaturTest {
    @Test
    fun `allgemeiner Methodeneingang erfindet ohne Deklaration keine Signatur`() {
        val wert = symbolischerEingangswert(
            art = AnschlussArtId("mathematik.methode"),
            name = "f",
            knotenId = KnotenId("eingang"),
        )
        val methode = assertIs<Methode>(wert.objekt)

        assertFalse(methode is SignaturtragendeMethode)
        assertTrue(runCatching { methode.methodenSignatur() }.isFailure)
    }

    @Test
    fun `deklarierte Signatur bewahrt Argumentraeume und Zielmenge ohne Auswertungsfaehigkeit`() {
        val knoten = KnotenDaten(
            id = KnotenId("eingang"),
            art = KARTEN_EINGANG_ART,
            name = "f",
            parameter = mapOf(
                KARTEN_METHODEN_SIGNATUR_AKTIV to "true",
                KARTEN_METHODEN_ARGUMENT_ANZAHL to "2",
                kartenMethodenArgumentNameSchlüssel(0) to "x",
                kartenMethodenArgumentWerteVorratSchlüssel(0) to "R",
                kartenMethodenArgumentNameSchlüssel(1) to "n",
                kartenMethodenArgumentWerteVorratSchlüssel(1) to "Z",
                KARTEN_METHODEN_ZIELMENGE to "C",
            ),
        )
        val signatur = requireNotNull(deklarierteMethodenSignatur(knoten))

        assertEquals(listOf("x", "n"), signatur.argumente.map { it.parameter.name })
        assertEquals(listOf(ReelleZahlen, GanzeZahlen), signatur.argumente.map { it.werteVorrat })
        assertEquals(KomplexeZahlen, signatur.zielMenge)

        val wert = symbolischerEingangswert(
            art = AnschlussArtId("mathematik.methode"),
            name = "f",
            knotenId = knoten.id,
            methodenSignatur = signatur,
        )
        val methode = assertIs<SignaturtragendeMethode>(wert.objekt)

        assertEquals(signatur, methode.signatur)
        assertFalse(methode is MathematischAuswertbareMethode)
    }
}

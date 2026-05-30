package com.TeutonStudio.KnotenKartenVerwalter

import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungsRegeln
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerbindungsRegelnTest {
    @Test
    fun standardRegelnErlaubenAusgangZuEingang() {
        assertTrue(VerbindungsRegeln().darfErstellen(emptyList(), verbindung()))
    }

    @Test
    fun standardRegelnVerbietenSelbstverbindung() {
        val verbindung = verbindung(quellKnotenId = "a", zielKnotenId = "a")

        assertFalse(VerbindungsRegeln().darfErstellen(emptyList(), verbindung))
    }

    @Test
    fun standardRegelnVerbietenDoppelteVerbindungen() {
        val verbindung = verbindung()

        assertFalse(VerbindungsRegeln().darfErstellen(listOf(verbindung), verbindung.copyMitId("neu")))
    }

    @Test
    fun richtungsregelKannDeaktiviertWerden() {
        val regeln = VerbindungsRegeln(nurAusgangZuEingang = false)

        assertTrue(
            regeln.darfErstellen(
                vorhandeneVerbindungen = emptyList(),
                neueVerbindung = verbindung(),
                quellRichtung = AnschlussRichtung.Eingang,
                zielRichtung = AnschlussRichtung.Ausgang,
            ),
        )
    }

    private fun verbindung(
        quellKnotenId: String = "quelle",
        zielKnotenId: String = "ziel",
    ) = VerbindungDaten(
        id = "v",
        quellKnotenId = quellKnotenId,
        quellAnschlussId = "out",
        zielKnotenId = zielKnotenId,
        zielAnschlussId = "in",
    )

    private fun VerbindungDaten.copyMitId(id: String) = VerbindungDaten(
        id = id,
        quellKnotenId = quellKnotenId,
        quellAnschlussId = quellAnschlussId,
        zielKnotenId = zielKnotenId,
        zielAnschlussId = zielAnschlussId,
        label = label,
        art = art,
        ausgewaehlt = ausgewaehlt,
    )
}

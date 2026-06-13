package de.TeutonStudio.KnotenKartenVerwalter

import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungsRegeln
import com.TeutonStudio.KnotenKartenVerwalter.daten.ZahlenTyp
import com.TeutonStudio.KnotenKartenVerwalter.daten.Zahlenraum
import com.TeutonStudio.KnotenKartenVerwalter.daten.mitTypPruefung
import org.junit.Assert.assertEquals
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

    @Test
    fun zahlenTypenWerdenGeprueft() {
        assertTrue(
            VerbindungsRegeln().darfErstellen(
                vorhandeneVerbindungen = emptyList(),
                neueVerbindung = verbindung(),
                quellTyp = ZahlenTyp(Zahlenraum.Ganz),
                zielTyp = ZahlenTyp(Zahlenraum.Reell),
            ),
        )
        assertFalse(
            VerbindungsRegeln().darfErstellen(
                vorhandeneVerbindungen = emptyList(),
                neueVerbindung = verbindung(),
                quellTyp = ZahlenTyp(Zahlenraum.Komplex),
                zielTyp = ZahlenTyp(Zahlenraum.Reell),
            ),
        )
    }

    @Test
    fun verbindungMerktTypUndFehlertext() {
        val verbindung = verbindung().mitTypPruefung(
            quellTyp = ZahlenTyp(Zahlenraum.Komplex),
            zielTyp = ZahlenTyp(Zahlenraum.Reell),
        )

        assertTrue(requireNotNull(verbindung.fehler).contains("passt nicht"))
        assertEquals(ZahlenTyp(Zahlenraum.Komplex), verbindung.zahlenTyp)
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
        zahlenTyp = zahlenTyp,
        fehler = fehler,
    )
}

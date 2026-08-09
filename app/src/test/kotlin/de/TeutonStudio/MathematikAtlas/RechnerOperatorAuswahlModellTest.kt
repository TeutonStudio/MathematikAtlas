package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RechnerOperatorAuswahlModellTest {
    private val kandidat = KnotenDaten(
        art = "test",
        name = "Test",
        anschlüsse = listOf(
            AnschlussDaten(
                name = "größenWert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = AnschlussArtId("mathematik.zahl"),
            ),
            AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = AnschlussArtId("mathematik.zahl"),
            ),
        ),
    )
    private val addition = RechnerOperatorAuswahlEintrag(
        id = "zahl.addition",
        titel = "Addition",
        symbolLatex = "+",
        kategorie = "Grundrechenarten",
        beschreibung = "Addiert Zahlen.",
        suchbegriffe = setOf("Summe"),
        kandidat = kandidat,
    )
    private val tangens = RechnerOperatorAuswahlEintrag(
        id = "zahl.tan",
        titel = "Tangens",
        symbolLatex = "\\tan",
        kategorie = "Trigonometrie",
        beschreibung = "Trigonometrische Funktion.",
        kandidat = kandidat,
    )

    @Test
    fun `Suche findet Titel ID Synonym und Anschlussrolle`() {
        val einträge = listOf(addition, tangens)

        assertEquals(listOf(addition), filtereRechnerOperatoren(einträge, "Addition", null))
        assertEquals(listOf(tangens), filtereRechnerOperatoren(einträge, "zahl.tan", null))
        assertEquals(listOf(addition), filtereRechnerOperatoren(einträge, "summe", null))
        assertEquals(einträge, filtereRechnerOperatoren(einträge, "grossenwert", null))
    }

    @Test
    fun `Kategorie und Suche werden gemeinsam angewendet`() {
        assertEquals(
            listOf(tangens),
            filtereRechnerOperatoren(listOf(addition, tangens), "", "Trigonometrie"),
        )
        assertEquals(
            emptyList(),
            filtereRechnerOperatoren(listOf(addition, tangens), "Summe", "Trigonometrie"),
        )
    }

    @Test
    fun `Formel wird erst gebaut und danach als Kandidat ersetzt`() {
        val formel = RechnerOperatorAuswahlEintrag(
            id = "zahl.formel",
            titel = "Eigene Formel",
            symbolLatex = "f(x)",
            kategorie = "Eigene Formeln",
            beschreibung = "Eigener Ausdruck.",
            art = RechnerOperatorAuswahlArt.FORMEL,
        )

        assertEquals(
            RechnerOperatorBestätigungsAktion.FORMEL_BAUEN,
            bestätigungsAktionFür(formel, addition.id),
        )
        assertEquals(
            RechnerOperatorBestätigungsAktion.KNOTEN_ERSETZEN,
            bestätigungsAktionFür(formel.copy(kandidat = kandidat), addition.id),
        )
    }

    @Test
    fun `Aktueller oder unbekannter Operator löst keinen Austausch aus`() {
        val unbekannt = RechnerOperatorAuswahlEintrag(
            id = "veraltet",
            titel = "Unbekannt",
            symbolLatex = "?",
            kategorie = "Nicht verfügbar",
            beschreibung = "Nicht registriert.",
            art = RechnerOperatorAuswahlArt.UNBEKANNT,
        )

        assertEquals(RechnerOperatorBestätigungsAktion.KEINE, bestätigungsAktionFür(addition, addition.id))
        assertEquals(RechnerOperatorBestätigungsAktion.KEINE, bestätigungsAktionFür(unbekannt, unbekannt.id))
    }
}

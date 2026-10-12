package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KartenMethodenFallbackTest {
    private val zahlArt = AnschlussArtId("mathematik.zahl")
    private val zahlMethodeArt = AnschlussArtId("mathematik.funktion.zahl")

    @Test
    fun `unverbundener Methodeneingang verwendet Kartenfallback und Edge hat Vorrang`() {
        val internerEingang = KnotenDaten(
            id = KnotenId("intern-eingang"),
            art = "mathematik.kartenEingang",
            name = "x",
            parameter = mapOf("name" to "x"),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("intern-eingang-wert"),
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = zahlArt,
                ),
            ),
        )
        val internerAusgang = KnotenDaten(
            id = KnotenId("intern-ausgang"),
            art = "mathematik.kartenAusgang",
            name = "wert",
            parameter = mapOf("name" to "wert"),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("intern-ausgang-wert"),
                    name = "wert",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = zahlArt,
                ),
            ),
        )
        val intern = KartenDaten(
            id = KartenId("fallback-karte"),
            name = "Fallback-Methode",
            version = 3,
            knoten = listOf(internerEingang, internerAusgang),
            verbindungen = listOf(
                VerbindungDaten(
                    von = AnschlussVerweis(internerEingang.id, internerEingang.anschlüsse.single().id),
                    zu = AnschlussVerweis(internerAusgang.id, internerAusgang.anschlüsse.single().id),
                ),
            ),
        )
        val methodenEingang = AnschlussDaten(
            id = AnschlussId("iteration-methode"),
            name = "methode",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = zahlMethodeArt,
        )
        val iteration = KnotenDaten(
            id = KnotenId("iteration"),
            art = "mathematik.iterierteSumme",
            name = "Iterierte Summe",
            anschlüsse = listOf(methodenEingang),
            eingangsKartenVerweise = mapOf("methode" to KartenVerweis(intern.id, intern.version)),
        )
        val expliziteMethode = Methode(
            name = "g",
            parameter = listOf(Variable("t")),
            ausgaben = mapOf("wert" to RationaleZahl.Eins),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf("t" to ReelleZahlen),
        )
        val quelle = KnotenDaten(
            id = KnotenId("quelle"),
            art = "test.methode",
            name = "Explizite Methode",
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("quelle-methode"),
                    name = "methode",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = zahlMethodeArt,
                ),
            ),
        )
        val register = MathematikAuswerterRegister().apply {
            registriere("mathematik.kartenAusgang") { kontext ->
                val wert = kontext.eingänge.getValue("wert")
                KnotenAuswertungsErgebnis(mapOf("wert" to wert.copy(zielMenge = ReelleZahlen)))
            }
            registriere("mathematik.iterierteSumme") { kontext ->
                KnotenAuswertungsErgebnis(mapOf("methode" to kontext.eingänge.getValue("methode")))
            }
            registriere("test.methode") {
                KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(expliziteMethode)))
            }
        }
        val auswerter = KartenAuswerter(
            register = register,
            kartenQuelle = KartenQuelle { verweis ->
                if (verweis == KartenVerweis(intern.id, intern.version)) intern else null
            },
        )

        val ohneEdge = auswerter.auswerten(KartenDaten(name = "Außen", knoten = listOf(iteration)))
        val fallback = assertIs<Methode>(
            ohneEdge.knoten.getValue(iteration.id).ausgaben.getValue("methode").objekt,
        )
        assertEquals("Fallback-Methode", fallback.name)

        // Rechner heben Eingänge auf AtlasWert mit zugelassenen Aussage-/Methodenarten an.
        // Ohne expliziten Standardwert muss die Quelle vor dem impliziten false greifen.
        val angehoben = iteration.copy(anschlüsse = listOf(methodenEingang.copy(
            art = AnschlussArtId("mathematik.atlasWert"),
            zulässigeArten = setOf(AnschlussArtId("mathematik.aussage"), AnschlussArtId("mathematik.methode")),
        )))
        val mitAtlasWert = auswerter.auswerten(KartenDaten(name = "Angehoben", knoten = listOf(angehoben)))
        assertEquals("Fallback-Methode", assertIs<Methode>(mitAtlasWert.knoten.getValue(iteration.id)
            .ausgaben.getValue("methode").objekt).name)


        val mitEdge = auswerter.auswerten(
            KartenDaten(
                name = "Außen",
                knoten = listOf(quelle, iteration),
                verbindungen = listOf(
                    VerbindungDaten(
                        von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                        zu = AnschlussVerweis(iteration.id, methodenEingang.id),
                    ),
                ),
            ),
        )
        val verbunden = assertIs<Methode>(
            mitEdge.knoten.getValue(iteration.id).ausgaben.getValue("methode").objekt,
        )
        assertEquals("g", verbunden.name)
    }
}

package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MethodenSignaturProjektionTest {
    @Test fun `Historischer Ergebnisexport normalisiert auch operative Metadatenbedingungen`() {
        val y=Variable("y")
        val w=BedingterWert(y,setOf(Ungleichheit(y,RationaleZahl.Null)),variablenQuellen=listOf(VariablenQuelle(de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId("alt"),"y",ReelleZahlen,bindungsId="qy")))
        val f=w.alsGebundeneErgebnisFormel()
        val m=f.alsMethode("id")
        val r=m.wendeMitIdentitätenAn(mapOf("qy" to RationaleZahl.von(2)))
        assertEquals(Tupel(listOf(RationaleZahl.von(2))),r.ergebnis)
        assertEquals(emptySet(),r.bedingungen)
        kotlin.test.assertFails {m.wendeMitIdentitätenAn(mapOf("qy" to RationaleZahl.Null))}
    }
    @Test fun `Partielle Bindung verwendet Komponentenidentitaet trotz geaenderter Reihenfolge`() {
        val x=Variable("x",bindungsId="qx");val y=Variable("y",bindungsId="qy")
        val m=MathematischeMethode("d",listOf(x,y),Tupel(listOf(addition(x,multiplikation(RationaleZahl.von(-1),y)))),Tupelraum(listOf(ReelleZahlen)),mapOf("x" to ReelleZahlen,"y" to ReelleZahlen))
        val ids=m.signatur.argumente.map {it.id}
        val ports=listOf(AnschlussDaten(name="methode",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=AnschlussArtId("mathematik.methode")),
            AnschlussDaten(name="zweiter",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=AnschlussArtId("mathematik.zahl"),reihenfolge=0,semantischeId=ids[1]),
            AnschlussDaten(name="erster",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=AnschlussArtId("mathematik.zahl"),reihenfolge=1,semantischeId=ids[0]))
        val k=KnotenDaten(art=METHODEN_AUFRUF_ART,name="partiell",anschlüsse=ports,parameter=mapOf(METHODEN_AUFRUF_PARTIELL to "true"))
        val e=MethodenAufrufAuswerter.auswerten(KnotenAuswertungsKontext(k,mapOf("methode" to BedingterWert(m),"zweiter" to BedingterWert(RationaleZahl.von(2))),RechenKontext()))
        val teil=e.ausgaben.getValue("wert").objekt as MathematischeMethode
        assertEquals(listOf(ids[0]),teil.signatur.argumente.map {it.id})
        assertEquals(Tupel(listOf(RationaleZahl.von(3))),teil.wendeMitIdentitätenAn(mapOf(ids[0] to RationaleZahl.von(5))).ergebnis)
    }

    @Test
    fun `Signaturknoten trennen Wertevorrat Zielmenge und Argumentanzahl`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to KomplexeZahlen, y.name to KomplexeZahlen),
        )
        val eingänge = mapOf("methode" to BedingterWert(methode))

        val wertevorrat = MethodenWertevorratAuswerter.auswerten(
            KnotenAuswertungsKontext(KnotenDaten(art = METHODEN_WERTEVORRAT_ART, name = "Wertevorrat"), eingänge, RechenKontext()),
        ).ausgaben.getValue("menge").objekt
        val zielmenge = MethodenZielmengeSignaturAuswerter.auswerten(
            KnotenAuswertungsKontext(KnotenDaten(art = METHODEN_ZIELMENGE_ART, name = "Zielmenge"), eingänge, RechenKontext()),
        ).ausgaben.getValue("menge").objekt
        val argumentanzahl = MethodenArgumentanzahlAuswerter.auswerten(
            KnotenAuswertungsKontext(KnotenDaten(art = METHODEN_ARGUMENTANZAHL_ART, name = "Argumentanzahl"), eingänge, RechenKontext()),
        ).ausgaben.getValue("anzahl").objekt

        assertEquals(Tupelraum(listOf(KomplexeZahlen, KomplexeZahlen)), wertevorrat)
        assertEquals(ReelleZahlen, zielmenge)
        assertEquals(RationaleZahl.von(2), argumentanzahl)
    }

    @Test
    fun `Tupelprojektion entpackt mehrstellige Methode positionsgetreu`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to Division(x, y)),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val knoten = tupelAufrufKnoten()

        val ergebnis = MethodenAufrufAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode),
                    "argument-0" to BedingterWert(Tupel(listOf(RationaleZahl.von(2), RationaleZahl.von(4)))),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(RationaleZahl.von(1, 2), ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals("f\\left(2,4\\right)", ergebnis.ausgaben.getValue("wert").latexDarstellung)
    }

    @Test
    fun `Tupelprojektion akzeptiert echtes Einertupel`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to addition(x, RationaleZahl.Eins)),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        val ergebnis = MethodenAufrufAuswerter.auswerten(
            KnotenAuswertungsKontext(
                tupelAufrufKnoten(),
                mapOf(
                    "methode" to BedingterWert(methode),
                    "argument-0" to BedingterWert(Tupel(listOf(RationaleZahl.von(4)))),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(RationaleZahl.von(5), ergebnis.ausgaben.getValue("wert").objekt)
    }

    @Test
    fun `Tupelprojektion verpackt Skalar nicht still`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertFailsWith<IllegalStateException> {
            MethodenAufrufAuswerter.auswerten(
                KnotenAuswertungsKontext(
                    tupelAufrufKnoten(),
                    mapOf(
                        "methode" to BedingterWert(methode),
                        "argument-0" to BedingterWert(RationaleZahl.von(4)),
                    ),
                    RechenKontext(),
                ),
            )
        }
    }

    @Test
    fun `Tupelprojektion diagnostiziert falsche Tupellaenge`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            MethodenAufrufAuswerter.auswerten(
                KnotenAuswertungsKontext(
                    tupelAufrufKnoten(),
                    mapOf(
                        "methode" to BedingterWert(methode),
                        "argument-0" to BedingterWert(Tupel(listOf(RationaleZahl.von(4)))),
                    ),
                    RechenKontext(),
                ),
            )
        }
        assertIs<String>(fehler.message)
    }

    @Test
    fun `ID basierte Zielraeume und Methodenwertannahmen werden substituiert`() {
        val x = Variable("x", bindungsId = "quelle-x")
        val ziel = ReellesIntervall(RationaleZahl.Null, false, x, false)
        val methode = MathematischeMethode("f", listOf(x), x, ziel, mapOf("x" to ReelleZahlen))
        fun aufruf(wert: RationaleZahl, annahmen: Set<Aussage> = emptySet()) = MethodenAufrufAuswerter.auswerten(
            KnotenAuswertungsKontext(tupelAufrufKnoten(), mapOf(
                "methode" to BedingterWert(methode, annahmen = annahmen),
                "argument-0" to BedingterWert(Tupel(listOf(wert))),
            ), RechenKontext()),
        ).ausgaben.getValue("wert")
        assertEquals(ReellesIntervall(RationaleZahl.Null, false, RationaleZahl.von(2), false), aufruf(RationaleZahl.von(2)).zielMenge)
        assertFailsWith<IllegalArgumentException> { aufruf(RationaleZahl.Null, setOf(Ungleichheit(x, RationaleZahl.Null))) }
    }

    private fun tupelAufrufKnoten() = KnotenDaten(
        art = METHODEN_AUFRUF_ART,
        name = "Methode aufrufen",
        parameter = mapOf(
            METHODEN_ANWENDUNG_ERGEBNIS_ART to "mathematik.zahl",
            METHODEN_AUFRUF_ARGUMENTPROJEKTION to METHODEN_ARGUMENTPROJEKTION_TUPEL,
        ),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "methode",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = AnschlussArtId("mathematik.methode"),
                reihenfolge = 0,
            ),
            AnschlussDaten(
                name = "argument-0",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = AnschlussArtId("mathematik.tupel"),
                reihenfolge = 1,
            ),
            AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = AnschlussArtId("mathematik.zahl"),
            ),
        ),
    )
}

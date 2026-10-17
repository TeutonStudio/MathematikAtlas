package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MethodenKonsolidierungVertragTest {
    private fun port(name: String, id: String? = null, reihe: Int = 0) = AnschlussDaten(
        name = name, richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
        art = AnschlussArtId("mathematik.objekt"), semantischeId = id, reihenfolge = reihe)
    private fun aufruf(m: Methode, ports: List<AnschlussDaten>, werte: Map<String, BedingterWert>, parameter: Map<String,String> = emptyMap()) =
        MethodenAufrufAuswerter.auswerten(KnotenAuswertungsKontext(
            KnotenDaten(art = METHODEN_AUFRUF_ART, name = "Aufruf", anschlüsse = listOf(port("methode")) + ports, parameter = parameter),
            werte + ("methode" to BedingterWert(m)), RechenKontext())).ausgaben.getValue("wert")
    private fun z(n: Long) = RationaleZahl.von(n)
    @Test fun `vollstaendiger Aufruf bindet nach ID trotz Layout und Namen`() {
        val x = Variable("x", bindungsId = "qx"); val y = Variable("y", bindungsId = "qy")
        val m = MathematischeMethode("d", listOf(x,y), subtraktion(x,y), ReelleZahlen, mapOf("x" to ReelleZahlen,"y" to ReelleZahlen))
        val werte = mapOf("rechts" to BedingterWert(z(2)), "links" to BedingterWert(z(5)))
        val ports = listOf(port("rechts","qy",0), port("links","qx",1))
        assertEquals(z(3), aufruf(m, ports, werte).objekt)
        assertEquals(z(3), aufruf(m.copy(parameter = m.parameter.reversed()), ports, werte).objekt)
        for (ids in listOf(listOf("qx","qx"), listOf("qx","fremd"))) assertFails {
            aufruf(m, listOf(port("links",ids[0]),port("rechts",ids[1],1)),werte)
        }
    }
    @Test fun `F mit Tupel und Methode besitzt zwei logische Argumente`() {
        val typ = ReelleZahlen.elementTypAusdruck()
        val sig = MethodenSignatur(listOf(MethodenKomponente("t","t",0,typ)), listOf(MethodenKomponente("r","r",0,typ)))
        val g = MethodenVariable("g","qg",sig)
        val a = AllgemeinerParameter("a",bindungsId="qa")
        val raum = Tupelraum(listOf(ReelleZahlen,ReelleZahlen))
        val m = MathematischeMethode("F",listOf(a,g),a,Tupelraum(listOf(raum)),
            mapOf("a" to raum,"g" to Abbildungsmenge(Tupelraum(listOf(ReelleZahlen)),Tupelraum(listOf(ReelleZahlen)))))
        val t = Variable("t"); val konkret = MathematischeMethode("g",listOf(t),t,ReelleZahlen,mapOf("t" to ReelleZahlen))
        val tupel = Tupel(listOf(z(2),z(3)))
        assertEquals(tupel, aufruf(m,listOf(port("funktion","qg"),port("paar","qa",1)),mapOf("paar" to BedingterWert(tupel),"funktion" to BedingterWert(konkret))).objekt)
    }
    @Test fun `Komponentenprojektion behaelt passende Raeume und Zeugnis nur als Herkunft`() {
        val teil = Tupel(listOf(z(2),z(3))); val raum = Tupelraum(listOf(ReelleZahlen,GanzeZahlen))
        val übersetzt = assertIs<ÜbersetzungsErgebnis.Erfolg>(ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(Tupel(listOf(z(1))),ÜbersetzungsZiel.SPALTE)))
        val wert = BedingterWert(Tupel(listOf(teil)), zielMenge=Tupelraum(listOf(raum)),
            werteVorrat=Tupelraum(listOf(raum)), komponentenMengen=listOf(raum),
            elementArt=AnschlussArtId("mathematik.zahl"),latexDarstellung="gesamt",
            übersetzungen=ÜbersetzungsMetadaten(aktuell=übersetzt.zeugnis,kontext=übersetzt.kontext))
        val r = aufruf(IdentitaetsMethode,listOf(port("tupel")),mapOf("tupel" to wert),
            mapOf(METHODEN_AUFRUF_ARGUMENTPROJEKTION to METHODEN_ARGUMENTPROJEKTION_TUPEL))
        assertEquals(teil,r.objekt); assertEquals(raum,r.zielMenge); assertEquals(raum,r.werteVorrat)
        assertNull(r.komponentenMengen);assertNull(r.elementArt);assertNull(r.latexDarstellung)
        assertNull(r.übersetzungen.aktuell);assertEquals(listOf(übersetzt.zeugnis),r.übersetzungen.herkunft)
    }
    @Test fun `signierter symbolischer Aufruf ist substituierbarer AST`() {
        val typ = ReelleZahlen.elementTypAusdruck()
        val g = MethodenVariable("g","quelle-g",MethodenSignatur(listOf(MethodenKomponente("a","a",0,typ)),listOf(MethodenKomponente("r","r",0,typ))))
        val ast = assertIs<MethodenZahlKomponente>(aufruf(g,listOf(port("a","a")),mapOf("a" to BedingterWert(z(3)))).objekt)
        assertSame(g,ast.aufruf.methode)
        val x = Variable("x");val konkret = MathematischeMethode("g",listOf(x),Potenz(x,z(2)),ReelleZahlen,mapOf("x" to ReelleZahlen))
        assertEquals(z(9),vereinfache(ersetze(ast,mapOf("quelle-g" to konkret)) as ZahlAusdruck))
        assertNotEquals(ast,aufruf(g.copy(bindungsId="andere-quelle"),listOf(port("a","a")),mapOf("a" to BedingterWert(z(3)))).objekt)
    }
    @Test fun `symbolische nicht unterstuetzte Ergebnistypen werden diagnostiziert`() {
        for (typ in listOf(WahrheitsMenge.elementTypAusdruck(), de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Menge))) {
            val g = MethodenVariable("g","qg",MethodenSignatur(emptyList(),listOf(MethodenKomponente("r","r",0,typ))))
            assertTrue(assertFails { aufruf(g,emptyList(),emptyMap()) }.message.orEmpty().contains("nicht unterstützt"))
        }
    }
    @Test fun `direkte Projektion lehnt null und mehrere Ergebnisse ab und erhaelt inneres Tupel`() {
        val innen = Tupel(listOf(z(1),z(2)))
        val m = Methode("f",emptyList(),innen,Tupelraum(listOf(ReelleZahlen,ReelleZahlen)))
        assertEquals(innen,aufruf(m,emptyList(),emptyMap(),mapOf(METHODEN_AUFRUF_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_DIREKT)).objekt)
        for (n in listOf(0,2)) {
            val multi = MathematischeMethode("f",emptyList(),Tupel(List(n){z(1)}),Tupelraum(List(n){ReelleZahlen}),ausgabeNamen=List(n){"r$it"})
            assertFails { aufruf(multi,emptyList(),emptyMap(),mapOf(METHODEN_AUFRUF_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_DIREKT)) }
            assertEquals(Tupel(List(n){z(1)}),aufruf(multi,emptyList(),emptyMap()).objekt)
        }
    }
    @Test fun `offene Voraussetzungen bleiben und widerlegte verhindern den Aufruf`() {
        val x=Variable("x",bindungsId="qx")
        val m=MathematischeMethode("f",listOf(x),x,ReelleZahlen,mapOf("x" to ReelleZahlen),voraussetzungen=setOf(Ungleichheit(x,z(0))))
        val u=Variable("u")
        assertTrue(Ungleichheit(u,z(0)) in aufruf(m,listOf(port("a","qx")),mapOf("a" to BedingterWert(u))).annahmen)
        assertFails { aufruf(m,listOf(port("a","qx")),mapOf("a" to BedingterWert(z(0)))) }
    }
    @Test fun `offene Mathematik Capability wird ausgefuehrt ohne konkreten Cast`() {
        val x=Variable("x",bindungsId="qx")
        val m=object : MathematischAuswertbareMethode {
            override val name="cap"
            override val mathematischeSignatur=MathematischeMethodenSignatur(listOf(MathematischeArgumentKomponente("qx","x",0,x,ReelleZahlen)),listOf(MathematischeErgebnisKomponente("r","r",0,ReelleZahlen)))
            override fun wendeMathematischAn(argumente:Map<String,MathematischesObjekt>):MathematischesObjekt = addition(argumente.getValue("x") as ZahlAusdruck,z(1))
            override fun zuLatex()="cap"
        }
        assertEquals(z(4),aufruf(m,listOf(port("a","qx")),mapOf("a" to BedingterWert(z(3)))).objekt)
    }
    @Test fun `ungueltige direkte Projektion fuehrt allgemeine Capability nicht aus`() {
        var ausgeführt = false
        val m = object : AllgemeinAuswertbareMethode {
            override val name = "allgemein"
            override val signatur = MethodenSignatur(emptyList(), emptyList())
            override fun rufeAuf(argumente: Map<String, AtlasWert>): Map<String, MethodenAufrufErgebnis> {
                ausgeführt = true
                return emptyMap()
            }
        }
        assertFails { aufruf(m, emptyList(), emptyMap(), mapOf(METHODEN_AUFRUF_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_DIREKT)) }
        assertFalse(ausgeführt)
    }
    @Test fun `gespeicherter Einzelresultatvertrag wird vor Anwendung geprueft`() {
        val m = MathematischeMethode("f", emptyList(), z(1), ReelleZahlen)
        assertEquals(z(1), aufruf(m, emptyList(), emptyMap(), mapOf(METHODEN_AUFRUF_PROJEKTION_FEST to "true", METHODEN_ANWENDUNG_ERGEBNIS_ART to "mathematik.zahl")).objekt)
        assertFails { aufruf(m, emptyList(), emptyMap(), mapOf(METHODEN_AUFRUF_PROJEKTION_FEST to "true", METHODEN_ANWENDUNG_ERGEBNIS_ART to "mathematik.menge")) }
    }

}

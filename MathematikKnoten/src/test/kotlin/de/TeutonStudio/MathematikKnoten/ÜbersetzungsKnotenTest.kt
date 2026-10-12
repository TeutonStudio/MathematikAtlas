package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
import kotlin.test.*

class ÜbersetzungsKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun auswerten(k: KnotenDaten, wert: BedingterWert, name: String = "wert") =
        register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k, mapOf(name to wert), RechenKontext()))

    @Test fun `beide Typen registriert und Übersetzer unvermehrbar mit stabilen IDs`() {
        assertTrue(KanonischerMathematikKnotenKatalog.alle().any { it.art == ÜBERSETZER_ART })
        assertTrue(KanonischerMathematikKnotenKatalog.alle().any { it.art == TUPEL_ABFLACHEN_ART })
        val k = ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        assertEquals("spalte", übersetzerZiel(k))
        assertEquals(listOf("wert", "ergebnis"), k.anschlüsse.map { it.name })
        ÜBERSETZER_ZIELE.forEach { ziel ->
            val neu = konfiguriereÜbersetzer(k, ziel)
            assertEquals(k.anschlüsse.map { it.id }, neu.anschlüsse.filter { it.name in setOf("wert", "ergebnis") }.map { it.id })
            assertTrue(neu.anschlüsse.none { it.kannSichErweitern })
        }
    }

    @Test fun `symbolische Komponenten werden orientiert ohne Konjugation`() {
        val werte = listOf<ZahlAusdruck>(Variable("x"), KomplexeZahl(z(2), z(3)))
        assertEquals(SpaltenVektor(werte), übersetzeStruktur(Tupel(werte), "spalte"))
        assertEquals(ZeilenVektor(werte), übersetzeStruktur(SpaltenVektor(werte), "zeile"))
        assertEquals(Tupel(werte), übersetzeStruktur(ZeilenVektor(werte), "tupel"))
        listOf(Tupel(emptyList()), Tupel(listOf(Tupel(listOf(z(1))))), Tupel(listOf(WahrheitsKonstante(true))),
            Tupel(listOf(EndlicheMenge(setOf(z(1)))))).forEach { assertFails { übersetzeStruktur(it, "spalte") } }
    }

    @Test fun `Abflachen besucht nur Tupel und erhaelt Annahmen Quellen und heterogene Blaetter`() {
        val menge = EndlicheMenge(setOf(z(1)))
        val vektor = SpaltenVektor(listOf(z(2)))
        val t = Tupel(listOf(Tupel(emptyList()), z(1), Tupel(listOf(menge, Tupel(listOf(vektor, WahrheitsKonstante(true)))))))
        val quelle = VariablenQuelle(KnotenId("q"), "x", ReelleZahlen)
        val wert = BedingterWert(t, setOf(Gleichheit(z(1), z(1))), variablenQuellen = listOf(quelle))
        val k = ÜbersetzungsKnotenVorlagen.Abflachen.erzeuge(GraphPunkt.Zero)
        val ergebnis = auswerten(k, wert, "tupel").ausgaben.getValue("tupel")
        assertEquals(Tupel(listOf(z(1), menge, vektor, WahrheitsKonstante(true))), ergebnis.objekt)
        assertEquals(wert.annahmen, ergebnis.annahmen)
        assertEquals(listOf(quelle), ergebnis.variablenQuellen)
    }

    @Test fun `Methodenbildung bindet nur freie markierte Quellen und erhaelt Aussenvariablen`() {
        val k = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "methode")
        val x = Variable("x"); val y = Variable("y")
        val annahme = Gleichheit(x, y)
        val wert = BedingterWert(addition(x, y), setOf(annahme), reelleVariablen = mapOf("x" to ReelleZahlen, "y" to ReelleZahlen),
            variablenQuellen = listOf(VariablenQuelle(KnotenId("x"), "x", ReelleZahlen),
                VariablenQuelle(KnotenId("y"), "y", ReelleZahlen, alsMethodenParameter = false),
                VariablenQuelle(KnotenId("z"), "z", ReelleZahlen)))
        val ergebnis = auswerten(k, wert).ausgaben.getValue("ergebnis")
        val methode = assertIs<MathematischeMethode>(ergebnis.objekt)
        assertEquals(listOf("x"), methode.parameter.map { it.name })
        assertEquals(listOf("y", "z"), ergebnis.variablenQuellen.map { it.name })
        assertEquals(mapOf("y" to ReelleZahlen), ergebnis.reelleVariablen)
        assertEquals(Tupel(listOf(addition(z(2), y))), methode.wendeMathematischAlsTupelAn(Tupel(listOf(z(2)))))
        val aufruf = KnotenDaten(art = "mathematik.methodeAufrufen", name = "Aufruf", anschlüsse = listOf(
            AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Methode.id),
            AnschlussDaten(name = "argument1", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id)))
        val angewandt = register.finde(aufruf.art)!!.auswerten(KnotenAuswertungsKontext(aufruf,
            mapOf("methode" to ergebnis, "argument1" to BedingterWert(z(2))), RechenKontext())).ausgaben.getValue("wert")
        assertEquals(setOf(Gleichheit(z(2), y)), angewandt.annahmen)
        assertEquals(mapOf("y" to ReelleZahlen), angewandt.reelleVariablen)
    }

    @Test fun `nullstellige und tupelwertige Methode bleiben kanonisch`() {
        val k = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "methode")
        val konstante = assertIs<MathematischeMethode>(auswerten(k, BedingterWert(z(7))).ausgaben.getValue("ergebnis").objekt)
        assertEquals(Tupel(listOf(z(7))), konstante.wendeMathematischAlsTupelAn(Tupel(emptyList())))
        assertEquals(Tupelraum(emptyList()), konstante.definitionsRaum())
        val tuple = Tupel(listOf(z(1), z(2)))
        val methode = assertIs<MathematischeMethode>(auswerten(k, BedingterWert(tuple)).ausgaben.getValue("ergebnis").objekt)
        assertEquals(tuple, methode.wendeMathematischAlsTupelAn(Tupel(emptyList())))
    }

    @Test fun `JSON bewahrt neue Typen Konfiguration und Portreferenzen`() {
        val k = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "zeile")
        val f = ÜbersetzungsKnotenVorlagen.Abflachen.erzeuge(GraphPunkt.Zero)
        val karte = KartenDaten(name = "Neu", knoten = listOf(k, f))
        assertEquals(karte.knoten, MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte)).knoten)
    }
    @Test fun `fehlende und widerspruechliche Herkunft wird auch historisch diagnostiziert`() {
        val neu = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "methode")
        val alt = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero)
        listOf(neu to "wert", alt to "term").forEach { (k,port) ->
            assertFails { auswerten(k,BedingterWert(Variable("x")),port) }
            val quellen = listOf(VariablenQuelle(KnotenId("a"),"x",ReelleZahlen),
                VariablenQuelle(KnotenId("b"),"x",GanzeZahlen))
            assertFails { auswerten(k,BedingterWert(Variable("x"),variablenQuellen=quellen),port) }
        }
    }

    @Test fun `leere und einzelne Tupelergebnisse erhalten ihre aeussere Struktur`() {
        val k = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), "methode")
        listOf(Tupel(emptyList()),Tupel(listOf(z(1)))).forEach { tupel ->
            val methode = assertIs<MathematischeMethode>(auswerten(k,BedingterWert(tupel)).ausgaben.getValue("ergebnis").objekt)
            assertEquals(tupel,methode.wendeMathematischAlsTupelAn(Tupel(emptyList())))
        }
        assertEquals(Tupel(emptyList()),tupelAbflachen(Tupel(listOf(Tupel(emptyList()),Tupel(listOf(Tupel(emptyList())))))))
        assertFails { übersetzeStruktur(SpaltenVektor(listOf(z(1))),"spalte") }
        assertFails { übersetzeStruktur(ZeilenVektor(listOf(z(1))),"zeile") }
    }

    @Test fun `Strukturwechsel erhaelt gelieferte Obermengen und passt ihre Struktur an`() {
        val tupel=Tupel(listOf(z(1),z(2)))
        val k=ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero)
        val quelle=BedingterWert(tupel,zielMenge=Tupelraum(listOf(ReelleZahlen,ReelleZahlen)),werteVorrat=ReelleZahlen)
        val spalte=auswerten(k,quelle).ausgaben.getValue("ergebnis")
        assertEquals(Vektorraum(VektorOrientierung.Spalte, 2, ReelleZahlen),spalte.zielMenge)
        assertNull(spalte.werteVorrat)
        val zeile=auswerten(k,BedingterWert(ZeilenVektor(listOf(z(1),z(2))),zielMenge=quelle.zielMenge,werteVorrat=ReelleZahlen)).ausgaben.getValue("ergebnis")
        assertEquals(spalte.zielMenge,zeile.zielMenge)
        val f=ÜbersetzungsKnotenVorlagen.Abflachen.erzeuge(GraphPunkt.Zero)
        val flach=auswerten(f,quelle.copy(objekt=Tupel(listOf(tupel))),"tupel").ausgaben.getValue("tupel")
        assertEquals(inferiereZielmenge(tupel),flach.zielMenge)
        assertNull(flach.werteVorrat)
    }

    @Test fun `heterogene Tupelports erhalten gespeicherte numerische Standardwerte vor und nach Laden`() {
        val basis=MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero)
        val k=konfiguriereTupel(basis.copy(parameter=basis.parameter+mapOf("standardwert.a" to "2","standardwert.b" to "3")),TUPEL_EINZEL_EINGABEN)
        assertTrue(k.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.all { it.art==MathematikAnschlussArten.Objekt.id && it.zulässigeArten.isEmpty() })
        val karte=KartenDaten(name="Defaults",knoten=listOf(k))
        listOf(karte,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte))).forEach { c ->
            val ergebnis=KartenAuswerter(register).auswerten(c)
            assertTrue(ergebnis.fehler.isEmpty(),ergebnis.fehler.joinToString())
            assertEquals(Tupel(listOf(z(2),z(3))),ergebnis.knoten.getValue(k.id).ausgaben.getValue("tupel").objekt)
        }
    }

}

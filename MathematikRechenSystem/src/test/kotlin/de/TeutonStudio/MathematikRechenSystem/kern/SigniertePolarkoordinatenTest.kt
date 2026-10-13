package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.*
import kotlin.math.abs
import kotlin.random.Random

class SigniertePolarkoordinatenTest {
    private fun z(x:Double)=rationaleKoordinate(x)
    private fun werte(t:Tupel)=t.elemente.map { ((DomaenenAuswerter.wert(it as ZahlAusdruck) as DomaenenErgebnis.Wert).wert as DomaenenWert.Reell).wert.toDouble() }
    @Test fun `Signierte Inverse deckt alle Quadranten Pole und hohe Dimensionen ab`() {
        val random=Random(231)
        val faelle=listOf(listOf(-2.0),listOf(0.0),listOf(2.0,-3.0),listOf(-2.0,3.0),listOf(0.0,0.0,-2.0),listOf(0.0,0.0,2.0),listOf(2.0,-3.0,4.0))+
            listOf(1,2,3,4,8,65).flatMap { n -> List(4) { List(n) { random.nextDouble(-4.0,4.0) } } }
        for(x in faelle) {
            val polar=Polarkoordinaten.polar(Tupel(x.map(::z)),PolarKonvention.SIGNED)
            val richtung=assertIs<Tupel>(polar.elemente[1])
            assertTrue(richtung.elemente.drop(1).all { it is GradWinkel })
            val normal=normalisiereWinkelraumArgument(richtung,winkelraum(x.size-1))
            assertEquals(Wahrheitswert.Wahr,ElementBeziehung(normal,winkelraum(x.size-1)).entscheide().wahrheitswert)
            x.zip(werte(Polarkoordinaten.kartesisch(polar,PolarKonvention.SIGNED))).forEach { (a,b) -> assertTrue(abs(a-b)<1e-9,"$x: $a statt $b") }
        }
    }
    @Test fun `Wn bindet typisierte Winkel in Listen Maps und Tupelaufrufen ohne Unitverlust`() {
        val w=TypisiertesElement("w","mathematik.tupel",strukturForm=listOf(RationaleZahl.von(2)))
        val methode=MathematischeMethode("winkel",listOf(w),SymbolischeZahlKomponente(w,RationaleZahl.von(2)),ReelleZahlen,mapOf("w" to winkelraum(1)))
        val argument=Tupel(listOf(RationaleZahl.von(-1),GradWinkel(RationaleZahl.von(90))))
        assertEquals(RationaleZahl.von(90),methode.wendeAn(listOf(argument)))
        assertEquals(RationaleZahl.von(90),methode.wendeMathematischAn(mapOf("w" to argument)))
        assertEquals(Tupel(listOf(RationaleZahl.von(90))),methode.wendeMathematischAlsTupelAn(Tupel(listOf(argument))))
        for(ungueltig in listOf(Tupel(listOf(RationaleZahl.Null,RationaleZahl.Null)),Tupel(listOf(RationaleZahl.Eins,GradWinkel(RationaleZahl.von(180)))))) assertFails { methode.wendeAn(listOf(ungueltig)) }
    }
    @Test fun `Nullrichtung ist kanonisch und ungueltige Signierte Werte werden abgewiesen`() {
        val polar=Polarkoordinaten.polar(Tupel(List(3) { RationaleZahl.Null }),PolarKonvention.SIGNED)
        assertEquals(RationaleZahl.Eins,(polar.elemente[1] as Tupel).elemente[0])
        for(sign in listOf(0L,2L)) assertFails { Polarkoordinaten.kartesischAusRichtung(RationaleZahl.Null,Tupel(listOf(RationaleZahl.von(sign)))) }
        assertFails { Polarkoordinaten.kartesischAusRichtung(RationaleZahl.von(-1),Tupel(listOf(RationaleZahl.Eins))) }
        assertFails { Polarkoordinaten.kartesischAusRichtung(RationaleZahl.Eins,Tupel(listOf(RationaleZahl.Eins,RationaleZahl.von(180)))) }
    }
}

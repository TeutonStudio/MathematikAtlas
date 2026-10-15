package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeTypen
import de.TeutonStudio.TypSystem.*
import kotlin.test.*

class MathematikKartenTypSystemTest {
    private val system=erzeugeMathematikKartenTypSystem(AnschlussArtRegister(MathematikAnschlussArten.alle))
    private fun tupel(n: Int)=TypAusdruck.Parameterisiert(MathematischeTypen.Tupel,List(n) { TypAusdruck.Atom(MathematischeTypen.Zahl) })
    @Test fun `strukturierte Tupel passen an grobe Tupel und Objektanschluesse`() {
        for(n in listOf(0,1,2,4)) for(art in listOf(MathematikAnschlussArten.Tupel,MathematikAnschlussArten.Objekt)) {
            assertEquals(TypPrüfung.Kompatibel,system.prüfe(tupel(n),TypAusdruck.Atom(TypId(art.id.wert))))
        }
    }
    @Test fun `Tupelbruecke erlaubt weder falsche Laenge noch Zahleneingaenge`() {
        assertIs<TypPrüfung.Inkompatibel>(system.prüfe(tupel(2),tupel(3)))
        assertIs<TypPrüfung.Inkompatibel>(system.prüfe(tupel(2),TypAusdruck.Atom(TypId(MathematikAnschlussArten.Zahl.id.wert))))
        val aussage=TypAusdruck.Parameterisiert(MathematischeTypen.Tupel,List(2) { TypAusdruck.Atom(MathematischeTypen.Aussage) })
        assertIs<TypPrüfung.Inkompatibel>(system.prüfe(aussage,tupel(2)))
    }
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import org.junit.Assert.*
import org.junit.Test

class MathematikTypSystemTest {
    private val arten = AnschlussArtRegister(MathematikAnschlussArten.alle)
    private val typen = mathematikTypSystem(arten)
    private val resolver = MathematikTypResolver(typen)

    @Test
    fun fundamentale_zahlbereiche_bilden_die_mathematische_untertyprelation_ab() {
        assertEquals(
            TypPrüfung.Kompatibel,
            typen.prüfe(TypAusdruck.Atom(MathematikTypen.N), TypAusdruck.Atom(MathematikTypen.R)),
        )
        assertEquals(
            TypPrüfung.Kompatibel,
            typen.prüfe(TypAusdruck.Atom(MathematikTypen.R), TypAusdruck.Atom(MathematikTypen.C)),
        )
        assertTrue(
            typen.prüfe(TypAusdruck.Atom(MathematikTypen.C), TypAusdruck.Atom(MathematikTypen.R))
                is TypPrüfung.Inkompatibel,
        )
    }

    @Test
    fun vektorraum_wird_aus_menge_dimension_und_orientierung_parametrisiert() {
        val raum = Vektorraum(VektorOrientierung.Spalte, 3, ReelleZahlen)
        assertEquals(
            TypAusdruck.Parameterisiert(
                MathematikTypen.SpaltenVektor,
                listOf(
                    TypAusdruck.Atom(MathematikTypen.R),
                    TypAusdruck.Atom(MathematikTypen.dimension(3)),
                ),
            ),
            resolver.elementTyp(raum),
        )
    }

    @Test
    fun matrizenraum_bewahrt_skalarraum_und_beide_dimensionen() {
        val raum = Matrizenraum(2, 4, KomplexeZahlen)
        assertEquals(
            TypAusdruck.Parameterisiert(
                MathematikTypen.Matrix,
                listOf(
                    TypAusdruck.Atom(MathematikTypen.C),
                    TypAusdruck.Atom(MathematikTypen.dimension(2)),
                    TypAusdruck.Atom(MathematikTypen.dimension(4)),
                ),
            ),
            resolver.elementTyp(raum),
        )
    }

    @Test
    fun abbildungsmenge_wird_als_methodentyp_aufgeloest() {
        val raum = Abbildungsmenge(
            zielMenge = ReelleZahlen,
            definitionsMenge = KomplexeZahlen,
        )
        assertEquals(
            TypAusdruck.Parameterisiert(
                MathematikTypen.Methode,
                listOf(
                    TypAusdruck.Atom(MathematikTypen.C),
                    TypAusdruck.Atom(MathematikTypen.R),
                ),
            ),
            resolver.elementTyp(raum),
        )
    }

    @Test
    fun potenzmenge_erzeugt_einen_mengenelementtyp() {
        assertEquals(
            TypAusdruck.Parameterisiert(
                MathematikTypen.Menge,
                listOf(TypAusdruck.Atom(MathematikTypen.R)),
            ),
            resolver.elementTyp(Potenzmenge(ReelleZahlen)),
        )
    }
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.katalog.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.*

class AnschlussVariantenTest {
    @Test fun vollstaendigeEnumerationEnthaeltOperatorenUndKeineVariantenDubletten() {
        val basis = KanonischerMathematikKnotenKatalog.alle()
        val varianten = KanonischerMathematikKnotenKatalog.anschlussVarianten(basis)
        assertTrue(varianten.size > basis.size)
        assertEquals(varianten.size, varianten.map(KanonischerMathematikKnotenKatalog::variantenKennung).distinct().size)
        OperatorKnotenSuchindex.alleMitMetadaten(basis).forEach { op ->
            assertTrue(varianten.any { it.art == op.vorlage.art && it.standardParameter["operator"] == op.vorlage.standardParameter["operator"] }, op.vorlage.name)
        }
        varianten.forEach { v ->
            val a = v.erzeuge(GraphPunkt.Zero); val b = v.erzeuge(GraphPunkt.Zero)
            assertNotEquals(a.id, b.id)
            assertTrue(a.anschlüsse.map { it.id }.intersect(b.anschlüsse.map { it.id }.toSet()).isEmpty())
        }
    }
    @Test fun variantenKennungenSindUnabhaengigVonInstanzIdsUndEnumeration() {
        val erste = KanonischerMathematikKnotenKatalog.anschlussVarianten().map(KanonischerMathematikKnotenKatalog::variantenKennung)
        val zweite = KanonischerMathematikKnotenKatalog.anschlussVarianten().map(KanonischerMathematikKnotenKatalog::variantenKennung)
        assertEquals(erste, zweite)
    }
    @Test fun uebersetzerEnthaltenAlleEndlichenEingabeUndAusgabeOptionen() {
        val alle = KanonischerMathematikKnotenKatalog.anschlussVarianten().filter { it.art == ÜBERSETZER_ART }
        fun für(ziel: String) = alle.filter { (it.standardEigenschaften[ÜBERSETZER_ZIEL] as? KnotenEigenschaft.Text)?.wert == ziel }
        val kartesisch = für("kartesisch")
        assertEquals(setOf("false", "true"), kartesisch.map { it.standardParameter[ÜBERSETZER_R1] }.toSet())
        assertEquals(de.TeutonStudio.MathematikRechenSystem.kern.PolarKonvention.entries.map { it.name }.toSet(),
            kartesisch.map { it.standardParameter[ÜBERSETZER_POLAR_KONVENTION] }.toSet())
        assertEquals(4, für("matrixzerlegen").map { it.standardParameter[MATRIX_ZERLEGEN_RICHTUNG] to it.standardParameter["matrixZerlegen.ausgabe"] }.distinct().size)
        assertEquals(setOf("keiner", "punkt", "mittelpunkt"), für("koordinatenmenge").map { it.standardParameter["geometrie.anker"] }.toSet())
        assertEquals(de.TeutonStudio.MathematikRechenSystem.kern.WinkelVertreter.entries.map { it.id }.toSet(), für("bogenmass").map { it.standardParameter[ÜBERSETZER_VERTRETER] }.toSet())
    }
    @Test fun erzeugungsModiUndOrientierungenHabenIhreEchtenAnschluesse() {
        val alle = KanonischerMathematikKnotenKatalog.anschlussVarianten()
        val vektoren = alle.filter { it.art == VEKTOR_KONSTRUKTOR_ART }
        assertEquals(setOf("spalte", "zeile"), vektoren.map { it.standardParameter[VEKTOR_ORIENTIERUNG_PARAMETER] }.toSet())
        assertEquals(setOf(VEKTOR_EINZEL_EINGABEN, VEKTOR_METHODE), vektoren.map { it.standardParameter[VEKTOR_ERZEUGUNGSART_PARAMETER] }.toSet())
        assertTrue(vektoren.filter { it.standardParameter[VEKTOR_ERZEUGUNGSART_PARAMETER] == VEKTOR_METHODE }
            .all { it.anschlüsse.any { p -> p.name == "methode" } })
        assertEquals(4, alle.filter { it.art == "mathematik.matrix" }.map { it.standardParameter["erzeugungsArt"] }.distinct().size)
    }
}

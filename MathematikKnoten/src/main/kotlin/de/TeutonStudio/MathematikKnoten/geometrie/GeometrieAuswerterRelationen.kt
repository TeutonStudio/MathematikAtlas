package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal fun MathematikAuswerterRegister.registriereGeometrieRelationen() {
    registriere("mathematik.geometrie.inzidenz") { k ->
        geometrieErgebnis("aussage", GeometrischeInzidenz(k.geometriePunkt("links"), k.geometrieObjekt("rechts")), k)
    }
    registriere("mathematik.geometrie.zwischen") { k ->
        geometrieErgebnis("aussage", Zwischenlage(k.geometriePunkt("a"), k.geometriePunkt("b"), k.geometriePunkt("c")), k)
    }
    registriere("mathematik.geometrie.kollinear") { k ->
        geometrieErgebnis("aussage", Kollinearität(listOf(
            k.geometriePunkt("a"), k.geometriePunkt("b"), k.geometriePunkt("c"),
        )), k)
    }
    registriere("mathematik.geometrie.parallel") { k ->
        geometrieErgebnis("aussage", GeometrischeParallelität(k.geometrieGerade("links"), k.geometrieGerade("rechts")), k)
    }
    registriere("mathematik.geometrie.orthogonal") { k ->
        geometrieErgebnis("aussage", GeometrischeOrthogonalität(k.geometrieGerade("links"), k.geometrieGerade("rechts")), k)
    }
    registriere("mathematik.geometrie.gleichheit") { k ->
        geometrieErgebnis("aussage", GeometrischeGleichheit(k.geometrieObjekt("links"), k.geometrieObjekt("rechts")), k)
    }
    registriere("mathematik.geometrie.streckenKongruenz") { k ->
        geometrieErgebnis("aussage", StreckenKongruenz(k.geometrieStrecke("links"), k.geometrieStrecke("rechts")), k)
    }
    registriere("mathematik.geometrie.winkelKongruenz") { k ->
        geometrieErgebnis("aussage", WinkelKongruenz(k.geometrieWinkel("links"), k.geometrieWinkel("rechts")), k)
    }
}

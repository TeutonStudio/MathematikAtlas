package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal fun MathematikAuswerterRegister.registriereGeometrieTransformationen() {
    registriere("mathematik.spalteZuTupel") { k ->
        geometrieErgebnis("tupel", k.geometrieSpalte("vektor").alsTupel(), k)
    }
    registriere("mathematik.zeileZuTupel") { k ->
        geometrieErgebnis("tupel", k.geometrieZeile("vektor").alsTupel(), k)
    }
    registriere("mathematik.geometrie.punktTransformationLinear") { k ->
        geometrieErgebnis("bild", transformierePunkt(k.geometrieTupel("punkt"), k.geometrieMatrix("matrix")), k)
    }
    registriere("mathematik.geometrie.punktTransformationAffin") { k ->
        val linear = k.geometrieMatrix("matrix") * k.geometrieTupel("punkt").alsSpaltenVektor()
        val translation = k.geometrieTupel("translation").alsSpaltenVektor()
        require(linear.werte.size == translation.werte.size)
        geometrieErgebnis("bild", Tupel(linear.werte.zip(translation.werte) { a, b -> vereinfache(addition(a, b)) }), k)
    }
    registriere("mathematik.geometrie.lineareTransformation") { k ->
        geometrieErgebnis("transformation", LineareGeometrieTransformation(
            k.geometrieMatrix("matrix"), k.geometrieRaum("quelle"), k.geometrieRaum("ziel"),
        ), k)
    }
    registriere("mathematik.geometrie.affineTransformation") { k ->
        geometrieErgebnis("transformation", AffineGeometrieTransformation(
            k.geometrieMatrix("matrix"),
            k.geometrieTupel("translation"),
            k.geometrieRaum("quelle"),
            k.geometrieRaum("ziel"),
        ), k)
    }
    registriere("mathematik.geometrie.transformieren") { k ->
        geometrieErgebnis("bild", transformiereGeometrie(
            k.geometrieObjekt("objekt"), k.geometrieTransformation("transformation"),
        ), k)
    }
}

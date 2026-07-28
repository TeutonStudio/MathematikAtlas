package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal fun geometrieErgebnis(name: String, objekt: MathematischesObjekt, k: KnotenAuswertungsKontext? = null) =
    KnotenAuswertungsErgebnis(mapOf(name to BedingterWert(objekt, k?.geometrieAnnahmen().orEmpty())))

internal fun KnotenAuswertungsKontext.geometrieAnnahmen(): Set<Aussage> = eingänge.values.flatMap { it.annahmen }.toSet()
internal fun KnotenAuswertungsKontext.geometrieRaum(name: String) = eingänge[name]?.objekt as? EuklidischerRaum ?: error("Raum $name fehlt.")
internal fun KnotenAuswertungsKontext.geometrieSystem(name: String) = eingänge[name]?.objekt as? GeometrischesKoordinatensystem ?: error("Koordinatensystem $name fehlt.")
internal fun KnotenAuswertungsKontext.geometrieTupel(name: String) = eingänge[name]?.objekt as? Tupel ?: error("Tupel $name fehlt.")
internal fun KnotenAuswertungsKontext.geometriePunkt(name: String) = eingänge[name]?.objekt as? GeometriePunkt ?: error("Punkt $name fehlt.")
internal fun KnotenAuswertungsKontext.geometrieGerade(name: String) = eingänge[name]?.objekt as? GeometrieGerade ?: error("Gerade $name fehlt.")
internal fun KnotenAuswertungsKontext.geometrieStrecke(name: String) = eingänge[name]?.objekt as? GeometrieStrecke ?: error("Strecke $name fehlt.")
internal fun KnotenAuswertungsKontext.geometrieWinkel(name: String) = eingänge[name]?.objekt as? GeometrieWinkel ?: error("Winkel $name fehlt.")
internal fun KnotenAuswertungsKontext.geometrieObjekt(name: String) = eingänge[name]?.objekt as? GeometrischerAusdruck ?: error("Geometrisches Objekt $name fehlt.")
internal fun KnotenAuswertungsKontext.geometrieTransformation(name: String) = eingänge[name]?.objekt as? GeometrischeTransformation ?: error("Transformation $name fehlt.")
internal fun KnotenAuswertungsKontext.geometrieSpalte(name: String) = eingänge[name]?.objekt as? SpaltenVektor ?: error("Spaltenvektor $name fehlt.")
internal fun KnotenAuswertungsKontext.geometrieZeile(name: String) = eingänge[name]?.objekt as? ZeilenVektor ?: error("Zeilenvektor $name fehlt.")
internal fun KnotenAuswertungsKontext.geometrieMatrix(name: String) = eingänge[name]?.objekt as? Matrix ?: error("Matrix $name fehlt.")

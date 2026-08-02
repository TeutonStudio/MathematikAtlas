package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val GEOMETRIE_TEILOBJEKT_ZELL_ID = "geometrieTeilobjekt.zellId"

enum class GeometrieTeilobjektTyp(
    val knotenArt: String,
    val dimension: Int,
    val ausgangName: String,
    val bezeichnung: String,
    val mehrzahl: String,
) {
    Ecke(
        knotenArt = "mathematik.geometrie.eckeVon",
        dimension = 0,
        ausgangName = "ecke",
        bezeichnung = "Ecke",
        mehrzahl = "Ecken",
    ),
    Kante(
        knotenArt = "mathematik.geometrie.kanteVon",
        dimension = 1,
        ausgangName = "kante",
        bezeichnung = "Kante",
        mehrzahl = "Kanten",
    ),
    Fläche(
        knotenArt = "mathematik.geometrie.flaecheVon",
        dimension = 2,
        ausgangName = "fläche",
        bezeichnung = "Fläche",
        mehrzahl = "Flächen",
    );

    companion object {
        fun vonKnotenArt(art: String): GeometrieTeilobjektTyp? = entries.firstOrNull { it.knotenArt == art }
    }
}

/** Liefert die geometrisch belegten Zellen der geforderten Dimension in stabiler Strukturreihenfolge. */
fun geometrieTeilobjekte(
    objekt: GeometrischerAusdruck,
    dimension: Int,
): List<GeometrischeZelle> = strukturVon(objekt)
    .stufen
    .firstOrNull { it.dimension == dimension }
    ?.zellen
    .orEmpty()
    .filter { it.geometrie != null }

/** Bestimmt die engste bereits vorhandene Anschlussart eines geometrischen Laufzeitobjekts. */
fun geometrieAnschlussArt(objekt: GeometrischerAusdruck): AnschlussArtId = when (objekt) {
    is GeometriePunkt -> GeometrieAnschlussArten.Punkt.id
    is GeometrieGerade -> GeometrieAnschlussArten.Gerade.id
    is GeometrieEbene -> GeometrieAnschlussArten.Ebene.id
    is GeometrieStrecke -> GeometrieAnschlussArten.Strecke.id
    is GeometrieStrahl -> GeometrieAnschlussArten.Strahl.id
    is GeometrieWinkel -> GeometrieAnschlussArten.Winkel.id
    is GeometrieKreislinie -> GeometrieAnschlussArten.Kreislinie.id
    is GeometriePolygon -> GeometrieAnschlussArten.Polygon.id
    is GeometrieDreieck -> GeometrieAnschlussArten.Dreieck.id
    is GeometrieGruppe -> GeometrieAnschlussArten.Gruppe.id
    is TransformiertesGeometrieObjekt -> GeometrieAnschlussArten.Objekt.id
    else -> GeometrieAnschlussArten.Objekt.id
}

internal fun MathematikAuswerterRegister.registriereGeometrieTeilobjekte() {
    GeometrieTeilobjektTyp.entries.forEach { typ ->
        registriere(typ.knotenArt) { kontext ->
            val eingang = kontext.eingänge["objekt"]
                ?: error("Ein geometrisches Objekt muss verbunden sein.")
            val objekt = eingang.objekt as? GeometrischerAusdruck
                ?: error("Der Eingang muss ein geometrisches Objekt liefern.")
            val zellen = geometrieTeilobjekte(objekt, typ.dimension)
            require(zellen.isNotEmpty()) {
                "${objekt.zuLatex()} besitzt keine auswählbaren ${typ.mehrzahl.lowercase()}."
            }

            val gespeicherteId = kontext.knoten.parameter[GEOMETRIE_TEILOBJEKT_ZELL_ID]
                ?.trim()
                .orEmpty()
            val ausgewählt = zellen.firstOrNull { it.id == gespeicherteId } ?: zellen.first()
            val geometrie = requireNotNull(ausgewählt.geometrie) {
                "Die ausgewählte ${typ.bezeichnung.lowercase()} '${ausgewählt.id}' besitzt keine Geometrie."
            }
            val warnungen = if (gespeicherteId.isNotEmpty() && ausgewählt.id != gespeicherteId) {
                listOf(
                    "Die gespeicherte ${typ.bezeichnung.lowercase()} '$gespeicherteId' existiert nicht mehr; " +
                        "stattdessen wird '${ausgewählt.id}' verwendet.",
                )
            } else {
                emptyList()
            }

            KnotenAuswertungsErgebnis(
                ausgaben = mapOf(
                    typ.ausgangName to BedingterWert(
                        objekt = geometrie,
                        annahmen = eingang.annahmen,
                        variablenQuellen = eingang.variablenQuellen,
                    ),
                ),
                warnungen = warnungen,
            )
        }
    }
}

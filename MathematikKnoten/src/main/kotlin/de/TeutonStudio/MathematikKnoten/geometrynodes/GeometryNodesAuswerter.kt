package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal fun MathematikAuswerterRegister.registriereGeometryNodes() {
    GeometryNodesVorlagen.definitionen.forEach { definition ->
        registriere(definition.vorlage.art) { k ->
            runCatching {
                pruefeGeometryNodesKonfiguration(k.knoten)?.let { error(it) }
                val p = definition.vorlage.standardParameter + k.knoten.parameter
                fun zahl(name: String) = p.getValue(name).toDouble()
                fun vektor(name: String) = GeometrieVektor3(zahl(name+"X"),zahl(name+"Y"),zahl(name+"Z"))
                fun geometrie(name: String, optional: Boolean = false): ProzeduraleGeometrie =
                    k.eingänge[name]?.let { it.objekt as? ProzeduraleGeometrie ?: error("'$name' benötigt ein Geometry Set.") }
                        ?: if(optional) ProzeduraleGeometrie() else error("Geometrieeingang '$name' muss verbunden sein.")
                fun skalar(name: String) = feldEingang<SkalarFeld>(k,name) { SkalarFeld.Konstant(zahl(name)) }
                fun vector(name: String, standard: VektorFeld? = null) = feldEingang<VektorFeld>(k,name) { standard ?: VektorFeld.Konstant(vektor(name)) }
                fun auswahl() = feldEingang<AuswahlFeld>(k,"auswahl") { AuswahlFeld.Konstant(p.getValue("auswahl").toBooleanStrict()) }
                val suffix = k.knoten.art.removePrefix(GEOMETRY_NODES_PREFIX)
                val wert: AtlasWert = when(suffix) {
                    "meshCube" -> GeometrieOperationen.wuerfel(vektor("groesse"))
                    "meshGrid" -> GeometrieOperationen.gitter(p.getValue("anzahlX").toInt(),p.getValue("anzahlY").toInt(),zahl("groesseX"),zahl("groesseY"))
                    "points" -> GeometrieOperationen.punkte(p.getValue("anzahl").toInt(),vector("position"))
                    "meshToPoints" -> GeometrieOperationen.meshZuPunkten(geometrie("geometrie"))
                    "joinGeometry" -> GeometrieOperationen.verbinden(listOf(geometrie("a",true),geometrie("b",true)))
                    "transformGeometry" -> GeometrieOperationen.transformieren(geometrie("geometrie"),GeometrieTransformation.trs(vektor("translation"),vektor("rotation"),vektor("skalierung")))
                    "position" -> VektorFeld.Position
                    "index" -> SkalarFeld.Index
                    "scalarMath" -> SkalarFeld.Rechnung(when(p.getValue("operator")) {
                        "add" -> SkalarOperation.ADDIEREN; "subtract" -> SkalarOperation.SUBTRAHIEREN; "multiply" -> SkalarOperation.MULTIPLIZIEREN; "divide" -> SkalarOperation.DIVIDIEREN; else -> error("Unbekannte Feldoperation.")
                    },skalar("a"),skalar("b"))
                    "vectorMath" -> when(p.getValue("operator")) {
                        "add" -> VektorFeld.Rechnung(VektorOperation.ADDIEREN,vector("a"),vector("b"))
                        "subtract" -> VektorFeld.Rechnung(VektorOperation.SUBTRAHIEREN,vector("a"),vector("b"))
                        "scale" -> VektorFeld.Skalieren(vector("a"),skalar("faktor"))
                        "combine" -> VektorFeld.Kombinieren(skalar("x"),skalar("y"),skalar("z"))
                        else -> error("Unbekannte Vektorfeldoperation.")
                    }
                    "compare" -> AuswahlFeld.Vergleich(when(p.getValue("operator")) {
                        "less" -> VergleichsOperation.KLEINER; "greater" -> VergleichsOperation.GROESSER; "equal" -> VergleichsOperation.GLEICH; else -> error("Unbekannter Feldvergleich.")
                    },skalar("a"),skalar("b"))
                    "setPosition" -> GeometrieOperationen.positionSetzen(geometrie("geometrie"),auswahl(),vector("position",VektorFeld.Position),vector("offset"))
                    "instanceOnPoints" -> GeometrieOperationen.aufPunktenInstanzieren(geometrie("punkte"),geometrie("instanz"),auswahl(),vector("rotation"),vector("skalierung"))
                    "realizeInstances" -> GeometrieOperationen.realisieren(geometrie("geometrie"))
                    "preview" -> geometrie("geometrie")
                    else -> error("Unbekannter Geometry Node.")
                }
                KnotenAuswertungsErgebnis(mapOf(definition.vorlage.anschlüsse.last().name to BedingterWert(wert,k.rechenKontext.annahmen)),eingänge=k.eingänge)
            }.getOrElse { KnotenAuswertungsErgebnis(emptyMap(),fehler=it.message ?: "Geometry-Node-Auswertung fehlgeschlagen.",eingänge=k.eingänge) }
        }
    }
}

private inline fun <reified T: AtlasWert> feldEingang(k: KnotenAuswertungsKontext,name: String,standard: () -> T): T =
    k.eingänge[name]?.let { it.objekt as? T ?: error("'$name' benötigt ${T::class.simpleName}.") } ?: standard()

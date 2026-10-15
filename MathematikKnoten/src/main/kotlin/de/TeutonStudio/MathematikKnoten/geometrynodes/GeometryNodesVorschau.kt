package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.*

data class GeometryVorschauLinie(val a: GeometrieVektor3,val b: GeometrieVektor3)
data class GeometryVorschauDaten(val punkte: List<GeometrieVektor3>,val linien: List<GeometryVorschauLinie>,val begrenzt: Boolean)

/** Nur ein begrenztes Renderabbild. Instanzen bleiben im fachlichen Modell referenziert. */
fun geometryVorschau(geometrie: ProzeduraleGeometrie): GeometryVorschauDaten {
    val punkte=ArrayList<GeometrieVektor3>(); val linien=ArrayList<GeometryVorschauLinie>()
    var begrenzt=false; var besuche=0
    fun besuchen(g: ProzeduraleGeometrie,t: GeometrieTransformation) {
        if(++besuche > 2048) { begrenzt=true; return }
        g.mesh?.let { mesh ->
            val freieLinien=5000-linien.size
            if(mesh.kanten.size > freieLinien) begrenzt=true
            mesh.kanten.take(freieLinien).forEach { k ->
                linien += GeometryVorschauLinie(t.anwenden(mesh.punkte[k.a]),t.anwenden(mesh.punkte[k.b]))
            }
            val freiePunkte=3000-punkte.size
            if(mesh.punkte.size > freiePunkte) begrenzt=true
            mesh.punkte.take(freiePunkte).forEach { p -> punkte += t.anwenden(p) }
        }
        g.punktwolke?.punkte?.let { ps ->
            val frei=3000-punkte.size
            if(ps.size > frei) begrenzt=true
            ps.take(frei).forEach { punkte += t.anwenden(it) }
        }
        for(instanz in g.instanzen) {
            if(besuche >= 2048 || punkte.size >= 3000 && linien.size >= 5000) { begrenzt=true; break }
            besuchen(instanz.geometrie,t*instanz.transformation)
        }
    }
    besuchen(geometrie,GeometrieTransformation.Identitaet)
    return GeometryVorschauDaten(punkte,linien,begrenzt)
}

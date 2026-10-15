package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.GeometrieGrenzen

const val GEOMETRY_NODES_PREFIX = "mathematik.geometryNodes."

/** Felder sind Anweisungen für einen Geometriekontext, keine bereits berechneten Zahlen. */
object GeometryNodesAnschlussArten {
    // Nominale Eltern-ID verhindert eine Initialisierungsabhängigkeit zurück zum Gesamtregister.
    val Geometrie = AnschlussArt(AnschlussArtId("${GEOMETRY_NODES_PREFIX}geometrie"), "Geometry Set", AnschlussArtId("atlas.wert"),beschreibung="Endliche numerische 3D-Geometrie mit Mesh, Punktwolke und Instanzreferenzen.")
    val Skalar = AnschlussArt(AnschlussArtId("${GEOMETRY_NODES_PREFIX}feld.skalar"), "Skalarfeld", AnschlussArtId("atlas.wert"),beschreibung="Verzögert pro Punkt ausgewertete skalare Berechnung.")
    val Vektor = AnschlussArt(AnschlussArtId("${GEOMETRY_NODES_PREFIX}feld.vektor"), "Vektorfeld", AnschlussArtId("atlas.wert"),beschreibung="Verzögert pro Punkt ausgewertete dreidimensionale Vektorberechnung.")
    val Auswahl = AnschlussArt(AnschlussArtId("${GEOMETRY_NODES_PREFIX}feld.bool"), "Auswahlfeld", AnschlussArtId("atlas.wert"),beschreibung="Verzögert pro Punkt ausgewertete boolesche Auswahl.")
    val alle = listOf(Geometrie, Skalar, Vektor, Auswahl)
}

data class GeometryNodesParameter(
    val schluessel: String,
    val titel: String,
    val standard: String,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val ganzzahlig: Boolean = false,
    val optionen: Map<String, String> = emptyMap(),
)

data class GeometryNodesDefinition(val vorlage: KnotenVorlage, val parameter: List<GeometryNodesParameter>)

/** Gemeinsame Konfiguration für Auswerter, Android und Desktop. Anschluss-IDs bleiben beim Konfigurieren bestehen. */
object GeometryNodesVorlagen {
    private val G get() = GeometryNodesAnschlussArten.Geometrie.id
    private val S get() = GeometryNodesAnschlussArten.Skalar.id
    private val V get() = GeometryNodesAnschlussArten.Vektor.id
    private val B get() = GeometryNodesAnschlussArten.Auswahl.id
    private fun zahl(key: String, titel: String = key, wert: String = "0", min: Double? = null) = GeometryNodesParameter(key,titel,wert,minimum=min)
    private fun anzahl(key: String, titel: String, wert: String, min: Int) = GeometryNodesParameter(key,titel,wert,min.toDouble(),GeometrieGrenzen.PUNKTE.toDouble(),true)
    private fun vektor(key: String, titel: String, wert: String = "0") = listOf("X","Y","Z").map { zahl(key+it,"$titel $it",wert) }
    private fun modus(optionen: Map<String,String>) = GeometryNodesParameter("operator","Operation",optionen.keys.first(),optionen=optionen)
    private val auswahl = GeometryNodesParameter("auswahl","Auswahl ohne Verbindung","true",optionen=linkedMapOf("true" to "Alle","false" to "Keine"))
    private fun definition(suffix: String, name: String, kategorie: String, beschreibung: String,
                           eingang: List<Pair<String,AnschlussArtId>> = emptyList(), ausgang: Pair<String,AnschlussArtId> = "geometrie" to G,
                           parameter: List<GeometryNodesParameter> = emptyList()): GeometryNodesDefinition {
        val ports = eingang.mapIndexed { i,(n,t) -> AnschlussDaten(name=n,art=t,richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,reihenfolge=i) } +
            AnschlussDaten(name=ausgang.first,art=ausgang.second,richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts)
        return GeometryNodesDefinition(KnotenVorlage(GEOMETRY_NODES_PREFIX+suffix,name,"Geometry Nodes: $kategorie",beschreibung,
            if(suffix=="preview") GraphGröße(510f,440f) else GraphGröße(285f,maxOf(120f,72f+eingang.size*27f)),ports,parameter.associate { it.schluessel to it.standard }),parameter)
    }
    val definitionen: List<GeometryNodesDefinition> = listOf(
        definition("meshCube","Mesh-Würfel","Mesh","Acht Eckpunkte, zwölf Kanten und sechs orientierte Flächen.",parameter=listOf("X","Y","Z").map { zahl("groesse$it","Größe $it","2",0.0) }),
        definition("meshGrid","Mesh-Gitter","Mesh","Regelmäßiges Gitter in der XY-Ebene; Index läuft zuerst entlang X.",parameter=listOf(anzahl("anzahlX","Eckpunkte X","4",2),anzahl("anzahlY","Eckpunkte Y","4",2),zahl("groesseX","Größe X","4",0.0),zahl("groesseY","Größe Y","4",0.0))),
        definition("points","Punkte","Punkte","Erzeugt Punkte. Das Positionsfeld sieht den laufenden Index und die Ursprungsposition.",listOf("position" to V),parameter=listOf(anzahl("anzahl","Anzahl","10",0))+vektor("position","Standardposition")),
        definition("meshToPoints","Mesh zu Punkten","Punkte","Konvertiert Mesh-Eckpunkte und erhält vorhandene Punkte. Instanzen vorher realisieren.",listOf("geometrie" to G)),
        definition("joinGeometry","Geometrie verbinden","Geometrie","Verbindet Meshes mit versetzten Indizes, Punktwolken und Instanzen. Fehlende Eingänge sind leer.",listOf("a" to G,"b" to G)),
        definition("transformGeometry","Geometrie transformieren","Geometrie","Translation × Rz × Ry × Rx × Skalierung. Winkel in Radiant; Instanzen bleiben Referenzen.",listOf("geometrie" to G),parameter=vektor("translation","Translation")+vektor("rotation","Rotation (rad)")+vektor("skalierung","Skalierung","1")),
        definition("position","Position","Felder","Liest erst am Verbraucher die ursprüngliche Punktposition.",ausgang="feld" to V),
        definition("index","Index","Felder","Liest erst am Verbraucher den nullbasierten Index der jeweiligen Komponente.",ausgang="feld" to S),
        definition("scalarMath","Feldrechner","Felder","Verzögerte skalare Feldrechnung.",listOf("a" to S,"b" to S),"feld" to S,listOf(modus(linkedMapOf("add" to "Addieren","subtract" to "Subtrahieren","multiply" to "Multiplizieren","divide" to "Dividieren")),zahl("a","Standard A"),zahl("b","Standard B","1"))),
        definition("vectorMath","Vektorfeldrechner","Felder","Vektoren addieren, subtrahieren, skalar skalieren oder aus drei Skalarfeldern kombinieren.",listOf("a" to V,"b" to V,"faktor" to S,"x" to S,"y" to S,"z" to S),"feld" to V,
            listOf(modus(linkedMapOf("add" to "Addieren","subtract" to "Subtrahieren","scale" to "Skalieren","combine" to "XYZ kombinieren")))+vektor("a","Standard A")+vektor("b","Standard B")+listOf(zahl("faktor","Faktor","1"),zahl("x","Standard X"),zahl("y","Standard Y"),zahl("z","Standard Z"))),
        definition("compare","Feldvergleich","Felder","Erzeugt ein boolesches Auswahlfeld aus zwei Skalarfeldern.",listOf("a" to S,"b" to S),"auswahl" to B,listOf(modus(linkedMapOf("less" to "Kleiner","greater" to "Größer","equal" to "Gleich")),zahl("a","Standard A"),zahl("b","Standard B","1"))),
        definition("setPosition","Position setzen","Geometrie","Setzt ausgewählte Eckpunkte/Punkte auf Position + Offset. Alle Felder sehen den ursprünglichen Input; Instanzen vorher realisieren.",listOf("geometrie" to G,"auswahl" to B,"position" to V,"offset" to V),parameter=listOf(auswahl)+vektor("offset","Offset")),
        definition("instanceOnPoints","Instanzen auf Punkten","Instanzen","Referenziert dieselbe Geometrie an ausgewählten Mesh-Eckpunkten und Punkten. Punkteingang muss realisiert sein.",listOf("punkte" to G,"instanz" to G,"auswahl" to B,"rotation" to V,"skalierung" to V),parameter=listOf(auswahl)+vektor("rotation","Rotation (rad)")+vektor("skalierung","Skalierung","1")),
        definition("realizeInstances","Instanzen realisieren","Instanzen","Expandiert Instanzen rekursiv mit geprüftem Gesamtbudget.",listOf("geometrie" to G)),
        definition("preview","Geometrievorschau","Geometrie","Interaktive 3D-Drahtansicht von Meshes, Punkten und Instanzen; reicht die unveränderte Geometrie weiter.",listOf("geometrie" to G)),
    )
    val alle = definitionen.map { it.vorlage }
    fun finde(art: String) = definitionen.firstOrNull { it.vorlage.art == art }
}

/** Validiert gespeicherte Parameter ohne vorauszusetzen, dass Geometrieeingänge bereits verbunden sind. */
fun pruefeGeometryNodesKonfiguration(knoten: KnotenDaten): String? {
    val definition = GeometryNodesVorlagen.finde(knoten.art) ?: return "Unbekannter Geometry-Node-Typ."
    for(p in definition.parameter) {
        val text = knoten.parameter[p.schluessel] ?: p.standard
        if(p.optionen.isNotEmpty()) {
            if(text !in p.optionen) return "${p.titel}: unbekannter Wert '$text'."
        } else {
            val wert = text.toDoubleOrNull()
            if(wert == null || !wert.isFinite()) return "${p.titel}: eine endliche Zahl ist erforderlich."
            if(p.ganzzahlig && text.toIntOrNull() == null) return "${p.titel}: eine ganze Zahl ist erforderlich."
            if(p.minimum != null && wert < p.minimum || p.maximum != null && wert > p.maximum) return "${p.titel}: Wert außerhalb des zulässigen Bereichs."
        }
    }
    if(knoten.art == GEOMETRY_NODES_PREFIX+"meshGrid") {
        val x=(knoten.parameter["anzahlX"] ?: "4").toLong(); val y=(knoten.parameter["anzahlY"] ?: "4").toLong()
        if(x*y > GeometrieGrenzen.PUNKTE || (x-1)*(y-1)*4 > GeometrieGrenzen.ECKEN) return "Das Gitter überschreitet das Geometriebudget."
    }
    return null
}

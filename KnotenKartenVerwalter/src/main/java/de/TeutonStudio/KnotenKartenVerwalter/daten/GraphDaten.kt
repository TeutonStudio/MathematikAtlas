package de.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Gemeinsamer Datenvertrag aller fachlichen Objekte, die als Graphobjekte dargestellt werden können.
 */
interface GraphDaten{
    val id: String
    var klasse: String?
}

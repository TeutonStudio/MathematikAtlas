package de.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Rekursiv persistierbarer Eigenschaftswert für Knotenkonfigurationen.
 * Er enthält nur plattformneutrale Primitive und bleibt dadurch für Karten
 * sowie Undo/Redo geeignet.
 */
sealed interface KnotenEigenschaft {
    data class Text(val wert: String) : KnotenEigenschaft
    data class Ganzzahl(val wert: Int) : KnotenEigenschaft
    data class Dezimalzahl(val wert: Double) : KnotenEigenschaft
    data class Wahrheitswert(val wert: Boolean) : KnotenEigenschaft
    data class Farbe(val argb: Long) : KnotenEigenschaft
    data class Liste(val werte: List<KnotenEigenschaft>) : KnotenEigenschaft
    data class Objekt(val felder: Map<String, KnotenEigenschaft>) : KnotenEigenschaft
}

fun Map<String, KnotenEigenschaft>.text(schlüssel: String, standard: String) =
    (this[schlüssel] as? KnotenEigenschaft.Text)?.wert ?: standard
fun Map<String, KnotenEigenschaft>.ganzzahl(schlüssel: String, standard: Int) =
    (this[schlüssel] as? KnotenEigenschaft.Ganzzahl)?.wert ?: standard
fun Map<String, KnotenEigenschaft>.dezimalzahl(schlüssel: String, standard: Double) =
    (this[schlüssel] as? KnotenEigenschaft.Dezimalzahl)?.wert?.takeIf { it.isFinite() } ?: standard
fun Map<String, KnotenEigenschaft>.wahrheitswert(schlüssel: String, standard: Boolean) =
    (this[schlüssel] as? KnotenEigenschaft.Wahrheitswert)?.wert ?: standard
fun Map<String, KnotenEigenschaft>.objekt(schlüssel: String) = this[schlüssel] as? KnotenEigenschaft.Objekt

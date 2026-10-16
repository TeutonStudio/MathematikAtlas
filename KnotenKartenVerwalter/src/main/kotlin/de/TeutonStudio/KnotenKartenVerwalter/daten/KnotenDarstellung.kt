package de.TeutonStudio.KnotenKartenVerwalter.daten

const val KNOTEN_HINTERGRUND_EIGENSCHAFT = "atlas.hintergrundfarbe"
const val KNOTEN_RAHMEN_EIGENSCHAFT = "atlas.rahmenfarbe"

fun parseKnotenFarbe(text: String): Long? = text.trim().removePrefix("#").let {
    if (it.length != 6 || it.any { c -> c.digitToIntOrNull(16) == null }) null
    else 0xFF000000L or it.toLong(16)
}

/** Validierung auch für Aufrufer außerhalb eines Composables. */
fun KnotenDaten.mitDarstellungsFarbe(schlüssel: String, argb: Long?): KnotenDaten {
    require(schlüssel in setOf(KNOTEN_HINTERGRUND_EIGENSCHAFT, KNOTEN_RAHMEN_EIGENSCHAFT))
    require(argb == null || argb in 0xFF000000L..0xFFFFFFFFL) { "Eine undurchsichtige RGB-Farbe ist erforderlich." }
    return copy(eigenschaften = if (argb == null) eigenschaften - schlüssel
        else eigenschaften + (schlüssel to KnotenEigenschaft.Farbe(argb)))
}

fun KnotenDaten.mitDarstellungsForm(form: KnotenDarstellungsForm, erlaubt: Set<KnotenDarstellungsForm>): KnotenDaten {
    require(form in erlaubt) { "Diese Form ist für den Knoten nicht freigegeben." }
    return copy(eigenschaften = eigenschaften + (KNOTEN_FORM_EIGENSCHAFT to KnotenEigenschaft.Text(form.schlüssel)))
}

/** Sicheres Inhaltsrechteck innerhalb der Kontur, relativ zum Körper. */
data class KnotenInhaltsRahmen(val x: Float, val y: Float, val breite: Float, val höhe: Float)

val KnotenDarstellungsForm.inhaltsRahmen: KnotenInhaltsRahmen get() = when (this) {
    KnotenDarstellungsForm.Ellipse -> KnotenInhaltsRahmen(.15f, .15f, .7f, .7f)
    KnotenDarstellungsForm.Dreieck -> KnotenInhaltsRahmen(.26f, .52f, .48f, .46f)
    KnotenDarstellungsForm.Raute -> KnotenInhaltsRahmen(.26f, .26f, .48f, .48f)
    else -> KnotenInhaltsRahmen(0f, 0f, 1f, 1f)
}

/** Begrenzter Inhalt benötigt auch in einer Kapsel sichere Einzüge; natürlich
 * gemessene kompakte Adapter behalten ihre bisherige zentrierte Darstellung. */
fun KnotenDaten.inhaltsRahmen(freieMessung: Boolean): KnotenInhaltsRahmen {
    if (darstellungsForm != KnotenDarstellungsForm.Adapter || freieMessung) return darstellungsForm.inhaltsRahmen
    val einzug = adapterEckenRadius(größe.breite, größe.höhe) * (1f - kotlin.math.sqrt(.5f))
    val x = einzug / größe.breite.coerceAtLeast(1f)
    val y = einzug / größe.höhe.coerceAtLeast(1f)
    return KnotenInhaltsRahmen(x, y, 1f - 2f * x, 1f - 2f * y)
}

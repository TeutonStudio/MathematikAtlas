package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikKartenAdapter.*

internal val HISTORISCHE_METHODEN_ANALYSIS = mapOf(
    "mathematik.spaltenMethodeDifferentieren" to "mathematik.methodenDifferentieren",
    "mathematik.zeilenMethodeDifferentieren" to "mathematik.methodenDifferentieren",
    "mathematik.spaltenMethodeIntegrieren" to "mathematik.methodenIntegrieren",
    "mathematik.zeilenMethodeIntegrieren" to "mathematik.methodenIntegrieren",
)

/** Die Projektion wird explizit; Instanz-, Anschluss- und Verbindungsidentitäten bleiben erhalten. */
fun KartenDaten.migriereMethodenKonsolidierung(): KartenDaten = copy(knoten = knoten.map { alt ->
    if (alt.art != METHODEN_ANWENDUNG_ART && alt.art !in HISTORISCHE_METHODEN_ANALYSIS || RECHNER_MIGRATION_DIAGNOSE in alt.parameter) return@map alt
    fun abgelehnt(grund: String) = alt.copy(parameter = alt.parameter + (RECHNER_MIGRATION_DIAGNOSE to "${alt.art} bleibt erhalten: $grund"))
    val ports = alt.anschlüsse
    if (ports.map { it.id }.distinct().size != ports.size || ports.groupBy { it.richtung }.any { (_, p) -> p.map { it.name }.distinct().size != p.size } ||
        ports.any { it.richtung == AnschlussRichtung.Neutral || it.belegung == AnschlussBelegung.Mehrfach }) return@map abgelehnt("Die Anschlussrollen oder Kardinalitäten sind nicht eindeutig.")
    val eingänge = ports.filter { it.richtung == AnschlussRichtung.Eingang }
    val ausgänge = ports.filter { it.richtung == AnschlussRichtung.Ausgang }
    val methode = eingänge.singleOrNull { it.name == "methode" } ?: return@map abgelehnt("Der eindeutige Methodeneingang fehlt.")
    val arten = AnschlussArtRegister(MathematikAnschlussArten.alle)
    if (!arten.istUnterart(methode.art, MathematikAnschlussArten.Methode.id) && methode.art !in MathematikAnschlussArten.historischeMethodenIds)
        return@map abgelehnt("Der gespeicherte Methodeneingang besitzt keine bekannte Methodenart.")
    val normalisiert = KartenDaten(name="Migration",knoten=listOf(alt)).migriereMethodenAnschlüsse().knoten.single()
    if (alt.art == METHODEN_ANWENDUNG_ART) {
        val ausgang = ausgänge.singleOrNull()?.takeIf { it.name == "wert" } ?: return@map abgelehnt("Die direkte Einzelausgabe ist nicht eindeutig.")
        val argumente = eingänge.filter { it.name != "methode" }
        if (argumente.map { it.reihenfolge }.distinct().size != argumente.size || argumente.any { it.semantischeId != null })
            return@map abgelehnt("Die historische positionsbasierte Argumentbindung ist nicht eindeutig.")
        val ergebnisArt = alt.parameter[METHODEN_ANWENDUNG_ERGEBNIS_ART] ?: ausgang.art.wert
        if (arten.finde(AnschlussArtId(ergebnisArt)) == null || !arten.istUnterart(ausgang.art, AnschlussArtId(ergebnisArt)))
            return@map abgelehnt("Der gespeicherte Einzelresultatvertrag ist nicht kompatibel.")
        normalisiert.copy(art = METHODEN_AUFRUF_ART, parameter = normalisiert.parameter + mapOf(
            METHODEN_AUFRUF_ARGUMENTPROJEKTION to METHODEN_ARGUMENTPROJEKTION_SEPARIERT,
            METHODEN_AUFRUF_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_DIREKT,
            METHODEN_AUFRUF_PROJEKTION_FEST to "true",
            METHODEN_ANWENDUNG_ERGEBNIS_ART to ergebnisArt,
            "festeEingänge" to argumente.size.toString(),
        ))
    } else {
        if (eingänge.size != 1 || ausgänge.singleOrNull()?.name != "methode" ||
            ausgänge.any { !arten.istUnterart(it.art, MathematikAnschlussArten.Methode.id) }) return@map abgelehnt("Die Analysis-Schnittstelle ist nicht eindeutig.")
        val orient = alt.parameter[VEKTOR_ORIENTIERUNG_PARAMETER]
            ?: if (alt.art.contains("zeilen")) VEKTOR_ORIENTIERUNG_ZEILE else VEKTOR_ORIENTIERUNG_SPALTE
        if (orient !in setOf(VEKTOR_ORIENTIERUNG_ZEILE,VEKTOR_ORIENTIERUNG_SPALTE)) return@map abgelehnt("Unbekannte gespeicherte Orientierung '$orient'.")
        normalisiert.copy(art = HISTORISCHE_METHODEN_ANALYSIS.getValue(alt.art), parameter = normalisiert.parameter + (VEKTOR_ORIENTIERUNG_PARAMETER to orient))
    }
})

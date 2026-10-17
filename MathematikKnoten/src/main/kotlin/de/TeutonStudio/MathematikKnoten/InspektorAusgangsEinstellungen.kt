package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.*

private val orientierungsArten = setOf("mathematik.tupelZuSpalte", "mathematik.tupelZuZeile",
    "mathematik.einheitsSpalte", "mathematik.einheitsZeile", "mathematik.kreuzproduktSpalte", "mathematik.kreuzproduktZeile",
    "mathematik.spaltenMethodeDifferentieren", "mathematik.zeilenMethodeDifferentieren", "mathematik.spaltenMethodeIntegrieren", "mathematik.zeilenMethodeIntegrieren")

fun hatGemeinsameAusgangsEinstellungen(k: KnotenDaten): Boolean = when {
    k.art == ÜBERSETZER_ART || k.art in orientierungsArten || istKonfigurierteVektorAnalysis(k) || k.art in setOf("mathematik.vektor", "mathematik.zeilenVektor", "mathematik.multinomVektor", TANGENTIAL_KNOTEN_ART) -> true
    k.art == MATRIXDIAGONALE_ART -> matrixdiagonaleModus(k) == MATRIXDIAGONALE_AUSLESEN
    k.art == ZAHLENRECHNER_ART -> UniversellerZahlenOperator.vonIdOderNull(k.parameter[ZAHLENRECHNER_OPERATOR]) == UniversellerZahlenOperator.DIFFERENTIAL
    k.art == MatrixRechner.KNOTEN_ART -> k.parameter[RECHNER_OPERATOR_PARAMETER] == MatrixRechnerOperator.ZERLEGEN.stabileId
    else -> false
}

/** Richtungsweite Fachoptionen, von Android und Desktop gemeinsam verwendet. */
@Composable
fun InspektorAusgangsEinstellungen(k: KnotenDaten, r: KnotenAuswertungsErgebnis?, übernehmen: (KnotenDaten) -> Unit) {
    @Composable fun auswahl(titel: String, werte: List<Pair<String, String>>, aktuell: String, ändern: (String) -> KnotenDaten) {
        Text(titel, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            werte.forEach { (wert, name) -> FilterChip(selected = aktuell == wert,
                onClick = { if (wert != aktuell) übernehmen(ändern(wert)) }, label = { Text(name) }) }
        }
    }
    val orientierungen = listOf(VEKTOR_ORIENTIERUNG_SPALTE to "Spalte", VEKTOR_ORIENTIERUNG_ZEILE to "Zeile")
    when {
        k.art == ÜBERSETZER_ART -> ÜbersetzerEinstellungen(k, r, übernehmen)
        k.art in orientierungsArten || istKonfigurierteVektorAnalysis(k) -> {
            val orientierung = k.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: orientierungFürOrientierungsKnoten(k)
            if (orientierung !in orientierungen.map { it.first }) Text("Unbekannte gespeicherte Orientierung '$orientierung'. Bitte eine gültige Orientierung wählen.", color=MaterialTheme.colorScheme.error)
            auswahl("Orientierung", orientierungen, orientierung) { konfiguriereOrientierungsKnoten(k, it) }
        }
        k.art in setOf("mathematik.vektor", "mathematik.zeilenVektor") -> auswahl("Orientierung", orientierungen, vektorKonstruktorKonfiguration(k).orientierung) { konfiguriereVektorKonstruktor(k, orientierung = it) }
        k.art == "mathematik.multinomVektor" -> {
            val form = k.parameter[MULTINOM_AUSGABEFORM_PARAMETER] ?: MULTINOM_AUSGABE_VEKTOR
            auswahl("Ausgabeform", listOf(MULTINOM_AUSGABE_VEKTOR to "Vektor", MULTINOM_AUSGABE_TUPEL to "Tupel"), form) { konfiguriereMultinomVektor(k, ausgabeForm = it) }
            if (form == MULTINOM_AUSGABE_VEKTOR) auswahl("Orientierung", orientierungen, k.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: VEKTOR_ORIENTIERUNG_SPALTE) { konfiguriereMultinomVektor(k, orientierung = it) }
        }
        k.art == MATRIXDIAGONALE_ART && matrixdiagonaleModus(k) == MATRIXDIAGONALE_AUSLESEN ->
            auswahl("Ausgabeformat", listOf("tupel" to "Tupel", "zeile" to "Zeile", "spalte" to "Spalte"), matrixdiagonaleAusgabe(k)) { konfiguriereMatrixdiagonale(k, ausgabe = it) }
        k.art == TANGENTIAL_KNOTEN_ART -> auswahl("Ausgabe", TangentialAusgabeForm.entries.map { it.name to if (it == TangentialAusgabeForm.METHODE) "Methode" else "Menge" }, aktuelleTangentialAusgabeform(k).name) { konfiguriereTangentialKnoten(k, TangentialAusgabeForm.valueOf(it)) }
        k.art == ZAHLENRECHNER_ART && UniversellerZahlenOperator.vonIdOderNull(k.parameter[ZAHLENRECHNER_OPERATOR]) == UniversellerZahlenOperator.DIFFERENTIAL -> {
            val art = aktuelleZahlenRechnerDifferentialErgebnisArt(k)
            if (art == null) Text("Historischer Termmodus; eine strukturierte Ergebnisart kann ausdrücklich gewählt werden.")
            auswahl("Ergebnisart", listOf(ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION.name to "f′", ZahlenRechnerDifferentialErgebnisArt.DIFFERENTIAL.name to "df"), art?.name.orEmpty()) { konfiguriereZahlenRechnerDifferential(k, ZahlenRechnerDifferentialErgebnisArt.valueOf(it)) }
        }
        k.art == MatrixRechner.KNOTEN_ART && k.parameter[RECHNER_OPERATOR_PARAMETER] == MatrixRechnerOperator.ZERLEGEN.stabileId ->
            auswahl("Zerlegung", MatrixZerlegeRichtung.entries.map { it.name to if (it == MatrixZerlegeRichtung.ZEILEN) "Zeilen" else "Spalten" }, k.parameter[MATRIX_ZERLEGEN_RICHTUNG] ?: MatrixZerlegeRichtung.ZEILEN.name) { k.copy(parameter = k.parameter + (MATRIX_ZERLEGEN_RICHTUNG to it)) }
    }
}

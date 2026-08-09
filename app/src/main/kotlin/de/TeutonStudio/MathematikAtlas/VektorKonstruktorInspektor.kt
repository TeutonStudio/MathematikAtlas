package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*

internal object VektorKonstruktorInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val config = vektorKonstruktorKonfiguration(knoten)
        Text("Erzeugungsart", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = config.erzeugungsArt == VEKTOR_EINZEL_EINGABEN,
                onClick = {
                    aktionen.knoten(
                        konfiguriereVektorKonstruktor(knoten, erzeugungsArt = VEKTOR_EINZEL_EINGABEN),
                    )
                },
                label = { Text("Elemente einzeln") },
            )
            FilterChip(
                selected = config.erzeugungsArt == VEKTOR_METHODE,
                onClick = {
                    aktionen.knoten(
                        konfiguriereVektorKonstruktor(knoten, erzeugungsArt = VEKTOR_METHODE),
                    )
                },
                label = { Text("Dimension + Methode") },
            )
        }
        Text("Orientierung", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = config.orientierung == VEKTOR_ORIENTIERUNG_SPALTE,
                onClick = {
                    aktionen.knoten(
                        konfiguriereVektorKonstruktor(knoten, orientierung = VEKTOR_ORIENTIERUNG_SPALTE),
                    )
                },
                label = { Text("Spalte") },
            )
            FilterChip(
                selected = config.orientierung == VEKTOR_ORIENTIERUNG_ZEILE,
                onClick = {
                    aktionen.knoten(
                        konfiguriereVektorKonstruktor(knoten, orientierung = VEKTOR_ORIENTIERUNG_ZEILE),
                    )
                },
                label = { Text("Zeile") },
            )
        }
        Text(
            if (config.erzeugungsArt == VEKTOR_METHODE) {
                "Die einstellige Indexmethode wird wie beim Tupelkonstruktor mit 1,…,n ausgewertet."
            } else {
                "Die Komponenten folgen der sichtbaren Eingangsreihenfolge."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

internal object MultinomVektorInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val form = knoten.parameter[MULTINOM_AUSGABEFORM_PARAMETER] ?: MULTINOM_AUSGABE_VEKTOR
        val orient = knoten.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: VEKTOR_ORIENTIERUNG_SPALTE
        Text("Ausgabeform", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = form == MULTINOM_AUSGABE_VEKTOR,
                onClick = { aktionen.knoten(konfiguriereMultinomVektor(knoten, ausgabeForm = MULTINOM_AUSGABE_VEKTOR)) },
                label = { Text("Vektor") },
            )
            FilterChip(
                selected = form == MULTINOM_AUSGABE_TUPEL,
                onClick = { aktionen.knoten(konfiguriereMultinomVektor(knoten, ausgabeForm = MULTINOM_AUSGABE_TUPEL)) },
                label = { Text("Tupel") },
            )
        }
        if (form == MULTINOM_AUSGABE_VEKTOR) {
            Text("Orientierung", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = orient == VEKTOR_ORIENTIERUNG_SPALTE,
                    onClick = { aktionen.knoten(konfiguriereMultinomVektor(knoten, orientierung = VEKTOR_ORIENTIERUNG_SPALTE)) },
                    label = { Text("Spalte") },
                )
                FilterChip(
                    selected = orient == VEKTOR_ORIENTIERUNG_ZEILE,
                    onClick = { aktionen.knoten(konfiguriereMultinomVektor(knoten, orientierung = VEKTOR_ORIENTIERUNG_ZEILE)) },
                    label = { Text("Zeile") },
                )
            }
        }
        LatexText("(x^k)_{0\\le k\\le dim}", style = MaterialTheme.typography.bodyLarge)
        Text(
            "x und dim stammen aus den Knoteneingängen; dim bezeichnet den höchsten Exponenten.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

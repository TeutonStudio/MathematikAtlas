package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikRechenSystem.kern.Wahrheitswert
import java.math.BigInteger

internal val KARTEN_EINGANGS_ZELLEN_BREITE = 124.dp
internal val KARTEN_ERGEBNIS_ZELLEN_BREITE = 152.dp
internal val KARTEN_TABELLEN_TRENNER_BREITE = 2.dp
internal val KARTEN_TABELLEN_SEITENGRÖSSE = BigInteger.valueOf(32)

@Composable
internal fun KartenTabelle(
    eingänge: List<KartenTabellenAnschluss>,
    freiePrädikate: Set<KartenTabellenAnschluss>,
    ausgänge: List<KartenTabellenAnschluss>,
    zeilen: List<KartenTabellenZeile>,
    text: (String, String) -> String,
    breite: Dp,
) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.width(breite.coerceAtMost(920.dp)).horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(breite)) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    for (feld in eingänge) {
                        val titel = if (feld in freiePrädikate) {
                            val mengen = parseKartenTabellenMengenListe(
                                text(kartenTabellenPrädikatMengenSchlüssel(feld), "R"),
                            )
                            val argumente = mengen.mapIndexed { index: Int, menge ->
                                text(
                                    kartenTabellenPrädikatArgumentSchlüssel(feld, index),
                                    standardArgumentFürKartenTabelle(menge),
                                )
                            }.joinToString(",")
                            "${feld.name}($argumente)"
                        } else feld.name
                        KartenTabellenKopfZelle(titel, KARTEN_EINGANGS_ZELLEN_BREITE)
                    }
                    if (eingänge.isNotEmpty()) {
                        VerticalDivider(
                            Modifier.fillMaxHeight(),
                            thickness = KARTEN_TABELLEN_TRENNER_BREITE,
                        )
                    }
                    for (ausgang in ausgänge) {
                        KartenTabellenKopfZelle(ausgang.name, KARTEN_ERGEBNIS_ZELLEN_BREITE)
                    }
                }
                HorizontalDivider(thickness = KARTEN_TABELLEN_TRENNER_BREITE)
                for (zeile in zeilen) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        for (wert in zeile.eingänge) {
                            KartenAussageZelle(if (wert) Wahrheitswert.Wahr else Wahrheitswert.Lüge)
                        }
                        if (eingänge.isNotEmpty()) {
                            VerticalDivider(
                                Modifier.fillMaxHeight(),
                                thickness = KARTEN_TABELLEN_TRENNER_BREITE,
                            )
                        }
                        for (zelle in zeile.ausgaben) KartenErgebnisZelle(zelle)
                    }
                }
            }
        }
    }
}

@Composable
private fun KartenTabellenKopfZelle(text: String, breite: Dp) {
    Box(
        Modifier.width(breite).padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun KartenAussageZelle(wert: Wahrheitswert?) {
    Box(
        Modifier.width(KARTEN_EINGANGS_ZELLEN_BREITE).padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (wert == null) Text("Nicht entscheidbar", style = MaterialTheme.typography.bodySmall)
        else LatexText(wert.latex, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun KartenErgebnisZelle(zelle: KartenTabellenZelle) {
    Box(
        Modifier.width(KARTEN_ERGEBNIS_ZELLEN_BREITE).padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (zelle) {
            is KartenTabellenZelle.WahrheitswertZelle -> if (zelle.wert == null) {
                Text("Nicht entscheidbar", style = MaterialTheme.typography.bodySmall)
            } else LatexText(zelle.wert.latex, style = MaterialTheme.typography.bodyMedium)
            is KartenTabellenZelle.ObjektZelle ->
                LatexText(zelle.latex, style = MaterialTheme.typography.bodyMedium)
            is KartenTabellenZelle.FehlerZelle -> Text(
                zelle.text,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            else -> Text("Nicht darstellbar", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun KartenTabellenSeitenNavigation(
    start: BigInteger,
    letzterStart: BigInteger,
    ändern: (BigInteger) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { ändern(BigInteger.ZERO) }, enabled = start > BigInteger.ZERO) {
            Text("Erste")
        }
        TextButton(
            onClick = { ändern(start.subtract(KARTEN_TABELLEN_SEITENGRÖSSE).max(BigInteger.ZERO)) },
            enabled = start > BigInteger.ZERO,
        ) { Text("Zurück") }
        TextButton(
            onClick = { ändern(start.add(KARTEN_TABELLEN_SEITENGRÖSSE).min(letzterStart)) },
            enabled = start < letzterStart,
        ) { Text("Weiter") }
        TextButton(onClick = { ändern(letzterStart) }, enabled = start < letzterStart) {
            Text("Letzte")
        }
    }
}

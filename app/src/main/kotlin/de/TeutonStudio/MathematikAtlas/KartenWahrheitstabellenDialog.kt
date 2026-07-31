package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.KartenQuelle
import de.TeutonStudio.MathematikKnoten.GesamterMathematikAuswerter
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import java.math.BigInteger

@Composable
internal fun KartenWahrheitstabellenDialog(
    zustand: AtlasZustand,
    knoten: KnotenDaten,
    quelle: KartenWahrheitstabellenQuelle,
    schließen: () -> Unit,
) {
    val texte = remember(knoten.id) { mutableStateMapOf<String, String>() }
    val eigenschaften = knoten.eigenschaften
    fun text(schlüssel: String, standard: String): String =
        texte[schlüssel] ?: (eigenschaften[schlüssel] as? KnotenEigenschaft.Text)?.wert ?: standard
    fun speichereText(schlüssel: String, wert: String) {
        texte[schlüssel] = wert
        zustand.editor.führeAus(
            KartenAktion.KnotenEigenschaftÄndern(
                knoten.id,
                schlüssel,
                KnotenEigenschaft.Text(wert),
            ),
        )
    }

    var weitereAnzeigen by remember(knoten.id) {
        mutableStateOf(
            (eigenschaften["$KARTEN_TABELLEN_PREFIX.weitereAusgänge"] as? KnotenEigenschaft.Wahrheitswert)
                ?.wert ?: false,
        )
    }
    val verbundeneFelder = quelle.eingänge.filterTo(linkedSetOf()) {
        hatKartenTabellenVerbindung(zustand, knoten, it.äußererAnschluss)
    }
    val verbundeneWerte = quelle.eingänge.associateWith {
        verbundenerKartenTabellenWert(zustand, knoten, it.äußererAnschluss)
    }
    val freieLogischeEingänge = quelle.eingänge.filter { feld ->
        feld !in verbundeneFelder && (
            zustand.anschlussArten.istUnterart(feld.art, MathematikAnschlussArten.Aussage.id) ||
                zustand.anschlussArten.istUnterart(feld.art, MathematikAnschlussArten.AussageFunktion.id)
            )
    }
    val freieAussagen = freieLogischeEingänge.filter {
        zustand.anschlussArten.istUnterart(it.art, MathematikAnschlussArten.Aussage.id)
    }
    val freiePrädikate = freieLogischeEingänge.filter {
        zustand.anschlussArten.istUnterart(it.art, MathematikAnschlussArten.AussageFunktion.id)
    }
    val zeilenAnzahl = kartenWahrheitstabellenZeilenAnzahl(freieLogischeEingänge.size)
    var seitenStart by remember(knoten.id, freieLogischeEingänge.map { it.äußererAnschluss.id }) {
        mutableStateOf(BigInteger.ZERO)
    }
    val letzterStart = remember(zeilenAnzahl) {
        zeilenAnzahl.subtract(BigInteger.ONE)
            .divide(KARTEN_TABELLEN_SEITENGRÖSSE)
            .multiply(KARTEN_TABELLEN_SEITENGRÖSSE)
    }
    val zeilenAufSeite = zeilenAnzahl.subtract(seitenStart)
        .min(KARTEN_TABELLEN_SEITENGRÖSSE)
        .toInt()
    val evaluator = remember(quelle.verweis) {
        KartenAuswerter(
            GesamterMathematikAuswerter.erzeugeRegister(),
            KartenQuelle(zustand.speicher::lade),
        )
    }
    val konfigurationsSignatur = texte.toMap().hashCode() * 31 +
        weitereAnzeigen.hashCode() * 17 + zustand.auswertung.hashCode()
    val zeilen = remember(quelle, seitenStart, zeilenAufSeite, konfigurationsSignatur) {
        evaluator.leereCache()
        List(zeilenAufSeite) { offset ->
            berechneKartenTabellenZeile(
                zustand = zustand,
                quelle = quelle,
                evaluator = evaluator,
                index = seitenStart + BigInteger.valueOf(offset.toLong()),
                freieLogischeEingänge = freieLogischeEingänge,
                freieAussagen = freieAussagen,
                freiePrädikate = freiePrädikate,
                verbundeneFelder = verbundeneFelder,
                verbundeneWerte = verbundeneWerte,
                weitereAnzeigen = weitereAnzeigen,
                text = ::text,
            )
        }
    }

    val ausgänge = quelle.aussageAusgänge +
        if (weitereAnzeigen) quelle.weitereAusgänge else emptyList()
    val tabellenBreite = KARTEN_EINGANGS_ZELLEN_BREITE * freieLogischeEingänge.size.toFloat() +
        (if (freieLogischeEingänge.isNotEmpty()) KARTEN_TABELLEN_TRENNER_BREITE else 0.dp) +
        KARTEN_ERGEBNIS_ZELLEN_BREITE * ausgänge.size.toFloat()

    Dialog(
        onDismissRequest = schließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val gewünschteBreite = (tabellenBreite + 48.dp)
                .coerceAtLeast(420.dp)
                .coerceAtMost(980.dp)
            val maximaleInhaltsHöhe = maxHeight * .72f
            Surface(
                Modifier
                    .width(gewünschteBreite.coerceAtMost(maxWidth * .94f))
                    .heightIn(max = maxHeight * .9f),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 8.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    KartenTabellenKopf(knoten, quelle, schließen)
                    HorizontalDivider()
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = maximaleInhaltsHöhe)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        KartenTabellenEingabeKonfiguration(
                            zustand = zustand,
                            quelle = quelle,
                            verbundeneFelder = verbundeneFelder,
                            verbundeneWerte = verbundeneWerte,
                            text = ::text,
                            speichereText = ::speichereText,
                        )
                        if (quelle.weitereAusgänge.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Weitere Ausgänge anzeigen", Modifier.weight(1f))
                                Switch(
                                    checked = weitereAnzeigen,
                                    onCheckedChange = { neu ->
                                        weitereAnzeigen = neu
                                        zustand.editor.führeAus(
                                            KartenAktion.KnotenEigenschaftÄndern(
                                                knoten.id,
                                                "$KARTEN_TABELLEN_PREFIX.weitereAusgänge",
                                                KnotenEigenschaft.Wahrheitswert(neu),
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                        Text(
                            "Zeilen ${seitenStart + BigInteger.ONE}–" +
                                "${seitenStart + BigInteger.valueOf(zeilenAufSeite.toLong())} von $zeilenAnzahl",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (zeilenAnzahl > KARTEN_TABELLEN_SEITENGRÖSSE) {
                            KartenTabellenSeitenNavigation(seitenStart, letzterStart) {
                                seitenStart = it
                            }
                        }
                        KartenTabelle(
                            eingänge = freieLogischeEingänge,
                            freiePrädikate = freiePrädikate.toSet(),
                            ausgänge = ausgänge,
                            zeilen = zeilen,
                            text = ::text,
                            breite = tabellenBreite,
                        )
                    }
                    HorizontalDivider()
                    KartenTabellenAktionen(zustand, knoten, quelle, schließen)
                }
            }
        }
    }
}

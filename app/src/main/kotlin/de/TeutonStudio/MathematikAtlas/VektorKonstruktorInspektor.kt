package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInspektorBereich
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*

internal object VektorKonstruktorInspektor : KnotenInspektor {
    override fun hatBereich(knoten: KnotenDaten, bereich: KnotenInspektorBereich): Boolean = when (bereich) {
        KnotenInspektorBereich.Knotendaten -> true
        KnotenInspektorBereich.Eingangsdaten -> false
        KnotenInspektorBereich.Ausgangsdaten -> true
    }
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) = FachInhalt(knoten, ergebnis, aktionen, true)
    @Composable override fun BereichInhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen, bereich: KnotenInspektorBereich) {
        when (bereich) {
            KnotenInspektorBereich.Knotendaten -> FachInhalt(knoten, ergebnis, aktionen, false)
            KnotenInspektorBereich.Eingangsdaten -> {  }
            KnotenInspektorBereich.Ausgangsdaten -> { VektorKonstruktorAusgang(knoten, ergebnis, aktionen) }
        }
    }

    @Composable
    private fun FachInhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
        mitAnschlüssen: Boolean,
    ) {
        val config = vektorKonstruktorKonfiguration(knoten)
        Text("Erzeugungsart", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard)) {
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
        if (mitAnschlüssen) VektorKonstruktorAusgang(knoten, ergebnis, aktionen)

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
    override val hauptBereich get() = KnotenInspektorBereich.Ausgangsdaten
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        InspektorAusgangsEinstellungen(knoten, ergebnis, aktionen::knoten)
    }
}

@Composable
private fun VektorKonstruktorAusgang(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
    de.TeutonStudio.MathematikKnoten.InspektorAusgangsEinstellungen(knoten, ergebnis, aktionen::knoten)
}

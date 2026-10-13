package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*

object ÜbersetzerInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        Text("Übersetzungsziel", style = MaterialTheme.typography.titleSmall)
        if (übersetzerZiel(knoten) in setOf("polar", "kartesisch")) {
            Row { Text("Radius und Winkel getrennt"); Switch(checked = übersetzerPolarGetrennt(knoten), onCheckedChange = { getrennt -> aktionen.knoten(konfiguriereÜbersetzerPolarAnschlüsse(knoten, getrennt)) }) }
        }
        Column {
            ÜBERSETZER_ZIELE.forEach { ziel ->
                TextButton(onClick = { aktionen.knoten(konfiguriereÜbersetzer(knoten, ziel)) }) {
                    Text(if (ziel == übersetzerZiel(knoten)) "✓ ${übersetzerZielTitel(ziel)}" else übersetzerZielTitel(ziel))
                }
            }
        }
        if (übersetzerZiel(knoten) == "matrixzerlegen") {
            Text("Zerlegung", style = MaterialTheme.typography.titleSmall)
            Row {
                de.TeutonStudio.MathematikRechenSystem.kern.MatrixZerlegeRichtung.entries.forEach { richtung ->
                    FilterChip(selected = (knoten.parameter[MATRIX_ZERLEGEN_RICHTUNG] ?: "ZEILEN") == richtung.name,
                        onClick = { aktionen.parameter(MATRIX_ZERLEGEN_RICHTUNG, richtung.name) },
                        label = { Text(if (richtung.name == "ZEILEN") "Zeilen" else "Spalten") })
                }
            }
            Row {
                listOf("vektor" to "Vektoren", "tupel" to "Tupel").forEach { (id, titel) ->
                    FilterChip(selected = (knoten.parameter["matrixZerlegen.ausgabe"] ?: "vektor") == id,
                        onClick = { aktionen.parameter("matrixZerlegen.ausgabe", id) }, label = { Text(titel) })
                }
            }
        }
        if (übersetzerZiel(knoten) == "koordinatenmenge") {
            Text("Objektanker (Zielpunkt unverbunden: Ursprung)")
            listOf("keiner" to "Koordinaten erhalten", "punkt" to "Objektpunkt wählen", "mittelpunkt" to "Mittelpunkt").forEach { (id, titel) ->
                TextButton(onClick = { aktionen.parameter("geometrie.anker", id) }) { Text(if ((knoten.parameter["geometrie.anker"] ?: "keiner") == id) "✓ $titel" else titel) }
            }
        }
        if (übersetzerZiel(knoten) in setOf("polar", "kartesisch")) {
            de.TeutonStudio.MathematikRechenSystem.kern.PolarKonvention.entries.forEach { konvention ->
                FilterChip(selected=übersetzerPolarKonvention(knoten)==konvention,onClick={aktionen.parameter(ÜBERSETZER_POLAR_KONVENTION,konvention.name)},label={Text(if(konvention==de.TeutonStudio.MathematikRechenSystem.kern.PolarKonvention.SIGNED) "Vorzeichen und Halbkreiswinkel" else "Klassische Azimutkonvention")})
            }
            Text(if(übersetzerPolarKonvention(knoten)==de.TeutonStudio.MathematikRechenSystem.kern.PolarKonvention.SIGNED) "Wn = {−1,+1} × [0°,180°)ⁿ" else de.TeutonStudio.MathematikRechenSystem.kern.Polarkoordinaten.KONVENTION)
        }
        if (übersetzerZiel(knoten) == "bogenmass") {
            Text("Hauptvertreter im Bogenmaß")
            de.TeutonStudio.MathematikRechenSystem.kern.WinkelVertreter.entries.forEach { vertreter ->
                TextButton(onClick = { aktionen.knoten(konfiguriereÜbersetzerVertreter(knoten, vertreter)) }) {
                    if (übersetzerVertreter(knoten) == vertreter) Text("✓ ")
                    LatexText(vertreter.menge.zuLatex())
                }
            }
        }
        if (übersetzerZiel(knoten) == "kartesisch") {
            Row {
                Checkbox(checked = übersetzerEindimensional(knoten), onCheckedChange = {
                    aktionen.knoten(konfiguriereÜbersetzerEindimensional(knoten, it))
                })
                Text("R¹: (Radius, Vorzeichen)")
            }
        }
    }
}

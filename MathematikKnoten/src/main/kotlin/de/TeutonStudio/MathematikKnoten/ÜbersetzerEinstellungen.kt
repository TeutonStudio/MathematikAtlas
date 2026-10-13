package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

@Composable
fun ÜbersetzerEinstellungen(knoten:KnotenDaten,ergebnis:KnotenAuswertungsErgebnis?,ändern:(KnotenDaten)->Unit) {
    fun parameter(name:String,wert:String) = ändern(knoten.copy(parameter=knoten.parameter+(name to wert)))
        Text("Übersetzungsziel", style = MaterialTheme.typography.titleSmall)
        if (übersetzerZiel(knoten) in setOf("polar", "kartesisch")) {
            Row { Text("Radius und Winkel getrennt"); Switch(checked = übersetzerPolarGetrennt(knoten), onCheckedChange = { getrennt -> ändern(konfiguriereÜbersetzerPolarAnschlüsse(knoten, getrennt)) }) }
        }
        Column {
            ÜBERSETZER_ZIELE.forEach { ziel ->
                TextButton(onClick = { ändern(konfiguriereÜbersetzer(knoten, ziel)) }) {
                    Text(if (ziel == übersetzerZiel(knoten)) "✓ ${übersetzerZielTitel(ziel)}" else übersetzerZielTitel(ziel))
                }
            }
        }
        if (übersetzerZiel(knoten) == "matrixzerlegen") {
            Text("Zerlegung", style = MaterialTheme.typography.titleSmall)
            Row {
                de.TeutonStudio.MathematikRechenSystem.kern.MatrixZerlegeRichtung.entries.forEach { richtung ->
                    FilterChip(selected = (knoten.parameter[MATRIX_ZERLEGEN_RICHTUNG] ?: "ZEILEN") == richtung.name,
                        onClick = { parameter(MATRIX_ZERLEGEN_RICHTUNG, richtung.name) },
                        label = { Text(if (richtung.name == "ZEILEN") "Zeilen" else "Spalten") })
                }
            }
            Row {
                listOf("vektor" to "Vektoren", "tupel" to "Tupel").forEach { (id, titel) ->
                    FilterChip(selected = (knoten.parameter["matrixZerlegen.ausgabe"] ?: "vektor") == id,
                        onClick = { parameter("matrixZerlegen.ausgabe", id) }, label = { Text(titel) })
                }
            }
        }
        if (übersetzerZiel(knoten) == "koordinatenmenge") {
            Text("Objektanker (Zielpunkt unverbunden: Ursprung)")
            listOf("keiner" to "Koordinaten erhalten", "punkt" to "Objektpunkt wählen", "mittelpunkt" to "Mittelpunkt").forEach { (id, titel) ->
                TextButton(onClick = { parameter("geometrie.anker", id) }) { Text(if ((knoten.parameter["geometrie.anker"] ?: "keiner") == id) "✓ $titel" else titel) }
            }
        }
        if (übersetzerZiel(knoten) in setOf("polar", "kartesisch")) {
            de.TeutonStudio.MathematikRechenSystem.kern.PolarKonvention.entries.forEach { konvention ->
                FilterChip(selected=übersetzerPolarKonvention(knoten)==konvention,onClick={parameter(ÜBERSETZER_POLAR_KONVENTION,konvention.name)},label={Text(if(konvention==de.TeutonStudio.MathematikRechenSystem.kern.PolarKonvention.SIGNED) "Vorzeichen und Halbkreiswinkel" else "Klassische Azimutkonvention")})
            }
            Text(if(übersetzerPolarKonvention(knoten)==de.TeutonStudio.MathematikRechenSystem.kern.PolarKonvention.SIGNED) "Wn = {−1,+1} × [0°,180°)ⁿ" else de.TeutonStudio.MathematikRechenSystem.kern.Polarkoordinaten.KONVENTION)
        }
        if (übersetzerZiel(knoten) == "bogenmass") {
            Text("Hauptvertreter im Bogenmaß")
            de.TeutonStudio.MathematikRechenSystem.kern.WinkelVertreter.entries.forEach { vertreter ->
                TextButton(onClick = { ändern(konfiguriereÜbersetzerVertreter(knoten, vertreter)) }) {
                    if (übersetzerVertreter(knoten) == vertreter) Text("✓ ")
                    LatexText(vertreter.menge.zuLatex())
                }
            }
        }
        if (übersetzerZiel(knoten) == "kartesisch") {
            Row {
                Checkbox(checked = übersetzerEindimensional(knoten), onCheckedChange = {
                    ändern(konfiguriereÜbersetzerEindimensional(knoten, it))
                })
                Text("R¹: (Radius, Vorzeichen)")
            }
        }
    if(übersetzerZiel(knoten)=="rueckfuehrung") {
        Text("Rückführungszeugnis",style=MaterialTheme.typography.titleSmall)
        val eingang=ergebnis?.eingänge?.get("wert")
        TextButton(onClick={ändern(konfiguriereRückführungsZeugnis(knoten,null))}) {Text("Aktuelles Eingangszeugnis verwenden")}
        val historie=eingang?.übersetzungen?.herkunft.orEmpty()
        historie.forEachIndexed { i,z ->
            val kompatibel=eingang?.objekt?.let { it as? MathematischesObjekt }?.let { ÜbersetzungsDienst.rückübersetze(it,z) is ÜbersetzungsErgebnis.Erfolg } == true
            TextButton(enabled=kompatibel,onClick={ändern(konfiguriereRückführungsZeugnis(knoten,z))}) {Text("Herkunft ${i+1}: ${z.hinweise().first()}")}
        }
        if(eingang?.übersetzungen?.aktuell?.rückführungsplan==null && historie.none { it.rückführungsplan!=null }) Text("Kein bestätigter Strukturplan verfügbar.")
        knoten.parameter[ÜBERSETZER_RÜCKFÜHRUNGSZEUGNIS]?.let { Text("Ein ausdrücklich gewähltes Herkunftszeugnis ist gespeichert.") }
        ergebnis?.übersetzungsDiagnose?.let {Text(it.beschreibung,color=MaterialTheme.colorScheme.error)}
    }
    RechnerSchemaSchaltfläche(knoten,ergebnis)
}

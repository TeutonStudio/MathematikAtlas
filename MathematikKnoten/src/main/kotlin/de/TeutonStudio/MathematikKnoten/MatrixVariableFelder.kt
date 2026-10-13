package de.TeutonStudio.MathematikKnoten

import androidx.compose.material3.*
import androidx.compose.runtime.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten

/** Gemeinsame Eingabe auf Android und Desktop; die App übernimmt atomar über Kartenaktionen. */
@Composable
fun MatrixVariableFelder(knoten:KnotenDaten,aendern:(KnotenDaten)->Unit) {
    val dimension=knoten.parameter[MATRIX_VARIABLE_DIMENSION]?.toIntOrNull()?:0
    var text by remember(knoten.id,dimension) { mutableStateOf(if(dimension==0) "2" else dimension.toString()) }
    Text(if(dimension==0) "Skalare Unbekannte" else "Matrixunbekannte X · $dimension × $dimension")
    OutlinedTextField(text,{text=it},label={Text("Quadratische Matrixdimension (1–8)")},singleLine=true,isError=text.toIntOrNull() !in 1..8)
    TextButton(onClick={aendern(konfiguriereMatrixVariable(knoten,text.toInt()))},enabled=text.toIntOrNull() in 1..8) { Text("Als Matrixvariable verwenden") }
    if(dimension>0) {
        TextButton(onClick={aendern(konfiguriereMatrixVariable(knoten,0))}) { Text("Als skalare Variable verwenden") }
        Text("Einträge heißen Name_1_1, Name_1_2, …; Reihenfolge zeilenweise. Verbinde die Matrix mit den vorhandenen Matrixoperatoren.")
    }
}

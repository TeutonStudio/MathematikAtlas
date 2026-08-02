package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.GEOMETRIE_TEILOBJEKT_ZELL_ID
import de.TeutonStudio.MathematikKnoten.GeometrieTeilobjektTyp
import de.TeutonStudio.MathematikKnoten.geometrieAnschlussArt
import de.TeutonStudio.MathematikKnoten.geometrieTeilobjekte
import de.TeutonStudio.MathematikRechenSystem.kern.GeometrischeZelle
import de.TeutonStudio.MathematikRechenSystem.kern.GeometrischerAusdruck

@OptIn(ExperimentalMaterial3Api::class)
internal object GeometrieTeilobjektInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val typ = GeometrieTeilobjektTyp.vonKnotenArt(knoten.art) ?: return
        val objekt = ergebnis?.eingänge?.get("objekt")?.objekt as? GeometrischerAusdruck
        val zellen = remember(objekt, typ) {
            objekt?.let { geometrieTeilobjekte(it, typ.dimension) }.orEmpty()
        }
        val gespeicherteId = knoten.parameter[GEOMETRIE_TEILOBJEKT_ZELL_ID].orEmpty()
        val auswahl = zellen.firstOrNull { it.id == gespeicherteId } ?: zellen.firstOrNull()
        val ausgang = knoten.anschlüsse.firstOrNull {
            it.richtung == AnschlussRichtung.Ausgang && it.name == typ.ausgangName
        }

        LaunchedEffect(knoten.id, gespeicherteId, auswahl?.id, auswahl?.geometrie, ausgang?.art) {
            val zelle = auswahl ?: return@LaunchedEffect
            val geometrie = zelle.geometrie ?: return@LaunchedEffect
            if (gespeicherteId != zelle.id) {
                aktionen.parameter(GEOMETRIE_TEILOBJEKT_ZELL_ID, zelle.id)
            }
            val konkreteArt = geometrieAnschlussArt(geometrie)
            if (ausgang != null && ausgang.art != konkreteArt) {
                aktionen.anschlussArt(AnschlussVerweis(knoten.id, ausgang.id), konkreteArt)
            }
        }

        Text("${typ.bezeichnung} auswählen", style = MaterialTheme.typography.titleSmall)
        var geöffnet by remember(knoten.id, typ, gespeicherteId, zellen) { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = geöffnet,
            onExpandedChange = { if (zellen.isNotEmpty()) geöffnet = it },
        ) {
            OutlinedTextField(
                value = auswahl?.let(::zellBezeichnung)
                    ?: if (objekt == null) "Geometrisches Objekt verbinden" else "Keine ${typ.mehrzahl} vorhanden",
                onValueChange = {},
                readOnly = true,
                enabled = zellen.isNotEmpty(),
                label = { Text(typ.bezeichnung) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geöffnet) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = geöffnet, onDismissRequest = { geöffnet = false }) {
                zellen.forEach { zelle ->
                    DropdownMenuItem(
                        text = { Text(zellBezeichnung(zelle)) },
                        onClick = {
                            geöffnet = false
                            aktionen.parameter(GEOMETRIE_TEILOBJEKT_ZELL_ID, zelle.id)
                            val geometrie = zelle.geometrie
                            if (ausgang != null && geometrie != null) {
                                aktionen.anschlussArt(
                                    AnschlussVerweis(knoten.id, ausgang.id),
                                    geometrieAnschlussArt(geometrie),
                                )
                            }
                        },
                    )
                }
            }
        }

        Text(
            "Die Auswahl wird über die stabile Zell-ID gespeichert. Der Ausgang übernimmt den konkreten Geometrietyp.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.warnungen.orEmpty().forEach { warnung ->
            Text(warnung, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        }
        ergebnis?.fehler?.let { fehler ->
            Text(fehler, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    private fun zellBezeichnung(zelle: GeometrischeZelle): String =
        "${zelle.id}: ${zelle.geometrie?.zuLatex().orEmpty()}"
}

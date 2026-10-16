package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*

/** Dieselben normierten Eckpunkte wie in der fachneutralen Treffergeometrie. */
@Composable
internal fun knotenKörperForm(knoten: KnotenDaten): Shape = when (knoten.darstellungsForm) {
    KnotenDarstellungsForm.Rechteck -> MaterialTheme.shapes.medium
    KnotenDarstellungsForm.Adapter -> RoundedCornerShape(adapterEckenRadius(knoten.größe.breite, knoten.größe.höhe).dp)
    KnotenDarstellungsForm.Ellipse -> GenericShape { size, _ -> addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)) }
    KnotenDarstellungsForm.Dreieck -> GenericShape { size, _ ->
        moveTo(size.width / 2f, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close()
    }
    KnotenDarstellungsForm.Raute -> GenericShape { size, _ ->
        moveTo(size.width / 2f, 0f); lineTo(size.width, size.height / 2f)
        lineTo(size.width / 2f, size.height); lineTo(0f, size.height / 2f); close()
    }
}

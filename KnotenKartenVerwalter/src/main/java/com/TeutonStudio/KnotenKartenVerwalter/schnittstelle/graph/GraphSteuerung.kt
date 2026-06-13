package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

interface GraphSteuerung {

    var aktuell: Int

    val verlauf: SnapshotStateMap<Int, Any>

    /**
     * Fügt eine Aktion hinter dem aktuellen Verlaufspunkt ein.
     *
     * Aktionen nach dem aktuellen Punkt werden entfernt, da sie nach
     * einer neuen Aktion nicht mehr wiederholt werden können.
     */
    fun neueAktion(
        aktion: Any,
    ) {
        verlauf.keys
            .filter { it > aktuell }
            .forEach {
                verlauf.remove(it)
            }

        aktuell += 1
        verlauf[aktuell] = aktion
    }

    /**
     * Vertikale Kontrollleiste für die Kartenansicht.
     */
    @Composable
    fun Karte.zuSteuerung(
        modifier: Modifier = Modifier,
    ) {
        if (!zustand.zeigeKontrollLeiste) return

        val zoomFaktor = 1.2f

        fun viewportMitte(): Offset = Offset(
            x = zustand.dimension.width / 2f,
            y = zustand.dimension.height / 2f,
        )

        Card(
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement =
                    Arrangement.spacedBy(4.dp),
            ) {
                KontrollKnopf(
                    text = "+",
                    beschreibung = "Hineinzoomen",
                    onClick = {
                        zustand.setzeZoom(
                            neuerZoom =
                                zustand.zoom * zoomFaktor,
                            fokus = viewportMitte(),
                        )
                    },
                )

                KontrollKnopf(
                    text = "−",
                    beschreibung = "Herauszoomen",
                    onClick = {
                        zustand.setzeZoom(
                            neuerZoom =
                                zustand.zoom / zoomFaktor,
                            fokus = viewportMitte(),
                        )
                    },
                )

                KontrollKnopf(
                    text = "−>",
                    beschreibung = "Wiederholen",
                    onClick = {
                        TODO("Noch nicht implementiert")
                    },
                )

                KontrollKnopf(
                    text = "<−",
                    beschreibung = "Rückgängig",
                    onClick = {
                        TODO("Noch nicht implementiert")
                    },
                )

                KontrollKnopf(
                    text = "□",
                    beschreibung = "Auf Inhalt zoomen",
                    onClick = {
                        passeInhaltEin()
                    },
                )
            }
        }
    }
}

@Composable
private fun KontrollKnopf(
    text: String, // TODO zu Icons
    beschreibung: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(
                RoundedCornerShape(6.dp),
            )
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(
                onClick = onClick,
            )
            .semantics {
                contentDescription = beschreibung
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}
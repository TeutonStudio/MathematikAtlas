package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt.Vergrößerbar.VergrößerBereich

typealias GraphDatenObjektInspektor<D> = GraphDatenObjekt.Inspektor<D>

private val gespeicherteInspektorBreitePx = mutableFloatStateOf(0f)

interface GraphDatenObjektInspektorBasis<D: GraphDaten> {
    public val daten: D

    public val inspektorName: String
        get() = (daten as? GraphDaten.benanntesGD)?.name ?: daten.id

    @Composable
    public fun Inhalt()

    @Composable
    public fun Composable(modifier: Modifier = Modifier) {
        CardColumn(modifier) {
            KopfZeile()
            Inhalt()
        }
    }

    @Composable
    public fun CardColumn(
        modifier: Modifier = Modifier,
        inhalt: @Composable ColumnScope.() -> Unit,
    ) {
        var breitePx by gespeicherteInspektorBreitePx
        val dichte = LocalDensity.current
        val minBreite = 280.dp
        val maxBreite = 720.dp
        val minHöhe = 120.dp
        val startBreite = 420.dp
        val aktuelleBreite = with(dichte) {
            breitePx
                .takeIf { it > 0f }
                ?.toDp()
                ?.coerceIn(minBreite, maxBreite)
                ?: startBreite
        }
        val cardModifier = Modifier.width(aktuelleBreite).heightIn(min = minHöhe)

        Box(
            modifier
                .padding(start = 12.dp, top = 12.dp, end = 28.dp, bottom = 12.dp)
                .width(aktuelleBreite + 16.dp)
        ) {
            Card(
                cardModifier.onSizeChanged {
                    if (breitePx <= 0f) breitePx = it.width.toFloat()
                }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = inhalt,
                )
            }

            InspektorGriffe(
                aktuelleBreite = breitePx,
                minBreite = minBreite,
                maxBreite = maxBreite,
                beiÄnderung = { neueBreite ->
                    breitePx = neueBreite
                },
            )
        }
    }

    @Composable
    private fun BoxScope.InspektorGriffe(
        aktuelleBreite: Float,
        minBreite: Dp,
        maxBreite: Dp,
        beiÄnderung: (breite: Float) -> Unit,
    ) {
        listOf(
            VergrößerBereich.Links,
        ).forEach { bereich ->
            val länge = if (bereich.istEcke) 14.dp else 26.dp
            val dicke = if (bereich.istEcke) 14.dp else 8.dp
            val farbe = Color(0xFF2563EB)
            val modifier = Modifier
                .align(bereich.ausrichtung)
                .then(
                    if (bereich.istVertikal) {
                        Modifier.width(dicke).height(länge)
                    } else {
                        Modifier.width(länge).height(dicke)
                    }
                )
                .background(farbe, CircleShape)
                .zIndex(4f)
                .pointerInput(bereich) {
                    var startBreite = 0f
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (startBreite <= 0f) {
                            startBreite = aktuelleBreite.takeIf { it > 0f } ?: this.size.width.toFloat()
                        }
                        val minBreitePx = minBreite.toPx()
                        val maxBreitePx = maxBreite.toPx()
                        val neueBreite = when {
                            bereich.links -> startBreite - dragAmount.x
                            else -> startBreite
                        }.coerceIn(minBreitePx, maxBreitePx)
                        startBreite = neueBreite
                        beiÄnderung(neueBreite)
                    }
                }

            Box(modifier)
        }
    }

    @Composable
    public fun KopfZeile() {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = inspektorName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = daten.id,
                color = Color.Gray,
                fontSize = 12.sp,
            )
        }
    }
}

public class BasisObjektKontext(
    private vararg val zeilen: GraphDatenObjektInspektorZeile,
) {
    @Composable
    public fun Inhalt() {
        zeilen.forEach { it.Composable() }
    }

    @Composable
    public fun CardColumn(
        inspektor: GraphDatenObjektInspektor<*>,
        modifier: Modifier = Modifier,
    ) {
        with(inspektor) {
            CardColumn(modifier) {
                KopfZeile()
                this@BasisObjektKontext.Inhalt()
            }
        }
    }
}

public sealed interface GraphDatenObjektInspektorZeile {
    @Composable
    public fun Composable()
}

public data class InfoZeile(
    val name: String,
    val wert: String,
) : GraphDatenObjektInspektorZeile {
    @Composable
    override fun Composable() {
        InspektorZeile(name) {
            Text(wert)
        }
    }
}

public class InhaltZeile(
    val name: String,
    val inhalt: @Composable RowScope.() -> Unit,
) : GraphDatenObjektInspektorZeile {
    @Composable
    override fun Composable() {
        InspektorZeile(name, inhalt)
    }
}

public data class AktionZeile(
    val name: String,
    val aktion: String,
    val beiKlick: () -> Unit,
) : GraphDatenObjektInspektorZeile {
    @Composable
    override fun Composable() {
        InspektorZeile(name) {
            TextButton(onClick = beiKlick) {
                Text(aktion)
            }
        }
    }
}

public data class AuswahlZeile(
    val name: String,
    val wert: String,
    val optionen: List<String>,
    val beiAuswahl: (String) -> Unit,
) : GraphDatenObjektInspektorZeile {
    @Composable
    override fun Composable() {
        var geöffnet by remember { mutableStateOf(false) }
        var auswahl by remember(wert) { mutableStateOf(wert) }

        InspektorZeile(name) {
            TextButton(onClick = { geöffnet = true }) {
                Text(auswahl)
            }
            DropdownMenu(
                expanded = geöffnet,
                onDismissRequest = { geöffnet = false },
            ) {
                optionen.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            geöffnet = false
                            auswahl = option
                            beiAuswahl(option)
                        },
                    )
                }
            }
        }
    }
}

public data class DefinitionZeile(
    val name: String,
    val wert: String,
    val beiÄnderung: (String) -> Unit,
) : GraphDatenObjektInspektorZeile {
    @Composable
    override fun Composable() {
        InspektorZeile(name) {
            OutlinedTextField(
                value = wert,
                onValueChange = beiÄnderung,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InspektorZeile(
    name: String,
    inhalt: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            color = Color.DarkGray,
        )
        Row(
            modifier = Modifier.weight(2f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = inhalt,
        )
    }
}

public fun info(name: String, wert: Any?): InfoZeile =
    InfoZeile(name, wert?.toString().orEmpty())

public fun zeile(
    name: String,
    inhalt: @Composable RowScope.() -> Unit,
): InhaltZeile = InhaltZeile(name, inhalt)

public fun aktion(name: String, aktion: String, beiKlick: () -> Unit): AktionZeile =
    AktionZeile(name, aktion, beiKlick)

public fun auswahl(
    name: String,
    wert: String,
    optionen: List<String>,
    beiAuswahl: (String) -> Unit,
): AuswahlZeile = AuswahlZeile(name, wert, optionen, beiAuswahl)

public fun definition(
    name: String,
    wert: String,
    beiÄnderung: (String) -> Unit,
): DefinitionZeile = DefinitionZeile(name, wert, beiÄnderung)

package de.TeutonStudio.MathematikAtlas.knoten.AussageKnoten

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.InhaltZeile
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.zeile

const val LATEX_REKURSIV_SCHLUESSEL = "latex-rekursiv"

fun GraphDatenKnoten.latexRekursiv(): Boolean =
    data[LATEX_REKURSIV_SCHLUESSEL] as? Boolean ?: true

fun GraphDatenKnoten.setzeLatexRekursiv(rekursiv: Boolean) {
    data[LATEX_REKURSIV_SCHLUESSEL] = rekursiv
}

@Composable
fun LaTeXFormelText(
    formel: String,
    karte: GraphDatenKarte,
) {
    val maximaleBreite = with(LocalDensity.current) {
        ((karte.breite.takeIf { it > 0f } ?: 600f) * 2f / 3f).toDp()
    }

    Latex(
        latex = formel,
        modifier = Modifier
            .widthIn(max = maximaleBreite)
            .horizontalScroll(rememberScrollState()),
        config = LatexConfig(
            fontSize = 16.sp,
            theme = LatexTheme.light(
                color = MaterialTheme.colorScheme.onSurface,
                backgroundColor = Color.Transparent,
            ),
            accessibilityEnabled = true,
        ),
    )
}

fun latexInfo(
    name: String,
    karte: GraphDatenKarte,
    formel: String,
): InhaltZeile = zeile(name) {
    LaTeXFormelText(
        formel = formel,
        karte = karte,
    )
}

@Composable
fun LaTeXModusSchalter(knoten: GraphDatenKnoten) {
    var rekursiv by remember(knoten.id) { mutableStateOf(knoten.latexRekursiv()) }

    Row {
        Text(if (rekursiv) "rekursiv" else "implizit")
        Switch(
            checked = rekursiv,
            onCheckedChange = {
                rekursiv = it
                knoten.setzeLatexRekursiv(it)
            },
        )
    }
}

fun GraphDatenKarte.latexFormelFuer(
    knoten: GraphDatenKnoten,
    rekursiv: Boolean = knoten.latexRekursiv(),
): String =
    latexFormelFuer(knoten, rekursiv, emptySet())

private fun GraphDatenKarte.latexFormelFuer(
    knoten: GraphDatenKnoten,
    rekursiv: Boolean,
    besucht: Set<GraphDatenId>,
): String {
    if (knoten.id in besucht) return "\\circlearrowleft"

    return when (knoten) {
        is AussageDefinition ->
            knoten.latexName().ifBlank {
                if (knoten.data[definition.WERT_SCHLÜSSEL] as? Boolean ?: true) "\\top" else "\\bot"
            }

        is OperatorDaten -> {
            val eingänge = knoten.anschlüsse
                .filterIsInstance<GraphDatenAnschluss.auswertbarerGDA>()
                .filter { it.istEingang }
                .sortedBy { knoten.anschlussIdx[it.id] ?: Int.MAX_VALUE }
            val argumente = eingänge.mapIndexed { index, eingang ->
                if (rekursiv) {
                    val quelle = quelleFuer(knoten.id, eingang.id)
                    if (quelle == null) eingangsName(index)
                    else latexFormelFuer(quelle.first, rekursiv, besucht + knoten.id)
                } else {
                    eingangsName(index)
                }
            }

            knoten.aussagenVerknüpfung().latex(argumente)
        }

        is AussageAuswerten -> {
            val hauptEingang = knoten.hauptEingang()
            if (rekursiv && hauptEingang != null) {
                val quelle = quelleFuer(knoten.id, hauptEingang.id)
                if (quelle == null) eingangsName(0)
                else latexFormelFuer(quelle.first, rekursiv, besucht + knoten.id)
            } else {
                eingangsName(0)
            }
        }

        else -> knoten.name.ifBlank { knoten.id }
    }
}

private fun GraphDatenKarte.quelleFuer(
    knotenId: GraphDatenId,
    anschlussId: GraphDatenId,
): Pair<GraphDatenKnoten, GraphDatenAnschluss>? {
    val quelle = verbindungen
        .asSequence()
        .mapNotNull { it.andereSeiteVon(knotenId, anschlussId) }
        .firstOrNull { (quellKnotenId, quellAnschlussId) ->
            val quellKnoten = this.knoten.find { it.id == quellKnotenId }
            val quellAnschluss = quellKnoten
                ?.anschlüsse
                ?.find { it.id == quellAnschlussId }
            quellAnschluss is GraphDatenAnschluss.auswertbarerGDA && quellAnschluss.istAusgang
        }
        ?: return null

    val quellKnoten = knoten.find { it.id == quelle.first } ?: return null
    val quellAnschluss = quellKnoten.anschlüsse.find { it.id == quelle.second } ?: return null
    return quellKnoten to quellAnschluss
}

private fun GraphDatenVerbindung.andereSeiteVon(
    knotenId: GraphDatenId,
    anschlussId: GraphDatenId,
): Pair<GraphDatenId, GraphDatenId>? =
    when {
        ids.knotenIdWeib == knotenId && ids.anschlussIdWeib == anschlussId ->
            ids.knotenIdMann to ids.anschlussIdMann

        ids.knotenIdMann == knotenId && ids.anschlussIdMann == anschlussId ->
            ids.knotenIdWeib to ids.anschlussIdWeib

        else -> null
    }

private fun eingangsName(index: Int): String = "A_{${index + 1}}"

private fun GraphDatenKnoten.latexName(): String =
    if (this is AussageDefinition && name == "Aussage") {
        id.substringAfterLast("-").uppercase()
    } else {
        name
    }

private fun operator.AussagenVerknüpfung.latex(argumente: List<String>): String =
    when (this) {
        operator.AussagenVerknüpfung.UND ->
            argumente.joinToString(" \\land ").eingeklammert()

        operator.AussagenVerknüpfung.ODER ->
            argumente.joinToString(" \\lor ").eingeklammert()

        operator.AussagenVerknüpfung.IMPLIKATION ->
            "${argumente.getOrElse(0) { eingangsName(0) }} \\Rightarrow ${argumente.getOrElse(1) { eingangsName(1) }}".eingeklammert()

        operator.AussagenVerknüpfung.KONTRAJUNKTION ->
            argumente.joinToString(" \\veebar ").eingeklammert()

        operator.AussagenVerknüpfung.NEGATION ->
            "\\lnot ${argumente.getOrElse(0) { eingangsName(0) }}".eingeklammert()
    }

private fun String.eingeklammert(): String = "\\left($this\\right)"

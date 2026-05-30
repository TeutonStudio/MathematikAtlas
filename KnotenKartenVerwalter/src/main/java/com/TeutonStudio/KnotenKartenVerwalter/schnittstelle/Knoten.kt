package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.ZahlenTyp
import com.TeutonStudio.KnotenKartenVerwalter.daten.Zahlenraum

public fun interface KnotenFabrik {
    public fun erstelle(daten: KnotenDaten): Knoten
}

/**
 * Registry wie ReactFlows `nodeTypes`: `KnotenDaten.knotenArt` entscheidet,
 * welche Knotenklasse und damit welche Anschlüsse verwendet werden.
 */
data class KnotenArten(
    private val fabriken: Map<String, KnotenFabrik> = standardFabriken,
) {
    public fun erstelle(daten: KnotenDaten): Knoten =
        (fabriken[daten.art] ?: fabriken.getValue(BasisKnoten.KNOTEN_ART)).erstelle(daten)

    public fun mit(art: String, fabrik: KnotenFabrik): KnotenArten =
        copy(fabriken = fabriken + (art to fabrik))

    public companion object {
        private val standardFabriken = mapOf(
            BasisKnoten.KNOTEN_ART to KnotenFabrik(::BasisKnoten),
            EingabeKnoten.KNOTEN_ART to KnotenFabrik(::EingabeKnoten),
            AusgabeKnoten.KNOTEN_ART to KnotenFabrik(::AusgabeKnoten),
            MathematikEingabeKnoten.KNOTEN_ART to KnotenFabrik(::MathematikEingabeKnoten),
            UnbekannteKnoten.KNOTEN_ART to KnotenFabrik(::UnbekannteKnoten),
            RechenKnoten.KNOTEN_ART to KnotenFabrik(::RechenKnoten),
            FormelKnoten.KNOTEN_ART to KnotenFabrik(::FormelKnoten),
            AuswertungsKnoten.KNOTEN_ART to KnotenFabrik(::AuswertungsKnoten),
            FunktionKnoten.KNOTEN_ART to KnotenFabrik(::FunktionKnoten),
        )

        public val Standard: KnotenArten = KnotenArten()
    }
}

/**
 * Rendert einen Knoten als Compose-Baustein.
 *
 * `modifierKnoten` positioniert und skaliert den gesamten Knoten. Über
 * `modifierAnschluss` kann die Karte jeden Anschluss zusätzlich mit
 * Pointer-Interaktion versehen.
 */
@Composable
public fun KnotenDaten.zuComposable(
    modifierKnoten: Modifier = Modifier,
    modifierAnschluss: (AnschlussRichtung, Int) -> Modifier = { _, _ -> AnschlussModifier },
    inhaltSkalierung: Float = 1f,
) = BasisKnoten(this).zuComposable(modifierKnoten, modifierAnschluss, inhaltSkalierung)


sealed interface Knoten: GraphObjekt {
    public val daten: KnotenDaten
    public val eingänge: Map<Int, Anschluss>
    public val ausgänge: Map<Int, Anschluss>

    public fun erhalteAnschlüsseGeordnet(): List<AnschlussDaten>
    public fun erhalteAnschlüsseGeordnet(richtung: AnschlussRichtung): List<AnschlussDaten>

    @Composable
    public fun zuComposable(
        modifierKnoten: Modifier = Modifier,
        modifierAnschluss: (AnschlussRichtung, Int) -> Modifier = { _, _ -> AnschlussModifier },
        inhaltSkalierung: Float = 1f,
    )
}

open class BasisKnoten(
    override val daten: KnotenDaten,
): Knoten {
    protected open val eingangsDaten: List<EingangDaten> = listOf(EingangDaten("in", "Eingang"))
    protected open val ausgangsDaten: List<AusgangDaten> = listOf(AusgangDaten("out", "Ausgang"))

    override val eingänge: Map<Int, Anschluss> by lazy {
        eingangsDaten.mapIndexed { index, anschluss ->
            index to BasisEingang(anschluss, this) as Anschluss
        }.toMap()
    }

    override val ausgänge: Map<Int, Anschluss> by lazy {
        ausgangsDaten.mapIndexed { index, anschluss ->
            index to BasisAusgang(anschluss, this) as Anschluss
        }.toMap()
    }

    override fun erhalteAnschlüsseGeordnet(): List<AnschlussDaten> = eingangsDaten + ausgangsDaten

    override fun erhalteAnschlüsseGeordnet(richtung: AnschlussRichtung): List<AnschlussDaten> = when (richtung) {
        AnschlussRichtung.Eingang -> eingangsDaten
        AnschlussRichtung.Ausgang -> ausgangsDaten
    }

    @Composable
    override fun zuComposable(modifier: Modifier) {
        zuComposable(modifierKnoten = modifier, modifierAnschluss = { _, _ -> AnschlussModifier })
    }

    @Composable
    override fun zuComposable(
        modifierKnoten: Modifier,
        modifierAnschluss: (AnschlussRichtung, Int) -> Modifier,
        inhaltSkalierung: Float,
    ) {
        Inhalt(modifierKnoten, modifierAnschluss, inhaltSkalierung)
    }

    @Composable
    protected open fun Inhalt(
        modifierKnoten: Modifier,
        modifierAnschluss: (AnschlussRichtung, Int) -> Modifier,
        inhaltSkalierung: Float,
    ) {
        KnotenRahmen(this, modifierKnoten, modifierAnschluss, inhaltSkalierung)
    }

    public companion object {
        public const val KNOTEN_ART: String = "default"
    }
}

open class EingabeKnoten(daten: KnotenDaten): BasisKnoten(daten) {
    override val eingangsDaten: List<EingangDaten> = emptyList()

    public companion object {
        public const val KNOTEN_ART: String = "eingabe"
    }
}

open class AusgabeKnoten(daten: KnotenDaten): BasisKnoten(daten) {
    override val ausgangsDaten: List<AusgangDaten> = emptyList()

    public companion object {
        public const val KNOTEN_ART: String = "ausgabe"
    }
}

open class MathematikEingabeKnoten(daten: KnotenDaten) : BasisKnoten(daten) {
    override val eingangsDaten: List<EingangDaten> = emptyList()
    override val ausgangsDaten: List<AusgangDaten> = listOf(AusgangDaten("wert", "Wert", zahlenTyp = daten.zahlenTypOderDefault()))

    public companion object {
        public const val KNOTEN_ART: String = "mathe-eingabe"
    }
}

open class UnbekannteKnoten(daten: KnotenDaten) : BasisKnoten(daten) {
    override val eingangsDaten: List<EingangDaten> = emptyList()
    override val ausgangsDaten: List<AusgangDaten> = listOf(AusgangDaten("variable", "Variable", zahlenTyp = daten.zahlenTypOderDefault()))

    public companion object {
        public const val KNOTEN_ART: String = "unbekannte"
    }
}

open class RechenKnoten(daten: KnotenDaten) : BasisKnoten(daten) {
    override val eingangsDaten: List<EingangDaten> = listOf(
        EingangDaten("links", "Links", zahlenTyp = daten.zahlenTypOderDefault()),
        EingangDaten("rechts", "Rechts", zahlenTyp = daten.zahlenTypOderDefault()),
    )
    override val ausgangsDaten: List<AusgangDaten> = listOf(AusgangDaten("ergebnis", "Ergebnis", zahlenTyp = daten.zahlenTypOderDefault()))

    public companion object {
        public const val KNOTEN_ART: String = "rechen"
    }
}

open class FormelKnoten(daten: KnotenDaten) : BasisKnoten(daten) {
    override val eingangsDaten: List<EingangDaten> = listOf(EingangDaten("in", "Eingabe", zahlenTyp = daten.zahlenTypOderDefault()))
    override val ausgangsDaten: List<AusgangDaten> = listOf(AusgangDaten("formel", "Formel", zahlenTyp = daten.zahlenTypOderDefault()))

    public companion object {
        public const val KNOTEN_ART: String = "formel"
    }
}

open class AuswertungsKnoten(daten: KnotenDaten) : BasisKnoten(daten) {
    override val eingangsDaten: List<EingangDaten> = listOf(EingangDaten("in", "Eingabe", zahlenTyp = daten.zahlenTypOderDefault()))
    override val ausgangsDaten: List<AusgangDaten> = listOf(AusgangDaten("ergebnis", "Ergebnis", zahlenTyp = daten.zahlenTypOderDefault()))

    public companion object {
        public const val KNOTEN_ART: String = "auswertung"
    }
}

open class FunktionKnoten(daten: KnotenDaten) : BasisKnoten(daten) {
    override val eingangsDaten: List<EingangDaten> = listOf(EingangDaten("argument", "Argument", zahlenTyp = daten.zahlenTypOderDefault()))
    override val ausgangsDaten: List<AusgangDaten> = listOf(AusgangDaten("wert", "Wert", zahlenTyp = daten.zahlenTypOderDefault()))

    public companion object {
        public const val KNOTEN_ART: String = "funktion"
    }
}

/**
 * Standarddarstellung eines Knotens.
 *
 * Der Inhalt bleibt innerhalb des Rahmens, während die Anschlüsse links und
 * rechts auf dem Rahmen liegen.
 */
@Composable
private fun KnotenRahmen(
    knoten: Knoten,
    modifierKnoten: Modifier = Modifier,
    modifierAnschluss: (AnschlussRichtung, Int) -> Modifier = { _, _ -> AnschlussModifier },
    inhaltSkalierung: Float = 1f,
) {
    val daten = knoten.daten
    val randFarbe = if (daten.ausgewaehlt) Color(0xFF2563EB) else Color(0xFF64748B)
    val skalierung = inhaltSkalierung.coerceAtLeast(0.1f)
    val form = RoundedCornerShape((8f * skalierung).dp)
    Box(
        modifier = modifierKnoten
            .border((1f * skalierung).dp, randFarbe, form)
            .background(Color.White, form),
    ) {
        // Der eigentliche Textinhalt bekommt seitlichen Abstand, damit er nicht
        // unter den auf dem Rahmen liegenden Anschlüssen liegt.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (18f * skalierung).dp, vertical = (10f * skalierung).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = daten.name,
                    style = TextStyle(color = Color(0xFF0F172A), fontSize = (14f * skalierung).sp),
                )
                BasicText(
                    text = daten.mathematischeKurzform() ?: daten.typ,
                    style = TextStyle(color = Color(0xFF64748B), fontSize = (14f * skalierung).sp),
                )
            }
        }

        AnschlussLeisteAmRand(
            kante = AnschlussKante.Links,
            modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().offset(x = (-5f * skalierung).dp),
            anschlüsse = knoten.erhalteAnschlüsseGeordnet().filter { it.kante == AnschlussKante.Links },
            modifierAnschluss = modifierAnschluss,
            knoten = knoten,
        )
        AnschlussLeisteAmRand(
            kante = AnschlussKante.Rechts,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().offset(x = (5f * skalierung).dp),
            anschlüsse = knoten.erhalteAnschlüsseGeordnet().filter { it.kante == AnschlussKante.Rechts },
            modifierAnschluss = modifierAnschluss,
            knoten = knoten,
        )
        AnschlussLeisteAmRand(
            kante = AnschlussKante.Oben,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().offset(y = (-5f * skalierung).dp),
            anschlüsse = knoten.erhalteAnschlüsseGeordnet().filter { it.kante == AnschlussKante.Oben },
            modifierAnschluss = modifierAnschluss,
            knoten = knoten,
        )
        AnschlussLeisteAmRand(
            kante = AnschlussKante.Unten,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().offset(y = (5f * skalierung).dp),
            anschlüsse = knoten.erhalteAnschlüsseGeordnet().filter { it.kante == AnschlussKante.Unten },
            modifierAnschluss = modifierAnschluss,
            knoten = knoten,
        )
    }
}

/**
 * Positioniert Anschlüsse gleichmäßig an einer Knotenkante.
 */
@Composable
private fun AnschlussLeisteAmRand(
    kante: AnschlussKante,
    modifier: Modifier,
    anschlüsse: List<AnschlussDaten>,
    modifierAnschluss: (AnschlussRichtung, Int) -> Modifier,
    knoten: Knoten,
) {
    if (anschlüsse.isEmpty()) return
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (kante == AnschlussKante.Links || kante == AnschlussKante.Rechts) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                anschlüsse.forEach { anschluss ->
                    anschluss.zuPfad(modifierAnschluss(anschluss.richtung, knoten.indexVon(anschluss)))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                anschlüsse.forEach { anschluss ->
                    anschluss.zuPfad(modifierAnschluss(anschluss.richtung, knoten.indexVon(anschluss)))
                }
            }
        }
    }
}

private fun Knoten.indexVon(anschluss: AnschlussDaten): Int =
    erhalteAnschlüsseGeordnet(anschluss.richtung).indexOfFirst { it.id == anschluss.id }.coerceAtLeast(0)

private fun KnotenDaten.zahlenTypOderDefault(): ZahlenTyp =
    data["zahlenTyp"] as? ZahlenTyp ?: ZahlenTyp(Zahlenraum.Reell)

private fun KnotenDaten.mathematischeKurzform(): String? {
    (data["kurzform"] as? String)?.takeIf { it.isNotBlank() }?.let { return it }
    val typ = data["zahlenTyp"] as? ZahlenTyp
    return when (art) {
        MathematikEingabeKnoten.KNOTEN_ART -> typ?.copy(wert = data["wert"] as? String ?: typ.wert)?.kurzform
        UnbekannteKnoten.KNOTEN_ART -> typ?.copy(anzeigename = data["variable"] as? String ?: name)?.kurzform
        RechenKnoten.KNOTEN_ART -> data["operator"] as? String ?: "?"
        FormelKnoten.KNOTEN_ART -> data["formel"] as? String ?: typ?.kurzform
        AuswertungsKnoten.KNOTEN_ART -> data["status"] as? String ?: "Auswertung"
        FunktionKnoten.KNOTEN_ART -> typ?.kurzform ?: data["funktion"] as? String
        else -> typ?.kurzform
    }
}

/**
 * Vorschau der Standard-Knotendarstellung.
 */
@Preview
@Composable
private fun KnotenPreview() {
    val daten = KnotenDaten(
        id = "knoten-1",
        name = "Ableitung",
    )
    daten.zuComposable(modifierAnschluss = { _, _ -> AnschlussModifier })
}

package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten

// Daten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten

// Composable
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.KnotenRahmen

/*public fun interface KnotenFabrik {
    public fun erstelle(daten: KnotenDaten, anschlüsse: KnotenAschlüsse): Knoten
}*/

/**
 * Registry wie ReactFlows `nodeTypes`: `KnotenDaten.knotenArt` entscheidet,
 * welche Knotenklasse und damit welche Anschlüsse verwendet werden.
 */
/*data class KnotenArten(
    private val fabriken: Map<String, KnotenFabrik> = standardFabriken,
) {
    public fun erstelle(daten: KnotenDaten, anschlüsse: KnotenAschlüsse): Knoten =
        (fabriken[daten.art] ?: fabriken.getValue(BasisKnoten.KNOTEN_ART)).erstelle(daten,anschlüsse)

    public fun mit(art: String, fabrik: KnotenFabrik): KnotenArten =
        copy(fabriken = fabriken + (art to fabrik))

    public companion object {
*//*        private fun registriereKnoten(knoten: object): Pair<String, KnotenFabrik> {
            return knoten.KNOTEN_ART to KnotenFabrik(::knoten)
        }*//*
        private val standardFabriken = mapOf(
            BasisKnoten.KNOTEN_ART to KnotenFabrik(::BasisKnoten),
            EingabeKnoten.KNOTEN_ART to KnotenFabrik(::EingabeKnoten),
            AusgabeKnoten.KNOTEN_ART to KnotenFabrik(::AusgabeKnoten),
            // TODO GruppenKnoten
        )

        public val Standard: KnotenArten = KnotenArten()
    }
}*/

/**
 * Rendert einen Knoten als Compose-Baustein.
 *
 * `modifierKnoten` positioniert und skaliert den gesamten Knoten. Über
 * `modifierAnschluss` kann die Karte jeden Anschluss zusätzlich mit
 * Pointer-Interaktion versehen.
 */
/*@Composable
public fun KnotenDaten.zuComposable(
    modifierKnoten: Modifier = Modifier,
    modifierAnschluss: (AnschlussRichtung, Int) -> Modifier = { _, _ -> AnschlussModifier },
    inhaltSkalierung: Float = 1f,
) = BasisKnoten(this).zuComposable(modifierKnoten, modifierAnschluss, inhaltSkalierung)*/

typealias KnotenFabrik = Map<String,KnotenKonstruktor>
typealias KnotenKonstruktor = (KnotenDaten) -> Knoten

@Suppress("UNCHECKED_CAST")
val BasisKnotenFabrik: KnotenFabrik = mapOf(
    BasisKnoten.KNOTEN_ART to ::BasisKnoten as KnotenKonstruktor,
    EingabeKnoten.KNOTEN_ART to ::EingabeKnoten as KnotenKonstruktor,
    AusgabeKnoten.KNOTEN_ART to ::AusgabeKnoten as KnotenKonstruktor,
)

typealias AnschlussModifier = (AnschlussDaten, Int) -> Modifier

sealed interface Knoten: GraphObjekt {
    public val daten: KnotenDaten
//    public val anschlüsse: KnotenAschlüsse

    @Composable
    public fun zuComposable(
        modifierKnoten: Modifier = Modifier,
        modifierAnschluss: AnschlussModifier = { daten,idx -> AnschlussModifierStandard },
        inhaltSkalierung: Float = 1f,
    )
    // TODO herausfinden, ob bestehende Verbindungen als Argument funktionieren
    public fun erhalteAnschlüsse(): KnotenAnschlüsse

    @Composable
    override fun zuComposable(
        modifier: Modifier
    ) { /* */ }
}

/**
 * Standard Knoten
 */
open class BasisKnoten(override val daten: KnotenDaten): Knoten {
    @Composable
    override fun zuComposable(
        modifierKnoten: Modifier,
        modifierAnschluss: AnschlussModifier,
        inhaltSkalierung: Float,
    ) { KnotenRahmen(this, modifierKnoten, modifierAnschluss, inhaltSkalierung) }

    override fun erhalteAnschlüsse(): KnotenAnschlüsse {
        TODO("Not yet implemented")
    }

    public companion object {
        public const val KNOTEN_ART: String = "default"
    }
}

/**
 * Standard Knoten mit einem Ausgang
 */
open class EingabeKnoten(daten: EingabeDaten): BasisKnoten(daten) {
    public companion object {
        public const val KNOTEN_ART: String = "eingabe"
    }
}

/**
 * Standard Knoten mit einem Eingang
 */
open class AusgabeKnoten(daten: AusgabeDaten): BasisKnoten(daten) {
    public companion object {
        public const val KNOTEN_ART: String = "ausgabe"
    }
}

/**
 * Vorschau der Standard-Knotendarstellung.
 */
@Preview
@Composable
private fun KnotenPreview() { // TODO
/*    val daten = KnotenDaten(
        id = "knoten-1",
        name = "Ableitung",
    )*/
    // daten.zuComposable(modifierAnschluss = { _, _ -> AnschlussModifierStandard })
}

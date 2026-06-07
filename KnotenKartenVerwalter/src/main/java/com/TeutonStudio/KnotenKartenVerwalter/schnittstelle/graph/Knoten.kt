package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussModifier
import com.TeutonStudio.KnotenKartenVerwalter.KnotenAnschlüsse
import com.TeutonStudio.KnotenKartenVerwalter.KnotenArt
import com.TeutonStudio.KnotenKartenVerwalter.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KnotenKonstruktor
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten

// Daten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten

// Composable
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.KnotenRahmen

@Suppress("UNCHECKED_CAST")
val BasisKnotenFabrik: KnotenFabrik = mapOf(
    BasisKnoten.KNOTEN_ART to ::BasisKnoten as KnotenKonstruktor,
    EingabeKnoten.KNOTEN_ART to ::EingabeKnoten as KnotenKonstruktor,
    AusgabeKnoten.KNOTEN_ART to ::AusgabeKnoten as KnotenKonstruktor,
)

/**
 * Knoten Elternklasse
 */
sealed interface Knoten: GraphObjekt {
    public val daten: KnotenDaten
//    public val anschlüsse: KnotenAschlüsse

    @Composable
    public fun zuComposable(
        modifierKnoten: Modifier = Modifier,
        modifierAnschluss: AnschlussModifier = { daten, idx -> AnschlussModifierStandard },
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
        public const val KNOTEN_ART: KnotenArt = "default"
    }
}

/**
 * Standard Knoten mit Ausgängen
 */
open class EingabeKnoten(daten: EingabeDaten): BasisKnoten(daten) {
    public companion object {
        public const val KNOTEN_ART: KnotenArt = "eingabe"
    }
}

/**
 * Standard Knoten Eingängen
 */
open class AusgabeKnoten(daten: AusgabeDaten): BasisKnoten(daten) {
    public companion object {
        public const val KNOTEN_ART: KnotenArt = "ausgabe"
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

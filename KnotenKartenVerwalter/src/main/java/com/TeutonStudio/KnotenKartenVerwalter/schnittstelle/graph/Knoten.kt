package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussFabrik
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussModifier
import com.TeutonStudio.KnotenKartenVerwalter.KnotenAnschlüsse
import com.TeutonStudio.KnotenKartenVerwalter.KnotenArt
import com.TeutonStudio.KnotenKartenVerwalter.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KnotenKonstruktor

// Daten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten

// Composable
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.KnotenRahmen

@Suppress("UNCHECKED_CAST")
val BasisKnotenFabrik: KnotenFabrik = mapOf(
    BasisKnoten.KNOTEN_ART to ::BasisKnoten as KnotenKonstruktor,
    EingabeKnoten.KNOTEN_ART to ::EingabeKnoten as KnotenKonstruktor,
    AusgabeKnoten.KNOTEN_ART to ::AusgabeKnoten as KnotenKonstruktor,
)

@Composable
public fun List<Knoten>.zuComposable(
    modifierKnoten: (KnotenDaten) -> Modifier,
    modifierAnschluss: (KnotenDaten) -> AnschlussModifier,
) = this.map { it.zuComposable(modifierKnoten(it.daten),modifierAnschluss(it.daten)) }

/**
 * Knoten Elternklasse
 */
sealed interface Knoten: GraphObjekt {
    public val daten: KnotenDaten
    public val anschlussFabrik: AnschlussFabrik
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
open class BasisKnoten(
    override val daten: KnotenDaten,
): Knoten {
    override val anschlussFabrik: AnschlussFabrik = BasisAnschlussFabrik

    @Composable
    override fun zuComposable(
        modifierKnoten: Modifier,
        modifierAnschluss: AnschlussModifier,
        inhaltSkalierung: Float,
    ) { KnotenRahmen(this,anschlussFabrik, modifierKnoten, modifierAnschluss, inhaltSkalierung) }

    override fun erhalteAnschlüsse(): KnotenAnschlüsse {
        return mapOf(
            EingangDaten("in1","Eingang 1", AnschlussKante.Links) to 0,
            EingangDaten("in2","Eingang 2", AnschlussKante.Links) to 1,
            AusgangDaten("out","Ausgang", AnschlussKante.Rechts) to 0
        )
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

    override fun erhalteAnschlüsse(): KnotenAnschlüsse {
        return mapOf(
            AusgangDaten("out1","Ausgang 1", AnschlussKante.Rechts) to 0,
            AusgangDaten("out2","Ausgang 2", AnschlussKante.Rechts) to 1,
        )
    }
}

/**
 * Standard Knoten Eingängen
 */
open class AusgabeKnoten(daten: AusgabeDaten): BasisKnoten(daten) {
    public companion object {
        public const val KNOTEN_ART: KnotenArt = "ausgabe"
    }

    override fun erhalteAnschlüsse(): KnotenAnschlüsse {
        return mapOf(
            EingangDaten("in1","Eingang 1", AnschlussKante.Links) to 0,
            EingangDaten("in2","Eingang 2", AnschlussKante.Links) to 1,
        )
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

package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussFabrik
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussModifier
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
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
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeAnschluss

// Composable
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.KnotenRahmen
import org.w3c.dom.Text
import kotlin.let

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

    @Composable
    override fun öffneKontext(
        pos: BildschirmPosition
    ) {
        Box(
            modifier = Modifier
                .offset { pos }
//                .onSizeChanged { fensterGröße = it }
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp),
        ) {
            Card() {
                Column {
                    Text("Kontextfenster des Knoten")
                }
            }
        }

    }

    @Composable
    override fun zuComposable(
        modifier: Modifier
    ) { TODO("Falsche Methode aufgerufen") }
}

/**
 * Standard Knoten
 */
open class BasisKnoten(
    override val daten: KnotenDaten,
): Knoten {
    override val anschlussFabrik: AnschlussFabrik = BasisAnschlussFabrik

    val anschlüsse
        get() = daten.anschlüsse.mapNotNull { anschlussFabrik.erzeugeAnschluss(it.key, this)?.let { a -> a to it.value } }.toMap()

    @Composable
    override fun zuComposable(
        modifierKnoten: Modifier,
        modifierAnschluss: AnschlussModifier,
        inhaltSkalierung: Float,
    ) { KnotenRahmen(daten,anschlüsse, modifierKnoten, modifierAnschluss, inhaltSkalierung) }

/*    override fun erhalteAnschlüsse(): KnotenAnschlüsse {
        return TODO("Korrekte Anschlussabfrage")
    }*/

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

/*    override fun erhalteAnschlüsse(): KnotenAnschlüsse {
        return mutableMapOf(
            AusgangDaten("out1","Ausgang 1", AnschlussKante.Rechts) to 0,
            AusgangDaten("out2","Ausgang 2", AnschlussKante.Rechts) to 1,
        )
    }*/
}

/**
 * Standard Knoten Eingängen
 */
open class AusgabeKnoten(daten: AusgabeDaten): BasisKnoten(daten) {
    public companion object {
        public const val KNOTEN_ART: KnotenArt = "ausgabe"
    }

/*    override fun erhalteAnschlüsse(): KnotenAnschlüsse {
        return mutableMapOf(
            EingangDaten("in1","Eingang 1", AnschlussKante.Links) to 0,
            EingangDaten("in2","Eingang 2", AnschlussKante.Links) to 1,
        )
    }*/
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

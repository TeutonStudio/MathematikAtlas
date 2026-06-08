package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussFabrik
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussModifier
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenArt
import com.TeutonStudio.KnotenKartenVerwalter.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KnotenKonstruktor
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten

// Daten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.erhalteSize
import com.TeutonStudio.KnotenKartenVerwalter.erhalteZoomfaktor
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeAnschluss

// Composable
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable.KnotenRahmen
import com.TeutonStudio.KnotenKartenVerwalter.zuAuswahl
import com.TeutonStudio.KnotenKartenVerwalter.zuBild
import kotlin.div
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
    public val besitzer: Karte
    public val anschlussFabrik: AnschlussFabrik

    val bildPos get() = daten.position.zuBild(besitzer.zustand.ansicht)
    val boxModiRect get() = { d: Density -> Modifier.offset { bildPos }.size( size = with(d) { (daten.erhalteSize() * zoomFaktor()).toDpSize() }) }
    val beiVerschiebung get() = { it: Offset ->
        besitzer.aktualisierung(daten.id,daten.position + it / zoomFaktor())
        besitzer.onAuswahlÄndern(daten.zuAuswahl())
    }

    @Composable
    public fun zuComposable(
        modifierKnoten: Modifier = Modifier.fillMaxSize(),
        modifierAnschluss: AnschlussModifier = { daten, idx -> AnschlussModifierStandard },
        inhaltSkalierung: Float = 1f,
    ) {
        val anschlussListe = remember(daten.anschlüsse) {
            daten.anschlüsse.mapNotNull { anschlussFabrik.erzeugeAnschluss(it.key, this)?.let { a -> a to it.value } }.toMap()
        }
        KnotenRahmen(daten,anschlussListe, boxModiRect, inhaltSkalierung,beiVerschiebung) { Inhalt(modifierKnoten) }
    }

    @Composable public fun Inhalt(modifier: Modifier) = Card(modifier = modifier, border = if (daten.ausgewaehlt) BorderStroke(5.dp,Color(0xFF2563EB)) else null) {Column(Modifier.padding(15.dp)) { Kopfzeile(); Textzeile(); Fußzeile() }}

    @Composable public fun Kopfzeile() = Text(daten.name)
    @Composable public fun Textzeile()
    @Composable public fun Fußzeile()

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

    private fun zoomFaktor(): Float = besitzer.zustand.ansicht.erhalteZoomfaktor().coerceAtLeast(0.01f)

    @Composable
    override fun zuComposable(modifier: Modifier) = TODO("Falsche Methode aufgerufen")
}

/**
 * Standard Knoten
 */
open class BasisKnoten(
    override val daten: KnotenDaten,
    override val besitzer: Karte,
): Knoten {
    override val anschlussFabrik: AnschlussFabrik = BasisAnschlussFabrik

    @Composable
    override fun Textzeile() {
        Text("Knoten Textzeile")
    }

    @Composable
    override fun Fußzeile() {
        Text("Knoten Fußzeile")
    }


    public companion object {
        public const val KNOTEN_ART: KnotenArt = "default"
    }
}

/**
 * Standard Knoten mit Ausgängen
 */
open class EingabeKnoten(daten: EingabeDaten, besitzer: Karte): BasisKnoten(daten,besitzer) {
    public companion object {
        public const val KNOTEN_ART: KnotenArt = "eingabe"
    }

}

/**
 * Standard Knoten Eingängen
 */
open class AusgabeKnoten(daten: AusgabeDaten, besitzer: Karte): BasisKnoten(daten,besitzer) {
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

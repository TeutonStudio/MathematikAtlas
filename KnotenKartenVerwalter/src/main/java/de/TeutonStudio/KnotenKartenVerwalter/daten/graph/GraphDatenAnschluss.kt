package de.TeutonStudio.KnotenKartenVerwalter.daten.graph

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussArt
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


typealias Kante = GraphDatenAnschluss.AnschlussKante
typealias Richtung = GraphDatenAnschluss.gerichteteGDA.AnschlussRichtung

interface GraphDatenAnschluss: GraphDaten {
    override var klasse: AnschlussArt?

    val kante: Kante
    var label: String

    enum class AnschlussKante {
        Links, Rechts, Oben, Unten;

        val istVertikal get() = this == Links || this == Rechts
        val istHorizontal get() = this == Oben || this == Unten

        fun <A> wertFür(links: A, rechts: A, oben: A, unten: A): A = wertFür(this, links, rechts, oben, unten)

        fun tangente(länge: Float = 1f): Offset = tangente(this, länge)
        fun alignment() = alignment(this)
        fun gegenüber() = gegenüber(this)

        public companion object {
            public fun Modifier.fillMaxKante(kante: AnschlussKante, @FloatRange fraction: Float = 1f): Modifier = if (kante.istVertikal) fillMaxHeight(fraction) else fillMaxWidth(fraction)
            public fun Modifier.offsetKante(kante: AnschlussKante, offset: Dp = 0.dp) = if(kante.istVertikal) offset(x=offset) else offset(y=offset)
        }
    }

    interface gerichteteGDA: GraphDatenAnschluss {
        val richtung: Richtung
        val istEingang: Boolean get() = richtung.istEingang
        val istAusgang: Boolean get() = richtung.istAusgang

        enum class AnschlussRichtung {
            Eingang, Ausgang;

            val istEingang get() = this == Eingang
            val istAusgang get() = this == Ausgang
        }

        override fun erlaubeVerbindung(mit: GraphDatenAnschluss): Boolean {
            val istGleichGerichtet = mit is gerichteteGDA && mit.richtung == richtung
            return super.erlaubeVerbindung(mit) && !istGleichGerichtet
        }
    }
    interface auswertbarerGDA : GraphDatenAnschluss, gerichteteGDA {
        var cache: PullDaten<*>
        abstract class PullDaten<T : Any> : ReadOnlyProperty<Any?, T> {
            private lateinit var wert: T

            constructor(speicher: String) { wert = ausSpeicher(speicher) }

            override operator fun getValue(thisRef: Any?, property: KProperty<*>, ): T = wert
            abstract fun ausSpeicher(wert: String): T
            abstract fun zuSpeicher(wert: T): String
        }

        override fun wurdeVerbunden(mit: GraphDatenAnschluss) {
            if (istEingang && mit is auswertbarerGDA && mit.istAusgang) {
                cache = mit.cache
            }
            super<GraphDatenAnschluss>.wurdeVerbunden(mit)
        }

        fun baueCache(eingangCache: List<PullDaten<*>?>): PullDaten<*>
    }

    public fun erlaubeVerbindung(mit: GraphDatenAnschluss): Boolean = mit != this
    public fun wurdeVerbunden(mit: GraphDatenAnschluss) {}
}


private fun <A> wertFür(
    kante: Kante,
    links: A, rechts: A, oben: A, unten: A,
): A = when (kante) {
    Kante.Links -> links
    Kante.Rechts -> rechts
    Kante.Oben -> oben
    Kante.Unten -> unten
}

private fun tangente(
    kante: Kante,
    länge: Float = 1f,
): Offset = kante.wertFür(
    Offset(-1f, 0f), Offset(1f, 0f),
    Offset(0f, -1f), Offset(0f, 1f),
) * länge

private fun alignment(kante: Kante): Alignment = kante.wertFür(
    Alignment.CenterStart, Alignment.CenterEnd,
    Alignment.TopCenter, Alignment.BottomCenter,
)

private fun gegenüber(kante: Kante) = kante.wertFür(
    Kante.Rechts,Kante.Links,
    Kante.Unten,Kante.Oben,
)

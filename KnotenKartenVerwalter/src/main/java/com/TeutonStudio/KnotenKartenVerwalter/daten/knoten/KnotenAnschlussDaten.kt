package com.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten

typealias AnschlussKnotenDaten = KnotenAnschlussDaten<out AnschlussDaten>

interface KnotenAnschlussDaten<D : AnschlussDaten>: KnotenGraphDaten, AnschlüsseDaten<D> {

    public fun duplizieren() = KnotenDaten<D>(id.duplizieren(),name.duplizieren()).apply {
        this@apply.position = this@KnotenAnschlussDaten.position + Offset(10f,10f)
        this@apply.anschlüsse.addAll(this@KnotenAnschlussDaten.anschlüsse)
        this@apply.anschlussIdx.putAll(this@KnotenAnschlussDaten.anschlussIdx)
        this@apply.data.putAll(this@KnotenAnschlussDaten.data)
    }

    public companion object {
        public fun String.duplizieren(separierer: String = "#") = split(separierer).let {
            setOf(it[0],it.getOrElse(1,{ "0" }).toInt() + 1) }.joinToString(separierer)
    }
}
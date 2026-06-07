package com.TeutonStudio.KnotenKartenVerwalter.daten.aktiv

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.Rechteck
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.GraphDaten


open class LiveKnoten(
    open var name: String,
    open var position: KartenPosition = Offset(0f, 0f),
    open var dimension: Rechteck = Offset(180f, 96f),
    open var ausgewaehlt: Boolean = false,
    open var data: Map<String, Any> = emptyMap(),
) {}
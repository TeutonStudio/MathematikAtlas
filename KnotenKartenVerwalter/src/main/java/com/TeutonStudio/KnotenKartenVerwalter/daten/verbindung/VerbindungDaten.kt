package com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung

import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.idReferenz
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BasisVerbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BezierVerbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungArt

open class VerbindungDaten(
    override val id: String,
    open val ids: IDEhe,
    open val label: String? = null,
    open val fehler: String? = null,
): GraphDaten(id) {
    override val klasse: VerbindungArt = BezierVerbindung.VERBINDUNG_ART

    constructor(
        id: String,
        knotenIds: Pair<String,String>,
        anschlussIds: Pair<String,String>,
        label: String?,
        fehler: String?,
    ): this(
        id,IDEhe(
            knotenIds.first,
            knotenIds.second,
            anschlussIds.first,
            anschlussIds.second,
        ),label,fehler
    )

    constructor(
        mann: Anschluss<out AnschlussDaten>,
        weib: Anschluss<out AnschlussDaten>,
        label: String?,
        fehler: String?,
    ): this(id(mann,weib),IDEhe(mann,weib),label,fehler)

    public companion object {
        private fun id(
            mann: Anschluss<out AnschlussDaten>,
            weib: Anschluss<out AnschlussDaten>,
        ): String = setOf(mann, weib).joinToString("-") { it.daten.id }
    }
}
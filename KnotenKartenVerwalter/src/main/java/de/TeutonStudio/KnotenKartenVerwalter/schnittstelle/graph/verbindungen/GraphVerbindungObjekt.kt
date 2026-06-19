package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

//typealias Kante = AnschlussGraphDaten.AnschlussKante

/**
 * Vertrag für Verbindungsobjekte zwischen zwei Anschlüssen einer Karte.
 * [Verbindung], [de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisObjektVerbindung] und [de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BezierVerbindung] sind die vorgesehenen Erweiterungspunkte.
 *
 * Verbindungen berechnen ihre Endpunkte aus den angeschlossenen Graphobjekten, liefern Pfad und Trefferabstand
 * und werden gemeinsam auf der Kartenebene gezeichnet.
 */
/*internal interface GraphVerbindungObjekt<V: VerbindungDaten>: GraphDatenObjekt<V> {
    *//* TODO herausfinden ob State oder var besser ist *//*
    public abstract var startKante: Kante
    public abstract val start: State<KartenPosition>
    *//* TODO herausfinden ob State oder var besser ist *//*
    public abstract var endeKante: Kante
    public abstract val ende: State<KartenPosition>

    *//** Setzt Verbindungen hinter Knoten und Anschlüsse. *//*
    @Composable public override fun Modifier.modiInputEvent() = this.zIndex(-1f)

    *//**
     * Erstellt die Canvas-Darstellung dieser Verbindung.
     * Sie wird innerhalb der Kartenebene unter den Knoten gezeichnet.
     *
     * @param modifier äußerer Modifier der Darstellung
     *//*
    @Composable public override fun zuComposable(modifier: Modifier) = Canvas(modifier = Modifier.modiInputEvent()) { zeichnung() }

    *//**
     * Verbindungen verwenden keine Box-Darstellung.
     * Sie werden stattdessen direkt über [zuComposable] auf eine Canvas gezeichnet.
     *
     * @receiver BoxScope der lokalen Darstellung
     *//*
    @Composable public override fun BoxScope.Darstellung() = TODO("Nicht benötigt für Verbindung")


    *//** Zeichenoperation für den aktuellen Verbindungspfad. *//*
    public val zeichnung: DrawScope.() -> Unit
        get() = {
            drawPath(
                path = erhaltePfad(),
                color = when {
                    istSelektiert.value -> graph.selektiertFarbe
                    daten.fehler != null -> Color(0xFFDC2626)
                    else -> Color(0xFF475569)
                },
                style = Stroke(width = if (istSelektiert.value) 8f else 3f, cap = StrokeCap.Round),
            )
        }
    *//** Berechnet den Abstand einer Kartenposition zum Verbindungspfad. *//*
    public abstract fun abstand(pos: KartenPosition): Offset

    *//** Liefert den zu zeichnenden Verbindungspfad im Kartenkoordinatenraum. *//*
    public abstract fun erhaltePfad(): Path

    *//** Prüft, ob die Verbindung den sichtbaren Kartenbereich überschneidet. *//*
    public fun istImViewport(viewport: RectF = graph.karte.zustand.erhalteViewportRect()): Boolean = listOf(start.value, ende.value).let { p ->
        val puffer = 80f
        RectF(
            p.minOf { it.x } - puffer,
            p.minOf { it.y } - puffer,
            p.maxOf { it.x } + puffer,
            p.maxOf { it.y } + puffer,
        )
    }.overlaps(viewport)


    public companion object {
        @Composable
        public fun Iterable<Verbindung>.zuComposable(*//*modifier: Modifier = Modifier*//*) {
            if (this.count() == 0) return
            Canvas(modifier = Modifier.fillMaxSize().zIndex(-1f)) { forEach { verbindung -> verbindung.zeichnung(this) } }
        }

        public fun Iterable<Verbindung>.sichtbar() = filter { it.istImViewport() }
    }
}*/

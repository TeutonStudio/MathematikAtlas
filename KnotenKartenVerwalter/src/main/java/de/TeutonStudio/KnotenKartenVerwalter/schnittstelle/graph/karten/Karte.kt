package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten.Companion.zuAuswahl
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AusgangDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.EinzelAuswahl
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AnschlussKnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import de.TeutonStudio.KnotenKartenVerwalter.printLogCat
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphCache
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.GraphKnotenObjekt.Companion.anschlüsseNachIDEhe
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.GraphKnotenObjekt.Companion.sichtbar
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.GraphKnotenObjekt.Companion.zuComposable
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.PullObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.erzeugeKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BezierVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.GraphVerbindungObjekt.Companion.sichtbar
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.GraphVerbindungObjekt.Companion.zuComposable
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.erzeugeVerbindung
import kotlin.math.min


sealed class Karte(
    override val graph: Graph,
    override val daten: KarteDaten,
): GraphKartenObjekt<KarteDaten> {
    override var layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)
    private val VERBINDUNG_TREFFER_RADIUS = 50f

    val knoten by GraphCache(daten.knoten) { d: AnschlussKnotenDaten ->
        knotenFabrik.erzeugeKnoten(graph,d,this).apply { registriere() }
    }

    val verbindungen by GraphCache(daten.verbindungen) { d: VerbindungDaten ->
        knoten.anschlüsseNachIDEhe(d.ids)?.let {
            verbindungFabrik.erzeugeVerbindung(graph,d,derivedStateOf { it.first.pos },derivedStateOf { it.second.pos },)?.apply {
                startKante = it.first.daten.kante
                endeKante = it.second.daten.kante
            }
        }?.apply { registriere() }
    }

    val anschlüsse get() = knoten.flatMap { it.anschlüsse }

    public var ctx by mutableStateOf("" to IntOffset.Zero)

    public fun keinKontext() { ctx = "" to IntOffset.Zero }

    public fun wähle(wahl: AuswahlDaten = AuswahlDaten.LEER) {
        zustand.auswahl.value = wahl
        onAuswahlÄndern(wahl)
    }

    public fun erhalteVerbindungNachPos(pos: KartenPosition): Pair<Verbindung,Offset>? = verbindungen.map { it to it.abstand(pos) }.minByOrNull { it.second.getDistanceSquared() }
    public fun erhalteAnschlussNachPos(pos: KartenPosition): Pair<Anschluss<out AnschlussDaten>,Offset>? = anschlüsse.map { it to it.pos - pos  }.minByOrNull { it.second.getDistanceSquared() }

    public override fun verschiebeKnoten(id: String, um: Offset) = knoten.filter { it.daten.id == id }.getOrNull(0)
        ?.apply { daten.position += um / zustand.zoom }.let { it != null }

    public override fun vernichteKnoten(knoten: Knoten) = daten.knoten.remove(knoten.daten).apply {
        daten.verbindungen.removeIf { it.ids.knotenIdMann == knoten.daten.id || it.ids.knotenIdWeib == knoten.daten.id }
        keinKontext(); wähle()
    }
    public override fun dupliziereKnoten(knoten: Knoten) = daten.knoten.add(knoten.daten.duplizieren()).apply { keinKontext() }
    public override fun planeVerbindung(a: Anschluss<out AnschlussDaten>) {
        pseudoVerbindung.value = BezierVerbindung(
            graph, VerbindungDaten(
                "pseudo",
                IDEhe(
                    a.besitzer.daten.id,
                    a.besitzer.daten.id,
                    a.daten.id,
                    a.daten.id,
                ),
            ),
            derivedStateOf { a.pos },
            derivedStateOf { a.dragZiel?.pos ?: a.dragPos }
        ).apply {
            startKante = a.daten.kante
            endeKante = startKante.gegenüber()
        }
/*        knoten.forEach {
            it.planeVerbindung
        }*/
    }
    public override fun definiereVerbindung(mann: Anschluss<out AnschlussDaten>, weib: Anschluss<out AnschlussDaten>) = daten.verbindungen.add(VerbindungDaten(mann,weib,"",null)).apply {
        if (weib.daten is AusgangDaten && mann.besitzer is PullObjekt) (mann.besitzer as PullObjekt).aktualisiereCache()
        if (mann.daten is AusgangDaten && weib.besitzer is PullObjekt) (weib.besitzer as PullObjekt).aktualisiereCache()
        mann.besitzer.definiereVerbindung()
        weib.besitzer.definiereVerbindung()
        keinKontext()
    }
    public override fun vernichteVerbindung(verbindung: Verbindung) = daten.verbindungen.remove(verbindung.daten).apply { keinKontext() }

    public fun inhaltsGrenzen(puffer: Float = 0f): Rect? {
        val grenzen = knoten.map { it.daten.dimension }.reduceOrNull { bisher, nächstes ->
            Rect(
                left = minOf(bisher.left,nächstes.left),
                top = minOf(bisher.top,nächstes.top),
                right = maxOf(bisher.right,nächstes.right),
                bottom = maxOf(bisher.bottom,nächstes.bottom),
            )
        } ?: return null

        val sichererPuffer = puffer.coerceAtLeast(0f)

        return Rect(
            left = grenzen.left - sichererPuffer,
            top = grenzen.top - sichererPuffer,
            right = grenzen.right + sichererPuffer,
            bottom = grenzen.bottom + sichererPuffer,
        )
    }

    public fun passeInhaltEin(
        rand: Float = 64f,
    ) {
        val grenzen = inhaltsGrenzen() ?: return

        val breite = zustand.dimension.width.toFloat()
        val höhe = zustand.dimension.height.toFloat()

        if (breite <= 0f || höhe <= 0f) return

        val verfügbareBreite = (breite - rand * 2f).coerceAtLeast(1f)
        val verfügbareHöhe = (höhe - rand * 2f).coerceAtLeast(1f)

        val neuerZoom = min(
            verfügbareBreite / grenzen.width.coerceAtLeast(1f),
            verfügbareHöhe / grenzen.height.coerceAtLeast(1f),
        ).coerceIn(
            KarteZustand.MIN_ZOOM,
            KarteZustand.MAX_ZOOM,
        )

        zustand.setzeAnsicht(
            neuerZoom = neuerZoom,
            neuePosition = Offset(
                x = breite / 2f - grenzen.center.x * neuerZoom,
                y = höhe / 2f - grenzen.center.y * neuerZoom,
            ),
        )
    }

    public override fun beiKlick(klickPos: Offset) {
        /* TODO herausfinden, wie ich it. tranformieren muss */
        val kartePos = klickPos.round().zuGraph()
        val v = erhalteVerbindungNachPos(kartePos)?.apply {
            if (second.getDistanceSquared() < VERBINDUNG_TREFFER_RADIUS) {
                wähle(first.daten.zuAuswahl())
            } else {
                wähle()
            }
        }
        if (v == null) {
            wähle()
        }
        keinKontext()
    }
    public override fun beiHalten(klickPos: Offset) {
        val karteCTX = { ctx = daten.id to klickPos.round() }
        val kartePos = klickPos.round().zuGraph()
        if (erhalteVerbindungNachPos(kartePos)?.let {
                printLogCat(it.first, it.second, it.second.getDistanceSquared())
                if (it.second.getDistanceSquared() < VERBINDUNG_TREFFER_RADIUS) {
                    wähle(it.first.daten.zuAuswahl())
                    ctx = it.first.daten.id to klickPos.round()
                    return@let it
                } else { return@let null }
            } == null) karteCTX()
    }
    public override fun beiTransform(centroid: Offset, zoomDelta: Float, panDelta: Offset, rotationChange: Float) {
        zustand.verschiebe(panDelta)
        zustand.zoome(zoomDelta)
    }

    /**
     * Erstellt das Kontextfenster dieser Karte.
     * Es wird von der Kartenebene an der übergebenen Bildschirmposition geöffnet.
     *
     * @param pos Position des Kontextfensters im Bildschirmkoordinatenraum
     */
    @Composable
    public override fun KontextFenster(
        pos: BildschirmPosition
    ) {
        Box(modifier = Modifier.offset { pos }.padding(vertical = 4.dp)) {
            Card() {
                Column(Modifier.padding(5.dp),horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("id: ${daten.id}",Modifier.scale(.9f),Color.Gray)
                    Text("neu",Modifier.clickable() { graph.karte })
                    Text("alles auswählen",Modifier.clickable() { graph.karte })
                }
            }
        }
    }

    /**
     * Erstellt die lokale Compose-Darstellung dieser Karte.
     * Sie bindet Welt und Overlay der Kartenebene in den Graphen ein.
     *
     * @receiver BoxScope der Kartendarstellung
     */
    @Composable
    public override fun BoxScope.Darstellung() {
        KartenWelt(); KartenOverlay()
    }

    @Composable private fun KartenWelt() = Box(
        modifier = Modifier.fillMaxSize().graphicsLayer {
            translationX = zustand.pos.x
            translationY = zustand.pos.y
            scaleX = zustand.zoom
            scaleY = zustand.zoom
            transformOrigin = TransformOrigin(0f, 0f)
        }
    ) {
        knoten.sichtbar().zuComposable(/*Modifier.zIndex(1f)*/)
        verbindungen.sichtbar().zuComposable(/*Modifier.zIndex(-1f)*/)
        pseudoVerbindung.value?.zuComposable()
    }

    @Composable private fun BoxScope.KartenOverlay() {
        graph.inhalt.forEach { if (it.öffneKontext.value) it.KontextFenster() }
        Box(Modifier.align(Alignment.CenterEnd)) {
            zustand.auswahl.erhalteInspektorObjekt()?.Inspektor()
        }
    }

    public fun MutableState<AuswahlDaten>.erhalteInspektorObjekt(): GraphObjekt? = when {
        value is EinzelAuswahl -> graph.inhalt.find { it.daten.id == (value as EinzelAuswahl).auswahlId }
        else -> null
    }
}

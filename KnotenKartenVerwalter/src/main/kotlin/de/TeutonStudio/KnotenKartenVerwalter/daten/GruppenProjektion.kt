package de.TeutonStudio.KnotenKartenVerwalter.daten

import java.util.UUID
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** Nur Darstellung: diese Arten werden weder registriert noch persistiert oder ausgewertet. */
const val GRUPPEN_BLOCK_ART = "editor.gruppenBlock"
const val GRUPPEN_EINGANG_ART = "editor.gruppenEingang"
const val GRUPPEN_AUSGANG_ART = "editor.gruppenAusgang"

private fun projektionsId(text: String) = UUID.nameUUIDFromBytes(text.toByteArray(Charsets.UTF_8)).toString()

data class GruppenProjektion(
    val karte: KartenDaten,
    val originalAnschlüsse: Map<AnschlussVerweis, AnschlussVerweis>,
    val sichtbareAnschlüsse: Map<AnschlussVerweis, AnschlussVerweis>,
    val blöcke: Map<KnotenId, VisuelleGruppenId>,
    val portale: Set<KnotenId>,
) {
    val anschlussPositionen: Map<AnschlussVerweis,GraphPunkt> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        karte.knoten.flatMap { k -> k.anschlüsse.map { a -> AnschlussVerweis(k.id,a.id) to (k.position + k.anschlussPositionRelativ(a)) } }.toMap()
    }
    fun original(ref: AnschlussVerweis) = originalAnschlüsse[ref] ?: ref
    fun sichtbar(ref: AnschlussVerweis) = sichtbareAnschlüsse[ref] ?: ref
}

/** Ein struktureller Durchlauf; Kamera und Animation sind keine Eingaben dieser Berechnung. */
fun KartenDaten.gruppenProjektion(geöffneteGruppe: VisuelleGruppenId? = null, abbruch: CoroutineContext = EmptyCoroutineContext): GruppenProjektion {
    abbruch.ensureActive()
    val index = GruppenIndex(this)
    val innen = geöffneteGruppe?.let(index::knotenIn) ?: knoten.mapTo(linkedSetOf()) { it.id }
    val gruppenImBereich = geöffneteGruppe?.let(index::nachfahren)
        ?: visuelleGruppen.mapTo(linkedSetOf()) { it.id }
    val direkteGruppe = visuelleGruppen.flatMap { g -> g.knotenIds.map { it to g.id } }.toMap()
    val blockFürGruppe = mutableMapOf<VisuelleGruppenId, VisuelleGruppenId?>()
    fun blockFür(id: VisuelleGruppenId): VisuelleGruppenId? = blockFürGruppe.getOrPut(id) {
        (listOf(id) + index.vorfahren(id)).lastOrNull {
            it != geöffneteGruppe && it in gruppenImBereich && index.gruppen[it]?.eingeklappt == true
        }
    }
    val blockFürKnoten = direkteGruppe.mapValues { blockFür(it.value) }
    val betroffen = verbindungen.flatMap { listOf(it.von to it, it.zu to it) }.groupBy({ it.first }, { it.second })
    val original = linkedMapOf<AnschlussVerweis, AnschlussVerweis>()
    val sichtbar = linkedMapOf<AnschlussVerweis, AnschlussVerweis>()
    val blöcke = linkedMapOf<KnotenId, VisuelleGruppenId>()
    val portale = linkedSetOf<KnotenId>()
    val darstellung = knoten.filter { it.id in innen && blockFürKnoten[it.id] == null }.toMutableList()
    val nachId = knoten.associateBy { it.id }
    fun randAnschlüsse(ids: Set<KnotenId>): List<Pair<AnschlussVerweis, AnschlussDaten>> =
        ids.sortedBy { it.wert }.flatMap { id ->
            abbruch.ensureActive()
            nachId[id]?.anschlüsse.orEmpty().sortedWith(compareBy({ it.richtung.ordinal }, { it.reihenfolge }, { it.id.wert })).mapNotNull { a ->
                val ref = AnschlussVerweis(id, a.id)
                val kanten = betroffen[ref].orEmpty()
                val außen = kanten.any { it.von.knotenId !in ids || it.zu.knotenId !in ids }
                if (außen || kanten.isEmpty() || a.richtung != AnschlussRichtung.Eingang || a.kannSichErweitern) ref to a else null
            }
        }
    val sichtbareBlöcke = gruppenImBereich.filter { it != geöffneteGruppe && blockFür(it) == it }
    sichtbareBlöcke.sortedBy { it.wert }.forEach { gid ->
        val g = index.gruppen.getValue(gid)
        val nodeId = KnotenId(projektionsId("gruppe:${gid.wert}"))
        blöcke[nodeId] = gid
        val ports = randAnschlüsse(index.knotenIn(gid)).mapIndexed { i, (ref, a) ->
            val port = a.copy(
                id = AnschlussId(projektionsId("gruppe:${gid.wert}:${ref.knotenId.wert}:${ref.anschlussId.wert}")),
                name = "${nachId[ref.knotenId]?.name}: ${a.name}",
                kante = if (a.richtung == AnschlussRichtung.Eingang) AnschlussKante.Links else AnschlussKante.Rechts,
                reihenfolge = i,
            )
            val ansichtRef = AnschlussVerweis(nodeId, port.id)
            original[ansichtRef] = ref
            sichtbar[ref] = ansichtRef
            port
        }
        val zeilen = maxOf(ports.count { it.kante == AnschlussKante.Links }, ports.count { it.kante == AnschlussKante.Rechts })
        darstellung += KnotenDaten(id = nodeId, art = GRUPPEN_BLOCK_ART, name = g.titel,
            position = g.position, größe = GraphGröße(260f, maxOf(110f, 48f + zeilen * 30f)), anschlüsse = ports)
    }
    val kanten = verbindungen.mapNotNull { v ->
        abbruch.ensureActive()
        if (v.von.knotenId !in innen || v.zu.knotenId !in innen) return@mapNotNull null
        val vonBlock = blockFürKnoten[v.von.knotenId]
        val zuBlock = blockFürKnoten[v.zu.knotenId]
        if (vonBlock != null && vonBlock == zuBlock) null
        else v.copy(von = sichtbar[v.von] ?: v.von, zu = sichtbar[v.zu] ?: v.zu)
    }.toMutableList()
    if (geöffneteGruppe != null) {
        val g = index.gruppen.getValue(geöffneteGruppe)
        val grenzen = randAnschlüsse(innen)
        grenzen.forEachIndexed { i, (ref, a) ->
            // Ein Portal pro Originalanschluss, auch bei mehreren Außenverbrauchern.
            val nodeId = KnotenId(projektionsId("portal:${g.id.wert}:${ref.knotenId.wert}:${ref.anschlussId.wert}"))
            val eingang = a.richtung == AnschlussRichtung.Eingang
            val port = a.copy(id = AnschlussId(projektionsId("portalanschluss:${nodeId.wert}")),
                richtung = if (eingang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang,
                kante = if (eingang) AnschlussKante.Rechts else AnschlussKante.Links)
            val portalRef = AnschlussVerweis(nodeId, port.id)
            original[portalRef] = ref
            portale += nodeId
            darstellung += KnotenDaten(id = nodeId,
                art = if (eingang) GRUPPEN_EINGANG_ART else GRUPPEN_AUSGANG_ART,
                name = "${if (eingang) "Karten-Eingang" else "Karten-Ausgang"}: ${nachId[ref.knotenId]?.name} / ${a.name}",
                position = GraphPunkt(if (eingang) g.position.x - 280f else g.position.x + g.größe.breite + 40f, g.position.y + 70f + i * 150f),
                größe = GraphGröße(230f, 120f), anschlüsse = listOf(port))
            val intern = sichtbar[ref] ?: ref
            kanten += VerbindungDaten(id = VerbindungsId(projektionsId("portalkante:${nodeId.wert}")),
                von = if (eingang) portalRef else intern, zu = if (eingang) intern else portalRef)
        }
    }
    val gruppen = visuelleGruppen.filter { it.id in gruppenImBereich && it.id != geöffneteGruppe && blockFür(it.id) == null }
    return GruppenProjektion(copy(knoten = darstellung, verbindungen = kanten, visuelleGruppen = gruppen), original, sichtbar, blöcke, portale)
}

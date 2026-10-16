package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

internal const val EINFUEGE_ABSTAND = 32f

/** Ausschließlich Positionsänderungen; gemessene Größen werden niemals gespeichert. */
internal data class EinfuegeLayout(
    val knotenDeltas: Map<KnotenId, Float>,
    val gruppenDeltas: Map<VisuelleGruppenId, Float>,
) {
    fun anwenden(karte: KartenDaten): KartenDaten = karte.copy(
        knoten = karte.knoten.map { k -> k.copy(position = k.position + GraphPunkt(knotenDeltas[k.id] ?: 0f, 0f)) },
        visuelleGruppen = karte.visuelleGruppen.map { g ->
            g.copy(position = g.position + GraphPunkt(gruppenDeltas[g.id] ?: 0f, 0f))
        },
    )
}

private data class LayoutBlock(
    val schlüssel: String,
    val knoten: Set<KnotenId>,
    val gruppen: Set<VisuelleGruppenId>,
    val links: Float,
    val rechts: Float,
    val oben: Float,
    val unten: Float,
) {
    fun aufGleicherHöhe(b: LayoutBlock) = oben < b.unten && b.oben < unten
}

/**
 * Geometrie enthält effektive Größen und Gruppenrahmen, aber keine Kameratransformation.
 * Die Einfügung bleibt fest. Monotone Sweeps laufen beidseitig von innen nach außen;
 * nur die Einfügung und tatsächlich bewegte Blöcke dürfen weitere Blöcke anschieben.
 */
internal fun planeEinfuegeLayout(
    geometrie: KartenDaten,
    eingefügt: KnotenId,
    quelle: KnotenId,
    ziel: KnotenId,
    anhänge: Map<KnotenId, KnotenAnhangMaße> = emptyMap(),
    geöffneteGruppe: VisuelleGruppenId? = null,
    inhaltsMinima: Map<KnotenId, GraphGröße> = emptyMap(),
): EinfuegeLayout {
    val index = GruppenIndex(geometrie)
    val nachId = geometrie.knoten.associateBy { it.id }
    if (listOf(eingefügt, quelle, ziel).any { it !in nachId }) return EinfuegeLayout(emptyMap(), emptyMap())
    val mitglieder = index.gruppen.keys.associateWith(index::knotenIn)
    val beteiligte = setOf(eingefügt, quelle, ziel)
    val gemeinsam = index.gruppen.values.filter { !it.eingeklappt && mitglieder.getValue(it.id).containsAll(beteiligte) }
        .maxWithOrNull(compareBy<VisuelleKnotenGruppeDaten>({ index.vorfahren(it.id).size }, { it.id.wert }))?.id
    val bereich = gemeinsam ?: geöffneteGruppe
    val innen = bereich?.let { mitglieder[it].orEmpty() } ?: nachId.keys
    val gruppenImBereich = bereich?.let(index::nachfahren) ?: index.gruppen.keys
    // Enthält ein Container den festen Knoten oder beide Enden, muss seine innere
    // Geometrie Platz schaffen. Alle übrigen Untergruppen bleiben starre Blöcke.
    val container = gruppenImBereich.filterTo(mutableSetOf()) { g ->
        g == bereich || eingefügt in mitglieder.getValue(g) ||
            (quelle in mitglieder.getValue(g) && ziel in mitglieder.getValue(g))
    }
    val blockGruppen = gruppenImBereich.filter { g ->
        g !in container && index.vorfahren(g).none { it in gruppenImBereich && it !in container }
    }
    val inBlöcken = blockGruppen.flatMapTo(mutableSetOf()) { mitglieder.getValue(it) }
    val projektion = geometrie.gruppenProjektion(bereich).let { it.copy(karte = it.karte.mitInhaltsminima(inhaltsMinima)) }
    val sichtbareBlöcke = projektion.blöcke.entries.associate { (id, gruppe) -> gruppe to projektion.karte.knoten.first { it.id == id } }
    val blöcke = buildList {
        for (gId in blockGruppen) {
            val g = index.gruppen.getValue(gId)
            // Eingeklappte Gruppen verwenden dieselbe Blockgeometrie wie der Renderer.
            val sichtbar = sichtbareBlöcke[gId]
            val größe = sichtbar?.größe ?: g.größe
            add(LayoutBlock("g:${gId.wert}", mitglieder.getValue(gId), index.nachfahren(gId),
                g.position.x, g.position.x + größe.breite, g.position.y, g.position.y + größe.höhe))
        }
        for (k in geometrie.knoten.filter { it.id in innen && it.id !in inBlöcken }) {
            val a = anhänge[k.id] ?: KnotenAnhangMaße()
            add(LayoutBlock("k:${k.id.wert}", setOf(k.id), emptySet(), k.position.x, k.position.x + k.größe.breite,
                k.position.y - a.oben, k.position.y + k.größe.höhe + a.unten))
        }
    }
    val fest = blöcke.singleOrNull { eingefügt in it.knoten } ?: return EinfuegeLayout(emptyMap(), emptyMap())
    val quellBlock = blöcke.singleOrNull { quelle in it.knoten }
    val zielBlock = blöcke.singleOrNull { ziel in it.knoten }
    val mitte = (fest.links + fest.rechts) / 2f
    val deltas = mutableMapOf<String, Float>()
    val linkeBlöcke = mutableListOf<Pair<LayoutBlock, Float>>()
    fun istLinks(b: LayoutBlock) = when (b) {
        quellBlock -> true
        zielBlock -> false
        else -> (b.links + b.rechts) / 2f < mitte ||
            ((b.links + b.rechts) / 2f == mitte && b.schlüssel < fest.schlüssel)
    }
    for (links in listOf(true, false)) {
        val kandidaten = blöcke.filter { it != fest && istLinks(it) == links }
        val startDeltas = kandidaten.associate { b -> b.schlüssel to when (b) {
            quellBlock -> minOf(0f, fest.links - EINFUEGE_ABSTAND - b.rechts)
            zielBlock -> maxOf(0f, fest.rechts + EINFUEGE_ABSTAND - b.links)
            else -> 0f
        } }
        // Nach der zwingenden Nachbarverschiebung sortieren, damit auch eine
        // rückwärts verlaufende Verbindung endlich und ohne Sonderfall aufgelöst wird.
        val sortiert = kandidaten.sortedWith(compareBy<LayoutBlock> {
            val dx = startDeltas.getValue(it.schlüssel)
            if (links) -(it.rechts + dx) else it.links + dx
        }.thenBy { it.schlüssel })
        val schieber = mutableListOf(fest to 0f)
        // Auch sehr breite, vertikal versetzte Blöcke können über die Mitte
        // hinausragen. Die rechte Seite berücksichtigt die fertige linke Seite.
        if (!links) schieber += linkeBlöcke
        for (b in sortiert) {
            var dx = startDeltas.getValue(b.schlüssel)
            fun begrenze(a: LayoutBlock, aDx: Float) {
                if (!a.aufGleicherHöhe(b)) return
                dx = if (links) minOf(dx, a.links + aDx - EINFUEGE_ABSTAND - b.rechts)
                else maxOf(dx, a.rechts + aDx + EINFUEGE_ABSTAND - b.links)
            }
            for ((a, aDx) in schieber) {
                if (a == fest || aDx != 0f) begrenze(a, aDx)
            }
            // Sobald dieser Block selbst bewegt wird, zählen auch unveränderte
            // frühere Hindernisse. Bestehende entfernte Überlappungen bleiben sonst.
            if (dx != 0f) for ((a, aDx) in schieber) begrenze(a, aDx)
            if (dx != 0f) {
                deltas[b.schlüssel] = dx
            }
            schieber += b to dx
            if (links) linkeBlöcke += b to dx
        }
    }
    return EinfuegeLayout(
        blöcke.flatMap { b -> deltas[b.schlüssel]?.let { dx -> b.knoten.map { it to dx } }.orEmpty() }.toMap(),
        blöcke.flatMap { b -> deltas[b.schlüssel]?.let { dx -> b.gruppen.map { it to dx } }.orEmpty() }.toMap(),
    )
}

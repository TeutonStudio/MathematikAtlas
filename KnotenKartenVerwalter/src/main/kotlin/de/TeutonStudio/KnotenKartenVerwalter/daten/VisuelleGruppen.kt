package de.TeutonStudio.KnotenKartenVerwalter.daten

const val VISUELLE_GRUPPE_STANDARD_TITEL = "Gruppe"
const val VISUELLE_GRUPPE_KOPFZEILE_HÖHE = 42f
const val VISUELLE_GRUPPE_INHALT_ABSTAND = 24f
const val VISUELLE_GRUPPE_MINDEST_BREITE = 180f
const val VISUELLE_GRUPPE_MINDEST_HÖHE = 110f

/** Liefert eine stabile Gruppengeometrie, die alle angegebenen Knoten vollständig umfasst. */
fun KartenDaten.visuelleGruppenGeometrieFür(knotenIds: Set<KnotenId>): Pair<GraphPunkt, GraphGröße>? {
    val gruppenKnoten = knoten.filter { it.id in knotenIds }
    if (gruppenKnoten.isEmpty()) return null
    val links = gruppenKnoten.minOf { it.position.x } - VISUELLE_GRUPPE_INHALT_ABSTAND
    val oben = gruppenKnoten.minOf { it.position.y } -
        VISUELLE_GRUPPE_KOPFZEILE_HÖHE - VISUELLE_GRUPPE_INHALT_ABSTAND
    val rechts = gruppenKnoten.maxOf { it.position.x + it.größe.breite } + VISUELLE_GRUPPE_INHALT_ABSTAND
    val unten = gruppenKnoten.maxOf { it.position.y + it.größe.höhe } + VISUELLE_GRUPPE_INHALT_ABSTAND
    return GraphPunkt(links, oben) to GraphGröße(
        breite = (rechts - links).coerceAtLeast(VISUELLE_GRUPPE_MINDEST_BREITE),
        höhe = (unten - oben).coerceAtLeast(VISUELLE_GRUPPE_MINDEST_HÖHE),
    )
}

/** Prüft die vollständige Lage eines Knotens im Inhaltsbereich unterhalb der Kopfzeile. */
fun VisuelleKnotenGruppeDaten.enthältVollständig(knoten: KnotenDaten): Boolean {
    val links = position.x
    val oben = position.y + VISUELLE_GRUPPE_KOPFZEILE_HÖHE
    val rechts = position.x + größe.breite
    val unten = position.y + größe.höhe
    return knoten.position.x >= links &&
        knoten.position.y >= oben &&
        knoten.position.x + knoten.größe.breite <= rechts &&
        knoten.position.y + knoten.größe.höhe <= unten
}

/** Ermittelt deterministisch alle vollständig enthaltenen Knoten. */
fun KartenDaten.vollständigEnthalteneKnoten(gruppe: VisuelleKnotenGruppeDaten): Set<KnotenId> =
    knoten.asSequence().filter(gruppe::enthältVollständig).map { it.id }.toSet()

/** Anteil der Knotenfläche im Inhaltsbereich der Gruppe unterhalb der Kopfzeile. */
fun VisuelleKnotenGruppeDaten.überdeckungsAnteil(knoten: KnotenDaten): Float {
    val knotenLinks = knoten.position.x
    val knotenOben = knoten.position.y
    val knotenRechts = knotenLinks + knoten.größe.breite
    val knotenUnten = knotenOben + knoten.größe.höhe
    val gruppenLinks = position.x
    val gruppenOben = position.y + VISUELLE_GRUPPE_KOPFZEILE_HÖHE
    val gruppenRechts = position.x + größe.breite
    val gruppenUnten = position.y + größe.höhe
    val breite = (minOf(knotenRechts, gruppenRechts) - maxOf(knotenLinks, gruppenLinks)).coerceAtLeast(0f)
    val höhe = (minOf(knotenUnten, gruppenUnten) - maxOf(knotenOben, gruppenOben)).coerceAtLeast(0f)
    val fläche = knoten.größe.breite * knoten.größe.höhe
    return if (fläche <= 0f) 0f else (breite * höhe / fläche).coerceIn(0f, 1f)
}

/**
 * Ordnet jeden Knoten höchstens einer Gruppe zu. Mehr als die Hälfte der Knotenfläche
 * muss im Inhaltsbereich liegen; bei gleichem Anteil bleibt die bisherige Gruppe stabil.
 */
fun KartenDaten.mitAutomatischenVisuellenGruppenMitgliedschaften(): KartenDaten {
    if (visuelleGruppen.isEmpty()) return this
    val hierarchie = mitRäumlicherGruppenHierarchie()
    val index = GruppenIndex(hierarchie)
    val vorher = hierarchie.visuelleGruppen.flatMap { g -> g.knotenIds.map { it to g.id } }.toMap()
    val eingefroren = hierarchie.visuelleGruppen.filter { g ->
        g.eingeklappt || index.vorfahren(g.id).any { index.gruppen[it]?.eingeklappt == true }
    }.mapTo(mutableSetOf()) { it.id }
    val zuordnung = knoten.mapNotNull { k ->
        val bisher = vorher[k.id]
        if (bisher in eingefroren) return@mapNotNull k.id to bisher!!
        val kandidaten = hierarchie.visuelleGruppen.filter { it.id !in eingefroren }
            .mapNotNull { g -> g.überdeckungsAnteil(k).takeIf { it > .5f }?.let { Triple(g, it, index.vorfahren(g.id).size) } }
        val tiefe = kandidaten.maxOfOrNull { it.third } ?: return@mapNotNull null
        val tiefste = kandidaten.filter { it.third == tiefe }
        val maximum = tiefste.maxOf { it.second }
        val beste = tiefste.filter { kotlin.math.abs(it.second - maximum) <= 1e-6f }
        val ziel = beste.firstOrNull { it.first.id == bisher } ?: beste.minBy { it.first.id.wert }
        k.id to ziel.first.id
    }.toMap()
    val mitglieder = zuordnung.entries.groupBy({ it.value }, { it.key })
    val neu = hierarchie.visuelleGruppen.map { it.copy(knotenIds = mitglieder[it.id].orEmpty().toSet()) }
    return if (neu == visuelleGruppen) this else copy(visuelleGruppen = neu)
}

/** Eltern werden nur aus echt größeren, vollständig umfassenden ausgeklappten Rahmen gewählt. */
fun KartenDaten.mitRäumlicherGruppenHierarchie(): KartenDaten {
    val index = GruppenIndex(this)
    val neu = visuelleGruppen.map { kind ->
        if (index.vorfahren(kind.id).any { index.gruppen[it]?.eingeklappt == true }) kind
        else {
            val kandidaten = visuelleGruppen.filter { eltern ->
                !eltern.eingeklappt && index.vorfahren(eltern.id).none { index.gruppen[it]?.eingeklappt == true } && eltern.id != kind.id &&
                    eltern.größe.breite * eltern.größe.höhe > kind.größe.breite * kind.größe.höhe &&
                    kind.position.x >= eltern.position.x &&
                    kind.position.y >= eltern.position.y + VISUELLE_GRUPPE_KOPFZEILE_HÖHE &&
                    kind.position.x + kind.größe.breite <= eltern.position.x + eltern.größe.breite &&
                    kind.position.y + kind.größe.höhe <= eltern.position.y + eltern.größe.höhe
            }
            val fläche = kandidaten.minOfOrNull { it.größe.breite * it.größe.höhe }
            val beste = kandidaten.filter { it.größe.breite * it.größe.höhe == fläche }
            val ziel = beste.firstOrNull { it.id == kind.elternGruppenId } ?: beste.minByOrNull { it.id.wert }
            kind.copy(elternGruppenId = ziel?.id)
        }
    }
    return if (neu == visuelleGruppen) this else copy(visuelleGruppen = neu)
}

/** Wiederverwendbarer Index; Traversierungen bleiben auch bei beschädigten Daten endlich. */
class GruppenIndex(karte: KartenDaten) {
    val gruppen = karte.visuelleGruppen.associateBy { it.id }
    val kinder = karte.visuelleGruppen.groupBy { it.elternGruppenId }
    fun vorfahren(id: VisuelleGruppenId): List<VisuelleGruppenId> {
        val besucht = mutableSetOf(id)
        val result = mutableListOf<VisuelleGruppenId>()
        var aktuell = gruppen[id]?.elternGruppenId
        while (aktuell != null && besucht.add(aktuell)) {
            result += aktuell
            aktuell = gruppen[aktuell]?.elternGruppenId
        }
        return result
    }
    fun nachfahren(id: VisuelleGruppenId): Set<VisuelleGruppenId> {
        val result = linkedSetOf<VisuelleGruppenId>()
        val offen = ArrayDeque<VisuelleGruppenId>()
        offen.add(id)
        while (offen.isNotEmpty()) {
            val next = offen.removeFirst()
            if (result.add(next)) kinder[next].orEmpty().forEach { offen.add(it.id) }
        }
        return result
    }
    fun knotenIn(id: VisuelleGruppenId): Set<KnotenId> =
        nachfahren(id).flatMapTo(linkedSetOf()) { gruppen[it]?.knotenIds.orEmpty() }
}

fun KartenDaten.prüfeGruppenHierarchie() {
    val index = GruppenIndex(this)
    require(index.gruppen.size == visuelleGruppen.size) { "Doppelte Gruppen-ID." }
    visuelleGruppen.forEach { g ->
        require(g.elternGruppenId == null || g.elternGruppenId in index.gruppen) { "Unbekannte Elterngruppe." }
        val besucht = mutableSetOf(g.id)
        var eltern = g.elternGruppenId
        while (eltern != null) {
            require(besucht.add(eltern)) { "Zyklus in Gruppenhierarchie." }
            eltern = index.gruppen[eltern]?.elternGruppenId
        }
    }
}

fun GraphGröße.alsGültigeVisuelleGruppenGröße() = GraphGröße(
    breite = breite.coerceAtLeast(VISUELLE_GRUPPE_MINDEST_BREITE),
    höhe = höhe.coerceAtLeast(VISUELLE_GRUPPE_MINDEST_HÖHE),
)

/**
 * Entfernt verwaiste und mehrdeutige Gruppenmitgliedschaften.
 * Die Gruppe selbst bleibt unabhängig von der Kinderzahl bestehen; ein Knoten gehört höchstens
 * einer visuellen Gruppe an. Bei Altgruppen ohne Geometrie wird einmalig der Kinderrahmen abgeleitet.
 */
fun KartenDaten.bereinigteVisuelleGruppen(): KartenDaten {
    val vorhandeneKnoten = knoten.associateBy { it.id }
    val bereitsVerwendet = mutableSetOf<KnotenId>()
    val bereinigt = visuelleGruppen.map { gruppe ->
        val vorhandeneIds = gruppe.knotenIds.filterTo(linkedSetOf()) { it in vorhandeneKnoten }
        val geometrieFehlt = gruppe.größe.breite <= 0f || gruppe.größe.höhe <= 0f
        val (position, größe) = if (geometrieFehlt) {
            visuelleGruppenGeometrieFür(vorhandeneIds)
                ?: (gruppe.position to GraphGröße(
                    VISUELLE_GRUPPE_MINDEST_BREITE,
                    VISUELLE_GRUPPE_MINDEST_HÖHE,
                ))
        } else gruppe.position to gruppe.größe.alsGültigeVisuelleGruppenGröße()
        val normalisiert = gruppe.copy(
            titel = gruppe.titel.trim().ifBlank { VISUELLE_GRUPPE_STANDARD_TITEL },
            position = position,
            größe = größe,
            knotenIds = emptySet(),
        )
        val gültigeIds = vorhandeneIds.asSequence()
            .filter { id -> id !in bereitsVerwendet }
            .onEach { id -> bereitsVerwendet += id }
            .toCollection(linkedSetOf())
        normalisiert.copy(knotenIds = gültigeIds)
    }
    return if (bereinigt == visuelleGruppen) this else copy(visuelleGruppen = bereinigt)
}

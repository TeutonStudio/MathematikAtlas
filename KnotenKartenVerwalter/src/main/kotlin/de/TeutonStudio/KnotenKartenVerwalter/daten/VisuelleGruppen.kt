package de.TeutonStudio.KnotenKartenVerwalter.daten

const val VISUELLE_GRUPPE_STANDARD_TITEL = "Gruppe"
const val VISUELLE_GRUPPE_KOPFZEILE_HÖHE = 42f
const val VISUELLE_GRUPPE_INHALT_ABSTAND = 18f
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

fun GraphGröße.alsGültigeVisuelleGruppenGröße() = GraphGröße(
    breite = breite.coerceAtLeast(VISUELLE_GRUPPE_MINDEST_BREITE),
    höhe = höhe.coerceAtLeast(VISUELLE_GRUPPE_MINDEST_HÖHE),
)

/**
 * Entfernt verwaiste, geometrisch ausgetretene und mehrdeutige Gruppenmitgliedschaften.
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
            .filter { id -> normalisiert.enthältVollständig(vorhandeneKnoten.getValue(id)) }
            .onEach(bereitsVerwendet::add)
            .toCollection(linkedSetOf())
        normalisiert.copy(knotenIds = gültigeIds)
    }
    return if (bereinigt == visuelleGruppen) this else copy(visuelleGruppen = bereinigt)
}

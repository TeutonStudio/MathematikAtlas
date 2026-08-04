package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungsId
import de.TeutonStudio.MathematikKnoten.RECHNER_OPERATOR_PARAMETER
import de.TeutonStudio.MathematikKnoten.STRUKTUR_RECHNER_FORMEL_LATEX
import de.TeutonStudio.MathematikKnoten.StrukturRechnerKnotenFamilie
import de.TeutonStudio.MathematikKnoten.StrukturRechnerOperatoren

internal fun strukturRechnerKonzept(
    knoten: KnotenDaten,
    familie: StrukturRechnerKnotenFamilie,
): KonzeptDefinition {
    val operatorId = knoten.parameter[RECHNER_OPERATOR_PARAMETER]
    val formel = operatorId == familie.formelOperatorId
    val operator = StrukturRechnerOperatoren.finde(familie, operatorId)
    val titel = if (formel) "Formel" else operator.titel
    val latex = if (formel) {
        knoten.parameter[STRUKTUR_RECHNER_FORMEL_LATEX].orEmpty().ifBlank { "f(\\ldots)" }
    } else {
        operator.definitionsLatex
    }
    val beschreibung = if (formel) {
        "Die Definitionskarte wird direkt aus dem gespeicherten typisierten CAS-Ausdruck und seinen aktuellen Anschlüssen erzeugt."
    } else {
        "Die Definitionskarte folgt dem ausgewählten Operatorvertrag und aktualisiert Ein- und Ausgangstypen gemeinsam."
    }
    return KonzeptDefinition(
        id = KonzeptId("strukturrechner-${familie.name.lowercase()}-${operatorId.orEmpty().replace('.', '-')}-${knoten.id.wert}"),
        name = "${familie.titel}: $titel",
        beschreibung = beschreibung,
        pfad = familie.kategorie.split(':').map(String::trim).filter(String::isNotBlank),
        tags = setOf(familie.titel, titel, operatorId.orEmpty(), "CAS", "dynamische Definition"),
        knotenArten = setOf(familie.knotenArt),
        knotenParameter = mapOf(RECHNER_OPERATOR_PARAMETER to operatorId.orEmpty()),
        reiter = listOf(
            KonzeptReiter(
                id = "definition",
                titel = if (formel) "Formel: $latex" else "Definition: $latex",
                rolle = KonzeptReiterRolle.Definition,
                karte = strukturRechnerDefinitionsKarte(knoten, familie, titel, latex, beschreibung),
            ),
        ),
    )
}

private fun strukturRechnerDefinitionsKarte(
    knoten: KnotenDaten,
    familie: StrukturRechnerKnotenFamilie,
    titel: String,
    latex: String,
    beschreibung: String,
): KartenDaten {
    val prefix = "definition-${familie.name.lowercase()}-${knoten.id.wert}"
    val eingänge = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
    val ausgänge = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Ausgang }
        .sortedBy { it.reihenfolge }
    val regelAnschlüsse = knoten.anschlüsse.mapIndexed { index, anschluss ->
        anschluss.copy(id = AnschlussId("$prefix-regel-$index"))
    }
    val regel = KnotenDaten(
        id = KnotenId("$prefix-regel"),
        art = TestDefinitionsKarten.KONZEPT_REGEL_ART,
        name = latex,
        position = GraphPunkt(390f, 70f),
        größe = GraphGröße(500f, maxOf(170f, 110f + 36f * maxOf(eingänge.size, ausgänge.size))),
        anschlüsse = regelAnschlüsse,
        parameter = mapOf(
            "regel" to beschreibung,
            "definition" to latex,
            "operator" to knoten.parameter[RECHNER_OPERATOR_PARAMETER].orEmpty(),
            "knotenArt" to familie.knotenArt,
        ),
    )
    val eingangsKnoten = eingänge.mapIndexed { index, anschluss ->
        strukturSchnittstelle(prefix, anschluss, index, true)
    }
    val ausgangsKnoten = ausgänge.mapIndexed { index, anschluss ->
        strukturSchnittstelle(prefix, anschluss, index, false)
    }
    val regelEingänge = regel.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
    val regelAusgänge = regel.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }.sortedBy { it.reihenfolge }
    val verbindungen = buildList {
        eingangsKnoten.forEachIndexed { index, quelle ->
            add(
                VerbindungDaten(
                    id = VerbindungsId("$prefix-e-$index"),
                    von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                    zu = AnschlussVerweis(regel.id, regelEingänge[index].id),
                ),
            )
        }
        ausgangsKnoten.forEachIndexed { index, ziel ->
            add(
                VerbindungDaten(
                    id = VerbindungsId("$prefix-a-$index"),
                    von = AnschlussVerweis(regel.id, regelAusgänge[index].id),
                    zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.single().id),
                ),
            )
        }
    }
    return KartenDaten(
        id = KartenId(prefix),
        name = "${familie.titel}: $titel",
        knoten = eingangsKnoten + regel + ausgangsKnoten,
        verbindungen = verbindungen,
    )
}

private fun strukturSchnittstelle(
    prefix: String,
    anschluss: AnschlussDaten,
    index: Int,
    eingang: Boolean,
): KnotenDaten {
    val id = "$prefix-${if (eingang) "eingang" else "ausgang"}-$index"
    return KnotenDaten(
        id = KnotenId(id),
        art = if (eingang) TestDefinitionsKarten.KONZEPT_EINGANG_ART else TestDefinitionsKarten.KONZEPT_AUSGANG_ART,
        name = anschluss.name,
        position = GraphPunkt(if (eingang) 30f else 970f, 70f + index * 120f),
        größe = GraphGröße(280f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("$id-wert"),
                name = "wert",
                richtung = if (eingang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang,
                kante = if (eingang) AnschlussKante.Rechts else AnschlussKante.Links,
                art = anschluss.art,
                zulässigeArten = anschluss.zulässigeArten,
            ),
        ),
        parameter = mapOf(
            "typ" to anschluss.art.wert,
            "rolle" to anschluss.name,
        ),
    )
}

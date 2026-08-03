package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

sealed interface KartenAktion {
    data class KnotenEinfügen(val knoten: KnotenDaten) : KartenAktion
    data class KnotenVerschieben(val id: KnotenId, val position: GraphPunkt) : KartenAktion
    data class KnotenMehrfachVerschieben(val ids: Set<KnotenId>, val delta: GraphPunkt) : KartenAktion
    data class KnotenMehrfachEinfügen(
        val knoten: List<KnotenDaten>,
        val verbindungen: List<VerbindungDaten>,
    ) : KartenAktion
    data class KnotenGrößeÄndern(val id: KnotenId, val größe: GraphGröße) : KartenAktion
    data class KnotenParameterÄndern(val id: KnotenId, val schlüssel: String, val wert: String) : KartenAktion
    data class KnotenEigenschaftÄndern(val id: KnotenId, val schlüssel: String, val wert: KnotenEigenschaft) : KartenAktion
    data class KnotenObjektEigenschaftFeldÄndern(val id: KnotenId, val schlüssel: String, val feld: String, val wert: KnotenEigenschaft) : KartenAktion
    data class KnotenEigenschaftenErsetzen(val id: KnotenId, val eigenschaften: Map<String, KnotenEigenschaft>) : KartenAktion
    /** Ersetzt die Anschlüsse eines Knotens, etwa um die Reihenfolge von Methodenargumenten zu ändern. */
    data class KnotenAnschlüsseÄndern(val id: KnotenId, val anschlüsse: List<AnschlussDaten>) : KartenAktion
    /** Ersetzt atomar Konfiguration und Anschlüsse; Verbindungen zu entfernten Anschlüssen entfallen. */
    data class KnotenKonfigurationErsetzen(
        val id: KnotenId,
        val parameter: Map<String, String>,
        val anschlüsse: List<AnschlussDaten>,
    ) : KartenAktion
    /** Ersetzt einen vollständigen Knoten atomar und entfernt nur Verbindungen zu entfallenen Anschlüssen. */
    data class KnotenErsetzen(val knoten: KnotenDaten) : KartenAktion
    data class KnotenLöschen(val id: KnotenId) : KartenAktion
    data class KnotenMehrfachLöschen(val ids: Set<KnotenId>) : KartenAktion
    data class VisuelleGruppeErstellen(
        val knotenIds: Set<KnotenId>,
        val titel: String = VISUELLE_GRUPPE_STANDARD_TITEL,
    ) : KartenAktion
    data class VisuelleGruppeVerschieben(val id: VisuelleGruppenId, val delta: GraphPunkt) : KartenAktion
    data class VisuelleGruppeGrößeÄndern(val id: VisuelleGruppenId, val größe: GraphGröße) : KartenAktion
    data class VisuelleGruppeTitelÄndern(val id: VisuelleGruppenId, val titel: String) : KartenAktion
    data class VisuelleGruppenKinderZuordnen(val id: VisuelleGruppenId) : KartenAktion
    data class VisuelleGruppeLöschen(val id: VisuelleGruppenId) : KartenAktion
    data class VisuelleGruppierungAufheben(val knotenIds: Set<KnotenId>) : KartenAktion
    /** Entfernt nur die Verbindungen eines Knotens; der Knoten selbst bleibt erhalten. */
    data class KnotenIsolieren(val id: KnotenId) : KartenAktion
    /** Fügt eine Verbindung ein und ersetzt dabei eine bereits am Ziel-Eingang liegende Verbindung. */
    data class VerbindungEinfügen(val verbindung: VerbindungDaten) : KartenAktion
    /** Verschiebt eine vorhandene Verbindung atomar auf einen neuen Zielanschluss. */
    data class VerbindungNeuVerbinden(val alteVerbindung: VerbindungsId, val verbindung: VerbindungDaten) : KartenAktion
    data class VerbindungLöschen(val id: VerbindungsId) : KartenAktion
    data class AnsichtÄndern(val ansicht: AnsichtsFenster) : KartenAktion
}

fun KartenDaten.wendeAn(aktion: KartenAktion): KartenDaten = when (aktion) {
    is KartenAktion.KnotenEinfügen -> copy(knoten = knoten + aktion.knoten)
    is KartenAktion.KnotenVerschieben -> copy(knoten = knoten.map { if (it.id == aktion.id) it.copy(position = aktion.position) else it })
    is KartenAktion.KnotenMehrfachVerschieben -> copy(knoten = knoten.map {
        if (it.id in aktion.ids) it.copy(position = it.position + aktion.delta) else it
    })
    is KartenAktion.KnotenMehrfachEinfügen -> copy(
        knoten = knoten + aktion.knoten,
        verbindungen = verbindungen + aktion.verbindungen,
    )
    is KartenAktion.KnotenGrößeÄndern -> copy(knoten = knoten.map { if (it.id == aktion.id) it.copy(größe = aktion.größe) else it })
    is KartenAktion.KnotenParameterÄndern -> copy(knoten = knoten.map {
        if (it.id == aktion.id) it.copy(parameter = it.parameter + (aktion.schlüssel to aktion.wert)) else it
    })
    is KartenAktion.KnotenEigenschaftÄndern -> copy(knoten = knoten.map {
        if (it.id == aktion.id) it.copy(eigenschaften = it.eigenschaften + (aktion.schlüssel to aktion.wert)) else it
    })
    is KartenAktion.KnotenObjektEigenschaftFeldÄndern -> copy(knoten = knoten.map {
        if (it.id != aktion.id) it else {
            val objekt = it.eigenschaften[aktion.schlüssel] as? KnotenEigenschaft.Objekt ?: KnotenEigenschaft.Objekt(emptyMap())
            it.copy(eigenschaften = it.eigenschaften + (aktion.schlüssel to objekt.copy(felder = objekt.felder + (aktion.feld to aktion.wert))))
        }
    })
    is KartenAktion.KnotenEigenschaftenErsetzen -> copy(knoten = knoten.map {
        if (it.id == aktion.id) it.copy(eigenschaften = aktion.eigenschaften) else it
    })
    is KartenAktion.KnotenAnschlüsseÄndern -> copy(knoten = knoten.map {
        if (it.id == aktion.id) it.copy(anschlüsse = aktion.anschlüsse) else it
    })
    is KartenAktion.KnotenKonfigurationErsetzen -> {
        val gültigeAnschlüsse = aktion.anschlüsse.map { it.id }.toSet()
        copy(
            knoten = knoten.map {
                if (it.id == aktion.id) it.copy(parameter = aktion.parameter, anschlüsse = aktion.anschlüsse) else it
            },
            verbindungen = verbindungen.filterNot { verbindung ->
                (verbindung.von.knotenId == aktion.id && verbindung.von.anschlussId !in gültigeAnschlüsse) ||
                    (verbindung.zu.knotenId == aktion.id && verbindung.zu.anschlussId !in gültigeAnschlüsse)
            },
        )
    }
    is KartenAktion.KnotenErsetzen -> {
        val gültigeAnschlüsse = aktion.knoten.anschlüsse.map { it.id }.toSet()
        copy(
            knoten = knoten.map { if (it.id == aktion.knoten.id) aktion.knoten else it },
            verbindungen = verbindungen.filterNot { verbindung ->
                (verbindung.von.knotenId == aktion.knoten.id && verbindung.von.anschlussId !in gültigeAnschlüsse) ||
                    (verbindung.zu.knotenId == aktion.knoten.id && verbindung.zu.anschlussId !in gültigeAnschlüsse)
            },
        )
    }
    is KartenAktion.KnotenLöschen -> copy(
        knoten = knoten.filterNot { it.id == aktion.id },
        verbindungen = verbindungen.filterNot { it.von.knotenId == aktion.id || it.zu.knotenId == aktion.id },
    )
    is KartenAktion.KnotenMehrfachLöschen -> copy(
        knoten = knoten.filterNot { it.id in aktion.ids },
        verbindungen = verbindungen.filterNot { it.von.knotenId in aktion.ids || it.zu.knotenId in aktion.ids },
    )
    is KartenAktion.VisuelleGruppeErstellen -> {
        val gültigeIds = aktion.knotenIds.intersect(knoten.mapTo(mutableSetOf()) { it.id })
        val geometrie = visuelleGruppenGeometrieFür(gültigeIds)
        if (gültigeIds.size < 2 || geometrie == null) this else {
            val übrigeGruppen = visuelleGruppen.map { gruppe ->
                gruppe.copy(knotenIds = gruppe.knotenIds - gültigeIds)
            }
            copy(
                visuelleGruppen = übrigeGruppen + VisuelleKnotenGruppeDaten(
                    knotenIds = gültigeIds,
                    titel = aktion.titel,
                    position = geometrie.first,
                    größe = geometrie.second,
                ),
            )
        }
    }
    is KartenAktion.VisuelleGruppeVerschieben -> {
        val gruppe = visuelleGruppen.firstOrNull { it.id == aktion.id }
        if (gruppe == null || aktion.delta == GraphPunkt.Zero) this else copy(
            knoten = knoten.map { knoten ->
                if (knoten.id in gruppe.knotenIds) knoten.copy(position = knoten.position + aktion.delta) else knoten
            },
            visuelleGruppen = visuelleGruppen.map {
                if (it.id == aktion.id) it.copy(position = it.position + aktion.delta) else it
            },
        )
    }
    is KartenAktion.VisuelleGruppeGrößeÄndern -> copy(
        visuelleGruppen = visuelleGruppen.map {
            if (it.id == aktion.id) it.copy(größe = aktion.größe.alsGültigeVisuelleGruppenGröße()) else it
        },
    )
    is KartenAktion.VisuelleGruppeTitelÄndern -> copy(
        visuelleGruppen = visuelleGruppen.map {
            if (it.id == aktion.id) it.copy(titel = aktion.titel) else it
        },
    )
    is KartenAktion.VisuelleGruppenKinderZuordnen -> {
        val gruppe = visuelleGruppen.firstOrNull { it.id == aktion.id }
        if (gruppe == null) this else {
            val enthalteneIds = vollständigEnthalteneKnoten(gruppe)
            copy(visuelleGruppen = visuelleGruppen.map {
                when (it.id) {
                    aktion.id -> it.copy(knotenIds = enthalteneIds)
                    else -> it.copy(knotenIds = it.knotenIds - enthalteneIds)
                }
            })
        }
    }
    is KartenAktion.VisuelleGruppeLöschen -> copy(
        visuelleGruppen = visuelleGruppen.filterNot { it.id == aktion.id },
    )
    is KartenAktion.VisuelleGruppierungAufheben -> copy(
        visuelleGruppen = visuelleGruppen.map { gruppe ->
            gruppe.copy(knotenIds = gruppe.knotenIds - aktion.knotenIds)
        },
    )
    is KartenAktion.KnotenIsolieren -> copy(
        verbindungen = verbindungen.filterNot { it.von.knotenId == aktion.id || it.zu.knotenId == aktion.id },
    )
    is KartenAktion.VerbindungEinfügen -> fügeVerbindungEin(aktion.verbindung)
    is KartenAktion.VerbindungNeuVerbinden -> copy(
        verbindungen = verbindungen.filterNot { it.id == aktion.alteVerbindung },
    ).fügeVerbindungEin(aktion.verbindung)
    is KartenAktion.VerbindungLöschen -> copy(verbindungen = verbindungen.filterNot { it.id == aktion.id })
    is KartenAktion.AnsichtÄndern -> copy(ansicht = aktion.ansicht)
}

/** Erzwingt die Graph-Invariante, dass ein gerichteter Eingang höchstens eine eingehende Kante besitzt. */
private fun KartenDaten.fügeVerbindungEin(verbindung: VerbindungDaten): KartenDaten {
    val zielIstEingang = findeAnschluss(verbindung.zu)?.richtung == AnschlussRichtung.Eingang
    val bereinigt = if (zielIstEingang) verbindungen.filterNot { it.zu == verbindung.zu } else verbindungen
    return copy(verbindungen = bereinigt + verbindung)
}

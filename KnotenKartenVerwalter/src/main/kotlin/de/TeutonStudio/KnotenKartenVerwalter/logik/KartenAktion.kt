package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

sealed interface KartenAktion {
    data class KnotenEinfügen(val knoten: KnotenDaten) : KartenAktion
    data class KnotenVerschieben(val id: KnotenId, val position: GraphPunkt) : KartenAktion
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
    data class KnotenLöschen(val id: KnotenId) : KartenAktion
    /** Entfernt nur die Verbindungen eines Knotens; der Knoten selbst bleibt erhalten. */
    data class KnotenIsolieren(val id: KnotenId) : KartenAktion
    data class VerbindungEinfügen(val verbindung: VerbindungDaten) : KartenAktion
    data class VerbindungLöschen(val id: VerbindungsId) : KartenAktion
    data class AnsichtÄndern(val ansicht: AnsichtsFenster) : KartenAktion
}

fun KartenDaten.wendeAn(aktion: KartenAktion): KartenDaten = when (aktion) {
    is KartenAktion.KnotenEinfügen -> copy(knoten = knoten + aktion.knoten)
    is KartenAktion.KnotenVerschieben -> copy(knoten = knoten.map { if (it.id == aktion.id) it.copy(position = aktion.position) else it })
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
    is KartenAktion.KnotenLöschen -> copy(
        knoten = knoten.filterNot { it.id == aktion.id },
        verbindungen = verbindungen.filterNot { it.von.knotenId == aktion.id || it.zu.knotenId == aktion.id },
    )
    is KartenAktion.KnotenIsolieren -> copy(
        verbindungen = verbindungen.filterNot { it.von.knotenId == aktion.id || it.zu.knotenId == aktion.id },
    )
    is KartenAktion.VerbindungEinfügen -> copy(verbindungen = verbindungen + aktion.verbindung)
    is KartenAktion.VerbindungLöschen -> copy(verbindungen = verbindungen.filterNot { it.id == aktion.id })
    is KartenAktion.AnsichtÄndern -> copy(ansicht = aktion.ansicht)
}

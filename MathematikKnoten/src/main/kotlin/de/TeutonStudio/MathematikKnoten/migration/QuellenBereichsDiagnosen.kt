package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

internal const val QUELLEN_BEREICH_DIAGNOSE_PREFIX="Quellenbereich bleibt erhalten: "
internal const val QUELLEN_IDENTITAET_DIAGNOSE_PREFIX="Quellenidentität bleibt erhalten: "
private class QuellenIdentitaetsFehler(message:String):IllegalArgumentException(message)

/** Originaldaten bewahren; nur eine ausdrücklich gültige Konfiguration löst die eigene Diagnose. */
fun KartenDaten.diagnostiziereQuellenBereiche():KartenDaten=copy(knoten=knoten.map {k ->
    if(k.art !in setOf("mathematik.variable","mathematik.allgemeinerParameter")) return@map k
    val vorher=k.parameter[RECHNER_MIGRATION_DIAGNOSE]
    if(vorher!=null && !vorher.startsWith(QUELLEN_BEREICH_DIAGNOSE_PREFIX) && !vorher.startsWith(QUELLEN_IDENTITAET_DIAGNOSE_PREFIX)) return@map k
    val original=if(vorher==null) k else k.copy(parameter=k.parameter-RECHNER_MIGRATION_DIAGNOSE)
    runCatching {
        if(knoten.count {it.id==k.id}!=1) throw QuellenIdentitaetsFehler("Die Quellen-ID ist mehrfach vorhanden. Die Quellen-IDs müssen ausdrücklich bereinigt werden.")
        if(k.art=="mathematik.allgemeinerParameter") allgemeinerParameterWert(original)
        else {
            val vertrag=k.eigenschaften[VARIABLE_GRUNDRAUM_VERTRAG]
            require(vertrag==null || vertrag==KnotenEigenschaft.Text("1")) {"Unbekannter Grundraumvertrag der Variable."}
            val ports=k.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang && it.name=="grundraum"}
            require(ports.size<=1) {"Der Grundraumeingang ist mehrdeutig."}
            val verbunden=ports.any {p -> verbindungen.any {it.zu==AnschlussVerweis(k.id,p.id)}}
            if(vertrag==null && !verbunden) {
                val struktur=k.parameter[VARIABLE_STRUKTUR] ?: VARIABLE_STRUKTUR_ZAHL
                require(struktur in setOf("zahl","tupel","winkeltupel","zeile","spalte")) {"Unbekannte historische Variablenstruktur '$struktur'."}
                if(struktur!=VARIABLE_STRUKTUR_ZAHL && VARIABLE_DIMENSION in k.parameter)
                    require(k.parameter[VARIABLE_DIMENSION]?.toIntOrNull()!=null) {"Die historische Variablendimension ist keine ganze Zahl."}
                if(struktur==VARIABLE_STRUKTUR_ZAHL && MATRIX_VARIABLE_DIMENSION in k.parameter)
                    require(k.parameter[MATRIX_VARIABLE_DIMENSION]?.toIntOrNull()!=null) {"Die historische Matrixdimension ist keine ganze Zahl."}
                val modus=k.parameter[VARIABLE_BEREICH_MODUS]
                require(modus==null || modus in setOf(VARIABLE_BEREICH_GRUNDMENGE,VARIABLE_BEREICH_INTERVALL)) {"Unbekannter historischer Bereichsmodus '$modus'."}
                if(modus==VARIABLE_BEREICH_INTERVALL) listOf(VARIABLE_INTERVALL_LINKS_OFFEN,VARIABLE_INTERVALL_RECHTS_OFFEN).forEach {feld ->
                    require(feld !in k.parameter || k.parameter[feld] in setOf("true","false")) {"Ungültige historische Intervalloption '$feld'."}
                }
                historischerVariablenGrundraum(k,historischeVariablenBasis(k))
                if(knoten.any {it.id.wert=="${k.id.wert}:migration-grundraum"}) throw QuellenIdentitaetsFehler("Die ID der historischen Grundraumquelle ist bereits belegt.")
            }
        }
        original
    }.getOrElse {fehler ->
        val prefix=if(fehler is QuellenIdentitaetsFehler) QUELLEN_IDENTITAET_DIAGNOSE_PREFIX else QUELLEN_BEREICH_DIAGNOSE_PREFIX
        original.copy(parameter=original.parameter+(RECHNER_MIGRATION_DIAGNOSE to
            "$prefix${fehler.message ?: "Ungültige Konfiguration."} Originaldaten sind exportierbar; die angegebene Konfiguration ausdrücklich reparieren."))
    }
})

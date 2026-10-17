package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypInferenzRegel

internal val strukturKonsolidierungAltarten = setOf(SPUR_ART, MATRIXDIAGONALE_ART,
    "mathematik.vektorRadiusSpalte", "mathematik.vektorRadiusZeile",
    "mathematik.kreuzproduktSpalte", "mathematik.kreuzproduktZeile")

/** Eindeutige Rollen statt positionsbasierter Vermutungen; Originaldaten bleiben bei Ablehnung erhalten. */
fun KartenDaten.migriereStrukturKonsolidierung(): KartenDaten {
    val rollen = mutableMapOf<KnotenId, Map<String,String>>()
    val neu = knoten.map { alt ->
        if (alt.art !in strukturKonsolidierungAltarten || RECHNER_MIGRATION_DIAGNOSE in alt.parameter) return@map alt
        runCatching {
            val matrix = alt.art in setOf(SPUR_ART,MATRIXDIAGONALE_ART)
            val kreuz = alt.art.startsWith("mathematik.kreuzprodukt")
            var parameter = alt.parameter
            val operator: String
            val eingangsRollen: Map<String,String>
            val ausgangsRollen: Map<String,String>
            when {
                alt.art==SPUR_ART -> {
                    operator=MatrixRechnerOperator.SPUR.stabileId
                    eingangsRollen=mapOf("matrix" to "matrix"); ausgangsRollen=mapOf("spur" to "wert")
                }
                alt.art==MATRIXDIAGONALE_ART -> {
                    val modus=parameter[MATRIXDIAGONALE_MODUS] ?: MATRIXDIAGONALE_AUSLESEN
                    require(modus in setOf(MATRIXDIAGONALE_AUSLESEN,MATRIXDIAGONALE_ERZEUGEN)) { "Unbekannter Diagonalmodus '$modus'." }
                    val art=matrixRechnerDiagonalArt(alt)
                    matrixRechnerDiagonalAusgabe(alt)
                    if(modus==MATRIXDIAGONALE_ERZEUGEN) {
                        operator=MatrixRechnerOperator.DIAGONALMATRIX_ERZEUGEN.stabileId
                        eingangsRollen=mapOf("werte" to "werte");ausgangsRollen=mapOf("matrix" to "wert")
                    } else {
                        operator=if(art==MatrixDiagonalArt.HAUPTDIAGONALE) MatrixRechnerOperator.HAUPTDIAGONALE.stabileId else MatrixRechnerOperator.NEBENDIAGONALE.stabileId
                        eingangsRollen=mapOf("matrix" to "matrix");ausgangsRollen=mapOf("diagonale" to "wert")
                    }
                }
                kreuz -> {
                    val orient=parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: if(alt.art.endsWith("Zeile")) VEKTOR_ORIENTIERUNG_ZEILE else VEKTOR_ORIENTIERUNG_SPALTE
                    parameter=parameter+(VEKTOR_ORIENTIERUNG_PARAMETER to orient)
                    kreuzproduktRechnerOrientierung(alt.copy(parameter=parameter))
                    operator=VektorRechnerOperator.KREUZPRODUKT.stabileId
                    eingangsRollen=mapOf("a" to "links","b" to "rechts");ausgangsRollen=mapOf("vektor" to "vektor")
                }
                else -> {
                    operator=VektorRechnerOperator.NORM.stabileId
                    eingangsRollen=mapOf("vektor" to "vektor");ausgangsRollen=mapOf("wert" to "norm")
                }
            }
            val eingänge=alt.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}
            val ausgänge=alt.anschlüsse.filter {it.richtung==AnschlussRichtung.Ausgang}
            require(alt.anschlüsse.size==eingänge.size+ausgänge.size &&
                eingänge.map {it.name}.toSet()==eingangsRollen.keys && eingänge.size==eingangsRollen.size &&
                ausgänge.map {it.name}.toSet()==ausgangsRollen.keys && ausgänge.size==ausgangsRollen.size &&
                alt.anschlüsse.map {it.id}.distinct().size==alt.anschlüsse.size &&
                alt.anschlüsse.none {it.belegung!=AnschlussBelegung.Einzeln || it.kannSichErweitern || it.sammelAnschluss!=null}) {
                "Die Anschlussrollen oder Kardinalitäten sind nicht eindeutig."
            }
            require(knoten.count {it.id==alt.id}==1 && eingänge.all { p ->
                verbindungen.count {it.zu==AnschlussVerweis(alt.id,p.id)}<=1
            }) { "Die gespeicherten Knotenidentitäten oder Einzelbelegungen sind mehrdeutig." }
            val register=AnschlussArtRegister(MathematikAnschlussArten.alle)
            fun kompatibel(port:AnschlussDaten, arten:Set<AnschlussArtId>) =
                (setOf(port.art)+port.zulässigeArten).all { art -> arten.any {register.istUnterart(art,it)} }
            eingänge.forEach { port ->
                val erlaubt=when {
                    port.name=="matrix" -> setOf(MathematikAnschlussArten.Matrix.id,MathematikAnschlussArten.Tensor.id)
                    port.name=="werte" -> setOf(MathematikAnschlussArten.Tupel.id,MathematikAnschlussArten.Vektor.id,MathematikAnschlussArten.Objekt.id)
                    else -> setOf(MathematikAnschlussArten.Vektor.id)
                }
                require(kompatibel(port,erlaubt)) { "Unbekannter Eingangsvertrag '${port.name}'." }
            }
            val erwarteterAusgang=when {
                alt.art==SPUR_ART || !matrix && !kreuz -> MathematikAnschlussArten.Zahl.id
                kreuz -> if(parameter[VEKTOR_ORIENTIERUNG_PARAMETER]==VEKTOR_ORIENTIERUNG_ZEILE) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id
                operator==MatrixRechnerOperator.DIAGONALMATRIX_ERZEUGEN.stabileId -> MathematikAnschlussArten.Matrix.id
                else -> when(matrixRechnerDiagonalAusgabe(alt)) {
                    VektorStrukturAusgabe.TUPEL -> MathematikAnschlussArten.Tupel.id
                    VektorStrukturAusgabe.ZEILE -> MathematikAnschlussArten.ZeilenVektor.id
                    VektorStrukturAusgabe.SPALTE -> MathematikAnschlussArten.SpaltenVektor.id
                }
            }
            require(kompatibel(ausgänge.single(),setOf(erwarteterAusgang))) { "Der gespeicherte Ergebnisvertrag widerspricht dem Modus." }
            if(kreuz) require(eingänge.all {kompatibel(it,setOf(erwarteterAusgang))}) { "Die gespeicherten Vektorrollen widersprechen der Orientierung." }
            val umbenannt=alt.copy(art=if(matrix) MatrixRechner.KNOTEN_ART else VektorRechner.KNOTEN_ART,
                anschlüsse=alt.anschlüsse.map {p -> p.remappeStrukturRollen(eingangsRollen).copy(name=(if(p.richtung==AnschlussRichtung.Eingang) eingangsRollen else ausgangsRollen).getValue(p.name))},
                parameter=remappeStrukturParameter(parameter,eingangsRollen,ausgangsRollen)+(RECHNER_OPERATOR_PARAMETER to operator),
                eingangsKartenVerweise=remappeEindeutig(alt.eingangsKartenVerweise) { rolle -> remappeFallbackRolle(rolle,eingangsRollen) })
            val zielKonfiguration=if(matrix) konfiguriereStrukturRechner(umbenannt,StrukturRechnerKnotenFamilie.MATRIX,operator)
                else konfiguriereVektorRechner(umbenannt,requireNotNull(VektorRechnerOperator.vonIdOderNull(operator)))
            // Die Rollen sind bereits vollständig geprüft; ihre Instanz-IDs überleben die Typverbreiterung.
            val konfiguriert=zielKonfiguration.copy(anschlüsse=zielKonfiguration.anschlüsse.map { p ->
                val original=umbenannt.anschlüsse.singleOrNull {it.name==p.name && it.richtung==p.richtung}
                    ?: error("Die Zielkonfiguration fügt eine unbekannte Anschlussrolle hinzu.")
                p.copy(id=original.id)
            })
            require(konfiguriert.anschlüsse.map {it.id}.toSet()==alt.anschlüsse.map {it.id}.toSet()) { "Die Anschlussidentitäten sind nicht vollständig erhaltbar." }
            val zielPorts=konfiguriert.anschlüsse.associateBy {it.id}
            rollen[alt.id]=eingangsRollen
            konfiguriert.copy(name=alt.name,anschlüsse=umbenannt.anschlüsse.map {p ->
                val ziel=zielPorts.getValue(p.id)
                p.copy(art=ziel.art,zulässigeArten=ziel.zulässigeArten,
                    artPriorisiertEingänge=p.artPriorisiertEingänge ?: ziel.artPriorisiertEingänge)
            })
        }.getOrElse { fehler -> alt.copy(parameter=alt.parameter+(RECHNER_MIGRATION_DIAGNOSE to "${alt.art} bleibt erhalten: ${fehler.message}")) }
    }
    return copy(knoten=neu,verbindungen=verbindungen.map {v ->
        val map=rollen[v.zu.knotenId]
        if(map==null || v.eingangsFallback==null) v else v.copy(eingangsFallback=remappeFallbackRolle(requireNotNull(v.eingangsFallback),map))
    })
}

package de.TeutonStudio.MathematikKnoten.katalog

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.registriereMethodenArgumente
import de.TeutonStudio.MathematikKnoten.*

/**
 * Benannte Registrierungseinheit für einen fachlich zusammengehörenden Teil des
 * Mathematik-Auswerterregisters.
 *
 * Die Reihenfolge ist Teil des Vertrags: spätere Verfeinerungspakete dürfen
 * bewusst zuvor registrierte Auswerter umschließen oder ersetzen. Dieser
 * Mechanismus macht diese bisher nur implizite Reihenfolge sichtbar und testbar.
 */
data class MathematikAuswerterPaket(
    val name: String,
    val registrieren: MathematikAuswerterRegister.() -> Unit,
) {
    fun installiereIn(register: MathematikAuswerterRegister) {
        registrieren.invoke(register)
    }
}

object StandardMathematikAuswerterPakete {
    /** Additive fachliche Registrierungen ohne beabsichtigte finale Wrapperrolle. */
    val basis: List<MathematikAuswerterPaket> = listOf(
        MathematikAuswerterPaket("übersetzungen") { registriereÜbersetzungsKnoten() },
        MathematikAuswerterPaket("zahlenrechner") { registriereUniversellenZahlenRechner() },
        MathematikAuswerterPaket("zahlenrechner-erweiterungen") { registriereZahlenRechnerErweiterungen() },
        MathematikAuswerterPaket("tupeloperationen") { registriereTupelOperationKnoten() },
        MathematikAuswerterPaket("transponieren") { registriereTransponieren() },
        MathematikAuswerterPaket("geometrie-grundobjekte") { registriereGeometrieGrundobjekte() },
        MathematikAuswerterPaket("geometrie-teilobjekte") { registriereGeometrieTeilobjekte() },
        MathematikAuswerterPaket("geometrie-transformationen") { registriereGeometrieTransformationen() },
        MathematikAuswerterPaket("mengenraeume") { registriereMengenraumKnoten() },
        MathematikAuswerterPaket("mengen-operatoren") { registriereMengenOperatorKnoten() },
        MathematikAuswerterPaket("praedikat") { registrierePraedikatKnoten() },
        MathematikAuswerterPaket("strukturrechner") { registriereStrukturRechnerKnoten() },
        MathematikAuswerterPaket("lineare-strukturen") { registriereLineareStrukturErweiterungen() },
        MathematikAuswerterPaket("lineare-algebra") { registriereLineareAlgebraGrundlagen() },
        MathematikAuswerterPaket("hyperanalysis") { registriereHyperAnalysisKnoten() },
        MathematikAuswerterPaket("differential") { registriereDifferentialKnoten() },
        MathematikAuswerterPaket("integral") { registriereIntegralKnoten() },
        MathematikAuswerterPaket("restriktion") { registriereRestriktionsKnoten() },
        MathematikAuswerterPaket("methodengraph") { registriereMethodenGraphKnoten() },
        MathematikAuswerterPaket("tangentialobjekt") { registriereTangentialKnoten() },
        MathematikAuswerterPaket("rand") { registriereRandKnoten() },
        MathematikAuswerterPaket("geometry-nodes") { registriereGeometryNodes() },
        MathematikAuswerterPaket("svg") { registriereSvgKnoten() },
        MathematikAuswerterPaket("methodenargumente") { registriereMethodenArgumente() },
        MathematikAuswerterPaket("mathematische-eigenschaften") { registriereMathematischeEigenschaften() },
        MathematikAuswerterPaket("exakte-eigenschaften") { registriereExakteEigenschaftsAuswertung() },
        MathematikAuswerterPaket("folgen-und-signaturen") { registriereFolgenUndSignaturEigenschaften() },
        MathematikAuswerterPaket("mengeneigenschaften") { registriereMengenEigenschaftsAuswertung() },
        // Muss nach den historischen Eigenschaftsauswertern laufen: dieses Paket entfernt
        // deren impliziten R-Fallback und delegiert nur nicht-topologische Altvarianten zurück.
        MathematikAuswerterPaket("topologische-strukturen") { registriereTopologischeStrukturen() },
    )

    /**
     * Bewusst nachgelagerte Adapter. Ihre Reihenfolge darf nicht alphabetisch
     * sortiert werden, weil mehrere Pakete bestehende Auswerter verfeinern.
     */
    val verfeinerungen: List<MathematikAuswerterPaket> = listOf(
        MathematikAuswerterPaket("konsolidierte-knoten") { registriereKonsolidierteKnoten() },
        MathematikAuswerterPaket("tupel-null-dimension") { registriereTupelNullDimension() },
        MathematikAuswerterPaket("vektorrechner-erweiterungen") { registriereVektorRechnerErweiterungen() },
        MathematikAuswerterPaket("skalarprodukt-verfeinerung") { registriereSkalarproduktErweiterungen() },
        MathematikAuswerterPaket("strukturformel-rechner") { registriereStrukturFormelRechner() },
        MathematikAuswerterPaket("strukturrechner-kompatibilitaet") { registriereStrukturRechnerKompatibilitaet() },
        MathematikAuswerterPaket("analysis-nullstellen") { registriereErweiterteAnalysisNullstellen() },
        MathematikAuswerterPaket("analysis-mengen-normalisierung") { registriereAnalysisMengenNormalisierung() },
        MathematikAuswerterPaket("tensor-operation-registry") { registriereTensorOperationRegistry() },
        MathematikAuswerterPaket("strukturierte-division") { registriereStrukturierteDivision() },
        MathematikAuswerterPaket("zahlenrechner-differential") { registriereZahlenRechnerDifferential() },
        MathematikAuswerterPaket("null-distanz") { registriereNullDistanz() },
        MathematikAuswerterPaket("vektorkonstruktor") { registriereVektorKonstruktor() },
        MathematikAuswerterPaket("multinomvektor") { registriereMultinomVektor() },
        MathematikAuswerterPaket("rechner-methodenhebung") { registriereRechnerMethodenHebung() },
    )

    val alle: List<MathematikAuswerterPaket>
        get() = basis + verfeinerungen

    fun installiereIn(register: MathematikAuswerterRegister) {
        alle.forEach { paket -> paket.installiereIn(register) }
        installiereKanonischeRechnerDispatcher(register)
        register.bezeugeHistorischeÜbersetzer()
    }
}

private fun installiereKanonischeRechnerDispatcher(register: MathematikAuswerterRegister) {
    // Explizite Komposition: spätere Paketregistrierungen können keinen Fachkern auswählen.
    val fachauswerterNachFamilie = mapOf(
        RechnerFamilie.ZAHL to mitNullDistanz(mitZahlenDifferential(mitStrukturierterDivision(mitZahlenErweiterungen(universellerZahlenFachauswerter)))),
        RechnerFamilie.VEKTOR to mitRechnerMethodenHebung(mitVektorAusgangKompatibilität(strukturFormelFachauswerter(StrukturRechnerKnotenFamilie.VEKTOR, mitSkalarprodukt(vektorFachauswerter)))),
        RechnerFamilie.MATRIX to mitRechnerMethodenHebung(strukturFormelFachauswerter(StrukturRechnerKnotenFamilie.MATRIX)),
        RechnerFamilie.TENSOR to mitRechnerMethodenHebung(strukturFormelFachauswerter(StrukturRechnerKnotenFamilie.TENSOR, tensorFachauswerter)),
    )
    RechnerFamilie.entries.forEach { familie ->
        val fachauswerter = fachauswerterNachFamilie.getValue(familie)
        register.registriere(familie.knotenArt, MathematikKnotenAuswerter { kontext ->
            val operatorId = kontext.knoten.parameter[familie.operatorParameter]
                ?: (if (familie == RechnerFamilie.TENSOR) kontext.knoten.parameter["operator"] else null)
            if (operatorId == null) return@MathematikKnotenAuswerter KnotenAuswertungsErgebnis(
                emptyMap(), fehler = "Gespeicherte Operator-ID fehlt für ${familie.name.lowercase()}; zuerst die Karte laden und ihre Migration prüfen.")
            when (RechnerOperatorRegistry.finde(familie, operatorId)) {
                is RechnerOperatorAuflösung.Bekannt -> fachauswerter.auswerten(kontext)
                is RechnerOperatorAuflösung.Unbekannt -> KnotenAuswertungsErgebnis(
                    ausgaben = emptyMap(),
                    fehler = "Unbekannter ${familie.name.lowercase()}-Operator '$operatorId'. Die gespeicherte ID bleibt erhalten.",
                )
            }
        })
    }
}

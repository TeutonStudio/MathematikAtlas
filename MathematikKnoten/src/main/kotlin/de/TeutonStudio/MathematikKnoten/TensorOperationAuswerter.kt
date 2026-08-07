package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger

/**
 * Registriert den Registryvertrag als letzte Tensorrechner-Schicht.
 * Konkret vorhandene Operationen verwenden weiterhin den bestehenden Rechenkern;
 * übrige gültige Operationen bleiben strukturierte symbolische Ergebnisse.
 */
internal fun MathematikAuswerterRegister.registriereTensorOperationRegistry() {
    requireNotNull(finde(TensorRechner.KNOTEN_ART)) {
        "Der bestehende Tensorrechner muss vor der Registry-Brücke registriert sein."
    }
    registriere(
        TensorRechner.KNOTEN_ART,
        MathematikKnotenAuswerter(::werteTensorOperationAus),
    )
}

private fun werteTensorOperationAus(
    kontext: KnotenAuswertungsKontext,
): KnotenAuswertungsErgebnis {
    kontext.knoten.parameter[TENSOR_ACHSEN_MIGRATIONSFEHLER]?.let { fehler ->
        return fehlerErgebnis(kontext, "$fehler Öffne den Inspector und bestätige die sichtbaren Achsen.")
    }
    val definition = aktuelleTensorOperationDefinition(kontext.knoten)
    val operanden = definition.eingangsRollen.mapNotNull { rolle ->
        tensorEingang(kontext, definition, rolle)?.objekt?.let { rolle to it }
    }.toMap(linkedMapOf())
    val fehlendeRollen = definition.eingangsRollen.filterNot(operanden::containsKey)
    if (fehlendeRollen.isNotEmpty()) {
        return fehlerErgebnis(
            kontext,
            "Fehlende Tensorrollen: ${fehlendeRollen.joinToString { it.wert }}.",
        )
    }

    val achsenErgebnis = ermittleAchsen(kontext, definition, operanden)
    if (achsenErgebnis is AchsenErgebnis.Fehler) {
        return fehlerErgebnis(kontext, achsenErgebnis.nachricht)
    }
    val achsen = (achsenErgebnis as AchsenErgebnis.Wert).spezifikation
    val legacyOperator = definition.alsBestehenderTensorOperatorOderNull()
    if (legacyOperator == null) {
        return symbolischesTensorErgebnis(kontext, definition, operanden, achsen)
    }

    val sichtbar = achsen.sichtbareIndizes()
    val stufe = tensorStufe(operanden.values)
    val interneAchsen = when (definition.id.wert) {
        TensorRechnerOperator.ACHSENPERMUTATION.stabileId -> {
            val rang = stufe ?: return fehlerErgebnis(
                kontext,
                "Für die Achsenpermutation konnte keine Tensorstufe bestimmt werden.",
            )
            val werte = if (sichtbar.isEmpty()) {
                standardTensorPermutation(rang).map { it + 1 }
            } else {
                sichtbar
            }
            runCatching { normalisiereTensorPermutation(werte, rang) }.getOrElse { fehler ->
                return fehlerErgebnis(
                    kontext,
                    fehler.message ?: "Ungültige Achsenpermutation.",
                )
            }
        }
        TensorRechnerOperator.KONTRAKTION.stabileId,
        TensorRechnerOperator.ACHSENSCHNITT.stabileId,
        -> {
            val rang = stufe ?: return fehlerErgebnis(
                kontext,
                "Für die Achsenoperation konnte keine Tensorstufe bestimmt werden.",
            )
            runCatching { normalisiereTensorAchsen(sichtbar, rang).map { it.position } }.getOrElse { fehler ->
                return fehlerErgebnis(
                    kontext,
                    fehler.message ?: "Ungültige Tensorachse.",
                )
            }
        }
        else -> emptyList()
    }
    val indizes = when (
        val gelesen = parseGanzeListe(
            kontext.knoten.parameter[TENSOR_OPERATION_PARAMETER]
                ?: kontext.knoten.parameter["indizes"],
            "Operationsparameter",
        )
    ) {
        is GanzzahlListenErgebnis.Wert -> gelesen.werte
        is GanzzahlListenErgebnis.Fehler -> return fehlerErgebnis(kontext, gelesen.nachricht)
    }
    val eingaben = operanden.map { (rolle, objekt) ->
        TensorRechnerEingabe(rolle.wert, objekt)
    }
    val rechenErgebnis = TensorRechner.erzeuge(
        operator = legacyOperator,
        eingaben = eingaben,
        konfiguration = TensorRechnerKonfiguration(
            achsen = interneAchsen,
            indizes = indizes,
            permutation = if (legacyOperator == TensorRechnerOperator.ACHSENPERMUTATION) interneAchsen else emptyList(),
        ),
    )
    return when (rechenErgebnis) {
        is TensorRechnerErgebnis.Wert -> {
            val rolle = definition.ausgangsRollen.single()
            val ausgangsName = ausgangsName(kontext, definition, rolle)
            KnotenAuswertungsErgebnis(
                ausgaben = mapOf(
                    ausgangsName to BedingterWert(
                        objekt = rechenErgebnis.objekt,
                        annahmen = kontext.gemeinsameEingangsAnnahmen(),
                        latexDarstellung = rechenErgebnis.objekt.zuLatex(),
                    ),
                ),
                warnungen = listOf(
                    "Operation: ${definition.titel}",
                    "Form: ${rechenErgebnis.form.joinToString("×")}",
                    "Status: konkret implementiert",
                ),
                eingänge = kontext.eingänge,
            )
        }
        is TensorRechnerErgebnis.Bedingt -> {
            val symbolisch = symbolischesTensorErgebnis(kontext, definition, operanden, achsen)
            symbolisch.copy(
                warnungen = (symbolisch.warnungen + rechenErgebnis.bedingungen).distinct(),
            )
        }
        is TensorRechnerErgebnis.Ungueltig -> fehlerErgebnis(kontext, rechenErgebnis.nachricht)
    }
}

private fun symbolischesTensorErgebnis(
    kontext: KnotenAuswertungsKontext,
    definition: TensorOperationDefinition,
    operanden: Map<TensorHandleRolle, MathematischesObjekt>,
    achsen: TensorAchsenSpezifikation?,
): KnotenAuswertungsErgebnis {
    val operation = TensorOperation(
        operationId = definition.id,
        operanden = operanden,
        achsen = achsen,
        parameter = kontext.knoten.parameter[TENSOR_OPERATION_PARAMETER]
            ?.takeIf(String::isNotBlank)
            ?.let { mapOf("konfiguration" to AllgemeinerParameter("konfiguration", it)) }
            ?: emptyMap(),
        unterstuetzungsStatus = definition.unterstuetzungsStatus,
    )
    return KnotenAuswertungsErgebnis(
        ausgaben = definition.ausgangsRollen.associate { rolle ->
            ausgangsName(kontext, definition, rolle) to BedingterWert(
                objekt = operation,
                annahmen = kontext.gemeinsameEingangsAnnahmen(),
                latexDarstellung = "${operation.zuLatex()}_{${rolle.wert.replace("_", "\\_")}}",
            )
        },
        warnungen = listOf(
            "Operation: ${definition.titel}",
            "Signaturfamilie: ${definition.familie.name}",
            "Status: ${definition.unterstuetzungsStatus.name}",
            "Symbolisch gültig; eine konkrete numerische Auswertung ist noch nicht registriert.",
        ),
        eingänge = kontext.eingänge,
    )
}

private fun fehlerErgebnis(
    kontext: KnotenAuswertungsKontext,
    nachricht: String,
): KnotenAuswertungsErgebnis = KnotenAuswertungsErgebnis(
    ausgaben = emptyMap(),
    fehler = nachricht,
    eingänge = kontext.eingänge,
)

private sealed interface AchsenErgebnis {
    data class Wert(val spezifikation: TensorAchsenSpezifikation?) : AchsenErgebnis
    data class Fehler(val nachricht: String) : AchsenErgebnis
}

private sealed interface GanzzahlListenErgebnis {
    data class Wert(val werte: List<Int>) : GanzzahlListenErgebnis
    data class Fehler(val nachricht: String) : GanzzahlListenErgebnis
}

private fun ermittleAchsen(
    kontext: KnotenAuswertungsKontext,
    definition: TensorOperationDefinition,
    operanden: Map<TensorHandleRolle, MathematischesObjekt>,
): AchsenErgebnis {
    if (!definition.benoetigtAchsenEingabe()) {
        return AchsenErgebnis.Wert(null)
    }
    val modus = aktuellerAchsenEingabeModus(kontext.knoten)
    val sichtbarErgebnis = when (modus) {
        AchsenEingabeModus.TUPEL -> {
            val verbunden = kontext.eingänge["achsen"]?.objekt
            when (verbunden) {
                null -> parseGanzeListe(
                    kontext.knoten.parameter[TENSOR_ACHSEN_SPEZIFIKATION]
                        ?: kontext.knoten.parameter["achsen"]
                        ?: kontext.knoten.parameter["permutation"],
                    "Achsenspezifikation",
                )
                is Tupel -> parseGanzzahlTupel(verbunden)
                else -> GanzzahlListenErgebnis.Fehler(
                    "Der Achseneingang muss ein Tupel ganzer Zahlen sein.",
                )
            }
        }
        AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES -> {
            val eintraege = kontext.eingänge.entries
                .filter { it.key.startsWith("achse.") }
                .sortedBy { it.key.substringAfter("achse.").toIntOrNull() ?: Int.MAX_VALUE }
            if (eintraege.isEmpty()) {
                parseGanzeListe(
                    kontext.knoten.parameter[TENSOR_ACHSEN_SPEZIFIKATION],
                    "Achsenspezifikation",
                )
            } else {
                val werte = eintraege.map { ganzzahlOderNull(it.value.objekt) }
                if (werte.any { it == null }) {
                    GanzzahlListenErgebnis.Fehler(
                        "Jeder dynamische Achseneingang muss eine ganze Zahl im Int-Bereich liefern.",
                    )
                } else {
                    GanzzahlListenErgebnis.Wert(werte.filterNotNull())
                }
            }
        }
    }
    val sichtbar = when (sichtbarErgebnis) {
        is GanzzahlListenErgebnis.Wert -> sichtbarErgebnis.werte
        is GanzzahlListenErgebnis.Fehler -> return AchsenErgebnis.Fehler(sichtbarErgebnis.nachricht)
    }
    if (!definition.pruefeAchsenAnzahl(sichtbar.size)) {
        return AchsenErgebnis.Fehler(
            "${definition.titel} erwartet ${definition.minimaleAchsenAnzahl}" +
                (definition.maximaleAchsenAnzahl?.let { " bis $it" } ?: " oder mehr") +
                " Achsen, erhielt aber ${sichtbar.size}.",
        )
    }
    val spezifikation = when (modus) {
        AchsenEingabeModus.TUPEL -> TensorAchsenSpezifikation.Tupel(sichtbar)
        AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES -> TensorAchsenSpezifikation.Dynamisch(
            sichtbar.mapIndexed { index, wert -> TensorHandleRolle("achse.${index + 1}") to wert }.toMap(),
        )
    }
    val stufe = tensorStufe(operanden.values)
    if (stufe != null) {
        runCatching { normalisiereTensorAchsen(sichtbar, stufe) }.getOrElse { fehler ->
            return AchsenErgebnis.Fehler(fehler.message ?: "Ungültige Tensorachse.")
        }
    }
    return AchsenErgebnis.Wert(spezifikation)
}

private fun tensorEingang(
    kontext: KnotenAuswertungsKontext,
    definition: TensorOperationDefinition,
    rolle: TensorHandleRolle,
): BedingterWert? = kontext.eingänge[rolle.wert] ?: when {
    rolle.wert == "skalar" && definition.id.wert == TensorRechnerOperator.SKALARMULTIPLIKATION.stabileId ->
        kontext.eingänge["links"]
    rolle.wert == "tensor" && definition.id.wert == TensorRechnerOperator.SKALARMULTIPLIKATION.stabileId ->
        kontext.eingänge["rechts"]
    rolle.wert == "tensor" -> kontext.eingänge["links"]
    else -> null
}

private fun ausgangsName(
    kontext: KnotenAuswertungsKontext,
    definition: TensorOperationDefinition,
    rolle: TensorHandleRolle,
): String {
    val vorhanden = kontext.knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Ausgang }
        .mapTo(linkedSetOf()) { it.name }
    return when {
        rolle.wert in vorhanden -> rolle.wert
        definition.ausgangsRollen.size == 1 && "wert" in vorhanden -> "wert"
        else -> rolle.wert
    }
}

private fun tensorStufe(objekte: Collection<MathematischesObjekt>): Int? = objekte.firstNotNullOfOrNull { objekt ->
    when (val ansicht = objekt.tensorielleAnsicht()) {
        is StrukturPruefung.Gueltig -> ansicht.wert.stufe
        else -> null
    }
}

private fun TensorAchsenSpezifikation?.sichtbareIndizes(): List<Int> = when (this) {
    null -> emptyList()
    is TensorAchsenSpezifikation.Tupel -> sichtbareIndizes
    is TensorAchsenSpezifikation.Dynamisch -> sichtbareIndizesNachRolle.entries
        .sortedBy { it.key.wert }
        .map { it.value }
}

private fun parseGanzeListe(
    text: String?,
    bezeichnung: String,
): GanzzahlListenErgebnis {
    val teile = text.orEmpty()
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
    if (teile.isEmpty()) return GanzzahlListenErgebnis.Wert(emptyList())
    val werte = teile.map(String::toIntOrNull)
    val fehlerIndex = werte.indexOfFirst { it == null }
    return if (fehlerIndex >= 0) {
        GanzzahlListenErgebnis.Fehler(
            "$bezeichnung enthält mit '${teile[fehlerIndex]}' keinen gültigen Ganzzahlwert.",
        )
    } else {
        GanzzahlListenErgebnis.Wert(werte.filterNotNull())
    }
}

private fun parseGanzzahlTupel(tupel: Tupel): GanzzahlListenErgebnis {
    val werte = tupel.elemente.map(::ganzzahlOderNull)
    val fehlerIndex = werte.indexOfFirst { it == null }
    return if (fehlerIndex >= 0) {
        GanzzahlListenErgebnis.Fehler(
            "Die ${fehlerIndex + 1}. Achsenkomponente ist keine ganze Zahl im Int-Bereich.",
        )
    } else {
        GanzzahlListenErgebnis.Wert(werte.filterNotNull())
    }
}

private fun ganzzahlOderNull(objekt: MathematischesObjekt): Int? = (objekt as? RationaleZahl)
    ?.takeIf { it.nenner == BigInteger.ONE && it.zähler.bitLength() < 31 }
    ?.zähler
    ?.toInt()
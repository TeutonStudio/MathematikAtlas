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
internal val tensorFachauswerter = de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter { kontext ->
    val ergebnis = werteTensorOperationAus(kontext)
    val quellen = kontext.eingänge.values.flatMap { it.variablenQuellen }
    ergebnis.copy(ausgaben = ergebnis.ausgaben.mapValues { (_, wert) -> wert.copy(
        variablenQuellen = (wert.variablenQuellen + quellen).distinctBy { it.identität },
        reelleVariablen = de.TeutonStudio.MathematikKartenAdapter.reelleVariablen(kontext.eingänge.values) + wert.reelleVariablen,
    ) })
}
internal fun MathematikAuswerterRegister.registriereTensorOperationRegistry() {
    registriere(TensorRechner.KNOTEN_ART, tensorFachauswerter)
}

private fun werteTensorOperationAus(
    kontext: KnotenAuswertungsKontext,
): KnotenAuswertungsErgebnis {
    kontext.knoten.parameter[TENSOR_ACHSEN_MIGRATIONSFEHLER]?.let { fehler ->
        return fehlerErgebnis(kontext, "$fehler Öffne den Inspector und bestätige die sichtbaren Achsen.")
    }
    val definition = aktuelleTensorOperationDefinition(kontext.knoten)
    if (definition.id.wert == "tensor.konstruktion" && kontext.knoten.parameter["erzeugungsArt"] == "indexmethode") {
        val methode = kontext.eingänge["methode"]?.objekt as? MathematischeMethode ?: error("Komponentenabbildung fehlt.")
        val vorbild = kontext.eingänge["vorbild"]?.objekt as? MathematischesObjekt
        val wert = strukturAusIndexMethode(methode, kontext.knoten.parameter["indexmethode.ausgabe"] ?: "tensor", vorbild)
        return KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(wert, kontext.annahmen(),variablenQuellen=kontext.eingänge.values.flatMap { it.variablenQuellen }.distinct())), eingänge = kontext.eingänge)
    }
    if (istAssoziativerRechnerOperator(definition.id.wert)) {
        val werte = runCatching { geordneteOperationsWerte(kontext).values.toList() }.getOrElse {
            return fehlerErgebnis(kontext, it.message ?: "Ungültige Operandenliste.")
        }
        val operator = definition.alsBestehenderTensorOperatorOderNull()
            ?: return fehlerErgebnis(kontext, "Unbekannter assoziativer Tensoroperator.")
        if (werte.size == 1) {
            return when (val geprüft = TensorRechner.erzeuge(operator, listOf(TensorRechnerEingabe("links", werte.single())))) {
                is TensorRechnerErgebnis.Wert -> KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(geprüft.objekt, kontext.annahmen())))
                is TensorRechnerErgebnis.Ungueltig -> fehlerErgebnis(kontext, geprüft.nachricht)
                is TensorRechnerErgebnis.Bedingt -> fehlerErgebnis(kontext, geprüft.bedingungen.joinToString())
            }
        }
        var wert = werte.first()
        var symbolisch = false
        val bedingungen = mutableListOf<String>()
        for (rechts in werte.drop(1)) {
            wert = when (val ergebnis = TensorRechner.erzeuge(operator,
                listOf(TensorRechnerEingabe("links", wert), TensorRechnerEingabe("rechts", rechts)))) {
                is TensorRechnerErgebnis.Wert -> ergebnis.objekt
                is TensorRechnerErgebnis.Ungueltig -> return fehlerErgebnis(kontext, ergebnis.nachricht)
                is TensorRechnerErgebnis.Bedingt -> {
                    symbolisch = true
                    bedingungen += ergebnis.bedingungen
                    TensorOperation(definition.id,
                        linkedMapOf(TensorHandleRolle("links") to wert, TensorHandleRolle("rechts") to rechts),
                        unterstuetzungsStatus = definition.unterstuetzungsStatus)
                }
            }
        }
        val rolle = definition.ausgangsRollen.single()
        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(ausgangsName(kontext, definition, rolle) to BedingterWert(wert, kontext.annahmen())),
            warnungen = listOf("Operation: ${definition.titel}", if (symbolisch) "Status: symbolisch" else "Status: konkret implementiert") + bedingungen,
            eingänge = kontext.eingänge,
        )
    }
    val operanden = definition.eingangsRollen.mapNotNull { rolle ->
        tensorEingang(kontext, definition, rolle)?.mathematischesObjekt("Tensoroperand ${rolle.wert}")?.let { rolle to it }
    }.toMap(linkedMapOf())
    val fehlendeRollen = definition.eingangsRollen.filterNot(operanden::containsKey)
    if (fehlendeRollen.isNotEmpty()) {
        return fehlerErgebnis(
            kontext,
            "Fehlende Tensorrollen: ${fehlendeRollen.joinToString { it.wert }}.",
        )
    }

    if(definition.id.wert in setOf("tensor.reshape","tensor.konstruktion")) {
        val form=kontext.knoten.parameter[TENSOR_OPERATION_PARAMETER].orEmpty().split(',').map { it.trim().toIntOrNull() ?: error("Die Zielform muss ganze Achsenlängen enthalten.") }
        val methode=umgeformteStrukturIndexMethode(operanden.values.single(),form,kontext.eingänge.values.flatMap { it.variablenQuellen }.associate { it.name to it.werteVorrat },kontext.annahmen())
        val tensor=strukturAusIndexMethode(methode,"tensor")
        return KnotenAuswertungsErgebnis(mapOf(ausgangsName(kontext,definition,definition.ausgangsRollen.single()) to BedingterWert(tensor,kontext.annahmen())),eingänge=kontext.eingänge)
    }
    if(definition.id.wert in setOf("matrix.rang","matrix.determinante","matrix.inverse")) {
        val matrix=matrixAusZweiachsigerStruktur(operanden.values.single())
        val operator=MatrixRechnerOperator.entries.single { it.stabileId==definition.id.wert }
        val vorräte=kontext.eingänge.values.flatMap { it.variablenQuellen }.associate { it.name to it.werteVorrat }
        val bereich=maximaleZahlenGrundmenge(matrix.zeilen.flatten().map { inferiereZahlenWertevorrat(it,vorräte,kontext.annahmen()) }).fundamentalerZahlbereichOderNull() ?: FundamentalerZahlbereich.REELL
        return when(val ergebnis=MatrixRechner.erzeuge(MatrixRechnerAnfrage(operator,listOf(MatrixOperand("matrix",matrix,bereich))))) {
            is MatrixRechnerErgebnis.ZahlWert -> KnotenAuswertungsErgebnis(mapOf(ausgangsName(kontext,definition,definition.ausgangsRollen.single()) to BedingterWert(ergebnis.wert,kontext.annahmen()+ergebnis.bedingungen)),eingänge=kontext.eingänge)
            is MatrixRechnerErgebnis.MatrixWert -> KnotenAuswertungsErgebnis(mapOf(ausgangsName(kontext,definition,definition.ausgangsRollen.single()) to BedingterWert(Tensor(ergebnis.wert.tensorForm,ergebnis.wert.zeilen.flatten()),kontext.annahmen()+ergebnis.bedingungen)),eingänge=kontext.eingänge)
            is MatrixRechnerErgebnis.Bedingt -> symbolischesTensorErgebnis(kontext,definition,operanden,null).let { it.copy(warnungen=it.warnungen+ergebnis.bedingungen) }
            is MatrixRechnerErgebnis.Ungueltig -> fehlerErgebnis(kontext,ergebnis.nachricht)
            else -> error("Der Matrixrang liefert keinen Zahlwert.")
        }
    }
    val achsenErgebnis = ermittleAchsen(kontext, definition, operanden)
    if (achsenErgebnis is AchsenErgebnis.Fehler) {
        return fehlerErgebnis(kontext, achsenErgebnis.nachricht)
    }
    val achsen = (achsenErgebnis as AchsenErgebnis.Wert).spezifikation
    if (definition.id.wert == TensorRechnerOperator.ZERLEGEN.stabileId) {
        return werteTensorZerlegenAus(kontext, operanden.values.single(), achsen)
    }
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
                        annahmen = kontext.annahmen() + rechenErgebnis.bedingungen,
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
                annahmen = kontext.annahmen(),
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
    if (objekt is TypisiertesElement) return@firstNotNullOfOrNull objekt.strukturForm?.size
    when (val ansicht = objekt.tensorielleAnsicht()) {
        is StrukturPruefung.Gueltig -> ansicht.wert.stufe
        else -> null
    }
}

private fun werteTensorZerlegenAus(
    kontext: KnotenAuswertungsKontext,
    tensor: MathematischesObjekt,
    achsen: TensorAchsenSpezifikation?,
): KnotenAuswertungsErgebnis {
    val sichtbar = achsen.sichtbareIndizes()
    if (sichtbar.size != 1) return fehlerErgebnis(kontext, "Tensor-Zerlegen benötigt genau eine Achse.")
    val form = when (tensor) {
        is Tensorartig -> tensor.tensorForm.map { RationaleZahl.von(it.toLong()) }
        is TypisiertesElement -> tensor.strukturForm
        else -> return fehlerErgebnis(kontext, "Tensor-Zerlegen akzeptiert ausschließlich tensorartige Objekte.")
    }
    val achse = sichtbar.single() - 1
    if (achse < 0 || form != null && achse !in form.indices) {
        return fehlerErgebnis(kontext, "Achse ${sichtbar.single()} liegt außerhalb der Tensorordnung${form?.size?.let { " $it" }.orEmpty()}.")
    }
    val relevanteLaenge = form?.getOrNull(achse)
    val konkret = relevanteLaenge?.tensorDimensionOderNull()
    val restForm = form?.filterIndexed { index, _ -> index != achse }
    val ausgaben = if (konkret != null) {
        List(konkret) { index ->
            "schnitt${index + 1}" to BedingterWert(
                tensorSchnittWert(tensor, achse, index, restForm),
                kontext.annahmen(),
            )
        }.toMap()
    } else {
        mapOf("methode" to BedingterWert(tensorZerlegeMethode(tensor, achse, restForm), kontext.annahmen()))
    }
    return KnotenAuswertungsErgebnis(ausgaben = ausgaben, eingänge = kontext.eingänge)
}

fun tensorZerlegeMethode(
    tensor: MathematischesObjekt,
    achse: Int,
    restForm: List<ZahlAusdruck>? = (tensor as? TypisiertesElement)?.strukturForm
        ?.filterIndexed { index, _ -> index != achse },
): MathematischeMethode {
    val index = Variable("i")
    val art = tensorSchnittAnschlussArt(restForm?.size)
    return MathematischeMethode(
        name = "zerlegen_${achse + 1}",
        parameter = listOf(index),
        vorschrift = strukturSchnitt(tensor, index, StrukturZugriffsArt.Schnitt, achse, art, restForm),
        zielMenge = StrukturErgebnisMenge(art, restForm, "\\operatorname{Schnitte}_{${achse + 1}}(T)"),
        werteVorräte = mapOf(index.name to EndlicheIndexMenge(strukturAchsenLaenge(tensor, achse))),
    )
}

private fun tensorSchnittWert(
    tensor: MathematischesObjekt,
    achse: Int,
    index: Int,
    restForm: List<ZahlAusdruck>?,
): MathematischesObjekt = strukturSchnitt(
    tensor,
    RationaleZahl.von((index + 1).toLong()),
    StrukturZugriffsArt.Schnitt,
    achse,
    tensorSchnittAnschlussArt(restForm?.size),
    restForm,
)

private fun tensorSchnittAnschlussArt(restRang: Int?): String = when (restRang) {
    0 -> "mathematik.zahl"
    2 -> "mathematik.matrix"
    else -> "mathematik.tensor"
}

private fun ZahlAusdruck.tensorDimensionOderNull(): Int? = (this as? RationaleZahl)?.let { zahl ->
    if (zahl.nenner == BigInteger.ONE && zahl.zähler.signum() > 0 && zahl.zähler.bitLength() < 31) zahl.zähler.toInt() else null
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

private fun ganzzahlOderNull(objekt: AtlasWert): Int? = (objekt as? RationaleZahl)
    ?.takeIf { it.nenner == BigInteger.ONE && it.zähler.bitLength() < 31 }
    ?.zähler
    ?.toInt()

package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.io.File
import kotlin.test.*

/** Reproduzierbare Inventur, Export optional über MATLAS_DEFINITIONSAUDIT. */
class DefinitionsInventarTest {
    @Test fun `Inventar umfasst kanonische Typen und jede registrierte Operatorsignatur`() {
        val kanonisch = KanonischerMathematikKnotenKatalog.alle()
        val typen = kanonisch.map { it.art }.toSet()
        assertTrue(typen.isNotEmpty())
        val zeilen = mutableListOf<List<String>>()
        fun add(quelle: String, k: KnotenDaten, variante: String) {
            val dynamisch = dynamischesKonzeptFürKnoten(k)?.reiter?.filter { it.rolle == KonzeptReiterRolle.Definition }
            val status = when {
                dynamisch.isNullOrEmpty() -> "statisch prüfen"
                dynamisch.any { r -> r.karte.knoten.any { it.art == KonzeptKnotenArten.REGEL } } -> "dynamische Regelkarte"
                dynamisch.all { r -> r.karte.knoten.isNotEmpty() } -> "dynamischer Graph"
                else -> "dynamisch ohne Graph"
            }
            zeilen += listOf(quelle, k.art, variante, k.name, status)
        }
        kanonisch.forEach { add("kanonisch", it.erzeuge(GraphPunkt.Zero), it.standardParameter.entries.sortedBy { it.key }.joinToString(";") { "${it.key}=${it.value}" }) }
        alleMathematikDefinitionsVorlagen().forEach { add("Definitionsvorlage", it.erzeuge(GraphPunkt.Zero), it.standardParameter.entries.sortedBy { it.key }.joinToString(";") { "${it.key}=${it.value}" }) }
        RechnerOperatorRegistry.alle.forEach { op -> op.signaturen.forEach { sig ->
            val k = KnotenDaten(art = op.familie.knotenArt, name = op.titel, parameter = mapOf("operator" to op.operatorId) + if (sig.id == "grad") mapOf(SINUS_WINKELRAUM to "grad") else emptyMap())
            add("Operatorsignatur", k, "${op.operatorId};${sig.id}")
        } }
        MengenRechnerOperator.entries.forEach { add("Mengenoperator", KnotenDaten(art = MengenRechner.KNOTEN_ART, name = it.name, parameter = mapOf("operator" to it.stabileId)), it.stabileId) }
        AussagenSatzOperator.entries.forEach { add("Aussagenoperator", KnotenDaten(art = "mathematik.aussagensatz", name = it.name, parameter = mapOf("operator" to it.stabileId)), it.stabileId) }
        MengenRelationsOperator.entries.forEach { add("Mengenrelation", KnotenDaten(art = MengenRelationRechner.KNOTEN_ART, name = it.name, parameter = mapOf("operator" to it.stabileId)), it.stabileId) }
        MengenKnotenAuswahl.entries.forEach { add("Mengenquelle", MengenKnotenVorlagen.vorlage(it).erzeuge(GraphPunkt.Zero), it.stabileId) }
        ÜBERSETZER_ZIELE.forEach { add("Übersetzerziel", konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero), it), it) }
        add("Sinusvariante", konfiguriereSinusWinkelraum(KnotenDaten(art = ZAHLENRECHNER_ART, name = "Sinus Grad"), true), "zahl.sin;grad")
        assertEquals(typen, zeilen.filter { it[0] == "kanonisch" }.map { it[1] }.toSet())
        assertEquals(RechnerOperatorRegistry.alle.sumOf { it.signaturen.size }, zeilen.count { it[0] == "Operatorsignatur" })
        System.getenv("MATLAS_DEFINITIONSAUDIT")?.let { pfad ->
            File(pfad).writeText(zeilen.sortedWith(compareBy({ it[0] }, { it[1] }, { it[2] })).joinToString("\n") { zeile -> zeile.joinToString("\t") { it.replace('\t', ' ').replace('\n', ' ') } } + "\n")
        }
    }
}

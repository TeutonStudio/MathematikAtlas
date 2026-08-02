package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.*

data class VisualisierungsPunkt(
    val x: Double,
    val y: Double,
    val z: Double? = null,
    val farbwert: Double? = null,
)

data class VisualisierungsIntervall(
    val von: Double,
    val bis: Double,
    val linksGeschlossen: Boolean,
    val rechtsGeschlossen: Boolean,
    val linksAmFensterrand: Boolean = false,
    val rechtsAmFensterrand: Boolean = false,
)

enum class VisualisierungsQualität {
    Exakt,
    Approximation,
    Teilweise,
    MathematischLeer,
    KeineTrefferImFenster,
}

/** Ergebnis einer ausdrücklich klassifizierten Mengenvisualisierung. */
sealed interface VisualisierungsErgebnis {
    data class Erfolgreich(
        val punkte: List<VisualisierungsPunkt>,
        val istApproximation: Boolean = true,
        val hinweise: List<String> = emptyList(),
        val qualität: VisualisierungsQualität = if (istApproximation) VisualisierungsQualität.Approximation else VisualisierungsQualität.Exakt,
        val intervalle: List<VisualisierungsIntervall> = emptyList(),
    ) : VisualisierungsErgebnis

    data class Teilweise(
        val punkte: List<VisualisierungsPunkt>,
        val hinweise: List<String>,
        val qualität: VisualisierungsQualität = VisualisierungsQualität.Teilweise,
        val intervalle: List<VisualisierungsIntervall> = emptyList(),
    ) : VisualisierungsErgebnis

    data class NichtDarstellbar(val grund: String) : VisualisierungsErgebnis
}

/** Semantische Zwischenschicht zwischen Mengenform und Materialisierung. */
sealed interface VisualisierungsDefinition {
    data class ExaktePunkte(
        val dimension: Int,
        val punkte: List<List<Double>>,
        val verworfeneElemente: Map<String, Int> = emptyMap(),
    ) : VisualisierungsDefinition

    data class Region(
        val dimension: Int,
        val mitgliedschaft: (List<Double>) -> NumerischeMitgliedschaft,
        val hinweise: List<String> = emptyList(),
    ) : VisualisierungsDefinition

    data class ProduktDomänen(
        val faktoren: List<NumerischeDomäne>,
    ) : VisualisierungsDefinition

    data class Zahlengerade(
        val punkte: List<Double>,
        val intervalle: List<VisualisierungsIntervall>,
        val hinweise: List<String> = emptyList(),
        val mathematischLeer: Boolean = false,
    ) : VisualisierungsDefinition

    data class NichtRäumlich(val grund: String) : VisualisierungsDefinition
}

data class NumerischeDomäne(
    val werte: List<Double>,
    val istApproximation: Boolean,
    val hinweise: List<String> = emptyList(),
)

sealed interface NumerischeMitgliedschaft {
    data object Enthalten : NumerischeMitgliedschaft
    data object NichtEnthalten : NumerischeMitgliedschaft
    data class Grenze(val residuum: Double) : NumerischeMitgliedschaft
    data class Unbekannt(val grund: String) : NumerischeMitgliedschaft
}

/**
 * Plattformneutrales Sampling. Mengen werden zunächst semantisch normalisiert;
 * erst anschließend werden Raster oder kartesische Punktlisten materialisiert.
 */
object VisualisierungsSampler {
    fun normalisiere(
        menge: MengenAusdruck,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsDefinition {
        val dimension = konfiguration.raumDimension
        if (konfiguration.dimension == RaumDimension.R1) {
            return ZahlengeradenNormalisierer.normalisiere(menge, konfiguration)
        }
        if (konfiguration.dimension == RaumDimension.R3 && (konfiguration.achsen.z.isNullOrBlank() || konfiguration.bereiche.z == null)) {
            return VisualisierungsDefinition.NichtRäumlich("Für R³ fehlen eine Z-Achse oder ein Z-Achsenbereich.")
        }
        return when (menge) {
            LeereMenge -> VisualisierungsDefinition.ExaktePunkte(dimension, emptyList())
            is EndlicheMenge -> normalisiereEndlicheMenge(menge, dimension)
            is KartesischesProdukt -> normalisiereProdukt(menge, konfiguration)
            is DefinierteMenge -> normalisiereDefinierteMenge(menge, konfiguration)
            is Vereinigung, is Schnitt, is MengenDifferenz, is SymmetrischeDifferenz,
            is GefilterteMenge, is PrädikatsMenge, is MengenFallAusdruck ->
                normalisiereAllgemeineRegion(menge, konfiguration)
            else -> VisualisierungsDefinition.NichtRäumlich(
                "Die Mengenform ${menge::class.simpleName} besitzt im gewählten Raum keine unterstützte numerische Normalisierung.",
            )
        }
    }

    fun sample(
        menge: MengenAusdruck,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsErgebnis {
        if (menge is Abbild) return sampleAbbild(menge, konfiguration)
        return materialisiere(normalisiere(menge, konfiguration), konfiguration)
    }

    private fun materialisiere(
        definition: VisualisierungsDefinition,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsErgebnis = when (definition) {
        is VisualisierungsDefinition.NichtRäumlich -> VisualisierungsErgebnis.NichtDarstellbar(definition.grund)
        is VisualisierungsDefinition.ExaktePunkte -> materialisiereExaktePunkte(definition, konfiguration)
        is VisualisierungsDefinition.ProduktDomänen -> materialisiereProdukt(definition, konfiguration)
        is VisualisierungsDefinition.Zahlengerade -> materialisiereZahlengerade(definition, konfiguration)
        is VisualisierungsDefinition.Region -> sampleRegion(definition, konfiguration)
    }

    private fun materialisiereZahlengerade(
        definition: VisualisierungsDefinition.Zahlengerade,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsErgebnis {
        if (definition.mathematischLeer) {
            return VisualisierungsErgebnis.Erfolgreich(
                punkte = emptyList(),
                istApproximation = false,
                hinweise = definition.hinweise.ifEmpty { listOf("Die Menge ist mathematisch leer.") },
                qualität = VisualisierungsQualität.MathematischLeer,
            )
        }
        if (definition.punkte.isEmpty() && definition.intervalle.isEmpty()) {
            return VisualisierungsErgebnis.Erfolgreich(
                punkte = emptyList(),
                istApproximation = false,
                hinweise = definition.hinweise.ifEmpty { listOf("Im sichtbaren Zahlenbereich liegen keine Mengenelemente.") },
                qualität = VisualisierungsQualität.KeineTrefferImFenster,
            )
        }
        return VisualisierungsErgebnis.Erfolgreich(
            punkte = definition.punkte.map { VisualisierungsPunkt(it, 0.0) },
            istApproximation = false,
            hinweise = definition.hinweise,
            qualität = VisualisierungsQualität.Exakt,
            intervalle = definition.intervalle,
        )
    }

    private fun normalisiereEndlicheMenge(
        menge: EndlicheMenge,
        dimension: Int,
    ): VisualisierungsDefinition.ExaktePunkte {
        val punkte = mutableListOf<List<Double>>()
        val verworfen = linkedMapOf<String, Int>()
        menge.elemente.forEach { element ->
            when (val adapter = koordinaten(element, dimension, emptyMap())) {
                is KoordinatenErgebnis.Erfolgreich -> punkte += adapter.werte
                is KoordinatenErgebnis.Fehler -> verworfen[adapter.grund] = verworfen.getOrDefault(adapter.grund, 0) + 1
            }
        }
        return VisualisierungsDefinition.ExaktePunkte(dimension, punkte.distinct(), verworfen)
    }

    private fun normalisiereProdukt(
        produkt: KartesischesProdukt,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsDefinition {
        val dimension = konfiguration.raumDimension
        if (produkt.mengen.size != dimension) {
            return VisualisierungsDefinition.NichtRäumlich(
                "Das kartesische Produkt besitzt ${produkt.mengen.size} Faktoren, für ${konfiguration.dimension} werden genau $dimension benötigt. Eine Projektion ist nicht konfiguriert.",
            )
        }
        val bereiche = konfiguration.achsenBereiche
        val domänen = produkt.mengen.mapIndexed { index, faktor ->
            when (val ergebnis = faktorDomäne(faktor, bereiche[index], konfiguration)) {
                is DomänenErgebnis.Erfolgreich -> ergebnis.domäne
                is DomänenErgebnis.Fehler -> return VisualisierungsDefinition.NichtRäumlich(
                    "Faktor ${index + 1} ist nicht darstellbar: ${ergebnis.grund}",
                )
            }
        }
        return VisualisierungsDefinition.ProduktDomänen(domänen)
    }

    private fun normalisiereDefinierteMenge(
        menge: DefinierteMenge,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsDefinition {
        val dimension = konfiguration.raumDimension
        val variablen = menge.variablen.map { it.variable.name }
        val achsen = konfiguration.achsenNamen
        if (variablen.size != dimension) {
            return VisualisierungsDefinition.NichtRäumlich(
                "Die definierte Menge bindet ${variablen.size} Variablen, ${konfiguration.dimension} benötigt genau $dimension. Das Weglassen einer Koordinate ist keine Projektion.",
            )
        }
        if (achsen.size != dimension || achsen.any(String::isBlank)) {
            return VisualisierungsDefinition.NichtRäumlich("Für ${konfiguration.dimension} fehlt mindestens eine Achsenzuordnung.")
        }
        if (achsen.distinct().size != achsen.size) {
            return VisualisierungsDefinition.NichtRäumlich("Die Achsenzuordnung enthält doppelte Variablennamen.")
        }
        val fehlend = variablen - achsen.toSet()
        val unbekannt = achsen - variablen.toSet()
        if (fehlend.isNotEmpty() || unbekannt.isNotEmpty()) {
            return VisualisierungsDefinition.NichtRäumlich(
                buildString {
                    if (fehlend.isNotEmpty()) append("Nicht zugeordnete Mengenvariable: ${fehlend.joinToString()}. ")
                    if (unbekannt.isNotEmpty()) append("Unbekannte Achsenvariable: ${unbekannt.joinToString()}.")
                }.trim(),
            )
        }
        val zusätzliche = menge.bedingung.freieVariablen().map { it.name }.toSet() - variablen.toSet()
        if (zusätzliche.isNotEmpty()) {
            return VisualisierungsDefinition.NichtRäumlich(
                "Die Mengenbedingung enthält zusätzliche freie Variablen: ${zusätzliche.sorted().joinToString()}.",
            )
        }
        return VisualisierungsDefinition.Region(dimension, mitgliedschaft = mitgliedschaft@{ punkt ->
            val umgebung = achsen.zip(punkt).toMap()
            for (gebunden in menge.variablen) {
                when (val grund = werteAussage(
                    ElementBeziehung(gebunden.variable, gebunden.grundMenge),
                    umgebung,
                    konfiguration.sampling.toleranz,
                )) {
                    NumerischeMitgliedschaft.Enthalten -> Unit
                    NumerischeMitgliedschaft.NichtEnthalten -> return@mitgliedschaft NumerischeMitgliedschaft.NichtEnthalten
                    is NumerischeMitgliedschaft.Grenze -> Unit
                    is NumerischeMitgliedschaft.Unbekannt -> return@mitgliedschaft NumerischeMitgliedschaft.Unbekannt(
                        "Grundmenge von ${gebunden.variable.name}: ${grund.grund}",
                    )
                }
            }
            werteAussage(menge.bedingung, umgebung, konfiguration.sampling.toleranz)
        })
    }

    private fun normalisiereAllgemeineRegion(
        menge: MengenAusdruck,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsDefinition {
        val exakte = exaktePunkteMitSemantik(menge, konfiguration)
        exakte?.let { return VisualisierungsDefinition.ExaktePunkte(konfiguration.raumDimension, it) }
        val hinweise = if (menge is PrädikatsMenge && konfiguration.sampling.fensterBegrenztePrädikatsMengen) {
            listOf("Fensterbegrenzte Approximation einer grundmengenfreien Prädikatsmenge.")
        } else emptyList()
        if (menge is PrädikatsMenge && !konfiguration.sampling.fensterBegrenztePrädikatsMengen) {
            return VisualisierungsDefinition.NichtRäumlich(
                "Die Prädikatsmenge besitzt keine sichere Obermenge. Aktiviere ausdrücklich die fensterbegrenzte Approximation.",
            )
        }
        return VisualisierungsDefinition.Region(
            dimension = konfiguration.raumDimension,
            hinweise = hinweise,
            mitgliedschaft = { punkt -> mitgliedschaft(menge, punkt, konfiguration) },
        )
    }

    private fun materialisiereExaktePunkte(
        definition: VisualisierungsDefinition.ExaktePunkte,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsErgebnis {
        val punkte = definition.punkte.map { werte -> werte.alsPunkt(konfiguration) }
        if (definition.punkte.isEmpty() && definition.verworfeneElemente.isEmpty()) {
            return VisualisierungsErgebnis.Erfolgreich(
                punkte = emptyList(),
                istApproximation = false,
                hinweise = listOf("Die Menge ist mathematisch leer."),
                qualität = VisualisierungsQualität.MathematischLeer,
            )
        }
        if (definition.verworfeneElemente.isNotEmpty()) {
            val hinweise = definition.verworfeneElemente.map { (grund, anzahl) -> "$anzahl × $grund" }
            return if (punkte.isEmpty()) {
                VisualisierungsErgebnis.NichtDarstellbar(
                    "Die endliche Menge enthält keine darstellbaren ${definition.dimension}-dimensionalen Koordinaten. ${hinweise.joinToString(" ")}",
                )
            } else VisualisierungsErgebnis.Teilweise(punkte, hinweise)
        }
        return VisualisierungsErgebnis.Erfolgreich(punkte, istApproximation = false)
    }

    private fun materialisiereProdukt(
        definition: VisualisierungsDefinition.ProduktDomänen,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsErgebnis {
        val größe = definition.faktoren.fold(1L) { akk, domäne ->
            if (domäne.werte.isEmpty()) return VisualisierungsErgebnis.Erfolgreich(
                emptyList(), false, listOf("Mindestens ein Produktfaktor ist leer."), VisualisierungsQualität.MathematischLeer,
            )
            akk * domäne.werte.size
        }
        if (größe > konfiguration.sampling.maximalesRasterBudget) {
            return VisualisierungsErgebnis.NichtDarstellbar(
                "Das Produkt würde $größe Punkte materialisieren und überschreitet das Rasterbudget von ${konfiguration.sampling.maximalesRasterBudget}.",
            )
        }
        var kombinationen = listOf(emptyList<Double>())
        definition.faktoren.forEach { domäne ->
            kombinationen = kombinationen.flatMap { präfix -> domäne.werte.map { präfix + it } }
        }
        val approximation = definition.faktoren.any { it.istApproximation }
        return VisualisierungsErgebnis.Erfolgreich(
            punkte = kombinationen.map { it.alsPunkt(konfiguration) },
            istApproximation = approximation,
            hinweise = definition.faktoren.flatMap { it.hinweise }.distinct(),
        )
    }

    private fun sampleRegion(
        region: VisualisierungsDefinition.Region,
        c: VisualisierungsKonfiguration,
    ): VisualisierungsErgebnis {
        val n = if (region.dimension == 2) c.sampling.auflösung2D else c.sampling.auflösung3D
        val rasterGröße = ganzzahlPotenz(n.toLong(), region.dimension)
        if (rasterGröße > c.sampling.maximalesRasterBudget) {
            return VisualisierungsErgebnis.NichtDarstellbar(
                "Das ${region.dimension}D-Raster benötigt $rasterGröße Prüfungen und überschreitet das Budget von ${c.sampling.maximalesRasterBudget}.",
            )
        }
        val bereiche = c.achsenBereiche
        val schritte = bereiche.map { (it.maximum - it.minimum) / (n - 1) }
        val punkte = mutableListOf<VisualisierungsPunkt>()
        var unbekannt: String? = null
        val indices = IntArray(region.dimension)
        fun besuche(tiefe: Int) {
            if (tiefe < region.dimension) {
                for (index in 0 until n) {
                    indices[tiefe] = index
                    besuche(tiefe + 1)
                }
                return
            }
            val koordinaten = indices.mapIndexed { index, rasterIndex ->
                lerp(bereiche[index], rasterIndex.toDouble() / (n - 1))
            }
            when (val wert = region.mitgliedschaft(koordinaten)) {
                NumerischeMitgliedschaft.Enthalten -> punkte += koordinaten.alsPunkt(c)
                NumerischeMitgliedschaft.NichtEnthalten -> Unit
                is NumerischeMitgliedschaft.Unbekannt -> if (unbekannt == null) unbekannt = wert.grund
                is NumerischeMitgliedschaft.Grenze -> {
                    val schwelle = if (region.dimension == 2) c.sampling.toleranz else
                        c.sampling.toleranz * bereiche.maxOf { it.maximum - it.minimum }
                    var schneiden = abs(wert.residuum) <= schwelle
                    if (!schneiden) {
                        for (achse in koordinaten.indices) {
                            val nachbar = koordinaten.toMutableList().also { it[achse] += schritte[achse] }
                            val nachbarWert = region.mitgliedschaft(nachbar) as? NumerischeMitgliedschaft.Grenze
                            if (nachbarWert != null && nachbarWert.residuum * wert.residuum <= 0.0) {
                                schneiden = true
                                break
                            }
                        }
                    }
                    if (schneiden) punkte += koordinaten.alsPunkt(c)
                }
            }
        }
        besuche(0)
        if (punkte.isEmpty() && unbekannt != null) {
            return VisualisierungsErgebnis.NichtDarstellbar("Die numerische Mitgliedschaft konnte nicht ausgewertet werden: $unbekannt")
        }
        if (punkte.isEmpty()) {
            return VisualisierungsErgebnis.Erfolgreich(
                emptyList(), true,
                region.hinweise + "Im gewählten Fenster wurden keine Treffer gefunden.",
                VisualisierungsQualität.KeineTrefferImFenster,
            )
        }
        return VisualisierungsErgebnis.Erfolgreich(
            punkte,
            istApproximation = true,
            hinweise = region.hinweise + if (region.dimension == 3) listOf("R³ wird als numerische Punktwolke angenähert.") else emptyList(),
        )
    }

    private fun sampleAbbild(
        abbild: Abbild,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsErgebnis {
        val parameter = abbild.methode.parameter.singleOrNull() as? Variable
            ?: return VisualisierungsErgebnis.NichtDarstellbar("Die darzustellende Abbildung benötigt genau einen numerischen Parameter.")
        val ausgabe = abbild.methode.vorschrift.takeIf { abbild.methode.ausgabeNamen.size == 1 }
            ?: return VisualisierungsErgebnis.NichtDarstellbar("Die darzustellende Abbildung benötigt genau eine Ausgabe.")
        val domäne = when (val ergebnis = faktorDomäne(abbild.menge, konfiguration.bereiche.x, konfiguration)) {
            is DomänenErgebnis.Erfolgreich -> ergebnis.domäne
            is DomänenErgebnis.Fehler -> return VisualisierungsErgebnis.NichtDarstellbar(ergebnis.grund)
        }
        if (domäne.werte.isEmpty()) {
            return VisualisierungsErgebnis.Erfolgreich(
                emptyList(), false, listOf("Die Definitionsmenge der Abbildung ist leer."), VisualisierungsQualität.MathematischLeer,
            )
        }
        val punkte = mutableListOf<VisualisierungsPunkt>()
        var übersprungen = 0
        domäne.werte.forEach { parameterWert ->
            val umgebung = mapOf(parameter.name to parameterWert)
            when (val koordinaten = koordinaten(ausgabe, konfiguration.raumDimension, umgebung)) {
                is KoordinatenErgebnis.Erfolgreich -> punkte += koordinaten.werte.alsPunkt(konfiguration, umgebung)
                is KoordinatenErgebnis.Fehler -> übersprungen++
            }
        }
        return when {
            punkte.isEmpty() -> VisualisierungsErgebnis.NichtDarstellbar(
                "Die Abbildung erzeugt keine numerischen ${konfiguration.raumDimension}-dimensionalen Tupel oder Vektoren.",
            )
            übersprungen > 0 -> VisualisierungsErgebnis.Teilweise(
                punkte,
                domäne.hinweise + "$übersprungen nicht numerisch auswertbare Bildpunkte wurden übersprungen.",
            )
            else -> VisualisierungsErgebnis.Erfolgreich(punkte, domäne.istApproximation, domäne.hinweise)
        }
    }

    private fun mitgliedschaft(
        menge: MengenAusdruck,
        punkt: List<Double>,
        c: VisualisierungsKonfiguration,
    ): NumerischeMitgliedschaft = when (menge) {
        LeereMenge -> NumerischeMitgliedschaft.NichtEnthalten
        is EndlicheMenge -> if (exaktePunkte(menge, punkt.size).orEmpty().any { gleichKoordinaten(it, punkt, c.sampling.toleranz) }) {
            NumerischeMitgliedschaft.Enthalten
        } else NumerischeMitgliedschaft.NichtEnthalten
        is KartesischesProdukt -> {
            if (menge.mengen.size != punkt.size) NumerischeMitgliedschaft.Unbekannt(
                "Produktdimension ${menge.mengen.size} passt nicht zur Raumdimension ${punkt.size}.",
            ) else {
                val ergebnisse = menge.mengen.mapIndexed { index, faktor -> faktorEnthält(faktor, punkt[index], c.sampling.toleranz) }
                kombiniereMitgliedschaften(ergebnisse, und = true)
            }
        }
        is DefinierteMenge -> {
            val definition = normalisiereDefinierteMenge(menge, c)
            if (definition is VisualisierungsDefinition.Region) definition.mitgliedschaft(punkt)
            else NumerischeMitgliedschaft.Unbekannt((definition as VisualisierungsDefinition.NichtRäumlich).grund)
        }
        is Vereinigung -> kombiniereMitgliedschaften(menge.mengen.map { mitgliedschaft(it, punkt, c) }, und = false)
        is Schnitt -> kombiniereMitgliedschaften(menge.mengen.map { mitgliedschaft(it, punkt, c) }, und = true)
        is MengenDifferenz -> differenz(mitgliedschaft(menge.links, punkt, c), mitgliedschaft(menge.rechts, punkt, c))
        is SymmetrischeDifferenz -> exklusivOder(mitgliedschaft(menge.links, punkt, c), mitgliedschaft(menge.rechts, punkt, c))
        is GefilterteMenge -> {
            val basis = mitgliedschaft(menge.menge, punkt, c)
            if (basis != NumerischeMitgliedschaft.Enthalten) basis else werteFilter(menge, punkt, c)
        }
        is PrädikatsMenge -> wertePrädikatsMenge(menge, punkt, c)
        is MengenFallAusdruck -> werteMengenFall(menge, punkt, c)
        else -> NumerischeMitgliedschaft.Unbekannt("${menge::class.simpleName} besitzt keine numerische Mitgliedschaftssemantik.")
    }

    private fun werteFilter(
        menge: GefilterteMenge,
        punkt: List<Double>,
        c: VisualisierungsKonfiguration,
    ): NumerischeMitgliedschaft = runCatching {
        val parameter = menge.methode.parameter.single()
        val aussage = menge.methode.vorschrift as Aussage
        val gebunden = ersetze(aussage, mapOf(parameter.name to punktObjekt(punkt)))
        werteAussage(gebunden, c.achsenNamen.zip(punkt).toMap(), c.sampling.toleranz)
    }.getOrElse { NumerischeMitgliedschaft.Unbekannt("Filtermethode: ${it.message ?: "nicht auswertbar"}") }

    private fun wertePrädikatsMenge(
        menge: PrädikatsMenge,
        punkt: List<Double>,
        c: VisualisierungsKonfiguration,
    ): NumerischeMitgliedschaft = runCatching {
        val gebunden = ersetze(menge.bedingung, mapOf(menge.element.name to punktObjekt(punkt)))
        werteAussage(gebunden, c.achsenNamen.zip(punkt).toMap(), c.sampling.toleranz)
    }.getOrElse { NumerischeMitgliedschaft.Unbekannt("Prädikatsmenge: ${it.message ?: "nicht auswertbar"}") }

    private fun werteMengenFall(
        menge: MengenFallAusdruck,
        punkt: List<Double>,
        c: VisualisierungsKonfiguration,
    ): NumerischeMitgliedschaft = when (menge.aussage.entscheide(RechenKontext()).wahrheitswert) {
        Wahrheitswert.Wahr -> mitgliedschaft(menge.wahr, punkt, c)
        Wahrheitswert.Lüge -> mitgliedschaft(menge.lüge, punkt, c)
        null -> when (werteAussage(menge.aussage, c.achsenNamen.zip(punkt).toMap(), c.sampling.toleranz)) {
            NumerischeMitgliedschaft.Enthalten -> mitgliedschaft(menge.wahr, punkt, c)
            NumerischeMitgliedschaft.NichtEnthalten -> mitgliedschaft(menge.lüge, punkt, c)
            is NumerischeMitgliedschaft.Grenze -> NumerischeMitgliedschaft.Unbekannt("Die Fallbedingung ist eine Gleichheitsgrenze ohne eindeutigen Wahrheitswert.")
            is NumerischeMitgliedschaft.Unbekannt -> NumerischeMitgliedschaft.Unbekannt("Die Fallbedingung ist nicht numerisch auswertbar.")
        }
    }

    private fun exaktePunkteMitSemantik(
        menge: MengenAusdruck,
        konfiguration: VisualisierungsKonfiguration,
    ): List<List<Double>>? = when (menge) {
        is GefilterteMenge -> exaktePunkteMitSemantik(menge.menge, konfiguration)?.filter { punkt ->
            werteFilter(menge, punkt, konfiguration) == NumerischeMitgliedschaft.Enthalten
        }
        is MengenFallAusdruck -> when (menge.aussage.entscheide(RechenKontext()).wahrheitswert) {
            Wahrheitswert.Wahr -> exaktePunkteMitSemantik(menge.wahr, konfiguration)
            Wahrheitswert.Lüge -> exaktePunkteMitSemantik(menge.lüge, konfiguration)
            null -> null
        }
        else -> exaktePunkte(menge, konfiguration.raumDimension)
    }

    private fun exaktePunkte(menge: MengenAusdruck, dimension: Int): List<List<Double>>? = when (menge) {
        LeereMenge -> emptyList()
        is EndlicheMenge -> normalisiereEndlicheMenge(menge, dimension).takeIf { it.verworfeneElemente.isEmpty() }?.punkte
        is KartesischesProdukt -> {
            if (menge.mengen.size != dimension) return null
            val faktoren = menge.mengen.map { faktor ->
                when (faktor) {
                    LeereMenge -> emptyList()
                    is EndlicheMenge -> faktor.elemente.map { element ->
                        numerischerWert(element as? ZahlAusdruck ?: return null, emptyMap()) ?: return null
                    }.distinct()
                    else -> return null
                }
            }
            var kombinationen = listOf(emptyList<Double>())
            faktoren.forEach { werte -> kombinationen = kombinationen.flatMap { präfix -> werte.map { präfix + it } } }
            kombinationen
        }
        is Vereinigung -> menge.mengen.map { exaktePunkte(it, dimension) ?: return null }.flatten().distinct()
        is Schnitt -> {
            val teile = menge.mengen.map { exaktePunkte(it, dimension) ?: return null }
            teile.firstOrNull()?.filter { punkt -> teile.drop(1).all { andere -> andere.any { gleichKoordinaten(it, punkt, 1e-9) } } }.orEmpty()
        }
        is MengenDifferenz -> {
            val links = exaktePunkte(menge.links, dimension) ?: return null
            val rechts = exaktePunkte(menge.rechts, dimension) ?: return null
            links.filterNot { punkt -> rechts.any { gleichKoordinaten(it, punkt, 1e-9) } }
        }
        is SymmetrischeDifferenz -> {
            val links = exaktePunkte(menge.links, dimension) ?: return null
            val rechts = exaktePunkte(menge.rechts, dimension) ?: return null
            links.filterNot { p -> rechts.any { gleichKoordinaten(it, p, 1e-9) } } +
                rechts.filterNot { p -> links.any { gleichKoordinaten(it, p, 1e-9) } }
        }
        else -> null
    }

    private sealed interface DomänenErgebnis {
        data class Erfolgreich(val domäne: NumerischeDomäne) : DomänenErgebnis
        data class Fehler(val grund: String) : DomänenErgebnis
    }

    private fun faktorDomäne(
        faktor: MengenAusdruck,
        bereich: ZahlenBereich,
        c: VisualisierungsKonfiguration,
    ): DomänenErgebnis = when (faktor) {
        LeereMenge -> DomänenErgebnis.Erfolgreich(NumerischeDomäne(emptyList(), false))
        is EndlicheMenge -> {
            val werte = faktor.elemente.map { element ->
                val zahl = element as? ZahlAusdruck ?: return DomänenErgebnis.Fehler("Die endliche Faktor-Menge enthält ein nichtskalares Element.")
                numerischerWert(zahl, emptyMap()) ?: return DomänenErgebnis.Fehler("Ein Faktorwert ist nicht numerisch auswertbar.")
            }.filter { it.isFinite() }.distinct().sorted()
            DomänenErgebnis.Erfolgreich(NumerischeDomäne(werte, false))
        }
        is ReellesIntervall -> {
            val links = numerischerWert(faktor.links, emptyMap())
                ?: return DomänenErgebnis.Fehler("Die linke Intervallgrenze ist nicht numerisch auswertbar.")
            val rechts = numerischerWert(faktor.rechts, emptyMap())
                ?: return DomänenErgebnis.Fehler("Die rechte Intervallgrenze ist nicht numerisch auswertbar.")
            val anzahl = c.achsenAuflösung
            val werte = List(anzahl) { index -> links + (rechts - links) * index.toDouble() / (anzahl - 1) }
                .filter { wert ->
                    (!faktor.linksOffen || wert > links + c.sampling.toleranz) &&
                        (!faktor.rechtsOffen || wert < rechts - c.sampling.toleranz)
                }
            DomänenErgebnis.Erfolgreich(
                NumerischeDomäne(werte, true, listOf("Ein kontinuierlicher Produktfaktor wird mit ${werte.size} Werten angenähert.")),
            )
        }
        ReelleZahlen -> DomänenErgebnis.Erfolgreich(
            NumerischeDomäne(rasterWerte(bereich, c.achsenAuflösung), true, listOf("ℝ wird auf den sichtbaren Achsenbereich begrenzt.")),
        )
        GanzeZahlen -> ganzzahlDomäne(bereich, natürliche = false, c)
        NatürlicheZahlen -> ganzzahlDomäne(bereich, natürliche = true, c)
        is Vereinigung, is Schnitt, is MengenDifferenz, is SymmetrischeDifferenz -> {
            val kandidaten = rasterWerte(bereich, c.achsenAuflösung)
            val werte = kandidaten.filter { faktorEnthält(faktor, it, c.sampling.toleranz) == NumerischeMitgliedschaft.Enthalten }
            DomänenErgebnis.Erfolgreich(
                NumerischeDomäne(werte, true, listOf("Die eindimensionale Mengenoperation wird auf einem gemeinsamen Achsenraster ausgewertet.")),
            )
        }
        else -> DomänenErgebnis.Fehler("${faktor::class.simpleName} ist keine unterstützte eindimensionale Faktor-Domäne.")
    }

    private fun ganzzahlDomäne(
        bereich: ZahlenBereich,
        natürliche: Boolean,
        c: VisualisierungsKonfiguration,
    ): DomänenErgebnis {
        val start = ceil(bereich.minimum).toLong().coerceAtLeast(if (natürliche) 0 else Long.MIN_VALUE)
        val ende = floor(bereich.maximum).toLong()
        if (ende < start) return DomänenErgebnis.Erfolgreich(NumerischeDomäne(emptyList(), false))
        val anzahl = ende - start + 1
        if (anzahl > c.sampling.maximalesRasterBudget) {
            return DomänenErgebnis.Fehler("Der sichtbare ganzzahlige Faktor enthält $anzahl Werte und überschreitet das Rasterbudget.")
        }
        return DomänenErgebnis.Erfolgreich(NumerischeDomäne((start..ende).map(Long::toDouble), false))
    }

    private fun faktorEnthält(
        faktor: MengenAusdruck,
        wert: Double,
        toleranz: Double,
    ): NumerischeMitgliedschaft = when (faktor) {
        LeereMenge -> NumerischeMitgliedschaft.NichtEnthalten
        ReelleZahlen -> NumerischeMitgliedschaft.Enthalten
        GanzeZahlen -> if (abs(wert - round(wert)) <= toleranz) NumerischeMitgliedschaft.Enthalten else NumerischeMitgliedschaft.NichtEnthalten
        NatürlicheZahlen -> if (wert >= -toleranz && abs(wert - round(wert)) <= toleranz) NumerischeMitgliedschaft.Enthalten else NumerischeMitgliedschaft.NichtEnthalten
        is ReellesIntervall -> {
            val links = numerischerWert(faktor.links, emptyMap()) ?: return NumerischeMitgliedschaft.Unbekannt("Intervallgrenze nicht numerisch.")
            val rechts = numerischerWert(faktor.rechts, emptyMap()) ?: return NumerischeMitgliedschaft.Unbekannt("Intervallgrenze nicht numerisch.")
            val linksOk = if (faktor.linksOffen) wert > links + toleranz else wert >= links - toleranz
            val rechtsOk = if (faktor.rechtsOffen) wert < rechts - toleranz else wert <= rechts + toleranz
            if (linksOk && rechtsOk) NumerischeMitgliedschaft.Enthalten else NumerischeMitgliedschaft.NichtEnthalten
        }
        is EndlicheMenge -> {
            val werte = faktor.elemente.mapNotNull { numerischerWert(it as? ZahlAusdruck ?: return@mapNotNull null, emptyMap()) }
            if (werte.size != faktor.elemente.size) NumerischeMitgliedschaft.Unbekannt("Die endliche Faktor-Menge enthält nichtnumerische Elemente.")
            else if (werte.any { abs(it - wert) <= toleranz }) NumerischeMitgliedschaft.Enthalten else NumerischeMitgliedschaft.NichtEnthalten
        }
        is Vereinigung -> kombiniereMitgliedschaften(faktor.mengen.map { faktorEnthält(it, wert, toleranz) }, und = false)
        is Schnitt -> kombiniereMitgliedschaften(faktor.mengen.map { faktorEnthält(it, wert, toleranz) }, und = true)
        is MengenDifferenz -> differenz(faktorEnthält(faktor.links, wert, toleranz), faktorEnthält(faktor.rechts, wert, toleranz))
        is SymmetrischeDifferenz -> exklusivOder(faktorEnthält(faktor.links, wert, toleranz), faktorEnthält(faktor.rechts, wert, toleranz))
        else -> NumerischeMitgliedschaft.Unbekannt("${faktor::class.simpleName} ist keine unterstützte Faktor-Menge.")
    }

    private fun werteAussage(
        aussage: Aussage,
        umgebung: Map<String, Double>,
        toleranz: Double,
    ): NumerischeMitgliedschaft {
        if (aussage is Gleichheit && aussage.links is ZahlAusdruck && aussage.rechts is ZahlAusdruck) {
            val links = numerischerWert(aussage.links as ZahlAusdruck, umgebung)
            val rechts = numerischerWert(aussage.rechts as ZahlAusdruck, umgebung)
            if (links != null && rechts != null) return NumerischeMitgliedschaft.Grenze(links - rechts)
        }
        return when (val ergebnis = NumerischerAuswerter.aussage(
            aussage,
            NumerischeUmgebung(umgebung),
            NumerischeOptionen(toleranz = toleranz),
        )) {
            is NumerischesErgebnis.Wert -> if (ergebnis.wert) NumerischeMitgliedschaft.Enthalten else NumerischeMitgliedschaft.NichtEnthalten
            is NumerischesErgebnis.Fehler -> NumerischeMitgliedschaft.Unbekannt(ergebnis.beschreibung)
        }
    }

    private fun numerischerWert(
        ausdruck: ZahlAusdruck,
        umgebung: Map<String, Double>,
    ): Double? = when (val ergebnis = NumerischerAuswerter.wert(ausdruck, NumerischeUmgebung(umgebung))) {
        is NumerischesErgebnis.Wert -> ergebnis.wert
        is NumerischesErgebnis.Fehler -> null
    }

    private sealed interface KoordinatenErgebnis {
        data class Erfolgreich(val werte: List<Double>) : KoordinatenErgebnis
        data class Fehler(val grund: String) : KoordinatenErgebnis
    }

    private fun koordinaten(
        objekt: MathematischesObjekt,
        dimension: Int,
        umgebung: Map<String, Double>,
    ): KoordinatenErgebnis {
        if (objekt is FallAusdruck) {
            return when (werteAussage(objekt.aussage, umgebung, 1e-9)) {
                NumerischeMitgliedschaft.Enthalten -> koordinaten(objekt.wahr, dimension, umgebung)
                NumerischeMitgliedschaft.NichtEnthalten -> koordinaten(objekt.lüge, dimension, umgebung)
                else -> KoordinatenErgebnis.Fehler("Fallbedingung nicht eindeutig auswertbar")
            }
        }
        val ausdrücke = when (objekt) {
            is ZahlAusdruck -> if (dimension == 1) listOf(objekt) else return KoordinatenErgebnis.Fehler("Ein skalares Element ist nur in R¹ eine Koordinate")
            is Tupel -> objekt.elemente.mapIndexed { index, element ->
                element as? ZahlAusdruck ?: return KoordinatenErgebnis.Fehler("Tupelkomponente ${index + 1} ist keine Zahl")
            }
            is SpaltenVektor -> objekt.werte
            is ZeilenVektor -> objekt.werte
            else -> return KoordinatenErgebnis.Fehler("Element ist weder Tupel noch Zeilen- oder Spaltenvektor")
        }
        if (ausdrücke.size != dimension) {
            return KoordinatenErgebnis.Fehler("Koordinatendimension ${ausdrücke.size} statt erwartet $dimension")
        }
        val werte = ausdrücke.mapIndexed { index, ausdruck ->
            numerischerWert(ausdruck, umgebung)
                ?: return KoordinatenErgebnis.Fehler("Koordinate ${index + 1} ist nicht numerisch auswertbar")
        }
        if (werte.any { !it.isFinite() }) return KoordinatenErgebnis.Fehler("Mindestens eine Koordinate ist nicht endlich")
        return KoordinatenErgebnis.Erfolgreich(werte)
    }

    private fun kombiniereMitgliedschaften(
        werte: List<NumerischeMitgliedschaft>,
        und: Boolean,
    ): NumerischeMitgliedschaft {
        if (und && werte.any { it == NumerischeMitgliedschaft.NichtEnthalten }) return NumerischeMitgliedschaft.NichtEnthalten
        if (!und && werte.any { it == NumerischeMitgliedschaft.Enthalten }) return NumerischeMitgliedschaft.Enthalten
        werte.filterIsInstance<NumerischeMitgliedschaft.Unbekannt>().firstOrNull()?.let { return it }
        werte.filterIsInstance<NumerischeMitgliedschaft.Grenze>().firstOrNull()?.let { return it }
        return if (und) NumerischeMitgliedschaft.Enthalten else NumerischeMitgliedschaft.NichtEnthalten
    }

    private fun differenz(
        links: NumerischeMitgliedschaft,
        rechts: NumerischeMitgliedschaft,
    ): NumerischeMitgliedschaft = when {
        links == NumerischeMitgliedschaft.NichtEnthalten -> NumerischeMitgliedschaft.NichtEnthalten
        rechts == NumerischeMitgliedschaft.Enthalten -> NumerischeMitgliedschaft.NichtEnthalten
        links == NumerischeMitgliedschaft.Enthalten && rechts == NumerischeMitgliedschaft.NichtEnthalten -> NumerischeMitgliedschaft.Enthalten
        links is NumerischeMitgliedschaft.Unbekannt -> links
        rechts is NumerischeMitgliedschaft.Unbekannt -> rechts
        else -> NumerischeMitgliedschaft.Unbekannt("Die Mengendifferenz ist an einer numerischen Grenze nicht eindeutig klassifiziert.")
    }

    private fun exklusivOder(
        links: NumerischeMitgliedschaft,
        rechts: NumerischeMitgliedschaft,
    ): NumerischeMitgliedschaft = when {
        links is NumerischeMitgliedschaft.Unbekannt -> links
        rechts is NumerischeMitgliedschaft.Unbekannt -> rechts
        links is NumerischeMitgliedschaft.Grenze || rechts is NumerischeMitgliedschaft.Grenze ->
            NumerischeMitgliedschaft.Unbekannt("Die symmetrische Differenz ist an einer Grenze nicht eindeutig klassifiziert.")
        (links == NumerischeMitgliedschaft.Enthalten) xor (rechts == NumerischeMitgliedschaft.Enthalten) -> NumerischeMitgliedschaft.Enthalten
        else -> NumerischeMitgliedschaft.NichtEnthalten
    }

    private fun List<Double>.alsPunkt(
        c: VisualisierungsKonfiguration,
        zusätzlicheUmgebung: Map<String, Double> = emptyMap(),
    ): VisualisierungsPunkt {
        val umgebung = zusätzlicheUmgebung + c.achsenNamen.zip(this).toMap()
        return VisualisierungsPunkt(
            x = this[0],
            y = getOrElse(1) { 0.0 },
            z = getOrNull(2),
            farbwert = if (c.farbe.modus == FarbModus.Spektrum) c.farbe.variable?.let(umgebung::get) else null,
        )
    }

    private val VisualisierungsKonfiguration.raumDimension: Int
        get() = when (dimension) {
            RaumDimension.R1 -> 1
            RaumDimension.R2 -> 2
            RaumDimension.R3 -> 3
        }

    private val VisualisierungsKonfiguration.achsenNamen: List<String>
        get() = when (dimension) {
            RaumDimension.R1 -> listOf(achsen.x)
            RaumDimension.R2 -> listOf(achsen.x, achsen.y)
            RaumDimension.R3 -> listOf(achsen.x, achsen.y, achsen.z.orEmpty())
        }

    private val VisualisierungsKonfiguration.achsenBereiche: List<ZahlenBereich>
        get() = when (dimension) {
            RaumDimension.R1 -> listOf(bereiche.x)
            RaumDimension.R2 -> listOf(bereiche.x, bereiche.y)
            RaumDimension.R3 -> listOfNotNull(bereiche.x, bereiche.y, bereiche.z)
        }

    private val VisualisierungsKonfiguration.achsenAuflösung: Int
        get() = when (dimension) {
            RaumDimension.R1 -> sampling.auflösung1D
            RaumDimension.R2 -> sampling.auflösung2D
            RaumDimension.R3 -> sampling.auflösung3D
        }

    private fun rasterWerte(bereich: ZahlenBereich, anzahl: Int): List<Double> =
        List(anzahl) { index -> lerp(bereich, index.toDouble() / (anzahl - 1)) }

    private fun lerp(bereich: ZahlenBereich, t: Double): Double =
        bereich.minimum + (bereich.maximum - bereich.minimum) * t

    private fun ganzzahlPotenz(basis: Long, exponent: Int): Long {
        var ergebnis = 1L
        repeat(exponent) {
            if (ergebnis > Long.MAX_VALUE / basis) return Long.MAX_VALUE
            ergebnis *= basis
        }
        return ergebnis
    }

    private fun gleichKoordinaten(a: List<Double>, b: List<Double>, toleranz: Double): Boolean =
        a.size == b.size && a.zip(b).all { (links, rechts) -> abs(links - rechts) <= toleranz }

    private fun punktObjekt(punkt: List<Double>): Tupel = Tupel(punkt.map(::rationaleZahl))

    private fun rationaleZahl(wert: Double): RationaleZahl {
        val dezimal = BigDecimal.valueOf(wert).stripTrailingZeros()
        val skala = dezimal.scale()
        return if (skala <= 0) {
            RationaleZahl.von(dezimal.unscaledValue() * BigInteger.TEN.pow(-skala))
        } else RationaleZahl.von(dezimal.unscaledValue(), BigInteger.TEN.pow(skala))
    }
}

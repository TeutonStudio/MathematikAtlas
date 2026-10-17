package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.KoordinatenErgebnis
import de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.ReelleKoordinatenKomponente
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.*

data class VisualisierungsPunkt(
    val x: Double,
    val y: Double,
    val z: Double? = null,
    val farbwert: Double? = null,
    val weitereFarbwerte: List<Double> = emptyList(),
    val rasterProbe: Boolean = false,
)

enum class DarstellungsNachweis {
    Exakt,
    Bewiesen,
    Numerisch,
    Gemischt,
    Unbekannt,
}

data class VisualisierungsLinie(
    val punkte: List<VisualisierungsPunkt>,
    val nachweis: DarstellungsNachweis = DarstellungsNachweis.Numerisch,
    val offenerRand: Boolean = false,
)

data class VisualisierungsDreieck(
    val a: VisualisierungsPunkt,
    val b: VisualisierungsPunkt,
    val c: VisualisierungsPunkt,
    val nachweis: DarstellungsNachweis = DarstellungsNachweis.Numerisch,
)

data class VisualisierungsStatistik(
    val auswertungen: Int = 0,
    val stützpunkte: Int = 0,
    val linien: Int = 0,
    val dreiecke: Int = 0,
    val zellen: Int = 0,
    val maximaleTiefe: Int = 0,
    val budgetErschöpft: Boolean = false,
    val zellArbeit: Int = 0,
    val punktArbeit: Int = 0,
    val randArbeit: Int = 0,
)

enum class ZellenStatus { Enthalten, NumerischEnthalten, Ausgeschlossen, Gemischt, Unbekannt }

data class VisualisierungsZelle(
    val minimum: List<Double>,
    val maximum: List<Double>,
    val status: ZellenStatus,
    val grund: String? = null,
)

sealed interface ZellNachweis {
    data object Enthalten : ZellNachweis
    data object Ausgeschlossen : ZellNachweis
    data class Gemischt(val grund: String) : ZellNachweis
    data class Unbekannt(val grund: String) : ZellNachweis
}

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
        val zellen: List<VisualisierungsZelle> = emptyList(),
        val linien: List<VisualisierungsLinie> = emptyList(),
        val dreiecke: List<VisualisierungsDreieck> = emptyList(),
        val statistik: VisualisierungsStatistik = VisualisierungsStatistik(),
        val interaktionsDreiecke: List<VisualisierungsDreieck> = emptyList(),
    ) : VisualisierungsErgebnis

    data class Teilweise(
        val punkte: List<VisualisierungsPunkt>,
        val hinweise: List<String>,
        val qualität: VisualisierungsQualität = VisualisierungsQualität.Teilweise,
        val intervalle: List<VisualisierungsIntervall> = emptyList(),
        val zellen: List<VisualisierungsZelle> = emptyList(),
        val linien: List<VisualisierungsLinie> = emptyList(),
        val dreiecke: List<VisualisierungsDreieck> = emptyList(),
        val statistik: VisualisierungsStatistik = VisualisierungsStatistik(),
        val interaktionsDreiecke: List<VisualisierungsDreieck> = emptyList(),
    ) : VisualisierungsErgebnis

    data class BedingtDarstellbar(
        val grund: String,
        val bedingungen: List<String> = emptyList(),
    ) : VisualisierungsErgebnis

    data class ProjektionErforderlich(
        val vorhandeneDimension: Int,
        val erwarteteDimension: Int,
        val grund: String,
    ) : VisualisierungsErgebnis

    data class NichtDarstellbar(val grund: String) : VisualisierungsErgebnis
}

/** Semantische Zwischenschicht zwischen Mengenform und Materialisierung. */
sealed interface VisualisierungsDefinition {
    data class ExaktePunkte(
        val dimension: Int,
        val punkte: List<List<Double>>,
        val diagnosen: List<KoordinatenErgebnis> = emptyList(),
    ) : VisualisierungsDefinition

    data class Region(
        val dimension: Int,
        val mitgliedschaft: (List<Double>) -> NumerischeMitgliedschaft,
        val hinweise: List<String> = emptyList(),
        val fensterBegrenzt: Boolean = false,
        val zellNachweis: ((List<RationalesIntervall>) -> ZellNachweis)? = null,
        val farbMitgliedschaft: ((List<Double>) -> List<Pair<Double, NumerischeMitgliedschaft>>)? = null,
        val randResiduum: ((List<Double>) -> Double?)? = null,
        val offenerRand: Boolean = false,
        /** Gemeinsames Zertifikatsbudget zählt Zellbesuche und AST-Arbeit. Nur Laufzeit. */
        val zellArbeitsBudget: AuswertungsBudget? = null,
        val farbProbenAnzahl: Int = 1,
        val numerischeFläche: Boolean = false,
        val samplingDomänen: List<NumerischeDomäne>? = null,
    ) : VisualisierungsDefinition

    data class ProduktDomänen(
        val faktoren: List<NumerischeDomäne>,
        val mitgliedschaft: ((List<Double>) -> NumerischeMitgliedschaft)? = null,
    ) : VisualisierungsDefinition

    data class Zahlengerade(
        val punkte: List<Double>,
        val intervalle: List<VisualisierungsIntervall>,
        val hinweise: List<String> = emptyList(),
        val mathematischLeer: Boolean = false,
    ) : VisualisierungsDefinition

    data class NichtRäumlich(val grund: String) : VisualisierungsDefinition

    data class BedingtRäumlich(
        val grund: String,
        val bedingungen: List<String> = emptyList(),
    ) : VisualisierungsDefinition

    data class ProjektionErforderlich(
        val vorhandeneDimension: Int,
        val erwarteteDimension: Int,
        val grund: String,
    ) : VisualisierungsDefinition
}

data class NumerischeDomäne(
    val werte: List<Double>,
    val istApproximation: Boolean,
    val hinweise: List<String> = emptyList(),
    val mathematischLeer: Boolean = false,
)

private class GemeinsamesOrbitBudgetErschöpft : RuntimeException()

private enum class OrbitParameterArt { Reell, Komplex, Tupel }

private data class OrbitParameterGeometrie(
    val art: OrbitParameterArt,
    val dimension: Int,
)

private data class EndlicheAlgebraSchätzung(
    val kardinalität: BigInteger,
    val arbeit: BigInteger,
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
        abbruchPrüfen: () -> Unit = {},
    ): VisualisierungsDefinition {
        val dimension = konfiguration.raumDimension
        if (konfiguration.dimension == RaumDimension.R3 && (konfiguration.achsen.z.isNullOrBlank() || konfiguration.bereiche.z == null))
            return VisualisierungsDefinition.NichtRäumlich("Für R³ fehlen eine Z-Achse oder ein Z-Achsenbereich.")
        raumInterpretationsHinweis(menge)?.let { grund ->
            return if (menge is KartesischerPotenzraum || menge is Tensorraum)
                VisualisierungsDefinition.BedingtRäumlich(grund) else VisualisierungsDefinition.NichtRäumlich(grund)
        }
        if (menge is GefilterteMenge) {
            raumInterpretationsHinweis(menge.menge)?.let {
                return if (menge.menge is KartesischerPotenzraum || menge.menge is Tensorraum)
                    VisualisierungsDefinition.BedingtRäumlich(it) else VisualisierungsDefinition.NichtRäumlich(it)
            }
            val n = if (menge.menge is EndlicheMenge) null else numerischeArgumentStruktur(menge.menge)?.blätter?.size
            if (n == 0) return VisualisierungsDefinition.ProjektionErforderlich(0, dimension,
                "Der nulldimensionale Raum enthält (); er besitzt keine räumlichen Achsen.")
            val projektion = konfiguration.ausgabeKoordinaten
            if (n != null && projektion.isNotEmpty() && (projektion.size != dimension || projektion.distinct().size != projektion.size || projektion.any { it !in 1..n }))
                return VisualisierungsDefinition.NichtRäumlich("Die Filterprojektion benötigt verschiedene vorhandene Koordinaten passend zum Darstellungsraum.")
            if (n != null && n != dimension) {
                val auswahl = konfiguration.ausgabeKoordinaten
                if (auswahl.isEmpty()) return VisualisierungsDefinition.ProjektionErforderlich(n, dimension,
                    "Die Filtergrundmenge benötigt eine ausdrückliche Projektion und feste Schnitte für ausgelassene Koordinaten.")
                if (auswahl.size != dimension || auswahl.distinct().size != auswahl.size || auswahl.any { it !in 1..n })
                    return VisualisierungsDefinition.NichtRäumlich("Die Filterprojektion benötigt verschiedene vorhandene Koordinaten passend zum Darstellungsraum.")
                val fehlende = (1..n).filterNot { it in auswahl || "k$it" in konfiguration.festeSchnitte }
                if (fehlende.isNotEmpty()) return VisualisierungsDefinition.BedingtRäumlich(
                    "Für ausgelassene Filterkoordinaten ${fehlende.joinToString { "k$it" }} fehlen feste Schnitte. Eine allgemeine existenzielle Projektion erfordert eine ausdrückliche Bildmengenabbildung.")
            }
        }
        if (!konfiguration.sampling.automatisch && menge is EndlicheMenge && menge.elemente.size > konfiguration.sampling.maximalesRasterBudget) {
            return VisualisierungsDefinition.NichtRäumlich(
                "Die endliche Menge enthält ${menge.elemente.size} Elemente und überschreitet das Rasterbudget von ${konfiguration.sampling.maximalesRasterBudget}.",
            )
        }
        val endlicheSchätzung = schätzeEndlicheAlgebra(menge)
        val budget = BigInteger.valueOf(konfiguration.sampling.maximalesRasterBudget.toLong())
        if (endlicheSchätzung != null && !(konfiguration.sampling.automatisch && menge is EndlicheMenge) &&
            (endlicheSchätzung.kardinalität > budget || endlicheSchätzung.arbeit > budget)
        ) {
            return VisualisierungsDefinition.NichtRäumlich(
                "Die endliche Mengenalgebra benötigt bis zu ${endlicheSchätzung.arbeit.max(endlicheSchätzung.kardinalität)} " +
                    "Auswertungsschritte und überschreitet das Rasterbudget von ${konfiguration.sampling.maximalesRasterBudget}.",
            )
        }
        if (konfiguration.ausgabeKoordinaten.isNotEmpty() && endlicheSchätzung != null) {
            when (val endlich = normalisiereEndlicheAlgebra(menge, konfiguration.sampling.maximalesRasterBudget)) {
                is EndlicheMenge -> return normalisiereEndlicheMenge(endlich, dimension, konfiguration.ausgabeKoordinaten, konfiguration.dimension == RaumDimension.C)
                LeereMenge -> return VisualisierungsDefinition.ExaktePunkte(dimension, emptyList())
                else -> Unit
            }
        }
        if (menge is GefilterteMenge && endlicheSchätzung == null) {
            normalisiereFilter(menge, konfiguration, abbruchPrüfen)?.let { return it }
        }
        if (menge is KoordinatenBild) return normalisiereKoordinatenBild(menge, dimension, konfiguration.ausgabeKoordinaten)
        if (menge is OrbitBeschraenktheitsMenge) {
            return normalisiereOrbitMenge(menge, konfiguration, abbruchPrüfen)
        }
        if (menge is Tupelraum || menge is KartesischerPotenzraum || menge is Vektorraum ||
            menge is Matrizenraum || menge is Tensorraum || menge is KartesischesProdukt ||
            menge == GaußscheGanzeZahlen || menge == GaußschePrimzahlen ||
            menge == NichtnegativeGanzeZahlenSemantik.menge || menge == Primzahlen) {
            val ursprünglicherRaum = numerischerRaum(menge)
            if (ursprünglicherRaum == null && menge is KartesischesProdukt && menge.mengen.all { produktFaktorDimension(it) != null })
                return normalisiereProdukt(menge, konfiguration, abbruchPrüfen)
            val raum = ursprünglicherRaum ?: numerischeArgumentStruktur(menge)
                ?: return VisualisierungsDefinition.NichtRäumlich("Die numerische Raumstruktur ist unbekannt oder überschreitet das Koordinatenbudget.")
            val n = raum.blätter.size
            if (n == 0) return VisualisierungsDefinition.ProjektionErforderlich(0, dimension,
                "Der nulldimensionale Raum enthält das leere Tupel (); er ist keine leere Menge und hat keine räumlichen Achsen.")
            val auswahl = konfiguration.ausgabeKoordinaten
            if (auswahl.isNotEmpty()) {
                if (auswahl.size != dimension || auswahl.distinct().size != auswahl.size || auswahl.any { it !in 1..n })
                    return VisualisierungsDefinition.NichtRäumlich("Die Projektion benötigt verschiedene vorhandene Koordinaten passend zum Darstellungsraum.")
                // Independent component spaces project onto their selected component factors.
                if (ursprünglicherRaum == null || raum.hatZusatzBedingung()) return VisualisierungsDefinition.NichtRäumlich("Projiziere diesen eingeschränkten Raum über eine ausdrückliche Bildmengenabbildung.")
                return normalisiereProdukt(KartesischesProdukt(auswahl.map { raum.blätter[it - 1] }), konfiguration, abbruchPrüfen)
            }
            if (n < dimension) return VisualisierungsDefinition.NichtRäumlich("Der Raum besitzt nur $n reelle Blattkoordinaten statt $dimension.")
            if (n > dimension) return VisualisierungsDefinition.ProjektionErforderlich(n, dimension,
                "Der Raum besitzt $n reelle Blattkoordinaten. Wähle eine ausdrückliche Projektion passend zum Darstellungsraum.")
            if (ursprünglicherRaum == null || raum.hatZusatzBedingung()) {
                val p = AllgemeinerParameter("punkt")
                return normalisiereFilter(GefilterteMenge(menge, MathematischeMethode("P", listOf(p), WahrheitsKonstante(true), WahrheitsMenge,
                    werteVorräte = mapOf(p.name to menge))), konfiguration, abbruchPrüfen)
                    ?: VisualisierungsDefinition.NichtRäumlich("Gaußsche Primzahlen konnten nicht normalisiert werden.")
            }
            if (menge is KartesischesProdukt && menge.mengen.any { it == KomplexeZahlen })
                return normalisiereProdukt(menge, konfiguration, abbruchPrüfen)
            return normalisiereProdukt(KartesischesProdukt(raum.blätter), konfiguration, abbruchPrüfen)
        }
        if (konfiguration.dimension == RaumDimension.R1 && menge is DefinierteMenge) {
            return normalisiereDefinierteMenge(menge, konfiguration, abbruchPrüfen)
        }
        if (konfiguration.dimension == RaumDimension.R1 && menge == RationaleZahlen) {
            return normalisiereAllgemeineRegion(menge, konfiguration, abbruchPrüfen)
        }
        if (konfiguration.dimension == RaumDimension.R1) {
            val exakt = ZahlengeradenNormalisierer.normalisiere(menge, konfiguration)
            if (exakt !is VisualisierungsDefinition.BedingtRäumlich) return exakt
            if (menge is EndlicheMenge) return normalisiereEndlicheMenge(menge, dimension, konfiguration.ausgabeKoordinaten, konfiguration.dimension == RaumDimension.C)
            if (menge is Vereinigung || menge is Schnitt || menge is MengenDifferenz ||
                menge is SymmetrischeDifferenz || menge is GefilterteMenge ||
                menge is PrädikatsMenge || menge is MengenFallAusdruck
            ) {
                return normalisiereAllgemeineRegion(menge, konfiguration, abbruchPrüfen)
            }
            return exakt
        }
        return when (menge) {
            LeereMenge -> VisualisierungsDefinition.ExaktePunkte(dimension, emptyList())
            KomplexeZahlen -> if (konfiguration.dimension == RaumDimension.C) {
                VisualisierungsDefinition.Region(
                    dimension = 2,
                    mitgliedschaft = { NumerischeMitgliedschaft.Enthalten },
                    zellNachweis = { ZellNachweis.Enthalten },
                )
            } else VisualisierungsDefinition.ProjektionErforderlich(
                vorhandeneDimension = 2,
                erwarteteDimension = dimension,
                grund = "ℂ wird über Real- und Imaginärteil im komplexen Darstellungsraum visualisiert.",
            )
            is EndlicheMenge -> normalisiereEndlicheMenge(menge, dimension, konfiguration.ausgabeKoordinaten, konfiguration.dimension == RaumDimension.C)
            is KartesischesProdukt -> normalisiereProdukt(menge, konfiguration, abbruchPrüfen)
            is DefinierteMenge -> normalisiereDefinierteMenge(menge, konfiguration, abbruchPrüfen)
            is Vereinigung, is Schnitt, is MengenDifferenz, is SymmetrischeDifferenz,
            is GefilterteMenge, is PrädikatsMenge, is MengenFallAusdruck ->
                normalisiereAllgemeineRegion(menge, konfiguration, abbruchPrüfen)
            else -> VisualisierungsDefinition.NichtRäumlich(
                "Die Mengenform ${menge::class.simpleName} besitzt im gewählten Raum keine unterstützte numerische Normalisierung.",
            )
        }
    }

    fun sample(
        menge: MengenAusdruck,
        konfiguration: VisualisierungsKonfiguration,
        abbruchPrüfen: () -> Unit = {},
    ): VisualisierungsErgebnis {
        val laufzeit = konfiguration.copy(sampling = konfiguration.sampling.copy(
            maximalesRasterBudget = if(konfiguration.sampling.automatisch) konfiguration.sampling.maximalesRasterBudget else minOf(konfiguration.sampling.maximalesRasterBudget,
                konfiguration.detailBudget),
        ))
        if (menge is Vereinigung && menge.mengen.any { it is EndlicheMenge || numerischeTrägerAchsen(it)?.any(::istDiskreteZahlmenge) == true }) {
            val teil = if(laufzeit.sampling.automatisch) laufzeit else laufzeit.copy(sampling = laufzeit.sampling.copy(maximalesRasterBudget = (laufzeit.sampling.maximalesRasterBudget / menge.mengen.size).coerceAtLeast(1)))
            val ergebnisse=mutableListOf<VisualisierungsErgebnis>()
            for(m in menge.mengen) {
                val sitzung=SamplingSitzung.aktuelle()
                val ergebnis=if(sitzung==null) sample(m,teil,abbruchPrüfen) else sitzung.mitAbbildung(
                    { vereinigeVisualisierungsErgebnisse(ergebnisse+it)!! }, { sample(m,teil,abbruchPrüfen) })
                ergebnisse+=ergebnis
            }
            return vereinigeVisualisierungsErgebnisse(ergebnisse) ?: VisualisierungsErgebnis.Erfolgreich(emptyList(),false)
        }
        if (menge is MethodenGraphMenge) {
            val m = menge.methode
            val signatur = m.mathematischeMethodenSignatur()
            val domäne = if (m.parameter.size == 1 && signatur.effektiverDefinitionsRaum == null)
                signatur.argumente.single().definitionsMenge else m.argumentRaum()
            val graphConfig = laufzeit.copy(methodenModus = MethodenDarstellungsModus.Funktionsgraph)
            val abbild = Abbild(if (m.parameter.size > 1 && domäne is Tupelraum) KartesischesProdukt(domäne.komponenten) else domäne, m)
            return if (m.parameter.size == 1 && (numerischeArgumentStruktur(domäne) is NumerischerRaum.Produkt) || m.vorschrift !is ZahlAusdruck)
                sampleStrukturierteBildmenge(abbild, graphConfig, abbruchPrüfen, graph = true)
            else sampleAbbild(abbild, graphConfig, abbruchPrüfen)
        }
        if (menge is Abbild) return sampleAbbild(menge, laufzeit, abbruchPrüfen)
        val räumlich = laufzeit.copy(bereiche = laufzeit.sampling.sichtBereiche ?: laufzeit.bereiche)
        // Die synchrone symbolische Normalisierung besitzt eine eigene Schutzgrenze.
        // Fortlaufende Materialisierung und adaptives Sampling enden am Speicher-/Qualitätsziel.
        val normalisierung=if(räumlich.sampling.automatisch) räumlich.copy(sampling=räumlich.sampling.copy(
            maximalesRasterBudget=AUTOMATISCHE_MATERIALISIERUNGS_GRENZE)) else räumlich
        return materialisiere(normalisiere(menge, normalisierung, abbruchPrüfen), räumlich, abbruchPrüfen)
    }

    private fun materialisiere(
        definition: VisualisierungsDefinition,
        konfiguration: VisualisierungsKonfiguration,
        abbruchPrüfen: () -> Unit,
    ): VisualisierungsErgebnis = when (definition) {
        is VisualisierungsDefinition.NichtRäumlich -> VisualisierungsErgebnis.NichtDarstellbar(definition.grund)
        is VisualisierungsDefinition.BedingtRäumlich -> VisualisierungsErgebnis.BedingtDarstellbar(
            definition.grund,
            definition.bedingungen,
        )
        is VisualisierungsDefinition.ProjektionErforderlich -> VisualisierungsErgebnis.ProjektionErforderlich(
            definition.vorhandeneDimension,
            definition.erwarteteDimension,
            definition.grund,
        )
        is VisualisierungsDefinition.ExaktePunkte -> materialisiereExaktePunkte(definition, konfiguration)
        is VisualisierungsDefinition.ProduktDomänen -> materialisiereProdukt(definition, konfiguration)
        is VisualisierungsDefinition.Zahlengerade -> materialisiereZahlengerade(definition, konfiguration)
        is VisualisierungsDefinition.Region -> sampleRegion(definition, konfiguration, abbruchPrüfen)
    }

    private fun normalisiereOrbitMenge(
        menge: OrbitBeschraenktheitsMenge,
        c: VisualisierungsKonfiguration,
        abbruchPrüfen: () -> Unit,
    ): VisualisierungsDefinition {
        val geometrie = orbitParameterGeometrie(menge.parameterRaum)
            ?: return VisualisierungsDefinition.NichtRäumlich(
                "Die räumliche Struktur des Orbit-Parameterraums kann nicht sicher bestimmt werden.",
            )
        val erwarteterRaum = when {
            geometrie.art == OrbitParameterArt.Komplex -> RaumDimension.C
            geometrie.dimension == 1 -> RaumDimension.R1
            geometrie.dimension == 2 -> RaumDimension.R2
            else -> RaumDimension.R3
        }
        if (c.dimension != erwarteterRaum) {
            return VisualisierungsDefinition.ProjektionErforderlich(
                vorhandeneDimension = geometrie.dimension,
                erwarteteDimension = c.raumDimension,
                grund = "Der Orbit-Parameterraum benötigt die Darstellung ${erwarteterRaum.name} ohne implizite Projektion.",
            )
        }
        val methodenParameter = menge.orbit.schritt
            .alsMathematischeMethode("die Orbitvisualisierung")
            .parameter[1]
        if (geometrie.art == OrbitParameterArt.Tupel && methodenParameter is Variable) {
            return VisualisierungsDefinition.NichtRäumlich(
                "Der mehrdimensionale Parameterraum benötigt ein allgemeines Parameterargument statt einer Zahlenvariable.",
            )
        }
        fun parameter(punkt: List<Double>): MathematischesObjekt? {
            if (punkt.size != geometrie.dimension || punkt.any { !it.isFinite() }) return null
            return when (geometrie.art) {
                OrbitParameterArt.Reell -> rationaleKoordinate(punkt.single())
                OrbitParameterArt.Komplex ->
                    KomplexeZahl(rationaleKoordinate(punkt[0]), rationaleKoordinate(punkt[1]))
                OrbitParameterArt.Tupel -> Tupel(punkt.map(::rationaleKoordinate))
            }
        }
        var verbleibendeSchritte = c.sampling.maximalesRasterBudget
        fun entscheide(block: (() -> Unit) -> OrbitEntscheidung): OrbitEntscheidung {
            if (verbleibendeSchritte <= 0) {
                return OrbitEntscheidung.Unbekannt("Das gemeinsame Orbit-Auswertungsbudget ist ausgeschöpft.")
            }
            verbleibendeSchritte-- // Die Entscheidung selbst verbraucht ebenfalls einen Auswertungsschritt.
            return try {
                block {
                    abbruchPrüfen()
                    if (verbleibendeSchritte <= 0) throw GemeinsamesOrbitBudgetErschöpft()
                    verbleibendeSchritte--
                }
            } catch (_: GemeinsamesOrbitBudgetErschöpft) {
                OrbitEntscheidung.Unbekannt("Das gemeinsame Orbit-Auswertungsbudget ist ausgeschöpft.")
            }
        }
        return VisualisierungsDefinition.Region(
            dimension = geometrie.dimension,
            hinweise = listOf(
                "Orbitzellen zeigen ausschließlich bewiesenen Einschluss, bewiesene Flucht oder ausdrücklich unbestimmte Bereiche.",
            ),
            mitgliedschaft = { punkt ->
                val parameterObjekt = parameter(punkt)
                    ?: return@Region NumerischeMitgliedschaft.Unbekannt("Der Rasterpunkt passt nicht zum Parameterraum.")
                when (val entscheidung = entscheide { budgetPrüfen ->
                    entscheideOrbitBeschraenktheit(
                        menge,
                        parameterObjekt,
                        c.sampling.maximaleOrbitSchritte,
                        budgetPrüfen,
                    )
                }) {
                    is OrbitEntscheidung.Enthalten -> NumerischeMitgliedschaft.Enthalten
                    is OrbitEntscheidung.Ausgeschlossen -> NumerischeMitgliedschaft.NichtEnthalten
                    is OrbitEntscheidung.Unbekannt -> NumerischeMitgliedschaft.Unbekannt(entscheidung.grund)
                }
            },
            zellNachweis = { zelle ->
                val entscheidung = if (
                    geometrie.art == OrbitParameterArt.Komplex &&
                    menge.parameterRaum == KomplexeZahlen &&
                    zelle.size == 2
                ) {
                    entscheide { budgetPrüfen ->
                        entscheideQuadratischeNullstartZelle(
                            menge,
                            zelle[0],
                            zelle[1],
                            c.sampling.maximaleOrbitSchritte,
                            budgetPrüfen,
                        )
                    }
                } else OrbitEntscheidung.Unbekannt("Für diese Orbitfamilie ist nur die Punktentscheidung verfügbar.")
                when (entscheidung) {
                    is OrbitEntscheidung.Enthalten -> ZellNachweis.Enthalten
                    is OrbitEntscheidung.Ausgeschlossen -> ZellNachweis.Ausgeschlossen
                    is OrbitEntscheidung.Unbekannt -> ZellNachweis.Unbekannt(entscheidung.grund)
                }
            },
        )
    }

    private fun orbitParameterGeometrie(menge: MengenAusdruck): OrbitParameterGeometrie? = when (menge) {
        NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen, NichtnegativeGanzeZahlenSemantik.menge, Primzahlen, is ReellesIntervall ->
            OrbitParameterGeometrie(OrbitParameterArt.Reell, 1)
        KomplexeZahlen -> OrbitParameterGeometrie(OrbitParameterArt.Komplex, 2)
        is KartesischesProdukt -> orbitTupelGeometrie(menge.mengen)
        is Tupelraum -> orbitTupelGeometrie(menge.komponenten)
        is MengenDifferenz -> orbitParameterGeometrie(menge.links)
        is GefilterteMenge -> orbitParameterGeometrie(menge.menge)
        is Vereinigung -> gemeinsameOrbitGeometrie(menge.mengen)
        is Schnitt -> if (menge.mengen.isEmpty()) menge.grundMenge?.let(::orbitParameterGeometrie)
            else gemeinsameOrbitGeometrie(menge.mengen)
        is SymmetrischeDifferenz -> gemeinsameOrbitGeometrie(listOf(menge.links, menge.rechts))
        is MengenFallAusdruck -> when (menge.aussage.entscheide().wahrheitswert) {
            Wahrheitswert.Wahr -> orbitParameterGeometrie(menge.wahr)
            Wahrheitswert.Lüge -> orbitParameterGeometrie(menge.lüge)
            null -> gemeinsameOrbitGeometrie(listOf(menge.wahr, menge.lüge))
        }
        is DefinierteMenge -> {
            val komponenten = menge.variablen.map { orbitParameterGeometrie(it.grundMenge) }
            if (komponenten.any { it != OrbitParameterGeometrie(OrbitParameterArt.Reell, 1) }) null
            else if (komponenten.size == 1) OrbitParameterGeometrie(OrbitParameterArt.Reell, 1)
            else komponenten.size.takeIf { it in 2..3 }?.let { OrbitParameterGeometrie(OrbitParameterArt.Tupel, it) }
        }
        is EndlicheMenge -> orbitGeometrieEndlicherElemente(menge)
        else -> null
    }

    private fun orbitTupelGeometrie(komponenten: List<MengenAusdruck>): OrbitParameterGeometrie? {
        if (komponenten.size !in 1..3) return null
        if (komponenten.any { orbitParameterGeometrie(it) != OrbitParameterGeometrie(OrbitParameterArt.Reell, 1) }) return null
        return OrbitParameterGeometrie(OrbitParameterArt.Tupel, komponenten.size)
    }

    private fun gemeinsameOrbitGeometrie(mengen: List<MengenAusdruck>): OrbitParameterGeometrie? =
        mengen.mapNotNull(::orbitParameterGeometrie)
            .takeIf { it.size == mengen.size }
            ?.distinct()
            ?.singleOrNull()

    private fun orbitGeometrieEndlicherElemente(menge: EndlicheMenge): OrbitParameterGeometrie? {
        if (menge.elemente.isEmpty()) return null
        if (menge.elemente.all { it is RationaleZahl }) return OrbitParameterGeometrie(OrbitParameterArt.Reell, 1)
        if (menge.elemente.all { it is KomplexeZahl }) return OrbitParameterGeometrie(OrbitParameterArt.Komplex, 2)
        val tupel = menge.elemente.filterIsInstance<Tupel>()
        val dimension = tupel.firstOrNull()?.elemente?.size ?: return null
        return if (
            tupel.size == menge.elemente.size && dimension in 1..3 &&
            tupel.all { it.elemente.size == dimension && it.elemente.all { wert -> wert is RationaleZahl } }
        ) OrbitParameterGeometrie(OrbitParameterArt.Tupel, dimension) else null
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
        projektion: List<Int> = emptyList(),
        komplex: Boolean = false,
    ): VisualisierungsDefinition.ExaktePunkte {
        val punkte = mutableListOf<List<Double>>()
        val diagnosen = mutableListOf<KoordinatenErgebnis>()
        menge.elemente.forEach { element ->
            SamplingSitzung.aktuelle()?.prüfe()
            samplingSpeicher(384)
            val punkt = if (komplex && element is ZahlAusdruck &&
                (DomaenenAuswerter.wert(element) as? DomaenenErgebnis.Wert)?.wert is DomaenenWert.Reell)
                KomplexeZahl(element, RationaleZahl.Null) else element
            when (val adapter = KoordinatenAdapter.extrahiere(punkt, dimension, ausgabeKoordinaten = projektion)) {
                is KoordinatenErgebnis.Darstellbar -> punkte += adapter.werte
                else -> diagnosen += adapter
            }
            if(punkte.size % 256 == 0) samplingZwischenstand {
                VisualisierungsErgebnis.Teilweise(punkte.map { VisualisierungsPunkt(it[0],it.getOrElse(1){0.0},it.getOrNull(2)) },
                    listOf("Endliche Menge wird aufgezählt; noch nicht vollständig."))
            }
        }
        return VisualisierungsDefinition.ExaktePunkte(dimension, punkte.distinct(), diagnosen)
    }

    private fun normalisiereKoordinatenBild(
        bild: KoordinatenBild,
        dimension: Int,
        projektion: List<Int> = emptyList(),
    ): VisualisierungsDefinition = when (val ergebnis = KoordinatenAdapter.extrahiere(bild, dimension, ausgabeKoordinaten = projektion)) {
        is KoordinatenErgebnis.Darstellbar -> VisualisierungsDefinition.ExaktePunkte(
            dimension,
            listOf(ergebnis.werte),
        )
        is KoordinatenErgebnis.BedingtDarstellbar -> VisualisierungsDefinition.BedingtRäumlich(
            ergebnis.grund,
            ergebnis.bedingungen,
        )
        is KoordinatenErgebnis.ProjektionErforderlich -> VisualisierungsDefinition.ProjektionErforderlich(
            ergebnis.vorhandeneDimension,
            ergebnis.erwarteteDimension,
            ergebnis.grund,
        )
        is KoordinatenErgebnis.NichtDarstellbar -> VisualisierungsDefinition.NichtRäumlich(ergebnis.grund)
    }

    private fun normalisiereProdukt(
        produkt: KartesischesProdukt,
        konfiguration: VisualisierungsKonfiguration,
        abbruchPrüfen: () -> Unit,
    ): VisualisierungsDefinition {
        val dimension = konfiguration.raumDimension
        val faktorDimensionen = produkt.mengen.map { faktor ->
            produktFaktorDimension(faktor) ?: return VisualisierungsDefinition.NichtRäumlich(
                "Die räumliche Dimension des Produktfaktors ${faktor::class.simpleName} ist nicht eindeutig.",
            )
        }
        val produktDimension = faktorDimensionen.sum()
        if (produktDimension != dimension) {
            return VisualisierungsDefinition.NichtRäumlich(
                "Das kartesische Produkt besitzt $produktDimension reelle Koordinaten, für ${konfiguration.dimension} werden genau $dimension benötigt. Eine Projektion ist nicht konfiguriert.",
            )
        }
        if (faktorDimensionen.any { it == 2 }) {
            val intervallBudget = AuswertungsBudget(
                (konfiguration.sampling.maximalesRasterBudget / 2).coerceAtLeast(1),
                abbruchPrüfen,
            )
            val reihenfolge = produkt.mengen.zip(faktorDimensionen).flatMapIndexed { index, (_, faktorDimension) ->
                if (faktorDimension == 2) listOf("Re(Faktor ${index + 1})", "Im(Faktor ${index + 1})")
                else listOf("Faktor ${index + 1}")
            }
            return VisualisierungsDefinition.Region(
                dimension = dimension,
                hinweise = listOf("Produktkoordinaten in Reihenfolge: ${reihenfolge.joinToString()}."),
                mitgliedschaft = { punkt ->
                    produktMitgliedschaft(produkt, faktorDimensionen, punkt, konfiguration)
                },
                zellArbeitsBudget = intervallBudget,
                zellNachweis = { zelle ->
                    produktZellNachweis(produkt, faktorDimensionen, zelle, konfiguration, intervallBudget)
                },
            )
        }
        if (produkt.mengen.any(::benötigtProduktRegion)) {
            val träger = numerischeTrägerAchsen(produkt)
            if (träger?.size == dimension && träger.any(::istDiskreteZahlmenge)) {
                val domänen = träger.mapIndexed { i, m -> when (val d = faktorDomäne(m, konfiguration.achsenBereiche[i], konfiguration)) {
                    is DomänenErgebnis.Erfolgreich -> d.domäne
                    is DomänenErgebnis.Fehler -> return if (d.offen) VisualisierungsDefinition.BedingtRäumlich(d.grund) else VisualisierungsDefinition.NichtRäumlich(d.grund)
                } }
                return VisualisierungsDefinition.ProduktDomänen(domänen) { punkt -> mitgliedschaft(produkt, punkt, konfiguration) }
            }
            val intervallBudget = AuswertungsBudget(
                (konfiguration.sampling.maximalesRasterBudget / 2).coerceAtLeast(1),
                abbruchPrüfen,
            )
            return VisualisierungsDefinition.Region(
                dimension = dimension,
                hinweise = listOf(
                    "Dichte oder mengenalgebraische Produktfaktoren werden im Sichtfenster mit offenen Zellnachweisen dargestellt.",
                ),
                fensterBegrenzt = true,
                mitgliedschaft = { punkt -> mitgliedschaft(produkt, punkt, konfiguration) },
                zellArbeitsBudget = intervallBudget,
                zellNachweis = { zelle -> zellNachweisMenge(produkt, zelle, konfiguration, intervallBudget) },
            )
        }
        val bereiche = konfiguration.achsenBereiche
        val domänen = produkt.mengen.mapIndexed { index, faktor ->
            when (val ergebnis = faktorDomäne(faktor, bereiche[index], konfiguration)) {
                is DomänenErgebnis.Erfolgreich -> ergebnis.domäne
                is DomänenErgebnis.Fehler -> return if (ergebnis.offen) VisualisierungsDefinition.BedingtRäumlich(ergebnis.grund)
                    else VisualisierungsDefinition.NichtRäumlich("Faktor ${index + 1} ist nicht darstellbar: ${ergebnis.grund}")
            }
        }
        return VisualisierungsDefinition.ProduktDomänen(domänen)
    }

    private fun produktFaktorDimension(faktor: MengenAusdruck): Int? =
        when (orbitParameterGeometrie(faktor)?.art) {
            OrbitParameterArt.Reell -> 1
            OrbitParameterArt.Komplex -> 2
            else -> null
        }

    private fun produktMitgliedschaft(
        produkt: KartesischesProdukt,
        faktorDimensionen: List<Int>,
        punkt: List<Double>,
        konfiguration: VisualisierungsKonfiguration,
    ): NumerischeMitgliedschaft {
        if (punkt.size != faktorDimensionen.sum()) {
            return NumerischeMitgliedschaft.Unbekannt("Produkt- und Punktdimension stimmen nicht überein.")
        }
        var offset = 0
        val ergebnisse = produkt.mengen.zip(faktorDimensionen).map { (faktor, faktorDimension) ->
            val koordinaten = punkt.subList(offset, offset + faktorDimension)
            offset += faktorDimension
            if (faktorDimension == 1) {
                faktorEnthält(faktor, koordinaten.single(), konfiguration.sampling.toleranz)
            } else {
                mitgliedschaft(
                    faktor,
                    koordinaten,
                    konfiguration.copy(
                        dimension = RaumDimension.C,
                        achsen = AchsenZuordnung("re", "im", null),
                    ),
                )
            }
        }
        return kombiniereMitgliedschaften(ergebnisse, und = true)
    }

    private fun produktZellNachweis(
        produkt: KartesischesProdukt,
        faktorDimensionen: List<Int>,
        zelle: List<RationalesIntervall>,
        konfiguration: VisualisierungsKonfiguration,
        budget: AuswertungsBudget,
    ): ZellNachweis {
        if (zelle.size != faktorDimensionen.sum()) {
            return ZellNachweis.Unbekannt("Produkt- und Zelldimension stimmen nicht überein.")
        }
        var offset = 0
        val nachweise = produkt.mengen.zip(faktorDimensionen).map { (faktor, faktorDimension) ->
            val komponenten = zelle.subList(offset, offset + faktorDimension)
            offset += faktorDimension
            when {
                faktorDimension == 1 -> zellNachweisGrundmenge(faktor, komponenten.single())
                faktor == KomplexeZahlen -> ZellNachweis.Enthalten
                else -> zellNachweisMenge(
                    faktor,
                    komponenten,
                    konfiguration.copy(
                        dimension = RaumDimension.C,
                        achsen = AchsenZuordnung("re", "im", null),
                    ),
                    budget,
                )
            }
        }
        return kombiniereZellNachweise(nachweise, und = true)
    }

    private fun benötigtProduktRegion(faktor: MengenAusdruck): Boolean = when (faktor) {
        RationaleZahlen, is Vereinigung, is Schnitt, is MengenDifferenz, is SymmetrischeDifferenz,
        is GefilterteMenge, is DefinierteMenge, is PrädikatsMenge, is MengenFallAusdruck -> true
        else -> false
    }

    private fun normalisiereDefinierteMenge(
        menge: DefinierteMenge,
        konfiguration: VisualisierungsKonfiguration,
        abbruchPrüfen: () -> Unit = {},
    ): VisualisierungsDefinition {
        val dimension = konfiguration.raumDimension
        val variablen = menge.variablen.map { it.variable.name }
        val achsen = konfiguration.achsenNamen
        if (achsen.size != dimension || achsen.any(String::isBlank)) {
            return VisualisierungsDefinition.NichtRäumlich("Für ${konfiguration.dimension} fehlt mindestens eine Achsenzuordnung.")
        }
        val farbVariable = konfiguration.farbe.variable?.takeIf { konfiguration.farbe.modus == FarbModus.Spektrum }
        val schnittVariablen = konfiguration.festeSchnitte.keys
        val zugeordnet = achsen + listOfNotNull(farbVariable) + schnittVariablen
        if (zugeordnet.distinct().size != zugeordnet.size) {
            return VisualisierungsDefinition.NichtRäumlich("Achsen, Farbdimension und feste Schnitte müssen verschiedene Variablen verwenden.")
        }
        val fehlend = variablen - zugeordnet.toSet()
        val unbekannt = zugeordnet - variablen.toSet()
        if (fehlend.isNotEmpty() || unbekannt.isNotEmpty()) {
            return VisualisierungsDefinition.NichtRäumlich(
                buildString {
                    append("Die definierte Menge bindet ${variablen.size} Variablen. ")
                    if (fehlend.isNotEmpty()) append("Nicht zugeordnete Mengenvariable: ${fehlend.joinToString()}. Weise sie einer Achse, Farbe oder einem festen Schnitt zu. ")
                    if (unbekannt.isNotEmpty()) append("Unbekannte zugeordnete Variable: ${unbekannt.joinToString()}.")
                }.trim(),
            )
        }
        val zusätzliche = menge.bedingung.freieVariablen().map { it.name }.toSet() - variablen.toSet()
        if (zusätzliche.isNotEmpty()) {
            return VisualisierungsDefinition.NichtRäumlich(
                "Die Mengenbedingung enthält zusätzliche freie Variablen: ${zusätzliche.sorted().joinToString()}.",
            )
        }
        fun wertePunkt(punkt: List<Double>, zusätzliche: Map<String, Double> = emptyMap()): NumerischeMitgliedschaft {
            val umgebung = achsen.zip(punkt).toMap() + konfiguration.festeSchnitte + zusätzliche
            for (gebunden in menge.variablen) {
                when (val grund = werteAussage(
                    ElementBeziehung(gebunden.variable, gebunden.grundMenge),
                    umgebung,
                    konfiguration.sampling.toleranz,
                )) {
                    NumerischeMitgliedschaft.Enthalten -> Unit
                    NumerischeMitgliedschaft.NichtEnthalten -> return NumerischeMitgliedschaft.NichtEnthalten
                    is NumerischeMitgliedschaft.Grenze -> Unit
                    is NumerischeMitgliedschaft.Unbekannt -> return NumerischeMitgliedschaft.Unbekannt(
                        "Grundmenge von ${gebunden.variable.name}: ${grund.grund}",
                    )
                }
            }
            return werteAussage(menge.bedingung, umgebung, konfiguration.sampling.toleranz)
        }
        val samplingDomänen = if (farbVariable == null && menge.variablen.any { istDiskreteZahlmenge(it.grundMenge) }) {
            val faktoren = achsen.mapIndexed { i, achse ->
                val grund = menge.variablen.single { it.variable.name == achse }.grundMenge
                when (val d = faktorDomäne(grund, konfiguration.achsenBereiche[i], konfiguration)) {
                    is DomänenErgebnis.Erfolgreich -> d.domäne
                    is DomänenErgebnis.Fehler -> if (grund is ReellesIntervall && grund.freieVariablen().isNotEmpty())
                        (faktorDomäne(ReelleZahlen, konfiguration.achsenBereiche[i], konfiguration) as DomänenErgebnis.Erfolgreich).domäne
                        else return if (d.offen) VisualisierungsDefinition.BedingtRäumlich(d.grund) else VisualisierungsDefinition.NichtRäumlich(d.grund)
                }
            }
            faktoren
        } else null
        val farbMitgliedschaft = farbVariable?.let { variable ->
            val farbBereich = konfiguration.farbe.bereich ?: ZahlenBereich(-1.0, 1.0)
            val werte = rasterWerte(farbBereich, minOf(48, konfiguration.sampling.auflösung2D))
            val prüfe: (List<Double>) -> List<Pair<Double, NumerischeMitgliedschaft>> = { punkt ->
                werte.map { wert -> wert to wertePunkt(punkt, mapOf(variable to wert)) }
            }
            prüfe
        }
        val intervallBudget = AuswertungsBudget(
            (konfiguration.sampling.maximalesRasterBudget / 2).coerceAtLeast(1),
            abbruchPrüfen,
        )
        val farbHinweise = if (farbVariable != null) {
            val bereich = konfiguration.farbe.bereich ?: ZahlenBereich(-1.0, 1.0)
            val anzahl = minOf(48, konfiguration.sampling.auflösung2D)
            listOf(
                "Die Farbdimension '$farbVariable' wird im Farbbereich " +
                    "[${kurzeZahl(bereich.minimum)}, ${kurzeZahl(bereich.maximum)}] mit $anzahl Rasterwerten numerisch abgetastet; " +
                    "die Werteliste ist fensterbegrenzt und unvollständig.",
            )
        } else emptyList()
        val vergleich = menge.bedingung as? Vergleich
        val hatStetigenRand = dimension == 2 && farbVariable == null && vergleich != null &&
            menge.variablen.all { it.grundMenge == ReelleZahlen } && istPolynom(vergleich.links) && istPolynom(vergleich.rechts)
        return VisualisierungsDefinition.Region(
            samplingDomänen = samplingDomänen,
            randResiduum = if (hatStetigenRand) { punkt ->
                val env = achsen.zip(punkt).toMap() + konfiguration.festeSchnitte
                val a = numerischerWert(vergleich!!.links, env); val b = numerischerWert(vergleich.rechts, env)
                if (a == null || b == null) null else a-b
            } else null,
            offenerRand = vergleich?.art in setOf(VergleichsArt.Kleiner, VergleichsArt.Größer),
            dimension = dimension,
            hinweise = farbHinweise,
            fensterBegrenzt = farbVariable != null,
            mitgliedschaft = { punkt ->
                if (farbVariable == null) wertePunkt(punkt)
                else NumerischeMitgliedschaft.Unbekannt("Die Mitgliedschaft besitzt eine zugeordnete Farbdimension; Werte werden getrennt ausgewertet.")
            },
            zellNachweis = { zelle ->
                zellNachweisDefinierteMenge(menge, achsen, zelle, konfiguration, farbVariable, intervallBudget)
            },
            farbMitgliedschaft = farbMitgliedschaft,
            zellArbeitsBudget = intervallBudget,
            numerischeFläche = menge.variablen.all {it.grundMenge == ReelleZahlen || it.grundMenge is ReellesIntervall},
            farbProbenAnzahl = if(farbVariable == null) 1 else minOf(48, konfiguration.sampling.auflösung2D),
        )
    }

    /** Bindet das öffentliche Gesamtargument einmal; keine Namenssubstitution im Pointer-/Zeichenpfad. */
    private fun externeFilterParameter(methode: MathematischeMethode): Set<MethodenParameter> {
        val objekte = listOf(methode.vorschrift) + methode.voraussetzungen +
            methode.werteVorräte.values + listOfNotNull(methode.effektiverWerteVorrat)
        fun freie(objekt: MathematischesObjekt): Set<MethodenParameter> = when(objekt) {
            is DefinierteMenge -> (objekt.variablen.flatMap { freie(it.grundMenge) } + freie(objekt.bedingung)).filterNot { frei ->
                objekt.variablen.any { (it.variable.bindungsId ?: it.variable.name) == (frei.bindungsId ?: frei.name) }
            }.toSet()
            is Tupelraum -> objekt.komponenten.flatMap(::freie).toSet()
            is KartesischesProdukt -> objekt.mengen.flatMap(::freie).toSet()
            else -> objekt.freieMethodenParameter()
        }
        return objekte.flatMap(::freie).filterNot { frei ->
            methode.parameter.any { (it.bindungsId ?: it.name) == (frei.bindungsId ?: frei.name) }
        }.toSet()
    }

    /** Entpackt Definitionsräume strukturell; ein Tupelargument bleibt ein Argument. */
    private fun filterBedingung(aussage: Aussage, reelleKoordinaten: Set<Variable> = emptySet()): Aussage {
        fun inMenge(element: MathematischesObjekt, menge: MengenAusdruck): Aussage {
            when (menge) {
                is MengenDifferenz -> return Konjunktion(listOf(inMenge(element, menge.links), Negation(inMenge(element, menge.rechts))))
                is Vereinigung -> return Disjunktion(menge.mengen.map { inMenge(element, it) })
                is Schnitt -> return if (menge.mengen.isEmpty()) menge.grundMenge?.let { inMenge(element, it) } ?: ElementBeziehung(element, menge)
                    else Konjunktion(menge.mengen.map { inMenge(element, it) })
                else -> Unit
            }
            val komponenten = menge.kartesischeFaktorenOderNull()
            if (element is Tupel && komponenten != null && element.elemente.size == komponenten.size)
                return Konjunktion(element.elemente.zip(komponenten).map { (e, m) -> inMenge(e, m) })
            val raum = numerischerRaum(menge)
            if (raum is NumerischerRaum.Produkt) {
                val b = raum.bedingung(element)
                return if (menge == GaußschePrimzahlen) b else filterBedingung(b, reelleKoordinaten)
            }
            if (menge is DefinierteMenge) {
                val elemente = if (menge.variablen.size == 1) listOf(element) else (element as? Tupel)?.elemente
                if (elemente != null && elemente.size == menge.variablen.size) {
                    val bindungen = menge.variablen.zip(elemente).associate { (v, e) -> (v.variable.bindungsId ?: v.variable.name) to e }
                    return Konjunktion(menge.variablen.zip(elemente).map { (v, e) -> inMenge(e, ersetze(v.grundMenge, bindungen) as MengenAusdruck) } +
                        filterBedingung(ersetze(menge.bedingung, bindungen), reelleKoordinaten))
                }
            }
            if (element is Variable && element in reelleKoordinaten && menge == ReelleZahlen) return WahrheitsKonstante(true)
            if (element is ZahlAusdruck && menge is ReellesIntervall) return Konjunktion(listOf(
                Vergleich(menge.links, if(menge.linksOffen) VergleichsArt.Kleiner else VergleichsArt.KleinerGleich, element),
                Vergleich(element, if(menge.rechtsOffen) VergleichsArt.Kleiner else VergleichsArt.KleinerGleich, menge.rechts),
            ))
            return ElementBeziehung(element, menge)
        }
        return when(aussage) {
            is ElementBeziehung -> inMenge(aussage.element, aussage.menge)
            is Konjunktion -> Konjunktion(aussage.aussagen.map { filterBedingung(it, reelleKoordinaten) })
            else -> aussage
        }
    }

    private fun normalisiereFilter(
        filter: GefilterteMenge, c: VisualisierungsKonfiguration, abbruchPrüfen: () -> Unit,
    ): VisualisierungsDefinition? {
        val variablen = mutableListOf<GebundeneMengenVariable>()
        fun blatt(menge: MengenAusdruck): Variable {
            val auswahl = c.ausgabeKoordinaten
            val achse = if (auswahl.isEmpty()) variablen.size else auswahl.indexOf(variablen.size + 1)
            val name = if (achse >= 0) c.achsenNamen.getOrNull(achse) ?: error("Für die Filterkoordinate fehlt eine Achse.")
                else "k${variablen.size + 1}"
            return Variable(name).also { variablen += GebundeneMengenVariable(it, menge) }
        }
        fun packe(menge: MengenAusdruck): MathematischesObjekt {
            val raum = numerischeArgumentStruktur(menge) ?: error(raumInterpretationsHinweis(menge) ?: "Die Grundmenge hat keine numerische Koordinatenstruktur.")
            return raum.packe(raum.blätter.map(::blatt).iterator())
        }
        return try {
            val punkt = packe(filter.menge)
            if (c.ausgabeKoordinaten.isEmpty() && variablen.size != c.raumDimension) return null
            val methode = filter.methode.alsMathematischeMethode("die Filterdarstellung")
            val extern = externeFilterParameter(methode)
            if (extern.isNotEmpty()) return VisualisierungsDefinition.BedingtRäumlich(
                "Der Filter enthält ungebundene externe Parameter: ${extern.joinToString { it.name }}.",
            )
            val argumente = Tupel(listOf(punkt))
            if(methode.prüfeDefinitionsBedingungen(argumente).any { it.entscheide().wahrheitswert == Wahrheitswert.Lüge }) {
                return normalisiereDefinierteMenge(DefinierteMenge(variablen,WahrheitsKonstante(false)),c,abbruchPrüfen)
            }
            val anwendung = methode.wendeMitBedingungenAn(argumente)
            val prädikat = anwendung.ergebnis.elemente.single().alsPrädikatsAussageOderNull() ?: return null
            // Diese konkrete Mitgliedschaft wird bereits durch die gebundenen Grundmengen zertifiziert.
            val voraussetzungen = anwendung.bedingungen.filterNot {
                it is ElementBeziehung && it.element == punkt && it.menge == filter.menge
            }
            fun teile(a: Aussage): List<Aussage> = if(a is Konjunktion) a.aussagen.flatMap(::teile) else listOf(vereinfacheAussage(a, RechenKontext()))
            val basisRaum = numerischerRaum(filter.menge)
            val basisBedingungen = if (basisRaum == null) listOf(ElementBeziehung(punkt, filter.menge))
                else if (basisRaum.hatZusatzBedingung()) listOf(basisRaum.bedingung(punkt)) else emptyList()
            val teile = (listOf(prädikat) + voraussetzungen + basisBedingungen).map { filterBedingung(it, variablen.map { v -> v.variable }.toSet()) }.flatMap(::teile).filterNot { it is WahrheitsKonstante && it.wert }
            val bedingung = if(teile.any { it is WahrheitsKonstante && !it.wert }) WahrheitsKonstante(false)
                else teile.singleOrNull() ?: if(teile.isEmpty()) WahrheitsKonstante(true) else Konjunktion(teile)
            val definition = normalisiereDefinierteMenge(DefinierteMenge(variablen, bedingung), c, abbruchPrüfen)
            definition
        } catch (abbruch: java.util.concurrent.CancellationException) { throw abbruch
        } catch (_: IllegalArgumentException) { null
        } catch (_: IllegalStateException) { null }
    }

    /** Unsupported certificates are not recursively requested throughout the entire viewport. */
    private fun hatZellNachweis(menge: MengenAusdruck): Boolean = when(menge) {
        LeereMenge, ReelleZahlen, RationaleZahlen, GanzeZahlen, NatürlicheZahlen, KomplexeZahlen,
        is ReellesIntervall, is EndlicheMenge, is DefinierteMenge -> true
        is KartesischesProdukt -> menge.mengen.all(::hatZellNachweis)
        is Vereinigung -> menge.mengen.all(::hatZellNachweis)
        is Schnitt -> menge.mengen.all(::hatZellNachweis) && (menge.mengen.isNotEmpty() || menge.grundMenge?.let(::hatZellNachweis) == true)
        is MengenDifferenz -> hatZellNachweis(menge.links) && hatZellNachweis(menge.rechts)
        is SymmetrischeDifferenz -> hatZellNachweis(menge.links) && hatZellNachweis(menge.rechts)
        else -> numerischerRaum(menge)?.blätter?.all { it != menge && hatZellNachweis(it) } == true
    }

    private fun normalisiereAllgemeineRegion(
        menge: MengenAusdruck,
        konfiguration: VisualisierungsKonfiguration,
        abbruchPrüfen: () -> Unit = {},
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
        val intervallBudget = AuswertungsBudget(
            (konfiguration.sampling.maximalesRasterBudget / 2).coerceAtLeast(1),
            abbruchPrüfen,
        )
        val träger = numerischeTrägerAchsen(menge)
        val domänen = if (träger?.size == konfiguration.raumDimension && träger.any(::istDiskreteZahlmenge)) {
            träger.mapIndexed { i, m -> when (val d = faktorDomäne(m, konfiguration.achsenBereiche[i], konfiguration)) {
                is DomänenErgebnis.Erfolgreich -> d.domäne
                is DomänenErgebnis.Fehler -> return if (d.offen) VisualisierungsDefinition.BedingtRäumlich(d.grund) else VisualisierungsDefinition.NichtRäumlich(d.grund)
            } }
        } else null
        return VisualisierungsDefinition.Region(
            samplingDomänen = domänen,
            dimension = konfiguration.raumDimension,
            numerischeFläche = menge is PrädikatsMenge && konfiguration.sampling.fensterBegrenztePrädikatsMengen,
            fensterBegrenzt = hinweise.isNotEmpty(),
            mitgliedschaft = { punkt -> mitgliedschaft(menge, punkt, konfiguration) },
            zellNachweis = if(hatZellNachweis(menge)) { zelle -> zellNachweisMenge(menge, zelle, konfiguration, intervallBudget) } else null,
            zellArbeitsBudget = intervallBudget,
            hinweise = hinweise + if(hatZellNachweis(menge)) emptyList() else listOf("Für diese Mengenform ist kein Zellzertifikat verfügbar; die Fläche wird numerisch angenähert."),
        )
    }

    private fun materialisiereExaktePunkte(
        definition: VisualisierungsDefinition.ExaktePunkte,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsErgebnis {
        val punkte = definition.punkte.map { werte -> werte.alsPunkt(konfiguration) }
        if (definition.punkte.isEmpty() && definition.diagnosen.isEmpty()) {
            return VisualisierungsErgebnis.Erfolgreich(
                punkte = emptyList(),
                istApproximation = false,
                hinweise = listOf("Die Menge ist mathematisch leer."),
                qualität = VisualisierungsQualität.MathematischLeer,
            )
        }
        if (definition.diagnosen.isNotEmpty()) {
            val gruppiert = definition.diagnosen.groupingBy { it }.eachCount()
            val hinweise = gruppiert.map { (diagnose, anzahl) -> "$anzahl × ${diagnose.beschreibung}" }
            return if (punkte.isEmpty()) {
                val projektionen = definition.diagnosen.filterIsInstance<KoordinatenErgebnis.ProjektionErforderlich>()
                val bedingte = definition.diagnosen.filterIsInstance<KoordinatenErgebnis.BedingtDarstellbar>()
                val nichtDarstellbare = definition.diagnosen.filterIsInstance<KoordinatenErgebnis.NichtDarstellbar>()
                when {
                    projektionen.isNotEmpty() && bedingte.isEmpty() && nichtDarstellbare.isEmpty() -> {
                        val erste = projektionen.first()
                        VisualisierungsErgebnis.ProjektionErforderlich(
                            erste.vorhandeneDimension,
                            erste.erwarteteDimension,
                            hinweise.joinToString(" "),
                        )
                    }
                    bedingte.isNotEmpty() && projektionen.isEmpty() && nichtDarstellbare.isEmpty() ->
                        VisualisierungsErgebnis.BedingtDarstellbar(
                            hinweise.joinToString(" "),
                            bedingte.flatMap { it.bedingungen }.distinct(),
                        )
                    else -> VisualisierungsErgebnis.NichtDarstellbar(
                        "Die endliche Menge enthält keine darstellbaren ${definition.dimension}-dimensionalen Koordinaten. ${hinweise.joinToString(" ")}",
                    )
                }
            } else VisualisierungsErgebnis.Teilweise(punkte, hinweise)
        }
        return VisualisierungsErgebnis.Erfolgreich(punkte, istApproximation = false)
    }

    private fun zellNachweisDefinierteMenge(
        menge: DefinierteMenge,
        achsen: List<String>,
        zelle: List<RationalesIntervall>,
        c: VisualisierungsKonfiguration,
        farbVariable: String?,
        budget: AuswertungsBudget,
    ): ZellNachweis {
        if (farbVariable != null) {
            return ZellNachweis.Unbekannt("Die Zelle enthält mehrere Werte der Farbdimension '$farbVariable'.")
        }
        val umgebung = achsen.zip(zelle).toMap() + c.festeSchnitte.mapValues { RationalesIntervall(rationaleKoordinate(it.value)) }
        val grundNachweise = menge.variablen.map { gebunden ->
            val intervall = umgebung[gebunden.variable.name]
                ?: return ZellNachweis.Unbekannt("Für ${gebunden.variable.name} fehlt eine Zell- oder Schnittbindung.")
            zellNachweisGrundmenge(gebunden.grundMenge, intervall)
        }
        if (grundNachweise.any { it is ZellNachweis.Ausgeschlossen }) return ZellNachweis.Ausgeschlossen
        val bedingung = ZertifizierterIntervallAuswerter.aussage(
            menge.bedingung,
            umgebung,
            budget,
        )
        if (bedingung.wahrheitswert == Wahrheitswert.Lüge) return ZellNachweis.Ausgeschlossen
        if (grundNachweise.all { it is ZellNachweis.Enthalten } && bedingung.wahrheitswert == Wahrheitswert.Wahr) {
            return ZellNachweis.Enthalten
        }
        val gründe = grundNachweise.mapNotNull {
            when (it) {
                is ZellNachweis.Gemischt -> it.grund
                is ZellNachweis.Unbekannt -> it.grund
                else -> null
            }
        } + listOfNotNull(bedingung.begründung.takeIf { bedingung.wahrheitswert == null && it.isNotBlank() })
        return ZellNachweis.Unbekannt(gründe.distinct().joinToString().ifBlank { "Zugehörigkeit ist in dieser Zelle nicht einheitlich bewiesen." })
    }

    private fun zellNachweisMenge(
        menge: MengenAusdruck,
        zelle: List<RationalesIntervall>,
        c: VisualisierungsKonfiguration,
        budget: AuswertungsBudget,
    ): ZellNachweis {
        val raum = numerischerRaum(menge)
        if (raum is NumerischerRaum.Produkt && raum.blätter.size == zelle.size) {
            if (raum.hatZusatzBedingung()) return ZellNachweis.Unbekannt("Zusätzliche diskrete Raumbedingungen werden punktweise geprüft.")
            return kombiniereZellNachweise(raum.blätter.zip(zelle).map { (m, z) -> zellNachweisGrundmenge(m, z) }, und = true)
        }
        return when (menge) {
        LeereMenge -> ZellNachweis.Ausgeschlossen
        ReelleZahlen, RationaleZahlen, GanzeZahlen, NatürlicheZahlen, is ReellesIntervall, is EndlicheMenge -> {
            if (zelle.size == 1) zellNachweisGrundmenge(menge, zelle.single())
            else ZellNachweis.Unbekannt("Eine eindimensionale Zahlenmenge benötigt eine eindimensionale Zelle.")
        }
        KomplexeZahlen -> if (c.dimension == RaumDimension.C && zelle.size == 2) ZellNachweis.Enthalten
            else ZellNachweis.Unbekannt("ℂ benötigt den komplexen Darstellungsraum.")
        is DefinierteMenge -> zellNachweisDefinierteMenge(menge, c.achsenNamen, zelle, c, null, budget)
        is KartesischesProdukt -> {
            if (menge.mengen.size != zelle.size) ZellNachweis.Unbekannt("Produkt- und Zelldimension stimmen nicht überein.")
            else kombiniereZellNachweise(menge.mengen.mapIndexed { index, faktor -> zellNachweisGrundmenge(faktor, zelle[index]) }, und = true)
        }
        is Vereinigung -> kombiniereZellNachweise(menge.mengen.map { zellNachweisMenge(it, zelle, c, budget) }, und = false)
        is Schnitt -> if (menge.mengen.isEmpty()) {
            menge.grundMenge?.let { zellNachweisMenge(it, zelle, c, budget) }
                ?: ZellNachweis.Unbekannt("Ein leerer Schnitt benötigt eine Grundmenge.")
        } else kombiniereZellNachweise(menge.mengen.map { zellNachweisMenge(it, zelle, c, budget) }, und = true)
        is MengenDifferenz -> differenzZellNachweis(
            zellNachweisMenge(menge.links, zelle, c, budget),
            zellNachweisMenge(menge.rechts, zelle, c, budget),
        )
        is SymmetrischeDifferenz -> if (menge.links == menge.rechts) ZellNachweis.Ausgeschlossen
            else xorZellNachweis(
                zellNachweisMenge(menge.links, zelle, c, budget),
                zellNachweisMenge(menge.rechts, zelle, c, budget),
            )
        is MengenFallAusdruck -> when (menge.aussage.entscheide().wahrheitswert) {
            Wahrheitswert.Wahr -> zellNachweisMenge(menge.wahr, zelle, c, budget)
            Wahrheitswert.Lüge -> zellNachweisMenge(menge.lüge, zelle, c, budget)
            null -> ZellNachweis.Unbekannt("Die Fallbedingung ist nicht bewiesen.")
        }
        else -> ZellNachweis.Unbekannt("${menge::class.simpleName} besitzt keinen Zellnachweis.")
    }

    }

    private fun zellNachweisGrundmenge(menge: MengenAusdruck, zelle: RationalesIntervall): ZellNachweis = when (menge) {
        ReelleZahlen -> ZellNachweis.Enthalten
        LeereMenge -> ZellNachweis.Ausgeschlossen
        NatürlicheZahlen -> when {
            zelle.maximum < RationaleZahl.Eins -> ZellNachweis.Ausgeschlossen
            zelle.punkt && zelle.minimum.nenner == BigInteger.ONE && zelle.minimum.zähler.signum() > 0 -> ZellNachweis.Enthalten
            zelle.punkt -> ZellNachweis.Ausgeschlossen
            else -> ZellNachweis.Unbekannt("Die Zelle enthält möglicherweise natürliche und nichtnatürliche Zahlen.")
        }
        GanzeZahlen -> when {
            zelle.punkt && zelle.minimum.nenner == BigInteger.ONE -> ZellNachweis.Enthalten
            zelle.punkt -> ZellNachweis.Ausgeschlossen
            else -> ZellNachweis.Unbekannt("Die Zelle enthält möglicherweise ganze und nichtganze Zahlen.")
        }
        RationaleZahlen -> if (zelle.punkt) ZellNachweis.Enthalten
            else ZellNachweis.Gemischt("Jedes nichtentartete reelle Intervall enthält rationale und irrationale Zahlen.")
        is ReellesIntervall -> {
            val links = vereinfache(menge.links) as? RationaleZahl
                ?: return ZellNachweis.Unbekannt("Linke Intervallgrenze ist nicht exakt rational.")
            val rechts = vereinfache(menge.rechts) as? RationaleZahl
                ?: return ZellNachweis.Unbekannt("Rechte Intervallgrenze ist nicht exakt rational.")
            val linksGanzDrin = zelle.minimum > links || zelle.minimum == links && !menge.linksOffen
            val rechtsGanzDrin = zelle.maximum < rechts || zelle.maximum == rechts && !menge.rechtsOffen
            val linksVorbei = zelle.maximum < links || zelle.maximum == links && menge.linksOffen
            val rechtsVorbei = zelle.minimum > rechts || zelle.minimum == rechts && menge.rechtsOffen
            when {
                linksGanzDrin && rechtsGanzDrin -> ZellNachweis.Enthalten
                linksVorbei || rechtsVorbei -> ZellNachweis.Ausgeschlossen
                else -> ZellNachweis.Gemischt("Die Zelle schneidet eine Intervallgrenze.")
            }
        }
        is EndlicheMenge -> if (zelle.punkt) {
            when (ElementBeziehung(zelle.minimum, menge).entscheide().wahrheitswert) {
                Wahrheitswert.Wahr -> ZellNachweis.Enthalten
                Wahrheitswert.Lüge -> ZellNachweis.Ausgeschlossen
                null -> ZellNachweis.Unbekannt("Punktgleichheit mit der endlichen Menge ist ungeklärt.")
            }
        } else ZellNachweis.Unbekannt("Eine nichtentartete Zelle kann Einzelpunkte enthalten.")
        is Vereinigung -> kombiniereZellNachweise(menge.mengen.map { zellNachweisGrundmenge(it, zelle) }, und = false)
        is Schnitt -> if (menge.mengen.isEmpty()) menge.grundMenge?.let { zellNachweisGrundmenge(it, zelle) }
            ?: ZellNachweis.Unbekannt("Ein leerer Schnitt benötigt eine Grundmenge.")
            else kombiniereZellNachweise(menge.mengen.map { zellNachweisGrundmenge(it, zelle) }, und = true)
        is MengenDifferenz -> differenzZellNachweis(zellNachweisGrundmenge(menge.links, zelle), zellNachweisGrundmenge(menge.rechts, zelle))
        is SymmetrischeDifferenz -> if (menge.links == menge.rechts) ZellNachweis.Ausgeschlossen
            else xorZellNachweis(zellNachweisGrundmenge(menge.links, zelle), zellNachweisGrundmenge(menge.rechts, zelle))
        else -> ZellNachweis.Unbekannt("${menge::class.simpleName} besitzt keinen eindimensionalen Zellnachweis.")
    }

    private fun kombiniereZellNachweise(nachweise: List<ZellNachweis>, und: Boolean): ZellNachweis {
        if (und && nachweise.any { it is ZellNachweis.Ausgeschlossen }) return ZellNachweis.Ausgeschlossen
        if (!und && nachweise.any { it is ZellNachweis.Enthalten }) return ZellNachweis.Enthalten
        if (nachweise.isEmpty()) return if (und) ZellNachweis.Unbekannt("Leere Konjunktion ohne Grundmenge.") else ZellNachweis.Ausgeschlossen
        if (und && nachweise.all { it is ZellNachweis.Enthalten }) return ZellNachweis.Enthalten
        if (!und && nachweise.all { it is ZellNachweis.Ausgeschlossen }) return ZellNachweis.Ausgeschlossen
        val gründe = nachweise.mapNotNull {
            when (it) {
                is ZellNachweis.Gemischt -> it.grund
                is ZellNachweis.Unbekannt -> it.grund
                else -> null
            }
        }.distinct()
        if (nachweise.any { it is ZellNachweis.Unbekannt }) return ZellNachweis.Unbekannt(gründe.joinToString())
        val gemischte = nachweise.count { it is ZellNachweis.Gemischt }
        val restIstNeutral = if (und) nachweise.all { it is ZellNachweis.Gemischt || it is ZellNachweis.Enthalten }
            else nachweise.all { it is ZellNachweis.Gemischt || it is ZellNachweis.Ausgeschlossen }
        return if (gemischte == 1 && restIstNeutral) {
            ZellNachweis.Gemischt(gründe.joinToString().ifBlank { "Ein Operand ist innerhalb der Zelle gemischt." })
        } else ZellNachweis.Unbekannt(
            "Die Abhängigkeit mehrerer gemischter Operanden innerhalb der Zelle ist nicht bewiesen.",
        )
    }

    private fun differenzZellNachweis(links: ZellNachweis, rechts: ZellNachweis): ZellNachweis = when {
        links is ZellNachweis.Ausgeschlossen || rechts is ZellNachweis.Enthalten -> ZellNachweis.Ausgeschlossen
        links is ZellNachweis.Enthalten && rechts is ZellNachweis.Ausgeschlossen -> ZellNachweis.Enthalten
        links is ZellNachweis.Unbekannt -> links
        rechts is ZellNachweis.Unbekannt -> rechts
        links is ZellNachweis.Gemischt && rechts is ZellNachweis.Gemischt ->
            ZellNachweis.Unbekannt("Die Abhängigkeit der gemischten Differenzoperanden ist nicht bewiesen.")
        else -> ZellNachweis.Gemischt("Die Differenz ist innerhalb der Zelle nicht einheitlich.")
    }

    private fun xorZellNachweis(links: ZellNachweis, rechts: ZellNachweis): ZellNachweis = when {
        links is ZellNachweis.Unbekannt -> links
        rechts is ZellNachweis.Unbekannt -> rechts
        links is ZellNachweis.Gemischt && rechts is ZellNachweis.Gemischt ->
            ZellNachweis.Unbekannt("Die Abhängigkeit der gemischten Operanden ist nicht bewiesen.")
        links is ZellNachweis.Gemischt || rechts is ZellNachweis.Gemischt ->
            ZellNachweis.Gemischt("Die symmetrische Differenz ist innerhalb der Zelle gemischt.")
        links::class == rechts::class -> ZellNachweis.Ausgeschlossen
        else -> ZellNachweis.Enthalten
    }

    private fun materialisiereProdukt(
        definition: VisualisierungsDefinition.ProduktDomänen,
        konfiguration: VisualisierungsKonfiguration,
    ): VisualisierungsErgebnis {
        val größe = definition.faktoren.fold(1L) { akk, domäne ->
            if (domäne.werte.isEmpty()) {
                val mathematischLeer = definition.faktoren.any { it.mathematischLeer }
                return VisualisierungsErgebnis.Erfolgreich(
                    emptyList(),
                    false,
                    definition.faktoren.flatMap { it.hinweise }.distinct() +
                        if (mathematischLeer) "Mindestens ein Produktfaktor ist mathematisch leer."
                        else "Mindestens ein Produktfaktor hat im Sichtfenster keine Treffer.",
                    if (mathematischLeer) VisualisierungsQualität.MathematischLeer
                    else VisualisierungsQualität.KeineTrefferImFenster,
                )
            }
            akk * domäne.werte.size
        }
        if (!konfiguration.sampling.automatisch && größe > konfiguration.sampling.maximalesRasterBudget) {
            return VisualisierungsErgebnis.NichtDarstellbar(
                "Das Produkt würde $größe Punkte materialisieren und überschreitet das Rasterbudget von ${konfiguration.sampling.maximalesRasterBudget}.",
            )
        }
        val approximation = definition.faktoren.any { it.istApproximation }
        val diagnosen = linkedSetOf<String>()
        val punkte = mutableListOf<VisualisierungsPunkt>()
        val koordinaten = MutableList(definition.faktoren.size) { 0.0 }
        var besucht=0
        fun besuche(achse:Int) {
            SamplingSitzung.aktuelle()?.prüfe()
            if(achse<definition.faktoren.size) {
                definition.faktoren[achse].werte.forEach { koordinaten[achse]=it;besuche(achse+1) }
                return
            }
            besucht++
            val enthalten=when(val m=definition.mitgliedschaft?.invoke(koordinaten)) {
                null,NumerischeMitgliedschaft.Enthalten -> true
                NumerischeMitgliedschaft.NichtEnthalten -> false
                is NumerischeMitgliedschaft.Grenze -> abs(m.residuum)<=konfiguration.sampling.toleranz
                is NumerischeMitgliedschaft.Unbekannt -> { diagnosen+=m.grund;false }
            }
            if(enthalten) { samplingSpeicher(192);punkte+=koordinaten.alsPunkt(konfiguration) }
            if(besucht%256==0) samplingZwischenstand {
                VisualisierungsErgebnis.Teilweise(punkte.toList(),listOf("Produkt wird aufgezählt; noch nicht vollständig.")+diagnosen)
            }
        }
        besuche(0)
        if(diagnosen.isNotEmpty()) return VisualisierungsErgebnis.Teilweise(punkte,diagnosen.toList())
        return VisualisierungsErgebnis.Erfolgreich(punkte,approximation,
            definition.faktoren.flatMap { it.hinweise }.distinct(),
            if(punkte.isEmpty()) VisualisierungsQualität.KeineTrefferImFenster else if(approximation) VisualisierungsQualität.Approximation else VisualisierungsQualität.Exakt)
    }

    private fun sampleRegion(
        region: VisualisierungsDefinition.Region,
        c: VisualisierungsKonfiguration,
        abbruchPrüfen: () -> Unit,
    ): VisualisierungsErgebnis {
        region.samplingDomänen?.let {
            return materialisiereProdukt(VisualisierungsDefinition.ProduktDomänen(it, region.mitgliedschaft), c)
        }
        val gesamtBudget = c.sampling.maximalesRasterBudget
        val zellLimit = if(region.zellNachweis == null) 0 else gesamtBudget / 2
        val zellBudget = region.zellArbeitsBudget ?: AuswertungsBudget(zellLimit.coerceAtLeast(1), abbruchPrüfen)
        val kostenProPunkt = region.farbProbenAnzahl + if(region.randResiduum == null) 0 else 1
        val konfiguriertesN = when(region.dimension) {
            1 -> c.sampling.auflösung1D; 2 -> c.sampling.auflösung2D; else -> c.sampling.auflösung3D
        }
        val zielAuflösung = if(c.sampling.bildBreite > 0 && c.sampling.bildHöhe > 0)
            ceil(maxOf(c.sampling.bildBreite, c.sampling.bildHöhe) * 1.2 / c.sampling.zielFehlerPixel).toInt().coerceIn(2,65536)
            else konfiguriertesN
        val bereiche = c.achsenBereiche
        val punkte = mutableListOf<VisualisierungsPunkt>()
        val unbekannteGründe = linkedSetOf<String>()
        val zellen = if(region.zellNachweis == null && (region.dimension != 2 || !region.numerischeFläche))
            listOf(VisualisierungsZelle(bereiche.map {it.minimum},bereiche.map {it.maximum},ZellenStatus.Unbekannt,"Kein Zellzertifikat verfügbar."))
            else if(zellLimit == 0 || region.zellNachweis == null) emptyList()
            else adaptiveZellen(region, bereiche, zielAuflösung, zellBudget, abbruchPrüfen)
        val zellArbeit = if(region.zellNachweis == null) 0 else zellBudget.verbraucht
        val punktBudget = ((gesamtBudget - zellArbeit).coerceAtLeast(0) / kostenProPunkt)
        val budgetN = floor(punktBudget.toDouble().pow(1.0 / region.dimension)).toInt()
        if(budgetN < 2) return VisualisierungsErgebnis.Teilweise(emptyList(),
            region.hinweise + "Das Arbeitsbudget reicht nicht für ein vollständiges ${region.dimension}D-Raster.",
            zellen = zellen.ifEmpty { listOf(VisualisierungsZelle(bereiche.map {it.minimum},bereiche.map {it.maximum},ZellenStatus.Unbekannt,"Arbeitsbudget ausgeschöpft.")) },
            statistik = VisualisierungsStatistik(auswertungen=zellArbeit,zellArbeit=zellArbeit,budgetErschöpft=true))
        val n = minOf(konfiguriertesN,budgetN)
        val rasterWerte = linkedMapOf<List<Int>, NumerischeMitgliedschaft>()
        val randWerte = linkedMapOf<List<Int>, NumerischeMitgliedschaft>()
        val indices = IntArray(region.dimension)
        fun besuche(tiefe: Int) {
            abbruchPrüfen()
            if (tiefe < region.dimension) {
                for (index in 0 until n) {
                    indices[tiefe] = index
                    besuche(tiefe + 1)
                }
                return
            }
            samplingSpeicher(512L + region.farbProbenAnzahl * 128L)
            if(rasterWerte.size % 256 == 0) samplingZwischenstand {
                VisualisierungsErgebnis.Teilweise(punkte.toList(),listOf("Region wird verfeinert; noch nicht vollständig."),zellen=zellen)
            }
            val koordinaten = indices.mapIndexed { index, rasterIndex ->
                lerp(bereiche[index], rasterIndex.toDouble() / (n - 1))
            }
            region.randResiduum?.invoke(koordinaten)?.takeIf { it.isFinite() }?.let {
                randWerte[indices.toList()] = NumerischeMitgliedschaft.Grenze(it)
            }
            val farbWerte = region.farbMitgliedschaft?.invoke(koordinaten)
            if (farbWerte != null) {
                rasterWerte[indices.toList()] = when {
                    farbWerte.any { it.second == NumerischeMitgliedschaft.Enthalten } -> NumerischeMitgliedschaft.Enthalten
                    farbWerte.any { it.second is NumerischeMitgliedschaft.Grenze } ->
                        farbWerte.first { it.second is NumerischeMitgliedschaft.Grenze }.second
                    farbWerte.any { it.second is NumerischeMitgliedschaft.Unbekannt } ->
                        farbWerte.first { it.second is NumerischeMitgliedschaft.Unbekannt }.second
                    else -> NumerischeMitgliedschaft.NichtEnthalten
                }
                farbWerte.forEach { (farbe, wert) ->
                    when (wert) {
                        NumerischeMitgliedschaft.Enthalten -> punkte += koordinaten.alsPunkt(c).copy(farbwert = farbe)
                        NumerischeMitgliedschaft.NichtEnthalten -> Unit
                        is NumerischeMitgliedschaft.Grenze -> if (abs(wert.residuum) <= c.sampling.toleranz) {
                            punkte += koordinaten.alsPunkt(c).copy(farbwert = farbe)
                        }
                        is NumerischeMitgliedschaft.Unbekannt -> unbekannteGründe += wert.grund
                    }
                }
                return
            }
            val wert = region.mitgliedschaft(koordinaten)
            rasterWerte[indices.toList()] = wert
            when (wert) {
                NumerischeMitgliedschaft.Enthalten -> punkte += koordinaten.alsPunkt(c)
                NumerischeMitgliedschaft.NichtEnthalten -> Unit
                is NumerischeMitgliedschaft.Unbekannt -> unbekannteGründe += wert.grund
                is NumerischeMitgliedschaft.Grenze -> {
                    val schwelle = c.sampling.toleranz
                    // Ein Vorzeichenwechsel wäre ohne Stetigkeitsnachweis kein Nullstellennachweis.
                    if (abs(wert.residuum) <= schwelle) punkte += koordinaten.alsPunkt(c)
                }
            }
        }
        besuche(0)
        val rasterGeometrie = RasterGeometrie.erzeuge(region.dimension, n, bereiche, rasterWerte).let { raster ->
            // Nur ein Vorzeichenwechsel begründet hier eine numerische Gebietsgrenze.
            // Ein identisch nuller oder nur berührender Ausdruck erzeugt keine erfundenen Rasterlinien.
            val residuen=randWerte.values.filterIsInstance<NumerischeMitgliedschaft.Grenze>().map { it.residuum }
            if (residuen.none { it<0 } || residuen.none { it>0 }) raster else raster.copy(linien = raster.linien +
                RasterGeometrie.erzeuge(2, n, bereiche, randWerte).linien.map { it.copy(offenerRand = region.offenerRand) })
        }
        val hatFläche = rasterWerte.values.any { it == NumerischeMitgliedschaft.Enthalten }
        val alleZellen = if(region.dimension == 2 && region.numerischeFläche && (hatFläche || region.zellNachweis == null))
            ergänzeNumerischeFlächen(zellen, n, bereiche, rasterWerte)
            else if(region.dimension == 2) zellen else zellen + rasterGeometrie.zellen
        val zusammengefasst = punkte.groupBy { Triple(it.x, it.y, it.z) }.values.map { gleichePosition ->
            val farben = gleichePosition.mapNotNull { it.farbwert }.distinct().sorted()
            gleichePosition.first().copy(farbwert = farben.firstOrNull(), weitereFarbwerte = farben.drop(1), rasterProbe = region.dimension == 2 && hatFläche && (region.numerischeFläche || zellen.any {it.status == ZellenStatus.Enthalten}))
        }
        val offeneZellen = alleZellen.filter { it.status == ZellenStatus.Unbekannt || it.status == ZellenStatus.Gemischt }
        val statistik = VisualisierungsStatistik(
            auswertungen = zellArbeit + rasterWerte.size * kostenProPunkt,
            zellArbeit = zellArbeit,
            punktArbeit = rasterWerte.size * region.farbProbenAnzahl,
            randArbeit = if(region.randResiduum == null) 0 else rasterWerte.size,
            stützpunkte = zusammengefasst.size,
            linien = rasterGeometrie.linien.size,
            dreiecke = rasterGeometrie.dreiecke.size,
            zellen = alleZellen.size,
            budgetErschöpft = zellen.any { it.grund?.contains("budget", ignoreCase = true) == true } || (n < konfiguriertesN && region.zellNachweis == null),
        )
        if (zusammengefasst.isEmpty() && unbekannteGründe.isEmpty() && offeneZellen.isEmpty() && alleZellen.none { it.status == ZellenStatus.Enthalten || it.status == ZellenStatus.NumerischEnthalten }) {
            return VisualisierungsErgebnis.Erfolgreich(
                emptyList(), true,
                region.hinweise + "Im gewählten Fenster wurden keine Treffer gefunden.",
                VisualisierungsQualität.KeineTrefferImFenster,
                zellen = alleZellen,
                linien = rasterGeometrie.linien,
                dreiecke = rasterGeometrie.dreiecke,
                statistik = statistik,
            )
        }
        val farbDetails = zusammengefasst.filter { it.weitereFarbwerte.isNotEmpty() }.take(4).map { punkt ->
            val werte = (listOfNotNull(punkt.farbwert) + punkt.weitereFarbwerte).distinct().sorted()
            val koordinaten = listOfNotNull(punkt.x, punkt.y, punkt.z).take(region.dimension).joinToString(", ") { kurzeZahl(it) }
            val belegung = if (werte.size <= 6) werte.joinToString(prefix = "{", postfix = "}") { kurzeZahl(it) }
            else "${werte.size} einzelne Rasterwerte mit Wertespanne [${kurzeZahl(werte.first())}, ${kurzeZahl(werte.last())}]"
            "Mehrdeutige Farbe bei ($koordinaten): belegt sind $belegung; die numerische Abtastung ist unvollständig."
        }
        val gemeinsameHinweise = region.hinweise +
            "Arbeitsbudget: ${statistik.auswertungen}/$gesamtBudget (Zellen ${statistik.zellArbeit}, Punkte ${statistik.punktArbeit}, Rand ${statistik.randArbeit})." +
            (if(statistik.budgetErschöpft) listOf("Das Arbeitsbudget ist ausgeschöpft; verbleibende Flächen werden nur numerisch angenähert oder als unbestimmt gekennzeichnet.") else emptyList()) +
            (if (region.fensterBegrenzt) listOf("Die Ergebnisqualität gilt ausschließlich im gewählten Sichtfenster.") else emptyList()) +
            (if (region.dimension == 3) listOf("R³ wird als zertifizierte Zellen und numerische Punktwolke dargestellt.") else emptyList()) +
            unbekannteGründe.take(4).map { "Unbestimmt: $it" } +
            offeneZellen.mapNotNull { it.grund }.distinct().take(4).map { "Unbestimmte Zelle: $it" } +
            farbDetails
        if (unbekannteGründe.isNotEmpty() || offeneZellen.isNotEmpty()) {
            return VisualisierungsErgebnis.Teilweise(
                punkte = zusammengefasst,
                hinweise = gemeinsameHinweise.distinct(),
                zellen = alleZellen,
                linien = rasterGeometrie.linien,
                dreiecke = rasterGeometrie.dreiecke,
                statistik = statistik,
            )
        }
        return VisualisierungsErgebnis.Erfolgreich(
            zusammengefasst,
            istApproximation = true,
            hinweise = gemeinsameHinweise.distinct(),
            zellen = alleZellen,
            linien = rasterGeometrie.linien,
            dreiecke = rasterGeometrie.dreiecke,
            statistik = statistik,
        )
    }

    private fun ergänzeNumerischeFlächen(
        zertifikate: List<VisualisierungsZelle>, n: Int, bereiche: List<ZahlenBereich>,
        werte: Map<List<Int>, NumerischeMitgliedschaft>,
    ): List<VisualisierungsZelle> {
        val schritte = bereiche.take(2).map { (it.maximum-it.minimum)/(n-1) }
        val offene = zertifikate.filter {it.status == ZellenStatus.Unbekannt}
            .ifEmpty { if(zertifikate.isEmpty()) listOf(VisualisierungsZelle(bereiche.map {it.minimum},bereiche.map {it.maximum},ZellenStatus.Unbekannt)) else emptyList() }
        val ergebnis = zertifikate.filterNot {it.status == ZellenStatus.Unbekannt}.toMutableList()
        offene.forEach { z ->
            fun index(a: Int, wert: Double) = floor((wert-bereiche[a].minimum)/schritte[a]).toInt().coerceIn(0,n-2)
            for(i in index(0,z.minimum[0])..index(0,z.maximum[0])) for(j in index(1,z.minimum[1])..index(1,z.maximum[1])) {
                val min = listOf(maxOf(z.minimum[0],bereiche[0].minimum+i*schritte[0]),maxOf(z.minimum[1],bereiche[1].minimum+j*schritte[1]))
                val max = listOf(minOf(z.maximum[0],bereiche[0].minimum+(i+1)*schritte[0]),minOf(z.maximum[1],bereiche[1].minimum+(j+1)*schritte[1]))
                if(min.indices.any {min[it]>=max[it]}) continue
                val proben = listOf(werte[listOf(i,j)],werte[listOf(i+1,j)],werte[listOf(i,j+1)],werte[listOf(i+1,j+1)])
                val status = when {
                    proben.any {it == null || it is NumerischeMitgliedschaft.Unbekannt} -> ZellenStatus.Unbekannt
                    proben.all {it == NumerischeMitgliedschaft.Enthalten} -> ZellenStatus.NumerischEnthalten
                    proben.all {it == NumerischeMitgliedschaft.NichtEnthalten} -> ZellenStatus.Ausgeschlossen
                    else -> z.status
                }
                ergebnis += VisualisierungsZelle(min,max,status,if(status == ZellenStatus.Unbekannt)
                    proben.filterIsInstance<NumerischeMitgliedschaft.Unbekannt>().firstOrNull()?.grund ?: z.grund
                    else "Numerische Näherung aus vier Eckproben; kein Zellbeweis.")
            }
        }
        return ergebnis
    }

    private fun adaptiveZellen(
        region: VisualisierungsDefinition.Region,
        bereiche: List<ZahlenBereich>,
        zielAuflösung: Int,
        budget: AuswertungsBudget,
        abbruchPrüfen: () -> Unit,
    ): List<VisualisierungsZelle> {
        val nachweis = region.zellNachweis ?: return listOf(
            VisualisierungsZelle(
                minimum = bereiche.map { it.minimum },
                maximum = bereiche.map { it.maximum },
                status = ZellenStatus.Unbekannt,
                grund = "Für diese Mengenform ist kein analytischer Zellnachweis verfügbar.",
            ),
        )
        val maximaleTiefe = ceil(ln(zielAuflösung.toDouble()) / ln(2.0)).toInt().coerceAtLeast(0)
        val ergebnis = mutableListOf<VisualisierungsZelle>()
        data class Auftrag(val minimum: List<Double>, val maximum: List<Double>, val tiefe: Int)
        val wartend = java.util.ArrayDeque<Auftrag>()
        wartend.add(Auftrag(bereiche.map { it.minimum }, bereiche.map { it.maximum }, 0))
        // Breitensuche klassifiziert zuerst alle groben Bereiche, bevor Randdetails Budget verbrauchen.
        while (wartend.isNotEmpty()) {
            abbruchPrüfen()
            samplingSpeicher(384)
            val (minimum, maximum, tiefe) = wartend.removeFirst()
            if (!budget.schritt()) {
                ergebnis += VisualisierungsZelle(minimum, maximum, ZellenStatus.Unbekannt, "Zellbudget ausgeschöpft.")
                continue
            }
            val intervalle = minimum.zip(maximum) { a,b -> RationalesIntervall(rationaleKoordinate(a), rationaleKoordinate(b)) }
            val wert = nachweis(intervalle)
            val unterstützt = (wert as? ZellNachweis.Unbekannt)?.grund?.let { "besitzt" !in it && "verfügbar" !in it } ?: true
            val offen = unterstützt && (wert is ZellNachweis.Unbekannt || wert is ZellNachweis.Gemischt)
            if (offen && tiefe < maximaleTiefe && budget.verbraucht + wartend.size + (1 shl region.dimension) <= budget.maximum) {
                samplingSpeicher((1 shl region.dimension)*384L)
                val mitten = minimum.zip(maximum) { a,b -> a+(b-a)/2 }
                repeat(1 shl region.dimension) { maske ->
                    wartend.add(Auftrag(minimum.indices.map { if(maske and (1 shl it)==0) minimum[it] else mitten[it] },
                        maximum.indices.map { if(maske and (1 shl it)==0) mitten[it] else maximum[it] },tiefe+1))
                }
            } else ergebnis += VisualisierungsZelle(minimum,maximum,when(wert) {
                ZellNachweis.Enthalten -> ZellenStatus.Enthalten
                ZellNachweis.Ausgeschlossen -> ZellenStatus.Ausgeschlossen
                is ZellNachweis.Gemischt -> ZellenStatus.Gemischt
                is ZellNachweis.Unbekannt -> ZellenStatus.Unbekannt
            },when(wert) {is ZellNachweis.Gemischt -> wert.grund;is ZellNachweis.Unbekannt -> wert.grund;else -> null})
            if(budget.verbraucht % 256 == 0) samplingZwischenstand {
                VisualisierungsErgebnis.Teilweise(emptyList(),listOf("Zellen werden klassifiziert; offene Bereiche sind unbekannt."),
                    zellen=ergebnis.toList()+wartend.map { VisualisierungsZelle(it.minimum,it.maximum,ZellenStatus.Unbekannt,"Noch nicht klassifiziert.") })
            }
        }
        samplingZwischenstand(true) { VisualisierungsErgebnis.Teilweise(emptyList(),listOf("Numerische Details werden berechnet."),zellen=ergebnis.toList()) }
        return ergebnis
    }

    private fun sampleAbbild(
        abbild: Abbild,
        konfiguration: VisualisierungsKonfiguration,
        abbruchPrüfen: () -> Unit,
    ): VisualisierungsErgebnis {
        val methode = abbild.methode
        if (methode.parameter.any { it !is Variable } || methode.parameter.size > 3 ||
            konfiguration.ausgabeKoordinaten.isNotEmpty() ||
            methode.parameter.size == 1 && numerischeArgumentStruktur(abbild.menge) is NumerischerRaum.Produkt) {
            return sampleStrukturierteBildmenge(abbild, konfiguration, abbruchPrüfen)
        }
        if (methode.parameter.isEmpty()) {
            return VisualisierungsErgebnis.NichtDarstellbar("Die darzustellende Methode benötigt mindestens einen numerischen Parameter.")
        }
        if (methode.parameter.size > 3) {
            return VisualisierungsErgebnis.NichtDarstellbar(
                "Die Methode besitzt ${methode.parameter.size} Parameter. Ohne ausdrücklich konfigurierte Projektion sind höchstens drei Parameter räumlich darstellbar.",
            )
        }
        val parameter = methode.parameter.mapIndexed { index, wert ->
            wert as? Variable ?: return VisualisierungsErgebnis.NichtDarstellbar(
                "Parameter ${index + 1} '${wert.name}' ist kein numerischer Variablenparameter.",
            )
        }
        val modus = when (konfiguration.methodenModus) {
            MethodenDarstellungsModus.Automatisch -> when {
                methode.ausgabeNamen.size == 1 && methode.vorschrift is ZahlAusdruck &&
                    methode.zielMenge == KomplexeZahlen ->
                    MethodenDarstellungsModus.Bild
                methode.ausgabeNamen.size == 1 && methode.vorschrift is ZahlAusdruck && parameter.size <= 2 ->
                    MethodenDarstellungsModus.Funktionsgraph
                methode.ausgabeNamen.size == 1 &&
                    (methode.vorschrift is Tupel || methode.vorschrift is SpaltenVektor || methode.vorschrift is ZeilenVektor) ->
                    MethodenDarstellungsModus.Bild
                methode.ausgabeNamen.size > 1 && konfiguration.achsenNamen.all(methode.ausgabeNamen::contains) ->
                    MethodenDarstellungsModus.Koordinatenausgabe
                else -> return VisualisierungsErgebnis.NichtDarstellbar(
                    "Die Methodensignatur ist nicht eindeutig als Funktionsgraph, Bild oder Koordinatenausgabe erkennbar. Wähle den Darstellungsmodus im Inspector ausdrücklich.",
                )
            }
            else -> konfiguration.methodenModus
        }
        val faktoren = when {
            parameter.size == 1 -> listOf(abbild.menge)
            abbild.menge is KartesischesProdukt && abbild.menge.mengen.size == parameter.size -> abbild.menge.mengen
            abbild.menge is KartesischesProdukt -> return VisualisierungsErgebnis.NichtDarstellbar(
                "Die Parameterdomäne besitzt ${abbild.menge.mengen.size} Faktoren, die Methode aber ${parameter.size} Parameter. Tupelwerte werden nicht implizit in Argumente aufgespalten.",
            )
            else -> return VisualisierungsErgebnis.NichtDarstellbar(
                "Eine ${parameter.size}-stellige Methode benötigt eine kartesische Parameterdomäne mit genau ${parameter.size} Faktoren. Tupelwerte werden nicht implizit in Argumente aufgespalten.",
            )
        }
        val basisAuflösung = when (parameter.size) {
            1 -> konfiguration.sampling.auflösung1D
            2 -> konfiguration.sampling.auflösung2D
            else -> konfiguration.sampling.auflösung3D
        }
        val budgetAuflösung = floor(
            konfiguration.sampling.maximalesRasterBudget.toDouble().pow(1.0 / parameter.size),
        ).toInt().coerceAtLeast(2)
        val parameterAuflösung = min(basisAuflösung, budgetAuflösung).coerceAtLeast(2)
        val domänenKonfiguration = konfiguration.copy(
            sampling = konfiguration.sampling.copy(
                maximalesRasterBudget=if(konfiguration.sampling.automatisch) AUTOMATISCHE_MATERIALISIERUNGS_GRENZE else konfiguration.sampling.maximalesRasterBudget,
                auflösung1D = parameterAuflösung,
                auflösung2D = parameterAuflösung,
                auflösung3D = parameterAuflösung,
            ),
        )
        val sicht = konfiguration.sampling.sichtBereiche
        val bereiche = if (modus == MethodenDarstellungsModus.Funktionsgraph && sicht != null)
            listOf(sicht.x, sicht.y, sicht.z).filterNotNull() else konfiguration.achsenBereiche
        val domänen = faktoren.mapIndexed { index, faktor ->
            val bereich = bereiche.getOrNull(index)
                ?: return VisualisierungsErgebnis.NichtDarstellbar("Für Parameter ${index + 1} fehlt ein Inspectorbereich.")
            when (val ergebnis = faktorDomäne(
                faktor,
                bereich,
                domänenKonfiguration,
                beschränkeEndlichesIntervallAufSichtfenster = modus == MethodenDarstellungsModus.Funktionsgraph,
            )) {
                is DomänenErgebnis.Erfolgreich -> ergebnis.domäne
                is DomänenErgebnis.Fehler -> return if (ergebnis.offen) VisualisierungsErgebnis.BedingtDarstellbar(ergebnis.grund)
                    else VisualisierungsErgebnis.NichtDarstellbar(
                    "Parameter '${parameter[index].name}' ist nicht darstellbar: ${ergebnis.grund}",
                )
            }
        }
        if (domänen.any { it.werte.isEmpty() }) {
            val mathematischLeer = domänen.any { it.mathematischLeer }
            return VisualisierungsErgebnis.Erfolgreich(
                emptyList(),
                false,
                domänen.flatMap { it.hinweise }.distinct() +
                    if (mathematischLeer) "Mindestens eine Parameterdomäne ist mathematisch leer."
                    else "Mindestens eine Parameterdomäne hat im Sichtfenster keine Treffer.",
                if (mathematischLeer) VisualisierungsQualität.MathematischLeer
                else VisualisierungsQualität.KeineTrefferImFenster,
            )
        }
        fun werteKoordinaten(argumente: List<Double>): KoordinatenErgebnis {
            val umgebung = parameter.map { it.name }.zip(argumente).toMap()
            return when (modus) {
                MethodenDarstellungsModus.Funktionsgraph -> funktionsgraphKoordinaten(methode, argumente, umgebung, konfiguration)
                MethodenDarstellungsModus.Bild -> {
                    if (methode.ausgabeNamen.size != 1) {
                        KoordinatenErgebnis.NichtDarstellbar("Der Bildmodus benötigt genau eine zusammengesetzte Methodenausgabe")
                    } else KoordinatenAdapter.extrahiere(
                        methode.vorschrift,
                        konfiguration.raumDimension,
                        umgebung,
                    )
                }
                MethodenDarstellungsModus.Koordinatenausgabe -> koordinatenausgabe(methode, umgebung, konfiguration)
                MethodenDarstellungsModus.Automatisch -> error("Der automatische Methodenmodus muss vor dem Sampling aufgelöst sein.")
            }
        }
        val kontinuierlich = parameter.size in 1..2 && domänen.all { it.istApproximation && it.werte.size >= 2 }
        if (kontinuierlich) {
            val parameterBereiche = domänen.map { domäne -> ZahlenBereich(domäne.werte.min(), domäne.werte.max()) }
            val adaptiv = AdaptiveMethodenGeometrie.sample(
                parameterBereiche,
                konfiguration,
                auswerten = { argumente ->
                    val umgebung = parameter.map { it.name }.zip(argumente).toMap()
                    when (val koordinaten = werteKoordinaten(argumente)) {
                        is KoordinatenErgebnis.Darstellbar -> AdaptivePunktAuswertung(
                            koordinaten.werte.alsPunkt(konfiguration, umgebung),
                        )
                        else -> AdaptivePunktAuswertung(null, koordinaten.beschreibung)
                    }
                },
                abbruchPrüfen = abbruchPrüfen,
                herkunft=abbild,
            )
            val abtastung = if (parameter.size == 2 && konfiguration.sampling.vorschau) "gleichmäßige Punktvorschau" else "adaptive Bildraumabtastung"
            val hinweise = domänen.flatMap { it.hinweise }.distinct() +
                "Methodenmodus: ${modus.name}; $abtastung mit ${adaptiv.statistik.auswertungen} Auswertungen."
            val budgetHinweis = if (adaptiv.statistik.budgetErschöpft) {
                listOf("Das Auswertungsbudget wurde vor Erreichen der Zielauflösung ausgeschöpft.")
            } else emptyList()
            if (adaptiv.punkte.isEmpty()) {
                return VisualisierungsErgebnis.NichtDarstellbar(
                    "Die Methode erzeugt keine zusammenhängend darstellbaren Werte. " +
                        (adaptiv.diagnosen + budgetHinweis).distinct().joinToString(" "),
                )
            }
            val alleHinweise = (hinweise + adaptiv.diagnosen.map { "Nicht dargestellt: $it" } + budgetHinweis).distinct()
            return if (adaptiv.diagnosen.isNotEmpty() || adaptiv.statistik.budgetErschöpft) {
                VisualisierungsErgebnis.Teilweise(
                    punkte = adaptiv.punkte,
                    hinweise = alleHinweise,
                    linien = adaptiv.linien,
                    dreiecke = adaptiv.dreiecke,
                    statistik = adaptiv.statistik,
                    interaktionsDreiecke=adaptiv.interaktionsDreiecke,
                )
            } else {
                VisualisierungsErgebnis.Erfolgreich(
                    punkte = adaptiv.punkte,
                    istApproximation = true,
                    hinweise = alleHinweise,
                    linien = adaptiv.linien,
                    dreiecke = adaptiv.dreiecke,
                    statistik = adaptiv.statistik,
                    interaktionsDreiecke=adaptiv.interaktionsDreiecke,
                )
            }
        }
        val erwartetePunkte = domänen.fold(1L) { akk, domäne ->
            if (akk > Long.MAX_VALUE / domäne.werte.size) Long.MAX_VALUE else akk * domäne.werte.size
        }
        if (!konfiguration.sampling.automatisch && erwartetePunkte > konfiguration.sampling.maximalesRasterBudget) {
            return VisualisierungsErgebnis.NichtDarstellbar(
                "Das Methodensampling würde $erwartetePunkte Parameterkombinationen materialisieren und überschreitet das Gesamtbudget von ${konfiguration.sampling.maximalesRasterBudget}.",
            )
        }
        val kombinationen=kartesischeStützwerte(domänen)
        val punkte = mutableListOf<VisualisierungsPunkt>()
        val diagnosen = mutableListOf<KoordinatenErgebnis>()
        kombinationen.forEach { argumente ->
            abbruchPrüfen()
            samplingSpeicher(256)
            if(punkte.size%256==0) samplingZwischenstand {
                VisualisierungsErgebnis.Teilweise(punkte.toList(),listOf("Methodenargumente werden ausgewertet; noch nicht vollständig."))
            }
            val umgebung = parameter.map { it.name }.zip(argumente).toMap()
            val koordinaten = werteKoordinaten(argumente)
            when (koordinaten) {
                is KoordinatenErgebnis.Darstellbar -> punkte += koordinaten.werte.alsPunkt(konfiguration, umgebung)
                is KoordinatenErgebnis.BedingtDarstellbar,
                is KoordinatenErgebnis.ProjektionErforderlich,
                is KoordinatenErgebnis.NichtDarstellbar -> diagnosen += koordinaten
            }
        }
        val domänenHinweise = domänen.flatMap { it.hinweise }.distinct() +
            "Methodenmodus: ${modus.name}; ${parameter.size} Parameter; $erwartetePunkte Kombinationen."
        val fehlerHinweise = diagnosen.groupingBy { it.beschreibung }.eachCount()
            .map { (grund, anzahl) -> "$anzahl × $grund" }
        return when {
            punkte.isEmpty() -> {
                val projektionen = diagnosen.filterIsInstance<KoordinatenErgebnis.ProjektionErforderlich>()
                val bedingte = diagnosen.filterIsInstance<KoordinatenErgebnis.BedingtDarstellbar>()
                val nichtDarstellbare = diagnosen.filterIsInstance<KoordinatenErgebnis.NichtDarstellbar>()
                when {
                    projektionen.isNotEmpty() && bedingte.isEmpty() && nichtDarstellbare.isEmpty() -> {
                        val erste = projektionen.first()
                        VisualisierungsErgebnis.ProjektionErforderlich(
                            erste.vorhandeneDimension,
                            erste.erwarteteDimension,
                            "Die Methode erzeugt nur projektionsbedürftige Werte. ${fehlerHinweise.joinToString(" ")}",
                        )
                    }
                    bedingte.isNotEmpty() && projektionen.isEmpty() && nichtDarstellbare.isEmpty() -> VisualisierungsErgebnis.BedingtDarstellbar(
                        "Die Methode ist nur bedingt darstellbar. ${fehlerHinweise.joinToString(" ")}",
                        bedingte.flatMap { it.bedingungen }.distinct(),
                    )
                    else -> VisualisierungsErgebnis.NichtDarstellbar(
                        "Die Methode erzeugt keine darstellbaren Werte. ${fehlerHinweise.joinToString(" ")}",
                    )
                }
            }
            diagnosen.isNotEmpty() -> VisualisierungsErgebnis.Teilweise(punkte, domänenHinweise + fehlerHinweise)
            else -> VisualisierungsErgebnis.Erfolgreich(
                punkte,
                domänen.any { it.istApproximation },
                domänenHinweise,
                statistik = VisualisierungsStatistik(
                    auswertungen = erwartetePunkte.toInt(),
                    stützpunkte = punkte.size,
                ),
            )
        }
    }

    private fun funktionsgraphKoordinaten(
        methode: Methode,
        argumente: List<Double>,
        umgebung: Map<String, Double>,
        konfiguration: VisualisierungsKonfiguration,
    ): KoordinatenErgebnis {
        val vorschrift = methode.vorschrift as? ZahlAusdruck
        if (methode.ausgabeNamen.size != 1 || vorschrift == null) {
            return KoordinatenErgebnis.NichtDarstellbar("Der Funktionsgraphmodus benötigt genau eine skalare Ausgabe")
        }
        val erwarteteDimension = argumente.size + 1
        if (argumente.size !in 1..2 || konfiguration.raumDimension != erwarteteDimension) {
            return KoordinatenErgebnis.NichtDarstellbar(
                "Ein Funktionsgraph mit ${argumente.size} Parametern benötigt R$erwarteteDimension",
            )
        }
        return when (val wert = KoordinatenAdapter.extrahiere(vorschrift, 1, umgebung)) {
            is KoordinatenErgebnis.Darstellbar -> KoordinatenErgebnis.Darstellbar(
                argumente.mapIndexed { index, argument ->
                    ReelleKoordinatenKomponente(
                        index,
                        methode.parameter[index] as Variable,
                        argument,
                    )
                } + wert.komponenten.map { it.copy(index = it.index + argumente.size) },
            )
            is KoordinatenErgebnis.ProjektionErforderlich -> wert.copy(
                vorhandeneDimension = argumente.size + wert.vorhandeneDimension,
                erwarteteDimension = erwarteteDimension,
            )
            is KoordinatenErgebnis.NichtDarstellbar -> wert.copy(
                grund = "Funktionswert ist nicht numerisch definiert: ${wert.grund}",
            )
            else -> wert
        }
    }

    private fun koordinatenausgabe(
        methode: Methode,
        umgebung: Map<String, Double>,
        konfiguration: VisualisierungsKonfiguration,
    ): KoordinatenErgebnis {
        if (methode.ausgabeNamen.size == 1) {
            return KoordinatenAdapter.extrahiere(methode.vorschrift, konfiguration.raumDimension, umgebung)
        }
        val achsen = konfiguration.achsenNamen
        val fehlend = achsen.filterNot(methode.ausgabeNamen::contains)
        if (fehlend.isNotEmpty()) {
            return KoordinatenErgebnis.NichtDarstellbar(
                "Für die Achsen ${fehlend.joinToString()} fehlen gleichnamige Methodenausgaben",
            )
        }
        return KoordinatenAdapter.extrahiere(
            Tupel(achsen.map(methode::vorschriftFür)),
            konfiguration.raumDimension,
            umgebung,
        )
    }

    private fun mitgliedschaft(
        menge: MengenAusdruck,
        punkt: List<Double>,
        c: VisualisierungsKonfiguration,
    ): NumerischeMitgliedschaft {
        val raum = if (menge is GefilterteMenge) null else numerischeArgumentStruktur(menge)
        if (raum is NumerischerRaum.Blatt && punkt.size == 1)
            return faktorEnthält(menge, punkt.single(), c.sampling.toleranz)
        if (raum is NumerischerRaum.Produkt && raum.blätter.size == punkt.size) {
            val objekt = raum.packe(punkt.map(::rationaleZahl).iterator())
            val bedingung = if (numerischerRaum(menge) == null) ElementBeziehung(objekt, menge) else raum.bedingung(objekt)
            val entscheidung = werteAussage(bedingung, emptyMap(), c.sampling.toleranz)
            if (entscheidung !is NumerischeMitgliedschaft.Unbekannt || menge !is MengenDifferenz && menge !is Schnitt) return entscheidung
        }
        return when (menge) {
        LeereMenge -> NumerischeMitgliedschaft.NichtEnthalten
        ReelleZahlen, GanzeZahlen, NatürlicheZahlen, RationaleZahlen, is ReellesIntervall -> {
            if (punkt.size == 1) faktorEnthält(menge, punkt.single(), c.sampling.toleranz)
            else NumerischeMitgliedschaft.Unbekannt("Eine eindimensionale Zahlenmenge benötigt genau eine Koordinate.")
        }
        KomplexeZahlen -> if (c.dimension == RaumDimension.C && punkt.size == 2) {
            NumerischeMitgliedschaft.Enthalten
        } else NumerischeMitgliedschaft.Unbekannt("ℂ benötigt zwei Koordinaten im komplexen Darstellungsraum.")
        is EndlicheMenge -> when (ElementBeziehung(punktObjekt(punkt, c), menge).entscheide().wahrheitswert) {
            Wahrheitswert.Wahr -> NumerischeMitgliedschaft.Enthalten
            Wahrheitswert.Lüge -> NumerischeMitgliedschaft.NichtEnthalten
            null -> NumerischeMitgliedschaft.Unbekannt("Die Gleichheit mit einem Element der endlichen Menge ist nicht entscheidbar.")
        }
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
        is Schnitt -> if (menge.mengen.isEmpty()) {
            menge.grundMenge?.let { mitgliedschaft(it, punkt, c) }
                ?: NumerischeMitgliedschaft.Unbekannt("Ein leerer Schnitt benötigt eine Grundmenge.")
        } else kombiniereMitgliedschaften(menge.mengen.map { mitgliedschaft(it, punkt, c) }, und = true)
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

    }

    private fun werteFilter(
        menge: GefilterteMenge, punkt: List<Double>, c: VisualisierungsKonfiguration,
    ): NumerischeMitgliedschaft { return try {
        val methode = menge.methode.alsMathematischeMethode("die Filtermitgliedschaft")
        if (externeFilterParameter(methode).isNotEmpty()) return NumerischeMitgliedschaft.Unbekannt("Der Filter enthält ungebundene externe Parameter.")
        val raum = numerischeArgumentStruktur(menge.menge)
        val objekt = if (raum != null && raum.blätter.size == punkt.size) raum.packe(punkt.map(::rationaleZahl).iterator()) else punktObjekt(punkt, c)
        val argumente = Tupel(listOf(objekt))
        val env = c.achsenNamen.zip(punkt).toMap() + c.festeSchnitte
        val voraussetzungen = kombiniereMitgliedschaften(methode.prüfeDefinitionsBedingungen(argumente).map {
            werteAussage(filterBedingung(it), env, c.sampling.toleranz)
        }, und = true)
        if (voraussetzungen != NumerischeMitgliedschaft.Enthalten) return voraussetzungen
        val anwendung = methode.wendeMitBedingungenAn(argumente)
        val prädikat = anwendung.ergebnis.elemente.single().alsPrädikatsAussageOderNull()
            ?: error("Der Filter liefert keine Aussage.")
        kombiniereMitgliedschaften((anwendung.bedingungen + prädikat).map {
            werteAussage(it, c.achsenNamen.zip(punkt).toMap() + c.festeSchnitte, c.sampling.toleranz)
        }, und = true)
    } catch (abbruch: java.util.concurrent.CancellationException) { throw abbruch
    } catch (fehler: IllegalArgumentException) {
        NumerischeMitgliedschaft.Unbekannt("Filtervoraussetzung: ${fehler.message}")
    } catch (fehler: IllegalStateException) {
        NumerischeMitgliedschaft.Unbekannt("Filtermethode: ${fehler.message ?: "nicht auswertbar"}")
    }

    }

    private fun wertePrädikatsMenge(
        menge: PrädikatsMenge,
        punkt: List<Double>,
        c: VisualisierungsKonfiguration,
    ): NumerischeMitgliedschaft = runCatching {
        val gebunden = ersetze(menge.bedingung, mapOf(menge.element.name to punktObjekt(punkt, c)))
        werteAussage(gebunden, c.achsenNamen.zip(punkt).toMap() + c.festeSchnitte, c.sampling.toleranz)
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
    ): List<List<Double>>? {
        val normalisiert = normalisiereEndlicheAlgebra(
            menge,
            konfiguration.sampling.maximalesRasterBudget,
        ) ?: return null
        return exaktePunkte(normalisiert, konfiguration.raumDimension)
    }

    /**
     * Schätzt den tatsächlich vom vorhandenen endlichen CAS-Pfad ausgeführten
     * Aufwand, bevor dieser Mengen oder quadratische Gleichheitsprüfungen
     * materialisiert. `null` bedeutet, dass der Ausdruck keine rein endliche
     * Algebra ist und daher von einem anderen Normalisierer behandelt wird.
     */
    private fun schätzeEndlicheAlgebra(menge: MengenAusdruck): EndlicheAlgebraSchätzung? = when (menge) {
        LeereMenge -> EndlicheAlgebraSchätzung(BigInteger.ZERO, BigInteger.ZERO)
        is EndlicheMenge -> BigInteger.valueOf(menge.elemente.size.toLong()).let {
            EndlicheAlgebraSchätzung(it, BigInteger.ZERO)
        }
        is Vereinigung -> {
            val teile = menge.mengen.map { schätzeEndlicheAlgebra(it) ?: return null }
            val kardinalität = teile.fold(BigInteger.ZERO) { summe, teil -> summe + teil.kardinalität }
            EndlicheAlgebraSchätzung(kardinalität, teile.summeArbeit() + kardinalität)
        }
        is Schnitt -> if (menge.mengen.isEmpty()) {
            menge.grundMenge?.let(::schätzeEndlicheAlgebra)
        } else {
            val teile = menge.mengen.map { schätzeEndlicheAlgebra(it) ?: return null }
            val erster = teile.first().kardinalität
            val rest = teile.drop(1).fold(BigInteger.ZERO) { summe, teil -> summe + teil.kardinalität }
            EndlicheAlgebraSchätzung(
                teile.minOf { it.kardinalität },
                teile.summeArbeit() + erster.multiply(rest),
            )
        }
        is MengenDifferenz -> {
            val links = schätzeEndlicheAlgebra(menge.links) ?: return null
            val rechts = schätzeEndlicheAlgebra(menge.rechts) ?: return null
            EndlicheAlgebraSchätzung(
                links.kardinalität,
                links.arbeit + rechts.arbeit + links.kardinalität.multiply(rechts.kardinalität),
            )
        }
        is SymmetrischeDifferenz -> {
            val links = schätzeEndlicheAlgebra(menge.links) ?: return null
            val rechts = schätzeEndlicheAlgebra(menge.rechts) ?: return null
            EndlicheAlgebraSchätzung(
                links.kardinalität + rechts.kardinalität,
                links.arbeit + rechts.arbeit +
                    links.kardinalität.multiply(rechts.kardinalität).multiply(BigInteger.TWO),
            )
        }
        is KartesischesProdukt -> {
            val faktoren = menge.mengen.map { schätzeEndlicheAlgebra(it) ?: return null }
            val kardinalität = faktoren.fold(BigInteger.ONE) { produkt, faktor ->
                produkt.multiply(faktor.kardinalität)
            }
            EndlicheAlgebraSchätzung(kardinalität, faktoren.summeArbeit() + kardinalität)
        }
        is GefilterteMenge -> {
            val basis = schätzeEndlicheAlgebra(menge.menge) ?: return null
            EndlicheAlgebraSchätzung(basis.kardinalität, basis.arbeit + basis.kardinalität)
        }
        is MengenFallAusdruck -> when (menge.aussage.entscheide(RechenKontext()).wahrheitswert) {
            Wahrheitswert.Wahr -> schätzeEndlicheAlgebra(menge.wahr)
            Wahrheitswert.Lüge -> schätzeEndlicheAlgebra(menge.lüge)
            null -> null
        }
        else -> null
    }

    private fun List<EndlicheAlgebraSchätzung>.summeArbeit(): BigInteger =
        fold(BigInteger.ZERO) { summe, teil -> summe + teil.arbeit }

    /** Führt Mengenalgebra vor jeder Koordinatenprojektion mit CAS-Entscheidungen aus. */
    private fun normalisiereEndlicheAlgebra(
        menge: MengenAusdruck,
        maximalesRasterBudget: Int,
    ): MengenAusdruck? = when (menge) {
        LeereMenge -> menge
        is EndlicheMenge -> menge.takeIf { it.elemente.size <= maximalesRasterBudget }
        is Vereinigung -> {
            val teile = menge.mengen.map { normalisiereEndlicheAlgebra(it, maximalesRasterBudget) ?: return null }
            if (endlicheObergrenze(teile) > BigInteger.valueOf(maximalesRasterBudget.toLong())) return null
            vereinige(teile).innerhalbEndlichemBudget(maximalesRasterBudget)
        }
        is Schnitt -> if (menge.mengen.isEmpty()) {
            menge.grundMenge?.let { normalisiereEndlicheAlgebra(it, maximalesRasterBudget) }
        } else {
            val teile = menge.mengen.map { normalisiereEndlicheAlgebra(it, maximalesRasterBudget) ?: return null }
            schneide(teile, menge.grundMenge).innerhalbEndlichemBudget(maximalesRasterBudget)
        }
        is MengenDifferenz -> {
            val links = normalisiereEndlicheAlgebra(menge.links, maximalesRasterBudget) ?: return null
            val rechts = normalisiereEndlicheAlgebra(menge.rechts, maximalesRasterBudget) ?: return null
            mengenDifferenz(links, rechts).innerhalbEndlichemBudget(maximalesRasterBudget)
        }
        is SymmetrischeDifferenz -> {
            val links = normalisiereEndlicheAlgebra(menge.links, maximalesRasterBudget) ?: return null
            val rechts = normalisiereEndlicheAlgebra(menge.rechts, maximalesRasterBudget) ?: return null
            if (endlicheObergrenze(listOf(links, rechts)) > BigInteger.valueOf(maximalesRasterBudget.toLong())) return null
            symmetrischeDifferenz(links, rechts).innerhalbEndlichemBudget(maximalesRasterBudget)
        }
        is KartesischesProdukt -> {
            val faktoren = menge.mengen.map { normalisiereEndlicheAlgebra(it, maximalesRasterBudget) ?: return null }
            if (faktoren.any { it == LeereMenge }) return LeereMenge
            val größe = faktoren.fold(BigInteger.ONE) { akk, faktor ->
                akk.multiply(BigInteger.valueOf((faktor as EndlicheMenge).elemente.size.toLong()))
            }
            if (größe > BigInteger.valueOf(maximalesRasterBudget.toLong())) return null
            kartesischesProdukt(faktoren).innerhalbEndlichemBudget(maximalesRasterBudget)
        }
        is GefilterteMenge -> {
            val basis = normalisiereEndlicheAlgebra(menge.menge, maximalesRasterBudget) ?: return null
            filtereMenge(basis, menge.methode).innerhalbEndlichemBudget(maximalesRasterBudget)
        }
        is MengenFallAusdruck -> when (menge.aussage.entscheide(RechenKontext()).wahrheitswert) {
            Wahrheitswert.Wahr -> normalisiereEndlicheAlgebra(menge.wahr, maximalesRasterBudget)
            Wahrheitswert.Lüge -> normalisiereEndlicheAlgebra(menge.lüge, maximalesRasterBudget)
            null -> null
        }
        else -> null
    }

    private fun MengenAusdruck.innerhalbEndlichemBudget(maximalesRasterBudget: Int): MengenAusdruck? = when (this) {
        LeereMenge -> this
        is EndlicheMenge -> takeIf { elemente.size <= maximalesRasterBudget }
        else -> null
    }

    private fun endlicheObergrenze(mengen: List<MengenAusdruck>): BigInteger = mengen.fold(BigInteger.ZERO) { summe, menge ->
        summe + BigInteger.valueOf((menge as? EndlicheMenge)?.elemente?.size?.toLong() ?: 0L)
    }

    private fun exaktePunkte(menge: MengenAusdruck, dimension: Int): List<List<Double>>? = when (menge) {
        LeereMenge -> emptyList()
        is EndlicheMenge -> normalisiereEndlicheMenge(menge, dimension).takeIf { it.diagnosen.isEmpty() }?.punkte
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
        else -> null
    }

    private sealed interface DomänenErgebnis {
        data class Erfolgreich(val domäne: NumerischeDomäne) : DomänenErgebnis
        data class Fehler(val grund: String, val offen: Boolean = false) : DomänenErgebnis
    }

    private fun faktorDomäne(
        faktor: MengenAusdruck,
        bereich: ZahlenBereich,
        c: VisualisierungsKonfiguration,
        beschränkeEndlichesIntervallAufSichtfenster: Boolean = true,
    ): DomänenErgebnis {
        try {
            diskreteAchsenWerte(faktor, bereich.minimum, bereich.maximum, c.sampling.maximalesRasterBudget)?.let { werte ->
                return DomänenErgebnis.Erfolgreich(NumerischeDomäne(werte.map { numerischerWert(it as ZahlAusdruck, emptyMap())!! }, false,
                    if (faktor is EndlicheMenge) emptyList() else listOf("Die diskrete Achse wird im Sichtfenster vollständig aufgezählt."),
                    mathematischLeer = faktor is EndlicheMenge && faktor.elemente.isEmpty()))
            }
        } catch (fehler: OffeneRaumBedingung) { return DomänenErgebnis.Fehler(fehler.message ?: "Offene Mitgliedschaft.", offen = true)
        } catch (fehler: IllegalArgumentException) { return DomänenErgebnis.Fehler(fehler.message ?: "Ungültige diskrete Achse.")
        } catch (fehler: IllegalStateException) { return DomänenErgebnis.Fehler(fehler.message ?: "Offene diskrete Mitgliedschaft.") }
        return when (faktor) {
        LeereMenge -> DomänenErgebnis.Erfolgreich(NumerischeDomäne(emptyList(), false, mathematischLeer = true))
        is EndlicheMenge -> {
            val werte = faktor.elemente.map { element ->
                val zahl = element as? ZahlAusdruck ?: return DomänenErgebnis.Fehler("Die endliche Faktor-Menge enthält ein nichtskalares Element.")
                val wert = numerischerWert(zahl, emptyMap()) ?: return DomänenErgebnis.Fehler("Ein Faktorwert ist nicht numerisch auswertbar.")
                if (!wert.isFinite()) return DomänenErgebnis.Fehler("Ein Faktorwert ist nicht endlich darstellbar.")
                wert
            }.distinct().sorted()
            DomänenErgebnis.Erfolgreich(NumerischeDomäne(werte, false, mathematischLeer = faktor.elemente.isEmpty()))
        }
        is ReellesIntervall -> {
            val links = numerischerWert(faktor.links, emptyMap())
                ?: return DomänenErgebnis.Fehler("Die linke Intervallgrenze ist nicht numerisch auswertbar.")
            val rechts = numerischerWert(faktor.rechts, emptyMap())
                ?: return DomänenErgebnis.Fehler("Die rechte Intervallgrenze ist nicht numerisch auswertbar.")
            if (!links.isFinite() || !rechts.isFinite()) {
                return DomänenErgebnis.Fehler("Die Intervallgrenzen sind nicht endlich numerisch darstellbar.")
            }
            val mathematischLeer = links > rechts || links == rechts && (faktor.linksOffen || faktor.rechtsOffen)
            if (mathematischLeer) {
                return DomänenErgebnis.Erfolgreich(NumerischeDomäne(emptyList(), false, mathematischLeer = true))
            }
            val sichtbarLinks = if (beschränkeEndlichesIntervallAufSichtfenster) maxOf(links, bereich.minimum) else links
            val sichtbarRechts = if (beschränkeEndlichesIntervallAufSichtfenster) minOf(rechts, bereich.maximum) else rechts
            if (sichtbarLinks > sichtbarRechts) {
                return DomänenErgebnis.Erfolgreich(
                    NumerischeDomäne(emptyList(), true, listOf("Das Intervall hat im sichtbaren Achsenbereich keine Treffer.")),
                )
            }
            val anzahl = c.achsenAuflösung
            val werte = (if (sichtbarLinks == sichtbarRechts) listOf(sichtbarLinks) else {
                List(anzahl) { index ->
                    sichtbarLinks + (sichtbarRechts - sichtbarLinks) * index.toDouble() / (anzahl - 1)
                }
            })
                .filter { wert ->
                    (!faktor.linksOffen || wert > links) &&
                        (!faktor.rechtsOffen || wert < rechts)
                }
            if (werte.any { !it.isFinite() }) return DomänenErgebnis.Fehler("Das Intervallraster enthält nichtendliche Koordinaten.")
            DomänenErgebnis.Erfolgreich(
                NumerischeDomäne(
                    werte,
                    true,
                    listOf(
                        if (beschränkeEndlichesIntervallAufSichtfenster) {
                            "Der sichtbare Teil eines kontinuierlichen Produktfaktors wird mit ${werte.size} Werten angenähert."
                        } else {
                            "Das endliche Parameterintervall wird mit ${werte.size} Werten angenähert."
                        },
                    ),
                ),
            )
        }
        ReelleZahlen, RationaleZahlen -> DomänenErgebnis.Erfolgreich(
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

    }

    private fun ganzzahlDomäne(
        bereich: ZahlenBereich,
        natürliche: Boolean,
        c: VisualisierungsKonfiguration,
    ): DomänenErgebnis {
        var start = BigDecimal.valueOf(bereich.minimum).setScale(0, RoundingMode.CEILING).toBigIntegerExact()
        if (natürliche && start < BigInteger.ONE) start = BigInteger.ONE
        val ende = BigDecimal.valueOf(bereich.maximum).setScale(0, RoundingMode.FLOOR).toBigIntegerExact()
        if (ende < start) return DomänenErgebnis.Erfolgreich(NumerischeDomäne(emptyList(), false))
        val anzahl = ende
            .subtract(start)
            .add(BigInteger.ONE)
        val budget = BigInteger.valueOf(c.sampling.maximalesRasterBudget.toLong())
        if (anzahl > budget) {
            return DomänenErgebnis.Fehler("Der sichtbare ganzzahlige Faktor enthält $anzahl Werte und überschreitet das Rasterbudget.")
        }
        return DomänenErgebnis.Erfolgreich(
            NumerischeDomäne(
                List(anzahl.toInt()) { index -> start.add(BigInteger.valueOf(index.toLong())).toDouble() },
                false,
            ),
        )
    }

    private fun faktorEnthält(
        faktor: MengenAusdruck,
        wert: Double,
        toleranz: Double,
    ): NumerischeMitgliedschaft = when (faktor) {
        LeereMenge -> NumerischeMitgliedschaft.NichtEnthalten
        ReelleZahlen -> NumerischeMitgliedschaft.Enthalten
        RationaleZahlen -> if (wert.isFinite()) NumerischeMitgliedschaft.Enthalten else NumerischeMitgliedschaft.NichtEnthalten
        GanzeZahlen -> if (wert.isFinite() && wert == round(wert)) NumerischeMitgliedschaft.Enthalten else NumerischeMitgliedschaft.NichtEnthalten
        NatürlicheZahlen -> if (wert.isFinite() && wert >= 1.0 && wert == round(wert)) NumerischeMitgliedschaft.Enthalten else NumerischeMitgliedschaft.NichtEnthalten
        NichtnegativeGanzeZahlenSemantik.menge, Primzahlen -> when (ElementBeziehung(rationaleZahl(wert), faktor).entscheide().wahrheitswert) {
            Wahrheitswert.Wahr -> NumerischeMitgliedschaft.Enthalten
            Wahrheitswert.Lüge -> NumerischeMitgliedschaft.NichtEnthalten
            null -> NumerischeMitgliedschaft.Unbekannt("Die Mitgliedschaft ist noch nicht bewiesen.")
        }
        is ReellesIntervall -> {
            val links = numerischerWert(faktor.links, emptyMap()) ?: return NumerischeMitgliedschaft.Unbekannt("Intervallgrenze nicht numerisch.")
            val rechts = numerischerWert(faktor.rechts, emptyMap()) ?: return NumerischeMitgliedschaft.Unbekannt("Intervallgrenze nicht numerisch.")
            val linksOk = if (faktor.linksOffen) wert > links else wert >= links
            val rechtsOk = if (faktor.rechtsOffen) wert < rechts else wert <= rechts
            if (linksOk && rechtsOk) NumerischeMitgliedschaft.Enthalten else NumerischeMitgliedschaft.NichtEnthalten
        }
        is EndlicheMenge -> {
            when (ElementBeziehung(rationaleZahl(wert), faktor).entscheide().wahrheitswert) {
                Wahrheitswert.Wahr -> NumerischeMitgliedschaft.Enthalten
                Wahrheitswert.Lüge -> NumerischeMitgliedschaft.NichtEnthalten
                null -> NumerischeMitgliedschaft.Unbekannt("Die Gleichheit mit einem endlichen Faktorwert ist nicht entscheidbar.")
            }
        }
        is Vereinigung -> kombiniereMitgliedschaften(faktor.mengen.map { faktorEnthält(it, wert, toleranz) }, und = false)
        is Schnitt -> if (faktor.mengen.isEmpty()) {
            faktor.grundMenge?.let { faktorEnthält(it, wert, toleranz) }
                ?: NumerischeMitgliedschaft.Unbekannt("Ein leerer Schnitt benötigt eine Grundmenge.")
        } else kombiniereMitgliedschaften(faktor.mengen.map { faktorEnthält(it, wert, toleranz) }, und = true)
        is MengenDifferenz -> differenz(faktorEnthält(faktor.links, wert, toleranz), faktorEnthält(faktor.rechts, wert, toleranz))
        is SymmetrischeDifferenz -> exklusivOder(faktorEnthält(faktor.links, wert, toleranz), faktorEnthält(faktor.rechts, wert, toleranz))
        else -> NumerischeMitgliedschaft.Unbekannt("${faktor::class.simpleName} ist keine unterstützte Faktor-Menge.")
    }

    private fun werteAussage(
        aussage: Aussage,
        umgebung: Map<String, Double>,
        toleranz: Double,
    ): NumerischeMitgliedschaft {
        if (aussage is Negation) return differenz(NumerischeMitgliedschaft.Enthalten, werteAussage(aussage.aussage, umgebung, toleranz))
        if (aussage is Konjunktion) return kombiniereMitgliedschaften(aussage.aussagen.map { werteAussage(it, umgebung, toleranz) }, und = true)
        if (aussage is Disjunktion) return kombiniereMitgliedschaften(aussage.aussagen.map { werteAussage(it, umgebung, toleranz) }, und = false)
        if (aussage is Gleichheit && aussage.links is ZahlAusdruck && aussage.rechts is ZahlAusdruck) {
            val links = numerischerWert(aussage.links as ZahlAusdruck, umgebung)
            val rechts = numerischerWert(aussage.rechts as ZahlAusdruck, umgebung)
            if (links != null && rechts != null) return NumerischeMitgliedschaft.Grenze(links - rechts)
        }
        if (aussage is ElementBeziehung) {
            val gebunden = ersetze(aussage, umgebung.mapValues { rationaleZahl(it.value) }) as Aussage
            when (gebunden.entscheide().wahrheitswert) {
                Wahrheitswert.Wahr -> return NumerischeMitgliedschaft.Enthalten
                Wahrheitswert.Lüge -> return NumerischeMitgliedschaft.NichtEnthalten
                null -> Unit
            }
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

    /** Nur globale Polynome besitzen hier einen nachgewiesenen stetigen reellen Rand. */
    private fun istPolynom(a: ZahlAusdruck): Boolean = when(a) {
        is Variable, is RationaleZahl -> true
        is Addition -> a.summanden.all(::istPolynom)
        is Multiplikation -> a.faktoren.all(::istPolynom)
        is Potenz -> istPolynom(a.basis) && (a.exponent as? RationaleZahl)?.let { it.nenner == BigInteger.ONE && it.zähler.signum() >= 0 } == true
        else -> false
    }

    private fun numerischerWert(
        ausdruck: ZahlAusdruck,
        umgebung: Map<String, Double>,
    ): Double? = when (val ergebnis = NumerischerAuswerter.wert(ausdruck, NumerischeUmgebung(umgebung))) {
        is NumerischesErgebnis.Wert -> ergebnis.wert
        is NumerischesErgebnis.Fehler -> null
    }

    private val KoordinatenErgebnis.beschreibung: String
        get() = when (this) {
            is KoordinatenErgebnis.Darstellbar -> "darstellbar"
            is KoordinatenErgebnis.BedingtDarstellbar -> grund
            is KoordinatenErgebnis.ProjektionErforderlich -> grund
            is KoordinatenErgebnis.NichtDarstellbar -> grund
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
        val umgebung = c.festeSchnitte + zusätzlicheUmgebung + c.achsenNamen.zip(this).toMap()
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
            RaumDimension.R2, RaumDimension.C -> 2
            RaumDimension.R3 -> 3
        }

    private val VisualisierungsKonfiguration.achsenNamen: List<String>
        get() = when (dimension) {
            RaumDimension.R1 -> listOf(achsen.x)
            RaumDimension.R2, RaumDimension.C -> listOf(achsen.x, achsen.y)
            RaumDimension.R3 -> listOf(achsen.x, achsen.y, achsen.z.orEmpty())
        }

    private val VisualisierungsKonfiguration.achsenBereiche: List<ZahlenBereich>
        get() = when (dimension) {
            RaumDimension.R1 -> listOf(bereiche.x)
            RaumDimension.R2, RaumDimension.C -> listOf(bereiche.x, bereiche.y)
            RaumDimension.R3 -> listOfNotNull(bereiche.x, bereiche.y, bereiche.z)
        }

    private val VisualisierungsKonfiguration.achsenAuflösung: Int
        get() = when (dimension) {
            RaumDimension.R1 -> sampling.auflösung1D
            RaumDimension.R2, RaumDimension.C -> sampling.auflösung2D
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

    private fun punktObjekt(
        punkt: List<Double>,
        c: VisualisierungsKonfiguration,
    ): MathematischesObjekt = when {
        c.dimension == RaumDimension.C && punkt.size == 2 ->
            KomplexeZahl(rationaleZahl(punkt[0]), rationaleZahl(punkt[1]))
        punkt.size == 1 -> rationaleZahl(punkt.single())
        else -> Tupel(punkt.map(::rationaleZahl))
    }

    private fun rationaleZahl(wert: Double): RationaleZahl {
        val dezimal = BigDecimal.valueOf(wert).stripTrailingZeros()
        val skala = dezimal.scale()
        return if (skala <= 0) {
            RationaleZahl.von(dezimal.unscaledValue() * BigInteger.TEN.pow(-skala))
        } else RationaleZahl.von(dezimal.unscaledValue(), BigInteger.TEN.pow(skala))
    }

    private fun kurzeZahl(wert: Double): String =
        BigDecimal.valueOf(wert).stripTrailingZeros().toPlainString()
}

internal fun vereinigeVisualisierungsErgebnisse(ergebnisse: List<VisualisierungsErgebnis>): VisualisierungsErgebnis? {
    if (ergebnisse.isEmpty()) return null
    val e = ergebnisse.mapNotNull { it as? VisualisierungsErgebnis.Erfolgreich }
    val t = ergebnisse.mapNotNull { it as? VisualisierungsErgebnis.Teilweise }
    val hinweise = ergebnisse.mapNotNull { when(it) {
        is VisualisierungsErgebnis.NichtDarstellbar -> it.grund
        is VisualisierungsErgebnis.ProjektionErforderlich -> it.grund
        is VisualisierungsErgebnis.BedingtDarstellbar -> it.grund
        else -> null
    } } + e.flatMap { it.hinweise } + t.flatMap { it.hinweise }
    val punkte=e.flatMap { it.punkte }+t.flatMap { it.punkte };val linien=e.flatMap { it.linien }+t.flatMap { it.linien }
    val dreiecke=e.flatMap { it.dreiecke }+t.flatMap { it.dreiecke };val zellen=e.flatMap { it.zellen }+t.flatMap { it.zellen }
    val interaktionsDreiecke=if(e.any { it.interaktionsDreiecke.isNotEmpty() } || t.any { it.interaktionsDreiecke.isNotEmpty() })
        e.flatMap { it.interaktionsDreiecke.ifEmpty { it.dreiecke } }+t.flatMap { it.interaktionsDreiecke.ifEmpty { it.dreiecke } } else emptyList()
    val intervalle=e.flatMap { it.intervalle }+t.flatMap { it.intervalle }
    val statistik=VisualisierungsStatistik(auswertungen=(e.map { it.statistik }+t.map { it.statistik }).sumOf { it.auswertungen },
        budgetErschöpft=(e.map { it.statistik }+t.map { it.statistik }).any { it.budgetErschöpft },
        zellArbeit=(e.map {it.statistik}+t.map {it.statistik}).sumOf {it.zellArbeit},
        punktArbeit=(e.map {it.statistik}+t.map {it.statistik}).sumOf {it.punktArbeit},
        randArbeit=(e.map {it.statistik}+t.map {it.statistik}).sumOf {it.randArbeit})
    return if(t.isNotEmpty() || e.size!=ergebnisse.size) VisualisierungsErgebnis.Teilweise(punkte,hinweise,zellen=zellen,linien=linien,dreiecke=dreiecke,intervalle=intervalle,statistik=statistik,interaktionsDreiecke=interaktionsDreiecke)
        else VisualisierungsErgebnis.Erfolgreich(punkte,e.any { it.istApproximation },hinweise,zellen=zellen,linien=linien,dreiecke=dreiecke,intervalle=intervalle,statistik=statistik,interaktionsDreiecke=interaktionsDreiecke)
}

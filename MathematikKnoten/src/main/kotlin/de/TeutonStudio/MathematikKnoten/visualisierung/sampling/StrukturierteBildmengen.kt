package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.CancellationException
import kotlin.math.*

/** Produktblätter sind Samplingachsen; der Packungsbaum erhält die öffentliche Argumentstruktur. */
private sealed interface ArgumentPackung {
    fun wert(achsen: List<MathematischesObjekt>): MathematischesObjekt
    data class Blatt(val index: Int) : ArgumentPackung {
        override fun wert(achsen: List<MathematischesObjekt>) = achsen[index]
    }
    data class Produkt(val kinder: List<ArgumentPackung>, val orientierung: VektorOrientierung? = null) : ArgumentPackung {
        override fun wert(achsen: List<MathematischesObjekt>): MathematischesObjekt {
            val werte = kinder.map { it.wert(achsen) }
            return when (orientierung) {
                VektorOrientierung.Zeile -> ZeilenVektor(werte.map { it as ZahlAusdruck })
                VektorOrientierung.Spalte -> SpaltenVektor(werte.map { it as ZahlAusdruck })
                null -> Tupel(werte)
            }
        }
    }
}

private data class ProduktAchse(
    val endlich: List<MathematischesObjekt>? = null,
    val minimum: Double = 0.0,
    val maximum: Double = 1.0,
    val linksOffen: Boolean = false,
    val rechtsOffen: Boolean = false,
) {
    fun kontinuierlicherWert(t: Double): MathematischesObjekt {
        var x = minimum + (maximum - minimum) * t
        if (linksOffen && x <= minimum) x = Math.nextUp(minimum)
        if (rechtsOffen && x >= maximum) x = Math.nextDown(maximum)
        require(x.isFinite() && (!linksOffen || x > minimum) && (!rechtsOffen || x < maximum)) {
            "Das Parameterintervall besitzt keine darstellbaren inneren Stützpunkte."
        }
        return rationaleStützstelle(x)
    }
    fun rasterWert(index: Int, anzahl: Int): MathematischesObjekt {
        val t = when {
            linksOffen && rechtsOffen -> (index + 1.0) / (anzahl + 1.0)
            linksOffen -> (index + 1.0) / anzahl
            rechtsOffen -> index.toDouble() / anzahl
            else -> index.toDouble() / (anzahl - 1)
        }
        return kontinuierlicherWert(t)
    }
}

/** Erst das gesamte Methodenergebnis auswerten, danach Ausgabekoordinaten auswählen. */
internal fun sampleStrukturierteBildmenge(
    abbild: Abbild,
    konfiguration: VisualisierungsKonfiguration,
    abbruchPrüfen: () -> Unit,
    graph: Boolean = false,
): VisualisierungsErgebnis {
    val methode = abbild.methode
    val dimension = when (konfiguration.dimension) {
        RaumDimension.R1 -> 1
        RaumDimension.R2, RaumDimension.C -> 2
        RaumDimension.R3 -> 3
    }
    if (!graph && konfiguration.methodenModus == MethodenDarstellungsModus.Funktionsgraph) {
        return VisualisierungsErgebnis.NichtDarstellbar(
            "Ein strukturierter Parameter besitzt mehrere innere Achsen. Wähle für seine vollständige Bildmenge den Bildmodus.",
        )
    }
    if (graph && konfiguration.ausgabeKoordinaten.isEmpty() && empfohleneRaumDimension(methode.graphRaum()) == null)
        return VisualisierungsErgebnis.ProjektionErforderlich(4, dimension, "Der Graph benötigt eine explizite Koordinatenprojektion.")
    val budget = konfiguration.sampling.maximalesRasterBudget.coerceAtLeast(1)
    val achsen = mutableListOf<ProduktAchse>()
    val fensterHinweise = mutableListOf<String>()
    fun reell(x: ZahlAusdruck): Double = ((DomaenenAuswerter.wert(x) as? DomaenenErgebnis.Wert)?.wert as? DomaenenWert.Reell)
        ?.wert?.toDouble()?.takeIf(Double::isFinite) ?: error("Eine Parametergrenze ist nicht endlich reell auswertbar.")
    fun baue(menge: MengenAusdruck): ArgumentPackung = when (menge) {
        is Tupelraum -> ArgumentPackung.Produkt(menge.komponenten.map(::baue))
        is KartesischesProdukt -> ArgumentPackung.Produkt(menge.mengen.map(::baue))
        is Vektorraum -> ArgumentPackung.Produkt(List(menge.dimension) { baue(menge.skalarMenge) }, menge.orientierung)
        else -> {
            val index = achsen.size
            val achse = when (menge) {
                LeereMenge -> ProduktAchse(endlich = emptyList())
                is EndlicheMenge -> ProduktAchse(endlich = menge.elemente.sortedBy { it.zuLatex() })
                is ReellesIntervall -> {
                    val min = reell(menge.links); val max = reell(menge.rechts)
                    when {
                        min > max || min == max && (menge.linksOffen || menge.rechtsOffen) -> ProduktAchse(endlich = emptyList())
                        min == max -> ProduktAchse(endlich = listOf(rationaleStützstelle(min)))
                        !(max - min).isFinite() -> error("Die Parameterintervallspanne ist nicht endlich darstellbar.")
                        else -> ProduktAchse(minimum = min, maximum = max, linksOffen = menge.linksOffen, rechtsOffen = menge.rechtsOffen)
                    }
                }
                ReelleZahlen, RationaleZahlen, GanzeZahlen, NatürlicheZahlen -> {
                    val fensterBereiche = if (graph) konfiguration.sampling.sichtBereiche ?: konfiguration.bereiche else konfiguration.bereiche
                    val fenster = when (index) {
                        0 -> fensterBereiche.x
                        1 -> fensterBereiche.y
                        else -> fensterBereiche.z ?: fensterBereiche.x
                    }
                    fensterHinweise += "Die unbeschränkte Parameterachse ${index + 1} wird durch ihr Parameterfenster begrenzt."
                    if (menge == GanzeZahlen || menge == NatürlicheZahlen) {
                        val start = maxOf(ceil(fenster.minimum), if (menge == NatürlicheZahlen) 1.0 else ceil(fenster.minimum))
                        val ende = floor(fenster.maximum)
                        val anzahl = if (ende < start) 0 else (ende - start + 1).also {
                            require(it <= budget) { "Die diskrete Parameterachse überschreitet das Samplingbudget." }
                        }.toInt()
                        ProduktAchse(endlich = List(anzahl) { rationaleStützstelle(start + it) })
                    } else ProduktAchse(minimum = fenster.minimum, maximum = fenster.maximum)
                }
                else -> error("${menge::class.simpleName} ist keine unterstützte numerische Produktdomäne.")
            }
            achsen += achse
            ArgumentPackung.Blatt(index)
        }
    }
    val packung = try { baue(abbild.menge) } catch (fehler: IllegalArgumentException) {
        return VisualisierungsErgebnis.NichtDarstellbar(fehler.message ?: "Ungültige Produktdomäne.")
    } catch (fehler: IllegalStateException) {
        return VisualisierungsErgebnis.NichtDarstellbar(fehler.message ?: "Nicht unterstützte Produktdomäne.")
    }
    if (achsen.any { it.endlich?.isEmpty() == true }) {
        return VisualisierungsErgebnis.Erfolgreich(emptyList(), false, listOf("Die Produktdomäne ist mathematisch leer."), VisualisierungsQualität.MathematischLeer)
    }
    val diskret = achsen.indices.filter { achsen[it].endlich != null }
    val kontinuierlich = achsen.indices.filter { achsen[it].endlich == null }
    fun begrenztesProdukt(faktoren: Iterable<Int>): Long = faktoren.fold(1L) { a, b ->
        if (b == 0) 0L else if (a > budget.toLong() / b) budget.toLong() + 1 else a * b
    }
    val zweigAnzahl = begrenztesProdukt(diskret.map { achsen[it].endlich!!.size })
    if (zweigAnzahl > budget) return VisualisierungsErgebnis.NichtDarstellbar(
        "Die vollständigen diskreten Parameterzweige überschreiten das Samplingbudget von $budget; keine Zweige werden stillschweigend ausgelassen.",
    )
    val basis = when (kontinuierlich.size) {
        0 -> 1
        1 -> konfiguration.sampling.auflösung1D
        2 -> konfiguration.sampling.auflösung2D
        else -> konfiguration.sampling.auflösung3D
    }.coerceAtLeast(2)
    val budgetAuflösung = if (kontinuierlich.isEmpty()) 1 else
        floor((budget.toDouble() / zweigAnzahl).pow(1.0 / kontinuierlich.size)).toInt().coerceAtLeast(2)
    var auflösung = minOf(basis, budgetAuflösung)
    while (auflösung > 2 && begrenztesProdukt(listOf(zweigAnzahl.toInt()) + List(kontinuierlich.size) { auflösung }) > budget) auflösung--
    val halton = begrenztesProdukt(listOf(zweigAnzahl.toInt()) + List(kontinuierlich.size) { auflösung }) > budget
    var zweige = listOf(emptyList<MathematischesObjekt>())
    diskret.forEach { i -> zweige = zweige.flatMap { prefix -> achsen[i].endlich!!.map { prefix + it } } }
    val werte = MutableList<MathematischesObjekt>(achsen.size) { RationaleZahl.Null }
    val punkte = mutableListOf<VisualisierungsPunkt>()
    val raster = List(zweige.size) { mutableListOf<VisualisierungsPunkt?>() }
    val diagnosen = linkedSetOf<String>()
    var projektion: KoordinatenErgebnis.ProjektionErforderlich? = null
    var auswertungen = 0
    fun auswerten(zweig: Int) {
        abbruchPrüfen()
        if (projektion != null && konfiguration.ausgabeKoordinaten.isEmpty()) return
        auswertungen++
        try {
            val argument = packung.wert(werte)
            val ergebnis = methode.wendeAn(methode.argumenteAusGesamtArgument(argument))
            val bild = if (graph) Tupel(listOf(argument, ergebnis)) else if (konfiguration.methodenModus == MethodenDarstellungsModus.Koordinatenausgabe && methode.ausgabeNamen.size > 1 && konfiguration.ausgabeKoordinaten.isEmpty()) {
                val vollständig = KoordinatenAdapter.extrahiere(ergebnis, 1, konfiguration.festeSchnitte)
                require(vollständig is KoordinatenErgebnis.Darstellbar || vollständig is KoordinatenErgebnis.ProjektionErforderlich) {
                    vollständig.beschreibung
                }
                val namen = listOf(konfiguration.achsen.x, konfiguration.achsen.y, konfiguration.achsen.z).take(dimension)
                val ausgaben = ergebnis as? Tupel ?: error("Explizite Mehrfachausgaben benötigen ein Ergebnistupel.")
                Tupel(namen.map { name ->
                    val i = methode.ausgabeNamen.indexOf(name)
                    require(i >= 0) { "Für die Achse '$name' fehlt eine öffentliche Methodenausgabe." }
                    ausgaben.elemente[i]
                })
            } else ergebnis
            when (val koordinaten = KoordinatenAdapter.extrahiere(bild, dimension,
                konfiguration.festeSchnitte, ausgabeKoordinaten = if (graph && konfiguration.ausgabeKoordinaten.isEmpty()) (1..dimension).toList() else konfiguration.ausgabeKoordinaten)) {
                is KoordinatenErgebnis.Darstellbar -> {
                    val w = koordinaten.werte
                    val punkt = VisualisierungsPunkt(w[0], w.getOrElse(1) { 0.0 }, w.getOrNull(2))
                    punkte += punkt; raster[zweig] += punkt
                }
                else -> {
                    diagnosen += koordinaten.beschreibung
                    if (koordinaten is KoordinatenErgebnis.ProjektionErforderlich) projektion = koordinaten
                    raster[zweig] += null
                }
            }
        } catch (fehler: CancellationException) { throw fehler
        } catch (fehler: Exception) {
            diagnosen += fehler.message ?: "Der strukturierte Methodenaufruf ist nicht auswertbar."
            raster[zweig] += null
        }
    }
    fun setzeZweig(z: Int) { diskret.forEachIndexed { i, achse -> werte[achse] = zweige[z][i] } }
    if (halton) {
        val primzahlen = erstePrimzahlen(kontinuierlich.size)
        repeat(budget / zweige.size) { nummer ->
            kontinuierlich.forEachIndexed { i, achse -> werte[achse] = achsen[achse].kontinuierlicherWert(haltonWert(nummer + 1, primzahlen[i])) }
            // Alle diskreten Zweige erhalten dieselben kontinuierlichen Stützwerte, insbesondere das Vorzeichenpaar.
            zweige.indices.forEach { z -> setzeZweig(z); auswerten(z) }
        }
    } else {
        fun rasterAchse(i: Int, zweig: Int) {
            if (i == kontinuierlich.size) { auswerten(zweig); return }
            val achse = kontinuierlich[i]
            repeat(auflösung) { j -> werte[achse] = achsen[achse].rasterWert(j, auflösung); rasterAchse(i + 1, zweig) }
        }
        zweige.indices.forEach { z -> setzeZweig(z); rasterAchse(0, z) }
    }
    if (punkte.isEmpty()) {
        projektion?.let { return VisualisierungsErgebnis.ProjektionErforderlich(it.vorhandeneDimension, it.erwarteteDimension, it.beschreibung) }
        return VisualisierungsErgebnis.NichtDarstellbar(diagnosen.joinToString(" ").ifBlank { "Die Bildmenge erzeugt keine numerischen Punkte." })
    }
    val linien = mutableListOf<VisualisierungsLinie>()
    val dreiecke = mutableListOf<VisualisierungsDreieck>()
    if (!halton && kontinuierlich.size == 1) raster.forEach { zweig ->
        var abschnitt = mutableListOf<VisualisierungsPunkt>()
        fun abschließen() { if (abschnitt.size > 1) linien += VisualisierungsLinie(abschnitt.toList()); abschnitt = mutableListOf() }
        zweig.forEach { if (it == null) abschließen() else abschnitt += it }; abschließen()
    }
    if (!halton && kontinuierlich.size == 2) raster.forEach { zweig ->
        for (i in 0 until auflösung - 1) for (j in 0 until auflösung - 1) {
            val a = zweig[i * auflösung + j]; val b = zweig[(i + 1) * auflösung + j]
            val c = zweig[i * auflösung + j + 1]; val d = zweig[(i + 1) * auflösung + j + 1]
            if (a != null && b != null && c != null) dreiecke += VisualisierungsDreieck(a, b, c)
            if (b != null && c != null && d != null) dreiecke += VisualisierungsDreieck(b, d, c)
        }
    }
    val hinweise = fensterHinweise.distinct() + listOf(
        "Bildmodus: ${methode.parameter.size} öffentliche Argumente, ${kontinuierlich.size} kontinuierliche Achsen, ${zweige.size} getrennte diskrete Zweige.",
        if (halton) "Deterministische Halton-Approximation mit gepaarten diskreten Zweigen: $auswertungen Auswertungen innerhalb Budget $budget."
        else "Vollständiges Produktgitter: $auswertungen Auswertungen; endliche Parameterintervalle bleiben unabhängig vom Kamerafenster.",
    ) + diagnosen
    val eindeutig = punkte.distinct()
    val statistik = VisualisierungsStatistik(auswertungen = auswertungen, stützpunkte = eindeutig.size, linien = linien.size, dreiecke = dreiecke.size)
    return if (diagnosen.isNotEmpty()) VisualisierungsErgebnis.Teilweise(eindeutig, hinweise, linien = linien, dreiecke = dreiecke, statistik = statistik)
    else VisualisierungsErgebnis.Erfolgreich(eindeutig, kontinuierlich.isNotEmpty(), hinweise, linien = linien, dreiecke = dreiecke, statistik = statistik)
}

private fun rationaleStützstelle(wert: Double): RationaleZahl {
    val d = BigDecimal.valueOf(wert).stripTrailingZeros()
    return if (d.scale() <= 0) RationaleZahl.von(d.unscaledValue() * BigInteger.TEN.pow(-d.scale()))
    else RationaleZahl.von(d.unscaledValue(), BigInteger.TEN.pow(d.scale()))
}

private fun erstePrimzahlen(anzahl: Int): List<Int> = buildList {
    var n = 2
    while (size < anzahl) {
        if (none { p -> p.toLong() * p <= n && n % p == 0 }) add(n)
        n++
    }
}

private fun haltonWert(index: Int, basis: Int): Double {
    var n = index; var faktor = 1.0; var wert = 0.0
    while (n > 0) { faktor /= basis; wert += faktor * (n % basis); n /= basis }
    return wert
}

private val KoordinatenErgebnis.beschreibung: String
    get() = when (this) {
        is KoordinatenErgebnis.Darstellbar -> "Darstellbar"
        is KoordinatenErgebnis.BedingtDarstellbar -> grund
        is KoordinatenErgebnis.ProjektionErforderlich -> grund
        is KoordinatenErgebnis.NichtDarstellbar -> grund
    }

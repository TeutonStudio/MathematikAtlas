package de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten

import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigDecimal

/**
 * Eine numerisch ausgewertete reelle Koordinatenkomponente mit ihrer mathematischen Herkunft.
 *
 * Der Adapter verliert die Quellstruktur absichtlich nicht bereits bei der Extraktion. Spätere
 * Dimensionsvisualisierung kann damit semantische Komponenten statt anonymer Double-Positionen
 * referenzieren.
 */
data class ReelleKoordinatenKomponente(
    val index: Int,
    val ausdruck: ZahlAusdruck,
    val wert: Double,
)

/**
 * Strukturierter Vertrag zwischen mathematischem Objekt und räumlicher Darstellung.
 *
 * Eine erforderliche Projektion ist ausdrücklich kein mathematischer Fehler. Ebenso wird eine
 * symbolisch noch offene Struktur nicht mit einer endgültig nicht darstellbaren Struktur vermischt.
 */
sealed interface KoordinatenErgebnis {
    data class Darstellbar(
        val komponenten: List<ReelleKoordinatenKomponente>,
    ) : KoordinatenErgebnis {
        val werte: List<Double> get() = komponenten.map(ReelleKoordinatenKomponente::wert)
    }

    data class BedingtDarstellbar(
        val grund: String,
        val bedingungen: List<String> = emptyList(),
    ) : KoordinatenErgebnis

    data class ProjektionErforderlich(
        val vorhandeneDimension: Int,
        val erwarteteDimension: Int,
        val grund: String,
    ) : KoordinatenErgebnis

    data class NichtDarstellbar(
        val grund: String,
    ) : KoordinatenErgebnis
}

/**
 * Zentraler Adapter für reelle Visualisierungskoordinaten.
 *
 * Er verwendet die Strukturverträge des Mathematik-Rechenkerns. Tupel, Zeilen- und Spaltenvektoren
 * erhalten damit keine voneinander unabhängigen Visualisierer-Sonderregeln mehr. Der
 * domänenerhaltende Auswerter lässt reell eingebettete komplexe Werte zu und kennzeichnet echte
 * komplexe oder quaternionische Werte ausdrücklich als projektionsbedürftig.
 */
object KoordinatenAdapter {
    fun extrahiere(
        objekt: MathematischesObjekt,
        erwarteteDimension: Int,
        umgebung: Map<String, Double> = emptyMap(),
        werteVorraete: Map<String, MengenAusdruck> = emptyMap(),
        domänenUmgebung: Map<String, DomaenenWert> = emptyMap(),
    ): KoordinatenErgebnis {
        require(erwarteteDimension > 0) { "Die erwartete Koordinatendimension muss positiv sein." }

        if (objekt is KoordinatenBild) {
            val systemDimension = objekt.koordinatensystem.raum.dimension
            if (systemDimension > erwarteteDimension) {
                return KoordinatenErgebnis.ProjektionErforderlich(
                    vorhandeneDimension = systemDimension,
                    erwarteteDimension = erwarteteDimension,
                    grund = "Das Koordinatensystem ${objekt.koordinatensystem.name} besitzt Dimension $systemDimension. Für R$erwarteteDimension ist eine ausdrückliche Projektion erforderlich.",
                )
            }
            if (systemDimension != erwarteteDimension) {
                return KoordinatenErgebnis.NichtDarstellbar(
                    "Das Koordinatensystem ${objekt.koordinatensystem.name} besitzt Dimension $systemDimension statt $erwarteteDimension.",
                )
            }
            val punkt = objekt.objekt as? GeometriePunkt
                ?: return KoordinatenErgebnis.NichtDarstellbar(
                    "Das Koordinatenbild von ${objekt.objekt::class.simpleName} ist keine einzelne Punktkoordinate und benötigt eine eigene geometrische Materialisierung.",
                )
            val koordinaten = punkt.koordinaten
                ?: return KoordinatenErgebnis.BedingtDarstellbar(
                    "Der Punkt ${punkt.name} besitzt im Koordinatensystem ${objekt.koordinatensystem.name} noch keine bestimmten Koordinaten.",
                )
            return extrahiere(
                koordinaten,
                erwarteteDimension,
                umgebung,
                werteVorraete,
                domänenUmgebung,
            )
        }

        if (objekt is FallAusdruck) {
            return when (val bedingung = NumerischerAuswerter.aussage(
                objekt.aussage,
                NumerischeUmgebung(umgebung),
            )) {
                is NumerischesErgebnis.Wert -> extrahiere(
                    if (bedingung.wert) objekt.wahr else objekt.lüge,
                    erwarteteDimension,
                    umgebung,
                    werteVorraete,
                    domänenUmgebung,
                )
                is NumerischesErgebnis.Fehler -> KoordinatenErgebnis.BedingtDarstellbar(
                    grund = "Fallbedingung nicht eindeutig auswertbar: ${bedingung.beschreibung}",
                )
            }
        }

        val ausdruecke = when (objekt) {
            is ZahlAusdruck -> {
                if (erwarteteDimension != 1) {
                    return KoordinatenErgebnis.NichtDarstellbar(
                        "Ein skalares Element ist nur in R¹ eine einzelne reelle Koordinate.",
                    )
                }
                listOf(objekt)
            }

            is Tupel, is ZeilenVektor, is SpaltenVektor -> when (
                val ansicht = runCatching {
                    objekt.numerischeKomponentenAnsicht(werteVorraete = werteVorraete)
                }.getOrElse {
                    return KoordinatenErgebnis.BedingtDarstellbar(
                        grund = it.message ?: "Die numerische Komponentenstruktur ist noch nicht bestimmbar.",
                    )
                }
            ) {
                is StrukturPruefung.Gueltig -> ansicht.wert.komponenten
                is StrukturPruefung.Bedingt -> return KoordinatenErgebnis.BedingtDarstellbar(
                    grund = "Die numerische Komponentenstruktur ist nur bedingt gültig.",
                    bedingungen = ansicht.bedingungen,
                )
                is StrukturPruefung.Ungueltig -> return KoordinatenErgebnis.NichtDarstellbar(ansicht.grund)
                is StrukturPruefung.Unentscheidbar -> return KoordinatenErgebnis.BedingtDarstellbar(ansicht.grund)
            }

            else -> return KoordinatenErgebnis.NichtDarstellbar(
                "Element ist weder Zahl, kartesisches Tupel noch Zeilen- oder Spaltenvektor.",
            )
        }

        if (ausdruecke.size > erwarteteDimension) {
            return KoordinatenErgebnis.ProjektionErforderlich(
                vorhandeneDimension = ausdruecke.size,
                erwarteteDimension = erwarteteDimension,
                grund = "Die Komponentenstruktur besitzt ${ausdruecke.size} reelle Komponenten, der aktuelle Raum aber nur $erwarteteDimension. Eine Projektion muss ausdrücklich gewählt werden.",
            )
        }
        if (ausdruecke.size != erwarteteDimension) {
            return KoordinatenErgebnis.NichtDarstellbar(
                "Koordinatendimension ${ausdruecke.size} statt erwartet $erwarteteDimension.",
            )
        }

        val kontext = DomaenenKontext(
            variablen = umgebung.mapValues { DomaenenWert.Reell(BigDecimal.valueOf(it.value)) } + domänenUmgebung,
        )
        val komponenten = ausdruecke.mapIndexed { index, ausdruck ->
            when (val ergebnis = DomaenenAuswerter.wert(ausdruck, kontext)) {
                is DomaenenErgebnis.Wert -> when (val reell = ergebnis.wert.alsReelleKoordinate()) {
                    is ReelleKoordinateErgebnis.Wert -> {
                        val wert = reell.wert.toDouble()
                        if (!wert.isFinite()) {
                            return KoordinatenErgebnis.NichtDarstellbar(
                                "Koordinate ${index + 1} ist nicht endlich.",
                            )
                        }
                        ReelleKoordinatenKomponente(index, ausdruck, wert)
                    }
                    is ReelleKoordinateErgebnis.ProjektionErforderlich -> return KoordinatenErgebnis.ProjektionErforderlich(
                        vorhandeneDimension = ausdruecke.size - 1 + ergebnis.wert.reelleDimension,
                        erwarteteDimension = erwarteteDimension,
                        grund = "Koordinate ${index + 1}: ${reell.grund.nachricht}",
                    )
                }
                is DomaenenErgebnis.Unentscheidbar -> return KoordinatenErgebnis.BedingtDarstellbar(
                    grund = "Koordinate ${index + 1} ist noch nicht entscheidbar: ${ergebnis.grund.nachricht}",
                )
                is DomaenenErgebnis.NichtDefiniert -> return KoordinatenErgebnis.NichtDarstellbar(
                    "Koordinate ${index + 1} ist nicht definiert: ${ergebnis.grund.nachricht}",
                )
                is DomaenenErgebnis.NichtEndlich -> return KoordinatenErgebnis.NichtDarstellbar(
                    "Koordinate ${index + 1} ist nicht endlich: ${ergebnis.grund.nachricht}",
                )
                is DomaenenErgebnis.NichtUnterstuetzt -> return KoordinatenErgebnis.NichtDarstellbar(
                    "Koordinate ${index + 1} wird nicht unterstützt: ${ergebnis.grund.nachricht}",
                )
            }
        }

        return KoordinatenErgebnis.Darstellbar(komponenten)
    }
}

private val DomaenenWert.reelleDimension: Int
    get() = when (this) {
        is DomaenenWert.Reell -> 1
        is DomaenenWert.Komplex -> 2
        is DomaenenWert.Quaternion -> 4
    }

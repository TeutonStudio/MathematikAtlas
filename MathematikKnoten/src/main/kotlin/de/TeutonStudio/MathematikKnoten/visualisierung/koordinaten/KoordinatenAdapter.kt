package de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten

import de.TeutonStudio.MathematikRechenSystem.kern.*

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
 * erhalten damit keine voneinander unabhängigen Visualisierer-Sonderregeln mehr. Komplexe und andere
 * nicht reell eindimensionale Zahlbereiche werden in späteren Ausbaustufen über denselben Vertrag
 * ergänzt, statt hier still projiziert zu werden.
 */
object KoordinatenAdapter {
    fun extrahiere(
        objekt: MathematischesObjekt,
        erwarteteDimension: Int,
        umgebung: Map<String, Double> = emptyMap(),
        werteVorraete: Map<String, MengenAusdruck> = emptyMap(),
    ): KoordinatenErgebnis {
        require(erwarteteDimension > 0) { "Die erwartete Koordinatendimension muss positiv sein." }

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

        val komponenten = ausdruecke.mapIndexed { index, ausdruck ->
            when (val ergebnis = NumerischerAuswerter.wert(ausdruck, NumerischeUmgebung(umgebung))) {
                is NumerischesErgebnis.Wert -> {
                    if (!ergebnis.wert.isFinite()) {
                        return KoordinatenErgebnis.NichtDarstellbar(
                            "Koordinate ${index + 1} ist nicht endlich.",
                        )
                    }
                    ReelleKoordinatenKomponente(index, ausdruck, ergebnis.wert)
                }
                is NumerischesErgebnis.Fehler -> return KoordinatenErgebnis.BedingtDarstellbar(
                    grund = "Koordinate ${index + 1} ist nicht numerisch auswertbar: ${ergebnis.beschreibung}",
                )
            }
        }

        return KoordinatenErgebnis.Darstellbar(komponenten)
    }
}

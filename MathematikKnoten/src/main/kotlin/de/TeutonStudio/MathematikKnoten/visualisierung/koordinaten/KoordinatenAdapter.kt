package de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten

import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigDecimal

/**
 * Eine numerisch ausgewertete reelle Koordinatenkomponente mit ihrer mathematischen Herkunft.
 * `semantik` erhält bei zusammengesetzten Skalaren die natürliche Komponentenrolle.
 */
data class ReelleKoordinatenKomponente(
    val index: Int,
    val ausdruck: ZahlAusdruck,
    val wert: Double,
    val semantik: String? = null,
)

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
 * Reelle Komponenten werden erst nach der domänenerhaltenden Auswertung gezählt. Dadurch bleiben
 * mathematisch verschiedene Räume unterscheidbar: ein komplexer Skalar liefert kanonisch
 * `Re(z), Im(z)`, während ein gewöhnliches Zweiertupel zwei eigenständige Faktoren bleibt.
 * Produktreihenfolgen wie `C×R` und `R×C` werden beim flachen Entfalten beibehalten.
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
            is ZahlAusdruck -> listOf(objekt)

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
                "Element ist weder Tupel noch Zeilen- oder Spaltenvektor und keine Zahl.",
            )
        }

        val kontext = DomaenenKontext(
            variablen = umgebung.mapValues { DomaenenWert.Reell(BigDecimal.valueOf(it.value)) } + domänenUmgebung,
        )
        val komponenten = mutableListOf<ReelleKoordinatenKomponente>()

        ausdruecke.forEachIndexed { quellIndex, ausdruck ->
            when (val ergebnis = DomaenenAuswerter.wert(ausdruck, kontext)) {
                is DomaenenErgebnis.Wert -> when (val wert = ergebnis.wert) {
                    is DomaenenWert.Reell -> komponenten += komponentenKomponente(
                        komponenten.size,
                        ausdruck,
                        wert.wert,
                        if (ausdruecke.size > 1) "Komponente ${quellIndex + 1}" else null,
                    ) ?: return KoordinatenErgebnis.NichtDarstellbar(
                        "Koordinate ${komponenten.size + 1} ist nicht endlich.",
                    )
                    is DomaenenWert.Komplex -> {
                        komponenten += komponentenKomponente(
                            komponenten.size,
                            ausdruck,
                            wert.reell,
                            "Re(${ausdruck.zuLatex()})",
                        ) ?: return KoordinatenErgebnis.NichtDarstellbar("Realteil ist nicht endlich.")
                        komponenten += komponentenKomponente(
                            komponenten.size,
                            ausdruck,
                            wert.imaginaer,
                            "Im(${ausdruck.zuLatex()})",
                        ) ?: return KoordinatenErgebnis.NichtDarstellbar("Imaginärteil ist nicht endlich.")
                    }
                    is DomaenenWert.Quaternion -> return KoordinatenErgebnis.ProjektionErforderlich(
                        vorhandeneDimension = komponenten.size + 4 + (ausdruecke.size - quellIndex - 1),
                        erwarteteDimension = erwarteteDimension,
                        grund = "Koordinate ${quellIndex + 1} ist quaternionisch und benötigt eine ausdrückliche R⁴-Projektion.",
                    )
                }
                is DomaenenErgebnis.Unentscheidbar -> return KoordinatenErgebnis.BedingtDarstellbar(
                    grund = "Koordinate ${quellIndex + 1} ist noch nicht entscheidbar: ${ergebnis.grund.nachricht}",
                )
                is DomaenenErgebnis.NichtDefiniert -> return KoordinatenErgebnis.NichtDarstellbar(
                    "Koordinate ${quellIndex + 1} ist nicht definiert: ${ergebnis.grund.nachricht}",
                )
                is DomaenenErgebnis.NichtEndlich -> return KoordinatenErgebnis.NichtDarstellbar(
                    "Koordinate ${quellIndex + 1} ist nicht endlich: ${ergebnis.grund.nachricht}",
                )
                is DomaenenErgebnis.NichtUnterstuetzt -> return KoordinatenErgebnis.NichtDarstellbar(
                    "Koordinate ${quellIndex + 1} wird nicht unterstützt: ${ergebnis.grund.nachricht}",
                )
            }
        }

        if (komponenten.size > erwarteteDimension) {
            return KoordinatenErgebnis.ProjektionErforderlich(
                vorhandeneDimension = komponenten.size,
                erwarteteDimension = erwarteteDimension,
                grund = "Die natürliche Komponentenstruktur besitzt ${komponenten.size} reelle Komponenten, der aktuelle Raum aber nur $erwarteteDimension. Eine Projektion muss ausdrücklich gewählt werden.",
            )
        }
        if (komponenten.size != erwarteteDimension) {
            return KoordinatenErgebnis.NichtDarstellbar(
                "Koordinatendimension ${komponenten.size} statt erwartet $erwarteteDimension.",
            )
        }

        return KoordinatenErgebnis.Darstellbar(komponenten)
    }

    private fun komponentenKomponente(
        index: Int,
        ausdruck: ZahlAusdruck,
        wert: BigDecimal,
        semantik: String?,
    ): ReelleKoordinatenKomponente? {
        val double = wert.toDouble()
        if (!double.isFinite()) return null
        return ReelleKoordinatenKomponente(index, ausdruck, double, semantik)
    }
}

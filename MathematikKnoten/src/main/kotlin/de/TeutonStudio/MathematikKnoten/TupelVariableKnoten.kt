package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val TUPEL_VARIABLE_ART = "mathematik.tupelvariable"

object TupelVariableKnotenVorlagen {
    val standard = KnotenVorlage(
        TUPEL_VARIABLE_ART,
        "Tupelvariable",
        "Tupel",
        "Erzeugt ein homogenes symbolisches Tupel mit stabilen Komponentenidentitäten.",
        GraphGröße(245f, 110f),
        listOf(
            AnschlussDaten(
                name = "tupel",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Tupel.id,
            ),
        ),
        mapOf("name" to "x", "dimension" to "2", "werteVorrat" to "R"),
    )
}

internal fun MathematikAuswerterRegister.registriereTupelVariable() {
    registriere(TUPEL_VARIABLE_ART) { k ->
        val name = k.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
        val dimension = k.knoten.parameter["dimension"]?.toIntOrNull()
            ?: error("Die Tupeldimension muss eine ganze Zahl sein.")
        require(dimension >= 1) { "Die Tupeldimension muss mindestens 1 sein." }
        val werteVorrat = tupelVariablenWerteVorrat(k.knoten.parameter["werteVorrat"])
        val komponenten = List(dimension) { index ->
            Variable("${name}_${index + 1}", "${name}_{${index + 1}}")
        }
        KnotenAuswertungsErgebnis(
            mapOf(
                "tupel" to BedingterWert(
                    objekt = Tupel(komponenten),
                    variablenQuellen = komponenten.mapIndexed { index, variable ->
                        VariablenQuelle(
                            knotenId = k.knoten.id,
                            name = variable.name,
                            werteVorrat = werteVorrat,
                            bindungsId = "${k.knoten.id.wert}:tupel",
                            bindungsName = "komponente-${index + 1}",
                            reihenfolge = index,
                        )
                    },
                ),
            ),
        )
    }
}

private fun tupelVariablenWerteVorrat(id: String?): MengenAusdruck = when (id?.trim()?.uppercase()) {
    "N", "ℕ" -> NatürlicheZahlen
    "Z", "ℤ" -> GanzeZahlen
    "Q", "ℚ" -> RationaleZahlen
    "C", "ℂ" -> KomplexeZahlen
    else -> ReelleZahlen
}

internal fun tupelVariablenFormel(knoten: KnotenDaten): String {
    val name = knoten.parameter["name"]?.trim().orEmpty().ifBlank { "x" }
    val dimension = knoten.parameter["dimension"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    return if (dimension == 1) {
        "${name}=\\left(${name}_{1}\\right)"
    } else {
        "${name}=\\left(${name}_{1},\\ldots,${name}_{${dimension}}\\right)"
    }
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Der Gleichungslöser benennt gebundene Skalare lokal und zerlegt Matrixargumente in skalare Unbekannte. */
internal fun BedingterWert.mitSkalarenLösungsUnbekannten(): BedingterWert {
    val ersetzungen = mutableMapOf<String, MathematischesObjekt>()
    val quellen = variablenQuellen.flatMap { q ->
        val bindung = q.strukturBindung
        if (bindung != null && variablenAnschlussArt(q.werteVorrat.elementTypAusdruck()) == MathematikAnschlussArten.Zahl.id) {
            ersetzungen[bindung.id] = Variable(bindung.parameterName)
            return@flatMap listOf(q.copy(name = bindung.parameterName, strukturBindung = null))
        }
        val raum = q.werteVorrat as? Matrizenraum
        if (raum == null || bindung == null || q.lösungsRolle != LösungsQuellenRolle.Variable) return@flatMap listOf(q)
        val komponenten = List(raum.zeilen) { i -> List(raum.spalten) { j -> Variable("${bindung.parameterName}_${i + 1}_${j + 1}") } }
        ersetzungen[q.name] = Matrix(komponenten)
        komponenten.flatten().map { v -> q.copy(name = v.name, werteVorrat = raum.skalarMenge,
            bindungsId = "${bindung.id}:${v.name}", strukturBindung = null) }
    }
    if (ersetzungen.isEmpty()) return this
    return copy(objekt = ersetze(objekt as MathematischesObjekt, ersetzungen),
        annahmen = annahmen.map { ersetze(it, ersetzungen) }.toSet(), variablenQuellen = quellen)
}

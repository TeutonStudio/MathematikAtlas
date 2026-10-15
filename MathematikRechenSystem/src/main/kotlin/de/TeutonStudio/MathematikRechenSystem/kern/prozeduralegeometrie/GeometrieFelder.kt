package de.TeutonStudio.MathematikRechenSystem.kern

/** Der Index beginnt für jede Point-Domäne (Mesh/Punktwolke) erneut bei null. */
data class GeometrieFeldKontext(val position: GeometrieVektor3, val index: Int) { init { require(index >= 0) } }
enum class SkalarOperation { ADDIEREN, SUBTRAHIEREN, MULTIPLIZIEREN, DIVIDIEREN }
enum class VektorOperation { ADDIEREN, SUBTRAHIEREN }
enum class VergleichsOperation { KLEINER, GROESSER, GLEICH }

sealed class GeometrieFeld<T>(internal val tiefe: Int, internal val groesse: Int) : DarstellungsWert {
    init { require(tiefe <= GeometrieGrenzen.TIEFE && groesse <= 4096) { "Feld ist zu tief verschachtelt." } }
    fun auswerten(kontext: GeometrieFeldKontext): T = GeometrieFeldAuswertung().wert(this, kontext)
    override fun zuLatex() = "\\text{${this::class.simpleName}}"
}
sealed class SkalarFeld(tiefe: Int, groesse: Int = 1) : GeometrieFeld<Double>(tiefe, groesse) {
    data class Konstant(val wert: Double) : SkalarFeld(1) { init { require(wert.isFinite()) } }
    data object Index : SkalarFeld(1)
    data class Rechnung(val operation: SkalarOperation, val links: SkalarFeld, val rechts: SkalarFeld) : SkalarFeld(1+maxOf(links.tiefe,rechts.tiefe),1+links.groesse+rechts.groesse)
}
sealed class VektorFeld(tiefe: Int, groesse: Int = 1) : GeometrieFeld<GeometrieVektor3>(tiefe, groesse) {
    data class Konstant(val wert: GeometrieVektor3) : VektorFeld(1)
    data object Position : VektorFeld(1)
    data class Kombinieren(val x: SkalarFeld, val y: SkalarFeld, val z: SkalarFeld) : VektorFeld(1+maxOf(x.tiefe,y.tiefe,z.tiefe),1+x.groesse+y.groesse+z.groesse)
    data class Rechnung(val operation: VektorOperation, val links: VektorFeld, val rechts: VektorFeld) : VektorFeld(1+maxOf(links.tiefe,rechts.tiefe),1+links.groesse+rechts.groesse)
    data class Skalieren(val vektor: VektorFeld, val faktor: SkalarFeld) : VektorFeld(1+maxOf(vektor.tiefe,faktor.tiefe),1+vektor.groesse+faktor.groesse)
}
sealed class AuswahlFeld(tiefe: Int, groesse: Int = 1) : GeometrieFeld<Boolean>(tiefe, groesse) {
    data class Konstant(val wert: Boolean) : AuswahlFeld(1)
    data class Vergleich(val operation: VergleichsOperation, val links: SkalarFeld, val rechts: SkalarFeld) : AuswahlFeld(1+maxOf(links.tiefe,rechts.tiefe),1+links.groesse+rechts.groesse)
}

/** Gemeinsames Arbeitsbudget über alle Punkte einer Operation, inklusive AST-Unterausdrücken. */
internal class GeometrieFeldAuswertung {
    private var schritte = 0
    @Suppress("UNCHECKED_CAST")
    fun <T> wert(feld: GeometrieFeld<T>, k: GeometrieFeldKontext): T {
        require(++schritte <= GeometrieGrenzen.FELD_OPERATIONEN) { "Feld-Auswertungsbudget überschritten." }
        val ergebnis: Any = when(feld) {
            is SkalarFeld.Konstant -> feld.wert
            SkalarFeld.Index -> k.index.toDouble()
            is SkalarFeld.Rechnung -> {
                val a=wert(feld.links,k); val b=wert(feld.rechts,k)
                when(feld.operation) {
                    SkalarOperation.ADDIEREN -> a+b
                    SkalarOperation.SUBTRAHIEREN -> a-b
                    SkalarOperation.MULTIPLIZIEREN -> a*b
                    SkalarOperation.DIVIDIEREN -> { require(b != 0.0) { "Division durch null im Geometriefeld." }; a/b }
                }.also { require(it.isFinite()) { "Nichtendliches Feldergebnis." } }
            }
            is VektorFeld.Konstant -> feld.wert
            VektorFeld.Position -> k.position
            is VektorFeld.Kombinieren -> GeometrieVektor3(wert(feld.x,k),wert(feld.y,k),wert(feld.z,k))
            is VektorFeld.Rechnung -> {
                val a=wert(feld.links,k); val b=wert(feld.rechts,k)
                when(feld.operation) { VektorOperation.ADDIEREN -> a+b; VektorOperation.SUBTRAHIEREN -> a-b }
            }
            is VektorFeld.Skalieren -> wert(feld.vektor,k)*wert(feld.faktor,k)
            is AuswahlFeld.Konstant -> feld.wert
            is AuswahlFeld.Vergleich -> {
                val a=wert(feld.links,k); val b=wert(feld.rechts,k)
                when(feld.operation) { VergleichsOperation.KLEINER -> a<b; VergleichsOperation.GROESSER -> a>b; VergleichsOperation.GLEICH -> a==b }
            }
        }
        return ergebnis as T
    }
}

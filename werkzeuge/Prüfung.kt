import de.TeutonStudio.MathematikRechenSystem.kern.*

fun main() {
    val halb = RationaleZahl.von(1, 2)
    check(halb + RationaleZahl.von(1, 3) == RationaleZahl.von(5, 6))

    val x = Variable("x")
    check(
        addition(addition(x, RationaleZahl.von(2)), RationaleZahl.von(3)) ==
            addition(x, RationaleZahl.von(5)),
    )
    check(
        löseLinear(
            Gleichheit(
                addition(multiplikation(RationaleZahl.von(2), x), RationaleZahl.von(4)),
                RationaleZahl.von(10),
            ),
            x,
        ).lösungen.single() == RationaleZahl.von(3),
    )

    val matrix = Matrix(
        listOf(
            listOf(RationaleZahl.von(2), RationaleZahl.Null),
            listOf(RationaleZahl.Null, RationaleZahl.von(4)),
        ),
    )
    check(matrix.inverseRational().zeilen[1][1] == RationaleZahl.von(1, 4))

    val methode = Methode(
        name = "f",
        parameter = listOf(x),
        vorschrift = Potenz(x, RationaleZahl.von(2)),
        zielMenge = ReelleZahlen,
        werteVorräte = mapOf("x" to ReelleZahlen),
    )
    check(totaleAbleitung(methode).zuLatex().contains("f"))

    println("Alle Kernprüfungen erfolgreich.")
}

package de.TeutonStudio.TypSystem

/**
 * Rein semantische Beschreibung einer kompakten Typgrafik. Die UI entscheidet selbst,
 * wie Farben, Glyphen und Streifen konkret gerendert werden. Dadurch bleibt der Typkern
 * sowohl für Compose als auch für spätere Godot-/Desktop-Adapter nutzbar.
 */
enum class TypVisualMuster { Einfach, Zusammengesetzt, Gestreift }

data class TypVisualSegment(
    val schlüssel: String,
    val label: String,
)

data class TypVisualDescriptor(
    val kurzLabel: String,
    val tooltip: String = kurzLabel,
    val muster: TypVisualMuster = TypVisualMuster.Einfach,
    val segmente: List<TypVisualSegment> = emptyList(),
)

/**
 * Erzeugt eine domänenneutrale, kompakte Darstellung. Domänen können [nameFür]
 * überschreiben, um stabile Typ-IDs in mathematische Glyphen oder Godot-Icons zu übersetzen.
 */
fun TypAusdruck.zuVisualDescriptor(
    nameFür: (TypId) -> String = { id -> id.wert.substringAfterLast('.') },
): TypVisualDescriptor = when (this) {
    TypAusdruck.Beliebig -> TypVisualDescriptor("*", "Beliebiger Typ")
    TypAusdruck.Unbekannt -> TypVisualDescriptor("?", "Unbekannter Typ")
    is TypAusdruck.Atom -> TypVisualDescriptor(nameFür(id), id.wert)
    is TypAusdruck.Literal -> TypVisualDescriptor(wert)
    is TypAusdruck.Variable -> TypVisualDescriptor(id.wert, "Typvariable ${id.wert}")
    is TypAusdruck.Vereinigung -> {
        val teile = alternativen.map { alternative -> alternative.zuVisualDescriptor(nameFür) }
        TypVisualDescriptor(
            kurzLabel = teile.joinToString(" ∨ ") { it.kurzLabel },
            tooltip = teile.joinToString(" oder ") { it.tooltip },
            muster = TypVisualMuster.Gestreift,
            segmente = teile.mapIndexed { index, teil ->
                TypVisualSegment("union.$index", teil.kurzLabel)
            },
        )
    }
    is TypAusdruck.Parameterisiert -> {
        val teile = argumente.map { it.zuVisualDescriptor(nameFür) }
        val konstruktorName = nameFür(konstruktor)
        val kurz = when {
            konstruktor.wert == "typ.tupel" ->
                teile.joinToString(prefix = "(", postfix = ")") { it.kurzLabel }
            konstruktor.wert.endsWith("methode") && teile.size == 2 ->
                "${teile[0].kurzLabel} → ${teile[1].kurzLabel}"
            else -> "$konstruktorName<${teile.joinToString { it.kurzLabel }}>"
        }
        TypVisualDescriptor(
            kurzLabel = kurz,
            tooltip = kurz,
            muster = TypVisualMuster.Zusammengesetzt,
            segmente = teile.mapIndexed { index, teil ->
                TypVisualSegment("$konstruktorName.$index", teil.kurzLabel)
            },
        )
    }
}

package de.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Fachneutrale Beschreibung einer kompakten Typgrafik am Anschluss.
 * Die Darstellung enthält bewusst keine Compose-Farben oder Icons; Plattform-
 * und Fachmodule können die stabilen Segmentschlüssel auf konkrete Glyphen,
 * Farben und Miniaturen abbilden.
 */
data class TypVisualSegment(
    val schlüssel: String,
    val kurztext: String,
)

data class TypVisualDescriptor(
    val kurztext: String,
    val tooltip: String = kurztext,
    val segmente: List<TypVisualSegment> = emptyList(),
) {
    /** Mehr als ein Segment wird am Handle als Oder-/Mehrfarbtyp dargestellt. */
    val istMehrfachTyp: Boolean get() = segmente.size > 1
}

fun interface TypVisualResolver {
    fun beschreibe(typ: TypAusdruck): TypVisualDescriptor
}

/** Konservativer Resolver für Domänen, die noch keine eigene Typnotation registriert haben. */
object StandardTypVisualResolver : TypVisualResolver {
    override fun beschreibe(typ: TypAusdruck): TypVisualDescriptor = when (typ) {
        TypAusdruck.Beliebig -> TypVisualDescriptor("*", "Beliebiger Typ")
        TypAusdruck.Unbekannt -> TypVisualDescriptor("?", "Unbekannter Typ")
        is TypAusdruck.Atom -> TypVisualDescriptor(
            kurztext = typ.id.wert.substringAfterLast('.'),
            tooltip = typ.id.wert,
            segmente = listOf(TypVisualSegment(typ.id.wert, typ.id.wert.substringAfterLast('.'))),
        )
        is TypAusdruck.Parameterisiert -> {
            val argumente = typ.argumente.joinToString(",") { beschreibe(it).kurztext }
            TypVisualDescriptor(
                kurztext = "${typ.konstruktor.wert.substringAfterLast('.')}<$argumente>",
                tooltip = typ.toString(),
                segmente = listOf(TypVisualSegment(typ.konstruktor.wert, typ.konstruktor.wert.substringAfterLast('.'))),
            )
        }
        is TypAusdruck.Vereinigung -> {
            val teile = typ.alternativen.map(::beschreibe)
            TypVisualDescriptor(
                kurztext = teile.joinToString(" ∨ ") { it.kurztext },
                tooltip = teile.joinToString(" oder ") { it.tooltip },
                segmente = teile.flatMap { descriptor ->
                    descriptor.segmente.ifEmpty {
                        listOf(TypVisualSegment(descriptor.kurztext, descriptor.kurztext))
                    }
                }.distinctBy(TypVisualSegment::schlüssel),
            )
        }
        is TypAusdruck.Variable -> TypVisualDescriptor(typ.id.wert, "Typvariable ${typ.id.wert}")
    }
}

package de.TeutonStudio.TypSystem

/**
 * Domänenneutrale Beschreibung einer kompakten Typgrafik.
 *
 * Die UI entscheidet selbst, wie Symbol, Tupel, Pfeil oder gestreifte Vereinigung
 * gezeichnet werden. Damit bleibt das TypSystem frei von Compose, ReactFlow oder
 * einer späteren Godot-Editoroberfläche.
 */
sealed interface TypMiniGrafik {
    data class Symbol(val text: String) : TypMiniGrafik
    data class Tupel(val elemente: List<TypMiniGrafik>) : TypMiniGrafik
    data class Pfeil(val quelle: TypMiniGrafik, val ziel: TypMiniGrafik) : TypMiniGrafik
    data class Parameter(val konstruktor: String, val argumente: List<TypMiniGrafik>) : TypMiniGrafik
    /** Die UI rendert Alternativen bevorzugt als diagonale Streifen analog Orchestrator. */
    data class Vereinigung(val alternativen: List<TypMiniGrafik>) : TypMiniGrafik
    data class Unbekannt(val text: String = "?") : TypMiniGrafik
}

data class TypVisualDescriptor(
    val kurzLabel: String,
    val tooltip: String = kurzLabel,
    val grafik: TypMiniGrafik,
)

fun interface TypVisualResolver {
    fun beschreibe(typ: TypAusdruck): TypVisualDescriptor
}

/** Konservativer Fallback für Domänen ohne eigene mathematische Notation. */
object StandardTypVisualResolver : TypVisualResolver {
    override fun beschreibe(typ: TypAusdruck): TypVisualDescriptor = when (typ) {
        TypAusdruck.Beliebig -> TypVisualDescriptor("*", "Beliebiger Typ", TypMiniGrafik.Symbol("*"))
        TypAusdruck.Unbekannt -> TypVisualDescriptor("?", "Unbekannter Typ", TypMiniGrafik.Unbekannt())
        is TypAusdruck.Atom -> TypVisualDescriptor(typ.id.wert, grafik = TypMiniGrafik.Symbol(typ.id.wert))
        is TypAusdruck.Literal -> TypVisualDescriptor(typ.wert, grafik = TypMiniGrafik.Symbol(typ.wert))
        is TypAusdruck.Variable -> TypVisualDescriptor(typ.id.wert, grafik = TypMiniGrafik.Symbol(typ.id.wert))
        is TypAusdruck.Parameterisiert -> {
            val kinder = typ.argumente.map(::beschreibe)
            val label = "${typ.konstruktor.wert}<${kinder.joinToString(",") { it.kurzLabel }}>"
            TypVisualDescriptor(
                kurzLabel = label,
                grafik = TypMiniGrafik.Parameter(typ.konstruktor.wert, kinder.map { it.grafik }),
            )
        }
        is TypAusdruck.Vereinigung -> {
            val kinder = typ.alternativen.map(::beschreibe)
            val label = kinder.joinToString(" ∨ ") { it.kurzLabel }
            TypVisualDescriptor(
                kurzLabel = label,
                grafik = TypMiniGrafik.Vereinigung(kinder.map { it.grafik }),
            )
        }
    }
}

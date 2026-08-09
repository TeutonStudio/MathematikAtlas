package de.TeutonStudio.MathematikAtlas

internal enum class EinstellungsSeiteId {
    Darstellung,
    Beispielkarten,
    Ueber,
}

internal data class EinstellungsSeiteDefinition(
    val id: EinstellungsSeiteId,
    val titel: String,
    val pfad: List<String>,
    val reihenfolge: Int,
)

internal val standardEinstellungsSeiten = listOf(
    EinstellungsSeiteDefinition(
        id = EinstellungsSeiteId.Darstellung,
        titel = "Darstellung",
        pfad = emptyList(),
        reihenfolge = 100,
    ),
    EinstellungsSeiteDefinition(
        id = EinstellungsSeiteId.Beispielkarten,
        titel = "Beispielkarten",
        pfad = listOf("Karten"),
        reihenfolge = 200,
    ),
    EinstellungsSeiteDefinition(
        id = EinstellungsSeiteId.Ueber,
        titel = "Über",
        pfad = listOf("Anwendung"),
        reihenfolge = 300,
    ),
)

internal sealed interface EinstellungsNavigationsElement {
    val reihenfolge: Int

    data class Seite(
        val definition: EinstellungsSeiteDefinition,
    ) : EinstellungsNavigationsElement {
        override val reihenfolge: Int = definition.reihenfolge
    }

    data class Ordner(
        /** Vollständiger logischer Pfad bis zum letzten zusammengefassten Ordner. */
        val pfad: List<String>,
        /** Nur die für diesen kompakten IDE-Pfad zusammengefassten Segmente. */
        val anzeigePfad: List<String>,
        val inhalt: EinstellungsNavigationsebene,
        override val reihenfolge: Int,
    ) : EinstellungsNavigationsElement {
        val titel: String get() = anzeigePfad.joinToString(" / ")
    }
}

internal data class EinstellungsNavigationsebene(
    val elemente: List<EinstellungsNavigationsElement>,
)

private class RohOrdner(
    val name: String,
    val pfad: List<String>,
) {
    val seiten = mutableListOf<EinstellungsSeiteDefinition>()
    val kinder = linkedMapOf<String, RohOrdner>()
}

internal fun baueEinstellungsNavigation(
    seiten: List<EinstellungsSeiteDefinition>,
): EinstellungsNavigationsebene {
    val wurzel = RohOrdner(name = "", pfad = emptyList())

    seiten.forEach { seite ->
        var ordner = wurzel
        seite.pfad.forEach { segment ->
            val pfad = ordner.pfad + segment
            ordner = ordner.kinder.getOrPut(segment) { RohOrdner(segment, pfad) }
        }
        ordner.seiten += seite
    }

    class NavigationBauer {
        fun baueEbene(rohknoten: RohOrdner): EinstellungsNavigationsebene {
            val elemente = buildList {
                addAll(rohknoten.seiten.map { seite -> EinstellungsNavigationsElement.Seite(seite) })
                addAll(rohknoten.kinder.values.map(::komprimiereOrdner))
            }.sortedWith(compareBy(EinstellungsNavigationsElement::reihenfolge, { element ->
                when (element) {
                    is EinstellungsNavigationsElement.Seite -> element.definition.titel
                    is EinstellungsNavigationsElement.Ordner -> element.titel
                }
            }))
            return EinstellungsNavigationsebene(elemente)
        }

        private fun komprimiereOrdner(start: RohOrdner): EinstellungsNavigationsElement.Ordner {
            val sichtbareSegmente = mutableListOf(start.name)
            var ende = start

            while (ende.seiten.isEmpty() && ende.kinder.size == 1) {
                val einzigesKind = ende.kinder.values.single()
                sichtbareSegmente += einzigesKind.name
                ende = einzigesKind
            }

            val inhalt = baueEbene(ende)
            return EinstellungsNavigationsElement.Ordner(
                pfad = ende.pfad,
                anzeigePfad = sichtbareSegmente,
                inhalt = inhalt,
                reihenfolge = inhalt.elemente.minOfOrNull(EinstellungsNavigationsElement::reihenfolge) ?: Int.MAX_VALUE,
            )
        }
    }

    return NavigationBauer().baueEbene(wurzel)
}

internal fun EinstellungsNavigationsebene.alleOrdnerPfade(): Set<List<String>> = buildSet {
    fun besuche(ebene: EinstellungsNavigationsebene) {
        ebene.elemente.forEach { element ->
            if (element is EinstellungsNavigationsElement.Ordner) {
                add(element.pfad)
                besuche(element.inhalt)
            }
        }
    }
    besuche(this@alleOrdnerPfade)
}

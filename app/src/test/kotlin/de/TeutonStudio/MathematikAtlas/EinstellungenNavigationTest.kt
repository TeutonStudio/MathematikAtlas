package de.TeutonStudio.MathematikAtlas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EinstellungenNavigationTest {
    @Test
    fun `einkindige Ordnerketten werden als IDE Pfad komprimiert`() {
        val seite = EinstellungsSeiteDefinition(
            id = EinstellungsSeiteId.Beispielkarten,
            titel = "Knotensammlungen",
            pfad = listOf("Inhalte", "Quellen", "Offiziell"),
            reihenfolge = 100,
        )

        val navigation = baueEinstellungsNavigation(listOf(seite))
        val ordner = assertIs<EinstellungsNavigationsElement.Ordner>(navigation.elemente.single())

        assertEquals(listOf("Inhalte", "Quellen", "Offiziell"), ordner.anzeigePfad)
        assertEquals(listOf("Inhalte", "Quellen", "Offiziell"), ordner.pfad)
        val enthalteneSeite = assertIs<EinstellungsNavigationsElement.Seite>(ordner.inhalt.elemente.single())
        assertEquals("Knotensammlungen", enthalteneSeite.definition.titel)
    }

    @Test
    fun `Verzweigungen bleiben echte Hierarchie`() {
        val navigation = baueEinstellungsNavigation(
            listOf(
                EinstellungsSeiteDefinition(
                    id = EinstellungsSeiteId.Beispielkarten,
                    titel = "Knotensammlungen",
                    pfad = listOf("Inhalte", "Quellen"),
                    reihenfolge = 100,
                ),
                EinstellungsSeiteDefinition(
                    id = EinstellungsSeiteId.Ueber,
                    titel = "Sicherheit",
                    pfad = listOf("Inhalte", "Sicherheit"),
                    reihenfolge = 200,
                ),
            ),
        )

        val inhalte = assertIs<EinstellungsNavigationsElement.Ordner>(navigation.elemente.single())
        assertEquals(listOf("Inhalte"), inhalte.anzeigePfad)
        assertEquals(
            listOf("Quellen", "Sicherheit"),
            inhalte.inhalt.elemente.map { element ->
                assertIs<EinstellungsNavigationsElement.Ordner>(element).titel
            },
        )
    }

    @Test
    fun `Standardseiten behalten bestehende und neue Bereiche`() {
        val navigation = baueEinstellungsNavigation(standardEinstellungsSeiten)

        assertEquals(
            setOf(EinstellungsSeiteId.Darstellung, EinstellungsSeiteId.Beispielkarten, EinstellungsSeiteId.Ueber),
            navigation.alleSeiten(),
        )
    }

    private fun EinstellungsNavigationsebene.alleSeiten(): Set<EinstellungsSeiteId> = buildSet {
        fun besuche(ebene: EinstellungsNavigationsebene) {
            ebene.elemente.forEach { element ->
                when (element) {
                    is EinstellungsNavigationsElement.Seite -> add(element.definition.id)
                    is EinstellungsNavigationsElement.Ordner -> besuche(element.inhalt)
                }
            }
        }
        besuche(this@alleSeiten)
    }
}

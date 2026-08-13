package de.TeutonStudio.KnotenKartenVerwalter.daten

import de.TeutonStudio.TypSystem.AnschlussVertrag
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypId
import kotlin.test.Test
import kotlin.test.assertEquals

class KartenSchnittstellenSignaturTest {
    private val zahlTyp = TypAusdruck.Atom(TypId("mathematik.zahl"))

    @Test
    fun `leere Karte besitzt Tupel leer nach Tupel leer als Grenzvertrag`() {
        val signatur = KartenDaten(name = "Leer").schnittstellenSignatur()

        assertEquals(TypAusdruck.Parameterisiert(TypId("mathematik.tupel"), emptyList()), signatur.eingangTyp)
        assertEquals(TypAusdruck.Parameterisiert(TypId("mathematik.tupel"), emptyList()), signatur.ausgangTyp)
    }

    @Test
    fun `Umbenennen aendert die stabile Komponentenidentitaet nicht`() {
        val id = KnotenId("grenze-1")
        val alt = grenze(id, "mathematik.kartenEingang", "x", AnschlussRichtung.Ausgang)
        val neu = alt.copy(parameter = mapOf("name" to "umbenannt"))

        val vorher = KartenDaten(name = "K", knoten = listOf(alt)).schnittstellenSignatur().eingang.single()
        val nachher = KartenDaten(name = "K", knoten = listOf(neu)).schnittstellenSignatur().eingang.single()

        assertEquals(vorher.id, nachher.id)
        assertEquals("x", vorher.name)
        assertEquals("umbenannt", nachher.name)
        assertEquals(zahlTyp, nachher.typ)
    }

    @Test
    fun `Reihenfolge ist explizite Projektion und IDs bleiben beim Umsortieren erhalten`() {
        val a = grenze(KnotenId("a"), "mathematik.kartenAusgang", "a", AnschlussRichtung.Eingang, GraphPunkt(0f, 0f))
        val b = grenze(KnotenId("b"), "mathematik.kartenAusgang", "b", AnschlussRichtung.Eingang, GraphPunkt(0f, 10f))
        val vorher = KartenDaten(name = "K", knoten = listOf(a, b)).schnittstellenSignatur().ausgang
        val nachher = KartenDaten(name = "K", knoten = listOf(a.copy(position = GraphPunkt(0f, 20f)), b)).schnittstellenSignatur().ausgang

        assertEquals(listOf("a", "b"), vorher.map { it.id })
        assertEquals(listOf("b", "a"), nachher.map { it.id })
        assertEquals(setOf("a", "b"), nachher.map { it.id }.toSet())
        assertEquals(listOf(0, 1), nachher.map { it.position })
    }

    private fun grenze(
        id: KnotenId,
        art: String,
        name: String,
        richtung: AnschlussRichtung,
        position: GraphPunkt = GraphPunkt.Zero,
    ) = KnotenDaten(
        id = id,
        art = art,
        name = name,
        position = position,
        parameter = mapOf("name" to name),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "wert",
                richtung = richtung,
                kante = if (richtung == AnschlussRichtung.Ausgang) AnschlussKante.Rechts else AnschlussKante.Links,
                art = AnschlussArtId("mathematik.zahl"),
                vertrag = AnschlussVertrag(zahlTyp),
            ),
        ),
    )
}

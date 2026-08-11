package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
import org.junit.Assert.*
import org.junit.Test

class SemantischeTypMigrationTest {
    @Test
    fun alte_anschlussart_wird_ohne_verlust_in_semantischen_typ_gespiegelt() {
        val anschluss = AnschlussDaten(
            id = AnschlussId("a"),
            name = "wert",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Zahl.id,
        )
        val karte = KartenDaten(
            id = KartenId("alt"),
            name = "Alt",
            knoten = listOf(KnotenDaten(id = KnotenId("k"), art = "test", name = "Test", anschlüsse = listOf(anschluss))),
        )

        val migriert = karte.migriereSemantischeTypverträge()
        val port = migriert.knoten.single().anschlüsse.single()
        assertEquals(TypAusdruck.Atom(TypId(MathematikAnschlussArten.Zahl.id.wert)), port.vertrag.typ)
        assertEquals(anschluss.art, port.art)
    }

    @Test
    fun oder_anschluss_wird_semantische_vereinigung() {
        val anschluss = AnschlussDaten(
            name = "wert",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Objekt.id,
            zulässigeArten = setOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Menge.id),
        )

        val migriert = anschluss.mitSemantischemStandardvertrag()
        val union = migriert.vertrag.typ as TypAusdruck.Vereinigung
        assertEquals(
            setOf(MathematikAnschlussArten.Zahl.id.wert, MathematikAnschlussArten.Menge.id.wert),
            union.alternativen.filterIsInstance<TypAusdruck.Atom>().map { it.id.wert }.toSet(),
        )
    }

    @Test
    fun kanonischer_katalog_liefert_typvertraege_bereits_vor_dem_speichern() {
        val vorlagen = KanonischerMathematikKnotenKatalog.alle()
        assertTrue(vorlagen.isNotEmpty())
        assertTrue(vorlagen.flatMap { it.anschlüsse }.isNotEmpty())
        assertTrue(
            vorlagen.flatMap { it.anschlüsse }.all { anschluss ->
                anschluss.vertrag.typ != TypAusdruck.Unbekannt
            },
        )
    }
}

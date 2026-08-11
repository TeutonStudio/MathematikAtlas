package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.TypSystem.*
import kotlin.test.*

class SemantischeTypMigrationTest {
    @Test
    fun `einfache Anschlussart wird semantischer Atomtyp`() {
        val anschluss = AnschlussDaten(
            name = "wert",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = MathematikAnschlussArten.Zahl.id,
        )

        val migriert = anschluss.migriereSemantischenTyp()

        assertEquals(
            TypAusdruck.Atom(TypId(MathematikAnschlussArten.Zahl.id.wert)),
            migriert.vertrag.typ,
        )
    }

    @Test
    fun `Oder Anschluss wird echter Vereinigungstyp und behaelt Legacy Arten`() {
        val erlaubte = setOf(MathematikAnschlussArten.Zahl.id, MathematikAnschlussArten.Methode.id)
        val anschluss = AnschlussDaten(
            name = "wert",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Objekt.id,
            zulässigeArten = erlaubte,
        )

        val migriert = anschluss.migriereSemantischenTyp()
        val typ = assertIs<TypAusdruck.Vereinigung>(migriert.vertrag.typ)

        assertEquals(erlaubte, migriert.zulässigeArten)
        assertEquals(
            erlaubte.map { TypAusdruck.Atom(TypId(it.wert)) }.toSet(),
            typ.alternativen.toSet(),
        )
    }

    @Test
    fun `Art folgt Eingang wird auch semantische Typinferenz`() {
        val anschluss = AnschlussDaten(
            name = "wert",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = MathematikAnschlussArten.Objekt.id,
            artFolgtEingang = "eingang",
        )

        val migriert = anschluss.migriereSemantischenTyp()

        assertEquals(TypInferenzRegel.FolgtEingang("eingang"), migriert.typInferenz)
    }

    @Test
    fun `Migration ist idempotent und ueberschreibt expliziten Vertrag nicht`() {
        val explizit = AnschlussVertrag(TypAusdruck.Atom(TypId("fach.spezial")))
        val anschluss = AnschlussDaten(
            name = "wert",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = MathematikAnschlussArten.Objekt.id,
            vertrag = explizit,
        )

        val einmal = anschluss.migriereSemantischenTyp()
        val zweimal = einmal.migriereSemantischenTyp()

        assertEquals(explizit, einmal.vertrag)
        assertEquals(einmal, zweimal)
    }
}

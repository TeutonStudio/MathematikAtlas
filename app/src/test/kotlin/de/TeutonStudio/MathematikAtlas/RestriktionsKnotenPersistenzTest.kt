package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import de.TeutonStudio.MathematikKnoten.METHODEN_BEREICHS_OPERATOR_ANPASSUNG
import de.TeutonStudio.MathematikKnoten.METHODEN_BEREICHS_OPERATOR_RESTRIKTION
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikKnoten.RESTRIKTIONS_ERGÄNZUNG_PREFIX
import de.TeutonStudio.MathematikKnoten.RESTRIKTIONS_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.RestriktionsKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.methodenBereichsOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RestriktionsKnotenPersistenzTest {
    @Test
    fun `Bereichsanpassung behaelt dynamischen Anschluss Identitaet und Reihenfolge im JSON`() {
        val ergänzung = AnschlussDaten(
            name = "${RESTRIKTIONS_ERGÄNZUNG_PREFIX}0",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Methode.id,
            reihenfolge = 2,
            dynamischErzeugt = true,
        )
        val grund = RestriktionsKnotenVorlagen.Bereichsanpassung.erzeuge(GraphPunkt(40f, 80f))
        val knoten = grund.copy(
            größe = GraphGröße(360f, 190f),
            anschlüsse = grund.anschlüsse + ergänzung,
        )

        val gelesen = KartenJson.lese(
            KartenJson.schreibe(KartenDaten(name = "Bereichsanpassungstest", knoten = listOf(knoten))),
        ).knoten.single()

        assertEquals(RESTRIKTIONS_KNOTEN_ART, gelesen.art)
        assertEquals(METHODEN_BEREICHS_OPERATOR_ANPASSUNG, gelesen.methodenBereichsOperator())
        assertEquals(GraphGröße(360f, 190f), gelesen.größe)
        val gelesenErgänzung = assertNotNull(
            gelesen.anschlüsse.singleOrNull { it.name == ergänzung.name },
        )
        assertEquals(ergänzung.id, gelesenErgänzung.id)
        assertEquals(2, gelesenErgänzung.reihenfolge)
        assertEquals(MathematikAnschlussArten.Methode.id, gelesenErgänzung.art)
        assertTrue(gelesenErgänzung.dynamischErzeugt)
    }

    @Test
    fun `reine Restriktion persistiert ohne Ergaenzungsanschluesse`() {
        val knoten = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)

        val gelesen = KartenJson.lese(
            KartenJson.schreibe(KartenDaten(name = "Restriktionstest", knoten = listOf(knoten))),
        ).knoten.single()

        assertEquals(METHODEN_BEREICHS_OPERATOR_RESTRIKTION, gelesen.methodenBereichsOperator())
        assertTrue(gelesen.anschlüsse.none { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) })
    }

    @Test
    fun `historischer freier Ergaenzungshandle wird bei reiner Restriktion entfernt`() {
        val grund = RestriktionsKnotenVorlagen.Restriktion.erzeuge(GraphPunkt.Zero)
        val ergänzung = AnschlussDaten(
            name = "${RESTRIKTIONS_ERGÄNZUNG_PREFIX}0",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Methode.id,
            reihenfolge = 2,
            dynamischErzeugt = true,
        )
        val historisch = grund.copy(
            parameter = emptyMap(),
            anschlüsse = grund.anschlüsse + ergänzung,
        )

        val gelesen = KartenJson.lese(
            KartenJson.schreibe(KartenDaten(name = "Historisch", knoten = listOf(historisch))),
        ).knoten.single()

        assertEquals(METHODEN_BEREICHS_OPERATOR_RESTRIKTION, gelesen.methodenBereichsOperator())
        assertTrue(gelesen.anschlüsse.none { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) })
    }
}

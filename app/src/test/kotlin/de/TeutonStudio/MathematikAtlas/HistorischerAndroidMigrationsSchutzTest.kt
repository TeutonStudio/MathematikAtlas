package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import kotlin.test.*

class HistorischerAndroidMigrationsSchutzTest {
    private fun android(karte: KartenDaten) = migriereAssoziativeKnoten(migriereAussagenOperatoren(karte)).migriereKonsolidierteKnoten()

    @Test fun `Android Nachmigration bewahrt doppelte Kantenidentitaeten samt Reihenfolge`() {
        val quelle=MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val ziel=MathematikKnotenVorlagen.Kehrwert.erzeuge(GraphPunkt.Zero)
        val edge=VerbindungDaten(von=AnschlussVerweis(quelle.id,quelle.anschlüsse.single().id),zu=AnschlussVerweis(ziel.id,ziel.anschlüsse.first().id))
        for(zweite in listOf(edge,edge.copy(eingangsReihenfolge=8,eingangsFallback="unbekannt@karte:fest"))) {
            val original=KartenDaten(name="Doppelte Kanten",knoten=listOf(quelle,ziel),verbindungen=listOf(edge,zweite))
            val geladen=MathematikKartenCodec.lade(KartenDatenJson.schreibe(original))
            val neu=android(geladen)
            assertEquals(original.verbindungen,neu.verbindungen)
            assertEquals(geladen.knoten,neu.knoten)
            assertEquals(neu,android(neu))
        }
    }
    @Test fun `Android Nachmigration behaelt gesperrten Originalknoten mit unbekanntem externem Endpunkt`() {
        val alt=MathematikKnotenVorlagen.Division.erzeuge(GraphPunkt.Zero).let {it.copy(parameter=it.parameter+(RECHNER_MIGRATION_DIAGNOSE to "Konkrete Sperre"))}
        val edge=VerbindungDaten(von=AnschlussVerweis(KnotenId("extern-fehlt"),AnschlussId("fehlt")),zu=AnschlussVerweis(alt.id,alt.anschlüsse.first().id),eingangsReihenfolge=6,eingangsFallback="dividend@karte:fest")
        val original=KartenDaten(name="Original",knoten=listOf(alt),verbindungen=listOf(edge))
        assertEquals(original,android(original))
        assertEquals(original,android(MathematikKartenCodec.lade(KartenDatenJson.schreibe(original))))
    }
}

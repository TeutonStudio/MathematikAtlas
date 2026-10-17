package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
import de.TeutonStudio.MathematikKnoten.katalog.OperatorKnotenSuchindex
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class KatalogMigrationSicherheitTest {
    @Test fun `Raumtypen sind gemeinsam sichtbar und Geometriealtarten nur Suchvorlagen`() {
        val katalog = KanonischerMathematikKnotenKatalog.alle()
        assertEquals(121, katalog.map { it.art }.toSet().size)
        assertTrue(katalog.map { it.art }.containsAll(MengenraumKnotenVorlagen.alle.filterNot { it.art in mengenKonsolidierungAltarten }.map { it.art }))
        val alt = GeometrieKnotenVorlagen.alle.filter { it.art in PraedikatKnotenMigration.alteKnotenArten }
        assertEquals(8, alt.size)
        alt.forEach { vorlage ->
            assertFalse(katalog.any { it.art == vorlage.art })
            val id = PraedikatKnotenMigration.operatorId(vorlage.erzeuge(GraphPunkt.Zero))
            assertTrue(OperatorKnotenSuchindex.alleMitMetadaten(katalog).any {
                it.operatorId == id && it.vorlage.art == RelationsOperatoren.KNOTEN_ART
            })
        }
    }

    @Test fun `alle Geometrierelationen migrieren ohne Identitaetsverlust`() {
        GeometrieKnotenVorlagen.alle.filter { it.art in PraedikatKnotenMigration.alteKnotenArten }.forEach { vorlage ->
            val alt = vorlage.erzeuge(GraphPunkt.Zero).copy(name = "Mein geometrischer Test")
            val karte = KartenDaten(name = "Geometrie", knoten = listOf(alt))
            val neu = karte.migrierePraedikatKnoten()
            assertEquals(RelationsOperatoren.KNOTEN_ART, neu.knoten.single().art)
            assertEquals(alt.id, neu.knoten.single().id)
            assertEquals(alt.name, neu.knoten.single().name)
            assertEquals(alt.anschlüsse.map { it.id }.toSet(), neu.knoten.single().anschlüsse.map { it.id }.toSet())
            assertEquals(neu, neu.migrierePraedikatKnoten())
        }
    }

    @Test fun `Inzidenzmigration remappt Standards und Kartenfallbacks mit Verbindungen`() {
        val verweis = KartenVerweis(KartenId("punktquelle"), 7)
        val alt = GeometrieKnotenVorlagen.Inzidenz.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("standardwert.links" to "quelle"), eingangsKartenVerweise = mapOf("links" to verweis))
        val port = alt.anschlüsse.first { it.name == "links" }
        val verbindung = VerbindungDaten(von = AnschlussVerweis(KnotenId("quelle"), AnschlussId("aus")),
            zu = AnschlussVerweis(alt.id, port.id), eingangsReihenfolge = 4)
        val karte = KartenDaten(name = "Inzidenz", knoten = listOf(alt), verbindungen = listOf(verbindung))
        val neu = karte.migrierePraedikatKnoten()
        assertEquals(karte.verbindungen, neu.verbindungen)
        assertEquals("punkt", neu.knoten.single().anschlüsse.single { it.id == port.id }.name)
        assertEquals("quelle", neu.knoten.single().parameter["standardwert.punkt"])
        assertEquals(mapOf("punkt" to verweis), neu.knoten.single().eingangsKartenVerweise)
        assertEquals(neu, MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)))
    }

    @Test fun `mehrdeutige Ports bleiben erhalten und sind nicht auswertbar`() {
        val basis = GeometrieKnotenVorlagen.Inzidenz.erzeuge(GraphPunkt.Zero)
        val extra = basis.anschlüsse.first().copy(id = AnschlussId("extra"), name = "extra")
        val alt = basis.copy(anschlüsse = basis.anschlüsse + extra)
        val karte = KartenDaten(name = "Unklar", knoten = listOf(alt))
        val neu = karte.migrierePraedikatKnoten()
        val gesperrt = neu.knoten.single()
        assertEquals(alt, gesperrt.copy(parameter = alt.parameter))
        assertNotNull(gesperrt.parameter[RECHNER_MIGRATION_DIAGNOSE])
        assertEquals(neu, neu.migrierePraedikatKnoten())
        val register = GesamterMathematikAuswerter.erzeugeRegister()
        val direkt = assertNotNull(register.finde(gesperrt.art)).auswerten(
            KnotenAuswertungsKontext(gesperrt, emptyMap(), RechenKontext()))
        assertTrue(direkt.ausgaben.isEmpty())
        assertTrue(direkt.fehler.orEmpty().startsWith("Migrationsdiagnose:"))
        val graph = KartenAuswerter(register).auswerten(neu).knoten.getValue(gesperrt.id)
        assertEquals(direkt.fehler, graph.fehler)
        assertEquals(neu, MathematikKartenCodec.importiere(MathematikKartenCodec.schreibe(neu)))
    }

    @Test fun `mehrdeutige Rollen kollidierender Quellen werden nicht umgeschrieben`() {
        val alt = GeometrieKnotenVorlagen.Inzidenz.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("standardwert.links" to "a", "standardwert.punkt" to "b"))
        val neu = KartenDaten(name = "Kollision", knoten = listOf(alt)).migrierePraedikatKnoten().knoten.single()
        assertEquals(alt, neu.copy(parameter = alt.parameter))
        assertNotNull(neu.parameter[RECHNER_MIGRATION_DIAGNOSE])
    }

    @Test fun `abgelehnte Rechnermigration erreicht keinen alten Mathematikauswerter`() {
        val alt = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf(RECHNER_MIGRATION_DIAGNOSE to "Nicht eindeutig"))
        val register = GesamterMathematikAuswerter.erzeugeRegister()
        val wert = KartenAuswerter(register).auswerten(KartenDaten(name = "Sperre", knoten = listOf(alt))).knoten.getValue(alt.id)
        assertTrue(wert.ausgaben.isEmpty())
        assertEquals("Migrationsdiagnose: Nicht eindeutig", wert.fehler)
    }
    @Test fun `falsche Arten und Ableitungen werden nicht zu Geometrie umgedeutet`() {
        val basis = GeometrieKnotenVorlagen.Inzidenz.erzeuge(GraphPunkt.Zero)
        val falsch = basis.copy(anschlüsse = basis.anschlüsse.map {
            if (it.name == "links") it.copy(art = MathematikAnschlussArten.Zahl.id) else it
        })
        val ableitung = basis.copy(anschlüsse = basis.anschlüsse.map {
            if (it.richtung == AnschlussRichtung.Ausgang) it.copy(artFolgtEingang = "links") else it
        })
        val historischeMethode = basis.copy(anschlüsse = basis.anschlüsse.map {
            if (it.name == "links") it.copy(art = AnschlussArtId("mathematik.funktion.zahl")) else it
        })
        listOf(falsch, ableitung, historischeMethode).forEach { alt ->
            val neu = KartenDaten(name = "Typfehler", knoten = listOf(alt)).migrierePraedikatKnoten().knoten.single()
            assertEquals(alt, neu.copy(parameter = alt.parameter))
            assertNotNull(neu.parameter[RECHNER_MIGRATION_DIAGNOSE])
            val json = KartenDatenJson.schreibe(KartenDaten(name = "Typfehler", knoten = listOf(alt)))
            listOf(MathematikKartenCodec::lese, MathematikKartenCodec::lade, MathematikKartenCodec::importiere).forEach { lese ->
                assertEquals(neu, lese(json).knoten.single())
            }
            assertEquals(neu, MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(
                KartenDaten(name = "Typfehler", knoten = listOf(neu)))).knoten.single())
        }
    }

    @Test fun `kanonische Bezeichnung umgeht keine Mehrfach oder ID Pruefung`() {
        val basis = PraedikatKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val doppelt = basis.copy(anschlüsse = basis.anschlüsse.mapIndexed { i, p ->
            if (i == 1) p.copy(id = basis.anschlüsse[0].id) else p
        })
        val mehrfach = basis.copy(anschlüsse = basis.anschlüsse.mapIndexed { i, p ->
            if (i == 0) p.copy(belegung = AnschlussBelegung.Mehrfach) else p
        })
        listOf(doppelt, mehrfach).forEach { alt ->
            val neu = KartenDaten(name = "Ungültig", knoten = listOf(alt)).migrierePraedikatKnoten().knoten.single()
            assertEquals(alt, neu.copy(parameter = alt.parameter))
            assertNotNull(neu.parameter[RECHNER_MIGRATION_DIAGNOSE])
        }
    }

    @Test fun `abgelehnte Mengenrelation bleibt durch alle Codecphasen unveraendert`() {
        val basis = MathematikKnotenVorlagen.Teilmenge.erzeuge(GraphPunkt.Zero)
        val alt = basis.copy(anschlüsse = basis.anschlüsse + basis.anschlüsse.first().copy(id = AnschlussId("extra"), name = "extra"))
        val karte = KartenDaten(name = "Altrelation", knoten = listOf(alt))
        val gesperrt = karte.migrierePraedikatKnoten()
        assertNotNull(gesperrt.knoten.single().parameter[RECHNER_MIGRATION_DIAGNOSE])
        val json = KartenDatenJson.schreibe(karte)
        assertEquals(gesperrt, MathematikKartenCodec.lese(json))
        assertEquals(gesperrt, MathematikKartenCodec.lade(json))
        assertEquals(gesperrt, MathematikKartenCodec.importiere(json))
        assertEquals(gesperrt, KartenDatenJson.lese(MathematikKartenCodec.schreibe(gesperrt)))
    }

    @Test fun `unbekannte Rollennamen werden nicht aus Layoutpositionen erfunden`() {
        val basis = GeometrieKnotenVorlagen.Inzidenz.erzeuge(GraphPunkt.Zero)
        val alt = basis.copy(anschlüsse = basis.anschlüsse.map {
            if (it.name == "links") it.copy(name = "unbekannt") else it
        })
        val neu = KartenDaten(name = "Rolle", knoten = listOf(alt)).migrierePraedikatKnoten().knoten.single()
        assertEquals(alt, neu.copy(parameter = alt.parameter))
        assertNotNull(neu.parameter[RECHNER_MIGRATION_DIAGNOSE])
    }

    @Test fun `Sperre gilt vor Kartenreferenzen und vorgegebenen Ausgaben`() {
        val alt = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf(RECHNER_MIGRATION_DIAGNOSE to "Original erhalten"),
            kartenVerweis = KartenVerweis(KartenId("fremdeKarte"), 1))
        val register = GesamterMathematikAuswerter.erzeugeRegister()
        val auswerter = KartenAuswerter(register, KartenQuelle { error("Gesperrte Karte darf nicht geladen werden") })
        val karte = KartenDaten(name = "Referenz", knoten = listOf(alt))
        val fest = mapOf(alt.id to mapOf("wert" to BedingterWert(RationaleZahl.Eins)))
        listOf(auswerter.auswerten(karte), auswerter.auswerten(karte, fest)).forEach {
            assertEquals("Migrationsdiagnose: Original erhalten", it.knoten.getValue(alt.id).fehler)
            assertTrue(it.knoten.getValue(alt.id).ausgaben.isEmpty())
        }
    }

}

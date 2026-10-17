package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import kotlin.test.*

class StandardKartenMigrationsProvenienzTest {
    private val hash="c9687628d041752314ae125004a5298fc6dca9033f7818efcb23d20664488761"
    private fun original()=KartenDatenJson.lese(requireNotNull(javaClass.getResource("/konsolidierung-m4/installiert-vorher-v10.json")).readText())
    @Test fun `vor M4 erfasster Installationshash bleibt mit Originaldaten exakt nachweisbar`() {
        val alt=original()
        assertEquals("mathematik.einzelmenge",alt.knoten.single {it.id.wert=="n4"}.art)
        assertEquals(hash,originalStandardKartenInhaltsHash(alt))
        assertNotEquals(hash,standardKartenInhaltsHash(alt))
        assertTrue(standardKarteUnverändert(alt,hash))
        assertFalse(standardKarteUnverändert(alt.copy(name="Nutzeränderung"),hash))
        assertFalse(standardKarteUnverändert(alt,""))
        val neu=MathematikKartenCodec.lade(KartenDatenJson.schreibe(alt))
        assertEquals(ENDLICHE_MENGE_ART,neu.knoten.single {it.id.wert=="n4"}.art)
        assertEquals(alt.verbindungen,neu.verbindungen)
    }

    @Test fun `rueckmigrierbare Originalaenderung ist trotz gleichem Mathematikhash eine Nutzeraenderung`() {
        val kanonisch=MathematikKartenCodec.lade(KartenDatenJson.schreibe(original()))
        val installiert=originalStandardKartenInhaltsHash(kanonisch)
        val geändert=kanonisch.copy(knoten=kanonisch.knoten.map {k->if(k.id.wert!="n4") k else k.copy(
            art="mathematik.einzelmenge",parameter=k.parameter+(ENDLICHE_MENGE_EINGABE to ENDLICHE_MENGE_LOKAL),
            anschlüsse=k.anschlüsse.map {if(it.richtung==AnschlussRichtung.Eingang) it.copy(name="element") else it})})
        assertEquals(installiert,standardKartenInhaltsHash(geändert))
        assertNotEquals(installiert,originalStandardKartenInhaltsHash(geändert))
        assertFalse(standardKarteUnverändert(geändert,installiert))
    }
}

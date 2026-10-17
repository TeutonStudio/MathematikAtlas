package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKnoten.*
import kotlin.test.*

class MengenQuellenKonfigurationTest {
    @Test fun `Quellenwechsel erhaelt IDs und ist ein Undo Redo Schritt`() {
        val vorher = MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val eintrag = MengenAuswahlEintrag("intern:N0", "Natürliche Zahlen mit Null", "Definition", MengenKnotenAuswahl.NATUERLICHE_MIT_NULL)
        val nachher = konfiguriereMengenQuelle(vorher, eintrag)
        assertEquals(vorher.id, nachher.id)
        assertEquals(vorher.anschlüsse, nachher.anschlüsse)
        val eingang = AnschlussDaten(name = "menge", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Menge.id)
        val ziel = KnotenDaten(art = "test", name = "Verbraucher", anschlüsse = listOf(eingang))
        val verbindung = VerbindungDaten(von = AnschlussVerweis(vorher.id, vorher.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.id), zu = AnschlussVerweis(ziel.id, eingang.id))
        val editor = KartenEditorZustand(KartenDaten(name = "Quelle", knoten = listOf(vorher, ziel), verbindungen = listOf(verbindung)), GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)))
        editor.führeAus(KartenAktion.KnotenErsetzen(nachher))
        assertEquals("N0", editor.karte.knoten.first().parameter[MENGEN_KNOTEN_AUSWAHL])
        assertEquals(listOf(verbindung), editor.karte.verbindungen)
        editor.rückgängig()
        assertEquals(vorher, editor.karte.knoten.first())
        editor.wiederholen()
        assertEquals(nachher, editor.karte.knoten.first())
        assertEquals(listOf(verbindung), editor.karte.verbindungen)
    }

    @Test fun `eigene Karten bleiben versionsfest und Rueckwechsel erhaelt Anschlussidentitaet`() {
        val vorher = MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val ref = KartenVerweis(KartenId("eigene-menge"), 4)
        val karte = EigeneMengenKarte("Meine Menge", ref, "resultat")
        val eigene = konfiguriereMengenQuelle(vorher, MengenAuswahlEintrag("karte:eigene-menge:4", "Meine Menge", "Eigene Quelle", eigeneKarte = karte))
        assertEquals(ref, eigene.kartenVerweis)
        assertEquals("resultat", eigene.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.name)
        val geladen = MathematikKartenCodec.lade(KartenDatenJson.schreibe(KartenDaten(name = "Eigene Quelle", knoten = listOf(eigene)))).knoten.single()
        assertEquals(ref, geladen.kartenVerweis)
        assertEquals(eigene.anschlüsse, geladen.anschlüsse)
        assertEquals(eigene.parameter, geladen.parameter)
        val zurück = konfiguriereMengenQuelle(eigene, MengenAuswahlEintrag("intern:P", "Primzahlen", "Definition", MengenKnotenAuswahl.PRIMZAHLEN))
        assertNull(zurück.kartenVerweis)
        assertEquals(vorher.anschlüsse, zurück.anschlüsse)
        assertEquals("P", zurück.parameter[MENGEN_KNOTEN_AUSWAHL])
    }
    @Test fun `Wechsel aus Tensorraum behaelt den bisherigen Vertrag eigener Mengenkarten`() {
        val tensor = konfiguriereMengenDimensionsart(MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),MENGEN_DIMENSION_TENSOR)
        val ref = KartenVerweis(KartenId("eigene-menge"),4)
        val eigene = konfiguriereMengenQuelle(tensor,MengenAuswahlEintrag("karte:eigene-menge:4","Meine Menge","Eigene Quelle",eigeneKarte=EigeneMengenKarte("Meine Menge",ref,"resultat")))
        assertEquals(ref,eigene.kartenVerweis)
        assertEquals(tensor.anschlüsse.single { it.name=="dimension" }.id,eigene.anschlüsse.single { it.name=="dimension" }.id)
        assertEquals(MathematikAnschlussArten.Zahl.id,eigene.anschlüsse.single { it.name=="dimension" }.art)
        assertFalse(MENGEN_DIMENSIONSART_PARAMETER in eigene.parameter)
        assertFalse(hatMengenDimensionsart(eigene))
    }
}

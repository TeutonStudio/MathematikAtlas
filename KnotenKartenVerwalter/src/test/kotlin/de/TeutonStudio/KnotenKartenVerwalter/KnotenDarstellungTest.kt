package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlin.test.*

class KnotenDarstellungTest {
    private val basis = KnotenDaten(art = "test", name = "Werkzeug", größe = GraphGröße(400f, 240f))

    @Test fun farbenUndFormenUeberstehenCodecUndKopieren() {
        val editor = KartenEditorZustand(KartenDaten(name = "Darstellung", knoten = listOf(basis)), GraphPrüfung(AnschlussArtRegister(emptyList())))
        val neu = basis.mitDarstellungsFarbe(KNOTEN_HINTERGRUND_EIGENSCHAFT, 0xFF123456)
            .mitDarstellungsFarbe(KNOTEN_RAHMEN_EIGENSCHAFT, 0xFFABCDEF)
            .mitDarstellungsForm(KnotenDarstellungsForm.Raute, KnotenDarstellungsForm.entries.toSet())
        editor.führeAus(KartenAktion.KnotenErsetzen(neu))
        assertEquals(neu, editor.karte.knoten.single())
        editor.rückgängig(); assertEquals(basis, editor.karte.knoten.single())
        editor.wiederholen(); assertEquals(neu, editor.karte.knoten.single())
        val gelesen = KartenDatenJson.lese(KartenDatenJson.schreibe(editor.karte))
        assertEquals(neu.eigenschaften, gelesen.knoten.single().eigenschaften)
        editor.wähleKnoten(neu.id); editor.dupliziereAuswahl()
        val kopie = editor.karte.knoten.single { it.id != neu.id }
        assertEquals(neu.eigenschaften, kopie.eigenschaften)
        assertNotEquals(neu.id, kopie.id)
    }

    @Test fun alteUndUnbekannteEigenschaftenBehaltenFallbackUndAutomatischEntferntNurEineFarbe() {
        assertEquals(KnotenDarstellungsForm.Rechteck, basis.darstellungsForm)
        assertEquals(KnotenDarstellungsForm.Rechteck, basis.copy(eigenschaften = mapOf(KNOTEN_FORM_EIGENSCHAFT to KnotenEigenschaft.Text("zukünftig"))).darstellungsForm)
        val farbig = basis.mitDarstellungsFarbe(KNOTEN_HINTERGRUND_EIGENSCHAFT, 0xFF123456).mitDarstellungsFarbe(KNOTEN_RAHMEN_EIGENSCHAFT, 0xFFABCDEF)
        val automatisch = farbig.mitDarstellungsFarbe(KNOTEN_HINTERGRUND_EIGENSCHAFT, null)
        assertFalse(KNOTEN_HINTERGRUND_EIGENSCHAFT in automatisch.eigenschaften)
        assertEquals(farbig.eigenschaften[KNOTEN_RAHMEN_EIGENSCHAFT], automatisch.eigenschaften[KNOTEN_RAHMEN_EIGENSCHAFT])
    }

    @Test fun eingabenWerdenVorModellaenderungValidiert() {
        assertEquals(0xFFAABBCC, parseKnotenFarbe(" #aabbcc "))
        listOf("", "#123", "1234567", "#GG0000", "0011 22", "-00001").forEach { assertNull(parseKnotenFarbe(it), it) }
        assertFailsWith<IllegalArgumentException> { basis.mitDarstellungsFarbe(KNOTEN_HINTERGRUND_EIGENSCHAFT, 0x00123456) }
        assertFailsWith<IllegalArgumentException> { basis.mitDarstellungsForm(KnotenDarstellungsForm.Ellipse, setOf(KnotenDarstellungsForm.Rechteck)) }
        assertFailsWith<IllegalArgumentException> { basis.mitDarstellungsFarbe("fach.farbe", 0xFF123456) }
    }

    @Test fun sichereInhaltsrechteckeLiegenInnerhalbJederForm() {
        KnotenDarstellungsForm.entries.forEach { form ->
            val knoten = basis.mitDarstellungsForm(form, KnotenDarstellungsForm.entries.toSet())
            val innen = knoten.inhaltsRahmen(freieMessung = false)
            if (form != KnotenDarstellungsForm.Rechteck)
                for (x in listOf(innen.x, innen.x + innen.breite)) for (y in listOf(innen.y, innen.y + innen.höhe))
                    assertTrue(knoten.enthältRelativenPunkt(GraphPunkt(x * 400f, y * 240f)), "$form ($x,$y)")
            assertTrue(knoten.enthältRelativenPunkt(GraphPunkt(200f, 120f)))
            if (form != KnotenDarstellungsForm.Rechteck) assertFalse(knoten.enthältRelativenPunkt(GraphPunkt(0f, 0f)), form.toString())
        }
    }

    @Test fun neueFormenPlatzierenAlleKantenanschluesseAufDerKontur() {
        for (form in listOf(KnotenDarstellungsForm.Ellipse, KnotenDarstellungsForm.Dreieck, KnotenDarstellungsForm.Raute)) {
            val ports = AnschlussKante.entries.flatMap { kante -> (0..6).map { AnschlussDaten(name = "$kante-$it", art = AnschlussArtId("test"), kante = kante, reihenfolge = it) } }
            val knoten = basis.copy(anschlüsse = ports).mitDarstellungsForm(form, KnotenDarstellungsForm.entries.toSet())
            ports.forEach { port ->
                val p = knoten.anschlussPositionRelativ(port)
                assertTrue(knoten.enthältRelativenPunkt(p), "$form $port $p")
                val außen = GraphPunkt(200f + (p.x - 200f) * 1.01f, 120f + (p.y - 120f) * 1.01f)
                assertFalse(knoten.enthältRelativenPunkt(außen), "$form $port $p")
            }
        }
    }
}

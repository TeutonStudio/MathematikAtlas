package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.*
import kotlin.system.measureNanoTime

/** Modellmessung; ersetzt ausdrücklich keine Frame-Timing-Messung auf Android. */
class GruppenSkalierungTest {
    @Test fun `100 500 und 1000 Knoten bleiben bei 30 Faltungen strukturell verlustfrei`() {
        for (anzahl in listOf(100,500,1000)) {
            val knoten=List(anzahl) { i -> KnotenDaten(id=KnotenId("k$i"),art="test",name="K$i",
                position=GraphPunkt(i*140f,150f),anschlüsse=listOf(
                    AnschlussDaten(id=AnschlussId("$i-in"),name="in",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=AnschlussArtId("test")),
                    AnschlussDaten(id=AnschlussId("$i-out"),name="out",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=AnschlussArtId("test")),
                    AnschlussDaten(id=AnschlussId("$i-in2"),name="in2",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=AnschlussArtId("test")))) }
            val verbindungen=(0 until minOf(2000,anzahl*2-3)).map { i ->
                val von=i/2;val zu=von+1+i%2
                VerbindungDaten(id=VerbindungsId("v$i"),von=AnschlussVerweis(knoten[von].id,knoten[von].anschlüsse[1].id),zu=AnschlussVerweis(knoten[zu].id,knoten[zu].anschlüsse[if(i%2==0)0 else 2].id))
            }
            val p=VisuelleKnotenGruppeDaten(id=VisuelleGruppenId("p"),eingeklappt=true,größe=GraphGröße(anzahl*140f+100f,600f))
            val c=VisuelleKnotenGruppeDaten(id=VisuelleGruppenId("c"),elternGruppenId=p.id,größe=GraphGröße(anzahl*140f+60f,500f))
            val d=VisuelleKnotenGruppeDaten(id=VisuelleGruppenId("d"),elternGruppenId=c.id,knotenIds=knoten.mapTo(linkedSetOf()){it.id},größe=GraphGröße(anzahl*140f,400f))
            val karte=KartenDaten(name="Skalierung",knoten=knoten,verbindungen=verbindungen,visuelleGruppen=listOf(p,c,d))
            repeat(5){karte.gruppenProjektion()}
            val zeiten=List(30) {
                measureNanoTime {
                    val projektion=karte.gruppenProjektion()
                    assertEquals(1,projektion.karte.knoten.size)
                    assertTrue(projektion.karte.verbindungen.isEmpty())
                    assertEquals(anzahl,knoten.size)
                    assertEquals(anzahl,projektion.originalAnschlüsse.values.count{it.anschlussId.wert.endsWith("out")})
                }
            }.sorted()
            println("Gruppenmodell: n=$anzahl, edges=${verbindungen.size}, p95=${zeiten[28]/1_000_000.0} ms, max=${zeiten.last()/1_000_000.0} ms (JVM, kein Frame-Timing)")
        }
    }
}

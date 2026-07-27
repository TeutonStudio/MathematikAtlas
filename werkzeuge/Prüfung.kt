import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

fun main() {
    val halb = RationaleZahl.von(1,2)
    check(halb + RationaleZahl.von(1,3) == RationaleZahl.von(5,6))
    val x = Variable("x")
    check(addition(addition(x, RationaleZahl.von(2)), RationaleZahl.von(3)) == addition(x, RationaleZahl.von(5)))
    check(löseLinear(Gleichheit(addition(multiplikation(RationaleZahl.von(2), x), RationaleZahl.von(4)), RationaleZahl.von(10)), x).lösungen.single() == RationaleZahl.von(3))
    val matrix = Matrix(listOf(listOf(RationaleZahl.von(2), RationaleZahl.Null), listOf(RationaleZahl.Null, RationaleZahl.von(4))))
    check(matrix.inverseRational().zeilen[1][1] == RationaleZahl.von(1,4))

    val objekt = AnschlussArt(AnschlussArtId("objekt"), "Objekt")
    val zahl = AnschlussArt(AnschlussArtId("zahl"), "Zahl", objekt.id)
    val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(objekt, zahl)))
    val aus = KnotenDaten(art="a",name="a",anschlüsse=listOf(AnschlussDaten(name="wert",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=zahl.id)))
    val ein = KnotenDaten(art="b",name="b",anschlüsse=listOf(AnschlussDaten(name="wert",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=objekt.id)))
    check(prüfung.prüfe(KartenDaten(name="t",knoten=listOf(aus,ein)), AnschlussVerweis(aus.id,aus.anschlüsse[0].id), AnschlussVerweis(ein.id,ein.anschlüsse[0].id)) is VerbindungsPrüfung.Erlaubt)

    val a = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf("wert" to "2"))
    val b = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf("wert" to "3"))
    val plus = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
    fun v(von:KnotenDaten, an:String, zu:KnotenDaten, bn:String)=VerbindungDaten(von=AnschlussVerweis(von.id,von.anschlüsse.first{it.name==an}.id),zu=AnschlussVerweis(zu.id,zu.anschlüsse.first{it.name==bn}.id))
    val karte=KartenDaten(name="summe",knoten=listOf(a,b,plus),verbindungen=listOf(v(a,"wert",plus,"a"),v(b,"wert",plus,"b")))
    val erg=KartenAuswerter(StandardMathematikAuswerter.erzeugeRegister()).auswerten(karte)
    check(erg.knoten.getValue(plus.id).ausgaben.getValue("wert").objekt == RationaleZahl.von(5))
    println("Alle Kernprüfungen erfolgreich.")
}

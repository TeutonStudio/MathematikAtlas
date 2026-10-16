package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal fun sinusGradKonzept(knoten: KnotenDaten): KonzeptDefinition? {
    if (knoten.art != ZAHLENRECHNER_ART || knoten.parameter[ZAHLENRECHNER_OPERATOR] != UniversellerZahlenOperator.SINUS.stabileId) return null
    val grad = knoten.parameter[SINUS_WINKELRAUM] == "grad"
    if (!grad && (knoten.parameter[ZAHLENRECHNER_GRADWINKEL] == "true" || knoten.parameter["zahlbereich"] == "C")) return null
    val id = if (grad) "sinus-grad-einheitskreis" else "sinus-reell-grad-komposition"
    val karte = if (grad) KartenDaten(id = KartenId("definition-$id"), name = "Sinus auf Grad", knoten = listOf(
        KnotenDaten(id = KnotenId("$id-regel"), art = KonzeptKnotenArten.REGEL, name = "Einheitskreis",
            parameter = mapOf("regel" to "sin auf der Gradmenge ist die y-Koordinate des zum Winkel gehörigen Punkts auf dem Einheitskreis. 360° und 0° beschreiben denselben Punkt. Wertebereich: −1 ≤ y ≤ 1. Diese geometrische Grunddefinition verwendet keinen reellen Sinusknoten."))))
    else sinusReellDefinitionsKarte()
    return KonzeptDefinition(id = KonzeptId(id), name = if (grad) "Sinus auf Grad" else "Reeller Sinus",
        beschreibung = if (grad) "y-Koordinate auf dem Einheitskreis" else "sin_R = sin_Grad ∘ q; q übersetzt reelles Bogenmaß in einen Winkel.",
        pfad = listOf("Analysis", "Trigonometrie"), tags = setOf("Sinus", "Grad", "Komposition"),
        knotenArten = setOf(ZAHLENRECHNER_ART), knotenParameter = mapOf(ZAHLENRECHNER_OPERATOR to UniversellerZahlenOperator.SINUS.stabileId, SINUS_WINKELRAUM to if (grad) "grad" else "reell"),
        reiter = listOf(KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, karte)))
}

/** Ausführbare, azyklische Zerlegung, ohne Rückverweis auf sin_R. */
internal fun sinusReellDefinitionsKarte(): KartenDaten {
    val prefix = "definition-sinus-reell"
    fun port(name: String, art: AnschlussArtId, richtung: AnschlussRichtung) = AnschlussDaten(
        id = AnschlussId("$prefix-$name-${richtung.name}"), name = "wert", art = art, richtung = richtung,
        kante = if (richtung == AnschlussRichtung.Eingang) AnschlussKante.Links else AnschlussKante.Rechts)
    val eingang = KnotenDaten(id = KnotenId("$prefix-x"), art = KonzeptKnotenArten.EINGANG, name = "x",
        position = GraphPunkt(30f, 80f), anschlüsse = listOf(port("x", MathematikAnschlussArten.Zahl.id, AnschlussRichtung.Ausgang)),
        parameter = mapOf("typ" to "mathematik.zahl", "rolle" to "x"))
    val q = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt(290f, 80f)).copy(id = KnotenId("$prefix-q")), "grad")
    val sin = konfiguriereSinusWinkelraum(ZahlenRechnerKnotenVorlagen.alle.single { it.standardParameter[ZAHLENRECHNER_OPERATOR] == UniversellerZahlenOperator.SINUS.stabileId }.erzeuge(GraphPunkt(570f, 80f)).copy(id = KnotenId("$prefix-sin-grad")), true)
    val ausgang = KnotenDaten(id = KnotenId("$prefix-y"), art = "mathematik.kartenAusgang", name = "sin(x)",
        position = GraphPunkt(860f, 80f), anschlüsse = listOf(port("y", MathematikAnschlussArten.Zahl.id, AnschlussRichtung.Eingang)))
    val knoten = listOf(eingang, q, sin, ausgang).map { k -> k.copy(anschlüsse = k.anschlüsse.mapIndexed { index, p -> p.copy(id = AnschlussId("${k.id.wert}-$index")) }) }
    fun verbindung(i: Int, aus: String, j: Int, ein: String) = VerbindungDaten(id = VerbindungsId("$prefix-$i-$j"),
        von = AnschlussVerweis(knoten[i].id, knoten[i].anschlüsse.single { it.name == aus && it.richtung == AnschlussRichtung.Ausgang }.id),
        zu = AnschlussVerweis(knoten[j].id, knoten[j].anschlüsse.single { it.name == ein && it.richtung == AnschlussRichtung.Eingang }.id))
    return KartenDaten(id = KartenId(prefix), erstelltAm = 0L, name = "sin_R = sin_Grad ∘ q", knoten = knoten,
        verbindungen = listOf(verbindung(0, "wert", 1, "wert"), verbindung(1, "ergebnis", 2, "a"), verbindung(2, "wert", 3, "wert")))
}

internal fun vektorUnterraumKonzept(knoten: KnotenDaten): KonzeptDefinition? {
    val potenz = knoten.art == VektorRechner.KNOTEN_ART && knoten.parameter[VEKTOR_RECHNER_OPERATOR] == VektorRechnerOperator.POTENZ.stabileId
    val unterraum = knoten.art == MengenRechner.KNOTEN_ART && knoten.parameter[MENGENRECHNER_OPERATOR_PARAMETER] == MengenRechnerOperator.UNTERRAUM.stabileId
    if (!potenz && !unterraum) return null
    val id = if (potenz) "vektorpotenz" else "unterraum"
    val karte = if (potenz) vektorPotenzDefinitionsKarte(knoten) else unterraumDefinitionsKarte(knoten)
    return KonzeptDefinition(id = KonzeptId(id), name = if (potenz) "Vektorpotenz" else "Linearer und affiner Spann",
        beschreibung = if (potenz) "P₂ₖ(a)=⟨a,a⟩ᵏ, P₂ₖ₊₁(a)=⟨a,a⟩ᵏa; p=0 ergibt 1. Die Karte zeigt den eingestellten Exponenten."
            else "{c+Σ bₖaᵏ | b∈ℝⁿ}; ohne Stützpunkt linear. Rang und Umgebungsdimension sind getrennt.",
        pfad = listOf("Lineare Algebra"), tags = setOf("Vektor", "Unterraum", "Skalarprodukt"), knotenArten = setOf(knoten.art), knotenParameter = mapOf("operator" to (knoten.parameter["operator"] ?: "")),
        reiter = listOf(KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, karte)))
}

internal fun vektorPotenzDefinitionsKarte(original: KnotenDaten): KartenDaten {
    val n = original.parameter[VEKTOR_POTENZ_EXPONENT]?.toIntOrNull()?.coerceAtLeast(0) ?: 2
    val prefix = "definition-vektorpotenz-$n"
    val nodes = mutableListOf<KnotenDaten>(); val edges = mutableListOf<VerbindungDaten>()
    fun add(k: KnotenDaten, name: String): KnotenDaten {
        val node = k.copy(id = KnotenId("$prefix-$name"), position = GraphPunkt(nodes.size*280f,80f), anschlüsse = k.anschlüsse.mapIndexed { i,p -> p.copy(id=AnschlussId("$prefix-$name-$i")) })
        nodes += node; return node
    }
    fun connect(a: KnotenDaten, out: String, b: KnotenDaten, input: String) {
        edges += VerbindungDaten(id=VerbindungsId("$prefix-edge-${edges.size}"), von=AnschlussVerweis(a.id,a.anschlüsse.single { it.name==out && it.richtung==AnschlussRichtung.Ausgang }.id), zu=AnschlussVerweis(b.id,b.anschlüsse.single { it.name==input && it.richtung==AnschlussRichtung.Eingang }.id))
    }
    fun input(name: String, art: AnschlussArtId) = add(KnotenDaten(art=KonzeptKnotenArten.EINGANG,name=name,anschlüsse=listOf(AnschlussDaten(name="wert",art=art,richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts)),parameter=mapOf("typ" to art.wert,"rolle" to name)),name)
    fun number(name:String, value:Int) = add(MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(0f,0f)).copy(parameter=mapOf("wert" to value.toString())),name)
    val a=input("a",MathematikAnschlussArten.Objekt.id)
    // Strukturwechsel bleibt als eigener ausführbarer Knoten sichtbar.
    val orientierung=if(original.parameter["definition.orientierung"]=="zeile") "zeile" else "spalte"
    val spalte=if(n==0) a else add(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt(0f,0f)),orientierung),"tupel-zu-$orientierung")
    if(n!=0) connect(a,"wert",spalte,"wert")
    val result: Pair<KnotenDaten,String> = if(n==0) number("eins",1) to "wert" else if(n==1) spalte to "ergebnis" else {
        val produkt=add(konfiguriereVektorRechner(original,VektorRechnerOperator.SKALARPRODUKT),"skalarprodukt")
        connect(spalte,"ergebnis",produkt,"links");connect(spalte,"ergebnis",produkt,"rechts")
        val pow=add(konfiguriereZahlenRechner(ZahlenRechnerKnotenVorlagen.alle.first().erzeuge(GraphPunkt(0f,0f)),UniversellerZahlenOperator.POTENZ),"skalare-potenz")
        val exponent=number("halbexponent",n/2)
        connect(produkt,"skalar",pow,"a");connect(exponent,"wert",pow,"b")
        if(n%2==0) pow to "wert" else {
            val skalierung=add(vektorRechnerVorlage(VektorRechnerOperator.SKALARMULTIPLIKATION).erzeuge(GraphPunkt(0f,0f)),"skalierung")
            connect(spalte,"ergebnis",skalierung,"vektor");connect(pow,"wert",skalierung,"skalar")
            skalierung to "vektor"
        }
    }
    val out=add(KnotenDaten(art="mathematik.kartenAusgang",name="P$n(a)",anschlüsse=listOf(AnschlussDaten(name="wert",art=MathematikAnschlussArten.Objekt.id,richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links))),"ausgang")
    connect(result.first,result.second,out,"wert")
    return KartenDaten(id=KartenId(prefix),name="Vektorpotenz $n",erstelltAm=0L,knoten=nodes,verbindungen=edges)
}

internal fun unterraumDefinitionsKarte(original: KnotenDaten): KartenDaten {
    val prefix="definition-unterraum"
    val nodes=mutableListOf<KnotenDaten>();val edges=mutableListOf<VerbindungDaten>()
    fun add(k:KnotenDaten,name:String):KnotenDaten {
        val node=k.copy(id=KnotenId("$prefix-$name"),position=GraphPunkt(nodes.size*240f,80f),anschlüsse=k.anschlüsse.mapIndexed { i,p -> p.copy(id=AnschlussId("$prefix-$name-$i")) })
        nodes+=node;return node
    }
    fun connect(a:KnotenDaten,out:String,b:KnotenDaten,input:String) {
        edges+=VerbindungDaten(id=VerbindungsId("$prefix-edge-${edges.size}"),von=AnschlussVerweis(a.id,a.anschlüsse.single { it.name==out && it.richtung==AnschlussRichtung.Ausgang }.id),zu=AnschlussVerweis(b.id,b.anschlüsse.single { it.name==input && it.richtung==AnschlussRichtung.Eingang }.id))
    }
    val n=original.parameter["definition.richtungsanzahl"]?.toIntOrNull() ?: original.anschlüsse.count { it.richtung==AnschlussRichtung.Eingang && it.name.startsWith("a.") }.takeIf { it > 0 } ?: 2
    if(n==0) {
        val c=if(original.parameter["unterraum.affin"]=="true") add(KnotenDaten(art=KonzeptKnotenArten.EINGANG,name="c",parameter=mapOf("rolle" to "c","typ" to "mathematik.objekt"),anschlüsse=listOf(AnschlussDaten(name="wert",art=MathematikAnschlussArten.Objekt.id,richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts))),"c") else {
            val d=original.parameter["unterraum.dimension"]?.toIntOrNull() ?: error("Die Definition der Nullpunktmenge benötigt die Umgebungsdimension.")
            val nullwert=add(MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf("wert" to "0")),"null")
            val tuple=add(MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero).copy(anschlüsse=List(d) { i -> AnschlussDaten(name="komponente$i",art=MathematikAnschlussArten.Objekt.id,richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,reihenfolge=i) }+AnschlussDaten(name="tupel",art=MathematikAnschlussArten.Tupel.id,richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts),parameter=mapOf("festeEingänge" to d.toString(),"erzeugungsArt" to TUPEL_EINZEL_EINGABEN)),"nulltupel")
            tuple.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.forEach { connect(nullwert,"wert",tuple,it.name) };tuple
        }
        val koordinaten=if(c.art=="mathematik.tupel") c else add(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"tupel"),"stuetzpunkttupel").also { connect(c,"wert",it,"wert") }
        val singleton=add(MathematikKnotenVorlagen.Einzelmenge.erzeuge(GraphPunkt.Zero),"einzelmenge");connect(koordinaten,if(koordinaten.art=="mathematik.tupel") "tupel" else "ergebnis",singleton,"element")
        val out=add(KnotenDaten(art="mathematik.kartenAusgang",name="Nullpunkt / Stützpunkt",anschlüsse=listOf(AnschlussDaten(name="wert",art=MathematikAnschlussArten.Menge.id,richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links))),"ausgang");connect(singleton,"menge",out,"wert")
        return KartenDaten(id=KartenId(prefix),name="Unterraum ohne Richtungen",erstelltAm=0L,knoten=nodes,verbindungen=edges)
    }
    val summanden=(1..n).map { i ->
        val input=add(KnotenDaten(art=KonzeptKnotenArten.EINGANG,name="a.$i",parameter=mapOf("rolle" to "a.$i","typ" to "mathematik.objekt"),anschlüsse=listOf(AnschlussDaten(name="wert",art=MathematikAnschlussArten.Objekt.id,richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts))),"a$i")
        val q=add(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"spalte"),"spalte$i");connect(input,"wert",q,"wert")
        val b=add(MathematikKnotenVorlagen.AllgemeinerParameter.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf("name" to "b$i"),eigenschaften=mapOf(WertebereichKonfiguration.EIGENSCHAFT to WertebereichKonfiguration.Zahl("R").zuEigenschaft())),"b$i")
        val mul=add(vektorRechnerVorlage(VektorRechnerOperator.SKALARMULTIPLIKATION).erzeuge(GraphPunkt.Zero),"skalierung$i")
        connect(q,"ergebnis",mul,"vektor");connect(b,"wert",mul,"skalar");mul
    }
    var sum=summanden.first();var outName="vektor"
    for(i in 1 until summanden.size) {
        val plus=add(vektorRechnerVorlage(VektorRechnerOperator.ADDITION).erzeuge(GraphPunkt.Zero),"summe$i")
        connect(sum,outName,plus,"vektor.1");connect(summanden[i],"vektor",plus,"vektor.2");sum=plus
    }
    if(original.parameter["unterraum.affin"]=="true") {
        val c=add(KnotenDaten(art=KonzeptKnotenArten.EINGANG,name="c",parameter=mapOf("rolle" to "c","typ" to "mathematik.objekt"),anschlüsse=listOf(AnschlussDaten(name="wert",art=MathematikAnschlussArten.Objekt.id,richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts))),"c")
        val q=add(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"spalte"),"stuetzpunkt");connect(c,"wert",q,"wert")
        val plus=add(vektorRechnerVorlage(VektorRechnerOperator.ADDITION).erzeuge(GraphPunkt.Zero),"affine-summe")
        connect(sum,outName,plus,"vektor.1");connect(q,"ergebnis",plus,"vektor.2");sum=plus
    }
    val tupel=add(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"tupel"),"koordinatentupel");connect(sum,outName,tupel,"wert")
    val methode=add(konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"methode").copy(parameter=mapOf("methode.tupelEinzelausgabe" to "true")),"linearkombination-methode");connect(tupel,"ergebnis",methode,"wert")
    val reell=add(MathematikKnotenVorlagen.ReelleZahlen.erzeuge(GraphPunkt.Zero),"reell")
    val koeffizienten=if(n==1) reell else {
        val produkt=add(konfiguriereMengenRechner(MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),MengenRechnerOperator.KARTESISCHES_PRODUKT,n),"koeffiziententupelraum")
        produkt.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.forEach { connect(reell,"menge",produkt,it.name) };produkt
    }
    val bild=add(MathematikKnotenVorlagen.Abbild.erzeuge(GraphPunkt.Zero),"bildmenge");connect(methode,"ergebnis",bild,"methode");connect(koeffizienten,"menge",bild,"menge")
    val out=add(KnotenDaten(art="mathematik.kartenAusgang",name="{c+Σbₖaᵏ | b∈ℝⁿ}",anschlüsse=listOf(AnschlussDaten(name="wert",art=MathematikAnschlussArten.Menge.id,richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links))),"ausgang");connect(bild,"menge",out,"wert")
    return KartenDaten(id=KartenId(prefix),name="Unterraum als Bild der Linearkombination",erstelltAm=0L,knoten=nodes,verbindungen=edges)
}

internal fun gradRichtungsKonzept(knoten: KnotenDaten): KonzeptDefinition? {
    if (knoten.art != MENGEN_KNOTEN_ART || knoten.kartenVerweis != null ||
        knoten.parameter[MENGEN_KNOTEN_AUSWAHL] != MengenKnotenAuswahl.GRADMENGE.stabileId) return null
    val dimension = knoten.parameter["definition.dim"]?.toIntOrNull()?.takeIf { it in 1..65 } ?: 3
    return KonzeptDefinition(
        id = KonzeptId("grad-richtungsraum"), name = "Gradmenge und Richtungsraum",
        beschreibung = "Ohne dim: Winkelgruppe G. Mit dim=d: ein Vorzeichen und d−1 Halbkreiswinkel. Die endliche Indexmenge ist {0,…,d−1}.",
        pfad = listOf("Mengen", "Winkel"), tags = setOf("Grad", "Polar", "Vorzeichen"),
        knotenArten = setOf(MENGEN_KNOTEN_ART),
        knotenParameter = mapOf(MENGEN_KNOTEN_AUSWAHL to MengenKnotenAuswahl.GRADMENGE.stabileId),
        reiter = listOf(KonzeptReiter("definition", "Definition · dim=$dimension", KonzeptReiterRolle.Definition, gradRichtungsDefinitionsKarte(dimension))),
    )
}

/** Gruppenkonstruktion des Nutzers, mit diskretem Index und kanonischen Halbkreisvertretern. */
internal fun gradRichtungsDefinitionsKarte(dimension: Int): KartenDaten {
    require(dimension in 1..65)
    val knoten = mutableListOf<KnotenDaten>()
    val verbindungen = mutableListOf<VerbindungDaten>()
    fun neu(v: KnotenVorlage, x: Float, y: Float, p: Map<String, String> = emptyMap()): KnotenDaten =
        v.erzeuge(GraphPunkt(x, y)).let { it.copy(parameter = it.parameter + p) }.also { knoten += it }
    fun verbinde(a: KnotenDaten, aus: String, b: KnotenDaten, ein: String) {
        verbindungen += VerbindungDaten(von = AnschlussVerweis(a.id, a.anschlüsse.single { it.name == aus }.id),
            zu = AnschlussVerweis(b.id, b.anschlüsse.single { it.name == ein }.id))
    }
    val plus = neu(MathematikKnotenVorlagen.Zahl, 0f, 300f, mapOf("wert" to "1"))
    val minus = neu(MathematikKnotenVorlagen.Zahl, 0f, 450f, mapOf("wert" to "-1"))
    val einzelPlus = neu(MathematikKnotenVorlagen.Einzelmenge, 300f, 300f)
    val einzelMinus = neu(MathematikKnotenVorlagen.Einzelmenge, 300f, 450f)
    verbinde(plus, "wert", einzelPlus, "element"); verbinde(minus, "wert", einzelMinus, "element")
    val union = neu(MathematikKnotenVorlagen.Vereinigung, 600f, 380f)
    verbinde(einzelPlus, "menge", union, "a"); verbinde(einzelMinus, "menge", union, "b")
    val winkel = neu(MathematikKnotenVorlagen.ReellesIntervall, 600f, 0f,
        mapOf("standardwert.links" to "0", "standardwert.rechts" to "180", "standardwert.linksOffen" to "false", "standardwert.rechtsOffen" to "true"))
    val i = neu(MathematikKnotenVorlagen.Variable, 0f, -220f, mapOf("name" to "i"))
    val indexGrundraum = neu(MathematikKnotenVorlagen.GanzeZahlen, -300f, -220f)
    verbinde(indexGrundraum, "menge", i, "grundraum")
    val nullwert = neu(MathematikKnotenVorlagen.Zahl, 0f, -50f, mapOf("wert" to "0"))
    val ungleich = neu(MathematikKnotenVorlagen.Ungleichheit, 300f, -180f)
    verbinde(i, "wert", ungleich, "links"); verbinde(nullwert, "wert", ungleich, "rechts")
    val fall = neu(MathematikKnotenVorlagen.Fall, 950f, 100f)
    verbinde(winkel, "menge", fall, "wahr"); verbinde(ungleich, "aussage", fall, "aussage"); verbinde(union, "menge", fall, "lüge")
    val methode = neu(MathematikKnotenVorlagen.TermZuMethode, 1270f, 100f, mapOf("name" to "A"))
    verbinde(fall, "wert", methode, "term")
    val index = neu(MathematikKnotenVorlagen.EndlicheMenge, 1270f, 350f, mapOf(
        ENDLICHE_MENGE_KONFIGURATION_PARAMETER to EndlicheMengeKonfiguration(einträge = List(dimension) { j ->
            EndlicheMengeEintrag("index-$j", MathematikAnschlussArten.Zahl.id.wert, EndlicheMengeQuelle.ZahlLiteral(j.toString()))
        }).zuParameter()))
    val produkt = neu(MathematikKnotenVorlagen.IteriertesKartesischesProdukt, 1600f, 100f)
    verbinde(methode, "methode", produkt, "methode"); verbinde(index, "menge", produkt, "indexmenge")
    return KartenDaten(name = "Grad-Richtungsraum · dim=$dimension", erstelltAm = 0L, knoten = knoten, verbindungen = verbindungen)
}

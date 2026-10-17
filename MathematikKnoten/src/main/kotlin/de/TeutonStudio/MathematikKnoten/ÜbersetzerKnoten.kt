package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val ÜBERSETZER_ART = "atlas.uebersetzer"
const val TUPEL_ABFLACHEN_ART = "mathematik.tupelAbflachen"
const val ÜBERSETZER_ZIEL = "uebersetzer.ziel"
const val ÜBERSETZER_POLAR_GETRENNT = "uebersetzer.polarGetrennt"
fun übersetzerPolarGetrennt(knoten: KnotenDaten) = (knoten.eigenschaften[ÜBERSETZER_POLAR_GETRENNT] as? KnotenEigenschaft.Text)?.wert == "true"

fun konfiguriereÜbersetzerPolarAnschlüsse(knoten: KnotenDaten, getrennt: Boolean): KnotenDaten {
    require(knoten.art == ÜBERSETZER_ART) { "Nur Übersetzer unterstützen die Polaraufteilung." }
    val aktualisiert = knoten.copy(eigenschaften = knoten.eigenschaften + (ÜBERSETZER_POLAR_GETRENNT to KnotenEigenschaft.Text(getrennt.toString())))
    return konfiguriereÜbersetzer(aktualisiert, übersetzerZiel(aktualisiert))
}
const val ÜBERSETZER_POLAR_KONVENTION = "uebersetzer.polarKonvention"
fun übersetzerPolarKonvention(knoten:KnotenDaten):PolarKonvention =
    knoten.parameter[ÜBERSETZER_POLAR_KONVENTION]?.let { raw -> PolarKonvention.entries.firstOrNull { it.name==raw } ?: error("Unbekannte Polarkonvention '$raw'.") } ?: PolarKonvention.SIGNED

const val ÜBERSETZER_R1 = "uebersetzer.r1"
val ÜBERSETZER_ZIELE = ÜbersetzungsZiel.knotenZiele
fun übersetzerZielTitel(ziel: String): String = when (ziel) {
    "abflachen" -> "Tupel abflachen"
    "rueckfuehrung" -> "Struktur zurückführen"
    "koordinatenmenge" -> "Geometrie → Koordinatenmenge"
    "mittelpunkt" -> "Geometrischer Mittelpunkt"
    "objektpunkt" -> "Ausgewählter Objektpunkt"
    "geometrie" -> "Koordinaten → Geometrie"
    "grad" -> "R → Grad"
    "bogenmass" -> "Grad → R"
    "indexmethode" -> "Indexmethode"
    "matrixzerlegen" -> "Matrix zerlegen"
    "polar" -> "Polarkoordinaten"
    "kartesisch" -> "Kartesische Koordinaten"
    else -> ziel
}
const val ÜBERSETZER_VERTRETER = "uebersetzer.vertreter"
fun übersetzerVertreter(knoten: KnotenDaten) = WinkelVertreter.vonId(knoten.parameter[ÜBERSETZER_VERTRETER])
fun konfiguriereÜbersetzerVertreter(knoten: KnotenDaten, vertreter: WinkelVertreter): KnotenDaten {
    require(knoten.art == ÜBERSETZER_ART)
    return knoten.copy(parameter = knoten.parameter + (ÜBERSETZER_VERTRETER to vertreter.id))
}
fun übersetzerEindimensional(knoten: KnotenDaten): Boolean = knoten.parameter[ÜBERSETZER_R1] == "true"
fun konfiguriereÜbersetzerEindimensional(knoten: KnotenDaten, aktiv: Boolean): KnotenDaten {
    require(knoten.art == ÜBERSETZER_ART)
    return knoten.copy(parameter = knoten.parameter + (ÜBERSETZER_R1 to aktiv.toString()))
}

fun übersetzerZiel(knoten: KnotenDaten): String =
    (knoten.eigenschaften[ÜBERSETZER_ZIEL] as? KnotenEigenschaft.Text)?.wert ?: "spalte"

fun konfiguriereÜbersetzer(knoten: KnotenDaten, ziel: String): KnotenDaten {
    require(knoten.art == ÜBERSETZER_ART && ziel in ÜBERSETZER_ZIELE) { "Ungültiges Übersetzungsziel '$ziel'." }
    val parameter=knoten.parameter+(ÜBERSETZER_POLAR_KONVENTION to (knoten.parameter[ÜBERSETZER_POLAR_KONVENTION] ?: PolarKonvention.SIGNED.name))+("methode.tupelEinzelausgabe" to (knoten.parameter["methode.tupelEinzelausgabe"] ?: "true"))
    val ausgangArt = when (ziel) {
        "rueckfuehrung" -> MathematikAnschlussArten.Objekt.id
        "koordinatenmenge" -> MathematikAnschlussArten.Menge.id
        "mittelpunkt", "objektpunkt" -> MathematikAnschlussArten.Tupel.id
        "geometrie" -> GeometrieAnschlussArten.Objekt.id
        "grad" -> MathematikAnschlussArten.Winkel.id
        "bogenmass" -> MathematikAnschlussArten.Zahl.id
        "tupel", "abflachen", "polar", "kartesisch" -> MathematikAnschlussArten.Tupel.id
        "matrixzerlegen" -> MathematikAnschlussArten.Methode.id
        "zeile" -> MathematikAnschlussArten.ZeilenVektor.id
        "spalte" -> MathematikAnschlussArten.SpaltenVektor.id
        else -> MathematikAnschlussArten.Methode.id
    }
    val getrennt = übersetzerPolarGetrennt(knoten)
    val spezial = when {
        ziel == "kartesisch" && getrennt -> listOf(
            AnschlussDaten(name = "radius", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id),
            AnschlussDaten(name = "winkel", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Tupel.id,
                vertrag = de.TeutonStudio.TypSystem.AnschlussVertrag(typ = de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Tupel))),
            knoten.anschlüsse.firstOrNull { it.name == "ergebnis" }?.copy(art = MathematikAnschlussArten.Tupel.id)
                ?: AnschlussDaten(name = "ergebnis", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Tupel.id),
        )
        ziel == "polar" && getrennt -> listOf(
            knoten.anschlüsse.firstOrNull { it.name == "wert" }?.copy(art = MathematikAnschlussArten.Objekt.id)
                ?: AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id),
            AnschlussDaten(name = "radius", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id),
            AnschlussDaten(name = "winkel", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Tupel.id),
        )
        else -> null
    }
    if(spezial!=null) return knoten.copy(parameter=parameter,eigenschaften=knoten.eigenschaften+(ÜBERSETZER_ZIEL to KnotenEigenschaft.Text(ziel)),anschlüsse=spezial.map { neu ->
        val alt=knoten.anschlüsse.firstOrNull { it.name==neu.name && it.richtung==neu.richtung }
        neu.copy(id=alt?.id ?: neu.id,kannSichErweitern=false,dynamischErzeugt=false)
    })
    val vorhandene = knoten.anschlüsse.filter { it.name !in setOf("anker", "zielpunkt") }
    val basis = if (übersetzerZiel(knoten) == "matrixzerlegen" && ziel != "matrixzerlegen")
        vorhandene.filter { it.richtung == AnschlussRichtung.Eingang } + vorhandene.first { it.richtung == AnschlussRichtung.Ausgang }.copy(name = "ergebnis")
    else vorhandene
    val geoPorts = if (ziel in setOf("koordinatenmenge", "objektpunkt")) (if (ziel == "objektpunkt") listOf("anker") else listOf("anker", "zielpunkt")).mapIndexed { i, name ->
        knoten.anschlüsse.firstOrNull { it.name == name } ?: AnschlussDaten(name = name, richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id, reihenfolge = i + 1)
    } else emptyList()
    return knoten.copy(parameter=parameter,eigenschaften = knoten.eigenschaften + (ÜBERSETZER_ZIEL to KnotenEigenschaft.Text(ziel)),
        anschlüsse = (basis + geoPorts).map { p -> if (p.richtung == AnschlussRichtung.Ausgang)
            p.copy(art = ausgangArt, kannSichErweitern = false, dynamischErzeugt = false)
            else p.copy(art = if (ziel == "indexmethode") MathematikAnschlussArten.AtlasWert.id else MathematikAnschlussArten.Objekt.id,
                zulässigeArten = if (p.name in setOf("anker", "zielpunkt")) setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id, GeometrieAnschlussArten.Punkt.id) else when (ziel) {
                    "koordinatenmenge", "mittelpunkt", "objektpunkt" -> setOf(GeometrieAnschlussArten.Objekt.id)
                    "geometrie" -> setOf(MathematikAnschlussArten.Menge.id, MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.SpaltenVektor.id, MathematikAnschlussArten.ZeilenVektor.id)
                    "grad" -> setOf(MathematikAnschlussArten.Zahl.id)
                    "bogenmass" -> setOf(MathematikAnschlussArten.Winkel.id)
                    "matrixzerlegen" -> setOf(MathematikAnschlussArten.Matrix.id)
                    "abflachen", "polar", "kartesisch" -> setOf(MathematikAnschlussArten.Tupel.id)
                    "indexmethode" -> setOf(MathematikAnschlussArten.AtlasWert.id, MathematikAnschlussArten.Vektor.id, MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id, MathematikAnschlussArten.Matrix.id, MathematikAnschlussArten.Tensor.id)
                    "tupel" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id)
                    "zeile" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.SpaltenVektor.id, MathematikAnschlussArten.ZeilenVektor.id)
                    "spalte" -> setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.SpaltenVektor.id)
                    else -> emptySet()
                }, kannSichErweitern = false, dynamischErzeugt = false) })
}


const val RICHTUNG_WINKEL_ART = "mathematik.richtungWinkel"
const val RICHTUNG_WINKEL_VORZEICHEN = "richtung.vorzeichen"
const val RICHTUNG_WINKEL_LISTE = "richtung.winkelGrad"
const val RICHTUNG_WINKEL_ANZAHL = "richtung.n"

/** n natürliche Halbkreiswinkel definieren eine Richtung in R^(n+1). */
val RichtungWinkelVorlage = KnotenVorlage(
    RICHTUNG_WINKEL_ART, "Richtung (n-dimensional)", "Vektoren",
    "n ∈ ℕ₀ legt n Halbkreiswinkel und damit eine Richtung in Rⁿ⁺¹ fest. Für n > 0 wird die Richtung numerisch angenähert berechnet.",
    GraphGröße(250f, 120f),
    listOf(AnschlussDaten(name = "n", richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id),
        AnschlussDaten(name = "richtung", richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.SpaltenVektor.id)),
    standardParameter = mapOf(RICHTUNG_WINKEL_VORZEICHEN to "1", RICHTUNG_WINKEL_LISTE to "", RICHTUNG_WINKEL_ANZAHL to "0"),
)

object ÜbersetzungsKnotenVorlagen {
    val Übersetzer = KnotenVorlage(ÜBERSETZER_ART, "Übersetzer", "Strukturen",
        "Übersetzt Tupel, Zeilen, Spalten, kartesische und polare Koordinaten oder einen Term in eine Methode.", GraphGröße(220f, 96f),
        listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id, zulässigeArten = setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.ZeilenVektor.id)),
            AnschlussDaten(name = "ergebnis", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.SpaltenVektor.id)),
        standardEigenschaften = mapOf(ÜBERSETZER_ZIEL to KnotenEigenschaft.Text("spalte")),
        standardParameter = mapOf(ÜBERSETZER_POLAR_KONVENTION to PolarKonvention.SIGNED.name,"methode.tupelEinzelausgabe" to "true")).alsAdapterVorlage()
    val Abflachen = KnotenVorlage(TUPEL_ABFLACHEN_ART, "Tupel abflachen", "Strukturen",
        "Öffnet verschachtelte Tupel rekursiv von links nach rechts; andere Elemente bleiben erhalten.", GraphGröße(240f, 110f),
        listOf(AnschlussDaten(name = "tupel", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Tupel.id),
            AnschlussDaten(name = "tupel", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Tupel.id)))
    val alle = listOf(Übersetzer, Abflachen, RichtungWinkelVorlage)
}

/** Gemeinsame Methodenbildung für den historischen Adapter und den Übersetzer. */
internal fun termAlsMethode(k: KnotenAuswertungsKontext, termWert: BedingterWert): BedingterWert {
    termWert.gebundeneFormel?.let { f ->
        val methode=f.alsMethode(k.knoten.parameter["name"] ?: "f")
        val ids=f.quellen.filter { it.art==FormelVariablenArt.FREI }.map {it.id}.toSet()
        return termWert.copy(objekt=methode,gebundeneFormel=null,annahmen=methode.voraussetzungen,zielMenge=null,werteVorrat=null,latexDarstellung=null,
            variablenQuellen=termWert.variablenQuellen.filterNot {it.bindungsId in ids})
    }
    var term = termWert.mathematischesObjekt("Term der Methodenbildung")
    require(term !is Methode) { "Eine Methode ist kein Term für die Methodenbildung." }
    val parameterQuelle=k.eingänge["parameterQuelle"]
    var parameterObjekt=parameterQuelle?.objekt as? MathematischesObjekt
    var quellAnnahmen=termWert.annahmen+parameterQuelle?.annahmen.orEmpty()
    var quellenWerte=termWert.variablenQuellen+parameterQuelle?.variablenQuellen.orEmpty()
    val strukturQuellen=quellenWerte.filter { it.strukturBindung!=null }.groupBy { it.strukturBindung!!.id }
    val freieVorher=(term.freieMethodenParameter()+parameterObjekt?.freieMethodenParameter().orEmpty()+quellAnnahmen.flatMap { it.freieMethodenParameter() }).toSet()
    val geplanteNamen=strukturQuellenNamen(quellenWerte,freieVorher)
    val umbenennungen=linkedMapOf<String,MathematischesObjekt>()
    val quellenNamen=mutableMapOf<String,String>()
    strukturQuellen.values.forEach { zusammen ->
        val bindung=zusammen.first().strukturBindung!!
        require(zusammen.all { it.strukturBindung!!.parameterName==bindung.parameterName && it.strukturBindung!!.werteVorrat==bindung.werteVorrat }) { "Die strukturierte Parameterquelle ist widersprüchlich." }
        val name=geplanteNamen.getValue(bindung.id)
        zusammen.forEach { q ->
            val frei=strukturQuellenParameter(q,freieVorher) ?: return@forEach
            require(q.strukturBindung!!.komponentenPfad.isEmpty()) { "Historische Komponentenquellen benötigen eine explizite Argumentpackungsmigration." }
            val parameter=when(frei) { is TypisiertesElement -> frei.copy(name=name,latex=name); is Variable -> frei.copy(name=name,latex=name,bindungsId=null); is WinkelParameter -> frei.copy(name=name,latex=name,bindungsId=null); is AllgemeinerParameter -> frei.copy(name=name,latex=name,bindungsId=null); else -> error("Nicht unterstützte strukturierte Parameterquelle.") }
            umbenennungen[frei.bindungsId ?: q.name]=parameter;quellenNamen[bindung.id]=name
        }
    }
    if(umbenennungen.isNotEmpty()) {
        term=ersetze(term,umbenennungen)
        parameterObjekt=parameterObjekt?.let { ersetze(it,umbenennungen) }
        quellAnnahmen=quellAnnahmen.map { ersetze(it,umbenennungen) as Aussage }.toSet()
        quellenWerte=quellenWerte.map { q -> quellenNamen[q.strukturBindung?.id]?.let { q.copy(name=it,strukturBindung=q.strukturBindung?.copy(parameterName=it)) } ?: q }
    }
    val zusätzlich=parameterObjekt?.freieMethodenParameter().orEmpty()
    val freie = (term.freieMethodenParameter()+zusätzlich).associateBy { it.name }
    val quellen = quellenWerte.filter { it.name in freie }.groupBy { it.name }
    val fehlende = freie.keys - quellen.keys
    require(fehlende.isEmpty()) { "Für die freien Parameter ${fehlende.sorted().joinToString(", ")} fehlt eine Parameter- oder Außenquelle." }
    val vorräte = quellen.mapValues { (name, qs) ->
        require(qs.map { it.werteVorrat }.distinct().size == 1 && qs.map { it.alsMethodenParameter }.distinct().size == 1) {
            "Die gleichnamigen Quellen für '$name' widersprechen einander."
        }
        qs.first().werteVorrat
    }
    val bindbare = quellen.filterValues { it.first().alsMethodenParameter }.keys
    val gespeichert = k.knoten.parameter["argumentReihenfolge"].orEmpty().split(',').map(String::trim)
        .filter { it in bindbare }.distinct()
    val namen = gespeichert + (bindbare - gespeichert.toSet()).sorted()
    val ziel = inferiereZielmenge(term, vorräte, quellAnnahmen)
    val einzelausgabe=term is Tupel && k.knoten.parameter["methode.tupelEinzelausgabe"] == "true"
    val strukturiertesEinzelergebnis=einzelausgabe || term !is Tupel && ziel is Tupelraum
    val ausgabeNamen = if (term is Tupel && !einzelausgabe) term.elemente.indices.map { "element${it + 1}" } else listOf("wert")
    val methode = MathematischeMethode(name = k.knoten.parameter["name"] ?: "f", parameter = namen.map { name ->
        if (quellen.getValue(name).first().lösungsRolle == LösungsQuellenRolle.Parameter)
            AllgemeinerParameter(name) else freie.getValue(name)
    },
        vorschrift = if (strukturiertesEinzelergebnis) Tupel(listOf(term)) else term, zielMenge = if (strukturiertesEinzelergebnis) Tupelraum(listOf(ziel)) else ziel, werteVorräte = namen.associateWith { vorräte.getValue(it) }, ausgabeNamen = ausgabeNamen,
        argumentIdentitäten = namen.associateWith { name ->
            val quelle = quellen.getValue(name).first()
            quelle.strukturBindung?.id ?: quelle.bindungsId?.let { id -> id + (quelle.bindungsName?.let { "::$it" } ?: "") } ?: quelle.knotenId.wert
        }, voraussetzungen = quellAnnahmen)
    return termWert.copy(objekt = methode, annahmen=quellAnnahmen, zielMenge = null, werteVorrat = null, latexDarstellung = null,
        reelleVariablen = termWert.reelleVariablen.filterKeys { it !in namen },
        variablenQuellen = quellenWerte.filterNot { it.name in namen && it.alsMethodenParameter })
}

fun MathematikAuswerterRegister.registriereÜbersetzungsKnoten() {
    registriere(RICHTUNG_WINKEL_ART) { k ->
        val vorzeichen = k.knoten.parameter[RICHTUNG_WINKEL_VORZEICHEN]?.toIntOrNull()
            ?: error("Richtungsvorzeichen muss -1 oder +1 sein.")
        val roh = k.knoten.parameter[RICHTUNG_WINKEL_LISTE].orEmpty()
        val winkel = if (roh.isBlank()) emptyList() else roh.split(';').map { eintrag ->
            eintrag.trim().toDoubleOrNull() ?: error("Ungültiger Halbkreiswinkel: '$eintrag'.")
        }
        val n = k.eingänge["n"]?.let { quelle ->
            val zahl = quelle.objekt as? RationaleZahl
                ?: error("n muss eine konkrete natürliche Zahl sein.")
            require(zahl.nenner == java.math.BigInteger.ONE && zahl.zähler.signum() >= 0) {
                "n muss eine natürliche ganze Zahl einschließlich 0 sein."
            }
            try { zahl.zähler.intValueExact() } catch (_: ArithmeticException) {
                error("n überschreitet den unterstützten Dimensionsbereich.")
            }
        } ?: k.knoten.parameter[RICHTUNG_WINKEL_ANZAHL]?.toIntOrNull()
            ?: error("n muss eine natürliche ganze Zahl sein.")
        require(n in 0..MAXIMALE_RICHTUNGS_WINKEL) { "n muss zwischen 0 und $MAXIMALE_RICHTUNGS_WINKEL liegen." }
        require(winkel.size <= n) { "Für n = $n sind höchstens $n Halbkreiswinkel zulässig." }
        // Nicht angegebene Winkel entsprechen einer Rotation um 0 Grad.
        val vollständig = winkel + List(n - winkel.size) { 0.0 }
        val angenähert = n > 0
        KnotenAuswertungsErgebnis(
            mapOf("richtung" to BedingterWert(richtungAlsSpaltenVektor(vorzeichen, vollständig),
                annahmen = k.eingänge.values.flatMap { it.annahmen }.toSet(),
                übersetzungen = if (angenähert) ÜbersetzungsMetadaten(kontext = ÜbersetzungsKontext(
                    datenGenauigkeit = ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT,
                )) else ÜbersetzungsMetadaten())),
            warnungen = if (angenähert) listOf("Die Richtung wurde numerisch angenähert berechnet.") else emptyList(),
        )
    }
    registriere(ÜBERSETZER_ART) { k ->
        val ziel = übersetzerZiel(k.knoten)
        if(ziel=="indexmethode") indexDefinitionsBaustein(k)?.let { return@registriere it }
        val getrennt = übersetzerPolarGetrennt(k.knoten)
        val wert = if (ziel == "kartesisch" && getrennt) {
            val radius = k.eingänge["radius"] ?: error("Radius fehlt.")
            val winkel = k.eingänge["winkel"] ?: error("Winkeltupel fehlt.")
            val richtung=winkel.mathematischesObjekt("Polarwinkel")
            val tupel=when(richtung) { is Tupel -> richtung; is OrientierterVektor -> Tupel(richtung.werte); else -> error("Richtung muss ein Winkeltupel sein.") }
            übersetzterWert(radius, Tupel(listOf(radius.mathematischesObjekt("Radius"), tupel))).copy(
                annahmen = radius.annahmen + winkel.annahmen,
                variablenQuellen = radius.variablenQuellen + winkel.variablenQuellen,
            )
        } else k.eingänge["wert"] ?: error("Der zu übersetzende Wert fehlt.")
        require(ziel in ÜBERSETZER_ZIELE) { "Unbekanntes Übersetzungsziel '$ziel'." }
        if(ziel=="rueckfuehrung") {
            val zeugnis=k.knoten.parameter[ÜBERSETZER_RÜCKFÜHRUNGSZEUGNIS]?.let(ÜbersetzungsCodec::dekodiereZeugnis) ?: wert.übersetzungen.aktuell ?: error("Kein aktuelles Übersetzungszeugnis vorhanden. Wählen Sie ein Herkunftszeugnis ausdrücklich aus.")
            return@registriere when(val r=ÜbersetzungsDienst.rückübersetze(wert.mathematischesObjekt("Rückführung"),zeugnis)) {
                is ÜbersetzungsErgebnis.NichtDefiniert -> KnotenAuswertungsErgebnis(emptyMap(),fehler=r.beschreibung,übersetzungsDiagnose=r,eingänge=k.eingänge)
                is ÜbersetzungsErgebnis.Erfolg -> KnotenAuswertungsErgebnis(mapOf("ergebnis" to wert.copy(objekt=r.wert,gebundeneFormel=null,annahmen=wert.annahmen+r.kontext.annahmen,zielMenge=r.zeugnis.zielTräger,werteVorrat=r.kontext.werteBereich,komponentenMengen=r.kontext.komponentenRäume,übersetzungen=wert.übersetzungen.mit(r.zeugnis,r.kontext))),eingänge=k.eingänge,bezeugteAusgaben=setOf("ergebnis"))
            }
        }
        if (ziel == "matrixzerlegen") return@registriere werteMatrixZerlegenAus(k, "wert")
        if(ziel=="indexmethode" && k.knoten.parameter["indexmethode.auswahl"]=="nichtnullIndikator") {
            val methode=wert.objekt as? MathematischeMethode ?: error("Die Indikatorbildung benötigt eine skalare Indexmethode.")
            val term=methode.vorschrift as? ZahlAusdruck ?: error("Die Indikatorbildung benötigt Zahlwerte.")
            val neu=methode.copy(vorschrift=FallAusdruck(RationaleZahl.Eins,Ungleichheit(term,RationaleZahl.Null),RationaleZahl.Null),zielMenge=Tupelraum(listOf(NatürlicheZahlen))).benenneIndexParameter(methode.parameter.map { it.name },methode.parameter.map { if(Regex("^__index_*::").containsMatchIn(it.name)) it.name.substringAfter("::") else it.zuLatex() })
            return@registriere KnotenAuswertungsErgebnis(mapOf("ergebnis" to wert.copy(objekt=neu,zielMenge=null,werteVorrat=null,latexDarstellung=null)),eingänge=k.eingänge)
        }
        if(ziel=="indexmethode" && k.knoten.parameter["indexmethode.auswahl"] in setOf("rationaleMatrix","matrixQuelle")) {
            val matrix=wert.objekt as? Matrix ?: error("Die Bereichsprüfung benötigt eine Matrix.")
            require(k.knoten.parameter["indexmethode.auswahl"]=="matrixQuelle" || matrix.zeilen.flatten().all { vereinfache(it) is RationaleZahl }) { "Diese Definition benötigt rationale Matrixeinträge." }
            return@registriere KnotenAuswertungsErgebnis(mapOf("ergebnis" to wert),eingänge=k.eingänge)
        }
        if(ziel=="indexmethode" && k.knoten.parameter["indexmethode.auswahl"]=="symbolischeMatrix") {
            val matrix=wert.objekt as? Matrix ?: error("Die Strukturabstraktion benötigt eine Matrix.")
            val frei=matrix.enthalteneMethodenParameter().map { it.name }.toSet()
            var name="__struktur"
            while(frei.any { it.startsWith(name) }) name+="_"
            val parameter=TypisiertesElement(name,"mathematik.matrix",latex="X",strukturForm=listOf(RationaleZahl.von(matrix.zeilenAnzahl.toLong()),RationaleZahl.von(matrix.spaltenAnzahl.toLong())))
            val symbolisch=Matrix(List(matrix.zeilenAnzahl) { i -> List(matrix.spaltenAnzahl) { j -> SymbolischeZahlKomponente(parameter,RationaleZahl.von((i+1).toLong()),listOf(RationaleZahl.von((j+1).toLong()))) } })
            val bereich=maximaleZahlenGrundmenge(matrix.zeilen.flatten().map { inferiereZahlenWertevorrat(it,wert.variablenQuellen.associate { q -> q.name to q.werteVorrat },wert.annahmen) })
            val quelle=VariablenQuelle(k.knoten.id,name,Matrizenraum(matrix.zeilenAnzahl,matrix.spaltenAnzahl,bereich))
            return@registriere KnotenAuswertungsErgebnis(mapOf("ergebnis" to wert.copy(objekt=symbolisch,variablenQuellen=wert.variablenQuellen+quelle,zielMenge=null,werteVorrat=null,latexDarstellung=null)),eingänge=k.eingänge)
        }
        if(ziel=="indexmethode" && k.knoten.parameter["indexmethode.auswahl"]=="iterationsbereich") {
            val exponent=vereinfache(wert.objekt as? ZahlAusdruck ?: error("Eine Iterationsanzahl muss ganzzahlig sein.")) as? RationaleZahl ?: error("Die Iterationsanzahl muss konkret ganzzahlig sein.")
            require(exponent.nenner==java.math.BigInteger.ONE) { "Die Iterationsanzahl muss ganzzahlig sein." }
            require(exponent.zähler.abs()<=java.math.BigInteger.valueOf(100_000)) { "Der Iterationsbereich überschreitet das Materialisierungsbudget." }
            val n=exponent.zähler.abs().toInt()
            require(n<=100_000) { "Der Iterationsbereich überschreitet das Materialisierungsbudget." }
            return@registriere KnotenAuswertungsErgebnis(mapOf("ergebnis" to BedingterWert(EndlicheMenge((1..n).map { RationaleZahl.von(it.toLong()) }.toSet()),wert.annahmen)),eingänge=k.eingänge)
        }
        val ergebnis = if (ziel == "methode" && k.knoten.parameter["uebersetzer.zeugnis"] == "skalarprodukt") {
            val p = k.eingänge["exponent"]?.objekt as? MathematischesObjekt
            if (p != null && nichtnegativerGanzzahlExponent(p) < 2) {
                val dummy = MathematischeMethode("ungenutztesSkalarprodukt", listOf(AllgemeinerParameter("a"), AllgemeinerParameter("b")), RationaleZahl.Eins, RationaleZahlen)
                return@registriere KnotenAuswertungsErgebnis(mapOf("ergebnis" to BedingterWert(dummy)), eingänge = k.eingänge)
            }
            val zertifikat=wert.objekt as? Methode ?: error("Die Zertifikatskarte liefert keine Methode.")
            require(zertifikat.parameter.isEmpty()) { "Eine Skalarprodukt-Zertifikatskarte darf keine öffentlichen Eingänge besitzen." }
            val aussage=zertifikat.wendeAn(emptyList()) as? BegriffsAussage ?: error("Die Zertifikatskarte muss eine Skalarprodukt-Aussage ausgeben.")
            require(aussage.pruefung.begriffsId==SKALARPRODUKT_BEGRIFF_ID && aussage.pruefung.status!=NachweisStatus.Widerlegt) { "Die ausgewählte Definition ist als Skalarprodukt widerlegt oder inkompatibel." }
            val zeugnis=aussage.pruefung.zeugnis as? SkalarproduktZeugnis
            val methode=zeugnis?.methode ?: aussage.pruefung.kandidat?.belegung?.get("methode") as? Methode ?: error("Die Aussage enthält keine ausführbare Skalarproduktmethode.")
            val bereiche=listOf("links","rechts").map { name ->
                val quelle=k.eingänge[name]?.mathematischesObjekt("Skalarproduktoperand") ?: error("Skalarproduktoperand fehlt.")
                when(val ansicht=quelle.numerischeKomponentenAnsicht()) {
                    is StrukturPruefung.Gueltig -> ansicht.wert.zahlBereich
                    is StrukturPruefung.Bedingt -> error(ansicht.bedingungen.joinToString())
                    is StrukturPruefung.Ungueltig -> error(ansicht.grund)
                    is StrukturPruefung.Unentscheidbar -> error(ansicht.grund)
                }
            }
            var bereich=maximaleZahlenGrundmenge(bereiche).skalarproduktZahlbereichOderNull() ?: error("Der Skalarkörper ist nicht registriert.")
            if(k.knoten.parameter[SKALARPRODUKT_ZAHLBEREICH_PARAMETER]==FundamentalerZahlbereich.QUATERNION.id) bereich=FundamentalerZahlbereich.QUATERNION
            val linearitaet=SkalarproduktLinearitaet.entries.firstOrNull { it.name==k.knoten.parameter[SKALARPRODUKT_LINEARITAET_PARAMETER] } ?: SkalarproduktLinearitaet.RECHTSLINEAR
            if(zeugnis!=null) {
                val erwartet=if(zeugnis.zahlbereich==FundamentalerZahlbereich.REELL && bereich.ordinal<FundamentalerZahlbereich.REELL.ordinal) FundamentalerZahlbereich.REELL else bereich
                skalarproduktZertifikatFehler(aussage,erwartet,linearitaet)?.let(::error)
            }
            wert.copy(objekt=methode,annahmen=wert.annahmen+(if(zeugnis==null) setOf(aussage) else emptySet()),zielMenge=null,werteVorrat=null,latexDarstellung=null)
        } else if (ziel == "methode") termAlsMethode(k, wert)
            else if (ziel == "indexmethode" && k.knoten.parameter["indexmethode.auswahl"] != null) {
                val quelle = wert.mathematischesObjekt("Indexauswahl")
                val auswahl = k.knoten.parameter.getValue("indexmethode.auswahl")
                val methode = if(auswahl=="umformen") {
                    val form=k.knoten.parameter["indexmethode.form"].orEmpty().split(',').map { it.trim().toIntOrNull() ?: error("Die Zielform muss ganze Achsenlängen enthalten.") }
                    umgeformteStrukturIndexMethode(quelle,form,wert.variablenQuellen.associate { it.name to it.werteVorrat },wert.annahmen)
                } else if(auswahl.startsWith("zyklus3:")) {
                    val werte = when(quelle) { is OrientierterVektor -> quelle.werte; is Tupel -> quelle.elemente.map { it as? ZahlAusdruck ?: error("Komponenten müssen Zahlen sein.") }; else -> error("Zyklische Auswahl benötigt einen Vektor.") }
                    require(werte.size==3) { "Diese zyklische Auswahl benötigt drei Komponenten." }
                    val versatz=auswahl.substringAfter(':').toInt()
                    strukturIndexMethode(SpaltenVektor(werte.indices.map { werte[(it+versatz)%3] }))
                } else {
                    val matrix=matrixAusZweiachsigerStruktur(quelle)
                    val vorräte=wert.variablenQuellen.associate { it.name to it.werteVorrat }
                    require(auswahl=="einheit" || matrix.zeilen.flatten().none { inferiereZahlenWertevorrat(it,vorräte,wert.annahmen).fundamentalerZahlbereichOderNull()==FundamentalerZahlbereich.QUATERNION }) { "Die Permutationsdefinition benötigt einen kommutativen Zahlbereich." }
                    if(auswahl.startsWith("minoren:")) {
                        require(matrix.zeilen.flatten().all { vereinfache(it) is RationaleZahl }) { "Die exakte Minor-Familie benötigt rationale Matrixeinträge." }
                        minorenFamilienIndexMethode(matrix,auswahl.substringAfter(':'))
                    } else if(auswahl=="einheit") {
                        require(matrix.zeilenAnzahl==matrix.spaltenAnzahl) { "Die Identitätskomponenten benötigen eine quadratische Matrix." }
                        if(k.knoten.parameter["indexmethode.rationalQuelle"]=="true") require(matrix.zeilen.flatten().all { vereinfache(it) is RationaleZahl }) { "Die exakte Matrixpolynomdefinition benötigt rationale Matrixeinträge." }
                        strukturIndexMethode(Matrix(List(matrix.zeilenAnzahl) { i -> List(matrix.spaltenAnzahl) { j -> if(i==j) RationaleZahl.Eins else RationaleZahl.Null } }))
                    } else if(auswahl=="alternierend") alternierendeAchsenMethode(matrix) else permutationsKomponentenMethode(matrix,minor=auswahl.startsWith("minor"),vorzeichen=auswahl.endsWith("vorzeichen"))
                }
                wert.copy(objekt=methode,zielMenge=null,werteVorrat=null,komponentenMengen=null,latexDarstellung=null)
            }
            else if (ziel == "indexmethode") wert.copy(objekt = indexMethodeFürBedingtenWert(wert,k.knoten.parameter["indexmethode.matrixFaktor"]=="true").let { m -> if (k.knoten.parameter["indexmethode.rollenKontext"] != "true" && k.knoten.parameter["indexmethode.parameterPräfix"] != null) m.benenneIndexParameter(m.parameter.indices.map { k.knoten.parameter.getValue("indexmethode.parameterPräfix") + (it + 1) }) else m }.benenneIndexParameter(if(k.knoten.parameter["indexmethode.rollenKontext"]=="true") emptyList() else k.knoten.parameter["indexmethode.parameterNamen"].orEmpty().split(',').map(String::trim).filter(String::isNotEmpty)).let { m ->
                val modus = k.knoten.parameter["indexmethode.achsenModus"]
                if (modus == null) m else {
                    val verbunden = k.eingänge["achsen"]?.objekt as? Tupel
                    val achsen = if (verbunden != null) verbunden.alsPositiveIndexListe()
                    else k.knoten.parameter["indexmethode.achsen"].orEmpty().split(',').filter { it.isNotBlank() }.map { it.trim().toIntOrNull() ?: error("Ungültige Achsenspezifikation.") }
                    m.transformiereIndexAchsen(modus, if (modus == "schnitt" && achsen.size == 1) achsen + (k.knoten.parameter["indexmethode.schnittIndex"]?.toIntOrNull() ?: error("Der Schnittindex fehlt.")) else if (achsen.isEmpty() && modus == "permutation") standardTensorPermutation(m.parameter.size).map { it + 1 } else achsen)
                }
            }, zielMenge = null, werteVorrat = null, komponentenMengen = null, latexDarstellung = null)
            else {
                val objekt = wert.mathematischesObjekt("Übersetzer")
                val neu = when (ziel) {
                    "koordinatenmenge", "mittelpunkt", "objektpunkt" -> {
                        val geometrie = objekt as? GeometrischerAusdruck ?: error("Erwartet wird ein geometrisches Objekt.")
                        val punkt = when (k.knoten.parameter["geometrie.anker"] ?: if (ziel == "mittelpunkt") "mittelpunkt" else if (ziel == "objektpunkt") "punkt" else "keiner") {
                            "mittelpunkt" -> geometrieMittelpunkt(geometrie)
                            "punkt" -> {
                                val anker = k.eingänge["anker"]?.mathematischesObjekt("Objektpunkt") ?: error("Der ausgewählte Objektpunkt fehlt.")
                                val punkt = if (anker is GeometriePunkt) anker.also { require(it.raum == geometrie.raum) { "Anker liegt in einem anderen Raum." } }
                                    else GeometriePunkt("A", geometrie.raum, Tupel(unterraumKomponenten(anker)))
                                val koordinaten = punkt.koordinaten ?: error("Ankerkoordinaten fehlen.")
                                require(ElementBeziehung(koordinaten, geometrieKoordinatenMenge(geometrie)).entscheide().wahrheitswert == Wahrheitswert.Wahr) { "Der gewählte Objektpunkt gehört nicht nachweislich zum Objekt." }
                                koordinaten
                            }
                            else -> null
                        }
                        if (ziel == "mittelpunkt" || ziel == "objektpunkt") punkt ?: error("Ein Objektpunkt muss ausgewählt werden.")
                        else {
                            val menge = geometrieKoordinatenMenge(geometrie)
                            if (punkt == null) menge else {
                                require(punkt.elemente.size == geometrie.raum.dimension) { "Ankerdimension passt nicht." }
                                val zielpunkt = k.eingänge["zielpunkt"]?.mathematischesObjekt("Zielpunkt")?.let { if (it is GeometriePunkt) { require(it.raum == geometrie.raum); it.zahlKoordinaten() ?: error("Zielpunktkoordinaten fehlen.") } else unterraumKomponenten(it) } ?: List(geometrie.raum.dimension) { RationaleZahl.Null }
                                require(zielpunkt.size == geometrie.raum.dimension) { "Zielpunktdimension passt nicht." }
                                VerschobeneKoordinatenMenge(menge, zielpunkt.mapIndexed { i, x -> vereinfache(subtraktion(x, punkt.elemente[i] as ZahlAusdruck)) })
                            }
                        }
                    }
                    "geometrie" -> when (objekt) {
                        is UnterraumMenge -> GeometrischeMengenRealisierung(objekt, EuklidischerRaum("E", objekt.dimension))
                        is KoordinatenBild -> objekt.objekt
                        is VerschobeneKoordinatenMenge -> GeometrischeMengenRealisierung(objekt, EuklidischerRaum("E", objekt.verschiebung.size))
                        is EndlicheMenge -> {
                            val t = objekt.elemente.singleOrNull() as? Tupel ?: error("Nur einelementige Punktmengen besitzen eine Punktrealisierung.")
                            require(t.elemente.all { it is ZahlAusdruck && it !is KomplexeZahl && ElementBeziehung(it, ReelleZahlen).entscheide().wahrheitswert != Wahrheitswert.Lüge }) { "Geometrie benötigt reelle Koordinaten." }
                            GeometriePunkt("P", EuklidischerRaum("E", t.elemente.size), t)
                        }
                        is Tupel, is OrientierterVektor -> {
                            val koordinaten = unterraumKomponenten(objekt)
                            require(koordinaten.all { ElementBeziehung(it, ReelleZahlen).entscheide().wahrheitswert != Wahrheitswert.Lüge }) { "Geometrie benötigt reelle Koordinaten." }
                            GeometriePunkt("P", EuklidischerRaum("E", koordinaten.size), Tupel(koordinaten))
                        }
                        else -> error("Diese Menge besitzt keine erkannte exakte geometrische Realisierung.")
                    }

                    "grad" -> {
                        val reell = objekt as? ZahlAusdruck ?: error("R → Grad erwartet Bogenmaß.")
                        require(istNachweisbarReell(reell, { name -> wert.variablenQuellen.any { it.name == name.name && prüfeTeilmenge(it.werteVorrat, ReelleZahlen).wahrheitswert == Wahrheitswert.Wahr } }, wert.annahmen)) { "Das Bogenmaß muss nachweisbar reell sein." }
                        winkelAusBogenmass(reell)
                    }
                    "bogenmass" -> winkelBogenmass(objekt as? WinkelAusdruck ?: error("Grad → R erwartet einen Winkel."), übersetzerVertreter(k.knoten))
                    "polar" -> Polarkoordinaten.polar(when(objekt) { is Tupel -> objekt; is OrientierterVektor -> Tupel(objekt.werte); else -> error("Erwartet wird ein kartesisches Tupel oder ein Vektor.") },übersetzerPolarKonvention(k.knoten))
                    "kartesisch" -> Polarkoordinaten.kartesisch(objekt as? Tupel ?: error("Erwartet wird ein Polartupel."), übersetzerPolarKonvention(k.knoten), übersetzerEindimensional(k.knoten) || k.knoten.parameter["uebersetzer.r1Automatisch"]=="true" && (objekt as? Tupel)?.elemente?.getOrNull(1) is ZahlAusdruck)
                    else -> if (ziel == "tupel" && objekt is Tupel || ziel == "spalte" && objekt is SpaltenVektor || ziel == "zeile" && objekt is ZeilenVektor) objekt else übersetzeStruktur(objekt, ziel)
                }
                val geoKoordinaten = if (ziel in setOf("koordinatenmenge", "mittelpunkt", "objektpunkt", "geometrie")) k.eingänge.values.flatMap { eingang ->
                    when (val x = eingang.objekt) {
                        is GeometrischerAusdruck -> geometrieBekannteKoordinaten(x)
                        is Tupel, is OrientierterVektor -> unterraumKomponenten(x)
                        is EndlicheMenge -> x.elemente.filterIsInstance<Tupel>().flatMap { unterraumKomponenten(it) }
                        else -> emptyList()
                    }
                } else emptyList()
                require(geoKoordinaten.none { it is KomplexeZahl || ElementBeziehung(it, ReelleZahlen).entscheide().wahrheitswert == Wahrheitswert.Lüge }) { "Geometrische Koordinaten einschließlich Anker und Zielpunkt müssen reell sein." }
                val geometrieAnnahmen = geoKoordinaten.map { ElementBeziehung(it, ReelleZahlen) }.filter { it.entscheide().wahrheitswert != Wahrheitswert.Wahr } + when (objekt) {
                    is GeometrischerAusdruck -> geometrieBekannteKoordinaten(objekt).map { ElementBeziehung(it, ReelleZahlen) }.filter { it.entscheide().wahrheitswert != Wahrheitswert.Wahr }
                    else -> emptyList()
                }
                val radius = if (ziel == "kartesisch") (objekt as? Tupel)?.elemente?.firstOrNull() as? ZahlAusdruck else null
                val radiusBedingung = radius?.let { Vergleich(it, VergleichsArt.GrößerGleich, RationaleZahl.Null) }
                val radialQuellen = wert.variablenQuellen.map { q ->
                    if (radius is Variable && q.name == radius.name) q.copy(werteVorrat = if (q.werteVorrat == ReelleZahlen)
                        BeschraenkteZahlmenge(FundamentalerZahlbereich.REELL, RationaleZahl.Null, true)
                        else Schnitt(listOf(q.werteVorrat, BeschraenkteZahlmenge(FundamentalerZahlbereich.REELL, RationaleZahl.Null, true))))
                    else q
                }
                val quelle = wert.copy(annahmen = k.eingänge.values.flatMap { it.annahmen }.toSet() + geometrieAnnahmen + listOfNotNull(radiusBedingung), variablenQuellen = radialQuellen)
                übersetzterWert(if (ziel in setOf("polar", "kartesisch", "koordinatenmenge", "mittelpunkt", "objektpunkt", "geometrie")) quelle.copy(zielMenge = null, werteVorrat = null, komponentenMengen = null) else quelle, neu)
            }
        val rollenErgebnis=if(ziel=="indexmethode" && k.knoten.parameter["indexmethode.rollenKontext"]=="true") {
            val methode=ergebnis.objekt as MathematischeMethode
            val freieNamen=k.eingänge.values.flatMap { (it.objekt as? MathematischesObjekt)?.enthalteneMethodenParameter().orEmpty() }.map { it.name }
            var präfix="__index"
            while(freieNamen.any { it.startsWith(präfix) }) präfix+="_"
            val auswahl=k.knoten.parameter["indexmethode.auswahl"]
            val explizit=k.knoten.parameter["indexmethode.parameterNamen"].orEmpty().split(',').filter { it.isNotBlank() }
            val rollen=when {
                explizit.isNotEmpty() -> explizit
                k.knoten.parameter["indexmethode.parameterPräfix"]!=null -> methode.parameter.indices.map { k.knoten.parameter.getValue("indexmethode.parameterPräfix")+(it+1) }
                auswahl=="minoren:komponenten" -> listOf("d","p","k")
                auswahl=="minoren:grade" -> listOf("d")
                auswahl=="minoren:vorzeichen" -> listOf("p")
                auswahl=="permutationen" -> listOf("p","k")
                auswahl=="minor" -> listOf("i","j","p","k")
                auswahl=="alternierend" -> listOf("i","j")
                auswahl?.endsWith("vorzeichen")==true -> listOf("p")
                else -> methode.parameter.indices.map { "i${it+1}" }
            }
            val anzeigen=rollen.map { rolle ->
                var name=rolle
                var suffix=0
                while(name in freieNamen) name="${rolle}_${++suffix}"
                val teile=Regex("^(.+?)[_]?([0-9]+)$").matchEntire(name)
                if(teile==null) name else "${teile.groupValues[1].trimEnd('_')}_{${teile.groupValues[2]}}"
            }
            ergebnis.copy(objekt=methode.benenneIndexParameter(rollen.map { "$präfix::$it" },anzeigen))
        } else ergebnis
        if(ziel=="polar" && getrennt) {
            val polar=rollenErgebnis.objekt as? Tupel ?: error("Polar muss ein Zweiertupel sein.")
            KnotenAuswertungsErgebnis(mapOf("radius" to übersetzterWert(rollenErgebnis,polar.elemente[0]),"winkel" to übersetzterWert(rollenErgebnis,polar.elemente[1])),eingänge=k.eingänge)
        } else KnotenAuswertungsErgebnis(mapOf("ergebnis" to rollenErgebnis), eingänge = k.eingänge)
    }

    bezeugeÜbersetzer(ÜBERSETZER_ART) { k ->
        if (k.knoten.parameter["indexmethode.baustein"] != null) ÜbersetzungsZiel.INDEXMETHODE else ÜbersetzungsZiel.vonId(übersetzerZiel(k.knoten))
    }
}

internal fun übersetzterWert(quelle: BedingterWert, ergebnis: MathematischesObjekt): BedingterWert = quelle.copy(
    objekt = ergebnis, latexDarstellung = null, werteVorrat = null, elementArt = null,
    komponentenMengen = when (ergebnis) {
        is Tupel, is OrientierterVektor -> quelle.komponentenMengen ?: (quelle.zielMenge as? Tupelraum)?.komponenten
        else -> null
    },
    zielMenge = strukturRaumÜbersetzung(quelle.komponentenMengen?.let(::Tupelraum) ?: quelle.zielMenge ?: quelle.werteVorrat, ergebnis) ?: inferiereZielmenge(ergebnis, listOf(quelle).rechnerQuellBereiche(), quelle.annahmen),
)

/** Gebundene Strukturparameter behalten ihre Identität; das neutrale Element liefert nur die Form. */
private fun indexMethodeFürBedingtenWert(wert:BedingterWert,matrixFaktor:Boolean):MathematischeMethode {
    val quelle=wert.mathematischesObjekt("Indexmethode")
    val vorbild=(quelle as? MethodenParameter)?.let { p -> wert.variablenQuellen.singleOrNull { it.name==p.name && it.bindungsWert!=null }?.bindungsWert }
    val konkret=quelle is Tupel || quelle is OrientierterVektor || quelle is Tensorartig
    return strukturIndexMethode(if(matrixFaktor && konkret) quelle.alsMatrixFaktor() else quelle,
        wert.variablenQuellen.associate { it.name to it.werteVorrat },wert.annahmen,
        if(matrixFaktor) vorbild?.alsMatrixFaktor() else vorbild)
}

const val ÜBERSETZER_RÜCKFÜHRUNGSZEUGNIS = "uebersetzer.rueckfuehrungsZeugnis"
fun konfiguriereRückführungsZeugnis(k:KnotenDaten,z:ÜbersetzungsZeugnis?):KnotenDaten {
    require(übersetzerZiel(k)=="rueckfuehrung")
    z?.let { require(it.prüfeÜbertragung().rückführungsplan!=null) { "Kein Strukturplan vorhanden." } }
    return k.copy(parameter=if(z==null) k.parameter-ÜBERSETZER_RÜCKFÜHRUNGSZEUGNIS else k.parameter+(ÜBERSETZER_RÜCKFÜHRUNGSZEUGNIS to ÜbersetzungsCodec.kodieren(z)))
}

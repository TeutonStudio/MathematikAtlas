package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussBelegung
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.BenannteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.FundamentalerZahlbereich
import de.TeutonStudio.MathematikRechenSystem.kern.GaußscheGanzeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.GaußschePrimzahlen
import de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.KomplexeZahl
import de.TeutonStudio.MathematikRechenSystem.kern.KomplexeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.MengenKonstante
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.kartesischePotenz
import de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.OrientierterVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Primzahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.SpaltenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.VektorQuelle
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerAnfrage
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.ZeilenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.KartesischerKoordinatenVertrag
import de.TeutonStudio.MathematikRechenSystem.kern.einheitsVektorWerte
import java.math.BigInteger

const val MENGEN_KNOTEN_ART = "mathematik.menge"
const val MENGEN_KNOTEN_AUSWAHL = "mengeAuswahl"
const val VEKTOR_RECHNER_OPERATOR = "operator"
const val VEKTOR_RECHNER_AUSGANG = "skalar"
const val EINHEITSVEKTOR_POSITION = "position"
const val EINHEITSVEKTOR_DIMENSION = "dimension"
private const val STANDARDWERT_PREFIX = "standardwert."

enum class MengenKnotenAuswahl(val operator: MengenKonstante) {
    LEERE_MENGE(MengenKonstante.LEER),
    NATUERLICHE_ZAHLEN(MengenKonstante.N),
    NATUERLICHE_MIT_NULL(MengenKonstante.N0),
    GANZE_ZAHLEN(MengenKonstante.Z),
    RATIONALE_ZAHLEN(MengenKonstante.Q),
    REELLE_ZAHLEN(MengenKonstante.R),
    KOMPLEXE_ZAHLEN(MengenKonstante.C),
    PRIMZAHLEN(MengenKonstante.P),
    GAUSSSCHE_GANZE(MengenKonstante.ZI),
    GAUSSSCHE_PRIMZAHLEN(MengenKonstante.PZI),
    GRADMENGE(MengenKonstante.GRAD),
    REELLE_MATRIZEN(MengenKonstante.R),
    KOMPLEXE_MATRIZEN(MengenKonstante.C);

    val istMatrizenmenge: Boolean get() = this == REELLE_MATRIZEN || this == KOMPLEXE_MATRIZEN

    val stabileId: String get() = if (istMatrizenmenge) "${operator.stabileId}.matrizen" else operator.stabileId
    val titel: String get() = if (istMatrizenmenge) "Matrizenmenge ${if (this == REELLE_MATRIZEN) "ℝ" else "ℂ"}ⁿˣᵐ" else operator.titel
    val beschreibung: String get() = if (istMatrizenmenge) "${operator.symbolLatex}^{n\\times m}, n,m \\in \\mathbb{N}_{>0}" else operator.definitionLatex
    fun menge(): MengenAusdruck = operator.auswerten()
    companion object {
        fun vonIdOderNull(id: String?): MengenKnotenAuswahl? = entries.firstOrNull { it.stabileId == id }
        fun vonId(id: String?): MengenKnotenAuswahl = vonIdOderNull(id) ?: REELLE_ZAHLEN
    }
}

private fun mengenAusgang() = AnschlussDaten(
    name = "menge",
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = MathematikAnschlussArten.Menge.id,
)

object MengenKnotenVorlagen {
    fun vorlage(auswahl: MengenKnotenAuswahl): KnotenVorlage = KnotenVorlage(
        art = MENGEN_KNOTEN_ART,
        name = if (auswahl.istMatrizenmenge) auswahl.titel else "Menge",
        kategorie = "Mengen",
        beschreibung = "Ein einheitlicher Mengenknoten; die konkrete Menge oder eine versionsfeste eigene Karte wird im Inspector gewählt.",
        standardGröße = GraphGröße(230f, 105f),
        anschlüsse = (if (auswahl.istMatrizenmenge) listOf(zahlEingang("n", 0), zahlEingang("m", 1))
            else listOf(zahlEingang("dimension", 0))) + mengenAusgang(),
        standardParameter = mapOf(MENGEN_KNOTEN_AUSWAHL to auswahl.stabileId),
    )

    val definitionen: List<KnotenVorlage> = MengenKnotenAuswahl.entries.map(::vorlage)
    val standard: KnotenVorlage = vorlage(MengenKnotenAuswahl.REELLE_ZAHLEN)
}

private fun zahlEingang(name: String, reihenfolge: Int) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.Zahl.id,
    reihenfolge = reihenfolge,
)

private fun vektorAusgang(name: String, art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = art,
)

object EinheitsvektorKnotenVorlagen {
    private fun vorlage(zeile: Boolean) = KnotenVorlage(
        art = if (zeile) "mathematik.einheitsZeile" else "mathematik.einheitsSpalte",
        name = if (zeile) "Einheitsvektor (Zeile)" else "Einheitsvektor (Spalte)",
        kategorie = "Vektoren",
        beschreibung = "Erzeugt einen Standardbasisvektor aus nullbasierter Position und positiver Dimension.",
        standardGröße = GraphGröße(255f, 125f),
        anschlüsse = listOf(
            zahlEingang(EINHEITSVEKTOR_POSITION, 0),
            zahlEingang(EINHEITSVEKTOR_DIMENSION, 1),
            vektorAusgang(
                "vektor",
                if (zeile) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id,
            ),
        ),
        standardParameter = mapOf(
            "$STANDARDWERT_PREFIX$EINHEITSVEKTOR_POSITION" to "0",
            "$STANDARDWERT_PREFIX$EINHEITSVEKTOR_DIMENSION" to "3",
        ),
    )

    val Spalte = vorlage(false)
    val Zeile = vorlage(true)
    val alle = listOf(Spalte, Zeile)
}

private fun vektorRechnerEingang(name: String, reihenfolge: Int) = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.Objekt.id,
    zulässigeArten = setOf(
        MathematikAnschlussArten.SpaltenVektor.id,
        MathematikAnschlussArten.ZeilenVektor.id,
        MathematikAnschlussArten.Tupel.id,
    ),
    reihenfolge = reihenfolge,
)

object VektorRechnerKnotenVorlagen {
    val standard = KnotenVorlage(
        art = VektorRechner.KNOTEN_ART,
        name = "Vektorrechner",
        kategorie = "Lineare Algebra",
        beschreibung = "Einheitlicher Vektorrechner; der Operator wird im Inspector gewählt.",
        standardGröße = GraphGröße(270f, 125f),
        anschlüsse = listOf(
            vektorRechnerEingang("links", 0),
            vektorRechnerEingang("rechts", 1),
            AnschlussDaten(
                name = VEKTOR_RECHNER_AUSGANG,
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Zahl.id,
            ),
        ),
        standardParameter = mapOf(
            VEKTOR_RECHNER_OPERATOR to VektorRechnerOperator.SKALARPRODUKT.stabileId,
        ),
    )
}

val historischeMengenKnotenArten: Set<String> = mapOf(
    "mathematik.leereMenge" to MengenKnotenAuswahl.LEERE_MENGE,
    "mathematik.natürlicheZahlen" to MengenKnotenAuswahl.NATUERLICHE_ZAHLEN,
    "mathematik.ganzeZahlen" to MengenKnotenAuswahl.GANZE_ZAHLEN,
    "mathematik.rationaleZahlen" to MengenKnotenAuswahl.RATIONALE_ZAHLEN,
    "mathematik.reelleZahlen" to MengenKnotenAuswahl.REELLE_ZAHLEN,
    "mathematik.komplexeZahlen" to MengenKnotenAuswahl.KOMPLEXE_ZAHLEN,
    "mathematik.primzahlen" to MengenKnotenAuswahl.PRIMZAHLEN,
    "mathematik.gaussZahlen" to MengenKnotenAuswahl.GAUSSSCHE_GANZE,
    "mathematik.gaussPrimzahlen" to MengenKnotenAuswahl.GAUSSSCHE_PRIMZAHLEN,
).keys

val historischeSkalarproduktArten: Set<String> = setOf(
    "mathematik.skalarprodukt",
    "mathematik.skalarproduktZeile",
)

internal fun MathematikAuswerterRegister.registriereKonsolidierteKnoten() {
    registriere(MENGEN_KNOTEN_ART) { kontext ->
        val id = kontext.knoten.parameter[MENGEN_KNOTEN_AUSWAHL]
        val auswahl = if (id == null) MengenKnotenAuswahl.REELLE_ZAHLEN else MengenKnotenAuswahl.vonIdOderNull(id)
        if (auswahl == null) return@registriere KnotenAuswertungsErgebnis(ausgaben = emptyMap(), fehler = "Unbekannter Mengenoperator: $id")
        if (auswahl.istMatrizenmenge) return@registriere matrizenMengenErgebnis(kontext, auswahl.menge())
        require(mengenDimensionsart(kontext.knoten) in setOf(MENGEN_DIMENSION_TUPEL, MENGEN_DIMENSION_TENSOR)) { "Unbekannte Dimensionsart." }
        if (auswahl != MengenKnotenAuswahl.GRADMENGE && "dimension" in kontext.eingänge)
            return@registriere mengenDimensionsErgebnis(kontext, auswahl.menge(), mengenDimensionsart(kontext.knoten) == MENGEN_DIMENSION_TENSOR)

        val menge = if (auswahl == MengenKnotenAuswahl.GRADMENGE && ("dimension" in kontext.eingänge || "dim" in kontext.eingänge)) {
            val dimension = kontext.natuerlicheGanzzahl(if ("dim" in kontext.eingänge) "dim" else "dimension", nullErlaubt = false)
            require(dimension <= 65) { "Die Dimension darf höchstens 65 sein." }
            de.TeutonStudio.MathematikRechenSystem.kern.winkelraum(dimension - 1)
        } else auswahl.menge()
        KnotenAuswertungsErgebnis(mapOf("menge" to kontext.rechnerErgebnisWert(menge)), eingänge = kontext.eingänge)
    }
    registriere("mathematik.einheitsSpalte") { kontext ->
        val orientierung=kontext.knoten.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: VEKTOR_ORIENTIERUNG_SPALTE
        require(orientierung in setOf(VEKTOR_ORIENTIERUNG_SPALTE,VEKTOR_ORIENTIERUNG_ZEILE)) { "Ungültige Basisvektororientierung." }
        einheitsvektorErgebnis(kontext,orientierung==VEKTOR_ORIENTIERUNG_ZEILE)
    }
}

private fun einheitsvektorErgebnis(
    kontext: KnotenAuswertungsKontext,
    zeile: Boolean,
): KnotenAuswertungsErgebnis {
    val position = kontext.natuerlicheGanzzahl(EINHEITSVEKTOR_POSITION, nullErlaubt = true)
    val dimension = kontext.natuerlicheGanzzahl(EINHEITSVEKTOR_DIMENSION, nullErlaubt = false)
    require(dimension > 0) { "Die Dimension muss positiv sein." }
    require(position < dimension) { "Die Dimension muss größer als die Position sein." }
    val werte = einheitsVektorWerte(dimension, position + 1)
    val vektor: MathematischesObjekt = if (zeile) ZeilenVektor(werte) else SpaltenVektor(werte)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf("vektor" to BedingterWert(vektor, kontext.eingänge.values.flatMap { it.annahmen }.toSet())),
        eingänge = kontext.eingänge,
    )
}

private fun KnotenAuswertungsKontext.natuerlicheGanzzahl(name: String, nullErlaubt: Boolean): Int {
    val zahl = eingänge[name]?.objekt as? RationaleZahl ?: error("Der Eingang '$name' muss eine natürliche Zahl sein.")
    require(zahl.nenner == BigInteger.ONE) { "Der Eingang '$name' muss ganzzahlig sein." }
    val wert = zahl.zähler.intValueExact()
    require(wert >= if (nullErlaubt) 0 else 1) {
        if (nullErlaubt) "Die Position darf nicht negativ sein." else "Die Dimension muss positiv sein."
    }
    return wert
}



fun KartenDaten.migriereKonsolidierteKnoten(): KartenDaten = migriereHistorischeKnotenRollen { alt ->
    migriereMengenKnoten(alt) ?: migriereEinheitsvektor(alt) ?: migriereSkalarprodukt(alt) ?: alt
}

private val alteMengenAuswahl = mapOf(
    "mathematik.leereMenge" to MengenKnotenAuswahl.LEERE_MENGE,
    "mathematik.natürlicheZahlen" to MengenKnotenAuswahl.NATUERLICHE_ZAHLEN,
    "mathematik.ganzeZahlen" to MengenKnotenAuswahl.GANZE_ZAHLEN,
    "mathematik.rationaleZahlen" to MengenKnotenAuswahl.RATIONALE_ZAHLEN,
    "mathematik.reelleZahlen" to MengenKnotenAuswahl.REELLE_ZAHLEN,
    "mathematik.komplexeZahlen" to MengenKnotenAuswahl.KOMPLEXE_ZAHLEN,
    "mathematik.primzahlen" to MengenKnotenAuswahl.PRIMZAHLEN,
    "mathematik.gaussZahlen" to MengenKnotenAuswahl.GAUSSSCHE_GANZE,
    "mathematik.gaussPrimzahlen" to MengenKnotenAuswahl.GAUSSSCHE_PRIMZAHLEN,
)

private fun migriereMengenKnoten(alt: KnotenDaten): KnotenDaten? {
    val auswahl = alteMengenAuswahl[alt.art] ?: return null
    prüfeHistorischeAnschlussForm(alt)
    require(alt.anschlüsse.size==1 && alt.anschlüsse.single().name=="menge") {"Historische Mengenquellen benötigen genau ihren Mengenausgang."}
    prüfeHistorischenPortTyp(alt.anschlüsse.single(),setOf(MathematikAnschlussArten.Menge.id))
    val ausgang = alt.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
        ?: mengenAusgang().copy(id = AnschlussId("${alt.id.wert}-menge"))
    return alt.copy(art=MENGEN_KNOTEN_ART,anschlüsse=listOf(ausgang.copy(name="menge")),
        parameter=alt.parameter+(MENGEN_KNOTEN_AUSWAHL to auswahl.stabileId))
}

private fun migriereEinheitsvektor(alt: KnotenDaten): KnotenDaten? {
    val historischeZeile = when (alt.art) {
        "mathematik.einheitsZeile" -> true
        "mathematik.einheitsSpalte" -> false
        else -> return null
    }
    val orientierung=alt.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: if(historischeZeile) VEKTOR_ORIENTIERUNG_ZEILE else VEKTOR_ORIENTIERUNG_SPALTE
    require(orientierung in setOf(VEKTOR_ORIENTIERUNG_SPALTE,VEKTOR_ORIENTIERUNG_ZEILE)) { "Unbekannte Basisvektororientierung." }
    val zeile=orientierung==VEKTOR_ORIENTIERUNG_ZEILE
    if (alt.anschlüsse.any { it.name == EINHEITSVEKTOR_POSITION } && alt.anschlüsse.any { it.name == EINHEITSVEKTOR_DIMENSION }) {
        return alt
    }
    val ausgang = alt.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
        ?: vektorAusgang(
            "vektor",
            if (zeile) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id,
        ).copy(id = AnschlussId("${alt.id.wert}-vektor"))
    prüfeHistorischeAnschlussForm(alt)
    require(alt.anschlüsse.size==1 && alt.anschlüsse.single().name=="vektor") {"Die historische Einheitsvektorform ist nicht eindeutig."}
    prüfeHistorischenPortTyp(alt.anschlüsse.single(),setOf(if(zeile) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id))
    val position = if("index" in alt.parameter) alt.parameter["index"]!!.toIntOrNull() ?: error("Ungültiger Basisindex.") else 0
    val dimension = if("dimension" in alt.parameter) alt.parameter["dimension"]!!.toIntOrNull() ?: error("Ungültige Basisdimension.") else 3
    require(position>=0 && dimension>0 && position<dimension) {"Basisindex und Dimension sind ungültig."}
    return alt.copy(
        anschlüsse = listOf(
            zahlEingang(EINHEITSVEKTOR_POSITION, 0).copy(id = AnschlussId("${alt.id.wert}-position")),
            zahlEingang(EINHEITSVEKTOR_DIMENSION, 1).copy(id = AnschlussId("${alt.id.wert}-dimension")),
            ausgang.copy(
                name = "vektor",
                art = if (zeile) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id,
            ),
        ),
        parameter = (alt.parameter - "index" - "dimension") + mapOf(
            "$STANDARDWERT_PREFIX$EINHEITSVEKTOR_POSITION" to position.toString(),
            "$STANDARDWERT_PREFIX$EINHEITSVEKTOR_DIMENSION" to dimension.toString(),
        ),
    )
}

private fun migriereSkalarprodukt(alt: KnotenDaten): KnotenDaten? {
    if (alt.art !in historischeSkalarproduktArten) return null
    prüfeHistorischeAnschlussForm(alt)
    val eingänge=alt.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}
    val rollen=if(eingänge.map {it.name}.toSet()==setOf("links","rechts")) mapOf("links" to "links","rechts" to "rechts") else mapOf("a" to "links","b" to "rechts")
    require(eingänge.map {it.name}.toSet()==rollen.keys && eingänge.none {it.belegung==AnschlussBelegung.Mehrfach}) {"Die Skalarproduktrollen sind nicht eindeutig."}
    val ausgang=alt.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}
    eingänge.forEach {prüfeHistorischenPortTyp(it,
        if(rollen.keys==setOf("links","rechts")) setOf(MathematikAnschlussArten.Objekt.id,MathematikAnschlussArten.Vektor.id,MathematikAnschlussArten.Tupel.id)
        else setOf(MathematikAnschlussArten.Vektor.id))}
    prüfeHistorischenPortTyp(ausgang,setOf(MathematikAnschlussArten.Zahl.id))
    require(ausgang.name=="wert") {"Unbekannte Skalarproduktausgangsrolle."}
    return alt.copy(art=VektorRechner.KNOTEN_ART,
        anschlüsse=eingänge.map {p -> p.copy(name=rollen.getValue(p.name))}+ausgang.copy(name=VEKTOR_RECHNER_AUSGANG),
        parameter=alt.parameter+(VEKTOR_RECHNER_OPERATOR to VektorRechnerOperator.SKALARPRODUKT.stabileId))
}

/** Ergänzt optionale Dimensionseingänge und erhält vorhandene Anschlussreferenzen. */
fun KnotenDaten.mitGradmengenDimension(): KnotenDaten {
    if (RECHNER_MIGRATION_DIAGNOSE in parameter || art != MENGEN_KNOTEN_ART || kartenVerweis != null) return this
    val auswahl = MengenKnotenAuswahl.entries.firstOrNull { it.stabileId == parameter[MENGEN_KNOTEN_AUSWAHL] } ?: return this
    if (auswahl.istMatrizenmenge) return this
    val name = "dimension"
    if (anschlüsse.any { it.richtung == AnschlussRichtung.Eingang && it.name in setOf("dimension", "dim") }) return this
    return copy(anschlüsse = listOf(zahlEingang(name, 0).copy(id = AnschlussId("${id.wert}-mengen-$name"))) + anschlüsse)
}

fun KartenDaten.migriereGradmengenDimension(): KartenDaten {
    val verbunden = verbindungen.mapTo(hashSetOf()) { it.zu.anschlussId }
    return copy(knoten = knoten.map { alt ->
        val k = alt.mitGradmengenDimension()
        if (k.art != MENGEN_KNOTEN_ART || k.kartenVerweis != null || MengenKnotenAuswahl.vonIdOderNull(k.parameter[MENGEN_KNOTEN_AUSWAHL])?.istMatrizenmenge == true) k else {
            val dimensionen = k.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && it.name in setOf("dim", "dimension") }
            val belegte = dimensionen.filter { it.id in verbunden }
            if (belegte.size > 1) k // Beide Verbindungen bleiben bis zur expliziten Verlustbestätigung erhalten.
            else {
                val aktiv = belegte.singleOrNull() ?: dimensionen.firstOrNull { it.name == if (k.parameter[MENGEN_KNOTEN_AUSWAHL] == "Grad") "dim" else "dimension" } ?: dimensionen.firstOrNull()
                k.copy(anschlüsse = k.anschlüsse.mapNotNull { a ->
                    if (a == aktiv) a.copy(name = "dimension") else if (a in dimensionen) null else a
                })
            }
        }
    })
}

/** Packt ausschließlich nachgewiesene, endlich indizierte SIGNED-Richtungsprodukte. */
internal fun kanonischerRichtungsraum(menge: MengenAusdruck): MengenAusdruck {
    // Das Produkt über {0} wird vom Kern bereits als zwei Auswahlfunktionen materialisiert.
    if (menge is de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge && menge.elemente.size == 2) {
        val methoden = menge.elemente.map { it as? de.TeutonStudio.MathematikRechenSystem.kern.Methode ?: return menge }
        val nullIndex = de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge(setOf(RationaleZahl.Null))
        if (methoden.all { m -> m.parameter.size == 1 && m.werteVorräte[m.parameter.single().name] == nullIndex } &&
            methoden.map { it.wendeAn(listOf(RationaleZahl.Null)) }.toSet() == setOf(RationaleZahl.von(-1), RationaleZahl.Eins)) {
            return de.TeutonStudio.MathematikRechenSystem.kern.winkelraum(0)
        }
        return menge
    }
    if (menge !is de.TeutonStudio.MathematikRechenSystem.kern.IteriertesKartesischesProdukt) return menge
    val index = menge.indexMenge as? de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge ?: return menge
    if (index.elemente.size !in 1..65) return menge
    val zahlen = index.elemente.map { it as? RationaleZahl ?: return menge }
    if (zahlen.toSet() != List(zahlen.size) { RationaleZahl.von(it.toLong()) }.toSet()) return menge
    val faktoren = List(zahlen.size) { i ->
        menge.methode.wendeAn(listOf(RationaleZahl.von(i.toLong()))) as? MengenAusdruck ?: return menge
    }
    val erwartet = de.TeutonStudio.MathematikRechenSystem.kern.winkelraum(faktoren.size - 1)
    return if (faktoren == erwartet.komponenten) erwartet else menge
}

/** Explizite Konfiguration; verbundene Duplikate werden durch die Kartenaktionsvorschau geschützt. */
fun KnotenDaten.mitEinheitlicherMengenDimension(): KnotenDaten {
    val kandidaten = anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && it.name in setOf("dim", "dimension") }
    val aktiv = kandidaten.firstOrNull { it.name == if (parameter[MENGEN_KNOTEN_AUSWAHL] == MengenKnotenAuswahl.GRADMENGE.stabileId) "dim" else "dimension" } ?: kandidaten.firstOrNull()
    return if (aktiv == null) mitGradmengenDimension() else copy(anschlüsse = anschlüsse.mapNotNull {
        if (it == aktiv) it.copy(name = "dimension") else if (it in kandidaten) null else it
    })
}

/** Varianten erhalten Anschluss-IDs nach Rolle; der Aufrufer bestätigt betroffene Verbindungen. */
fun konfiguriereMengenAuswahl(knoten: KnotenDaten, auswahl: MengenKnotenAuswahl): KnotenDaten {
    require(knoten.art == MENGEN_KNOTEN_ART)
    val vorlage = MengenKnotenVorlagen.vorlage(auswahl)
    val anschlüsse = vorlage.anschlüsse.map { neu ->
        (knoten.anschlüsse.firstOrNull { it.name == neu.name && it.richtung == neu.richtung }
            ?: knoten.anschlüsse.singleOrNull { neu.richtung == AnschlussRichtung.Ausgang && it.richtung == AnschlussRichtung.Ausgang })
            ?.copy(name = neu.name, art = neu.art, zulässigeArten = neu.zulässigeArten) ?: neu.copy(id = AnschlussId("${knoten.id.wert}:menge:${neu.name}"))
    }
    val neu = knoten.copy(kartenVerweis = null, parameter = (knoten.parameter - "raum.matrixHistorisch") + (MENGEN_KNOTEN_AUSWAHL to auswahl.stabileId), anschlüsse = anschlüsse)
    return if (hatMengenDimensionsart(neu)) konfiguriereMengenDimensionsart(neu, mengenDimensionsart(neu).takeIf { it in setOf(MENGEN_DIMENSION_TUPEL, MENGEN_DIMENSION_TENSOR) } ?: MENGEN_DIMENSION_TUPEL) else neu.copy(parameter = neu.parameter - MENGEN_DIMENSIONSART_PARAMETER)
}


const val MENGEN_DIMENSIONSART_PARAMETER = "dimensionsart"
const val MENGEN_DIMENSION_TUPEL = "tupel"
const val MENGEN_DIMENSION_TENSOR = "tensor"
fun sichtbareMengenAuswahlen(): List<MengenKnotenAuswahl> = MengenKnotenAuswahl.entries.filterNot { it.istMatrizenmenge }
fun mengenDimensionsart(knoten: KnotenDaten): String = knoten.parameter[MENGEN_DIMENSIONSART_PARAMETER] ?: MENGEN_DIMENSION_TUPEL
fun hatMengenDimensionsart(knoten: KnotenDaten): Boolean = knoten.art == MENGEN_KNOTEN_ART && knoten.kartenVerweis == null &&
    MengenKnotenAuswahl.vonIdOderNull(knoten.parameter[MENGEN_KNOTEN_AUSWAHL])?.let { !it.istMatrizenmenge && it != MengenKnotenAuswahl.GRADMENGE } == true

fun konfiguriereMengenDimensionsart(knoten: KnotenDaten, art: String): KnotenDaten {
    require(hatMengenDimensionsart(knoten)) { "Diese Mengenquelle besitzt keine umschaltbare Dimensionsart." }
    require(art in setOf(MENGEN_DIMENSION_TUPEL, MENGEN_DIMENSION_TENSOR)) { "Unbekannte Dimensionsart: $art" }
    val wechsel = mengenDimensionsart(knoten) != art
    return knoten.copy(
        parameter = (if (wechsel) knoten.parameter.filterKeys { it != "raum.matrixHistorisch" && it != "standardwert.dimension" && !it.startsWith("standardwert.dimension.") } else knoten.parameter) + (MENGEN_DIMENSIONSART_PARAMETER to art),
        eingangsKartenVerweise = if (wechsel) knoten.eingangsKartenVerweise.filterKeys { it != "dimension" && !it.startsWith("dimension@") } else knoten.eingangsKartenVerweise,
        anschlüsse = knoten.anschlüsse.map { port ->
            if (port.richtung == AnschlussRichtung.Eingang && port.name == "dimension") port.copy(
                art = if (art == MENGEN_DIMENSION_TENSOR) MathematikAnschlussArten.Tupel.id else MathematikAnschlussArten.Zahl.id,
                zulässigeArten = emptySet(),
            ) else port
        },
    )
}

package de.TeutonStudio.MathematikKnoten

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
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
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
import java.math.BigInteger

const val MENGEN_KNOTEN_ART = "mathematik.menge"
const val MENGEN_KNOTEN_AUSWAHL = "mengeAuswahl"
const val VEKTOR_RECHNER_OPERATOR = "operator"
const val EINHEITSVEKTOR_POSITION = "position"
const val EINHEITSVEKTOR_DIMENSION = "dimension"
private const val STANDARDWERT_PREFIX = "standardwert."

enum class MengenKnotenAuswahl(
    val stabileId: String,
    val titel: String,
    val beschreibung: String,
    private val erzeuger: () -> MengenAusdruck,
) {
    LEERE_MENGE("leer", "Leere Menge", "Die Menge ohne Elemente.", { LeereMenge }),
    NATUERLICHE_ZAHLEN("N", "Natürliche Zahlen", "Die positiven natürlichen Zahlen.", { NatürlicheZahlen }),
    NATUERLICHE_MIT_NULL("N0", "Natürliche Zahlen mit Null", "Die nichtnegativen ganzen Zahlen.", { BenannteMenge("N0", "\\mathbb N_0") }),
    GANZE_ZAHLEN("Z", "Ganze Zahlen", "Die ganzen Zahlen.", { GanzeZahlen }),
    RATIONALE_ZAHLEN("Q", "Rationale Zahlen", "Die rationalen Zahlen.", { RationaleZahlen }),
    REELLE_ZAHLEN("R", "Reelle Zahlen", "Die reellen Zahlen.", { ReelleZahlen }),
    KOMPLEXE_ZAHLEN("C", "Komplexe Zahlen", "Die komplexen Zahlen.", { KomplexeZahlen }),
    PRIMZAHLEN("P", "Primzahlen", "Die positiven Primzahlen.", { Primzahlen }),
    GAUSSSCHE_GANZE("Zi", "Gaußsche ganze Zahlen", "Der Ring der Zahlen a+bi mit a,b in Z.", { GaußscheGanzeZahlen }),
    GAUSSSCHE_PRIMZAHLEN("PZi", "Gaußsche Primzahlen", "Die Primelemente der gaußschen ganzen Zahlen.", { GaußschePrimzahlen }),
    ;

    fun menge(): MengenAusdruck = erzeuger()

    companion object {
        fun vonId(id: String?): MengenKnotenAuswahl = entries.firstOrNull { it.stabileId == id } ?: REELLE_ZAHLEN
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
        name = "Menge",
        kategorie = "Mengen",
        beschreibung = "Ein einheitlicher Mengenknoten; die konkrete Menge oder eine versionsfeste eigene Karte wird im Inspector gewählt.",
        standardGröße = GraphGröße(230f, 105f),
        anschlüsse = listOf(mengenAusgang()),
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
                name = "wert",
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
        val auswahl = MengenKnotenAuswahl.vonId(kontext.knoten.parameter[MENGEN_KNOTEN_AUSWAHL])
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("menge" to BedingterWert(auswahl.menge())),
        )
    }
    EinheitsvektorKnotenVorlagen.alle.forEach { vorlage ->
        registriere(vorlage.art) { kontext -> einheitsvektorErgebnis(kontext, vorlage.art.endsWith("Zeile")) }
    }
    registriere(VektorRechner.KNOTEN_ART) { kontext ->
        val operator = VektorRechnerOperator.entries.firstOrNull {
            it.stabileId == kontext.knoten.parameter[VEKTOR_RECHNER_OPERATOR]
        } ?: VektorRechnerOperator.SKALARPRODUKT
        val quellen = listOf("links", "rechts").mapNotNull { name ->
            kontext.eingänge[name]?.objekt?.let(::vektorQuelle)
        }
        val ergebnis = VektorRechner.erzeuge(VektorRechnerAnfrage(operator = operator, vektoren = quellen))
        val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()
        when (ergebnis) {
            is VektorRechnerErgebnis.ZahlWert -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf("wert" to BedingterWert(ergebnis.wert, annahmen + ergebnis.bedingungen)),
                eingänge = kontext.eingänge,
            )
            is VektorRechnerErgebnis.VektorWert -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf("wert" to BedingterWert(ergebnis.wert, annahmen + ergebnis.bedingungen)),
                eingänge = kontext.eingänge,
            )
            is VektorRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
        }
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
    val werte = List(dimension) { index -> if (index == position) RationaleZahl.Eins else RationaleZahl.Null }
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

private fun vektorQuelle(objekt: MathematischesObjekt): VektorQuelle {
    val werte: List<ZahlAusdruck>
    val quelle: (KartesischerKoordinatenVertrag) -> VektorQuelle
    when (objekt) {
        is OrientierterVektor -> {
            werte = objekt.werte
            quelle = { vertrag -> VektorQuelle.Vektor(objekt, vertrag) }
        }
        is Tupel -> {
            werte = objekt.elemente.mapIndexed { index, element ->
                element as? ZahlAusdruck ?: error("Tupelkomponente ${index + 1} ist keine Zahl.")
            }
            quelle = { vertrag -> VektorQuelle.Koordinaten(objekt, vertrag) }
        }
        else -> error("Der Vektorrechner erwartet einen Vektor oder ein kartesisches Zahlentupel.")
    }
    require(werte.isNotEmpty()) { "Ein leerer Vektor ist für diese Operation nicht zulässig." }
    val bereich = when {
        werte.any { it is KomplexeZahl } -> FundamentalerZahlbereich.KOMPLEX
        werte.all { it is RationaleZahl } -> FundamentalerZahlbereich.RATIONAL
        else -> FundamentalerZahlbereich.REELL
    }
    return quelle(
        KartesischerKoordinatenVertrag(
            dimension = werte.size,
            zahlbereich = bereich,
            basisId = "standard",
            koordinatensystemId = "kartesisch",
            standardBasis = true,
        ),
    )
}

fun KartenDaten.migriereKonsolidierteKnoten(): KartenDaten = copy(
    knoten = knoten.map { alt ->
        migriereMengenKnoten(alt)
            ?: migriereEinheitsvektor(alt)
            ?: migriereSkalarprodukt(alt)
            ?: alt
    },
)

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
    val ausgang = alt.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
        ?: mengenAusgang().copy(id = AnschlussId("${alt.id.wert}-menge"))
    return alt.copy(
        art = MENGEN_KNOTEN_ART,
        anschlüsse = listOf(
            ausgang.copy(
                name = "menge",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Menge.id,
                kannSichErweitern = false,
                dynamischErzeugt = false,
            ),
        ),
        parameter = alt.parameter + (MENGEN_KNOTEN_AUSWAHL to auswahl.stabileId),
        kartenVerweis = null,
    )
}

private fun migriereEinheitsvektor(alt: KnotenDaten): KnotenDaten? {
    val zeile = when (alt.art) {
        "mathematik.einheitsZeile" -> true
        "mathematik.einheitsSpalte" -> false
        else -> return null
    }
    if (alt.anschlüsse.any { it.name == EINHEITSVEKTOR_POSITION } && alt.anschlüsse.any { it.name == EINHEITSVEKTOR_DIMENSION }) {
        return alt
    }
    val ausgang = alt.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
        ?: vektorAusgang(
            "vektor",
            if (zeile) MathematikAnschlussArten.ZeilenVektor.id else MathematikAnschlussArten.SpaltenVektor.id,
        ).copy(id = AnschlussId("${alt.id.wert}-vektor"))
    val position = alt.parameter["index"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val dimension = alt.parameter["dimension"]?.toIntOrNull()?.coerceAtLeast(1) ?: 3
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
    val eingänge = alt.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
    val ausgang = alt.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
        ?: VektorRechnerKnotenVorlagen.standard.anschlüsse.last().copy(id = AnschlussId("${alt.id.wert}-wert"))
    return alt.copy(
        art = VektorRechner.KNOTEN_ART,
        name = if (alt.name.startsWith("Skalarprodukt")) "Vektorrechner" else alt.name,
        anschlüsse = listOf(
            (eingänge.getOrNull(0) ?: vektorRechnerEingang("links", 0).copy(id = AnschlussId("${alt.id.wert}-links"))).copy(
                name = "links",
                art = MathematikAnschlussArten.Objekt.id,
                zulässigeArten = vektorRechnerEingang("links", 0).zulässigeArten,
                reihenfolge = 0,
            ),
            (eingänge.getOrNull(1) ?: vektorRechnerEingang("rechts", 1).copy(id = AnschlussId("${alt.id.wert}-rechts"))).copy(
                name = "rechts",
                art = MathematikAnschlussArten.Objekt.id,
                zulässigeArten = vektorRechnerEingang("rechts", 1).zulässigeArten,
                reihenfolge = 1,
            ),
            ausgang.copy(name = "wert", art = MathematikAnschlussArten.Zahl.id),
        ),
        parameter = alt.parameter + (VEKTOR_RECHNER_OPERATOR to VektorRechnerOperator.SKALARPRODUKT.stabileId),
    )
}

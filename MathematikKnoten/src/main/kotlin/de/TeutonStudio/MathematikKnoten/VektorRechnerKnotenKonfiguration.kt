package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val VEKTOR_RECHNER_METRIK = "metrik"
const val VEKTOR_RECHNER_ACHSE = "achse"
const val VEKTOR_RECHNER_STRUKTUR_AUSGABE = "strukturAusgabe"
const val VEKTOR_RECHNER_ERGEBNIS = "ergebnis"

private val vektorZulaessigeArten = setOf(
    MathematikAnschlussArten.SpaltenVektor.id,
    MathematikAnschlussArten.ZeilenVektor.id,
    MathematikAnschlussArten.Tupel.id,
)

private val strukturZulaessigeArten = vektorZulaessigeArten + setOf(
    MathematikAnschlussArten.Zahl.id,
    MathematikAnschlussArten.Objekt.id,
)

fun vektorRechnerAnschluesse(operator: VektorRechnerOperator): List<AnschlussDaten> {
    fun eingang(
        name: String,
        art: AnschlussArtId = MathematikAnschlussArten.Objekt.id,
        zulaessig: Set<AnschlussArtId> = emptySet(),
        reihe: Int,
        erweiterbar: Boolean = false,
    ) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        zulässigeArten = zulaessig,
        reihenfolge = reihe,
        kannSichErweitern = erweiterbar,
    )

    fun vektor(name: String, reihe: Int, erweiterbar: Boolean = false) =
        eingang(name, MathematikAnschlussArten.Objekt.id, vektorZulaessigeArten, reihe, erweiterbar)

    fun ausgang(art: AnschlussArtId, zulaessig: Set<AnschlussArtId> = emptySet()) = AnschlussDaten(
        name = VEKTOR_RECHNER_ERGEBNIS,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
        zulässigeArten = zulaessig,
    )

    return when (operator) {
        VektorRechnerOperator.ADDITION -> listOf(
            vektor("vektor.1", 0),
            vektor("vektor.2", 1, erweiterbar = true),
            ausgang(MathematikAnschlussArten.Vektor.id),
        )
        VektorRechnerOperator.SUBTRAKTION,
        VektorRechnerOperator.SKALARPRODUKT,
        VektorRechnerOperator.KREUZPRODUKT,
        VektorRechnerOperator.HADAMARD_PRODUKT,
        VektorRechnerOperator.PROJEKTION,
        VektorRechnerOperator.WINKEL,
        VektorRechnerOperator.DISTANZ,
        -> listOf(
            vektor("links", 0),
            vektor("rechts", 1),
            ausgang(
                if (operator in setOf(
                        VektorRechnerOperator.SKALARPRODUKT,
                        VektorRechnerOperator.WINKEL,
                        VektorRechnerOperator.DISTANZ,
                    )
                ) MathematikAnschlussArten.Zahl.id else MathematikAnschlussArten.Vektor.id,
            ),
        )
        VektorRechnerOperator.SKALARMULTIPLIKATION -> listOf(
            vektor("vektor", 0),
            eingang("skalar", MathematikAnschlussArten.Zahl.id, reihe = 1),
            ausgang(MathematikAnschlussArten.Vektor.id),
        )
        VektorRechnerOperator.NEGATION,
        VektorRechnerOperator.NORMALISIERUNG,
        -> listOf(vektor("vektor", 0), ausgang(MathematikAnschlussArten.Vektor.id))
        VektorRechnerOperator.NORM,
        VektorRechnerOperator.WINKEL_ZU_ACHSE,
        -> listOf(vektor("vektor", 0), ausgang(MathematikAnschlussArten.Zahl.id))
        VektorRechnerOperator.VEKTORFELD_INTEGRIEREN -> listOf(
            eingang("methode", MathematikAnschlussArten.Methode.id, reihe = 0),
            eingang("menge", MathematikAnschlussArten.Menge.id, reihe = 1),
            eingang("mass", MathematikAnschlussArten.Mass.id, reihe = 2),
            ausgang(MathematikAnschlussArten.Objekt.id, setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.Vektor.id)),
        )
        VektorRechnerOperator.ZERLEGEN -> listOf(
            eingang("struktur", MathematikAnschlussArten.Objekt.id, strukturZulaessigeArten, 0),
            ausgang(MathematikAnschlussArten.Tupel.id),
        )
        VektorRechnerOperator.ZUSAMMENFUEHREN -> listOf(
            eingang("element.1", MathematikAnschlussArten.Objekt.id, strukturZulaessigeArten, 0),
            eingang("element.2", MathematikAnschlussArten.Objekt.id, strukturZulaessigeArten, 1, erweiterbar = true),
            ausgang(MathematikAnschlussArten.Objekt.id, setOf(
                MathematikAnschlussArten.Tupel.id,
                MathematikAnschlussArten.SpaltenVektor.id,
                MathematikAnschlussArten.ZeilenVektor.id,
            )),
        )
    }
}

fun konfiguriereVektorRechner(
    knoten: KnotenDaten,
    operator: VektorRechnerOperator,
): KnotenDaten {
    require(knoten.art == VektorRechner.KNOTEN_ART)
    val neu = erhalteVektorRechnerAnschlussIds(knoten.anschlüsse, vektorRechnerAnschluesse(operator))
    return knoten.copy(
        name = if (knoten.name == "Vektorrechner" || VektorRechnerOperator.entries.any { it.titel == knoten.name }) "Vektorrechner" else knoten.name,
        anschlüsse = neu,
        parameter = knoten.parameter + mapOf(
            VEKTOR_RECHNER_OPERATOR to operator.stabileId,
            VEKTOR_RECHNER_METRIK to (knoten.parameter[VEKTOR_RECHNER_METRIK] ?: VektorMetrik.EUKLIDISCH.stabileId),
            VEKTOR_RECHNER_ACHSE to (knoten.parameter[VEKTOR_RECHNER_ACHSE] ?: "1"),
            VEKTOR_RECHNER_STRUKTUR_AUSGABE to (knoten.parameter[VEKTOR_RECHNER_STRUKTUR_AUSGABE] ?: VektorStrukturAusgabe.TUPEL.stabileId),
        ),
    )
}

fun vektorRechnerVorlage(operator: VektorRechnerOperator): KnotenVorlage = KnotenVorlage(
    art = VektorRechner.KNOTEN_ART,
    name = "Vektorrechner · ${operator.titel}",
    kategorie = "Lineare Algebra: Vektorrechner",
    beschreibung = "Vorkonfigurierte Variante des kanonischen Vektorrechners für ${operator.titel}.",
    standardGröße = GraphGröße(285f, 145f),
    anschlüsse = vektorRechnerAnschluesse(operator),
    standardParameter = mapOf(
        VEKTOR_RECHNER_OPERATOR to operator.stabileId,
        VEKTOR_RECHNER_METRIK to VektorMetrik.EUKLIDISCH.stabileId,
        VEKTOR_RECHNER_ACHSE to "1",
        VEKTOR_RECHNER_STRUKTUR_AUSGABE to VektorStrukturAusgabe.TUPEL.stabileId,
    ),
)

internal fun MathematikAuswerterRegister.registriereVektorRechnerErweiterungen() {
    registriere(VektorRechner.KNOTEN_ART) { kontext ->
        val operator = VektorRechnerOperator.vonIdOderNull(kontext.knoten.parameter[VEKTOR_RECHNER_OPERATOR])
            ?: VektorRechnerOperator.SKALARPRODUKT
        val vektorNamen = when (operator) {
            VektorRechnerOperator.ADDITION -> kontext.eingänge.keys.filter { it.startsWith("vektor.") }.sorted()
            VektorRechnerOperator.SUBTRAKTION,
            VektorRechnerOperator.SKALARPRODUKT,
            VektorRechnerOperator.KREUZPRODUKT,
            VektorRechnerOperator.HADAMARD_PRODUKT,
            VektorRechnerOperator.PROJEKTION,
            VektorRechnerOperator.WINKEL,
            VektorRechnerOperator.DISTANZ,
            -> listOf("links", "rechts")
            VektorRechnerOperator.SKALARMULTIPLIKATION,
            VektorRechnerOperator.NEGATION,
            VektorRechnerOperator.NORM,
            VektorRechnerOperator.NORMALISIERUNG,
            VektorRechnerOperator.WINKEL_ZU_ACHSE,
            -> listOf("vektor")
            else -> emptyList()
        }
        val quellen = vektorNamen.mapNotNull { name -> kontext.eingänge[name]?.objekt?.let(::vektorQuelleErweitert) }
        val skalare = listOfNotNull(kontext.eingänge["skalar"]?.objekt as? ZahlAusdruck)
        val objekte = when (operator) {
            VektorRechnerOperator.ZERLEGEN -> listOfNotNull(kontext.eingänge["struktur"]?.objekt)
            VektorRechnerOperator.ZUSAMMENFUEHREN -> kontext.eingänge
                .filterKeys { it.startsWith("element.") }
                .toList()
                .sortedBy { (name, _) -> name.substringAfterLast('.').toIntOrNull() ?: Int.MAX_VALUE }
                .map { it.second.objekt }
            else -> emptyList()
        }
        val ergebnis = VektorRechner.erzeuge(
            VektorRechnerAnfrage(
                operator = operator,
                vektoren = quellen,
                skalare = skalare,
                objekte = objekte,
                methode = kontext.eingänge["methode"]?.objekt as? Methode,
                menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck,
                mass = kontext.eingänge["mass"]?.objekt as? IntegralMass,
                metrik = VektorMetrik.vonIdOderStandard(kontext.knoten.parameter[VEKTOR_RECHNER_METRIK]),
                achse = kontext.knoten.parameter[VEKTOR_RECHNER_ACHSE]?.toIntOrNull() ?: 1,
                strukturAusgabe = VektorStrukturAusgabe.vonIdOderStandard(
                    kontext.knoten.parameter[VEKTOR_RECHNER_STRUKTUR_AUSGABE],
                ),
            ),
        )
        val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()
        val ausgangName = kontext.knoten.anschlüsse.singleOrNull { it.richtung == AnschlussRichtung.Ausgang }?.name
            ?: VEKTOR_RECHNER_ERGEBNIS
        when (ergebnis) {
            is VektorRechnerErgebnis.ZahlWert -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf(ausgangName to BedingterWert(ergebnis.wert, annahmen + ergebnis.bedingungen)),
                eingänge = kontext.eingänge,
            )
            is VektorRechnerErgebnis.VektorWert -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf(ausgangName to BedingterWert(ergebnis.wert, annahmen + ergebnis.bedingungen)),
                eingänge = kontext.eingänge,
            )
            is VektorRechnerErgebnis.ObjektWert -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf(ausgangName to BedingterWert(ergebnis.wert, annahmen + ergebnis.bedingungen)),
                eingänge = kontext.eingänge,
            )
            is VektorRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
        }
    }
}

fun KartenDaten.migriereVektorRechnerKonfiguration(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        if (knoten.art != VektorRechner.KNOTEN_ART) return@map knoten
        val operator = VektorRechnerOperator.vonIdOderNull(knoten.parameter[VEKTOR_RECHNER_OPERATOR])
            ?: VektorRechnerOperator.SKALARPRODUKT
        konfiguriereVektorRechner(knoten, operator)
    },
)

private fun erhalteVektorRechnerAnschlussIds(
    bisher: List<AnschlussDaten>,
    gewuenscht: List<AnschlussDaten>,
): List<AnschlussDaten> {
    val verbraucht = mutableSetOf<AnschlussId>()
    return gewuenscht.map { soll ->
        val kandidat = bisher.firstOrNull {
            it.id !in verbraucht && it.richtung == soll.richtung && it.name == soll.name
        } ?: bisher.firstOrNull {
            it.id !in verbraucht && it.richtung == soll.richtung && it.reihenfolge == soll.reihenfolge
        }
        if (kandidat == null) soll else {
            verbraucht += kandidat.id
            soll.copy(id = kandidat.id)
        }
    }
}

private fun vektorQuelleErweitert(objekt: MathematischesObjekt): VektorQuelle {
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

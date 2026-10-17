package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val VEKTOR_POTENZ_EXPONENT = "vektor.exponent"
const val VEKTOR_RECHNER_METRIK = "metrik"
const val VEKTOR_RECHNER_ACHSE = "achse"
const val VEKTOR_RECHNER_STRUKTUR_AUSGABE = "strukturAusgabe"
const val VEKTOR_RECHNER_ERGEBNIS = "ergebnis"

private val vektorZulaessigeArten = setOf(
    MathematikAnschlussArten.SpaltenVektor.id,
    MathematikAnschlussArten.ZeilenVektor.id,
    MathematikAnschlussArten.Tupel.id,
)

private val zusammenfuehrenZulaessigeArten = vektorZulaessigeArten + setOf(
    MathematikAnschlussArten.Zahl.id,
    MathematikAnschlussArten.Objekt.id,
)

fun vektorRechnerAnschluesse(operator: VektorRechnerOperator): List<AnschlussDaten> =
    rechnerAnschlüsse(RechnerFamilie.VEKTOR, operator.stabileId)

fun konfiguriereVektorRechner(
    knoten: KnotenDaten,
    operator: VektorRechnerOperator,
): KnotenDaten {
    require(knoten.art == VektorRechner.KNOTEN_ART)
    val gewuenscht = vektorRechnerAnschluesse(operator).map { port ->
        if (operator == VektorRechnerOperator.KREUZPRODUKT && port.richtung == AnschlussRichtung.Ausgang) {
            val art = when (kreuzproduktRechnerOrientierung(knoten)) {
                VektorOrientierung.Spalte -> MathematikAnschlussArten.SpaltenVektor.id
                VektorOrientierung.Zeile -> MathematikAnschlussArten.ZeilenVektor.id
                null -> port.art
            }
            port.copy(art = art)
        } else port
    } + if (operator in setOf(VektorRechnerOperator.POTENZ, VektorRechnerOperator.SKALARPRODUKT)) knoten.anschlüsse.filter { it.name == SKALARPRODUKT_DEFINITION_EINGANG } else emptyList()
    val signatur = if (operator.stabileId == knoten.parameter[VEKTOR_RECHNER_OPERATOR] && operator in setOf(VektorRechnerOperator.ADDITION, VektorRechnerOperator.ZUSAMMENFUEHREN)) {
        val präfix = if (operator == VektorRechnerOperator.ADDITION) "vektor." else "element."
        gewuenscht + knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && it.name.startsWith(präfix) &&
            it.name.substringAfterLast('.').toIntOrNull()?.let { n -> n > 2 } == true }
    } else if (
        operator == VektorRechnerOperator.ZERLEGEN &&
        knoten.parameter[VEKTOR_RECHNER_OPERATOR] == VektorRechnerOperator.ZERLEGEN.stabileId
    ) {
        gewuenscht + knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang && it.name.startsWith("element") }
    } else gewuenscht
    val neu = erhalteRechnerAnschlussIds(knoten.anschlüsse, signatur, RechnerFamilie.VEKTOR, operator.stabileId).map { port ->
        if (operator == VektorRechnerOperator.KREUZPRODUKT && port.richtung == AnschlussRichtung.Ausgang)
            knoten.anschlüsse.singleOrNull { it.richtung == port.richtung && it.name == port.name }?.let { port.copy(id=it.id) } ?: port
        else port
    }
    return normalisiereRechnerMethodenAnschluesse(
        knoten.copy(
            name = if (knoten.name == "Vektorrechner" || VektorRechnerOperator.entries.any { it.titel == knoten.name }) {
                "Vektorrechner"
            } else knoten.name,
            anschlüsse = erhalteDynamischeOperanden(knoten, neu,
                knoten.parameter[VEKTOR_RECHNER_OPERATOR] == operator.stabileId),
            parameter = knoten.parameter + mapOf(
                VEKTOR_RECHNER_OPERATOR to operator.stabileId,
                VEKTOR_RECHNER_METRIK to (knoten.parameter[VEKTOR_RECHNER_METRIK] ?: VektorMetriken.standard.stabileId),
                VEKTOR_RECHNER_ACHSE to (knoten.parameter[VEKTOR_RECHNER_ACHSE] ?: "1"),
                VEKTOR_RECHNER_STRUKTUR_AUSGABE to (
                    knoten.parameter[VEKTOR_RECHNER_STRUKTUR_AUSGABE] ?: VektorStrukturAusgabe.TUPEL.stabileId
                ),
                INTEGRAL_MASS_MODUS_PARAMETER to (
                    knoten.parameter[INTEGRAL_MASS_MODUS_PARAMETER] ?: IntegralMassModus.AUTO.name
                ),
                INTEGRAL_MASS_SYMBOL_PARAMETER to (
                    knoten.parameter[INTEGRAL_MASS_SYMBOL_PARAMETER] ?: "\\mu"
                ),
            ),
        ),
    ) .let { normalisiert ->
        if(knoten.parameter[VEKTOR_RECHNER_OPERATOR]!=operator.stabileId) normalisiert else
            normalisiert.copy(anschlüsse=normalisiert.anschlüsse.map {p ->
                val alt=knoten.anschlüsse.singleOrNull {it.id==p.id} ?: return@map p
                p.copy(vertrag=alt.vertrag,typInferenz=alt.typInferenz,semantischeId=alt.semantischeId,
                    artFolgtEingang=alt.artFolgtEingang,artVereinigtEingänge=alt.artVereinigtEingänge,
                    artAbbildungVonEingang=alt.artAbbildungVonEingang,
                    artPriorisiertEingänge=alt.artPriorisiertEingänge ?: p.artPriorisiertEingänge)
            })
    }

}

fun vektorRechnerVorlage(operator: VektorRechnerOperator): KnotenVorlage = methodenfaehigeRechnerVorlage(
    KnotenVorlage(
        art = VektorRechner.KNOTEN_ART,
        name = "Vektorrechner · ${operator.titel}",
        kategorie = "Lineare Algebra: Vektorrechner",
        beschreibung = "Vorkonfigurierte Variante des kanonischen Vektorrechners für ${operator.titel}.",
        standardGröße = GraphGröße(285f, 145f),
        anschlüsse = vektorRechnerAnschluesse(operator),
        standardParameter = mapOf(
            VEKTOR_RECHNER_OPERATOR to operator.stabileId,
            VEKTOR_RECHNER_METRIK to VektorMetriken.standard.stabileId,
            VEKTOR_RECHNER_ACHSE to "1",
            VEKTOR_RECHNER_STRUKTUR_AUSGABE to VektorStrukturAusgabe.TUPEL.stabileId,
            INTEGRAL_MASS_MODUS_PARAMETER to IntegralMassModus.AUTO.name,
            INTEGRAL_MASS_SYMBOL_PARAMETER to "\\mu",
        ),
    ),
)

internal val vektorFachauswerter = de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter { kontext ->
        val gespeichert = kontext.knoten.parameter[VEKTOR_RECHNER_OPERATOR]
        val operator = VektorRechnerOperator.vonIdOderNull(gespeichert)
            ?: if (gespeichert == null) VektorRechnerOperator.SKALARPRODUKT else error("Unbekannter Vektoroperator: $gespeichert")
        if (operator == VektorRechnerOperator.DISTANZ) require(kontext.knoten.parameter[VEKTOR_RECHNER_METRIK]?.let { id -> VektorMetriken.alle.any { it.stabileId==id } } != false) { "Unbekannte gespeicherte Vektormetrik." }
        val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()

        if (operator == VektorRechnerOperator.ZERLEGEN) {
            val struktur = kontext.eingänge["struktur"]?.objekt
                ?: error("Zerlegen benötigt einen Vektor oder ein Tupel.")
            val elemente: List<MathematischesObjekt>? = when (struktur) {
                is Tupel -> struktur.elemente
                is OrientierterVektor -> struktur.werte
                is TypisiertesElement -> struktur.strukturForm?.firstOrNull()?.let { dimension ->
                    dimension.positiveDimensionOderNull()?.let { anzahl ->
                        List(anzahl) { index ->
                            strukturKomponente(struktur, RationaleZahl.von((index + 1).toLong()), numerisch = true)
                        }
                    }
                }
                else -> error("Zerlegen akzeptiert nur Tupel und Vektoren.")
            }
            val ausgänge = if (elemente == null) {
                mapOf("methode" to BedingterWert(vektorZerlegeMethode(struktur), annahmen))
            } else {
                kontext.knoten.anschlüsse
                    .filter { it.richtung == AnschlussRichtung.Ausgang }
                    .sortedBy { it.reihenfolge }
                    .filterNot { it.name == "methode" }
                    .mapIndexedNotNull { position, anschluss ->
                        val index = anschluss.name.removePrefix("element").toIntOrNull()?.minus(1) ?: position
                        elemente.getOrNull(index)?.let { element ->
                            anschluss.name to BedingterWert(element, annahmen)
                        }
                    }
                    .toMap()
            }
            return@MathematikKnotenAuswerter KnotenAuswertungsErgebnis(
                ausgaben = ausgänge,
                eingänge = kontext.eingänge,
            )
        }

        val vektorNamen = when (operator) {
            VektorRechnerOperator.POTENZ -> listOf("a")
            VektorRechnerOperator.ADDITION, VektorRechnerOperator.HADAMARD_PRODUKT -> geordneteOperationsWerte(kontext).keys.toList()
            VektorRechnerOperator.HADAMARD_DIVISION,
            VektorRechnerOperator.SUBTRAKTION,
            VektorRechnerOperator.SKALARPRODUKT,
            VektorRechnerOperator.KREUZPRODUKT,
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
        val vorräte = kontext.eingänge.values.rechnerQuellBereiche()
        val quellen = vektorNamen.mapNotNull { name ->
            kontext.eingänge[name]
                ?.mathematischesObjekt("Vektoroperand '$name'")
                ?.let { vektorQuelleErweitert(it,vorräte,annahmen) }
        }
        val skalare = listOfNotNull(kontext.eingänge[if (operator == VektorRechnerOperator.KARTESISCH) "radius" else "skalar"]?.mathematischesObjekt("Skalar") as? ZahlAusdruck)
        val objekte = when (operator) {
            VektorRechnerOperator.RADIUS, VektorRechnerOperator.POLARWINKEL -> listOfNotNull(kontext.eingänge["kartesisch"]?.mathematischesObjekt("Kartesisches Tupel"))
            VektorRechnerOperator.KARTESISCH -> listOfNotNull(kontext.eingänge["winkel"]?.mathematischesObjekt("Winkeltupel / Vorzeichen"))
            VektorRechnerOperator.ZUSAMMENFUEHREN -> kontext.knoten.anschlüsse
                .filter { it.richtung == AnschlussRichtung.Eingang && it.name.startsWith("element.") }
                .sortedBy { it.reihenfolge }
                .mapNotNull { port -> kontext.eingänge[port.name]?.mathematischesObjekt("Vektorelement '${port.name}'") }
            else -> emptyList()
        }
        val integrationsMenge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
        val ergebnis = VektorRechner.erzeuge(
            VektorRechnerAnfrage(
                operator = operator,
                exponent = if (operator == VektorRechnerOperator.POTENZ) (kontext.eingänge["p"]?.objekt as? ZahlAusdruck ?: kontext.knoten.parameter[VEKTOR_POTENZ_EXPONENT]?.toLongOrNull()?.let(RationaleZahl::von) ?: RationaleZahl.von(2)) else RationaleZahl.von(2),
                vektoren = quellen,
                variablenVorräte = vorräte,
                annahmen = annahmen,
                kreuzproduktOrientierung = if (operator == VektorRechnerOperator.KREUZPRODUKT) kreuzproduktRechnerOrientierung(kontext.knoten) else null,
                skalare = skalare,
                objekte = objekte,
                methode = kontext.eingänge["vektorfeld"]?.objekt as? Methode,
                menge = integrationsMenge,
                mass = if (operator == VektorRechnerOperator.VEKTORFELD_INTEGRIEREN && integrationsMenge != null) {
                    bestimmeVektorfeldIntegralMass(kontext.knoten, integrationsMenge)
                } else null,
                metrik = VektorMetriken.vonIdOderStandard(kontext.knoten.parameter[VEKTOR_RECHNER_METRIK]),
                achse = kontext.knoten.parameter[VEKTOR_RECHNER_ACHSE]?.toIntOrNull() ?: 1,
                strukturAusgabe = VektorStrukturAusgabe.vonIdOderStandard(
                    kontext.knoten.parameter[VEKTOR_RECHNER_STRUKTUR_AUSGABE],
                ),
            ),
        )
        val ausgangName = kontext.knoten.anschlüsse.singleOrNull {
            it.richtung == AnschlussRichtung.Ausgang
        }?.name ?: VEKTOR_RECHNER_ERGEBNIS
        when (ergebnis) {
            is VektorRechnerErgebnis.ZahlWert -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf(
                    ausgangName to kontext.rechnerErgebnisWert(ergebnis.wert, ergebnis.bedingungen.toSet(),ergebnis.bereich.alsMenge()),
                ),
                eingänge = kontext.eingänge,
            )
            is VektorRechnerErgebnis.VektorWert -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf(
                    ausgangName to kontext.rechnerErgebnisWert(ergebnis.wert, ergebnis.bedingungen.toSet()),
                ),
                eingänge = kontext.eingänge,
            )
            is VektorRechnerErgebnis.Ungueltig -> KnotenAuswertungsErgebnis(
                ausgaben = emptyMap(),
                fehler = ergebnis.nachricht,
                warnungen = listOf("Code: ${ergebnis.code}"),
                eingänge = kontext.eingänge,
            )
        }
}

internal fun MathematikAuswerterRegister.registriereVektorRechnerErweiterungen() { registriere(VektorRechner.KNOTEN_ART, vektorFachauswerter) }


fun vektorZerlegeMethode(struktur: MathematischesObjekt): MathematischeMethode {
    val index = Variable("i")
    val laenge = strukturAchsenLaenge(struktur, 0)
    return MathematischeMethode(
        name = "zerlegen",
        parameter = listOf(index),
        vorschrift = strukturKomponente(struktur, index, numerisch = true),
        zielMenge = KomplexeZahlen,
        werteVorräte = mapOf(index.name to EndlicheIndexMenge(laenge)),
    )
}

private fun ZahlAusdruck.positiveDimensionOderNull(): Int? = (this as? RationaleZahl)?.let { zahl ->
    if (
        zahl.nenner == java.math.BigInteger.ONE &&
        zahl.zähler.signum() > 0 &&
        zahl.zähler.bitLength() < 31
    ) zahl.zähler.toInt() else null
}

fun KartenDaten.migriereVektorRechnerKonfiguration(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        if (knoten.art != VektorRechner.KNOTEN_ART) return@map knoten
        val gespeichert = knoten.parameter[VEKTOR_RECHNER_OPERATOR]
        val operator = VektorRechnerOperator.vonIdOderNull(gespeichert)
        if (operator == null && gespeichert != null) knoten
        else konfiguriereVektorRechner(knoten, operator ?: VektorRechnerOperator.SKALARPRODUKT)
    },
)

private fun bestimmeVektorfeldIntegralMass(
    knoten: KnotenDaten,
    menge: MengenAusdruck,
): IntegralMass? {
    val bereich = IntegralBereich(vektorIntegralKomponenten(menge))
    val modus = IntegralMassModus.entries.firstOrNull {
        it.name == knoten.parameter[INTEGRAL_MASS_MODUS_PARAMETER]
    } ?: IntegralMassModus.AUTO
    return when (modus) {
        IntegralMassModus.AUTO -> leiteIntegralMassOderNull(bereich)
        IntegralMassModus.STANDARD_REELL -> IntegralMass.StandardReell
        IntegralMassModus.ZAEHLMASS -> IntegralMass.Zaehlmass
        IntegralMassModus.ALLGEMEIN -> IntegralMass.Allgemein(
            knoten.parameter[INTEGRAL_MASS_SYMBOL_PARAMETER].orEmpty().ifBlank { "\\mu" },
        )
        IntegralMassModus.NICHTSTANDARD -> IntegralMass.NichtstandardZellgewicht()
    }
}

private fun vektorIntegralKomponenten(menge: MengenAusdruck): List<MengenAusdruck> = when (menge) {
    is KartesischesProdukt -> menge.mengen
    is Tupelraum -> menge.komponenten
    else -> listOf(menge)
}

internal fun vektorQuelleErweitert(objekt: MathematischesObjekt, vorräte: Map<String,MengenAusdruck> = emptyMap(), annahmen: Set<Aussage> = emptySet()): VektorQuelle {
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
    val bereich = maximaleZahlenGrundmenge(werte.map { inferiereZahlenWertevorrat(it,vorräte,annahmen) })
        .fundamentalerZahlbereichOderNull() ?: error("Der Koeffizientenbereich ist nicht nachgewiesen.")
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

/** Fehlende Konfiguration behält die bestehende freie Orientierung des Rechners. */
internal fun kreuzproduktRechnerOrientierung(k: KnotenDaten): VektorOrientierung? =
    when (val wert=k.parameter[VEKTOR_ORIENTIERUNG_PARAMETER]) {
        null -> null
        VEKTOR_ORIENTIERUNG_SPALTE -> VektorOrientierung.Spalte
        VEKTOR_ORIENTIERUNG_ZEILE -> VektorOrientierung.Zeile
        else -> error("Unbekannte gespeicherte Kreuzproduktorientierung '$wert'.")
    }

fun konfiguriereKreuzproduktRechnerOrientierung(k: KnotenDaten, orientierung: String?): KnotenDaten {
    require(k.art==VektorRechner.KNOTEN_ART && k.parameter[VEKTOR_RECHNER_OPERATOR]==VektorRechnerOperator.KREUZPRODUKT.stabileId)
    val neu=k.copy(parameter=k.parameter-VEKTOR_ORIENTIERUNG_PARAMETER + listOfNotNull(orientierung?.let { VEKTOR_ORIENTIERUNG_PARAMETER to it }).toMap())
    kreuzproduktRechnerOrientierung(neu)
    return konfiguriereVektorRechner(neu,VektorRechnerOperator.KREUZPRODUKT)
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.AnschlussVertrag
import de.TeutonStudio.TypSystem.TypAusdruck

const val METHODEN_AUFRUF_STELLIGKEIT = "methodenAufruf.stelligkeit"
const val METHODEN_AUFRUF_ZIELMENGE = "methodenAufruf.zielMenge"
const val METHODEN_AUFRUF_VERTRAGSFEHLER = "methodenAufruf.vertragsfehler"
const val METHODEN_AUFRUF_PARAMETER_PREFIX = "methodenAufruf.parameter."
const val METHODEN_AUSGANG_ARGUMENTPROJEKTION_PREFIX = "methodenAusgang."

fun methodenAusgangArgumentprojektionSchlüssel(anschlussName: String): String =
    "$METHODEN_AUSGANG_ARGUMENTPROJEKTION_PREFIX$anschlussName.argumentprojektion"

fun methodenAusgangErgebnisprojektionSchlüssel(anschlussName: String): String =
    "$METHODEN_AUSGANG_ARGUMENTPROJEKTION_PREFIX$anschlussName.ergebnisprojektion"

fun KnotenDaten.methodenAusgangArgumentprojektion(anschlussName: String): String =
    parameter[methodenAusgangArgumentprojektionSchlüssel(anschlussName)]
        ?.takeIf { it == METHODEN_ARGUMENTPROJEKTION_TUPEL }
        ?: METHODEN_ARGUMENTPROJEKTION_SEPARIERT

fun KnotenDaten.methodenAusgangErgebnisprojektion(anschlussName: String): String =
    parameter[methodenAusgangErgebnisprojektionSchlüssel(anschlussName)]
        ?.takeIf { it == METHODEN_ERGEBNISPROJEKTION_TUPEL }
        ?: METHODEN_ERGEBNISPROJEKTION_DIREKT

/**
 * Synchronisiert UI-Projektionen ausschließlich aus der neutralen Methodensignatur.
 * Mathematische Mengen werden nur als optionale Zusatzmetadaten angehängt und bestimmen
 * weder Anzahl noch Typ der allgemeinen Handles.
 */
fun synchronisiereMethodenAufrufe(
    karte: KartenDaten,
    auswertung: KartenAuswertungsErgebnis,
    prüfung: GraphPrüfung,
): KartenDaten {

    val synchronisierteKnoten = karte.knoten.map { knoten ->
        if (RECHNER_MIGRATION_DIAGNOSE in knoten.parameter) return@map knoten
        when (knoten.art) {
            METHODEN_AUFRUF_ART -> {
                val methode = auswertung.knoten[knoten.id]
                    ?.eingänge
                    ?.get("methode")
                    ?.objekt as? Methode
                val fest = knoten.parameter[METHODEN_AUFRUF_PROJEKTION_FEST] == "true"
                val argumentProjektion = if (fest) knoten.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION] ?: METHODEN_ARGUMENTPROJEKTION_SEPARIERT else methodenArgumentprojektionFürVerbraucher(karte, knoten)
                val ergebnisProjektion = if (fest) knoten.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION] ?: METHODEN_ERGEBNISPROJEKTION_DIREKT else methodenErgebnisprojektionFürVerbraucher(karte, knoten)
                val belegt = knoten.anschlüsse.filter { p ->
                    karte.verbindungen.any { it.von == AnschlussVerweis(knoten.id,p.id) || it.zu == AnschlussVerweis(knoten.id,p.id) } ||
                        "standardwert.${p.name}" in knoten.parameter || p.name in knoten.eingangsKartenVerweise
                }.map { it.id }.toSet()
                synchronisiereMethodenAufruf(knoten, methode, argumentProjektion, ergebnisProjektion, belegt,
                    auswertung.knoten[knoten.id]?.eingänge.orEmpty())
            }
            METHODEN_ARGUMENTE_ART -> {
                val methode = auswertung.knoten[knoten.id]
                    ?.eingänge
                    ?.get("methode")
                    ?.objekt as? Methode
                val projektion = methodenArgumentprojektionFürVerbraucher(karte, knoten)
                synchronisiereMethodenArgumente(knoten, methode, projektion)
            }
            METHODEN_ZIELMENGE_ART -> {
                val projektion = methodenErgebnisprojektionFürVerbraucher(karte, knoten)
                knoten.copy(
                    parameter = knoten.parameter +
                        (METHODEN_ZIELMENGE_ERGEBNISPROJEKTION to projektion),
                )
            }
            else -> knoten
        }.let { synchronisiereSemantischeAusgänge(it, auswertung, karte) }
    }

    val argumentIdErsetzungen=buildMap {
        for(alt in karte.knoten.filter {it.art==METHODEN_AUFRUF_ART}) {
            if(METHODEN_STRUKTUR_PACKUNG in alt.eigenschaften) continue
            val neu=synchronisierteKnoten.single {it.id==alt.id}
            alt.argumentAnschlüsse().forEachIndexed {i,p ->
                val id=p.semantischeId ?: alt.parameter["$METHODEN_AUFRUF_PARAMETER_PREFIX$i.id"]
                val q=if(id!=null) neu.argumentAnschlüsse().singleOrNull {it.semantischeId==id} else neu.argumentAnschlüsse().getOrNull(i)
                if(q!=null && p.id!=q.id) put(AnschlussVerweis(alt.id,p.id),AnschlussVerweis(neu.id,q.id))
            }
        }
    }

    var ergebnis = karte.copy(
        knoten = synchronisierteKnoten,
        verbindungen = karte.verbindungen.map { verbindung ->
            verbindung.copy(
                von = argumentIdErsetzungen[verbindung.von] ?: verbindung.von,
                zu = argumentIdErsetzungen[verbindung.zu] ?: verbindung.zu,
            )
        },
    )
    val vorhandeneAnschlüsse = ergebnis.knoten.flatMap { knoten ->
        knoten.anschlüsse.map { AnschlussVerweis(knoten.id, it.id) }
    }.toSet()
    ergebnis = ergebnis.copy(verbindungen = ergebnis.verbindungen.filter {
        it.von in vorhandeneAnschlüsse && it.zu in vorhandeneAnschlüsse
    })

    // Eine einzelne unvereinbare Kante darf keine unabhängigen Kanten mitreißen.
    val geschützt = ergebnis.knoten.filter { RECHNER_MIGRATION_DIAGNOSE in it.parameter || METHODEN_AUFRUF_VERTRAGSFEHLER in it.parameter }.map { it.id }.toSet()
    val inkompatibleIds = prüfung.inkompatibleVerbindungen(ergebnis)
        .filter { it.von.knotenId !in geschützt && it.zu.knotenId !in geschützt }.map { it.id }.toSet()
    val gültigeVerbindungen = ergebnis.verbindungen.filterNot { it.id in inkompatibleIds }

    return synchronisiereStrukturZerleger(
        ergebnis.copy(verbindungen = gültigeVerbindungen),
        auswertung,
        prüfung,
    )
}

/** Signaturen und Variablentypen sind abgeleitete Laufzeitverträge, keine UI-Schattenwerte. */
private fun synchronisiereSemantischeAusgänge(knoten: KnotenDaten, auswertung: KartenAuswertungsErgebnis, karte: KartenDaten): KnotenDaten {
    if (RECHNER_MIGRATION_DIAGNOSE in knoten.parameter || METHODEN_AUFRUF_VERTRAGSFEHLER in knoten.parameter ||
        knoten.art == METHODEN_AUFRUF_ART && knoten.parameter[METHODEN_AUFRUF_PROJEKTION_FEST] == "true") return knoten
    val ergebnis = auswertung.knoten[knoten.id]
    val grundraum = (ergebnis?.eingänge?.get("grundraum")?.objekt as? MengenAusdruck)?.let(::kanonischerRichtungsraum)
    val ports = knoten.anschlüsse.map { p ->
        val methode = ergebnis?.ausgaben?.get(p.name)?.objekt as? Methode
        when {
            p.richtung == AnschlussRichtung.Ausgang && methode != null ->
                p.copy(vertrag = p.vertrag.copy(typ = methode.methodenTypAusdruck()))
            p.richtung == AnschlussRichtung.Ausgang && knoten.art == METHODEN_AUFRUF_ART &&
                ergebnis?.eingänge?.get("methode")?.objekt is AllgemeinAuswertbareMethode -> {
                val ergebnisWert = ergebnis.ausgaben[p.name]
                val wert = ergebnisWert?.objekt
                val argumentPort = knoten.anschlüsse.singleOrNull { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }
                val alterArgumentPort = argumentPort?.let { argument -> karte.knoten.singleOrNull { it.id == knoten.id }?.anschlüsse?.singleOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == argument.name } }
                val quellRef = alterArgumentPort?.let { argument -> karte.verbindungen.singleOrNull { it.zu == AnschlussVerweis(knoten.id, argument.id) }?.von }
                val quellTyp = quellRef?.let { ref -> karte.knoten.singleOrNull { it.id == ref.knotenId }?.anschlüsse?.singleOrNull { it.id == ref.anschlussId }?.vertrag?.typ }?.takeUnless { it == TypAusdruck.Unbekannt }
                val argumentTyp = if (knoten.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION] == METHODEN_ARGUMENTPROJEKTION_TUPEL) {
                    (quellTyp as? TypAusdruck.Parameterisiert)?.takeIf { it.konstruktor == MathematischeTypen.Tupel }?.argumente?.singleOrNull()
                } else quellTyp
                val durchgereichterTyp = if ((ergebnis.eingänge["methode"]?.objekt == IdentitaetsMethode || ergebnis.eingänge["methode"]?.objekt is TypisierteIdentitaetsMethode) && argumentTyp != null) {
                    if (knoten.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION] == METHODEN_ERGEBNISPROJEKTION_TUPEL) TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, listOf(argumentTyp)) else argumentTyp
                } else null
                val typ = ergebnisWert?.zielMenge?.elementTypAusdruck() ?: ergebnisWert?.werteVorrat?.elementTypAusdruck() ?: durchgereichterTyp ?: when (wert) {
                    is Methode -> wert.methodenTypAusdruck()
                    is MathematischesObjekt -> runCatching { inferiereZielmenge(wert, ergebnisWert.reelleVariablen, ergebnisWert.annahmen).elementTypAusdruck() }.getOrNull()
                    else -> null
                }
                if (typ == null) p else p.copy(art = anschlussArtFürTyp(typ), vertrag = AnschlussVertrag(typ))
            }
            p.richtung == AnschlussRichtung.Ausgang && knoten.art == "mathematik.variable" -> {
                // Fehlende Eingaben sind eine Auswertungsdiagnose, kein Grund, bestehende Edges zu löschen.
                if (grundraum == null) p else {
                    val typ = grundraum.elementTypAusdruck()
                    p.copy(art = variablenAnschlussArt(typ), vertrag = p.vertrag.copy(typ = typ))
                }
            }
            p.richtung == AnschlussRichtung.Ausgang && ergebnis?.ausgaben?.get(p.name) != null &&
                (p.vertrag.typ as? TypAusdruck.Parameterisiert)?.konstruktor == MathematischeTypen.Methode ->
                p.copy(vertrag = p.vertrag.copy(typ = TypAusdruck.Unbekannt))
            else -> p
        }
    }
    val methode = ergebnis?.ausgaben?.values?.mapNotNull { it.objekt as? Methode }?.singleOrNull()
    val standardnamen = setOf("Term zu Methode", "Aussage zu Methode", "Aussage zu Prädikat")
    val name = if (knoten.art == "mathematik.termZuMethode" && knoten.name in standardnamen && methode != null) {
        if (methode.istPrädikat()) "Aussage zu Prädikat"
        else if (knoten.anschlüsse.any { it.name == "term" && it.richtung == AnschlussRichtung.Eingang && it.art == MathematikAnschlussArten.Aussage.id }) "Aussage zu Methode"
        else "Term zu Methode"
    } else knoten.name
    return knoten.copy(name = name, anschlüsse = ports)
}

private data class MethodenQuellAusgang(
    val knoten: KnotenDaten,
    val anschluss: AnschlussDaten,
)

private fun methodenQuellAusgang(karte: KartenDaten, verbraucher: KnotenDaten): MethodenQuellAusgang? {
    val methodenEingang = verbraucher.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    } ?: return null
    val eingangsVerweis = AnschlussVerweis(verbraucher.id, methodenEingang.id)
    val verbindung = karte.verbindungen.singleOrNull { it.zu == eingangsVerweis } ?: return null
    val quelle = karte.knoten.firstOrNull { it.id == verbindung.von.knotenId } ?: return null
    val ausgang = quelle.anschlüsse.firstOrNull { it.id == verbindung.von.anschlussId } ?: return null
    if (ausgang.richtung != AnschlussRichtung.Ausgang || ausgang.art != MathematikAnschlussArten.Methode.id) {
        return null
    }
    return MethodenQuellAusgang(quelle, ausgang)
}

private fun methodenArgumentprojektionFürVerbraucher(karte: KartenDaten, verbraucher: KnotenDaten): String {
    val quelle = methodenQuellAusgang(karte, verbraucher) ?: return METHODEN_ARGUMENTPROJEKTION_SEPARIERT
    return quelle.knoten.methodenAusgangArgumentprojektion(quelle.anschluss.name)
}

private fun methodenErgebnisprojektionFürVerbraucher(karte: KartenDaten, verbraucher: KnotenDaten): String {
    val quelle = methodenQuellAusgang(karte, verbraucher) ?: return METHODEN_ERGEBNISPROJEKTION_DIREKT
    return quelle.knoten.methodenAusgangErgebnisprojektion(quelle.anschluss.name)
}

private fun synchronisiereMethodenAufruf(
    knoten: KnotenDaten,
    methode: Methode?,
    argumentProjektion: String,
    ergebnisProjektion: String,
    belegt: Set<AnschlussId>,
    eingangswerte: Map<String, BedingterWert>,
): KnotenDaten {
    if (METHODEN_STRUKTUR_PACKUNG in knoten.eigenschaften || METHODEN_STRUKTUR_MIGRATIONSFEHLER in knoten.parameter) return knoten
    val methodenEingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    } ?: return knoten
    val ausgang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "wert"
    } ?: return knoten
    val bisherigeArgumente = knoten.argumentAnschlüsse()

    // Eine vorübergehend fehlende Signatur darf weder Anschlüsse noch Quellen löschen.
    if (methode == null) return knoten

    val signatur = (methode as? SignaturtragendeMethode)?.signatur ?: return knoten.copy(
        parameter = knoten.parameter + (
            METHODEN_AUFRUF_VERTRAGSFEHLER to "Die Methode '${methode.name}' besitzt keine neutrale Methodensignatur."
        ),
    )
    fun abgelehnt(grund: String) = knoten.copy(parameter = knoten.parameter + (METHODEN_AUFRUF_VERTRAGSFEHLER to grund))
    val ids = signatur.argumente.map { it.id }
    val vorhandeneIds = bisherigeArgumente.mapNotNull { it.semantischeId }
    if (vorhandeneIds.distinct().size != vorhandeneIds.size || vorhandeneIds.any { it !in ids } ||
        bisherigeArgumente.map { it.name }.distinct().size != bisherigeArgumente.size ||
        knoten.anschlüsse.map { it.id }.distinct().size != knoten.anschlüsse.size) return abgelehnt("Die gespeicherten Argumentidentitäten passen nicht eindeutig zur Methodensignatur.")
    if (knoten.parameter[METHODEN_AUFRUF_PROJEKTION_FEST] == "true" && ergebnisProjektion == METHODEN_ERGEBNISPROJEKTION_DIREKT && signatur.ergebnisse.size != 1)
        return abgelehnt("Die gespeicherte direkte Projektion benötigt genau eine Ergebniskomponente.")
    val mathematischeSignatur = (methode as? MathematischeSignaturtragendeMethode)?.mathematischeSignatur

    val effektiveErgebnisProjektion = when {
        ergebnisProjektion == METHODEN_ERGEBNISPROJEKTION_TUPEL -> METHODEN_ERGEBNISPROJEKTION_TUPEL
        signatur.ergebnisse.size == 1 -> METHODEN_ERGEBNISPROJEKTION_DIREKT
        else -> METHODEN_ERGEBNISPROJEKTION_TUPEL
    }
    val ergebnisTyp = if (effektiveErgebnisProjektion == METHODEN_ERGEBNISPROJEKTION_TUPEL) {
        signatur.ergebnisTyp
    } else {
        signatur.ergebnisse.single().typ
    }
    if (knoten.parameter[METHODEN_AUFRUF_PROJEKTION_FEST] == "true") {
        knoten.parameter[METHODEN_ANWENDUNG_ERGEBNIS_ART]?.let { art ->
            MethodenAnforderung.ErgebnisArt(art).prüfe(methode)?.let { return abgelehnt(it) }
        }
        if (ausgang.vertrag.typ != TypAusdruck.Unbekannt &&
            MathematischeTypen.typSystem.prüfe(ergebnisTyp, ausgang.vertrag.typ) != de.TeutonStudio.TypSystem.TypPrüfung.Kompatibel)
            return abgelehnt("Der gespeicherte Einzelresultattyp passt nicht zur Methodensignatur.")
    }
    val ergebnisArt = if(knoten.parameter[METHODEN_AUFRUF_PARTIELL]=="true") MathematikAnschlussArten.Methode.id else anschlussArtFürTyp(ergebnisTyp)

    val verwendeteNamen = bisherigeArgumente.map { it.name }.toMutableSet()
    fun neuerName(index: Int): String {
        val basis = "argument-$index"
        var name = basis
        var suffix = 1
        while (!verwendeteNamen.add(name)) name = "$basis-${suffix++}"
        return name
    }
    val argumente = if (argumentProjektion == METHODEN_ARGUMENTPROJEKTION_TUPEL) {
        if (signatur.argumente.isEmpty()) emptyList()
        else listOf(
            tupelAnschluss(knoten.id, bisherigeArgumente.firstOrNull()).copy(
                vertrag = AnschlussVertrag(signatur.argumentTyp),
            ),
        )
    } else {
        signatur.argumente.mapIndexed { index, komponent ->
            val bisher = bisherigeArgumente.singleOrNull {it.semantischeId==komponent.id} ?: bisherigeArgumente.getOrNull(index)?.takeIf {it.semantischeId==null}
            (bisher ?: AnschlussDaten(
                id = argumentId(knoten.id, index),
                name = "argument-$index",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = anschlussArtFürTyp(komponent.typ),
            )).copy(
                id = bisher?.id ?: AnschlussId("${knoten.id.wert}:methodenAufruf:komponente:${komponent.id}"),
                semantischeId = komponent.id,
                name = bisher?.name ?: neuerName(index),
                art = if (knoten.parameter[METHODEN_AUFRUF_PROJEKTION_FEST] == "true" && bisher != null) bisher.art else anschlussArtFürTyp(komponent.typ),
                reihenfolge = index + 1,
                kannSichErweitern = false,
                dynamischErzeugt = false,
                vertrag = if (knoten.parameter[METHODEN_AUFRUF_PROJEKTION_FEST] == "true" && bisher != null) bisher.vertrag else AnschlussVertrag(komponent.typ),
            )
        }
    }

    val entfernte = bisherigeArgumente.filter { alt -> argumente.none { it.id == alt.id } }
    if (entfernte.any { it.id in belegt }) return abgelehnt("Die Signatur-/Projektionsänderung würde belegte Argumentanschlüsse entfernen; die Originaldaten bleiben erhalten.")
    if (knoten.parameter[METHODEN_AUFRUF_PROJEKTION_FEST] == "true" && bisherigeArgumente.any { alt ->
        if (alt.id !in belegt) false else argumente.singleOrNull { it.id == alt.id }?.let { neu ->
            val wert = eingangswerte[alt.name]
            val objekt = wert?.objekt
            val tatsächlicherTyp = when (objekt) {
                is Methode -> objekt.methodenTypAusdruck()
                is MathematischesObjekt -> runCatching {
                    inferiereZielmenge(objekt, wert.reelleVariablen, wert.annahmen).elementTypAusdruck()
                }.getOrNull()
                else -> null
            } ?: wert?.zielMenge?.elementTypAusdruck() ?: wert?.werteVorrat?.elementTypAusdruck()
            val altTyp = tatsächlicherTyp ?: alt.vertrag.typ.takeUnless { it == TypAusdruck.Unbekannt }
                ?: alt.art.takeUnless { it == MathematikAnschlussArten.Objekt.id }
                    ?.let { TypAusdruck.Atom(de.TeutonStudio.TypSystem.TypId(it.wert)) }
            val zielTyp = if (argumentProjektion == METHODEN_ARGUMENTPROJEKTION_TUPEL) signatur.argumentTyp
                else signatur.argumente.single { it.id == neu.semantischeId }.typ
            altTyp != null && MathematischeTypen.typSystem.prüfe(altTyp, zielTyp) is de.TeutonStudio.TypSystem.TypPrüfung.Inkompatibel
        } ?: false
    }) return abgelehnt("Die Methodensignatur ist mit einem belegten Argumentanschluss unvereinbar; die Originaldaten bleiben erhalten.")
    val vertragsParameter = buildMap {
        put(METHODEN_AUFRUF_STELLIGKEIT, signatur.argumente.size.toString())
        put(METHODEN_ANWENDUNG_ERGEBNIS_ART, if (knoten.parameter[METHODEN_AUFRUF_PROJEKTION_FEST] == "true") knoten.parameter[METHODEN_ANWENDUNG_ERGEBNIS_ART] ?: ausgang.art.wert else ergebnisArt.wert)
        put(METHODEN_AUFRUF_ARGUMENTPROJEKTION, argumentProjektion)
        put(METHODEN_AUFRUF_ERGEBNISPROJEKTION, effektiveErgebnisProjektion)
        put("festeEingänge", argumente.size.toString())
        mathematischeSignatur?.let { mathematisch ->
            val ziel = if (effektiveErgebnisProjektion == METHODEN_ERGEBNISPROJEKTION_TUPEL) {
                mathematisch.zielRaum
            } else {
                mathematisch.ergebnisse.single().zielMenge
            }
            put(METHODEN_AUFRUF_ZIELMENGE, ziel.zuLatex())
        }
        signatur.argumente.forEachIndexed { index, komponent ->
            val präfix = "$METHODEN_AUFRUF_PARAMETER_PREFIX$index."
            put("${präfix}id", komponent.id)
            put("${präfix}name", komponent.name)
            put("${präfix}art", anschlussArtFürTyp(komponent.typ).wert)
            mathematischeSignatur?.argumente?.getOrNull(index)?.definitionsMenge?.let {
                put("${präfix}werteVorrat", it.zuLatex())
            }
        }
    }
    return knoten.copy(
        anschlüsse = listOf(methodenEingang.copy(reihenfolge = 0)) + argumente +
            ausgang.copy(
                art = if (knoten.parameter[METHODEN_AUFRUF_PROJEKTION_FEST] == "true") ausgang.art else ergebnisArt,
                vertrag = if (knoten.parameter[METHODEN_AUFRUF_PROJEKTION_FEST] == "true") ausgang.vertrag else AnschlussVertrag(if(knoten.parameter[METHODEN_AUFRUF_PARTIELL]=="true") TypAusdruck.Atom(MathematischeTypen.Methode) else ergebnisTyp),
            ),
        parameter = knoten.parameter
            .filterKeys { !it.startsWith(METHODEN_AUFRUF_PARAMETER_PREFIX) }
            .minus(METHODEN_AUFRUF_STELLIGKEIT)
            .minus(METHODEN_AUFRUF_ZIELMENGE)
            .minus(METHODEN_AUFRUF_VERTRAGSFEHLER) + vertragsParameter,
    )
}

private fun synchronisiereMethodenArgumente(
    knoten: KnotenDaten,
    methode: Methode?,
    konfigurierteProjektion: String,
): KnotenDaten {
    val methodenEingang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Eingang && it.name == "methode"
    } ?: return knoten
    val signatur = (methode as? SignaturtragendeMethode)?.signatur
    val projektion = if (signatur == null) METHODEN_ARGUMENTPROJEKTION_TUPEL else konfigurierteProjektion
    val bisherigeAusgänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }

    val ausgänge = if (projektion == METHODEN_ARGUMENTPROJEKTION_TUPEL) {
        val bisher = bisherigeAusgänge.firstOrNull { it.name == "argumente" }
        val tupelTyp = signatur?.argumentTyp
            ?: TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, emptyList())
        listOf(
            (bisher ?: AnschlussDaten(
                id = methodenArgumentTupelId(knoten.id),
                name = "argumente",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Tupel.id,
            )).copy(
                id = methodenArgumentTupelId(knoten.id),
                name = "argumente",
                art = MathematikAnschlussArten.Tupel.id,
                reihenfolge = 0,
                kannSichErweitern = false,
                dynamischErzeugt = false,
                vertrag = AnschlussVertrag(tupelTyp),
            ),
        )
    } else {
        requireNotNull(signatur)
        buildList {
            signatur.argumente.forEachIndexed { index, argument ->
                val name = methodenArgumentAusgangName(argument, index)
                val bisher = bisherigeAusgänge.firstOrNull { it.id == methodenArgumentId(knoten.id, index) }
                    ?: bisherigeAusgänge.firstOrNull { it.name == name }
                add(
                    (bisher ?: AnschlussDaten(
                        id = methodenArgumentId(knoten.id, index),
                        name = name,
                        richtung = AnschlussRichtung.Ausgang,
                        kante = AnschlussKante.Rechts,
                        art = anschlussArtFürTyp(argument.typ),
                    )).copy(
                        id = methodenArgumentId(knoten.id, index),
                        name = name,
                        art = anschlussArtFürTyp(argument.typ),
                        reihenfolge = index,
                        kannSichErweitern = false,
                        dynamischErzeugt = false,
                        vertrag = AnschlussVertrag(argument.typ),
                    ),
                )
            }
            val bisherDimension = bisherigeAusgänge.firstOrNull { it.name == "dimension" }
            add(
                (bisherDimension ?: AnschlussDaten(
                    id = methodenArgumentDimensionId(knoten.id),
                    name = "dimension",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Zahl.id,
                )).copy(
                    id = methodenArgumentDimensionId(knoten.id),
                    name = "dimension",
                    art = MathematikAnschlussArten.Zahl.id,
                    reihenfolge = signatur.argumente.size,
                    kannSichErweitern = false,
                    dynamischErzeugt = false,
                    vertrag = AnschlussVertrag(TypAusdruck.Atom(MathematischeTypen.NatuerlichMitNull)),
                ),
            )
        }
    }

    return knoten.copy(
        anschlüsse = listOf(methodenEingang.copy(reihenfolge = 0)) + ausgänge,
        parameter = knoten.parameter + (METHODEN_ARGUMENTE_PROJEKTION to projektion),
    )
}

private fun tupelAnschluss(knotenId: KnotenId, bisher: AnschlussDaten?): AnschlussDaten =
    (bisher ?: AnschlussDaten(
        id = argumentId(knotenId, 0),
        name = "argument-0",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Tupel.id,
    )).copy(
        id = bisher?.id ?: argumentId(knotenId, 0),
        name = bisher?.name ?: "argument-0",
        art = MathematikAnschlussArten.Tupel.id,
        reihenfolge = 1,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    )

private fun KnotenDaten.argumentAnschlüsse() = anschlüsse
    .filter { it.richtung == AnschlussRichtung.Eingang && it.name != "methode" }
    .sortedBy { it.reihenfolge }

private fun argumentId(knotenId: KnotenId, index: Int) =
    AnschlussId("${knotenId.wert}:methodenAufruf:argument:$index")

private fun methodenArgumentTupelId(knotenId: KnotenId) =
    AnschlussId("${knotenId.wert}:methodenArgumente:tupel")

private fun methodenArgumentId(knotenId: KnotenId, index: Int) =
    AnschlussId("${knotenId.wert}:methodenArgumente:argument:$index")

private fun methodenArgumentDimensionId(knotenId: KnotenId) =
    AnschlussId("${knotenId.wert}:methodenArgumente:dimension")

/**
 * Grobe Anschlussart als UI-Fallback. Der präzise semantische Typ bleibt immer im
 * [AnschlussVertrag], weshalb unbekannte spätere Engine-Typen nicht auf Mathematik
 * abgebildet werden müssen.
 */
private fun anschlussArtFürTyp(typ: TypAusdruck): AnschlussArtId = when (typ) {
    is TypAusdruck.Atom -> when (typ.id) {
        MathematischeTypen.Natuerlich, MathematischeTypen.Ganz, MathematischeTypen.Rational,
        MathematischeTypen.Reell, MathematischeTypen.Komplex, MathematischeTypen.Zahl -> MathematikAnschlussArten.Zahl.id
        else -> AnschlussArtId(typ.id.wert)
    }
    is TypAusdruck.Parameterisiert -> when (typ.konstruktor) {
        MathematischeTypen.Tupel -> MathematikAnschlussArten.Tupel.id
        else -> AnschlussArtId(typ.konstruktor.wert)
    }
    TypAusdruck.Beliebig,
    TypAusdruck.Unbekannt,
    is TypAusdruck.Variable,
    is TypAusdruck.Vereinigung,
    is TypAusdruck.Literal,
    -> MathematikAnschlussArten.AtlasWert.id
}

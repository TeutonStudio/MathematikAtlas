package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val TOPOLOGISCHER_RAUM_KNOTEN_ART = "mathematik.topologischerRaum"
const val METRISCHER_RAUM_KNOTEN_ART = "mathematik.metrischerRaum"
const val TOPOLOGIE_MODUS_PARAMETER = "topologieModus"
const val TOPOLOGIE_SYMBOL_PARAMETER = "topologieSymbol"

/** Anforderungen, die eine Eigenschaft zusätzlich zu ihrem Subjekt benötigt. */
enum class StrukturAnforderung {
    TOPOLOGISCHER_RAUM,
    AFFINE_STRUKTUR,
    QUELL_TOPOLOGIE,
    ZIEL_TOPOLOGIE,
}

data class ErweiterteEigenschaftDefinition(
    val id: String,
    val titel: String,
    val adjektiv: String,
    val strukturAnforderungen: Set<StrukturAnforderung> = emptySet(),
)

object StrukturEigenschaften {
    val Endlich = ErweiterteEigenschaftDefinition("endlich", "Endlichkeit", "endlich")
    val Unendlich = ErweiterteEigenschaftDefinition("unendlich", "Unendlichkeit", "unendlich")
    val Abzaehlbar = ErweiterteEigenschaftDefinition("abzählbar", "Abzählbarkeit", "abzählbar")
    val Ueberabzaehlbar = ErweiterteEigenschaftDefinition("überabzählbar", "Überabzählbarkeit", "überabzählbar")

    val intrinsischeMengenEigenschaften = listOf(Endlich, Unendlich, Abzaehlbar, Ueberabzaehlbar)
    val intrinsischeIds = intrinsischeMengenEigenschaften.mapTo(linkedSetOf()) { it.id }

    fun findeIntrinsisch(id: String?): ErweiterteEigenschaftDefinition? {
        val normalisiert = id.orEmpty().trim().lowercase()
            .replace("ueberabzaehlbar", "überabzählbar")
            .replace("uberabzahlbar", "überabzählbar")
            .replace("abzaehlbar", "abzählbar")
        return intrinsischeMengenEigenschaften.firstOrNull { it.id == normalisiert }
    }

    fun strukturAnforderungen(id: String?): Set<StrukturAnforderung> = when (id?.trim()?.lowercase()) {
        "offen", "abgeschlossen", "geschlossen" -> setOf(StrukturAnforderung.TOPOLOGISCHER_RAUM)
        "konvexe-menge" -> setOf(StrukturAnforderung.AFFINE_STRUKTUR)
        "stetig" -> setOf(StrukturAnforderung.QUELL_TOPOLOGIE, StrukturAnforderung.ZIEL_TOPOLOGIE)
        else -> emptySet()
    }
}

object TopologischeStrukturKnotenVorlagen {
    private fun eingang(name: String, art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
    )

    private fun ausgang(name: String, art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
    )

    val TopologischerRaum = KnotenVorlage(
        art = TOPOLOGISCHER_RAUM_KNOTEN_ART,
        name = "Topologischer Raum",
        kategorie = "Mengenlehre: Topologie",
        beschreibung = "Erweitert eine Menge X explizit um eine Topologie τ und erzeugt den Raum (X,τ).",
        standardGröße = GraphGröße(300f, 135f),
        anschlüsse = listOf(
            eingang("menge", MathematikAnschlussArten.Menge.id),
            eingang("topologie", MathematikAnschlussArten.Topologie.id),
            ausgang("raum", MathematikAnschlussArten.TopologischerRaum.id),
        ),
        standardParameter = mapOf(
            TOPOLOGIE_MODUS_PARAMETER to "kanonisch",
            TOPOLOGIE_SYMBOL_PARAMETER to "\\tau",
        ),
    )

    val MetrischerRaum = KnotenVorlage(
        art = METRISCHER_RAUM_KNOTEN_ART,
        name = "Metrischer Raum",
        kategorie = "Mengenlehre: Topologie",
        beschreibung = "Erweitert eine Menge X um eine Metrik d und liefert zusätzlich die induzierte topologische Raumsicht (X,τ_d).",
        standardGröße = GraphGröße(315f, 145f),
        anschlüsse = listOf(
            eingang("menge", MathematikAnschlussArten.Menge.id),
            eingang("metrik", MathematikAnschlussArten.Methode.id),
            ausgang("raum", MathematikAnschlussArten.MetrischerRaum.id),
            ausgang("topologie", MathematikAnschlussArten.TopologischerRaum.id),
        ),
    )

    val alle = listOf(TopologischerRaum, MetrischerRaum)
}

/**
 * Sichtbare Eigenschaftsvorlagen erhalten nur die für ihren aktuellen Modus nötigen
 * Strukturanschlüsse. Historische Karten bleiben über ihre bestehenden Anschlüsse ladbar.
 */
fun topologieFaehigeEigenschaftsVorlage(vorlage: KnotenVorlage): KnotenVorlage = when (vorlage.art) {
    MENGEN_EIGENSCHAFT_KNOTEN_ART -> konfiguriereMengenEigenschaftVorlage(vorlage)
    METHODEN_EIGENSCHAFT_KNOTEN_ART -> konfiguriereMethodenEigenschaftVorlage(vorlage)
    else -> vorlage
}

private fun konfiguriereMengenEigenschaftVorlage(vorlage: KnotenVorlage): KnotenVorlage {
    val eigenschaft = vorlage.standardParameter[EIGENSCHAFT_PARAMETER]
    val basis = vorlage.anschlüsse.filterNot { it.name == "raum" }
    val anschlüsse = if (
        StrukturEigenschaften.strukturAnforderungen(eigenschaft).contains(StrukturAnforderung.TOPOLOGISCHER_RAUM)
    ) {
        basis.mitEingangVorAusgang("raum", MathematikAnschlussArten.TopologischerRaum.id)
    } else basis
    return vorlage.copy(
        beschreibung = "Prüft intrinsische oder strukturabhängige Mengeneigenschaften ohne impliziten Umgebungsraum.",
        anschlüsse = anschlüsse,
        standardParameter = vorlage.standardParameter - EIGENSCHAFT_KONTEXT_PARAMETER - "umgebungsraum" - "topologie",
    )
}

private fun konfiguriereMethodenEigenschaftVorlage(vorlage: KnotenVorlage): KnotenVorlage {
    val eigenschaft = vorlage.standardParameter[EIGENSCHAFT_PARAMETER]
    val basis = vorlage.anschlüsse.filterNot { it.name in setOf("quellRaum", "zielRaum") }
    val anschlüsse = if (eigenschaft == MathematischeEigenschaftRegister.Stetig.id) {
        basis
            .mitEingangVorAusgang("quellRaum", MathematikAnschlussArten.TopologischerRaum.id)
            .mitEingangVorAusgang("zielRaum", MathematikAnschlussArten.TopologischerRaum.id)
    } else basis
    return vorlage.copy(anschlüsse = anschlüsse)
}

fun konfiguriereMengenEigenschaftKnoten(knoten: KnotenDaten, eigenschaftId: String): KnotenDaten {
    val altNachRolle = knoten.anschlüsse.associateBy { it.name to it.richtung }
    val basis = knoten.anschlüsse.filterNot { it.name == "raum" }
    val brauchtRaum = StrukturEigenschaften.strukturAnforderungen(eigenschaftId)
        .contains(StrukturAnforderung.TOPOLOGISCHER_RAUM)
    val neu = if (brauchtRaum) {
        basis.mitEingangVorAusgang("raum", MathematikAnschlussArten.TopologischerRaum.id)
    } else basis
    return knoten.copy(
        anschlüsse = neu.map { ziel ->
            altNachRolle[ziel.name to ziel.richtung]?.let { ziel.copy(id = it.id) } ?: ziel
        },
        parameter = knoten.parameter + (EIGENSCHAFT_PARAMETER to eigenschaftId),
    )
}

fun konfiguriereMethodenEigenschaftKnoten(knoten: KnotenDaten, eigenschaftId: String): KnotenDaten {
    val altNachRolle = knoten.anschlüsse.associateBy { it.name to it.richtung }
    val basis = knoten.anschlüsse.filterNot { it.name in setOf("quellRaum", "zielRaum") }
    val neu = if (eigenschaftId == MathematischeEigenschaftRegister.Stetig.id) {
        basis
            .mitEingangVorAusgang("quellRaum", MathematikAnschlussArten.TopologischerRaum.id)
            .mitEingangVorAusgang("zielRaum", MathematikAnschlussArten.TopologischerRaum.id)
    } else basis
    return knoten.copy(
        anschlüsse = neu.map { ziel ->
            altNachRolle[ziel.name to ziel.richtung]?.let { ziel.copy(id = it.id) } ?: ziel
        },
        parameter = knoten.parameter + (EIGENSCHAFT_PARAMETER to eigenschaftId),
    )
}

private fun List<AnschlussDaten>.mitEingangVorAusgang(
    name: String,
    art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId,
): List<AnschlussDaten> {
    if (any { it.name == name && it.richtung == AnschlussRichtung.Eingang }) return this
    val eingang = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
    )
    val ersterAusgang = indexOfFirst { it.richtung == AnschlussRichtung.Ausgang }.takeIf { it >= 0 } ?: size
    return take(ersterAusgang) + eingang + drop(ersterAusgang)
}

internal fun MathematikAuswerterRegister.registriereTopologischeStrukturen() {
    registriere(TOPOLOGISCHER_RAUM_KNOTEN_ART, MathematikKnotenAuswerter(::werteTopologischenRaumAus))
    registriere(METRISCHER_RAUM_KNOTEN_ART, MathematikKnotenAuswerter(::werteMetrischenRaumAus))

    val bisherigeMengenAuswertung = finde(MENGEN_EIGENSCHAFT_KNOTEN_ART)
    registriere(MENGEN_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        val id = kontext.knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty().trim().lowercase()
        when {
            StrukturEigenschaften.findeIntrinsisch(id) != null -> intrinsischeMengenEigenschaft(kontext)
            id in setOf("offen", "abgeschlossen", "geschlossen") -> topologischeMengenEigenschaft(kontext)
            else -> bisherigeMengenAuswertung?.auswerten(kontext)
                ?: error("Für die Mengeneigenschaft '$id' ist kein Auswerter registriert.")
        }
    }

    val bisherigeMethodenAuswertung = finde(METHODEN_EIGENSCHAFT_KNOTEN_ART)
    registriere(METHODEN_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        val id = kontext.knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty().trim().lowercase()
        if (id == MathematischeEigenschaftRegister.Stetig.id) {
            stetigkeitsEigenschaft(kontext)
        } else {
            bisherigeMethodenAuswertung?.auswerten(kontext)
                ?: error("Für die Methodeneigenschaft '$id' ist kein Auswerter registriert.")
        }
    }
}

private fun werteTopologischenRaumAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
        ?: return KnotenAuswertungsErgebnis(
            emptyMap(),
            fehler = "Für den topologischen Raum fehlt die Trägermenge.",
            eingänge = kontext.eingänge,
        )
    val verbunden = kontext.eingänge["topologie"]?.objekt as? Topologie
    val topologie = verbunden ?: when (kontext.knoten.parameter[TOPOLOGIE_MODUS_PARAMETER]?.trim()?.lowercase()) {
        "diskret" -> DiskreteTopologie(menge)
        "indiskret", "trivial" -> IndiskreteTopologie(menge)
        "symbolisch", "benutzerdefiniert" -> SymbolischeTopologie(
            menge,
            kontext.knoten.parameter[TOPOLOGIE_SYMBOL_PARAMETER]?.takeIf { it.isNotBlank() } ?: "\\tau",
        )
        null, "", "kanonisch", "automatisch" -> StandardTopologieRegister.fuer(menge)
            ?: return KnotenAuswertungsErgebnis(
                emptyMap(),
                fehler = "Für ${menge.zuLatex()} ist keine kanonische Standardtopologie registriert. Wähle diskret, indiskret oder symbolisch.",
                eingänge = kontext.eingänge,
            )
        else -> return KnotenAuswertungsErgebnis(
            emptyMap(),
            fehler = "Unbekannter Topologiemodus '${kontext.knoten.parameter[TOPOLOGIE_MODUS_PARAMETER]}'.",
            eingänge = kontext.eingänge,
        )
    }
    if (topologie.traeger != menge) {
        return KnotenAuswertungsErgebnis(
            emptyMap(),
            fehler = "Die verbundene Topologie gehört zu ${topologie.traeger.zuLatex()} statt zur Trägermenge ${menge.zuLatex()}.",
            eingänge = kontext.eingänge,
        )
    }
    val raum = TopologischerRaum(menge, topologie)
    val warnungen = if (topologieAxiomStatus(topologie) == TopologieAxiomStatus.BEDINGT) {
        listOf("Die symbolische Topologie wird unter der Voraussetzung verwendet, dass sie die Topologieaxiome erfüllt.")
    } else emptyList()
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf("raum" to BedingterWert(raum, kontext.gemeinsameAnnahmenTopologie())),
        eingänge = kontext.eingänge,
        warnungen = warnungen,
    )
}

private fun werteMetrischenRaumAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
        ?: return KnotenAuswertungsErgebnis(
            emptyMap(),
            fehler = "Für den metrischen Raum fehlt die Trägermenge.",
            eingänge = kontext.eingänge,
        )
    val metrik = kontext.eingänge["metrik"]?.objekt as? Methode
        ?: return KnotenAuswertungsErgebnis(
            emptyMap(),
            fehler = "Für den metrischen Raum fehlt die Metrik.",
            eingänge = kontext.eingänge,
        )

    return when (val pruefung = pruefeMetrik(menge, metrik)) {
        is StrukturPruefung.Ungueltig ->
            KnotenAuswertungsErgebnis(emptyMap(), fehler = pruefung.grund, eingänge = kontext.eingänge)
        is StrukturPruefung.Unentscheidbar ->
            KnotenAuswertungsErgebnis(emptyMap(), fehler = pruefung.grund, eingänge = kontext.eingänge)
        is StrukturPruefung.Gueltig ->
            metrischerRaumErgebnis(kontext, MetrischerRaum(menge, metrik), emptyList())
        is StrukturPruefung.Bedingt -> {
            val raum = pruefung.wert?.let { MetrischerRaum(it.traeger, it.metrik) }
                ?: return KnotenAuswertungsErgebnis(
                    emptyMap(),
                    fehler = "Die Metrikstruktur ist noch unvollständig.",
                    eingänge = kontext.eingänge,
                )
            metrischerRaumErgebnis(
                kontext,
                raum,
                listOf("Metrikaxiome noch nicht bewiesen: ${pruefung.bedingungen.joinToString(", ")}.")
            )
        }
    }
}

private fun metrischerRaumErgebnis(
    kontext: KnotenAuswertungsKontext,
    raum: MetrischerRaum,
    warnungen: List<String>,
): KnotenAuswertungsErgebnis {
    val annahmen = kontext.gemeinsameAnnahmenTopologie()
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "raum" to BedingterWert(raum, annahmen),
            "topologie" to BedingterWert(raum.alsTopologischerRaum, annahmen),
        ),
        eingänge = kontext.eingänge,
        warnungen = warnungen,
    )
}

private fun intrinsischeMengenEigenschaft(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
        ?: error("Die zu prüfende Menge fehlt.")
    val definition = StrukturEigenschaften.findeIntrinsisch(kontext.knoten.parameter[EIGENSCHAFT_PARAMETER])
        ?: error("Unbekannte intrinsische Mengeneigenschaft.")
    val vertrag = kardinalitaetsVertrag(menge)
    val status = when (definition.id) {
        StrukturEigenschaften.Endlich.id -> vertrag.endlichkeit.alsAussageStatus(erwartetEndlich = true)
        StrukturEigenschaften.Unendlich.id -> vertrag.endlichkeit.alsAussageStatus(erwartetEndlich = false)
        StrukturEigenschaften.Abzaehlbar.id ->
            vertrag.abzaehlbarkeit.alsAussageStatus(erwartetAbzaehlbar = true)
        StrukturEigenschaften.Ueberabzaehlbar.id ->
            vertrag.abzaehlbarkeit.alsAussageStatus(erwartetAbzaehlbar = false)
        else -> AussageStatus.UNENTSCHEIDBAR
    }
    return kontext.eigenschaftsErgebnis(
        EigenschaftsAussage(
            eigenschaftId = definition.id,
            eigenschaftLatex = definition.adjektiv,
            subjektLatex = menge.zuLatex(),
            unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
            aussageStatus = status,
            diagnose = EigenschaftsDiagnose(
                code = "kardinalitaet-${definition.id}",
                nachricht = vertrag.begruendung,
            ),
        ),
    )
}

private fun topologischeMengenEigenschaft(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
        ?: error("Die zu prüfende Menge fehlt.")
    val id = kontext.knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty().trim().lowercase()
    val offen = id == "offen"
    val raum = kontext.eingänge["raum"]?.objekt.alsTopologischerRaumOderNull()
    val aussage = if (raum == null) {
        EigenschaftsAussage(
            eigenschaftId = if (offen) "offen" else "abgeschlossen",
            eigenschaftLatex = if (offen) "offen" else "abgeschlossen",
            subjektLatex = menge.zuLatex(),
            unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
            aussageStatus = AussageStatus.BEDINGT,
            diagnose = EigenschaftsDiagnose(
                code = "topologischer-raum-fehlt",
                nachricht = "Topologischer Raum fehlt. Offenheit und Abgeschlossenheit sind keine Eigenschaften einer nackten Menge.",
                voraussetzungen = listOf("Topologischer Raum (X,τ)", "Nachweis A ⊆ X"),
            ),
        )
    } else {
        val status = if (offen) raum.offenheitsStatus(menge) else raum.abgeschlossenheitsStatus(menge)
        EigenschaftsAussage(
            eigenschaftId = if (offen) "offen" else "abgeschlossen",
            eigenschaftLatex = if (offen) "offen" else "abgeschlossen",
            subjektLatex = menge.zuLatex(),
            unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
            aussageStatus = status,
            diagnose = EigenschaftsDiagnose(
                code = "topologie-${if (offen) "offen" else "abgeschlossen"}",
                nachricht = when (status) {
                    AussageStatus.BEWIESEN ->
                        "Die Eigenschaft folgt aus der explizit verbundenen Topologie ${raum.topologie.zuLatex()}."
                    AussageStatus.WIDERLEGT ->
                        "Die Menge erfüllt die Eigenschaft in der explizit verbundenen Topologie nicht."
                    AussageStatus.BEDINGT ->
                        "Zunächst muss A ⊆ X strukturell nachgewiesen werden."
                    AussageStatus.UNENTSCHEIDBAR ->
                        "Der topologische Raum ist bekannt, der aktuelle Prüfer kann die Zugehörigkeit zur Topologie aber noch nicht entscheiden."
                },
                voraussetzungen = listOf("A ⊆ ${raum.traeger.zuLatex()}"),
            ),
            kontextLatex = raum.zuLatex(),
        )
    }
    return kontext.eigenschaftsErgebnis(aussage)
}

private fun stetigkeitsEigenschaft(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val methode = kontext.eingänge["methode"]?.objekt as? Methode
        ?: error("Die zu prüfende Methode fehlt.")
    val quellRaum = kontext.eingänge["quellRaum"]?.objekt.alsTopologischerRaumOderNull()
    val zielRaum = kontext.eingänge["zielRaum"]?.objekt.alsTopologischerRaumOderNull()

    if (quellRaum == null || zielRaum == null) {
        val fehlend = buildList {
            if (quellRaum == null) add("Quelltopologie")
            if (zielRaum == null) add("Zieltopologie")
        }
        return kontext.eigenschaftsErgebnis(
            EigenschaftsAussage(
                eigenschaftId = "stetig",
                eigenschaftLatex = "stetig",
                subjektLatex = methode.name,
                unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
                aussageStatus = AussageStatus.BEDINGT,
                diagnose = EigenschaftsDiagnose(
                    code = "stetigkeitsraeume-fehlen",
                    nachricht = "Stetigkeit ist erst nach Wahl einer Quell- und Zieltopologie definiert.",
                    voraussetzungen = fehlend,
                ),
            ),
        )
    }

    val signatur = runCatching { methode.methodenSignatur() }.getOrElse { fehler ->
        return kontext.eigenschaftsErgebnis(
            EigenschaftsAussage(
                eigenschaftId = "stetig",
                eigenschaftLatex = "stetig",
                subjektLatex = methode.name,
                unterstuetzung = UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH,
                aussageStatus = AussageStatus.UNENTSCHEIDBAR,
                diagnose = EigenschaftsDiagnose(
                    code = "stetigkeits-signatur-fehlt",
                    nachricht = fehler.message ?: "Die Methodensignatur ist unvollständig.",
                ),
            ),
        )
    }

    val erwarteteQuelle = signatur.effektiverWerteVorrat
        ?: signatur.argumente.singleOrNull()?.werteVorrat
        ?: signatur.werteVorrat
    val falscheQuelle = quellRaum.traeger != erwarteteQuelle
    val falschesZiel = zielRaum.traeger != signatur.zielMenge
    if (falscheQuelle || falschesZiel) {
        return kontext.eigenschaftsErgebnis(
            EigenschaftsAussage(
                eigenschaftId = "stetig",
                eigenschaftLatex = "stetig",
                subjektLatex = methode.name,
                unterstuetzung = UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH,
                aussageStatus = AussageStatus.UNENTSCHEIDBAR,
                diagnose = EigenschaftsDiagnose(
                    code = "stetigkeits-signatur-inkompatibel",
                    nachricht = buildString {
                        append("Die verbundenen Räume passen nicht zur Methodensignatur. ")
                        append("Erwartet: ${erwarteteQuelle.zuLatex()} → ${signatur.zielMenge.zuLatex()}; ")
                        append("verbunden: ${quellRaum.traeger.zuLatex()} → ${zielRaum.traeger.zuLatex()}.")
                    },
                ),
                kontextLatex = "${quellRaum.zuLatex()}\\to${zielRaum.zuLatex()}",
            ),
        )
    }

    val status = when {
        quellRaum.topologie is DiskreteTopologie -> AussageStatus.BEWIESEN
        zielRaum.topologie is IndiskreteTopologie -> AussageStatus.BEWIESEN
        else -> AussageStatus.UNENTSCHEIDBAR
    }
    return kontext.eigenschaftsErgebnis(
        EigenschaftsAussage(
            eigenschaftId = "stetig",
            eigenschaftLatex = "stetig",
            subjektLatex = methode.name,
            unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
            aussageStatus = status,
            diagnose = EigenschaftsDiagnose(
                code = if (status == AussageStatus.BEWIESEN) {
                    "stetigkeit-strukturell"
                } else {
                    "stetigkeit-urbildkriterium"
                },
                nachricht = if (status == AussageStatus.BEWIESEN) {
                    "Die Stetigkeit folgt unmittelbar aus der verbundenen diskreten Quell- oder indiskreten Zieltopologie."
                } else {
                    "Kanonisch gilt: Für jedes V der Zieltopologie muss f⁻¹(V) zur Quelltopologie gehören. Der konkrete Urbildnachweis bleibt symbolisch."
                },
            ),
            kontextLatex = "${quellRaum.zuLatex()}\\to${zielRaum.zuLatex()}",
        ),
    )
}

private fun MathematischesObjekt?.alsTopologischerRaumOderNull(): TopologischerRaum? = when (this) {
    is TopologischerRaum -> this
    is MetrischerRaum -> alsTopologischerRaum
    else -> null
}

private fun EndlichkeitsStatus.alsAussageStatus(erwartetEndlich: Boolean): AussageStatus = when (this) {
    EndlichkeitsStatus.ENDLICH -> if (erwartetEndlich) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
    EndlichkeitsStatus.UNENDLICH -> if (erwartetEndlich) AussageStatus.WIDERLEGT else AussageStatus.BEWIESEN
    EndlichkeitsStatus.UNENTSCHEIDBAR -> AussageStatus.UNENTSCHEIDBAR
}

private fun AbzaehlbarkeitsStatus.alsAussageStatus(erwartetAbzaehlbar: Boolean): AussageStatus = when (this) {
    AbzaehlbarkeitsStatus.ABZAEHLBAR ->
        if (erwartetAbzaehlbar) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
    AbzaehlbarkeitsStatus.UEBERABZAEHLBAR ->
        if (erwartetAbzaehlbar) AussageStatus.WIDERLEGT else AussageStatus.BEWIESEN
    AbzaehlbarkeitsStatus.UNENTSCHEIDBAR -> AussageStatus.UNENTSCHEIDBAR
}

private fun KnotenAuswertungsKontext.eigenschaftsErgebnis(aussage: EigenschaftsAussage): KnotenAuswertungsErgebnis =
    KnotenAuswertungsErgebnis(
        ausgaben = mapOf("aussage" to BedingterWert(aussage, gemeinsameAnnahmenTopologie())),
        eingänge = eingänge,
        warnungen = aussage.diagnose?.takeIf {
            aussage.aussageStatus in setOf(AussageStatus.BEDINGT, AussageStatus.UNENTSCHEIDBAR)
        }?.let { listOf(it.nachricht) }.orEmpty(),
    )

private fun KnotenAuswertungsKontext.gemeinsameAnnahmenTopologie(): Set<Aussage> =
    eingänge.values.flatMap { it.annahmen }.toSet()

/** Intrinsische Mengeneigenschaften können ohne Strukturkontext automatisch erscheinen. */
fun automatischeAdjektive(objekt: MengenAusdruck): List<AutomatischesAdjektiv> {
    val vertrag = kardinalitaetsVertrag(objekt)
    val eigenschaften = buildList {
        when (vertrag.endlichkeit) {
            EndlichkeitsStatus.ENDLICH -> add(StrukturEigenschaften.Endlich)
            EndlichkeitsStatus.UNENDLICH -> add(StrukturEigenschaften.Unendlich)
            EndlichkeitsStatus.UNENTSCHEIDBAR -> Unit
        }
        when (vertrag.abzaehlbarkeit) {
            AbzaehlbarkeitsStatus.ABZAEHLBAR -> add(StrukturEigenschaften.Abzaehlbar)
            AbzaehlbarkeitsStatus.UEBERABZAEHLBAR -> add(StrukturEigenschaften.Ueberabzaehlbar)
            AbzaehlbarkeitsStatus.UNENTSCHEIDBAR -> Unit
        }
    }
    return eigenschaften.mapIndexed { index, definition ->
        AutomatischesAdjektiv(
            eigenschaftId = definition.id,
            text = definition.adjektiv,
            wissensId = "eigenschaft.${definition.id}",
            subjektLatex = objekt.zuLatex(),
            erklärung = vertrag.begruendung,
            rang = 280 + index,
        )
    }
}

/** Bei einem strukturierten Raum dürfen zusätzlich tatsächlich belegte Topologieadjektive erscheinen. */
fun automatischeAdjektive(objekt: TopologischerRaum): List<AutomatischesAdjektiv> = buildList {
    addAll(automatischeAdjektive(objekt.traeger))
    listOf(
        "offen" to objekt.offenheitsStatus(objekt.traeger),
        "abgeschlossen" to objekt.abgeschlossenheitsStatus(objekt.traeger),
    ).filter { it.second == AussageStatus.BEWIESEN }.forEachIndexed { index, (id, _) ->
        add(
            AutomatischesAdjektiv(
                eigenschaftId = id,
                text = id,
                wissensId = "eigenschaft.$id",
                subjektLatex = objekt.traeger.zuLatex(),
                erklärung = "Die Eigenschaft folgt aus der explizit vorhandenen topologischen Struktur ${objekt.topologie.zuLatex()}.",
                rang = 300 + index,
            ),
        )
    }
}

package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val TOPOLOGIE_ART = "mathematik.topologie"
const val TOPOLOGISCHER_RAUM_ART = "mathematik.topologischerRaum"
const val METRISCHER_RAUM_ART = "mathematik.metrischerRaum"
const val METRIK_ART = "mathematik.metrik"
const val STRUKTUR_EIGENSCHAFT_KNOTEN_ART = "mathematik.strukturEigenschaft"
const val EIGENSCHAFT_PARAMETER = "eigenschaft"

private val TOPOLOGIE_ID = AnschlussArtId(TOPOLOGIE_ART)
private val TOPOLOGISCHER_RAUM_ID = AnschlussArtId(TOPOLOGISCHER_RAUM_ART)
private val METRISCHER_RAUM_ID = AnschlussArtId(METRISCHER_RAUM_ART)
private val METRIK_ID = AnschlussArtId(METRIK_ART)

object TopologischeStrukturKnotenVorlagen {
    val Topologie = KnotenVorlage(
        art = TOPOLOGIE_ART,
        name = "Topologie",
        kategorie = "Topologie",
        beschreibung = "Erzeugt eine explizite Topologie τ auf einer Trägermenge X.",
        standardGröße = GraphGröße(260f, 150f),
        anschlüsse = listOf(
            AnschlussDaten("träger", AnschlussRichtung.Eingang, AnschlussKante.Links, AnschlussArtId("mathematik.menge")),
            AnschlussDaten("offeneMengen", AnschlussRichtung.Eingang, AnschlussKante.Links, AnschlussArtId("mathematik.menge")),
            AnschlussDaten("topologie", AnschlussRichtung.Ausgang, AnschlussKante.Rechts, TOPOLOGIE_ID),
        ),
    )

    val TopologischerRaum = KnotenVorlage(
        art = TOPOLOGISCHER_RAUM_ART,
        name = "Topologischer Raum",
        kategorie = "Topologie",
        beschreibung = "Verbindet eine Trägermenge X mit einer expliziten Topologie τ.",
        standardGröße = GraphGröße(280f, 140f),
        anschlüsse = listOf(
            AnschlussDaten("träger", AnschlussRichtung.Eingang, AnschlussKante.Links, AnschlussArtId("mathematik.menge")),
            AnschlussDaten("topologie", AnschlussRichtung.Eingang, AnschlussKante.Links, TOPOLOGIE_ID),
            AnschlussDaten("raum", AnschlussRichtung.Ausgang, AnschlussKante.Rechts, TOPOLOGISCHER_RAUM_ID),
        ),
    )

    val MetrischerRaum = KnotenVorlage(
        art = METRISCHER_RAUM_ART,
        name = "Metrischer Raum",
        kategorie = "Topologie",
        beschreibung = "Verbindet eine Trägermenge X mit einer expliziten Metrik d und der induzierten Topologie.",
        standardGröße = GraphGröße(280f, 140f),
        anschlüsse = listOf(
            AnschlussDaten("träger", AnschlussRichtung.Eingang, AnschlussKante.Links, AnschlussArtId("mathematik.menge")),
            AnschlussDaten("metrik", AnschlussRichtung.Eingang, AnschlussKante.Links, METRIK_ID),
            AnschlussDaten("raum", AnschlussRichtung.Ausgang, AnschlussKante.Rechts, METRISCHER_RAUM_ID),
        ),
    )

    val Eigenschaft = KnotenVorlage(
        art = STRUKTUR_EIGENSCHAFT_KNOTEN_ART,
        name = "Eigenschaft",
        kategorie = "Eigenschaften",
        beschreibung = "Prüft eine strukturabhängige mathematische Eigenschaft mit sichtbarem Kontext.",
        standardGröße = GraphGröße(270f, 150f),
        anschlüsse = listOf(
            AnschlussDaten("objekt", AnschlussRichtung.Eingang, AnschlussKante.Links, AnschlussArtId("mathematik.objekt")),
            AnschlussDaten("aussage", AnschlussRichtung.Ausgang, AnschlussKante.Rechts, AnschlussArtId("mathematik.aussage")),
        ),
        parameter = mapOf(EIGENSCHAFT_PARAMETER to StrukturEigenschaften.Endlich.id),
    )
}

fun registriereTopologischeStrukturAnschlussArten(register: AnschlussArtRegister) {
    register.registriere(AnschlussArtDefinition(TOPOLOGIE_ID, "Topologie", AnschlussKategorie.Menge, 0xFF00897B))
    register.registriere(AnschlussArtDefinition(TOPOLOGISCHER_RAUM_ID, "Topologischer Raum", AnschlussKategorie.Menge, 0xFF00796B))
    register.registriere(AnschlussArtDefinition(METRIK_ID, "Metrik", AnschlussKategorie.Methode, 0xFF5E35B1))
    register.registriere(AnschlussArtDefinition(METRISCHER_RAUM_ID, "Metrischer Raum", AnschlussKategorie.Menge, 0xFF00695C))
}

fun MathematikAuswerterRegister.registriereTopologischeStrukturAuswerter() {
    registriere(TOPOLOGIE_ART) { kontext ->
        val traeger = kontext.eingänge["träger"]?.objekt as? MengenAusdruck
            ?: error("Die Trägermenge X fehlt.")
        val offeneMengen = kontext.eingänge["offeneMengen"]?.objekt as? MengenAusdruck
            ?: error("Die Familie der offenen Mengen fehlt.")
        val topologie = Topologie(traeger, offeneMengen)
        KnotenAuswertungsErgebnis(mapOf("topologie" to BedingterWert(topologie, kontext.gemeinsameAnnahmenTopologie())))
    }
    registriere(TOPOLOGISCHER_RAUM_ART) { kontext ->
        val traeger = kontext.eingänge["träger"]?.objekt as? MengenAusdruck
            ?: error("Die Trägermenge X fehlt.")
        val topologie = kontext.eingänge["topologie"]?.objekt as? Topologie
            ?: error("Die Topologie τ fehlt.")
        require(topologie.traeger == traeger) { "Die Topologie ist nicht auf der verbundenen Trägermenge definiert." }
        KnotenAuswertungsErgebnis(
            mapOf("raum" to BedingterWert(TopologischerRaum(traeger, topologie), kontext.gemeinsameAnnahmenTopologie())),
        )
    }
    registriere(METRISCHER_RAUM_ART) { kontext ->
        val traeger = kontext.eingänge["träger"]?.objekt as? MengenAusdruck
            ?: error("Die Trägermenge X fehlt.")
        val metrik = kontext.eingänge["metrik"]?.objekt as? Metrik
            ?: error("Die Metrik d fehlt.")
        require(metrik.traeger == traeger) { "Die Metrik ist nicht auf der verbundenen Trägermenge definiert." }
        val raum = MetrischerRaum(traeger, metrik)
        KnotenAuswertungsErgebnis(mapOf("raum" to BedingterWert(raum, kontext.gemeinsameAnnahmenTopologie())))
    }
    registriere(STRUKTUR_EIGENSCHAFT_KNOTEN_ART) { kontext -> werteStrukturEigenschaftAus(kontext) }
}

private fun werteStrukturEigenschaftAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val id = kontext.knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty().trim().lowercase()
    return when (id) {
        "endlich", "unendlich", "abzählbar", "ueberabzaehlbar", "überabzählbar" ->
            kardinalitaetsEigenschaft(kontext, id)
        "offen", "abgeschlossen" -> topologischeMengenEigenschaft(kontext)
        "stetig" -> stetigkeitsEigenschaft(kontext)
        else -> kontext.eigenschaftsErgebnis(
            EigenschaftsAussage(
                eigenschaftId = id,
                eigenschaftLatex = id,
                subjektLatex = kontext.eingänge["objekt"]?.anzeigeLatex().orEmpty(),
                unterstuetzung = UnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT,
                aussageStatus = AussageStatus.UNENTSCHEIDBAR,
                diagnose = EigenschaftsDiagnose("eigenschaft-unbekannt", "Für '$id' ist noch keine strukturierte Prüfung registriert."),
            ),
        )
    }
}

private fun kardinalitaetsEigenschaft(
    kontext: KnotenAuswertungsKontext,
    id: String,
): KnotenAuswertungsErgebnis {
    val menge = kontext.eingänge["objekt"]?.objekt as? MengenAusdruck
        ?: kontext.eingänge["menge"]?.objekt as? MengenAusdruck
        ?: error("Die zu prüfende Menge fehlt.")
    val vertrag = kardinalitaetsVertrag(menge)
    val definition = when (id) {
        "endlich" -> StrukturEigenschaften.Endlich
        "unendlich" -> StrukturEigenschaften.Unendlich
        "abzählbar" -> StrukturEigenschaften.Abzaehlbar
        else -> StrukturEigenschaften.Ueberabzaehlbar
    }
    val status = when (definition) {
        StrukturEigenschaften.Endlich -> vertrag.endlichkeit.alsAussageStatus(erwartetEndlich = true)
        StrukturEigenschaften.Unendlich -> vertrag.endlichkeit.alsAussageStatus(erwartetEndlich = false)
        StrukturEigenschaften.Abzaehlbar -> vertrag.abzaehlbarkeit.alsAussageStatus(erwartetAbzaehlbar = true)
        StrukturEigenschaften.Ueberabzaehlbar -> vertrag.abzaehlbarkeit.alsAussageStatus(erwartetAbzaehlbar = false)
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

    val signatur = runCatching { methode.mathematischeMethodenSignatur() }.getOrElse { fehler ->
        return kontext.eigenschaftsErgebnis(
            EigenschaftsAussage(
                eigenschaftId = "stetig",
                eigenschaftLatex = "stetig",
                subjektLatex = methode.name,
                unterstuetzung = UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH,
                aussageStatus = AussageStatus.UNENTSCHEIDBAR,
                diagnose = EigenschaftsDiagnose(
                    code = "stetigkeits-signatur-fehlt",
                    nachricht = fehler.message ?: "Die mathematische Methodensignatur ist unvollständig.",
                ),
            ),
        )
    }

    val erwarteteQuelle = signatur.effektiverDefinitionsRaum
        ?: signatur.argumente.singleOrNull()?.definitionsMenge
        ?: signatur.kanonischerArgumentRaum
    val erwartetesZiel = if (signatur.ergebnisse.size == 1) signatur.ergebnisse.single().zielMenge else signatur.zielRaum
    val falscheQuelle = quellRaum.traeger != erwarteteQuelle
    val falschesZiel = zielRaum.traeger != erwartetesZiel
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
                        append("Erwartet: ${erwarteteQuelle.zuLatex()} → ${erwartetesZiel.zuLatex()}; ")
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

private fun AtlasWert?.alsTopologischerRaumOderNull(): TopologischerRaum? = when (this) {
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

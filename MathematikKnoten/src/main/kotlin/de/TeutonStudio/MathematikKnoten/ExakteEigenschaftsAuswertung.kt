package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.Addition
import de.TeutonStudio.MathematikRechenSystem.kern.Aussage
import de.TeutonStudio.MathematikRechenSystem.kern.AussageStatus
import de.TeutonStudio.MathematikRechenSystem.kern.DefinierteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.EigenschaftsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.EigenschaftsDiagnose
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.GebundeneMengenVariable
import de.TeutonStudio.MathematikRechenSystem.kern.Gleichheit
import de.TeutonStudio.MathematikRechenSystem.kern.KomplexeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.Multiplikation
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.UnterstuetzungsStatus
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.Vergleich
import de.TeutonStudio.MathematikRechenSystem.kern.VergleichsArt
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.ableiten
import de.TeutonStudio.MathematikRechenSystem.kern.alsMathematischeMethode
import de.TeutonStudio.MathematikRechenSystem.kern.löseLinear
import de.TeutonStudio.MathematikRechenSystem.kern.mathematischeMethodenSignatur
import de.TeutonStudio.MathematikRechenSystem.kern.neutraleMethodenSignatur
import de.TeutonStudio.MathematikRechenSystem.kern.vereinfache

private enum class GlobaleKrümmung {
    STRENG_KONVEX,
    AFFIN,
    STRENG_KONKAV,
    WECHSELT,
    UNENTSCHEIDBAR,
}

private data class AbleitungsAnalyse(
    val variable: Variable,
    val definitionsMenge: MengenAusdruck,
    val ersteAbleitung: ZahlAusdruck,
    val zweiteAbleitung: ZahlAusdruck,
    val dritteAbleitung: ZahlAusdruck?,
    val krümmung: GlobaleKrümmung,
)

/**
 * Überschreibt die allgemeinen symbolischen Handler nur für die zwei
 * Analysis-Knoten. Unbekannte Fälle bleiben strukturiert symbolisch statt in
 * erfundene Wahrheitswerte oder leere Mengen zu kollabieren.
 */
internal fun MathematikAuswerterRegister.registriereExakteEigenschaftsAuswertung() {
    registriere(METHODEN_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        val methode = kontext.methodenEingang()
        val id = kontext.knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty()
        val definition = MathematischeEigenschaftRegister.finde(id)
            ?: error("Unbekannte Methodeneigenschaft '$id'.")
        val aussage = if (definition.id in setOf(
                MathematischeEigenschaftRegister.Konvex.id,
                MathematischeEigenschaftRegister.Konkav.id,
            )
        ) {
            globaleKonvexitätsAussage(
                methode = methode,
                definition = definition,
                streng = kontext.knoten.parameter[EIGENSCHAFT_STRENGE_PARAMETER].istStreng(),
                gebiet = kontext.knoten.parameter[EIGENSCHAFT_KONTEXT_PARAMETER],
            )
        } else {
            reguläreMethodenEigenschaft(methode, definition, kontext.knoten.parameter)
        }
        kontext.aussageAusgabe(aussage)
    }

    registriere(ANALYSIS_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        val methode = kontext.methodenEingang()
        val id = kontext.knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty()
        val definition = MathematischeEigenschaftRegister.finde(id)
            ?: error("Unbekannte Analysis-Eigenschaft '$id'.")
        val menge = exakteOderSymbolischeStellenMenge(
            methode = methode,
            definition = definition,
            geltung = kontext.knoten.parameter[EIGENSCHAFT_GELTUNG_PARAMETER].orEmpty(),
            streng = kontext.knoten.parameter[EIGENSCHAFT_STRENGE_PARAMETER].istStreng(),
        )
        val wert = BedingterWert(menge, kontext.gemeinsameAnnahmen())
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                // `stellen` ist der verbindliche Vertrag aus #273.
                "stellen" to wert,
                // Lade- und Quellkompatibilität mit dem ersten Entwurf dieses Branches.
                "stellenmenge" to wert,
            ),
            eingänge = kontext.eingänge,
        )
    }
}

private fun globaleKonvexitätsAussage(
    methode: Methode,
    definition: MathematischeEigenschaftDefinition,
    streng: Boolean,
    gebiet: String?,
): EigenschaftsAussage {
    val istKonvex = definition.id == MathematischeEigenschaftRegister.Konvex.id
    val gebietExplizitNichtKonvex = gebiet.orEmpty().contains("nicht-konvex", ignoreCase = true)
    if (gebietExplizitNichtKonvex) {
        return EigenschaftsAussage(
            eigenschaftId = definition.id,
            eigenschaftLatex = definition.adjektiv,
            subjektLatex = methode.name,
            unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
            aussageStatus = AussageStatus.BEDINGT,
            diagnose = EigenschaftsDiagnose(
                code = "gebiet-nicht-konvex",
                nachricht = "Die Jensen-Definition verlangt ein konvexes Prüfgebiet; das gewählte Gebiet erfüllt diese Voraussetzung nicht.",
                voraussetzungen = listOf("konvexes Prüfgebiet"),
            ),
            kontextLatex = gebiet,
        )
    }

    val signaturFehler = konvexitätsSignaturFehler(methode)
    if (signaturFehler != null) {
        return EigenschaftsAussage(
            eigenschaftId = definition.id,
            eigenschaftLatex = definition.adjektiv,
            subjektLatex = methode.name,
            unterstuetzung = UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH,
            aussageStatus = AussageStatus.UNENTSCHEIDBAR,
            diagnose = EigenschaftsDiagnose(
                code = "fehlende-geordnete-affine-struktur",
                nachricht = signaturFehler,
                voraussetzungen = listOf("konvexer Argumentbereich", "reell geordnete Zielmenge"),
            ),
            kontextLatex = gebiet,
        )
    }

    if (methode.neutraleMethodenSignatur().argumente.size > 1) {
        return EigenschaftsAussage(
            eigenschaftId = definition.id,
            eigenschaftLatex = definition.adjektiv,
            subjektLatex = methode.name,
            unterstuetzung = UnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT,
            aussageStatus = AussageStatus.UNENTSCHEIDBAR,
            diagnose = EigenschaftsDiagnose(
                code = "hesse-mehrdimensional-fehlt",
                nachricht = "Die Eigenschaft ist mathematisch definiert; der mehrdimensionale Hesse- und Gebietsprüfer ist noch nicht implementiert.",
                voraussetzungen = listOf("konvexes Prüfgebiet", "Hesse-Matrix oder Jensen-Nachweis"),
            ),
            kontextLatex = gebiet,
        )
    }

    val analyse = analysiereAbleitungen(methode)
    if (analyse == null) {
        return EigenschaftsAussage(
            eigenschaftId = definition.id,
            eigenschaftLatex = definition.adjektiv,
            subjektLatex = methode.name,
            unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
            aussageStatus = AussageStatus.UNENTSCHEIDBAR,
            diagnose = EigenschaftsDiagnose(
                code = "jensen-symbolisch-unentscheidbar",
                nachricht = "Jensen-Definition und Ableitungskriterium sind verfügbar, der vorliegende Ausdruck erlaubt jedoch keinen exakten globalen Vorzeichennachweis.",
            ),
            kontextLatex = gebiet,
        )
    }

    val wahr = when (analyse.krümmung) {
        GlobaleKrümmung.STRENG_KONVEX -> istKonvex
        GlobaleKrümmung.STRENG_KONKAV -> !istKonvex
        GlobaleKrümmung.AFFIN -> !streng
        GlobaleKrümmung.WECHSELT -> false
        GlobaleKrümmung.UNENTSCHEIDBAR -> null
    }
    return EigenschaftsAussage(
        eigenschaftId = definition.id,
        eigenschaftLatex = if (streng) "streng ${definition.adjektiv}" else definition.adjektiv,
        subjektLatex = methode.name,
        unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
        aussageStatus = when (wahr) {
            true -> AussageStatus.BEWIESEN
            false -> AussageStatus.WIDERLEGT
            null -> AussageStatus.UNENTSCHEIDBAR
        },
        diagnose = EigenschaftsDiagnose(
            code = "zweite-ableitung-${analyse.krümmung.name.lowercase()}",
            nachricht = when (analyse.krümmung) {
                GlobaleKrümmung.STRENG_KONVEX -> "Die zweite Ableitung ist auf dem gesamten Prüfgebiet strikt positiv."
                GlobaleKrümmung.STRENG_KONKAV -> "Die zweite Ableitung ist auf dem gesamten Prüfgebiet strikt negativ."
                GlobaleKrümmung.AFFIN -> "Die zweite Ableitung verschwindet identisch; die Methode ist affin und daher nur nicht-streng konvex und konkav."
                GlobaleKrümmung.WECHSELT -> "Die zweite Ableitung wechselt auf dem Prüfgebiet ihr Vorzeichen."
                GlobaleKrümmung.UNENTSCHEIDBAR -> "Das Vorzeichen der zweiten Ableitung bleibt symbolisch unentscheidbar."
            },
        ),
        kontextLatex = gebiet?.takeUnless { it == "automatisch" }
            ?: analyse.definitionsMenge.zuLatex(),
    )
}

private fun reguläreMethodenEigenschaft(
    methode: Methode,
    definition: MathematischeEigenschaftDefinition,
    parameter: Map<String, String>,
): EigenschaftsAussage {
    val signatur = runCatching { methode.mathematischeMethodenSignatur() }.getOrNull()
    val reelleSignatur = signatur != null &&
        signatur.argumente.isNotEmpty() &&
        signatur.argumente.all { argument ->
            argument.definitionsMenge in setOf(
                de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen,
                de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen,
                de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahlen,
                ReelleZahlen,
            )
        } &&
        signatur.ergebnisse.size == 1 &&
        signatur.ergebnisse.single().zielMenge in setOf(
            de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen,
            de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen,
            de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahlen,
            ReelleZahlen,
        )
    val istIntegrabilität = definition.gruppe == EigenschaftsGruppe.Integrabilität
    val integralBegriff = parameter["integralBegriff"].orEmpty()
    val maßKontext = parameter["integrationsKontextReferenz"].orEmpty()
    val bedingt = istIntegrabilität && (integralBegriff.isBlank() || maßKontext.isBlank())
    val ordnung = parameter[EIGENSCHAFT_ORDNUNG_PARAMETER]?.toIntOrNull()?.coerceAtLeast(1) ?: 1

    return EigenschaftsAussage(
        eigenschaftId = definition.id,
        eigenschaftLatex = when (definition.id) {
            MathematischeEigenschaftRegister.Cn.id -> "C^{$ordnung}"
            MathematischeEigenschaftRegister.Dn.id -> "D^{$ordnung}"
            else -> definition.adjektiv
        },
        subjektLatex = methode.name,
        unterstuetzung = if (reelleSignatur) UnterstuetzungsStatus.IMPLEMENTIERT
            else UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH,
        aussageStatus = when {
            !reelleSignatur -> AussageStatus.UNENTSCHEIDBAR
            bedingt -> AussageStatus.BEDINGT
            else -> AussageStatus.UNENTSCHEIDBAR
        },
        diagnose = EigenschaftsDiagnose(
            code = when {
                !reelleSignatur -> "fehlende-analysis-struktur"
                bedingt -> "integrationskontext-fehlt"
                else -> "symbolische-eigenschaft"
            },
            nachricht = when {
                !reelleSignatur -> "Die gewählte Eigenschaft benötigt einen passenden topologischen, differentiellen oder integralen Strukturvertrag."
                bedingt -> "Integralbegriff, Bereich und Maß beziehungsweise Gewichtung müssen ausdrücklich festgelegt werden."
                else -> "Die Eigenschaft ist strukturell zulässig; der konkrete Nachweis bleibt für die symbolische Vorschrift unentscheidbar."
            },
            voraussetzungen = if (bedingt) listOf("Integralbegriff", "Integrationsbereich", "Maß oder Gewichtung") else emptyList(),
        ),
        kontextLatex = parameter[EIGENSCHAFT_KONTEXT_PARAMETER],
    )
}

private fun exakteOderSymbolischeStellenMenge(
    methode: Methode,
    definition: MathematischeEigenschaftDefinition,
    geltung: String,
    streng: Boolean,
): MengenAusdruck {
    val analyse = analysiereAbleitungen(methode)
        ?: return symbolischeStellenMenge(methode, definition, streng, "Ableitungsregeln reichen für den Ausdruck nicht aus.")
    return when (definition.id) {
        MathematischeEigenschaftRegister.Minimum.id ->
            extremStellen(analyse, minimum = true, streng = streng, global = geltung.equals("global", true))
        MathematischeEigenschaftRegister.Maximum.id ->
            extremStellen(analyse, minimum = false, streng = streng, global = geltung.equals("global", true))
        MathematischeEigenschaftRegister.Extremum.id -> {
            val minima = extremStellen(analyse, minimum = true, streng = streng, global = geltung.equals("global", true))
            val maxima = extremStellen(analyse, minimum = false, streng = streng, global = geltung.equals("global", true))
            vereinigeExakt(minima, maxima)
        }
        MathematischeEigenschaftRegister.Sattelpunkt.id -> sattelStellen(analyse)
        MathematischeEigenschaftRegister.Konvexitaetsbereich.id -> krümmungsBereich(analyse, konvex = true, streng = streng)
        MathematischeEigenschaftRegister.Konkavitaetsbereich.id -> krümmungsBereich(analyse, konvex = false, streng = streng)
        MathematischeEigenschaftRegister.Wendestelle.id -> wendeStellen(analyse)
        else -> symbolischeStellenMenge(methode, definition, streng, "Für diese Stellenart ist keine exakte Regel registriert.")
    }
}

private fun extremStellen(
    analyse: AbleitungsAnalyse,
    minimum: Boolean,
    streng: Boolean,
    global: Boolean,
): MengenAusdruck {
    if (analyse.ersteAbleitung == RationaleZahl.Null) {
        return if (streng) LeereMenge else analyse.definitionsMenge
    }
    val kandidaten = lineareNullstellen(analyse.ersteAbleitung, analyse.variable) ?: return DefinierteMenge(
        listOf(GebundeneMengenVariable(analyse.variable, analyse.definitionsMenge)),
        EigenschaftsAussage(
            eigenschaftId = if (minimum) "minimum" else "maximum",
            eigenschaftLatex = if (global) "global" else "lokal",
            subjektLatex = analyse.variable.zuLatex(),
            unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
            aussageStatus = AussageStatus.UNENTSCHEIDBAR,
            diagnose = EigenschaftsDiagnose("nichtlineare-kandidaten", "Die Kandidatenbedingung bleibt symbolisch."),
        ),
    )
    if (kandidaten.isEmpty()) return LeereMenge
    val passt = when (analyse.krümmung) {
        GlobaleKrümmung.STRENG_KONVEX -> minimum
        GlobaleKrümmung.STRENG_KONKAV -> !minimum
        GlobaleKrümmung.AFFIN -> !streng
        GlobaleKrümmung.WECHSELT,
        GlobaleKrümmung.UNENTSCHEIDBAR,
        -> false
    }
    return if (passt) EndlicheMenge(kandidaten.toSet()) else LeereMenge
}

private fun sattelStellen(analyse: AbleitungsAnalyse): MengenAusdruck {
    val stationär = lineareNullstellen(analyse.ersteAbleitung, analyse.variable) ?: return LeereMenge
    val krümmungsNullen = lineareNullstellen(analyse.zweiteAbleitung, analyse.variable) ?: return LeereMenge
    val dritteNichtNull = (analyse.dritteAbleitung as? RationaleZahl)?.let { !it.istNull() } == true
    return if (dritteNichtNull) EndlicheMenge(stationär.intersect(krümmungsNullen).toSet()) else LeereMenge
}

private fun wendeStellen(analyse: AbleitungsAnalyse): MengenAusdruck {
    val nullen = lineareNullstellen(analyse.zweiteAbleitung, analyse.variable) ?: return LeereMenge
    val dritteNichtNull = (analyse.dritteAbleitung as? RationaleZahl)?.let { !it.istNull() } == true
    return if (dritteNichtNull) EndlicheMenge(nullen.toSet()) else LeereMenge
}

private fun krümmungsBereich(
    analyse: AbleitungsAnalyse,
    konvex: Boolean,
    streng: Boolean,
): MengenAusdruck {
    val art = when {
        konvex && streng -> VergleichsArt.Größer
        konvex -> VergleichsArt.GrößerGleich
        !konvex && streng -> VergleichsArt.Kleiner
        else -> VergleichsArt.KleinerGleich
    }
    return DefinierteMenge(
        variablen = listOf(GebundeneMengenVariable(analyse.variable, analyse.definitionsMenge)),
        bedingung = Vergleich(analyse.zweiteAbleitung, art, RationaleZahl.Null),
    )
}

private fun symbolischeStellenMenge(
    methode: Methode,
    definition: MathematischeEigenschaftDefinition,
    streng: Boolean,
    grund: String,
): MengenAusdruck {
    val signatur = methode.mathematischeMethodenSignatur()
    val variablen = signatur.argumente.sortedBy { it.position }.mapIndexed { index, argument ->
        val parameter = argument.parameter
        val variable = parameter as? Variable ?: Variable(parameter.name.ifBlank { "x_${index + 1}" })
        GebundeneMengenVariable(
            variable,
            argument.definitionsMenge,
        )
    }.ifEmpty { listOf(GebundeneMengenVariable(Variable("x"), ReelleZahlen)) }
    return DefinierteMenge(
        variablen = variablen,
        bedingung = EigenschaftsAussage(
            eigenschaftId = definition.id,
            eigenschaftLatex = if (streng) "streng ${definition.adjektiv}" else definition.adjektiv,
            subjektLatex = methode.name,
            unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
            aussageStatus = AussageStatus.UNENTSCHEIDBAR,
            diagnose = EigenschaftsDiagnose(
                code = "symbolische-stellenmenge",
                nachricht = grund,
            ),
        ),
    )
}

private fun analysiereAbleitungen(methode: Methode): AbleitungsAnalyse? {
    val signatur = runCatching { methode.mathematischeMethodenSignatur() }.getOrNull() ?: return null
    val argument = signatur.argumente.singleOrNull() ?: return null
    val variable = argument.parameter as? Variable ?: return null
    val definitionsMenge = argument.definitionsMenge
    val zielMenge = signatur.ergebnisse.singleOrNull()?.zielMenge ?: return null
    if (definitionsMenge !in setOf(
            de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen,
            de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen,
            de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahlen,
            ReelleZahlen,
        ) || zielMenge !in setOf(
            de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen,
            de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen,
            de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahlen,
            ReelleZahlen,
        )
    ) return null
    val mathematisch = runCatching { methode.alsMathematischeMethode("Ableitungsanalyse") }.getOrNull() ?: return null
    val term = mathematisch.vorschrift as? ZahlAusdruck ?: return null
    val erste = runCatching { ableiten(term, variable).ergebnis }.getOrNull() ?: return null
    val zweite = runCatching { ableiten(erste, variable).ergebnis }.getOrNull() ?: return null
    val dritte = runCatching { ableiten(zweite, variable).ergebnis }.getOrNull()
    return AbleitungsAnalyse(
        variable = variable,
        definitionsMenge = definitionsMenge,
        ersteAbleitung = vereinfache(erste),
        zweiteAbleitung = vereinfache(zweite),
        dritteAbleitung = dritte?.let(::vereinfache),
        krümmung = globaleKrümmung(zweite, variable),
    )
}

private fun globaleKrümmung(zweiteAbleitung: ZahlAusdruck, variable: Variable): GlobaleKrümmung {
    val vereinfacht = vereinfache(zweiteAbleitung)
    if (vereinfacht is RationaleZahl) {
        return when {
            vereinfacht > RationaleZahl.Null -> GlobaleKrümmung.STRENG_KONVEX
            vereinfacht < RationaleZahl.Null -> GlobaleKrümmung.STRENG_KONKAV
            else -> GlobaleKrümmung.AFFIN
        }
    }
    val linear = lineareKoeffizienten(vereinfacht, variable)
    if (linear != null && !linear.first.istNull()) return GlobaleKrümmung.WECHSELT
    return GlobaleKrümmung.UNENTSCHEIDBAR
}

private fun lineareNullstellen(ausdruck: ZahlAusdruck, variable: Variable): List<MathematischesObjekt>? {
    if (ausdruck == RationaleZahl.Null) return emptyList()
    return runCatching {
        löseLinear(Gleichheit(ausdruck, RationaleZahl.Null), variable).lösungen
    }.getOrNull()
}

private fun lineareKoeffizienten(
    ausdruck: ZahlAusdruck,
    variable: Variable,
): Pair<RationaleZahl, RationaleZahl>? = when (val term = vereinfache(ausdruck)) {
    is RationaleZahl -> RationaleZahl.Null to term
    is Variable -> if (term == variable) RationaleZahl.Eins to RationaleZahl.Null else null
    is Addition -> term.summanden.map { lineareKoeffizienten(it, variable) ?: return null }
        .fold(RationaleZahl.Null to RationaleZahl.Null) { acc, koeffizienten ->
            (acc.first + koeffizienten.first) to (acc.second + koeffizienten.second)
        }
    is Multiplikation -> {
        val konstante = term.faktoren.filterIsInstance<RationaleZahl>()
            .fold(RationaleZahl.Eins) { links, rechts -> links * rechts }
        val andere = term.faktoren.filterNot { it is RationaleZahl }
        if (andere.size == 1 && andere.single() == variable) konstante to RationaleZahl.Null else null
    }
    else -> null
}

private fun vereinigeExakt(links: MengenAusdruck, rechts: MengenAusdruck): MengenAusdruck = when {
    links == LeereMenge -> rechts
    rechts == LeereMenge -> links
    links == rechts -> links
    links is EndlicheMenge && rechts is EndlicheMenge -> EndlicheMenge(links.elemente + rechts.elemente)
    else -> de.TeutonStudio.MathematikRechenSystem.kern.Vereinigung(listOf(links, rechts))
}

private fun konvexitätsSignaturFehler(methode: Methode): String? {
    val signatur = runCatching { methode.mathematischeMethodenSignatur() }.getOrElse {
        return "Funktionskonvexität benötigt eine mathematische Raum-/Mengensignatur."
    }
    val zielMenge = signatur.ergebnisse.singleOrNull()?.zielMenge
        ?: return "Funktionskonvexität benötigt genau eine skalare Ergebniskomponente."
    return when {
        zielMenge == KomplexeZahlen -> "Komplexe Zahlen besitzen keine mit der Feldstruktur verträgliche kanonische Gesamtordnung für eine Jensen-Ungleichung."
        zielMenge != ReelleZahlen && zielMenge !in setOf(
            de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen,
            de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen,
            de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahlen,
        ) -> "Funktionskonvexität benötigt eine geordnete skalare Zielstruktur."
        signatur.argumente.isEmpty() -> "Eine nullstellige Methode besitzt keinen Argumentbereich für konvexe Kombinationen."
        signatur.argumente.any { it.definitionsMenge !in setOf(
            de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen,
            de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen,
            de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahlen,
            ReelleZahlen,
        ) } -> "Der Argumentbereich benötigt eine sichtbare reelle affine Struktur."
        else -> null
    }
}

private fun String?.istStreng(): Boolean = when (this?.trim()?.lowercase()) {
    "streng", "strict", "true" -> true
    else -> false
}

private fun KnotenAuswertungsKontext.methodenEingang(): Methode =
    eingänge["methode"]?.objekt as? Methode ?: error("Die zu prüfende Methode fehlt.")

private fun KnotenAuswertungsKontext.gemeinsameAnnahmen(): Set<Aussage> =
    eingänge.values.flatMap { it.annahmen }.toSet()

private fun KnotenAuswertungsKontext.aussageAusgabe(aussage: EigenschaftsAussage): KnotenAuswertungsErgebnis =
    KnotenAuswertungsErgebnis(
        ausgaben = mapOf("aussage" to BedingterWert(aussage, gemeinsameAnnahmen())),
        eingänge = eingänge,
        warnungen = aussage.diagnose?.takeIf {
            aussage.unterstuetzung != UnterstuetzungsStatus.IMPLEMENTIERT ||
                aussage.aussageStatus in setOf(AussageStatus.BEDINGT, AussageStatus.UNENTSCHEIDBAR)
        }?.let { listOf(it.nachricht) }.orEmpty(),
    )

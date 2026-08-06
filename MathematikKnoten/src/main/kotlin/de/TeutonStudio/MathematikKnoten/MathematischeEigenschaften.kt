package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.AussageStatus
import de.TeutonStudio.MathematikRechenSystem.kern.BenannteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.DefinierteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.EigenschaftsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.EigenschaftsDiagnose
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.GebundeneMengenVariable
import de.TeutonStudio.MathematikRechenSystem.kern.KomplexeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Matrizenraum
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.ReellesIntervall
import de.TeutonStudio.MathematikRechenSystem.kern.SpaltenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Tupelraum
import de.TeutonStudio.MathematikRechenSystem.kern.UnterstuetzungsStatus
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.Vektorraum
import de.TeutonStudio.MathematikRechenSystem.kern.ZeilenVektor

const val METHODEN_EIGENSCHAFT_KNOTEN_ART = "mathematik.methodenEigenschaft"
const val ANALYSIS_EIGENSCHAFT_KNOTEN_ART = "mathematik.analysisEigenschaft"
const val FOLGEN_EIGENSCHAFT_KNOTEN_ART = "mathematik.folgenEigenschaft"
const val METHODEN_STELLIGKEIT_KNOTEN_ART = "mathematik.methodenStelligkeit"
const val MENGEN_EIGENSCHAFT_KNOTEN_ART = "mathematik.mengenEigenschaft"

const val EIGENSCHAFT_PARAMETER = "eigenschaft"
const val EIGENSCHAFT_ORDNUNG_PARAMETER = "ordnungN"
const val EIGENSCHAFT_GELTUNG_PARAMETER = "geltung"
const val EIGENSCHAFT_STRENGE_PARAMETER = "strenge"
const val EIGENSCHAFT_KONTEXT_PARAMETER = "kontext"

enum class EigenschaftsSubjektArt { Methode, Menge, Folge, Methodensignatur, Argumentstelle }
enum class EigenschaftsGruppe { Regularität, Integrabilität, Funktionsgeometrie, Signatur, Folge, Wertart, Topologie, Konvexität }

data class MathematischeEigenschaftDefinition(
    val id: String,
    val titel: String,
    val adjektiv: String,
    val subjektArt: EigenschaftsSubjektArt,
    val gruppe: EigenschaftsGruppe,
    val wissensId: String,
    val rang: Int,
    val aliase: Set<String> = emptySet(),
    val benötigtOrdnung: Boolean = false,
    val benötigtUmgebungsraum: Boolean = false,
) {
    init {
        require(id.isNotBlank())
        require(titel.isNotBlank())
        require(adjektiv.isNotBlank())
        require(wissensId.isNotBlank())
    }
}

/** Kanonisches Register aller durch dieses Epic eingeführten Eigenschaftsbegriffe. */
object MathematischeEigenschaftRegister {
    val Stetig = definition("stetig", "Stetigkeit", "stetig", EigenschaftsSubjektArt.Methode, EigenschaftsGruppe.Regularität, 100)
    val Differenzierbar = definition("differenzierbar", "Differenzierbarkeit", "differenzierbar", EigenschaftsSubjektArt.Methode, EigenschaftsGruppe.Regularität, 110)
    val Cn = definition("c-n", "Cⁿ-Regularität", "Cⁿ", EigenschaftsSubjektArt.Methode, EigenschaftsGruppe.Regularität, 120, benötigtOrdnung = true)
    val Dn = definition("d-n", "Dⁿ-Regularität", "Dⁿ", EigenschaftsSubjektArt.Methode, EigenschaftsGruppe.Regularität, 121, benötigtOrdnung = true)
    val RiemannIntegrierbar = definition("riemann-integrierbar", "Riemann-Integrierbarkeit", "Riemann-integrierbar", EigenschaftsSubjektArt.Methode, EigenschaftsGruppe.Integrabilität, 130)
    val LebesgueIntegrierbar = definition("lebesgue-integrierbar", "Lebesgue-Integrierbarkeit", "Lebesgue-integrierbar", EigenschaftsSubjektArt.Methode, EigenschaftsGruppe.Integrabilität, 131)
    val Konvex = definition("konvex", "Konvexität", "konvex", EigenschaftsSubjektArt.Methode, EigenschaftsGruppe.Konvexität, 140, benötigtUmgebungsraum = true)
    val Konkav = definition("konkav", "Konkavität", "konkav", EigenschaftsSubjektArt.Methode, EigenschaftsGruppe.Konvexität, 141, benötigtUmgebungsraum = true, aliase = setOf("concave"))

    val Einstellig = definition("einstellig", "Einstelligkeit", "einstellig", EigenschaftsSubjektArt.Methodensignatur, EigenschaftsGruppe.Signatur, 200, aliase = setOf("univariat"))
    val Mehrstellig = definition("mehrstellig", "Mehrstelligkeit", "mehrstellig", EigenschaftsSubjektArt.Methodensignatur, EigenschaftsGruppe.Signatur, 201, aliase = setOf("multivariat"))

    val EinseitigeFolge = definition("einseitige-folge", "Einseitige Folge", "einseitig indiziert", EigenschaftsSubjektArt.Folge, EigenschaftsGruppe.Folge, 210, aliase = setOf("halbfolge", "N0-Folge"))
    val ZweiseitigeFolge = definition("zweiseitige-folge", "Zweiseitige Folge", "zweiseitig indiziert", EigenschaftsSubjektArt.Folge, EigenschaftsGruppe.Folge, 211, aliase = setOf("bi-infinite Folge", "Z-Folge"))
    val Reellwertig = definition("reellwertig", "Reellwertigkeit", "reellwertig", EigenschaftsSubjektArt.Folge, EigenschaftsGruppe.Wertart, 220)
    val Komplexwertig = definition("komplexwertig", "Komplexwertigkeit", "komplexwertig", EigenschaftsSubjektArt.Folge, EigenschaftsGruppe.Wertart, 221)
    val Polynomwertig = definition("polynomwertig", "Polynomwertigkeit", "polynomwertig", EigenschaftsSubjektArt.Folge, EigenschaftsGruppe.Wertart, 222)
    val Vektorwertig = definition("vektorwertig", "Vektorwertigkeit", "vektorwertig", EigenschaftsSubjektArt.Folge, EigenschaftsGruppe.Wertart, 223)

    val Offen = definition("offen", "Offenheit", "offen", EigenschaftsSubjektArt.Menge, EigenschaftsGruppe.Topologie, 300, benötigtUmgebungsraum = true)
    val Abgeschlossen = definition("abgeschlossen", "Abgeschlossenheit", "abgeschlossen", EigenschaftsSubjektArt.Menge, EigenschaftsGruppe.Topologie, 301, benötigtUmgebungsraum = true)
    val KonvexeMenge = definition("konvexe-menge", "Konvexe Menge", "konvex", EigenschaftsSubjektArt.Menge, EigenschaftsGruppe.Konvexität, 302, benötigtUmgebungsraum = true)

    val Minimum = definition("minimumstellen", "Minimumstellen", "Minimumstelle", EigenschaftsSubjektArt.Argumentstelle, EigenschaftsGruppe.Funktionsgeometrie, 400)
    val Maximum = definition("maximumstellen", "Maximumstellen", "Maximumstelle", EigenschaftsSubjektArt.Argumentstelle, EigenschaftsGruppe.Funktionsgeometrie, 401)
    val Extremum = definition("extremstellen", "Extremstellen", "Extremstelle", EigenschaftsSubjektArt.Argumentstelle, EigenschaftsGruppe.Funktionsgeometrie, 402)
    val Sattelpunkt = definition("sattelpunkte", "Sattelpunkte", "Sattelpunkt", EigenschaftsSubjektArt.Argumentstelle, EigenschaftsGruppe.Funktionsgeometrie, 403)
    val Konvexitaetsbereich = definition("konvexitaetsbereich", "Konvexitätsbereich", "lokal konvex", EigenschaftsSubjektArt.Argumentstelle, EigenschaftsGruppe.Funktionsgeometrie, 404)
    val Konkavitaetsbereich = definition("konkavitaetsbereich", "Konkavitätsbereich", "lokal konkav", EigenschaftsSubjektArt.Argumentstelle, EigenschaftsGruppe.Funktionsgeometrie, 405)
    val Wendestelle = definition("wendestellen", "Wendestellen", "Wendestelle", EigenschaftsSubjektArt.Argumentstelle, EigenschaftsGruppe.Funktionsgeometrie, 406)

    val alle: List<MathematischeEigenschaftDefinition> = listOf(
        Stetig, Differenzierbar, Cn, Dn, RiemannIntegrierbar, LebesgueIntegrierbar, Konvex, Konkav,
        Einstellig, Mehrstellig,
        EinseitigeFolge, ZweiseitigeFolge, Reellwertig, Komplexwertig, Polynomwertig, Vektorwertig,
        Offen, Abgeschlossen, KonvexeMenge,
        Minimum, Maximum, Extremum, Sattelpunkt, Konvexitaetsbereich, Konkavitaetsbereich, Wendestelle,
    )
    private val nachId = alle.associateBy(MathematischeEigenschaftDefinition::id)
    private val nachAlias = alle.flatMap { definition -> definition.aliase.map { it.lowercase() to definition } }.toMap()

    fun finde(idOderAlias: String): MathematischeEigenschaftDefinition? {
        val normalisiert = idOderAlias.trim().lowercase()
        return nachId[normalisiert] ?: nachAlias[normalisiert]
    }

    private fun definition(
        id: String,
        titel: String,
        adjektiv: String,
        subjektArt: EigenschaftsSubjektArt,
        gruppe: EigenschaftsGruppe,
        rang: Int,
        aliase: Set<String> = emptySet(),
        benötigtOrdnung: Boolean = false,
        benötigtUmgebungsraum: Boolean = false,
    ) = MathematischeEigenschaftDefinition(
        id = id,
        titel = titel,
        adjektiv = adjektiv,
        subjektArt = subjektArt,
        gruppe = gruppe,
        wissensId = "eigenschaft.$id",
        rang = rang,
        aliase = aliase,
        benötigtOrdnung = benötigtOrdnung,
        benötigtUmgebungsraum = benötigtUmgebungsraum,
    )
}

data class ArgumentRolle(
    val stabileId: String,
    val sichtbarerName: String,
    val position: Int,
) {
    init {
        require(stabileId.isNotBlank())
        require(sichtbarerName.isNotBlank())
        require(position >= 0)
    }
}

enum class ArgumentAnsicht { EinzelArgumente, Tupel, Koordinaten }

data class MethodenSignaturAnsicht(
    val stelligkeit: Int,
    val rollen: List<ArgumentRolle>,
    val ansicht: ArgumentAnsicht,
) {
    init {
        require(stelligkeit == rollen.size)
        require(rollen.map(ArgumentRolle::stabileId).distinct().size == rollen.size)
    }

    companion object {
        fun von(methode: Methode, ansicht: ArgumentAnsicht = ArgumentAnsicht.EinzelArgumente): MethodenSignaturAnsicht =
            MethodenSignaturAnsicht(
                stelligkeit = methode.parameter.size,
                rollen = methode.parameter.mapIndexed { index, parameter ->
                    ArgumentRolle(
                        stabileId = "argument.$index.${parameter.name}",
                        sichtbarerName = parameter.name,
                        position = index,
                    )
                },
                ansicht = ansicht,
            )
    }
}

enum class FolgenArt(val indexMenge: MengenAusdruck) {
    Einseitig(NatürlicheZahlen),
    Zweiseitig(GanzeZahlen),
    KeineFolge(BenannteMenge("?"));

    companion object {
        fun aus(methode: Methode): FolgenArt {
            if (methode.parameter.size != 1) return KeineFolge
            val parameter = methode.parameter.single()
            return when (methode.werteVorräte[parameter.name]) {
                NatürlicheZahlen -> Einseitig
                GanzeZahlen -> Zweiseitig
                else -> KeineFolge
            }
        }

        /** Lademigration für die frühere missverständliche Kennung. */
        fun ausHistorischerKennung(kennung: String): FolgenArt = when (kennung.trim().lowercase()) {
            "unnatürlichestupel", "unnatuerlichestupel", "halbfolge", "n0-folge" -> Einseitig
            "z-folge", "zweiseitig", "bi-infinite" -> Zweiseitig
            else -> KeineFolge
        }
    }
}

data class FolgenVertrag(
    val art: FolgenArt,
    val indexRolle: ArgumentRolle?,
    val zielMenge: MengenAusdruck,
) {
    companion object {
        fun von(methode: Methode): FolgenVertrag = FolgenVertrag(
            art = FolgenArt.aus(methode),
            indexRolle = methode.parameter.singleOrNull()?.let { ArgumentRolle("index.${it.name}", it.name, 0) },
            zielMenge = methode.zielMenge,
        )
    }
}

data class AutomatischesAdjektiv(
    val eigenschaftId: String,
    val text: String,
    val wissensId: String,
    val subjektLatex: String,
    val erklärung: String,
    val rang: Int,
)

/** Stabile, reduzierte Adjektivliste für die interaktive Knotendarstellung. */
fun automatischeAdjektive(objekt: Any): List<AutomatischesAdjektiv> {
    val kandidaten = when (objekt) {
        is Methode -> buildList {
            val signatur = MethodenSignaturAnsicht.von(objekt)
            add(if (signatur.stelligkeit == 1) MathematischeEigenschaftRegister.Einstellig else MathematischeEigenschaftRegister.Mehrstellig)
            when (FolgenArt.aus(objekt)) {
                FolgenArt.Einseitig -> add(MathematischeEigenschaftRegister.EinseitigeFolge)
                FolgenArt.Zweiseitig -> add(MathematischeEigenschaftRegister.ZweiseitigeFolge)
                FolgenArt.KeineFolge -> Unit
            }
            wertArt(objekt)?.let(::add)
        }
        is MengenAusdruck -> buildList {
            mengenAussage(objekt, MathematischeEigenschaftRegister.Offen).takeIf { it.aussageStatus == AussageStatus.BEWIESEN }?.let {
                add(MathematischeEigenschaftRegister.Offen)
            }
            mengenAussage(objekt, MathematischeEigenschaftRegister.Abgeschlossen).takeIf { it.aussageStatus == AussageStatus.BEWIESEN }?.let {
                add(MathematischeEigenschaftRegister.Abgeschlossen)
            }
            mengenAussage(objekt, MathematischeEigenschaftRegister.KonvexeMenge).takeIf { it.aussageStatus == AussageStatus.BEWIESEN }?.let {
                add(MathematischeEigenschaftRegister.KonvexeMenge)
            }
        }
        else -> emptyList()
    }
    return kandidaten.distinctBy(MathematischeEigenschaftDefinition::id)
        .sortedBy(MathematischeEigenschaftDefinition::rang)
        .map { definition ->
            AutomatischesAdjektiv(
                eigenschaftId = definition.id,
                text = definition.adjektiv,
                wissensId = definition.wissensId,
                subjektLatex = when (objekt) {
                    is Methode -> objekt.name
                    is MengenAusdruck -> objekt.zuLatex()
                    else -> objekt.toString()
                },
                erklärung = "${definition.titel} ist eine automatisch aus der mathematischen Struktur abgeleitete Eigenschaft. Die Anzeige ist nicht persistiert.",
                rang = definition.rang,
            )
        }
}

object MathematischeEigenschaftKnotenVorlagen {
    private fun eingang(name: String, art: AnschlussArtId) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
    )

    private fun ausgang(name: String, art: AnschlussArtId) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
    )

    val MethodenEigenschaft = KnotenVorlage(
        art = METHODEN_EIGENSCHAFT_KNOTEN_ART,
        name = "Methodeneigenschaft",
        kategorie = "Analysis: Eigenschaften",
        beschreibung = "Prüft Regularität, Integrabilität oder globale Konvexität einer Methode und gibt eine strukturierte Aussage aus.",
        standardGröße = GraphGröße(290f, 125f),
        anschlüsse = listOf(
            eingang("methode", MathematikAnschlussArten.Methode.id),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        ),
        standardParameter = mapOf(
            EIGENSCHAFT_PARAMETER to MathematischeEigenschaftRegister.Stetig.id,
            EIGENSCHAFT_ORDNUNG_PARAMETER to "1",
            EIGENSCHAFT_STRENGE_PARAMETER to "nicht-streng",
            EIGENSCHAFT_KONTEXT_PARAMETER to "automatisch",
        ),
    )

    val AnalysisEigenschaft = KnotenVorlage(
        art = ANALYSIS_EIGENSCHAFT_KNOTEN_ART,
        name = "Analysis-Eigenschaftsstellen",
        kategorie = "Analysis: Funktionsgeometrie",
        beschreibung = "Bestimmt die Teilmenge der Argumentstellen, an denen eine lokale Analysis-Eigenschaft gilt.",
        standardGröße = GraphGröße(305f, 125f),
        anschlüsse = listOf(
            eingang("methode", MathematikAnschlussArten.Methode.id),
            ausgang("stellenmenge", MathematikAnschlussArten.Menge.id),
        ),
        standardParameter = mapOf(
            EIGENSCHAFT_PARAMETER to MathematischeEigenschaftRegister.Extremum.id,
            EIGENSCHAFT_GELTUNG_PARAMETER to "lokal",
            EIGENSCHAFT_STRENGE_PARAMETER to "nicht-streng",
            EIGENSCHAFT_KONTEXT_PARAMETER to "automatisch",
        ),
    )

    val FolgenEigenschaft = KnotenVorlage(
        art = FOLGEN_EIGENSCHAFT_KNOTEN_ART,
        name = "Folgenei­genschaft",
        kategorie = "Methoden: Folgen",
        beschreibung = "Prüft Indexierungsart und Wertart einer als Methode modellierten Folge.",
        standardGröße = GraphGröße(285f, 120f),
        anschlüsse = listOf(
            eingang("methode", MathematikAnschlussArten.Methode.id),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        ),
        standardParameter = mapOf(EIGENSCHAFT_PARAMETER to MathematischeEigenschaftRegister.EinseitigeFolge.id),
    )

    val MethodenStelligkeit = KnotenVorlage(
        art = METHODEN_STELLIGKEIT_KNOTEN_ART,
        name = "Methodenstelligkeit",
        kategorie = "Methoden: Signatur",
        beschreibung = "Prüft eine Methodensignatur auf Ein- oder Mehrstelligkeit; Argumentrollen bleiben stabil geordnet.",
        standardGröße = GraphGröße(275f, 120f),
        anschlüsse = listOf(
            eingang("methode", MathematikAnschlussArten.Methode.id),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        ),
        standardParameter = mapOf(
            EIGENSCHAFT_PARAMETER to MathematischeEigenschaftRegister.Einstellig.id,
            "argumentAnsicht" to ArgumentAnsicht.EinzelArgumente.name,
        ),
    )

    val MengenEigenschaft = KnotenVorlage(
        art = MENGEN_EIGENSCHAFT_KNOTEN_ART,
        name = "Mengeneigenschaft",
        kategorie = "Mengen: Eigenschaften",
        beschreibung = "Prüft Offenheit, Abgeschlossenheit oder Konvexität relativ zu einem expliziten Umgebungsraum.",
        standardGröße = GraphGröße(285f, 120f),
        anschlüsse = listOf(
            eingang("menge", MathematikAnschlussArten.Menge.id),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        ),
        standardParameter = mapOf(
            EIGENSCHAFT_PARAMETER to MathematischeEigenschaftRegister.Offen.id,
            EIGENSCHAFT_KONTEXT_PARAMETER to "R",
        ),
    )

    val alle = listOf(MethodenEigenschaft, AnalysisEigenschaft, FolgenEigenschaft, MethodenStelligkeit, MengenEigenschaft)
}

/** Gemeinsamer Kern für globale und lokale Konvexitätsfragen. */
object KonvexitaetsKern {
    fun globaleAussage(
        methode: Methode,
        definition: MathematischeEigenschaftDefinition,
        streng: Boolean,
    ): EigenschaftsAussage {
        val passend = methode.parameter.isNotEmpty() && methode.parameter.all { parameter ->
            methode.werteVorräte[parameter.name] in setOf(NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen)
        } && methode.zielMenge in setOf(NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen)
        val support = if (passend) UnterstuetzungsStatus.IMPLEMENTIERT else UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH
        return EigenschaftsAussage(
            eigenschaftId = definition.id,
            eigenschaftLatex = if (streng) "streng ${definition.adjektiv}" else definition.adjektiv,
            subjektLatex = methode.name,
            unterstuetzung = support,
            aussageStatus = AussageStatus.UNENTSCHEIDBAR,
            diagnose = EigenschaftsDiagnose(
                code = if (passend) "symbolische-konvexitaet" else "fehlende-geordnete-affine-struktur",
                nachricht = if (passend) {
                    "Jensen- und Hesse-Kriterium sind registriert; die konkrete Vorschrift ist im aktuellen CAS nicht vollständig entscheidbar."
                } else {
                    "Konvexität benötigt einen konvexen Definitionsbereich und eine geordnete reelle Zielstruktur."
                },
                voraussetzungen = listOf("konvexer Definitionsbereich", "geordnete reelle Zielstruktur"),
            ),
            kontextLatex = methode.parameter.joinToString(",") { parameter ->
                methode.werteVorräte[parameter.name]?.zuLatex() ?: "?"
            },
        )
    }

    fun lokaleMenge(
        methode: Methode,
        definition: MathematischeEigenschaftDefinition,
        streng: Boolean,
    ): MengenAusdruck = positionsMenge(methode, definition, streng)
}

internal fun MathematikAuswerterRegister.registriereMathematischeEigenschaften() {
    registriere(METHODEN_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        val methode = kontext.methode()
        val definition = kontext.definition(EigenschaftsSubjektArt.Methode)
        val aussage = when (definition.id) {
            MathematischeEigenschaftRegister.Konvex.id,
            MathematischeEigenschaftRegister.Konkav.id,
            -> KonvexitaetsKern.globaleAussage(
                methode = methode,
                definition = definition,
                streng = kontext.knoten.parameter[EIGENSCHAFT_STRENGE_PARAMETER] == "streng",
            )
            else -> methodenAussage(methode, definition, kontext.knoten.parameter)
        }
        kontext.aussageErgebnis(aussage)
    }

    registriere(ANALYSIS_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        val methode = kontext.methode()
        val definition = kontext.definition(EigenschaftsSubjektArt.Argumentstelle)
        val menge = when (definition.id) {
            MathematischeEigenschaftRegister.Konvexitaetsbereich.id,
            MathematischeEigenschaftRegister.Konkavitaetsbereich.id,
            -> KonvexitaetsKern.lokaleMenge(
                methode = methode,
                definition = definition,
                streng = kontext.knoten.parameter[EIGENSCHAFT_STRENGE_PARAMETER] == "streng",
            )
            else -> positionsMenge(
                methode = methode,
                definition = definition,
                streng = kontext.knoten.parameter[EIGENSCHAFT_STRENGE_PARAMETER] == "streng",
            )
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("stellenmenge" to BedingterWert(menge, kontext.gemeinsameAnnahmen())),
            eingänge = kontext.eingänge,
        )
    }

    registriere(FOLGEN_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        val methode = kontext.methode()
        val definition = kontext.definition(EigenschaftsSubjektArt.Folge)
        kontext.aussageErgebnis(folgenAussage(methode, definition))
    }

    registriere(METHODEN_STELLIGKEIT_KNOTEN_ART) { kontext ->
        val methode = kontext.methode()
        val definition = kontext.definition(EigenschaftsSubjektArt.Methodensignatur)
        val istEinstellig = methode.parameter.size == 1
        val erwartetEinstellig = definition.id == MathematischeEigenschaftRegister.Einstellig.id
        val wahr = istEinstellig == erwartetEinstellig
        kontext.aussageErgebnis(
            EigenschaftsAussage(
                eigenschaftId = definition.id,
                eigenschaftLatex = definition.adjektiv,
                subjektLatex = methode.name,
                unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
                aussageStatus = if (wahr) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT,
                diagnose = EigenschaftsDiagnose(
                    code = "stelligkeit-${methode.parameter.size}",
                    nachricht = "Die Signatur besitzt ${methode.parameter.size} stabile Argumentrollen.",
                ),
            ),
        )
    }

    registriere(MENGEN_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        val menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck ?: error("Die zu prüfende Menge fehlt.")
        val definition = kontext.definition(EigenschaftsSubjektArt.Menge)
        kontext.aussageErgebnis(mengenAussage(menge, definition))
    }
}

private fun methodenAussage(
    methode: Methode,
    definition: MathematischeEigenschaftDefinition,
    parameter: Map<String, String>,
): EigenschaftsAussage {
    val istReelleZahlmethode = methode.parameter.isNotEmpty() && methode.parameter.all {
        methode.werteVorräte[it.name] in setOf(NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen)
    } && methode.zielMenge in setOf(NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen)
    val support = when (definition.gruppe) {
        EigenschaftsGruppe.Regularität,
        EigenschaftsGruppe.Integrabilität,
        -> if (istReelleZahlmethode) UnterstuetzungsStatus.IMPLEMENTIERT else UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH
        else -> UnterstuetzungsStatus.IMPLEMENTIERT
    }
    val ordnung = parameter[EIGENSCHAFT_ORDNUNG_PARAMETER]?.toIntOrNull()?.coerceAtLeast(0) ?: 1
    return EigenschaftsAussage(
        eigenschaftId = definition.id,
        eigenschaftLatex = when (definition.id) {
            MathematischeEigenschaftRegister.Cn.id -> "C^{$ordnung}"
            MathematischeEigenschaftRegister.Dn.id -> "D^{$ordnung}"
            else -> definition.adjektiv
        },
        subjektLatex = methode.name,
        unterstuetzung = support,
        aussageStatus = AussageStatus.UNENTSCHEIDBAR,
        diagnose = EigenschaftsDiagnose(
            code = if (support == UnterstuetzungsStatus.IMPLEMENTIERT) "symbolische-eigenschaft" else "fehlender-analysis-kontext",
            nachricht = if (support == UnterstuetzungsStatus.IMPLEMENTIERT) {
                "Die Eigenschaft ist semantisch modelliert; ein vollständiger Beweis hängt von Vorschrift und Definitionsbereich ab."
            } else {
                "Diese Eigenschaft benötigt eine reellwertige Methode auf einem reellen Definitionsbereich."
            },
        ),
        kontextLatex = parameter[EIGENSCHAFT_KONTEXT_PARAMETER],
    )
}

private fun folgenAussage(
    methode: Methode,
    definition: MathematischeEigenschaftDefinition,
): EigenschaftsAussage {
    val art = FolgenArt.aus(methode)
    val wertDefinition = wertArt(methode)
    val wahr = when (definition.id) {
        MathematischeEigenschaftRegister.EinseitigeFolge.id -> art == FolgenArt.Einseitig
        MathematischeEigenschaftRegister.ZweiseitigeFolge.id -> art == FolgenArt.Zweiseitig
        else -> wertDefinition?.id == definition.id
    }
    val entscheidbar = definition.gruppe in setOf(EigenschaftsGruppe.Folge, EigenschaftsGruppe.Wertart)
    return EigenschaftsAussage(
        eigenschaftId = definition.id,
        eigenschaftLatex = definition.adjektiv,
        subjektLatex = methode.name,
        unterstuetzung = if (entscheidbar) UnterstuetzungsStatus.IMPLEMENTIERT else UnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT,
        aussageStatus = if (!entscheidbar) AussageStatus.UNENTSCHEIDBAR else if (wahr) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT,
        diagnose = EigenschaftsDiagnose(
            code = "folgenvertrag-${art.name.lowercase()}",
            nachricht = "Indexmenge: ${art.indexMenge.zuLatex()}, Zielmenge: ${methode.zielMenge.zuLatex()}.",
        ),
    )
}

private fun wertArt(methode: Methode): MathematischeEigenschaftDefinition? = when {
    methode.zielMenge in setOf(NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen) -> MathematischeEigenschaftRegister.Reellwertig
    methode.zielMenge == KomplexeZahlen -> MathematischeEigenschaftRegister.Komplexwertig
    methode.zielMenge is Vektorraum || methode.vorschrift is SpaltenVektor || methode.vorschrift is ZeilenVektor -> MathematischeEigenschaftRegister.Vektorwertig
    methode.zielMenge is Matrizenraum || methode.zielMenge is Tupelraum -> MathematischeEigenschaftRegister.Vektorwertig
    methode.vorschrift::class.simpleName?.contains("Polynom", ignoreCase = true) == true -> MathematischeEigenschaftRegister.Polynomwertig
    else -> null
}

private fun mengenAussage(
    menge: MengenAusdruck,
    definition: MathematischeEigenschaftDefinition,
): EigenschaftsAussage {
    val status = when (definition.id) {
        MathematischeEigenschaftRegister.Offen.id -> when (menge) {
            LeereMenge -> AussageStatus.BEWIESEN
            is ReellesIntervall -> if (menge.linksOffen && menge.rechtsOffen) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
            is EndlicheMenge -> if (menge.elemente.isEmpty()) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
            else -> AussageStatus.UNENTSCHEIDBAR
        }
        MathematischeEigenschaftRegister.Abgeschlossen.id -> when (menge) {
            LeereMenge -> AussageStatus.BEWIESEN
            is ReellesIntervall -> if (!menge.linksOffen && !menge.rechtsOffen) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
            is EndlicheMenge -> AussageStatus.BEWIESEN
            else -> AussageStatus.UNENTSCHEIDBAR
        }
        MathematischeEigenschaftRegister.KonvexeMenge.id -> when (menge) {
            LeereMenge, is ReellesIntervall -> AussageStatus.BEWIESEN
            is EndlicheMenge -> if (menge.elemente.size <= 1) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
            else -> AussageStatus.UNENTSCHEIDBAR
        }
        else -> AussageStatus.UNENTSCHEIDBAR
    }
    return EigenschaftsAussage(
        eigenschaftId = definition.id,
        eigenschaftLatex = definition.adjektiv,
        subjektLatex = menge.zuLatex(),
        unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
        aussageStatus = status,
        diagnose = EigenschaftsDiagnose(
            code = "mengenstruktur-${definition.id}",
            nachricht = if (status == AussageStatus.UNENTSCHEIDBAR) {
                "Die Mengenstruktur reicht ohne explizite Topologie oder affine Struktur nicht für einen Beweis."
            } else {
                "Die Eigenschaft folgt aus der kanonischen Mengenstruktur im Umgebungsraum R."
            },
            voraussetzungen = if (definition.benötigtUmgebungsraum) listOf("Umgebungsraum R") else emptyList(),
        ),
        kontextLatex = "\\mathbb{R}",
    )
}

private fun positionsMenge(
    methode: Methode,
    definition: MathematischeEigenschaftDefinition,
    streng: Boolean,
): MengenAusdruck {
    val variablen = methode.parameter.mapIndexed { index, parameter ->
        val variable = parameter as? Variable ?: Variable(parameter.name.ifBlank { "x_${index + 1}" })
        GebundeneMengenVariable(
            variable = variable,
            grundMenge = methode.werteVorräte[parameter.name] ?: ReelleZahlen,
        )
    }.ifEmpty {
        listOf(GebundeneMengenVariable(Variable("x"), ReelleZahlen))
    }
    val argumente = variablen.joinToString(",") { it.variable.zuLatex() }
    val subjekt = "${methode.name}\\left($argumente\\right)"
    val aussage = EigenschaftsAussage(
        eigenschaftId = definition.id,
        eigenschaftLatex = if (streng) "streng ${definition.adjektiv}" else definition.adjektiv,
        subjektLatex = subjekt,
        unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
        aussageStatus = AussageStatus.UNENTSCHEIDBAR,
        diagnose = EigenschaftsDiagnose(
            code = "symbolische-stellenmenge",
            nachricht = "Die Stellenmenge bleibt symbolisch, bis Ableitungs-, Jensen- oder Hesse-Kriterien einen exakten Nachweis liefern.",
        ),
    )
    return DefinierteMenge(variablen, aussage)
}

private fun KnotenAuswertungsKontext.methode(): Methode =
    eingänge["methode"]?.objekt as? Methode ?: error("Die zu prüfende Methode fehlt.")

private fun KnotenAuswertungsKontext.definition(erwartet: EigenschaftsSubjektArt): MathematischeEigenschaftDefinition {
    val id = knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty()
    val definition = MathematischeEigenschaftRegister.finde(id)
        ?: error("Unbekannte Eigenschaft '$id'.")
    require(definition.subjektArt == erwartet || erwartet == EigenschaftsSubjektArt.Folge && definition.gruppe == EigenschaftsGruppe.Wertart) {
        "Die Eigenschaft '${definition.titel}' passt nicht zum Knoten ${knoten.name}."
    }
    return definition
}

private fun KnotenAuswertungsKontext.aussageErgebnis(aussage: EigenschaftsAussage) = KnotenAuswertungsErgebnis(
    ausgaben = mapOf("aussage" to BedingterWert(aussage, gemeinsameAnnahmen())),
    eingänge = eingänge,
    warnungen = aussage.diagnose?.takeIf {
        aussage.unterstuetzung != UnterstuetzungsStatus.IMPLEMENTIERT || aussage.aussageStatus == AussageStatus.UNENTSCHEIDBAR
    }?.let { listOf(it.nachricht) }.orEmpty(),
)

private fun KnotenAuswertungsKontext.gemeinsameAnnahmen() = eingänge.values.flatMap { it.annahmen }.toSet()

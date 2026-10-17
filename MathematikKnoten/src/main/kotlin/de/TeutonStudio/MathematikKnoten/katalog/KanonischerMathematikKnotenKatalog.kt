package de.TeutonStudio.MathematikKnoten.katalog

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.MengenRechnerMigration
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.MathematikKartenAdapter.*

/**
 * Einzige fachliche Quelle für den sichtbaren mathematischen Erstellen-Katalog.
 *
 * Der lade-kompatible Basiskatalog darf historische Vorlagen enthalten. Diese
 * Fassade entscheidet zentral und plattformunabhängig, welche kanonischen
 * Varianten Android und Desktop tatsächlich zum Erzeugen anbieten.
 */
object KanonischerMathematikKnotenKatalog {
    private val historischeOrientierungsDuplikate = setOf(
        "mathematik.vektor",
        "mathematik.zeilenVektor",
        "mathematik.tupelZuSpalte",
        "mathematik.tupelZuZeile",
        "mathematik.einheitsSpalte",
        "mathematik.einheitsZeile",
        "mathematik.vektorRadiusSpalte",
        "mathematik.vektorRadiusZeile",
        "mathematik.kreuzproduktSpalte",
        "mathematik.kreuzproduktZeile",
        "mathematik.spaltenMethodeDifferentieren",
        "mathematik.zeilenMethodeDifferentieren",
        "mathematik.spaltenMethodeIntegrieren",
        "mathematik.zeilenMethodeIntegrieren",
        "mathematik.skalarprodukt",
        "mathematik.skalarproduktZeile",
    )

    private val historischeMengenEinzelknoten: Set<String> = MengenRechnerMigration.alteKnotenArten.keys

    fun alle(): List<KnotenVorlage> {
        val basis = (de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen() + MengenraumKnotenVorlagen.alle)
            .map(::kanonisierePraedikatVorlage)
            .map(::topologieFaehigeEigenschaftsVorlage)
            .map(::methodenfaehigeRechnerVorlage)
            .map(::joinVorlage)
        val bereinigt = basis.filterNot { vorlage ->
            vorlage.art in RechnerEinzeloperationMigration.zuordnungen ||
                vorlage.art == TENSORPRODUKT_ART ||
                vorlage.art in historischeOrientierungsDuplikate ||
                vorlage.art in historischeMengenEinzelknoten ||
                vorlage.art in PraedikatKnotenMigration.alteKnotenArten ||
                vorlage.art == MULTINOMVEKTOR_ART ||
                (
                    vorlage.art == ZAHLENRECHNER_ART &&
                        vorlage.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                        UniversellerZahlenOperator.KOMPLEXER_RADIUS.stabileId
                    )
        }

        return (
            bereinigt +
                VektorKonstruktorVorlagen.standard +
                VektorOrientierungsVorlagen.alle.flatMap { v ->
                    if (v.art in HISTORISCHE_METHODEN_ANALYSIS || v.art in setOf("mathematik.kreuzproduktSpalte","mathematik.tupelZuSpalte")) listOf(v, v.let { val k=konfiguriereOrientierungsKnoten(it.erzeuge(GraphPunkt.Zero),VEKTOR_ORIENTIERUNG_ZEILE); it.copy(anschlüsse=k.anschlüsse,standardParameter=k.parameter) }) else listOf(v)
                } +
                listOf(MathematikKnotenVorlagen.Kleiner, MathematikKnotenVorlagen.KleinerGleich, MathematikKnotenVorlagen.Größer, MathematikKnotenVorlagen.GrößerGleich) +
                MultinomVektorKnotenVorlagen.standard +
                MengenRechnerKnotenVorlagen.standard +
                PraedikatKnotenVorlagen.standard +
                MengenMassKnotenVorlagen.alle +
                RandKnotenVorlagen.alle +
                TangentialKnotenVorlagen.alle +
                TopologischeStrukturKnotenVorlagen.alle +
                ÜbersetzungsKnotenVorlagen.alle +
                SvgKnotenVorlagen.alle +
                GeometryNodesVorlagen.alle
            )
            .map(::kanonisiereMethodenUndRelationsVorlage)
            .map(::topologieFaehigeEigenschaftsVorlage)
            .map(::methodenfaehigeRechnerVorlage)
            .map(::joinVorlage)
            .distinctBy { vorlage ->
                if (vorlage.art in setOf("mathematik.methodenDifferentieren", "mathematik.methodenIntegrieren", MatrixRechner.KNOTEN_ART, VektorRechner.KNOTEN_ART, ÜBERSETZER_ART)) variantenKennung(vorlage)
                else vorlage.art to listOf(
                    vorlage.standardParameter["operator"].orEmpty(),
                    vorlage.standardParameter["eingabeModus"].orEmpty(),
                    vorlage.standardParameter[SVG_OPERATOR_PARAMETER].orEmpty(),
                    vorlage.name,
                ).joinToString("|")
            }
    }

    /** Bestehende diskrete Erzeugungsvarianten für anschlussbezogene Auswahl.
     * Freie Werte und Dimensionen bleiben unverändert; historische Knotentypen
     * werden nicht erneut als erzeugbare Varianten angeboten.
     */
    fun anschlussVarianten(basis: List<KnotenVorlage> = alle()): List<KnotenVorlage> {
        val operatoren = OperatorKnotenSuchindex.alleMitMetadaten(basis).map { it.vorlage }
        val vorlagen = (basis + operatoren).flatMap { v ->
            if (v.art != ÜBERSETZER_ART) listOf(v) else ÜBERSETZER_ZIELE.map { ziel ->
                val k = konfiguriereÜbersetzer(v.erzeuge(GraphPunkt.Zero), ziel)
                v.copy(name = "Übersetzer · ${übersetzerZielTitel(ziel)}", anschlüsse = k.anschlüsse,
                    standardParameter = k.parameter, standardEigenschaften = k.eigenschaften)
            }
        }
        return vorlagen.flatMap { v ->
            val k = v.erzeuge(GraphPunkt.Zero)
            val varianten: List<Pair<String, KnotenDaten>> = when (v.art) {
                ENDLICHE_MENGE_ART -> listOf(ENDLICHE_MENGE_LOKAL,ENDLICHE_MENGE_GRAPH).map {it to konfiguriereEndlicheMenge(k,it)}
                "mathematik.tupel" -> listOf(TUPEL_EINZEL_EINGABEN, TUPEL_METHODE).map { it to konfiguriereTupel(k, it) }
                "mathematik.matrix" -> listOf(MATRIX_EINZEL_EINGABEN, MATRIX_ZEILEN, MATRIX_SPALTEN, MATRIX_METHODE).map {
                    it to konfiguriereMatrix(k, it, matrixKonfiguration(k).höhe, matrixKonfiguration(k).breite)
                }
                VEKTOR_KONSTRUKTOR_ART -> listOf(VEKTOR_EINZEL_EINGABEN, VEKTOR_METHODE).flatMap { art ->
                    listOf(VEKTOR_ORIENTIERUNG_SPALTE, VEKTOR_ORIENTIERUNG_ZEILE).map { orient ->
                        "$art · $orient" to konfiguriereVektorKonstruktor(k, art, orient)
                    }
                }
                "mathematik.variable" -> listOf("zahl", "tupel", "winkeltupel", "zeile", "spalte").map { it to konfiguriereVariablenStruktur(k, it) }
                TUPEL_ERGÄNZEN_ART -> TupelErgänzenModus.entries.map { it.name to konfiguriereTupelErgänzen(k, it) }
                MatrixRechner.KNOTEN_ART -> when {
                    istMatrixDiagonalLeser(k) -> VektorStrukturAusgabe.entries.map { it.titel to konfiguriereMatrixRechnerDiagonalAusgabe(k,it.stabileId) }
                    k.parameter[RECHNER_OPERATOR_PARAMETER]==MatrixRechnerOperator.DIAGONALMATRIX_ERZEUGEN.stabileId -> MatrixDiagonalArt.entries.map {it.parameterWert to konfiguriereMatrixRechnerDiagonalArt(k,it.parameterWert)}
                    else -> listOf("" to k)
                }
                VektorRechner.KNOTEN_ART -> if(k.parameter[VEKTOR_RECHNER_OPERATOR]==VektorRechnerOperator.KREUZPRODUKT.stabileId)
                    listOf(null,VEKTOR_ORIENTIERUNG_SPALTE,VEKTOR_ORIENTIERUNG_ZEILE).map { (it ?: "Vom ersten Vektor") to konfiguriereKreuzproduktRechnerOrientierung(k,it) }
                    else listOf("" to k)
                MULTINOMVEKTOR_ART -> listOf(MULTINOM_AUSGABE_VEKTOR, MULTINOM_AUSGABE_TUPEL).flatMap { form ->
                    listOf(VEKTOR_ORIENTIERUNG_SPALTE, VEKTOR_ORIENTIERUNG_ZEILE).map { orient ->
                        "$form · $orient" to konfiguriereMultinomVektor(k, form, orient)
                    }
                }
                MENGENKONSTRUKTOR_ART -> MathematikAnschlussArten.alle.flatMap { art ->
                    val struktur = art.id in setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.SpaltenVektor.id,
                        MathematikAnschlussArten.ZeilenVektor.id, MathematikAnschlussArten.Matrix.id, MathematikAnschlussArten.Tensor.id)
                    val modi = if (struktur) listOf(STRUKTURFORM_UNBEKANNT, STRUKTURFORM_EINGANG) else listOf(STRUKTURFORM_UNBEKANNT)
                    modi.flatMap { modus ->
                        val eingaben = if (modus == STRUKTURFORM_EINGANG) listOf(STRUKTURFORM_EINZELN, STRUKTURFORM_TUPEL) else listOf(STRUKTURFORM_EINZELN)
                        eingaben.map { eingabe -> "${art.name} · $modus · $eingabe" to konfiguriereMengenkonstruktor(k, art.id, modus, eingabe) }
                    }
                }
                METHODEN_GRAPH_KNOTEN_ART -> listOf(false, true).map { (if (it) "Term" else "Methode") to konfiguriereGraphEingang(k, it) }
                MENGEN_EIGENSCHAFT_KNOTEN_ART -> MathematischeEigenschaftRegister.alle.filter { it.subjektArt == EigenschaftsSubjektArt.Menge }
                    .map { it.titel to konfiguriereMengenEigenschaftKnoten(k, it.id) }
                METHODEN_EIGENSCHAFT_KNOTEN_ART -> MathematischeEigenschaftRegister.alle.filter {
                    it.subjektArt == EigenschaftsSubjektArt.Methode && it.gruppe != EigenschaftsGruppe.Funktionsgeometrie
                }.map { it.titel to konfiguriereMethodenEigenschaftKnoten(k, it.id) }
                ZAHLENRECHNER_ART -> {
                    val op = UniversellerZahlenOperator.entries.firstOrNull { it.stabileId == k.parameter[ZAHLENRECHNER_OPERATOR] }
                    val grund = if (k.parameter[ZAHLENRECHNER_OPERATOR] == "zahl.differential")
                        ZahlenRechnerDifferentialErgebnisArt.entries.map { it.titel to konfiguriereZahlenRechnerDifferential(k, it) }
                    else if (op != null && istKomplexKonstruktor(op)) listOf(ZAHLENRECHNER_KOMPLEX_SEPARIERT, ZAHLENRECHNER_KOMPLEX_TUPEL)
                        .map { it to konfiguriereZahlenRechner(k, op, it) }
                    else if (op == UniversellerZahlenOperator.SINUS) listOf(false, true).map { (if (it) "Grad" else "Bogenmaß") to konfiguriereSinusWinkelraum(k, it) }
                    else if (op == UniversellerZahlenOperator.DIVISION) DivisionsSeite.entries.map { it.name to konfiguriereDivisionsSeite(k, it) }
                    else listOf("" to k)
                    grund
                }
                INTEGRAL_KNOTEN_ART -> IntegralAusgabeform.entries.map { it.name to konfiguriereIntegralKnoten(k, it) }
                TANGENTIAL_KNOTEN_ART -> TangentialAusgabeForm.entries.map { it.name to konfiguriereTangentialKnoten(k, it) }
                MathematikKnotenVorlagen.ORDNUNGSRELATION_ART -> listOf("kleiner", "kleinerGleich", "größer", "größerGleich").map { it to konfiguriereOrdnungsrelation(k, it) }
                ÜBERSETZER_ART -> when (val ziel = übersetzerZiel(k)) {
                    "polar", "kartesisch" -> listOf(false, true).flatMap { getrennt -> PolarKonvention.entries.flatMap { konvention ->
                        (if (ziel == "kartesisch") listOf(false, true) else listOf(false)).map { r1 ->
                            val polar = konfiguriereÜbersetzerPolarAnschlüsse(k.copy(parameter = k.parameter +
                                (ÜBERSETZER_POLAR_KONVENTION to konvention.name)), getrennt)
                            "${if (getrennt) "Komponenten" else "Tupel"} · ${konvention.name}${if (r1) " · R¹" else ""}" to
                                konfiguriereÜbersetzerEindimensional(polar, r1)
                        }
                    } }
                    "bogenmass" -> WinkelVertreter.entries.map { it.id to konfiguriereÜbersetzerVertreter(k, it) }
                    "matrixzerlegen" -> MatrixZerlegeRichtung.entries.flatMap { richtung -> listOf("vektor", "tupel").map { ausgabe ->
                        "${richtung.name} · $ausgabe" to konfiguriereÜbersetzer(k.copy(parameter = k.parameter +
                            mapOf(MATRIX_ZERLEGEN_RICHTUNG to richtung.name, "matrixZerlegen.ausgabe" to ausgabe)), ziel)
                    } }
                    "koordinatenmenge" -> listOf("keiner", "punkt", "mittelpunkt").map {
                        it to konfiguriereÜbersetzer(k.copy(parameter = k.parameter + ("geometrie.anker" to it)), ziel)
                    }
                    else -> listOf("" to k)
                }
                DIFFERENTIAL_KNOTEN_ART -> DifferentialAusgabeForm.entries.map { it.name to konfiguriereDifferentialKnoten(k, it) }
                TensorRechner.KNOTEN_ART -> AchsenEingabeModus.entries.map { it.name to konfiguriereTensorOperation(k, aktuelleTensorOperationDefinition(k), it) }
                else -> if (v.art in setOf("mathematik.tupelZuSpalte", "mathematik.einheitsSpalte", "mathematik.kreuzproduktSpalte",
                    "mathematik.spaltenMethodeDifferentieren", "mathematik.spaltenMethodeIntegrieren"))
                    listOf(VEKTOR_ORIENTIERUNG_SPALTE, VEKTOR_ORIENTIERUNG_ZEILE).map { it to konfiguriereOrientierungsKnoten(k, it) }
                else listOf("" to k)
            }
            varianten.map { (titel, neu) -> v.copy(
                name = if (titel.isEmpty()) v.name else "${v.name} · $titel",
                standardParameter = neu.parameter, standardEigenschaften = neu.eigenschaften,
                anschlüsse = neu.anschlüsse, standardGröße = neu.größe,
            ) }
        }.map(::methodenfaehigeRechnerVorlage).map(::joinVorlage).distinctBy(::variantenKennung)
    }

    /** Vollständiger Vertrag ohne zufällige Instanz-IDs oder Anzeigenamen. */
    fun variantenKennung(v: KnotenVorlage): String = listOf(v.art, (v.standardParameter - TENSOR_ACHSEN_IDS).toSortedMap(),
        v.standardEigenschaften.toSortedMap(), v.kartenVerweis,
        v.anschlüsse.map { it.copy(id = AnschlussId("vorlage")) }).joinToString("|")

    internal fun kanonisiereMethodenUndRelationsVorlage(v: KnotenVorlage): KnotenVorlage {
        if (v.art in mengenKonsolidierungAltarten) {
            val alt=KnotenDaten(art=v.art,name=v.name,anschlüsse=v.anschlüsse,parameter=v.standardParameter,eigenschaften=v.standardEigenschaften)
            val neu=KartenDaten(name="Vorlage",knoten=listOf(alt)).migriereMengenKonsolidierung().knoten.single()
            check(RECHNER_MIGRATION_DIAGNOSE !in neu.parameter) { "Ungültige Mengenvorlage ${v.name}: ${neu.parameter[RECHNER_MIGRATION_DIAGNOSE]}" }
            return v.copy(art=neu.art,anschlüsse=neu.anschlüsse,standardParameter=neu.parameter,standardEigenschaften=neu.eigenschaften)
        }
        if (v.art in übersetzungsKonsolidierungAltarten) {
            val alt=KnotenDaten(art=v.art,name=v.name,anschlüsse=v.anschlüsse,parameter=v.standardParameter,eigenschaften=v.standardEigenschaften)
            val neu=KartenDaten(name="Vorlage",knoten=listOf(alt)).migriereÜbersetzungsKonsolidierung().knoten.single()
            check(RECHNER_MIGRATION_DIAGNOSE !in neu.parameter) { "Ungültige Übersetzervorlage ${v.name}: ${neu.parameter[RECHNER_MIGRATION_DIAGNOSE]}" }
            return v.copy(art=neu.art,anschlüsse=neu.anschlüsse,standardParameter=neu.parameter,standardEigenschaften=neu.eigenschaften).alsAdapterVorlage()
        }
        if (v.art in strukturKonsolidierungAltarten) {
            val alt=KnotenDaten(art=v.art,name=v.name,anschlüsse=v.anschlüsse,parameter=v.standardParameter,eigenschaften=v.standardEigenschaften)
            val neu=KartenDaten(name="Vorlage",knoten=listOf(alt)).migriereStrukturKonsolidierung().knoten.single()
            check(RECHNER_MIGRATION_DIAGNOSE !in neu.parameter) { "Ungültige kanonische Vorlage ${v.name}: ${neu.parameter[RECHNER_MIGRATION_DIAGNOSE]}" }
            return v.copy(art=neu.art,anschlüsse=neu.anschlüsse,standardParameter=neu.parameter,standardEigenschaften=neu.eigenschaften)
        }
        if (v.art != METHODEN_ANWENDUNG_ART && v.art !in HISTORISCHE_METHODEN_ANALYSIS && v.art != "mathematik.ordnungsrelation") return v
        val alt = KnotenDaten(art=v.art, name=v.name, anschlüsse=v.anschlüsse, parameter=v.standardParameter, eigenschaften=v.standardEigenschaften)
        val neu = KartenDaten(name="Vorlage",knoten=listOf(alt)).migriereMethodenKonsolidierung().migrierePraedikatKnoten().knoten.single()
        check(RECHNER_MIGRATION_DIAGNOSE !in neu.parameter) { "Ungültige kanonische Vorlage ${v.name}" }
        val parameter = if (v.art in HISTORISCHE_METHODEN_ANALYSIS) neu.parameter +
            (methodenErgebnisArtSchlüssel("methode") to if (neu.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] == VEKTOR_ORIENTIERUNG_ZEILE) "mathematik.vektor.zeile" else "mathematik.vektor.spalte") else neu.parameter
        val name = if (v.art in HISTORISCHE_METHODEN_ANALYSIS) {
            if (neu.art == "mathematik.methodenDifferentieren") "Vektormethode differentieren" else "Vektormethode integrieren"
        } else v.name
        return v.copy(art=neu.art, name=name, anschlüsse=neu.anschlüsse, standardParameter=parameter, standardEigenschaften=neu.eigenschaften)
    }

    private fun kanonisierePraedikatVorlage(vorlage: KnotenVorlage): KnotenVorlage =
        if (
            vorlage.art == "mathematik.termZuMethode" &&
            vorlage.standardParameter["name"] == "P" &&
            vorlage.name == "Aussage zu Methode"
        ) {
            vorlage.copy(
                name = "Aussage zu Prädikat",
                beschreibung = "Erzeugt aus einer Aussage ein typisiertes Prädikat.",
            )
        } else vorlage
}

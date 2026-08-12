package de.TeutonStudio.MathematikAtlas

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.alleMathematikDefinitionsVorlagen
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensVerfügbarkeit
import de.TeutonStudio.MathematikKnoten.konzeptknoten.KonzeptKnotenRegister

internal enum class KnotenWählerModus(
    val stabileId: String,
    val anzeigeName: String,
    val beschreibung: String,
) {
    Standard(
        stabileId = "standard",
        anzeigeName = "Standard",
        beschreibung = "Technische Knotenliste mit den bisherigen Kategorien.",
    ),
    Konzeptbibliothek(
        stabileId = "concept-library",
        anzeigeName = "Konzeptbibliothek",
        beschreibung = "Fachlich geordnete Knoten mit Vorschau, Definition und Drag-and-drop.",
    ),
    ;

    companion object {
        fun aus(stabileId: String?): KnotenWählerModus =
            entries.firstOrNull { it.stabileId == stabileId } ?: Standard
    }
}

internal class KnotenWählerModusSpeicher(context: Context) {
    private val einstellungen = context.getSharedPreferences("knotenwaehler", Context.MODE_PRIVATE)

    fun lade(): KnotenWählerModus =
        KnotenWählerModus.aus(einstellungen.getString(SCHLÜSSEL, null))

    fun speichere(modus: KnotenWählerModus) {
        einstellungen.edit().putString(SCHLÜSSEL, modus.stabileId).apply()
    }

    private companion object {
        const val SCHLÜSSEL = "modus"
    }
}

internal data class KonzeptKategorie(
    val id: String,
    val bezeichnung: String,
    val kinder: List<KonzeptKategorie> = emptyList(),
)

internal enum class KonzeptVerfügbarkeit { Verfügbar, Geplant }

internal data class KonzeptBibliothekEintrag(
    val id: String,
    val titel: String,
    val beschreibung: String,
    val kategoriePfade: List<List<String>>,
    val suchbegriffe: Set<String>,
    val verfügbarkeit: KonzeptVerfügbarkeit,
    val vorlage: KnotenVorlage? = null,
) {
    val istEinfügbar: Boolean
        get() = verfügbarkeit == KonzeptVerfügbarkeit.Verfügbar && vorlage != null
}

internal data class KonzeptBibliothekFilter(
    val suchtext: String = "",
    val erforderlicherEingang: AnschlussArtId? = null,
    val erforderlicherAusgang: AnschlussArtId? = null,
    val kategoriePfad: List<String>? = null,
)

/** App-Adapter auf die kanonische Enzyklopädiequelle im Modul MathematikKnoten. */
internal object KonzeptBibliothekRegister {
    val kategorien: List<KonzeptKategorie> = listOf(
        KonzeptKategorie(
            "analysis",
            "Analysis",
            listOf(
                KonzeptKategorie("funktionen", "Funktionen"),
                KonzeptKategorie("folgen-reihen", "Folgen und Reihen"),
                KonzeptKategorie("differential-integral", "Differential- und Integralrechnung"),
                KonzeptKategorie(
                    "eigenschaften",
                    "Eigenschaften",
                    listOf(
                        KonzeptKategorie("regularitaet", "Regularität"),
                        KonzeptKategorie("integrabilitaet", "Integrabilität"),
                        KonzeptKategorie("funktionsgeometrie", "Funktionsgeometrie"),
                    ),
                ),
            ),
        ),
        KonzeptKategorie(
            "methoden",
            "Methoden",
            listOf(
                KonzeptKategorie("signatur", "Signatur"),
                KonzeptKategorie("folgen", "Folgen"),
                KonzeptKategorie("wertarten", "Wertarten"),
            ),
        ),
        KonzeptKategorie(
            "lineare-algebra",
            "Lineare Algebra",
            listOf(
                KonzeptKategorie("vektoren", "Vektoren"),
                KonzeptKategorie("matrizen", "Matrizen"),
                KonzeptKategorie("tensoren", "Tensoren"),
                KonzeptKategorie("skalarprodukte", "Skalarprodukte"),
            ),
        ),
        KonzeptKategorie(
            "geometrie",
            "Geometrie",
            listOf(
                KonzeptKategorie("grundobjekte", "Grundobjekte"),
                KonzeptKategorie("konstruktionen", "Konstruktionen"),
                KonzeptKategorie("transformationen", "Transformationen"),
                KonzeptKategorie("visualisierung", "Darstellung"),
            ),
        ),
        KonzeptKategorie(
            "mengenlehre",
            "Mengenlehre",
            listOf(
                KonzeptKategorie("mengen", "Mengen"),
                KonzeptKategorie("mengenoperationen", "Mengenoperationen"),
                KonzeptKategorie("mengendefinitionen", "Mengendefinitionen"),
                KonzeptKategorie("konstruktionen", "Konstruktionen"),
                KonzeptKategorie("axiome", "Axiome"),
                KonzeptKategorie(
                    "eigenschaften",
                    "Eigenschaften",
                    listOf(
                        KonzeptKategorie("kardinalitaet", "Kardinalität"),
                        KonzeptKategorie("topologie", "Topologie"),
                        KonzeptKategorie("konvexitaet", "Konvexität"),
                    ),
                ),
                KonzeptKategorie(
                    "topologie",
                    "Topologie",
                    listOf(KonzeptKategorie("raeume", "Räume")),
                ),
            ),
        ),
        KonzeptKategorie(
            "logik",
            "Logik",
            listOf(
                KonzeptKategorie("aussagen", "Aussagen"),
                KonzeptKategorie(
                    "praedikate",
                    "Prädikate",
                    listOf(KonzeptKategorie("axiome", "Axiome")),
                ),
                KonzeptKategorie("quantoren", "Quantoren"),
            ),
        ),
        KonzeptKategorie(
            "arithmetik",
            "Arithmetik",
            listOf(KonzeptKategorie("natuerliche-zahlen", "Natürliche Zahlen")),
        ),
        KonzeptKategorie(
            "algebra",
            "Algebra",
            listOf(
                KonzeptKategorie("zahlen", "Zahlen"),
                KonzeptKategorie("operationen", "Operationen"),
                KonzeptKategorie("methoden", "Methoden und Abbildungen"),
                KonzeptKategorie(
                    "strukturen",
                    "Strukturen",
                    listOf(
                        KonzeptKategorie("gruppen", "Gruppen"),
                        KonzeptKategorie("ringe-koerper", "Ringe und Körper"),
                    ),
                ),
            ),
        ),
        KonzeptKategorie(
            "topologie",
            "Topologie",
            listOf(
                KonzeptKategorie("grundbegriffe", "Grundbegriffe"),
                KonzeptKategorie("abbildungen", "Abbildungen"),
            ),
        ),
        KonzeptKategorie("stochastik", "Stochastik", listOf(KonzeptKategorie("grundbegriffe", "Grundbegriffe"))),
        KonzeptKategorie("eigene-karten", "Eigene Karten"),
    )

    private val kategorienNachPfad: Map<List<String>, KonzeptKategorie> = buildMap {
        fun erfasse(kategorie: KonzeptKategorie, eltern: List<String>) {
            val pfad = eltern + kategorie.id
            put(pfad, kategorie)
            kategorie.kinder.forEach { kind -> erfasse(kind, pfad) }
        }
        kategorien.forEach { haupt -> erfasse(haupt, emptyList()) }
    }

    fun erstelle(vorlagen: List<KnotenVorlage>): List<KonzeptBibliothekEintrag> {
        val eindeutigeVorlagen = vorlagen.distinctBy(KnotenVorlage::bibliotheksId)
        val angeforderteVorlagenIds = eindeutigeVorlagen
            .mapTo(linkedSetOf(), KnotenVorlage::bibliotheksId)
        val vollständigeKanonischeVorlagen = alleMathematikDefinitionsVorlagen()

        val kanonischeEinträge = KonzeptKnotenRegister.erstelle(vollständigeKanonischeVorlagen).flatMap { wissen ->
            val kategoriePfade = wissen.fachPfade
                .map { it.segmente }
                .sortedBy { it.joinToString("/") }
            if (wissen.knotenVorlagen.isEmpty()) {
                listOf(
                    KonzeptBibliothekEintrag(
                        id = wissen.id.wert,
                        titel = wissen.titel,
                        beschreibung = wissen.kurzbeschreibung,
                        kategoriePfade = kategoriePfade,
                        suchbegriffe = wissen.alleSuchtexte,
                        verfügbarkeit = KonzeptVerfügbarkeit.Geplant,
                    ),
                )
            } else {
                wissen.knotenVorlagen
                    .filter { vorlage -> vorlage.bibliotheksId() in angeforderteVorlagenIds }
                    .map { vorlage ->
                        KonzeptBibliothekEintrag(
                            id = vorlage.bibliotheksId(),
                            titel = vorlage.name,
                            beschreibung = vorlage.beschreibung,
                            kategoriePfade = kategoriePfade,
                            suchbegriffe = wissen.alleSuchtexte + setOf(
                                vorlage.name,
                                vorlage.beschreibung,
                                vorlage.kategorie,
                                vorlage.art,
                            ) + vorlage.standardParameter.keys + vorlage.standardParameter.values,
                            verfügbarkeit = when (wissen.verfügbarkeit) {
                                WissensVerfügbarkeit.Verfügbar -> KonzeptVerfügbarkeit.Verfügbar
                                WissensVerfügbarkeit.Geplant,
                                WissensVerfügbarkeit.Historisch,
                                -> KonzeptVerfügbarkeit.Geplant
                            },
                            vorlage = vorlage,
                        )
                    }
            }
        }
        val abgedeckteVorlagenIds = kanonischeEinträge
            .mapNotNull { eintrag -> eintrag.vorlage?.bibliotheksId() }
            .toSet()
        val ergänzendeEinträge = eindeutigeVorlagen.asSequence()
            .filterNot { vorlage -> vorlage.bibliotheksId() in abgedeckteVorlagenIds }
            .map(::ergänzenderEintrag)
            .toList()

        return (kanonischeEinträge + ergänzendeEinträge)
            .distinctBy(KonzeptBibliothekEintrag::id)
            .sortedWith(compareBy(KonzeptBibliothekEintrag::titel, KonzeptBibliothekEintrag::id))
    }

    fun bezeichnungFür(pfad: List<String>): String = pfad.indices.mapNotNull { index ->
        kategorienNachPfad[pfad.take(index + 1)]?.bezeichnung
    }.joinToString(" / ")

    fun unterkategorien(hauptkategorie: String): List<KonzeptKategorie> =
        kategorien.firstOrNull { it.id == hauptkategorie }?.kinder.orEmpty()

    fun validierungsFehler(einträge: List<KonzeptBibliothekEintrag>): List<String> = buildList {
        einträge.groupingBy(KonzeptBibliothekEintrag::id).eachCount().filterValues { it > 1 }.keys.forEach {
            add("Doppelte Konzept-ID: $it")
        }
        einträge.forEach { eintrag ->
            if (eintrag.kategoriePfade.isEmpty()) add("${eintrag.id}: kein Kategoriepfad")
            eintrag.kategoriePfade.forEach { pfad ->
                if (pfad.isEmpty() || pfad.size > 3) {
                    add("${eintrag.id}: ungültige Hierarchietiefe ${pfad.size}")
                } else {
                    val unbekannteEbene = pfad.indices.firstOrNull { index ->
                        kategorienNachPfad[pfad.take(index + 1)] == null
                    }
                    if (unbekannteEbene != null) {
                        add(
                            "${eintrag.id}: unbekannter Kategoriepfad " +
                                pfad.take(unbekannteEbene + 1).joinToString("/"),
                        )
                    }
                }
            }
            if (eintrag.verfügbarkeit == KonzeptVerfügbarkeit.Verfügbar && eintrag.vorlage == null) {
                add("${eintrag.id}: verfügbarer Eintrag ohne Knotenvorlage")
            }
            if (eintrag.verfügbarkeit == KonzeptVerfügbarkeit.Geplant && eintrag.vorlage != null) {
                add("${eintrag.id}: geplanter Eintrag besitzt irrtümlich eine Knotenvorlage")
            }
        }
    }

    private fun ergänzenderEintrag(vorlage: KnotenVorlage): KonzeptBibliothekEintrag =
        KonzeptBibliothekEintrag(
            id = vorlage.bibliotheksId(),
            titel = vorlage.name,
            beschreibung = vorlage.beschreibung,
            kategoriePfade = kategoriePfadeFürErweiterung(vorlage),
            suchbegriffe = buildSet {
                add(vorlage.art)
                add(vorlage.name)
                add(vorlage.kategorie)
                add(vorlage.beschreibung)
                addAll(vorlage.standardParameter.keys)
                addAll(vorlage.standardParameter.values)
                addAll(vorlage.anschlüsse.map { it.name })
                addAll(vorlage.anschlüsse.map { it.art.wert })
            },
            verfügbarkeit = KonzeptVerfügbarkeit.Verfügbar,
            vorlage = vorlage,
        )

    private fun kategoriePfadeFürErweiterung(vorlage: KnotenVorlage): List<List<String>> {
        val art = vorlage.art.lowercase()
        val name = vorlage.name.lowercase()
        val kategorie = vorlage.kategorie.lowercase()
        val eigenschaft = vorlage.standardParameter["eigenschaft"]?.lowercase().orEmpty()
        val pfade = linkedSetOf<List<String>>()

        if (kategorie in setOf("eigene karten", "gespeicherte karten", "gruppen") || vorlage.kartenVerweis != null) {
            pfade += listOf("eigene-karten")
        }

        if (art in setOf("mathematik.topologischerraum", "mathematik.metrischerraum")) {
            pfade += listOf("mengenlehre", "topologie", "raeume")
            pfade += listOf("topologie", "grundbegriffe")
        }
        if (art.contains("mengeneigenschaft")) {
            when (eigenschaft) {
                "endlich", "unendlich", "abzählbar", "abzaehlbar", "überabzählbar", "ueberabzaehlbar", "uberabzahlbar" ->
                    pfade += listOf("mengenlehre", "eigenschaften", "kardinalitaet")
                "offen", "abgeschlossen", "geschlossen" ->
                    pfade += listOf("mengenlehre", "eigenschaften", "topologie")
                "konvexe-menge" -> pfade += listOf("mengenlehre", "eigenschaften", "konvexitaet")
            }
        }
        if (art.contains("methodeneigenschaft") && eigenschaft == "stetig") {
            pfade += listOf("analysis", "eigenschaften", "regularitaet")
            pfade += listOf("topologie", "abbildungen")
        }

        if (kategorie.startsWith("geometrie:") || art.contains("geometrie")) {
            val unterkategorie = when {
                kategorie.contains("transformation") || art.contains("transformation") -> "transformationen"
                kategorie.contains("darstellung") || art.contains("visualisierung") -> "visualisierung"
                kategorie.contains("konstruktion") -> "konstruktionen"
                else -> "grundobjekte"
            }
            pfade += listOf("geometrie", unterkategorie)
        }

        if (kategorie == "vektoren" || art.contains("vektor")) pfade += listOf("lineare-algebra", "vektoren")
        if (kategorie == "matrizen" || art.contains("matrix") || art.contains("spur")) pfade += listOf("lineare-algebra", "matrizen")
        if (art.contains("tensor")) pfade += listOf("lineare-algebra", "tensoren")
        if (art.contains("skalarprodukt") || name.contains("skalarprodukt")) {
            pfade += listOf("lineare-algebra", "skalarprodukte")
            pfade += listOf("geometrie", "grundobjekte")
        }

        if (kategorie == "mengen" || kategorie.contains("mengen") || art.contains("menge")) {
            val unterkategorie = when {
                art.contains("konstruktor") || art.contains("definator") -> "mengendefinitionen"
                kategorie.contains("rechnung") || art.contains("schnitt") || art.contains("vereinigung") ||
                    art.contains("differenz") || art.contains("produkt") -> "mengenoperationen"
                else -> "mengen"
            }
            pfade += listOf("mengenlehre", unterkategorie)
        }

        if (kategorie.startsWith("aussagen") || kategorie == "aussage" || art.contains("aussage") ||
            art.contains("praedikat") || art.contains("prädikat") || art.contains("quantor") ||
            art.contains("gleichheit") || art.contains("ordnung")
        ) {
            val unterkategorie = when {
                art.contains("quantor") -> "quantoren"
                art.contains("praedikat") || art.contains("prädikat") || art.contains("gleichheit") || art.contains("ordnung") -> "praedikate"
                else -> "aussagen"
            }
            pfade += listOf("logik", unterkategorie)
        }

        if (kategorie == "analysis" || art.contains("ableit") || art.contains("integr") ||
            art.contains("grenz") || art.contains("folge") || art.contains("reihe")
        ) {
            val unterkategorie = when {
                art.contains("folge") || art.contains("reihe") || art.contains("grenz") -> "folgen-reihen"
                art.contains("ableit") || art.contains("integr") -> "differential-integral"
                else -> "funktionen"
            }
            pfade += listOf("analysis", unterkategorie)
        }

        if (kategorie in setOf("methoden", "abbildungen") || art.contains("methode") || art.contains("abbild")) {
            pfade += listOf("algebra", "methoden")
            pfade += listOf("analysis", "funktionen")
        }

        if (kategorie in setOf("rechnen", "algebra", "zahlen", "operatoren", "steuerung") ||
            art.contains("zahl") || art.contains("rechner")
        ) {
            val unterkategorie = if (art.contains("zahl") && !art.contains("rechner")) "zahlen" else "operationen"
            pfade += listOf("algebra", unterkategorie)
        }

        if (pfade.isEmpty()) pfade += listOf("algebra", "operationen")
        return pfade.toList()
    }
}

private fun KnotenVorlage.bibliotheksId(): String {
    val variantenSchlüssel = standardParameter.toSortedMap()
        .entries.joinToString(";") { (schlüssel, wert) -> "$schlüssel=$wert" }
    return listOf(art, name, variantenSchlüssel).joinToString("|")
}

internal fun KonzeptBibliothekEintrag.passt(filter: KonzeptBibliothekFilter): Boolean {
    val suchtext = filter.suchtext.trim().lowercase()
    val suchePasst = suchtext.isBlank() || buildSet {
        add(titel)
        add(beschreibung)
        addAll(suchbegriffe)
        kategoriePfade.forEach { add(KonzeptBibliothekRegister.bezeichnungFür(it)) }
    }.any { it.lowercase().contains(suchtext) }
    if (!suchePasst) return false

    val kategoriePasst = filter.kategoriePfad?.let { gewählt ->
        kategoriePfade.any { pfad ->
            pfad == gewählt || (gewählt.size == 1 && pfad.firstOrNull() == gewählt.first())
        }
    } ?: true
    if (!kategoriePasst) return false

    val eingangPasst = filter.erforderlicherEingang?.let { art ->
        vorlage?.anschlüsse?.any { it.richtung == AnschlussRichtung.Eingang && it.art == art } == true
    } ?: true
    if (!eingangPasst) return false

    return filter.erforderlicherAusgang?.let { art ->
        vorlage?.anschlüsse?.any { it.richtung == AnschlussRichtung.Ausgang && it.art == art } == true
    } ?: true
}

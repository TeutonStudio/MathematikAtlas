package de.TeutonStudio.MathematikAtlas

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.*

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

internal object KonzeptBibliothekRegister {
    val kategorien: List<KonzeptKategorie> = listOf(
        KonzeptKategorie(
            "analysis",
            "Analysis",
            listOf(
                KonzeptKategorie("funktionen", "Funktionen"),
                KonzeptKategorie("folgen-reihen", "Folgen und Reihen"),
                KonzeptKategorie("differential-integral", "Differential- und Integralrechnung"),
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
            ),
        ),
        KonzeptKategorie(
            "logik",
            "Logik",
            listOf(
                KonzeptKategorie("aussagen", "Aussagen"),
                KonzeptKategorie("praedikate", "Prädikate"),
                KonzeptKategorie("quantoren", "Quantoren"),
            ),
        ),
        KonzeptKategorie(
            "algebra",
            "Algebra",
            listOf(
                KonzeptKategorie("zahlen", "Zahlen"),
                KonzeptKategorie("operationen", "Operationen"),
                KonzeptKategorie("methoden", "Methoden und Abbildungen"),
            ),
        ),
        KonzeptKategorie(
            "topologie",
            "Topologie",
            listOf(KonzeptKategorie("grundbegriffe", "Grundbegriffe")),
        ),
        KonzeptKategorie(
            "stochastik",
            "Stochastik",
            listOf(KonzeptKategorie("grundbegriffe", "Grundbegriffe")),
        ),
        KonzeptKategorie("eigene-karten", "Eigene Karten"),
        KonzeptKategorie("karteneingaenge", "Karteneingänge"),
        KonzeptKategorie("kartenausgaenge", "Kartenausgänge"),
    )

    private val kategorienNachId: Map<String, KonzeptKategorie> = buildMap {
        kategorien.forEach { haupt ->
            put(haupt.id, haupt)
            haupt.kinder.forEach { kind -> put("${haupt.id}/${kind.id}", kind) }
        }
    }

    fun erstelle(vorlagen: List<KnotenVorlage>): List<KonzeptBibliothekEintrag> {
        val verfügbareEinträge = vorlagen.map { vorlage ->
            val variantenSchlüssel = vorlage.standardParameter.toSortedMap()
                .entries.joinToString(";") { (schlüssel, wert) -> "$schlüssel=$wert" }
            val id = listOf(vorlage.art, vorlage.name, variantenSchlüssel)
                .joinToString("|")
            KonzeptBibliothekEintrag(
                id = id,
                titel = vorlage.name,
                beschreibung = vorlage.beschreibung,
                kategoriePfade = kategoriePfadeFür(vorlage),
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
        }

        return (verfügbareEinträge + geplanteEinträge)
            .distinctBy(KonzeptBibliothekEintrag::id)
            .sortedWith(compareBy(KonzeptBibliothekEintrag::titel, KonzeptBibliothekEintrag::id))
    }

    fun bezeichnungFür(pfad: List<String>): String = pfad.mapIndexedNotNull { index, id ->
        val schlüssel = if (index == 0) id else "${pfad.first()}/$id"
        kategorienNachId[schlüssel]?.bezeichnung
    }.joinToString(" / ")

    fun unterkategorien(hauptkategorie: String): List<KonzeptKategorie> =
        kategorien.firstOrNull { it.id == hauptkategorie }?.kinder.orEmpty()

    fun validierungsFehler(einträge: List<KonzeptBibliothekEintrag>): List<String> = buildList {
        val doppelteIds = einträge.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
        doppelteIds.forEach { add("Doppelte Konzept-ID: $it") }
        einträge.forEach { eintrag ->
            if (eintrag.kategoriePfade.isEmpty()) add("${eintrag.id}: kein Kategoriepfad")
            eintrag.kategoriePfade.forEach { pfad ->
                if (pfad.isEmpty() || pfad.size > 3) add("${eintrag.id}: ungültige Hierarchietiefe ${pfad.size}")
                val haupt = kategorien.firstOrNull { it.id == pfad.firstOrNull() }
                if (haupt == null) {
                    add("${eintrag.id}: unbekannte Hauptkategorie ${pfad.firstOrNull()}")
                } else if (pfad.size >= 2 && haupt.kinder.none { it.id == pfad[1] }) {
                    add("${eintrag.id}: unbekannte Unterkategorie ${pfad[1]}")
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

    private fun kategoriePfadeFür(vorlage: KnotenVorlage): List<List<String>> {
        val art = vorlage.art.lowercase()
        val name = vorlage.name.lowercase()
        val kategorie = vorlage.kategorie.lowercase()
        val pfade = linkedSetOf<List<String>>()

        when {
            kategorie == "gespeicherte karten" || vorlage.kartenVerweis != null ->
                pfade += listOf("eigene-karten")
            art.contains("karteneingang") || name.contains("karteneingang") ->
                pfade += listOf("karteneingaenge")
            art.contains("kartenausgang") || name.contains("kartenausgang") ->
                pfade += listOf("kartenausgaenge")
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

    private val geplanteEinträge = listOf(
        KonzeptBibliothekEintrag(
            id = "geplant.topologie.offene-menge",
            titel = "Offene Menge",
            beschreibung = "Grundbegriff der Topologie; eine erzeugbare Knotenvorlage ist noch nicht registriert.",
            kategoriePfade = listOf(listOf("topologie", "grundbegriffe")),
            suchbegriffe = setOf("offene menge", "topologie", "umgebung"),
            verfügbarkeit = KonzeptVerfügbarkeit.Geplant,
        ),
        KonzeptBibliothekEintrag(
            id = "geplant.stochastik.zufallsvariable",
            titel = "Zufallsvariable",
            beschreibung = "Messbare Abbildung eines Wahrscheinlichkeitsraums; noch nicht als Knoten verfügbar.",
            kategoriePfade = listOf(listOf("stochastik", "grundbegriffe")),
            suchbegriffe = setOf("zufallsvariable", "stochastik", "wahrscheinlichkeit"),
            verfügbarkeit = KonzeptVerfügbarkeit.Geplant,
        ),
    )
}

internal fun KonzeptBibliothekEintrag.passt(filter: KonzeptBibliothekFilter): Boolean {
    val vorlage = vorlage
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

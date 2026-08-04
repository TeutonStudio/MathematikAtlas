package de.TeutonStudio.MathematikAtlas

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
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

/**
 * App-Adapter auf die kanonische Enzyklopädiequelle im Modul MathematikKnoten.
 * Fachliche Zuordnung, Suchbegriffe, Varianten und Verfügbarkeit werden nicht
 * mehr parallel im App-Modul hergeleitet.
 */
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
        KonzeptKategorie("topologie", "Topologie", listOf(KonzeptKategorie("grundbegriffe", "Grundbegriffe"))),
        KonzeptKategorie("stochastik", "Stochastik", listOf(KonzeptKategorie("grundbegriffe", "Grundbegriffe"))),
        KonzeptKategorie("eigene-karten", "Eigene Karten"),
    )

    private val kategorienNachId: Map<String, KonzeptKategorie> = buildMap {
        kategorien.forEach { haupt ->
            put(haupt.id, haupt)
            haupt.kinder.forEach { kind -> put("${haupt.id}/${kind.id}", kind) }
        }
    }

    fun erstelle(vorlagen: List<KnotenVorlage>): List<KonzeptBibliothekEintrag> =
        KonzeptKnotenRegister.erstelle(vorlagen).flatMap { wissen ->
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
                wissen.knotenVorlagen.map { vorlage ->
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

    fun bezeichnungFür(pfad: List<String>): String = pfad.mapIndexedNotNull { index, id ->
        val schlüssel = if (index == 0) id else "${pfad.first()}/$id"
        kategorienNachId[schlüssel]?.bezeichnung
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

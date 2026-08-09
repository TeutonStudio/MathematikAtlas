package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

enum class VerwaltungsBereich { Karten, Konzepte, Variablen, Auswertung, Fehler }

/** Reine Lade-Migration für bekannte assoziative Knoten; auch von JVM-Tests prüfbar. */
internal fun migriereAssoziativeKnoten(karte: KartenDaten): KartenDaten {
    val migriert = migriereFallunterscheidung(
        migriereDivisionV240(
            migriereAbbildZuAllgemeinerMethode(
                migriereTermZuMethodeUndVariablen(
                    migriereKartenAusgangZuEinzelanschluss(migriereOrdnungsrelation(karte)),
                ),
            ),
        ),
    )
    val assoziativAktualisiert = migriert.copy(knoten = migriert.knoten.map { ursprünglicherKnoten ->
        val knoten = if (ursprünglicherKnoten.art == "mathematik.differenz" && ursprünglicherKnoten.name == "Mengendifferenz") ursprünglicherKnoten.copy(name = "Differenz") else ursprünglicherKnoten
        if (knoten.art !in assoziativeKnotenArten) knoten else {
            val festeEingänge = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2
            val verbundeneEingänge = migriert.verbindungen.map { it.zu }.toSet()
            val überzähligeFesteEingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && !it.dynamischErzeugt }
                .sortedBy { it.reihenfolge }.drop(festeEingänge).filter { AnschlussVerweis(knoten.id, it.id) !in verbundeneEingänge }.map { it.id }.toSet()
            knoten.copy(
                anschlüsse = knoten.anschlüsse.filterNot { it.id in überzähligeFesteEingänge }.map { anschluss -> if (anschluss.richtung == AnschlussRichtung.Eingang) anschluss.copy(kannSichErweitern = true) else anschluss },
                parameter = knoten.parameter + mapOf("festeEingänge" to festeEingänge.toString(), "operatorAnzeige" to if (knoten.parameter["operatorAnzeige"] == "name") "name" else "wert"),
            )
        }
    })
    return migriereMatrixKnoten(assoziativAktualisiert)
}

/** Vereinigt die vier historischen Vergleichsarten zu einem parametrierten Knoten. */
internal fun migriereOrdnungsrelation(karte: KartenDaten): KartenDaten = karte.copy(
    knoten = karte.knoten.map { knoten ->
        val relation = when (knoten.art) {
            "mathematik.kleiner" -> "kleiner"
            "mathematik.kleinerGleich" -> "kleinerGleich"
            "mathematik.größer" -> "größer"
            "mathematik.größerGleich" -> "größerGleich"
            else -> null
        }
        if (relation == null) knoten else knoten.copy(
            art = MathematikKnotenVorlagen.ORDNUNGSRELATION_ART,
            parameter = knoten.parameter + ("relation" to relation),
        )
    },
)

/** Aktualisiert bestehende Bildmengen-Knoten ohne benutzerdefinierte Namen oder Kanten zu verändern. */
internal fun migriereAbbildZuAllgemeinerMethode(karte: KartenDaten): KartenDaten = karte.copy(
    knoten = karte.knoten.map { knoten ->
        if (knoten.art != "mathematik.abbild") knoten else knoten.copy(
            name = if (knoten.name == "Abbild") "Bildmenge" else knoten.name,
            anschlüsse = knoten.anschlüsse.map { anschluss ->
                if (anschluss.name == "methode" && anschluss.richtung == AnschlussRichtung.Eingang) anschluss.copy(art = MathematikAnschlussArten.Methode.id) else anschluss
            },
        )
    },
)

/** Überführt die alte verkabelte Methodensignatur in die persistierten Inspector-Parameter. */
internal fun migriereTermZuMethodeUndVariablen(karte: KartenDaten): KartenDaten {
    val entfernteAnschlüsse = mutableSetOf<AnschlussVerweis>()
    val termKnoten = mutableSetOf<KnotenId>()
    val knoten = karte.knoten.map { alt ->
        when (alt.art) {
            "mathematik.variable" -> {
                alt.anschlüsse.filter { it.name == "wertevorrat" }.forEach { entfernteAnschlüsse += AnschlussVerweis(alt.id, it.id) }
                alt.copy(
                    anschlüsse = alt.anschlüsse.filterNot { it.name == "wertevorrat" },
                    parameter = alt.parameter + ("werteVorrat" to (alt.parameter["werteVorrat"] ?: "R")),
                )
            }
            "mathematik.allgemeinerParameter" -> {
                val alterWertevorrat = alt.parameter["werteVorrat"] ?: "R"
                alt.copy(
                    parameter = alt.parameter - "werteVorrat",
                    eigenschaften = if (WertebereichKonfiguration.EIGENSCHAFT in alt.eigenschaften) alt.eigenschaften else {
                        alt.eigenschaften + (WertebereichKonfiguration.EIGENSCHAFT to WertebereichKonfiguration.Zahl(alterWertevorrat).zuEigenschaft())
                    },
                )
            }
            "mathematik.termZuMethode" -> {
                termKnoten += alt.id
                val term = alt.anschlüsse.firstOrNull { it.name == "term" }
                    ?: AnschlussDaten(name = "term", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id)
                val methode = alt.anschlüsse.firstOrNull { it.name == "methode" }
                    ?: AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Methode.id)
                alt.anschlüsse.filter { it.id != term.id && it.id != methode.id }.forEach { entfernteAnschlüsse += AnschlussVerweis(alt.id, it.id) }
                alt.copy(
                    anschlüsse = listOf(
                        term.copy(richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = if (term.art == MathematikAnschlussArten.Aussage.id) MathematikAnschlussArten.Aussage.id else MathematikAnschlussArten.Objekt.id, reihenfolge = 0, kannSichErweitern = false, dynamischErzeugt = false),
                        methode.copy(richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = if (methode.art == MathematikAnschlussArten.AussageMethode.id) MathematikAnschlussArten.AussageMethode.id else MathematikAnschlussArten.Methode.id, reihenfolge = 0, kannSichErweitern = false, dynamischErzeugt = false),
                    ),
                    parameter = (alt.parameter - "zielmenge") + ("argumentReihenfolge" to (alt.parameter["argumentReihenfolge"] ?: "")),
                )
            }
            else -> alt
        }
    }
    val nachId = knoten.associateBy { it.id }
    return karte.copy(
        knoten = knoten,
        verbindungen = karte.verbindungen.filter { verbindung ->
            if (verbindung.von in entfernteAnschlüsse || verbindung.zu in entfernteAnschlüsse) return@filter false
            if (verbindung.von.knotenId !in termKnoten) return@filter true
            val ziel = nachId[verbindung.zu.knotenId]?.anschlüsse?.firstOrNull { it.id == verbindung.zu.anschlussId } ?: return@filter false
            ziel.art in setOf(MathematikAnschlussArten.Methode.id, MathematikAnschlussArten.AussageMethode.id, MathematikAnschlussArten.Objekt.id)
        },
    )
}

/**
 * Ersetzt den früheren Null-Aussage-Ausgang der Division durch den optionalen Null-Ersatz-Eingang.
 *
 * Bestehende Verbindungen des Aussageausgangs werden nicht verworfen: Die Migration erzeugt dafür
 * einen expliziten Gleichheitsknoten `Nenner = 0` und leitet die Verbindungen auf dessen Ausgang um.
 */
internal fun migriereDivisionV240(karte: KartenDaten): KartenDaten {
    val entfernteAnschlüsse = mutableSetOf<AnschlussVerweis>()
    val ausgangUmleitungen = mutableMapOf<AnschlussVerweis, AnschlussVerweis>()
    val ergänzteKnoten = mutableListOf<KnotenDaten>()
    val ergänzteVerbindungen = mutableListOf<VerbindungDaten>()

    fun migrationsKnoten(
        vorlage: KnotenVorlage,
        id: String,
        position: GraphPunkt,
        parameter: Map<String, String> = emptyMap(),
    ): KnotenDaten {
        val erzeugt = vorlage.erzeuge(position)
        return erzeugt.copy(
            id = KnotenId(id),
            anschlüsse = erzeugt.anschlüsse.map { anschluss ->
                anschluss.copy(id = AnschlussId("$id-${anschluss.richtung.name.lowercase()}-${anschluss.name}"))
            },
            parameter = erzeugt.parameter + parameter,
        )
    }

    val knoten = karte.knoten.map { alt ->
        if (alt.art != "mathematik.division") return@map alt
        val dividend = alt.anschlüsse.firstOrNull { it.name == "dividend" && it.richtung == AnschlussRichtung.Eingang }
            ?: AnschlussDaten(name = "dividend", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id)
        val divisor = alt.anschlüsse.firstOrNull { it.name == "divisor" && it.richtung == AnschlussRichtung.Eingang }
            ?: AnschlussDaten(name = "divisor", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 1)
        val ersatz = alt.anschlüsse.firstOrNull { it.name == "fallsNennerNull" && it.richtung == AnschlussRichtung.Eingang }
            ?: AnschlussDaten(name = "fallsNennerNull", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 2)
        val wert = alt.anschlüsse.firstOrNull { it.name == "wert" && it.richtung == AnschlussRichtung.Ausgang }
            ?: AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id)
        val alterNullAusgang = alt.anschlüsse.firstOrNull {
            it.name == "divisorNull" && it.richtung == AnschlussRichtung.Ausgang
        }

        if (alterNullAusgang != null) {
            val alterNullVerweis = AnschlussVerweis(alt.id, alterNullAusgang.id)
            val divisorVerweis = AnschlussVerweis(alt.id, divisor.id)
            val divisorQuelle = karte.verbindungen.firstOrNull { it.zu == divisorVerweis }?.von
            val besitztFolgeverbindung = karte.verbindungen.any { it.von == alterNullVerweis }
            if (besitztFolgeverbindung && divisorQuelle != null) {
                val prefix = "migration-v240-${alt.id.wert}-divisor-null"
                val nullKnoten = migrationsKnoten(
                    MathematikKnotenVorlagen.Zahl,
                    "$prefix-null",
                    alt.position + GraphPunkt(0f, alt.größe.höhe + 70f),
                    mapOf("wert" to "0"),
                )
                val gleichheit = migrationsKnoten(
                    MathematikKnotenVorlagen.Gleichheit,
                    "$prefix-gleichheit",
                    alt.position + GraphPunkt(300f, alt.größe.höhe + 70f),
                )
                val links = gleichheit.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang && it.name == "links" }
                val rechts = gleichheit.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang && it.name == "rechts" }
                val aussage = gleichheit.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang && it.name == "aussage" }
                val nullWert = nullKnoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang && it.name == "wert" }
                ergänzteKnoten += listOf(nullKnoten, gleichheit)
                ergänzteVerbindungen += VerbindungDaten(
                    id = VerbindungsId("$prefix-divisor"),
                    von = divisorQuelle,
                    zu = AnschlussVerweis(gleichheit.id, links.id),
                )
                ergänzteVerbindungen += VerbindungDaten(
                    id = VerbindungsId("$prefix-nullwert"),
                    von = AnschlussVerweis(nullKnoten.id, nullWert.id),
                    zu = AnschlussVerweis(gleichheit.id, rechts.id),
                )
                ausgangUmleitungen[alterNullVerweis] = AnschlussVerweis(gleichheit.id, aussage.id)
            } else {
                entfernteAnschlüsse += alterNullVerweis
            }
        }

        val behalten = setOf(dividend.id, divisor.id, ersatz.id, wert.id)
        alt.anschlüsse.filterNot { it.id in behalten || it == alterNullAusgang }.forEach {
            entfernteAnschlüsse += AnschlussVerweis(alt.id, it.id)
        }
        alt.copy(
            anschlüsse = listOf(
                dividend.copy(name = "dividend", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 0, kannSichErweitern = false, dynamischErzeugt = false),
                divisor.copy(name = "divisor", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 1, kannSichErweitern = false, dynamischErzeugt = false),
                ersatz.copy(name = "fallsNennerNull", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 2, kannSichErweitern = false, dynamischErzeugt = false),
                wert.copy(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 0, kannSichErweitern = false, dynamischErzeugt = false),
            ),
        )
    }

    val alleKnoten = knoten + ergänzteKnoten
    val gültigeAnschlüsse = alleKnoten.flatMap { k -> k.anschlüsse.map { AnschlussVerweis(k.id, it.id) } }.toSet()
    val migrierteVerbindungen = karte.verbindungen.mapNotNull { verbindung ->
        val von = ausgangUmleitungen[verbindung.von] ?: verbindung.von
        val migriert = verbindung.copy(von = von)
        migriert.takeIf {
            it.von !in entfernteAnschlüsse && it.zu !in entfernteAnschlüsse &&
                it.von in gültigeAnschlüsse && it.zu in gültigeAnschlüsse
        }
    }
    return karte.copy(
        knoten = alleKnoten,
        verbindungen = (migrierteVerbindungen + ergänzteVerbindungen).distinctBy(VerbindungDaten::id),
    )
}

/** Überführt die alte verzweigende Fall-Karte in einen auswählenden Drei-Eingang-Knoten. */
internal fun migriereFallunterscheidung(karte: KartenDaten): KartenDaten {
    val ausgangUmleitungen = mutableMapOf<AnschlussVerweis, AnschlussVerweis>()
    val knoten = karte.knoten.map { alt ->
        if (alt.art != "mathematik.fall") return@map alt
        if (alt.anschlüsse.any { it.name == "wahr" }) {
            return@map alt.copy(anschlüsse = alt.anschlüsse.map { anschluss ->
                if (anschluss.name == "wert" && anschluss.richtung == AnschlussRichtung.Ausgang) anschluss.copy(
                    art = MathematikAnschlussArten.Objekt.id,
                    artVereinigtEingänge = listOf("wahr", "lüge"),
                ) else anschluss
            })
        }
        val wahr = alt.anschlüsse.firstOrNull { it.name == "term" && it.richtung == AnschlussRichtung.Eingang }
            ?: AnschlussDaten(name = "wahr", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id)
        val aussage = alt.anschlüsse.firstOrNull { it.name == "aussage" && it.richtung == AnschlussRichtung.Eingang }
            ?: AnschlussDaten(name = "aussage", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Aussage.id)
        val lüge = AnschlussDaten(name = "lüge", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id, reihenfolge = 2)
        val alterWert = alt.anschlüsse.firstOrNull { it.name == "wert" && it.richtung == AnschlussRichtung.Ausgang }
            ?: alt.anschlüsse.firstOrNull { it.name == "fall" && it.richtung == AnschlussRichtung.Ausgang }
            ?: AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Objekt.id)
        val wert = alterWert.copy(
            name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts,
            art = MathematikAnschlussArten.Objekt.id, reihenfolge = 0,
            kannSichErweitern = false, dynamischErzeugt = false,
            artVereinigtEingänge = listOf("wahr", "lüge"),
        )
        val wertVerweis = AnschlussVerweis(alt.id, wert.id)
        alt.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang && it.name in setOf("fall", "sonst", "wert") }
            .forEach { ausgangUmleitungen[AnschlussVerweis(alt.id, it.id)] = wertVerweis }
        alt.copy(
            anschlüsse = listOf(
                wahr.copy(name = "wahr", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id, reihenfolge = 0, kannSichErweitern = false, dynamischErzeugt = false),
                aussage.copy(name = "aussage", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Aussage.id, reihenfolge = 1, kannSichErweitern = false, dynamischErzeugt = false),
                lüge,
                wert,
            ),
        )
    }
    val gültigeAnschlüsse = knoten.flatMap { knotenDaten -> knotenDaten.anschlüsse.map { AnschlussVerweis(knotenDaten.id, it.id) } }.toSet()
    return karte.copy(
        knoten = knoten,
        verbindungen = karte.verbindungen.map { verbindung ->
            verbindung.copy(von = ausgangUmleitungen[verbindung.von] ?: verbindung.von)
        }.filter { it.von in gültigeAnschlüsse && it.zu in gültigeAnschlüsse }.distinct(),
    )
}

private val assoziativeKnotenArten = setOf(
    "mathematik.addition", "mathematik.extremwert", "mathematik.vereinigung", "mathematik.schnitt", "mathematik.kartesischesProdukt", "mathematik.tupel", "mathematik.vektor", "mathematik.zeilenVektor",
)

/** Migriert den nicht verlustfrei zerlegbaren alten Zeilenvektor-Modus auf einzelne 2×2-Eingänge. */
internal fun migriereMatrixKnoten(karte: KartenDaten): KartenDaten {
    val zuMigrieren = karte.knoten.filter { matrix -> matrix.art == "mathematik.matrix" && "erzeugungsArt" !in matrix.parameter }.map { it.id }.toSet()
    if (zuMigrieren.isEmpty()) return karte
    val knoten = karte.knoten.map { matrix -> if (matrix.id in zuMigrieren) konfiguriereMatrix(matrix, MATRIX_EINZEL_EINGABEN, höhe = 2, breite = 2) else matrix }
    val anschlüsse = knoten.associate { matrix -> matrix.id to matrix.anschlüsse.map { it.id }.toSet() }
    return karte.copy(
        knoten = knoten,
        verbindungen = karte.verbindungen.filter { verbindung ->
            (verbindung.von.knotenId !in zuMigrieren || verbindung.von.anschlussId in anschlüsse[verbindung.von.knotenId].orEmpty()) &&
                (verbindung.zu.knotenId !in zuMigrieren || verbindung.zu.anschlussId in anschlüsse[verbindung.zu.knotenId].orEmpty())
        },
    )
}

internal fun öffentlicheKartenAnschlüsse(
    karte: KartenDaten,
    interneArt: String,
    richtung: AnschlussRichtung,
    kante: AnschlussKante,
): List<AnschlussDaten> = karte.knoten.asSequence()
    .filter { it.art == interneArt }
    .mapNotNull { intern ->
        intern.anschlüsse.firstOrNull { it.name == "wert" }?.let { wert ->
            Triple(intern, öffentlicherKartenName(intern), wert.art)
        }
    }
    .withIndex()
    .distinctBy { it.value.second }
    .sortedWith(compareBy({ it.value.first.position.y }, { it.value.first.position.x }, { it.index }))
    .map { it.value }
    .mapIndexed { index, (_, name, art) ->
        AnschlussDaten(name = name, richtung = richtung, kante = kante, art = art, reihenfolge = index)
    }
    .toList()

internal fun öffentlicherKartenName(knoten: KnotenDaten): String = knoten.parameter["name"]?.trim()?.takeIf(String::isNotEmpty) ?: knoten.name

internal fun migriereKartenAusgangZuEinzelanschluss(karte: KartenDaten): KartenDaten {
    val entfernteAnschlüsse = karte.knoten.asSequence()
        .filter { it.art == "mathematik.kartenAusgang" }
        .flatMap { knoten -> knoten.anschlüsse.asSequence().filter { it.name == "zielmenge" }.map { AnschlussVerweis(knoten.id, it.id) } }
        .toSet()
    if (entfernteAnschlüsse.isEmpty()) return karte
    return karte.copy(
        knoten = karte.knoten.map { knoten -> if (knoten.art == "mathematik.kartenAusgang") knoten.copy(anschlüsse = knoten.anschlüsse.filterNot { it.name == "zielmenge" }) else knoten },
        verbindungen = karte.verbindungen.filter { it.von !in entfernteAnschlüsse && it.zu !in entfernteAnschlüsse },
    )
}

package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

enum class VerwaltungsBereich { Karten, Konzepte, Variablen, Auswertung, Fehler }

/** Reine Lade-Migration für bekannte assoziative Knoten; auch von JVM-Tests prüfbar. */
internal fun migriereAssoziativeKnoten(karte: KartenDaten): KartenDaten {
    val migriert = migriereFallunterscheidung(
        migriereDivisionV232(
            migriereAbbildZuAllgemeinerMethode(migriereTermZuMethodeUndVariablen(migriereKartenAusgangZuEinzelanschluss(karte))),
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

/** Erweitert den persistierten Methodenanschluss von Abbild-Knoten ohne Kanten zu verändern. */
internal fun migriereAbbildZuAllgemeinerMethode(karte: KartenDaten): KartenDaten = karte.copy(
    knoten = karte.knoten.map { knoten ->
        if (knoten.art != "mathematik.abbild") knoten else knoten.copy(
            anschlüsse = knoten.anschlüsse.map { anschluss ->
                if (anschluss.name == "methode" && anschluss.richtung == AnschlussRichtung.Eingang) anschluss.copy(art = MathematikAnschlussArten.Funktion.id) else anschluss
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
                    ?: AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Funktion.id)
                alt.anschlüsse.filter { it.id != term.id && it.id != methode.id }.forEach { entfernteAnschlüsse += AnschlussVerweis(alt.id, it.id) }
                alt.copy(
                    anschlüsse = listOf(
                        term.copy(richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Objekt.id, reihenfolge = 0, kannSichErweitern = false, dynamischErzeugt = false),
                        methode.copy(richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Funktion.id, reihenfolge = 0, kannSichErweitern = false, dynamischErzeugt = false),
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
            ziel.art in setOf(MathematikAnschlussArten.Funktion.id, MathematikAnschlussArten.Objekt.id)
        },
    )
}

/** Ersetzt den früheren Null-Aussage-Ausgang der Division durch den optionalen Null-Ersatz-Eingang. */
internal fun migriereDivisionV232(karte: KartenDaten): KartenDaten {
    val entfernteAnschlüsse = mutableSetOf<AnschlussVerweis>()
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
        val behalten = setOf(dividend.id, divisor.id, ersatz.id, wert.id)
        alt.anschlüsse.filterNot { it.id in behalten }.forEach { entfernteAnschlüsse += AnschlussVerweis(alt.id, it.id) }
        alt.copy(
            anschlüsse = listOf(
                dividend.copy(name = "dividend", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 0, kannSichErweitern = false, dynamischErzeugt = false),
                divisor.copy(name = "divisor", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 1, kannSichErweitern = false, dynamischErzeugt = false),
                ersatz.copy(name = "fallsNennerNull", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 2, kannSichErweitern = false, dynamischErzeugt = false),
                wert.copy(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id, reihenfolge = 0, kannSichErweitern = false, dynamischErzeugt = false),
            ),
        )
    }
    val gültigeAnschlüsse = knoten.flatMap { k -> k.anschlüsse.map { AnschlussVerweis(k.id, it.id) } }.toSet()
    return karte.copy(
        knoten = knoten,
        verbindungen = karte.verbindungen.filter { verbindung ->
            verbindung.von !in entfernteAnschlüsse && verbindung.zu !in entfernteAnschlüsse &&
                verbindung.von in gültigeAnschlüsse && verbindung.zu in gültigeAnschlüsse
        },
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
    .mapNotNull { intern -> intern.anschlüsse.firstOrNull { it.name == "wert" }?.let { wert -> öffentlicherKartenName(intern) to wert.art } }
    .distinctBy { it.first }
    .mapIndexed { index, (name, art) -> AnschlussDaten(name = name, richtung = richtung, kante = kante, art = art, reihenfolge = index) }
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

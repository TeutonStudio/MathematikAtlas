package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Base64
import java.util.UUID

const val ENDLICHE_MENGE_KONFIGURATION_PARAMETER = "elementKonfiguration"
const val ENDLICHE_MENGE_ALT_PARAMETER = "elemente"

data class EndlicheMengeKonfiguration(
    val version: Int = 2,
    val einträge: List<EndlicheMengeEintrag> = emptyList(),
    val gemeinsameArt: String? = null,
) {
    init { require(version == 2) { "Unbekannte Endliche-Menge-Konfigurationsversion $version." } }

    fun zuParameter(): String = buildList {
        add("v2:${kodieren(gemeinsameArt.orEmpty())}")
        einträge.forEach { eintrag ->
            val grunddaten = listOf(kodieren(eintrag.id), kodieren(eintrag.art))
            add(
                when (val quelle = eintrag.quelle) {
                    is EndlicheMengeQuelle.ZahlLiteral ->
                        (listOf("z") + grunddaten + kodieren(quelle.wert)).joinToString(":")
                    is EndlicheMengeQuelle.TupelLiteral ->
                        (listOf("t") + grunddaten + quelle.werte.size.toString() + quelle.werte.map(::kodieren)).joinToString(":")
                    is EndlicheMengeQuelle.Konstante ->
                        (listOf("k") + grunddaten + kodieren(quelle.id)).joinToString(":")
                },
            )
        }
    }.joinToString("|")

    companion object {
        fun standard(): EndlicheMengeKonfiguration = EndlicheMengeKonfiguration(
            einträge = listOf("1", "2", "3").mapIndexed { index, wert ->
                EndlicheMengeEintrag(
                    id = "standard-${index + 1}",
                    art = MathematikAnschlussArten.Zahl.id.wert,
                    quelle = EndlicheMengeQuelle.ZahlLiteral(wert),
                )
            },
        ).mitErkannterGemeinsamerArt()
    }
}

data class EndlicheMengeEintrag(
    val id: String,
    val art: String,
    val quelle: EndlicheMengeQuelle,
)

sealed interface EndlicheMengeQuelle {
    data class ZahlLiteral(val wert: String) : EndlicheMengeQuelle
    data class TupelLiteral(val werte: List<String>) : EndlicheMengeQuelle
    data class Konstante(val id: String) : EndlicheMengeQuelle
}

data class EndlicheMengeLeseErgebnis(
    val konfiguration: EndlicheMengeKonfiguration,
    val altformat: Boolean = false,
    val fehler: String? = null,
)

data class EndlicheMengeElementAuswertung(
    val objekt: MathematischesObjekt? = null,
    val art: AnschlussArtId? = null,
    val fehler: String? = null,
)

data class EndlicheMengeNormalisierung(
    val konfiguration: EndlicheMengeKonfiguration,
    val warnungen: List<String> = emptyList(),
)

data class EndlicheMengeKonstante(
    val id: String,
    val name: String,
    val art: AnschlussArtId,
    val objekt: MathematischesObjekt,
)

object EndlicheMengeKonstanten {
    val alle: List<EndlicheMengeKonstante> = listOf(
        EndlicheMengeKonstante("zahl.pi", "π", MathematikAnschlussArten.Zahl.id, Pi),
        EndlicheMengeKonstante("zahl.e", "e", MathematikAnschlussArten.Zahl.id, EulerscheZahl),
        EndlicheMengeKonstante(
            "zahl.i",
            "i",
            MathematikAnschlussArten.Zahl.id,
            KomplexeZahl(RationaleZahl.Null, RationaleZahl.Eins),
        ),
        EndlicheMengeKonstante(
            "aussage.wahr",
            "Wahr",
            MathematikAnschlussArten.Aussage.id,
            WahrheitsKonstante(true),
        ),
        EndlicheMengeKonstante(
            "aussage.lüge",
            "Lüge",
            MathematikAnschlussArten.Aussage.id,
            WahrheitsKonstante(false),
        ),
        EndlicheMengeKonstante("menge.leer", "∅", MathematikAnschlussArten.Menge.id, LeereMenge),
        EndlicheMengeKonstante("menge.n", "ℕ", MathematikAnschlussArten.Menge.id, NatürlicheZahlen),
        EndlicheMengeKonstante("menge.z", "ℤ", MathematikAnschlussArten.Menge.id, GanzeZahlen),
        EndlicheMengeKonstante("menge.q", "ℚ", MathematikAnschlussArten.Menge.id, RationaleZahlen),
        EndlicheMengeKonstante("menge.r", "ℝ", MathematikAnschlussArten.Menge.id, ReelleZahlen),
        EndlicheMengeKonstante("menge.c", "ℂ", MathematikAnschlussArten.Menge.id, KomplexeZahlen),
        EndlicheMengeKonstante("menge.prim", "Primzahlen", MathematikAnschlussArten.Menge.id, Primzahlen),
        EndlicheMengeKonstante(
            "menge.gauss",
            "Gaußsche ganze Zahlen",
            MathematikAnschlussArten.Menge.id,
            GaußscheGanzeZahlen,
        ),
        EndlicheMengeKonstante(
            "menge.gaussPrim",
            "Gaußsche Primzahlen",
            MathematikAnschlussArten.Menge.id,
            GaußschePrimzahlen,
        ),
    )

    fun finde(id: String): EndlicheMengeKonstante? = alle.firstOrNull { it.id == id }
}

private val encoder = Base64.getUrlEncoder().withoutPadding()
private val decoder = Base64.getUrlDecoder()
private val artRegister by lazy { AnschlussArtRegister(MathematikAnschlussArten.alle) }

fun neuerEndlicheMengeEintrag(): EndlicheMengeEintrag = EndlicheMengeEintrag(
    id = UUID.randomUUID().toString(),
    art = MathematikAnschlussArten.Zahl.id.wert,
    quelle = EndlicheMengeQuelle.ZahlLiteral(""),
)

fun leseEndlicheMengeKonfiguration(knoten: KnotenDaten): EndlicheMengeLeseErgebnis {
    val neu = knoten.parameter[ENDLICHE_MENGE_KONFIGURATION_PARAMETER]
    if (!neu.isNullOrBlank()) {
        return runCatching { EndlicheMengeLeseErgebnis(parseKonfiguration(neu)) }
            .getOrElse { EndlicheMengeLeseErgebnis(EndlicheMengeKonfiguration(), fehler = it.message) }
    }

    val alt = knoten.parameter[ENDLICHE_MENGE_ALT_PARAMETER]
    if (alt != null) {
        val einträge = alt.split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
            .mapIndexed { index, wert ->
                EndlicheMengeEintrag(
                    id = "legacy-${index + 1}",
                    art = MathematikAnschlussArten.Zahl.id.wert,
                    quelle = EndlicheMengeQuelle.ZahlLiteral(wert),
                )
            }
        return EndlicheMengeLeseErgebnis(
            EndlicheMengeKonfiguration(einträge = einträge).mitErkannterGemeinsamerArt(),
            altformat = true,
        )
    }
    return EndlicheMengeLeseErgebnis(EndlicheMengeKonfiguration())
}

fun EndlicheMengeKonfiguration.mitErkannterGemeinsamerArt(): EndlicheMengeKonfiguration {
    val arten = einträge.mapNotNull { it.tatsächlicheArt() }
    return copy(gemeinsameArt = artRegister.gemeinsameOberart(arten)?.wert)
}

fun EndlicheMengeEintrag.tatsächlicheArt(): AnschlussArtId? = when (val quelle = quelle) {
    is EndlicheMengeQuelle.ZahlLiteral -> MathematikAnschlussArten.Zahl.id
    is EndlicheMengeQuelle.TupelLiteral -> MathematikAnschlussArten.Tupel.id
    is EndlicheMengeQuelle.Konstante -> EndlicheMengeKonstanten.finde(quelle.id)?.art
}

fun EndlicheMengeEintrag.auswerten(): EndlicheMengeElementAuswertung = runCatching {
    when (val quelle = quelle) {
        is EndlicheMengeQuelle.ZahlLiteral -> {
            require(art == MathematikAnschlussArten.Zahl.id.wert) {
                "Ein direkt eingegebener Zahlwert benötigt den Typ Zahl."
            }
            EndlicheMengeElementAuswertung(
                objekt = parseEndlicheMengeZahl(quelle.wert),
                art = MathematikAnschlussArten.Zahl.id,
            )
        }
        is EndlicheMengeQuelle.TupelLiteral -> {
            require(art == MathematikAnschlussArten.Tupel.id.wert) {
                "Ein direkt eingegebenes Tupel benötigt den Typ Tupel."
            }
            require(quelle.werte.isNotEmpty()) { "Ein Tupel benötigt mindestens eine Komponente." }
            EndlicheMengeElementAuswertung(
                objekt = Tupel(quelle.werte.mapIndexed { index, wert ->
                    runCatching { parseEndlicheMengeZahl(wert) }
                        .getOrElse { error("Tupelkomponente ${index + 1}: ${it.message}") }
                }),
                art = MathematikAnschlussArten.Tupel.id,
            )
        }
        is EndlicheMengeQuelle.Konstante -> {
            val konstante = EndlicheMengeKonstanten.finde(quelle.id)
                ?: error("Die ausgewählte Konstante ist nicht mehr verfügbar.")
            val ausgewählteArt = AnschlussArtId(art)
            require(artRegister.istUnterart(konstante.art, ausgewählteArt)) {
                "${konstante.name} ist nicht mit dem Typ ${ausgewählteArt.wert} kompatibel."
            }
            EndlicheMengeElementAuswertung(konstante.objekt, konstante.art)
        }
    }
}.getOrElse { EndlicheMengeElementAuswertung(fehler = it.message ?: "Ungültiges Mengenelement.") }

fun normalisiereEndlicheMengeKonfiguration(
    konfiguration: EndlicheMengeKonfiguration,
): EndlicheMengeNormalisierung {
    val gesehen = linkedSetOf<MathematischesObjekt>()
    var duplikate = 0
    val eindeutig = konfiguration.einträge.filter { eintrag ->
        val objekt = eintrag.auswerten().objekt ?: return@filter true
        if (gesehen.add(objekt)) true else {
            duplikate += 1
            false
        }
    }
    val warnungen = when (duplikate) {
        0 -> emptyList()
        1 -> listOf("Ein doppeltes Element wurde zusammengeführt.")
        else -> listOf("$duplikate doppelte Elemente wurden zusammengeführt.")
    }
    return EndlicheMengeNormalisierung(
        konfiguration.copy(einträge = eindeutig).mitErkannterGemeinsamerArt(),
        warnungen,
    )
}

fun parseEndlicheMengeZahl(text: String): ZahlAusdruck {
    val kompakt = text.trim().replace(" ", "")
    require(kompakt.isNotEmpty()) { "Zahlwert fehlt." }
    if (!kompakt.endsWith('i')) return parseReelleKomponente(kompakt)

    val ohneI = kompakt.dropLast(1)
    if (ohneI.isEmpty() || ohneI == "+") return KomplexeZahl(RationaleZahl.Null, RationaleZahl.Eins)
    if (ohneI == "-") return KomplexeZahl(RationaleZahl.Null, RationaleZahl.von(-1))

    val trennung = (1 until ohneI.length).lastOrNull { ohneI[it] == '+' || ohneI[it] == '-' }
    return if (trennung == null) {
        KomplexeZahl(RationaleZahl.Null, parseReelleKomponente(ohneI))
    } else {
        val real = parseReelleKomponente(ohneI.substring(0, trennung))
        val imaginärText = ohneI.substring(trennung)
        val imaginär = when (imaginärText) {
            "+" -> RationaleZahl.Eins
            "-" -> RationaleZahl.von(-1)
            else -> parseReelleKomponente(imaginärText)
        }
        KomplexeZahl(real, imaginär)
    }
}

private fun parseReelleKomponente(text: String): ZahlAusdruck = when (text.lowercase()) {
    "pi", "π" -> Pi
    "e" -> EulerscheZahl
    else -> if ('.' in text) {
        val dezimal = BigDecimal(text)
        val skaliert = dezimal.stripTrailingZeros()
        val nenner = BigInteger.TEN.pow(skaliert.scale().coerceAtLeast(0))
        RationaleZahl.von(skaliert.unscaledValue(), nenner)
    } else RationaleZahl.parse(text)
}

private fun parseKonfiguration(text: String): EndlicheMengeKonfiguration {
    val segmente = text.split('|')
    require(segmente.isNotEmpty() && segmente.first().startsWith("v2:")) {
        "Ungültiges Format der Endliche-Menge-Konfiguration."
    }
    val gemeinsameArt = dekodieren(segmente.first().substringAfter("v2:")).ifBlank { null }
    val einträge = segmente.drop(1).filter(String::isNotBlank).map(::parseEintrag)
    return EndlicheMengeKonfiguration(einträge = einträge, gemeinsameArt = gemeinsameArt)
}

private fun parseEintrag(text: String): EndlicheMengeEintrag {
    val teile = text.split(':')
    require(teile.size >= 4) { "Unvollständiger Endliche-Menge-Eintrag." }
    val id = dekodieren(teile[1])
    val art = dekodieren(teile[2])
    require(id.isNotBlank() && art.isNotBlank()) { "Element-ID und Typ dürfen nicht leer sein." }
    val quelle = when (teile[0]) {
        "z" -> {
            require(teile.size == 4) { "Ungültiger Zahleneintrag." }
            EndlicheMengeQuelle.ZahlLiteral(dekodieren(teile[3]))
        }
        "k" -> {
            require(teile.size == 4) { "Ungültiger Konstanteneintrag." }
            EndlicheMengeQuelle.Konstante(dekodieren(teile[3]))
        }
        "t" -> {
            require(teile.size >= 5) { "Ungültiger Tupeleintrag." }
            val anzahl = teile[3].toIntOrNull() ?: error("Ungültige Tupeldimension.")
            require(anzahl >= 1 && teile.size == 4 + anzahl) { "Tupeldimension und Komponenten stimmen nicht überein." }
            EndlicheMengeQuelle.TupelLiteral(teile.drop(4).map(::dekodieren))
        }
        else -> error("Unbekannte Elementquelle ${teile[0]}.")
    }
    return EndlicheMengeEintrag(id, art, quelle)
}

private fun kodieren(text: String): String = if (text.isEmpty()) "~" else
    encoder.encodeToString(text.toByteArray(Charsets.UTF_8))

private fun dekodieren(text: String): String = if (text == "~") "" else
    String(decoder.decode(text), Charsets.UTF_8)

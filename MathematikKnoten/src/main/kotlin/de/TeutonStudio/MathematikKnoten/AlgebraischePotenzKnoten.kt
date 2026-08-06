package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger

const val ALGEBRAISCHE_POTENZ_KNOTEN_ART = "mathematik.algebraischePotenz"
const val POTENZ_ORDNUNG_PARAMETER = "potenz.ordnung"
const val POTENZ_STRUKTUR_MODUS_PARAMETER = "potenz.strukturModus"
const val POTENZ_STRUKTUR_ID_PARAMETER = "potenz.strukturId"
const val POTENZ_MIGRATIONSFEHLER_PARAMETER = "potenz.migrationsFehler"

private const val HISTORISCHE_STRUKTURPOTENZ_KNOTEN_ART = "mathematik.potenzStrukturell"

enum class PotenzStrukturModus {
    AUTO,
    EXPLIZIT,
}

object AlgebraischePotenzKnotenVorlagen {
    val Potenz = KnotenVorlage(
        art = ALGEBRAISCHE_POTENZ_KNOTEN_ART,
        name = "Potenz",
        kategorie = "Algebra: Operationen",
        beschreibung = "Natürliche Potenz als iterative Multiplikation in einer nachgewiesenen Struktur. Methoden werden punktweise potenziert, niemals komponiert.",
        standardGröße = GraphGröße(285f, 140f),
        anschlüsse = listOf(
            potenzEingang("basis", MathematikAnschlussArten.Objekt.id, 0),
            potenzEingang("ordnung", MathematikAnschlussArten.Zahl.id, 1),
            potenzEingang("struktur", MathematikAnschlussArten.Objekt.id, 2),
            AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Objekt.id,
            ),
        ),
        standardParameter = mapOf(
            POTENZ_ORDNUNG_PARAMETER to "2",
            POTENZ_STRUKTUR_MODUS_PARAMETER to PotenzStrukturModus.AUTO.name,
            POTENZ_STRUKTUR_ID_PARAMETER to "",
        ),
    )

    val alle = listOf(Potenz)
}

internal fun MathematikAuswerterRegister.registriereAlgebraischePotenz() {
    registriere(ALGEBRAISCHE_POTENZ_KNOTEN_ART) { kontext ->
        kontext.werteAlgebraischePotenzAus()
    }
}

private fun KnotenAuswertungsKontext.werteAlgebraischePotenzAus(): KnotenAuswertungsErgebnis {
    knoten.parameter[POTENZ_MIGRATIONSFEHLER_PARAMETER]?.let { fehler ->
        return fehlerErgebnis(fehler)
    }
    val basisWert = eingänge["basis"]
        ?: return fehlerErgebnis("Die Potenz benötigt eine Basis.")
    val ordnung = bestimmePotenzOrdnung()
        ?: return fehlerErgebnis("Die Potenzordnung muss konkret oder symbolisch in ℕ₀ liegen.")
    val strukturModus = PotenzStrukturModus.entries.firstOrNull {
        it.name == knoten.parameter[POTENZ_STRUKTUR_MODUS_PARAMETER]
    } ?: PotenzStrukturModus.AUTO
    val strukturEingang = eingänge["struktur"]?.objekt
    if (strukturEingang != null && strukturEingang !is PotenzStruktur) {
        return fehlerErgebnis("Der Eingang 'struktur' benötigt einen PotenzStruktur-Vertrag.")
    }
    val expliziteStruktur = strukturEingang as? PotenzStruktur
    if (strukturModus == PotenzStrukturModus.EXPLIZIT && expliziteStruktur == null) {
        return fehlerErgebnis("Der explizite Strukturmodus benötigt eine verbundene Potenzstruktur.")
    }

    val ergebnis = PotenzDienst.werteAus(
        basis = basisWert.objekt,
        ordnung = ordnung,
        expliziteStruktur = expliziteStruktur,
        kontext = rechenKontext.copy(annahmen = rechenKontext.annahmen + gemeinsameAnnahmen()),
        werteVorräte = (basisWert.objekt as? Methode)?.werteVorräte.orEmpty(),
    )
    return when (ergebnis) {
        is PotenzDienstErgebnis.Ungueltig -> fehlerErgebnis(
            "${ergebnis.code}: ${ergebnis.grund}",
        )
        is PotenzDienstErgebnis.ObjektWert -> wertErgebnis(
            wert = ergebnis.wert,
            annahmen = ergebnis.voraussetzungen,
            zielMenge = ergebnis.traeger,
            strukturId = ergebnis.strukturId,
            status = "BERECHNET",
        )
        is PotenzDienstErgebnis.MethodenWert -> wertErgebnis(
            wert = ergebnis.methode,
            annahmen = ergebnis.voraussetzungen,
            zielMenge = ergebnis.methode.zielMenge,
            strukturId = ergebnis.strukturId,
            status = "BERECHNET",
        )
        is PotenzDienstErgebnis.Symbolisch -> wertErgebnis(
            wert = ergebnis.wert,
            annahmen = ergebnis.voraussetzungen,
            zielMenge = ergebnis.symbolischesZielOderNull(),
            strukturId = ergebnis.strukturId,
            status = if (ergebnis.voraussetzungen.isEmpty()) "SYMBOLISCH" else "BEDINGT",
        )
    }
}

private fun KnotenAuswertungsKontext.bestimmePotenzOrdnung(): IterationsOrdnung? {
    val verbunden = eingänge["ordnung"]
    val objekt = verbunden?.objekt
    if (objekt != null) {
        return when (val pruefung = pruefePotenzOrdnung(objekt, verbunden.annahmen)) {
            is PotenzOrdnungsPruefung.Gueltig -> pruefung.ordnung
            is PotenzOrdnungsPruefung.Ungueltig -> null
        }
    }
    val fallback = knoten.parameter[POTENZ_ORDNUNG_PARAMETER]
        ?.trim()
        ?.toBigIntegerOrNull()
        ?: return null
    return fallback.takeIf { it.signum() >= 0 }?.let(IterationsOrdnung::Konkret)
}

private sealed interface PotenzOrdnungsPruefung {
    data class Gueltig(val ordnung: IterationsOrdnung) : PotenzOrdnungsPruefung
    data class Ungueltig(val grund: String) : PotenzOrdnungsPruefung
}

private fun pruefePotenzOrdnung(
    objekt: MathematischesObjekt,
    annahmen: Set<Aussage>,
): PotenzOrdnungsPruefung = when (objekt) {
    is RationaleZahl -> when {
        objekt.nenner != BigInteger.ONE -> PotenzOrdnungsPruefung.Ungueltig(
            "Die Potenzordnung muss ganzzahlig sein.",
        )
        objekt.zähler.signum() < 0 -> PotenzOrdnungsPruefung.Ungueltig(
            "Negative Exponenten gehören nicht zur natürlichen Iterationspotenz.",
        )
        else -> PotenzOrdnungsPruefung.Gueltig(IterationsOrdnung.Konkret(objekt.zähler))
    }
    is ZahlAusdruck -> PotenzOrdnungsPruefung.Gueltig(
        IterationsOrdnung.Symbolisch(
            ausdruck = objekt,
            annahmen = annahmen + UnentscheidbareAussage(
                "${objekt.zuLatex()}\\in\\mathbb N_0",
                "Potenzordnung",
            ),
        ),
    )
    else -> PotenzOrdnungsPruefung.Ungueltig("Die Potenzordnung muss ein Zahlterm sein.")
}

private fun KnotenAuswertungsKontext.wertErgebnis(
    wert: MathematischesObjekt,
    annahmen: Set<Aussage>,
    zielMenge: MengenAusdruck?,
    strukturId: String,
    status: String,
): KnotenAuswertungsErgebnis = KnotenAuswertungsErgebnis(
    ausgaben = mapOf(
        "wert" to BedingterWert(
            objekt = wert,
            annahmen = gemeinsameAnnahmen() + annahmen,
            zielMenge = zielMenge,
        ),
    ),
    warnungen = listOf(
        "Status: $status",
        "Potenzstruktur: $strukturId",
        "Operator: ${IterationsArt.MULTIPLIKATION.operatorId}",
    ),
    eingänge = eingänge,
)

private fun PotenzDienstErgebnis.Symbolisch.symbolischesZielOderNull(): MengenAusdruck? = when (wert) {
    is AlgebraischePotenz -> wert.struktur.traegerMenge
    is Methode -> wert.zielMenge
    else -> null
}

private fun KnotenAuswertungsKontext.gemeinsameAnnahmen(): Set<Aussage> =
    eingänge.values.flatMap { it.annahmen }.toSet()

private fun KnotenAuswertungsKontext.fehlerErgebnis(nachricht: String): KnotenAuswertungsErgebnis =
    KnotenAuswertungsErgebnis(
        ausgaben = emptyMap(),
        fehler = nachricht,
        eingänge = eingänge,
    )

fun KartenDaten.migriereAlgebraischePotenzKnoten(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        when (knoten.art) {
            ALGEBRAISCHE_POTENZ_KNOTEN_ART -> knoten.normalisierePotenzParameter()
            HISTORISCHE_STRUKTURPOTENZ_KNOTEN_ART -> knoten.copy(
                art = ALGEBRAISCHE_POTENZ_KNOTEN_ART,
                name = "Potenz",
                parameter = knoten.parameter + mapOf(
                    POTENZ_ORDNUNG_PARAMETER to (knoten.parameter["exponent"] ?: "2"),
                    POTENZ_STRUKTUR_MODUS_PARAMETER to PotenzStrukturModus.AUTO.name,
                    POTENZ_STRUKTUR_ID_PARAMETER to "",
                ),
            ).normalisierePotenzParameter()
            else -> knoten
        }
    },
)

private fun KnotenDaten.normalisierePotenzParameter(): KnotenDaten = copy(
    parameter = AlgebraischePotenzKnotenVorlagen.Potenz.standardParameter + parameter,
)

private fun potenzEingang(
    name: String,
    art: AnschlussArtId,
    reihenfolge: Int,
): AnschlussDaten = AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
)

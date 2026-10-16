package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

object MethodenQuellKnotenVorlagen {
    val Methode = KnotenVorlage(METHODEN_QUELLE_ART, "Methode", "Methoden",
        "Identität, ein konfigurierter Knoten oder eine versionsfeste Karte als Methode.", GraphGröße(245f, 125f),
        listOf(AnschlussDaten(name = "menge", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Menge.id),
            AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Methode.id)))
}

fun methodenQuellKnoten(knoten: KnotenDaten): KnotenDaten? =
    (knoten.eigenschaften[METHODEN_QUELLE_KNOTEN] as? KnotenEigenschaft.Text)?.wert?.let { KartenDatenJson.lese(it).knoten.single() }

/** Ein Quellenwechsel ist eine atomare Modelländerung mit stabilem Methodenausgang. */
fun konfiguriereMethodenQuelle(knoten: KnotenDaten, quelle: KnotenDaten? = null, karte: KartenVerweis? = null, quelleId: String? = null): KnotenDaten {
    require(knoten.art == METHODEN_QUELLE_ART)
    require(quelle == null || karte == null)
    if (quelle != null) require(quelle.art != METHODEN_QUELLE_ART && quelle.kartenVerweis == null)
    val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang && it.name == "methode" }
    val identitaet = quelle == null && karte == null
    val menge = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == "menge" }
        ?: MethodenQuellKnotenVorlagen.Methode.anschlüsse.first().copy(id = AnschlussId("${knoten.id.wert}:identitaet:menge"))
    val eigenschaften = knoten.eigenschaften - METHODEN_QUELLE_KNOTEN - METHODEN_QUELLE_ID
    val id = quelleId ?: if (quelle != null) knoten.eigenschaften.text(METHODEN_QUELLE_ID, "").takeIf { it.startsWith("knoten:${quelle.art}:") || it.startsWith("operator:") } ?: "knoten:${quelle.art}:${quelle.parameter}" else if (karte != null) "karte:$karte" else "identitaet"
    return knoten.copy(kartenVerweis = karte,
        eigenschaften = (if (quelle == null) eigenschaften else eigenschaften +
            (METHODEN_QUELLE_KNOTEN to KnotenEigenschaft.Text(KartenDatenJson.schreibe(KartenDaten(name = "Knotenquelle", knoten = listOf(quelle)))))) + (METHODEN_QUELLE_ID to KnotenEigenschaft.Text(id)),
        anschlüsse = (if (identitaet) listOf(menge) else emptyList()) + ausgang).also(::validiereMethodenQuellenKonfiguration)
}

data class MethodenKnotenQuelle(val id: String, val titel: String, val kategorie: String, val knoten: KnotenDaten, val fehler: String?, val signatur: String = "")

/** Probeauswertung über denselben Kartenexport wie die produktive Auswertung. */
fun methodenKnotenQuellen(): List<MethodenKnotenQuelle> {
    val register = GesamterMathematikAuswerter.erzeugeRegister()
    val auswerter = KartenAuswerter(register)
    val vorlagen = de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog.alle().filter { it.art !in setOf(METHODEN_QUELLE_ART, "mathematik.kartenEingang", "mathematik.kartenAusgang") }
    val kandidaten = vorlagen.map { v -> Triple("knoten:${v.art}:${v.standardParameter}", v.kategorie, v.erzeuge(GraphPunkt(0f, 0f))) } +
        RechnerOperatorRegistry.alle.mapNotNull { op ->
            vorlagen.firstOrNull { it.art == op.familie.knotenArt }?.let { v ->
                Triple("operator:${op.familie}:${op.operatorId}", op.kategorie,
                    RechnerOperatorRegistry.konfiguriere(v.erzeuge(GraphPunkt(0f, 0f)), op.operatorId).copy(name = op.titel))
            }
        }
    return kandidaten.distinctBy { it.first }.map { (id, kategorie, quelle) ->
        val probe = runCatching { auswerter.exportiereKarteAlsMethode(methodenQuellKarte(quelle, KnotenId("probe:$id"))) }
        MethodenKnotenQuelle(id, quelle.name, kategorie, quelle, probe.exceptionOrNull()?.message ?: probe.getOrNull()?.fehler, methodenQuellenSignatur(probe.getOrNull()?.ausgaben?.get("methode")?.objekt as? SignaturtragendeMethode))
    }
}

/** Verwendet die vorhandenen fachlichen Konfigurationsfunktionen auch für eingebettete Quellen. */
fun konfiguriereMethodenQuellParameter(quelle: KnotenDaten, schlüssel: String, wert: String): KnotenDaten {
    val familie = RechnerOperatorRegistry.familieFür(quelle.art)
    if (familie != null && schlüssel == familie.operatorParameter) {
        require(RechnerOperatorRegistry.finde(familie, wert) is RechnerOperatorAuflösung.Bekannt) { "Unbekannter Operator." }
        return RechnerOperatorRegistry.konfiguriere(quelle, wert)
    }
    if (schlüssel == "festeEingänge") {
        val n = wert.toIntOrNull()?.takeIf { it in 2..128 } ?: error("Die Anzahl muss zwischen 2 und 128 liegen.")
        val register = de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister(MathematikAnschlussArten.alle)
        val editor = de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand(KartenDaten(name = "Quelle", knoten = listOf(quelle)), de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung(register))
        editor.setzeFesteEingangAnzahl(quelle.id, n)
        return editor.karte.knoten.single()
    }
    return quelle.copy(parameter = quelle.parameter + (schlüssel to wert))
}

fun methodenQuellenSignatur(methode: SignaturtragendeMethode?): String = methode?.signatur?.let { signatur ->
    "(${signatur.argumente.joinToString { it.name }}) → (${signatur.ergebnisse.joinToString { it.name }})"
}.orEmpty()

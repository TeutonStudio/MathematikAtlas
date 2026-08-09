package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import java.util.UUID

object FaltungsKnotenVorlagen {
    private fun eingang(
        name: String,
        art: AnschlussArtId,
        reihe: Int = 0,
        erweiterbar: Boolean = false,
    ) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = reihe,
        kannSichErweitern = erweiterbar,
    )

    private fun ausgang(name: String, art: AnschlussArtId, folgt: String? = null) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
        artFolgtEingang = folgt,
    )

    val Faltungskonstruktor = KnotenVorlage(
        art = FALTUNGSKONSTRUKTOR_ART,
        name = "Faltungskonstruktor",
        kategorie = "Operatoren",
        beschreibung = "Bindet Index und Akkumulator einer endlichen Faltung.",
        standardGröße = GraphGröße(285f, 145f),
        anschlüsse = listOf(
            eingang("indexmenge", MathematikAnschlussArten.Menge.id, 0),
            eingang("neutral", MathematikAnschlussArten.Objekt.id, 1),
            ausgang("index", MathematikAnschlussArten.Zahl.id),
            ausgang("akkumulator", MathematikAnschlussArten.Objekt.id, folgt = "neutral"),
        ),
        standardParameter = mapOf(
            FALTUNG_OPERATOR to "summe",
            FALTUNG_INDEXNAME to "i",
            FALTUNG_AKKUMULATORNAME to "a",
        ),
    )

    val Faltungsdefinator = KnotenVorlage(
        art = FALTUNGSDEFINATOR_ART,
        name = "Faltungsdefinator",
        kategorie = "Operatoren",
        beschreibung = "Schließt eine gekoppelte Faltung mit dem nächsten Akkumulatorwert ab.",
        standardGröße = GraphGröße(285f, 115f),
        anschlüsse = listOf(
            eingang("nächsterAkkumulator", MathematikAnschlussArten.Objekt.id),
            ausgang("wert", MathematikAnschlussArten.Objekt.id, folgt = "nächsterAkkumulator"),
        ),
        standardParameter = mapOf(FALTUNG_OPERATOR to "summe"),
    )

    val MethodeAufrufen = KnotenVorlage(
        art = METHODEN_AUFRUF_ART,
        name = "Methode aufrufen",
        kategorie = "Methoden",
        beschreibung = "Wendet eine Methode an; unbekannte Signaturen werden sicher als Argumenttupel behandelt.",
        standardGröße = GraphGröße(285f, 135f),
        anschlüsse = listOf(
            eingang("methode", MathematikAnschlussArten.Methode.id, 0),
            eingang("argument-0", MathematikAnschlussArten.Tupel.id, 1),
            ausgang("wert", MathematikAnschlussArten.Objekt.id),
        ),
        standardParameter = mapOf(
            METHODEN_ANWENDUNG_ERGEBNIS_ART to MathematikAnschlussArten.Objekt.id.wert,
            METHODEN_AUFRUF_ARGUMENTPROJEKTION to METHODEN_ARGUMENTPROJEKTION_TUPEL,
            "festeEingänge" to "1",
        ),
    )

    val MethodenAnwendungZahl = methodenAnwendung(
        name = "Zahlmethode anwenden",
        methodeArt = MathematikAnschlussArten.ZahlMethode.id,
        ergebnisArt = MathematikAnschlussArten.Zahl.id,
    )
    val MethodenAnwendungAussage = methodenAnwendung(
        name = "Aussagenmethode anwenden",
        methodeArt = MathematikAnschlussArten.AussageMethode.id,
        ergebnisArt = MathematikAnschlussArten.Aussage.id,
    )
    val MethodenAnwendungMenge = methodenAnwendung(
        name = "Mengenmethode anwenden",
        methodeArt = MathematikAnschlussArten.MengenMethode.id,
        ergebnisArt = MathematikAnschlussArten.Menge.id,
    )
    val MethodenAnwendungObjekt = methodenAnwendung(
        name = "Methode allgemein anwenden",
        methodeArt = MathematikAnschlussArten.Methode.id,
        ergebnisArt = MathematikAnschlussArten.Objekt.id,
    )

    val MethodenZielmenge = KnotenVorlage(
        art = METHODEN_ZIELMENGE_ART,
        name = "Methoden-Zielmenge",
        kategorie = "Methoden",
        beschreibung = "Gibt die deklarierte Zielmenge einer Methode aus.",
        standardGröße = GraphGröße(250f, 105f),
        anschlüsse = listOf(
            eingang("methode", MathematikAnschlussArten.Methode.id),
            ausgang("menge", MathematikAnschlussArten.Menge.id),
        ),
    )

    val MethodenWertevorrat = KnotenVorlage(
        art = METHODEN_WERTEVORRAT_ART,
        name = "Methoden-Wertevorrat",
        kategorie = "Methoden",
        beschreibung = "Gibt den kanonischen Wertevorrat einer Methode aus.",
        standardGröße = GraphGröße(250f, 105f),
        anschlüsse = listOf(
            eingang("methode", MathematikAnschlussArten.Methode.id),
            ausgang("menge", MathematikAnschlussArten.Menge.id),
        ),
    )

    val MethodenArgumente = KnotenVorlage(
        art = METHODEN_ARGUMENTE_ART,
        name = "Methodenargumente",
        kategorie = "Methoden",
        beschreibung = "Liest Namen und Wertevorräte der geordneten Methodenargumente strukturiert aus.",
        standardGröße = GraphGröße(280f, 115f),
        anschlüsse = listOf(
            eingang("methode", MathematikAnschlussArten.Methode.id),
            ausgang("argumente", MathematikAnschlussArten.Tupel.id),
        ),
        standardParameter = mapOf(
            METHODEN_ARGUMENTE_PROJEKTION to METHODEN_ARGUMENTPROJEKTION_TUPEL,
        ),
    )

    /** Historische Vorlage bleibt nur als Ladevertrag erhalten und wird nicht mehr katalogisiert. */
    val MethodenArgumentanzahl = KnotenVorlage(
        art = METHODEN_ARGUMENTANZAHL_ART,
        name = "Methoden-Argumentanzahl",
        kategorie = "Methoden",
        beschreibung = "Historischer Kompatibilitätsknoten für gespeicherte Karten.",
        standardGröße = GraphGröße(270f, 105f),
        anschlüsse = listOf(
            eingang("methode", MathematikAnschlussArten.Methode.id),
            ausgang("anzahl", MathematikAnschlussArten.Zahl.id),
        ),
    )

    val alle = listOf(
        MethodeAufrufen,
        MethodenAnwendungZahl,
        MethodenAnwendungAussage,
        MethodenAnwendungMenge,
        MethodenAnwendungObjekt,
        MethodenZielmenge,
        MethodenWertevorrat,
        MethodenArgumente,
    )

    private fun methodenAnwendung(
        name: String,
        methodeArt: AnschlussArtId,
        ergebnisArt: AnschlussArtId,
    ) = KnotenVorlage(
        art = METHODEN_ANWENDUNG_ART,
        name = name,
        kategorie = "Methoden",
        beschreibung = "Wendet eine einwertige Methode auf ein Argument an.",
        standardGröße = GraphGröße(265f, 115f),
        anschlüsse = listOf(
            eingang("methode", methodeArt, 0),
            eingang("argument", MathematikAnschlussArten.Objekt.id, 1),
            ausgang("wert", ergebnisArt),
        ),
        standardParameter = mapOf(METHODEN_ANWENDUNG_ERGEBNIS_ART to ergebnisArt.wert),
    )
}

data class FaltungsPaar(
    val konstruktor: KnotenDaten,
    val definator: KnotenDaten,
    val paarId: String,
)

fun erzeugeFaltungsPaar(
    position: GraphPunkt,
    operator: String = "summe",
    wertArt: AnschlussArtId = MathematikAnschlussArten.Zahl.id,
): FaltungsPaar {
    val paarId = UUID.randomUUID().toString()
    val konstruktorBasis = FaltungsKnotenVorlagen.Faltungskonstruktor.erzeuge(position)
    val konstruktor = konstruktorBasis.copy(
        parameter = konstruktorBasis.parameter + mapOf(FALTUNG_PAAR to paarId, FALTUNG_OPERATOR to operator),
        anschlüsse = konstruktorBasis.anschlüsse.map { anschluss ->
            if (anschluss.name in setOf("neutral", "akkumulator")) anschluss.copy(art = wertArt) else anschluss
        },
    )
    val definatorBasis = FaltungsKnotenVorlagen.Faltungsdefinator.erzeuge(position + GraphPunkt(430f, 0f))
    val definator = definatorBasis.copy(
        parameter = definatorBasis.parameter + mapOf(FALTUNG_PAAR to paarId, FALTUNG_OPERATOR to operator),
        anschlüsse = definatorBasis.anschlüsse.map { it.copy(art = wertArt) },
    )
    return FaltungsPaar(konstruktor, definator, paarId)
}

fun KnotenDaten.faltungsPaarId(): String? =
    parameter[FALTUNG_PAAR]?.trim()?.takeIf(String::isNotEmpty)

fun KnotenDaten.istFaltungsEndpunkt(): Boolean =
    art == FALTUNGSKONSTRUKTOR_ART || art == FALTUNGSDEFINATOR_ART

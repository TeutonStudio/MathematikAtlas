package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

internal fun dynamischesKonzeptFürKnoten(zustand: AtlasZustand, knoten: KnotenDaten): KonzeptDefinition? {
    kleineIssuesKonzeptFürKnoten(knoten)?.let { return it }
    if (knoten.art == "mathematik.endlicheMenge") return endlicheMengeKonzept(knoten)
    if (knoten.art == MENGEN_KNOTEN_ART) {
        val verweis = knoten.kartenVerweis ?: return null
        val karte = zustand.speicher.lade(verweis)
            ?: return fehlendesKartenKonzept(knoten, "Die referenzierte Kartenversion ${verweis.version} ist nicht mehr vorhanden.")
        return KonzeptDefinition(
            id = KonzeptId("menge-karte-${verweis.kartenId.wert}-${verweis.version}"),
            name = karte.name,
            beschreibung = "Eigene Mengendefinition als versionsfeste Kartenreferenz ${verweis.kartenId.wert}, Version ${verweis.version}.",
            pfad = listOf("Mengen", "Eigene Karten"),
            tags = setOf("Menge", "Eigene Karte", "Version ${verweis.version}"),
            knotenArten = setOf(MENGEN_KNOTEN_ART),
            reiter = listOf(KonzeptReiter("definition", "Definition · v${verweis.version}", KonzeptReiterRolle.Definition, karte)),
        )
    }
    if (knoten.art == ZAHLENRECHNER_ART) {
        when (UniversellerZahlenOperator.vonId(knoten.parameter[ZAHLENRECHNER_OPERATOR])) {
            UniversellerZahlenOperator.QUADRAT -> return iterierteMultiplikationsDefinition(knoten, 2)
            UniversellerZahlenOperator.KUBIK -> return iterierteMultiplikationsDefinition(knoten, 3)
            else -> Unit
        }
    }
    if (istZahlenRechnerFormel(knoten)) return zahlenRechnerFormelKonzept(knoten)
    val familie = StrukturRechnerKnotenFamilie.fuerKnotenArt(knoten.art)
    if (familie != null && knoten.parameter[RECHNER_OPERATOR_PARAMETER] == familie.formelOperatorId) {
        return strukturRechnerKonzept(knoten, familie)
    }
    return null
}

/**
 * Quadrat und Kubik werden nicht durch eine zweite Formelbeschreibung erklärt,
 * sondern durch den produktiven Multiplikationsknoten selbst. Die Karte lässt sich
 * kopieren und auswerten; derselbe Eingang x wird zwei- beziehungsweise dreimal in
 * die geordnete Multiplikation geführt.
 */
private fun iterierteMultiplikationsDefinition(
    ursprung: KnotenDaten,
    faktorAnzahl: Int,
): KonzeptDefinition {
    require(faktorAnzahl in setOf(2, 3))
    val titel = if (faktorAnzahl == 2) "Quadrat" else "Kubik"
    val exponent = if (faktorAnzahl == 2) "2" else "3"
    val latex = "x^{$exponent}=x" + "\\cdot x".repeat(faktorAnzahl - 1)
    return KonzeptDefinition(
        id = KonzeptId("zahlenrechner-${titel.lowercase()}-iterierte-multiplikation"),
        name = titel,
        beschreibung = "$titel ist die ${faktorAnzahl}-fache Multiplikation desselben Arguments.",
        pfad = listOf("Algebra", "Operationen"),
        tags = setOf(titel, "Multiplikation", "Potenz", latex),
        knotenArten = setOf(ZAHLENRECHNER_ART),
        knotenParameter = mapOf(
            ZAHLENRECHNER_OPERATOR to ursprung.parameter[ZAHLENRECHNER_OPERATOR].orEmpty(),
        ),
        reiter = listOf(
            KonzeptReiter(
                id = "definition",
                titel = latex,
                rolle = KonzeptReiterRolle.Definition,
                karte = iterierteMultiplikationsKarte(titel, faktorAnzahl),
            ),
        ),
    )
}

private fun iterierteMultiplikationsKarte(titel: String, faktorAnzahl: Int): KartenDaten {
    val prefix = "definition-${titel.lowercase()}-multiplikation"
    val eingangId = KnotenId("$prefix-eingang-x")
    val eingangAnschlussId = AnschlussId("$prefix-eingang-x-wert")
    val eingang = KnotenDaten(
        id = eingangId,
        art = KonzeptKnotenArten.EINGANG,
        name = "x",
        position = GraphPunkt(50f, 85f),
        größe = GraphGröße(210f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = eingangAnschlussId,
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Zahl.id,
            ),
        ),
        parameter = mapOf(
            "typ" to MathematikAnschlussArten.Zahl.id.wert,
            "rolle" to "x",
        ),
    )

    val multiplikationsBasis = KnotenDaten(
        id = KnotenId("$prefix-multiplikation"),
        art = ZAHLENRECHNER_ART,
        name = "Multiplikation",
        position = GraphPunkt(355f, 65f),
        größe = GraphGröße(270f, 90f + faktorAnzahl * 24f),
    )
    val multiplikation = konfiguriereZahlenRechner(
        knoten = multiplikationsBasis,
        operator = UniversellerZahlenOperator.MULTIPLIKATION,
        festeEingänge = faktorAnzahl,
    )
    val faktorEingänge = multiplikation.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
    val produktAusgang = multiplikation.anschlüsse.first { it.richtung == AnschlussRichtung.Ausgang }

    val ausgangId = KnotenId("$prefix-ausgang")
    val ausgangAnschlussId = AnschlussId("$prefix-ausgang-wert")
    val ausgang = KnotenDaten(
        id = ausgangId,
        art = KonzeptKnotenArten.AUSGANG,
        name = if (faktorAnzahl == 2) "x²" else "x³",
        position = GraphPunkt(720f, 85f),
        größe = GraphGröße(210f, 92f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = ausgangAnschlussId,
                name = "wert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Zahl.id,
            ),
        ),
        parameter = mapOf(
            "typ" to MathematikAnschlussArten.Zahl.id.wert,
            "rolle" to "wert",
        ),
    )

    val verbindungen = faktorEingänge.mapIndexed { index, faktor ->
        VerbindungDaten(
            id = VerbindungsId("$prefix-faktor-$index"),
            von = AnschlussVerweis(eingang.id, eingangAnschlussId),
            zu = AnschlussVerweis(multiplikation.id, faktor.id),
        )
    } + VerbindungDaten(
        id = VerbindungsId("$prefix-ergebnis"),
        von = AnschlussVerweis(multiplikation.id, produktAusgang.id),
        zu = AnschlussVerweis(ausgang.id, ausgangAnschlussId),
    )

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition: $titel",
        knoten = listOf(eingang, multiplikation, ausgang),
        verbindungen = verbindungen,
    )
}

private fun fehlendesKartenKonzept(knoten: KnotenDaten, fehler: String): KonzeptDefinition = KonzeptDefinition(
    id = KonzeptId("menge-fehlende-karte-${knoten.id.wert}"),
    name = knoten.name,
    beschreibung = fehler,
    pfad = listOf("Mengen", "Eigene Karten"),
    tags = setOf("Menge", "Eigene Karte", "Fehlende Version"),
    knotenArten = setOf(MENGEN_KNOTEN_ART),
    reiter = listOf(
        KonzeptReiter(
            id = "fehler",
            titel = "Fehlende Version",
            rolle = KonzeptReiterRolle.Definition,
            karte = KartenDaten(
                id = KartenId("fehlende-mengenkarte-${knoten.id.wert}"),
                name = "Fehlende Mengendefinition",
                knoten = listOf(
                    KnotenDaten(
                        id = KnotenId("fehlende-mengenkarte-${knoten.id.wert}-regel"),
                        art = KonzeptKnotenArten.REGEL,
                        name = "Referenz nicht auflösbar",
                        position = GraphPunkt(70f, 65f),
                        größe = GraphGröße(620f, 210f),
                        parameter = mapOf("regel" to fehler, "knotenArt" to MENGEN_KNOTEN_ART),
                    ),
                ),
            ),
        ),
    ),
)

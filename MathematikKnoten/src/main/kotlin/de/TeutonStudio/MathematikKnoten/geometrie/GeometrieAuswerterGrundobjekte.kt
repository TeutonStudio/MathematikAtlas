package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

internal fun MathematikAuswerterRegister.registriereGeometrieGrundobjekte() {
    registriere("mathematik.geometrie.raum") { k ->
        val name = k.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "E" }
        val dimension = k.knoten.parameter["dimension"]?.toIntOrNull()?.takeIf { it > 0 }
            ?: error("Die Raumdimension muss eine positive ganze Zahl sein.")
        geometrieErgebnis("raum", EuklidischerRaum(name, dimension))
    }
    registriere("mathematik.geometrie.standardKoordinatensystem") { k ->
        val name = k.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "K" }
        geometrieErgebnis("system", GeometrischesKoordinatensystem(k.geometrieRaum("raum"), name), k)
    }
    registriere("mathematik.geometrie.punktAusKoordinaten") { k ->
        val system = k.geometrieSystem("system")
        val name = k.knoten.parameter["name"]?.trim().orEmpty().ifBlank { "A" }
        geometrieErgebnis("punkt", GeometriePunkt(name, system.raum, k.geometrieTupel("koordinaten")), k)
    }
    registriere("mathematik.geometrie.geradeDurchPunkte") { k ->
        val a = k.geometriePunkt("a")
        val b = k.geometriePunkt("b")
        KnotenAuswertungsErgebnis(mapOf(
            "gerade" to BedingterWert(GeometrieGerade(a, b), k.geometrieAnnahmen()),
            "punkteIdentisch" to BedingterWert(GeometrischeGleichheit(a, b), k.geometrieAnnahmen()),
        ))
    }
    registriere("mathematik.geometrie.strecke") { k ->
        val a = k.geometriePunkt("anfang")
        val b = k.geometriePunkt("ende")
        KnotenAuswertungsErgebnis(mapOf(
            "strecke" to BedingterWert(GeometrieStrecke(a, b), k.geometrieAnnahmen()),
            "entartet" to BedingterWert(GeometrischeGleichheit(a, b), k.geometrieAnnahmen()),
        ))
    }
    registriere("mathematik.geometrie.strahl") { k ->
        val a = k.geometriePunkt("ursprung")
        val b = k.geometriePunkt("richtung")
        KnotenAuswertungsErgebnis(mapOf(
            "strahl" to BedingterWert(GeometrieStrahl(a, b), k.geometrieAnnahmen()),
            "entartet" to BedingterWert(GeometrischeGleichheit(a, b), k.geometrieAnnahmen()),
        ))
    }
    registriere("mathematik.geometrie.winkel") { k ->
        geometrieErgebnis("winkel", GeometrieWinkel(
            k.geometriePunkt("a"),
            k.geometriePunkt("scheitel"),
            k.geometriePunkt("c"),
            k.knoten.parameter["orientiert"]?.toBooleanStrictOrNull() ?: false,
        ), k)
    }
    registriere("mathematik.geometrie.kreislinie") { k ->
        val mitte = k.geometriePunkt("mittelpunkt")
        val rand = k.geometriePunkt("randpunkt")
        KnotenAuswertungsErgebnis(mapOf(
            "kreis" to BedingterWert(GeometrieKreislinie(mitte, rand), k.geometrieAnnahmen()),
            "radiusNull" to BedingterWert(GeometrischeGleichheit(mitte, rand), k.geometrieAnnahmen()),
        ))
    }
    registriere("mathematik.geometrie.polygon") { k ->
        geometrieErgebnis("polygon", GeometriePolygon(listOf(
            k.geometriePunkt("a"), k.geometriePunkt("b"), k.geometriePunkt("c"),
        )), k)
    }
    registriere("mathematik.geometrie.dreieck") { k ->
        val angaben = DreiecksWerte(
            a = k.optionaleZahl("a"),
            b = k.optionaleZahl("b"),
            c = k.optionaleZahl("c"),
            alpha = k.optionaleZahl("α"),
            beta = k.optionaleZahl("β"),
            gamma = k.optionaleZahl("γ"),
        )
        val lösung = löseDreieck(angaben)
        val zusätzlicheAnnahmen = when (lösung) {
            is DreiecksLösung.Vollständig -> lösung.annahmen
            is DreiecksLösung.Unentscheidbar -> lösung.annahmen
            else -> lösung.werte.notwendigeDreiecksAnnahmen()
        }
        val ausgaben = linkedMapOf<String, BedingterWert>()
        lösung.werte.a?.let { ausgaben["a"] = k.dreiecksWert(it, zusätzlicheAnnahmen) }
        lösung.werte.b?.let { ausgaben["b"] = k.dreiecksWert(it, zusätzlicheAnnahmen) }
        lösung.werte.c?.let { ausgaben["c"] = k.dreiecksWert(it, zusätzlicheAnnahmen) }
        lösung.werte.alpha?.let { ausgaben["α"] = k.dreiecksWert(it, zusätzlicheAnnahmen) }
        lösung.werte.beta?.let { ausgaben["β"] = k.dreiecksWert(it, zusätzlicheAnnahmen) }
        lösung.werte.gamma?.let { ausgaben["γ"] = k.dreiecksWert(it, zusätzlicheAnnahmen) }
        if (lösung is DreiecksLösung.Vollständig) {
            ausgaben["dreieck"] = k.dreiecksWert(lösung.dreieck, zusätzlicheAnnahmen)
        }

        KnotenAuswertungsErgebnis(
            ausgaben = ausgaben,
            fehler = (lösung as? DreiecksLösung.Ungültig)?.grund,
            eingänge = k.eingänge,
            warnungen = when (lösung) {
                is DreiecksLösung.Partiell -> listOf(lösung.hinweis)
                is DreiecksLösung.Mehrdeutig -> listOf("Die Angaben bestimmen zwei mögliche Dreiecke.")
                is DreiecksLösung.Unentscheidbar -> listOf(lösung.grund)
                else -> emptyList()
            },
        )
    }
    registriere("mathematik.geometrie.gruppe") { k ->
        geometrieErgebnis("gruppe", GeometrieGruppe(listOf(k.geometrieObjekt("a"), k.geometrieObjekt("b"))), k)
    }
    registriere("mathematik.geometrie.zuStruktur") { k ->
        geometrieErgebnis("struktur", strukturVon(k.geometrieObjekt("objekt")), k)
    }
    registriere("mathematik.geometrie.zuTrägermenge") { k ->
        geometrieErgebnis("menge", GeometrischeTrägermenge(k.geometrieObjekt("objekt")), k)
    }
    registriere("mathematik.geometrie.koordinatenBild") { k ->
        geometrieErgebnis("menge", KoordinatenBild(k.geometrieObjekt("objekt"), k.geometrieSystem("system")), k)
    }
    registriere("mathematik.geometrie.visualisierung") { k ->
        val wert = k.eingänge["objekt"] ?: error("Geometrieeingang fehlt.")
        require(wert.objekt is GeometrischerAusdruck)
        KnotenAuswertungsErgebnis(mapOf("objekt" to wert))
    }
}

private fun KnotenAuswertungsKontext.optionaleZahl(name: String): ZahlAusdruck? =
    eingänge[name]?.objekt as? ZahlAusdruck

private fun KnotenAuswertungsKontext.dreiecksWert(
    objekt: MathematischesObjekt,
    zusätzlicheAnnahmen: Set<Aussage>,
): BedingterWert {
    val werte = eingänge.values
    return BedingterWert(
        objekt = objekt,
        annahmen = geometrieAnnahmen() + zusätzlicheAnnahmen,
        reelleVariablen = werte.flatMap { it.reelleVariablen.entries }.associate { it.toPair() },
        variablenQuellen = werte.flatMap { it.variablenQuellen }.geordnetEindeutig(),
    )
}

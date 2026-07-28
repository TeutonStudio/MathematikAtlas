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

package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.FachKatalog
import de.TeutonStudio.MathematikKnoten.enzyklopädie.VariantenId
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensKartenReferenz
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensKartenRolle
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensReifegrad

internal fun KnotenVorlage.stabileKonzeptId(): String {
    val variantenSchlüssel = standardParameter.toSortedMap()
        .entries.joinToString(";") { (schlüssel, wert) -> "$schlüssel=$wert" }
    return listOf(art, name, variantenSchlüssel).joinToString("|")
}

internal fun KnotenVorlage.stabileVariantenId(): VariantenId = VariantenId(
    buildString {
        append(art)
        standardParameter.toSortedMap().forEach { (schlüssel, wert) ->
            append('|').append(schlüssel).append('=').append(wert)
        }
        append('|').append(name)
    },
)

private fun KnotenVorlage.wissensBeschreibung(): String =
    beschreibung.trim().ifBlank {
        "Mathematisches Konzept „$name“ der Knotenart $art."
    }

private fun Iterable<String>.bereinigteSuchbegriffe(): Set<String> =
    map(String::trim).filter(String::isNotBlank).toCollection(linkedSetOf())

private fun KnotenVorlage.suchbegriffe(): Set<String> = buildList {
    add(art)
    add(name)
    add(kategorie)
    add(wissensBeschreibung())
    addAll(standardParameter.keys)
    addAll(standardParameter.values)
    addAll(anschlüsse.map { it.name })
    addAll(anschlüsse.map { it.art.wert })
}.bereinigteSuchbegriffe()

internal fun einzelnesVorlagenKonzept(
    vorlage: KnotenVorlage,
    id: WissensId = WissensId(vorlage.stabileKonzeptId()),
    generatorId: String = "konzeptkarte.generisch",
): WissensEintrag = WissensEintrag(
    id = id,
    titel = vorlage.name,
    kurzbeschreibung = vorlage.wissensBeschreibung(),
    fachPfade = FachKatalog.fürVorlage(
        art = vorlage.art,
        name = vorlage.name,
        kategorie = vorlage.kategorie,
        besitztKartenVerweis = vorlage.kartenVerweis != null,
    ),
    suchbegriffe = vorlage.suchbegriffe(),
    reifegrad = WissensReifegrad.Geprüft,
    knotenArten = setOf(vorlage.art),
    varianten = setOf(vorlage.stabileVariantenId()),
    knotenVorlagen = listOf(vorlage),
    karten = listOf(
        WissensKartenReferenz.Generator(
            id = "${id.wert}.definition",
            generatorId = generatorId,
            rolle = WissensKartenRolle.Definition,
            primär = true,
        ),
    ),
)

internal fun gruppiertesVorlagenKonzept(
    id: WissensId,
    titel: String,
    beschreibung: String,
    vorlagen: List<KnotenVorlage>,
    generatorId: String,
    assetDatei: String? = null,
    zusätzlicheSuchbegriffe: Set<String> = emptySet(),
): WissensEintrag {
    require(vorlagen.isNotEmpty()) { "$id benötigt mindestens eine Knotenvorlage." }
    val definition = assetDatei?.let { datei ->
        WissensKartenReferenz.Asset(
            id = id.wert,
            datei = datei,
            formatVersion = KartenDatenJson.FORMAT_VERSION,
            rolle = WissensKartenRolle.Definition,
            primär = true,
        )
    } ?: WissensKartenReferenz.Generator(
        id = "${id.wert}.definition",
        generatorId = generatorId,
        rolle = WissensKartenRolle.Definition,
        primär = true,
    )
    return WissensEintrag(
        id = id,
        titel = titel,
        kurzbeschreibung = beschreibung,
        fachPfade = vorlagen.flatMap { vorlage ->
            FachKatalog.fürVorlage(
                art = vorlage.art,
                name = vorlage.name,
                kategorie = vorlage.kategorie,
                besitztKartenVerweis = vorlage.kartenVerweis != null,
            )
        }.toSet(),
        suchbegriffe = (
            zusätzlicheSuchbegriffe +
                vorlagen.flatMap { it.suchbegriffe() }
            ).bereinigteSuchbegriffe(),
        aliase = vorlagen.mapTo(linkedSetOf(), KnotenVorlage::stabileKonzeptId),
        reifegrad = WissensReifegrad.Geprüft,
        knotenArten = vorlagen.map(KnotenVorlage::art).toSet(),
        varianten = vorlagen.mapTo(linkedSetOf(), KnotenVorlage::stabileVariantenId),
        knotenVorlagen = vorlagen,
        karten = listOf(definition),
    )
}

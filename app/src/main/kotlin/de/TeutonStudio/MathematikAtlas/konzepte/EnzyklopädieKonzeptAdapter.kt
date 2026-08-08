package de.TeutonStudio.MathematikAtlas

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.MENGEN_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.istZahlenRechnerFormel
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.*
import de.TeutonStudio.MathematikKnoten.konzeptknoten.stabileVariantenId

private const val KONZEPTKARTEN_ASSET_PFAD = "de/TeutonStudio/MathematikKnoten/konzeptkarte"

internal class AndroidKonzeptKartenQuelle(context: Context) : KonzeptKartenQuelle {
    private val assets = context.applicationContext.assets
    override fun lese(pfad: String): String? = runCatching {
        assets.open(pfad).bufferedReader().use { it.readText() }
    }.getOrNull()

    fun ladeDirekt(asset: WissensKartenReferenz.Asset): KartenDaten? = runCatching {
        val text = lese("$KONZEPTKARTEN_ASSET_PFAD/${asset.datei}") ?: return@runCatching null
        if (KartenDatenJson.formatVersion(text) != asset.formatVersion) return@runCatching null
        KartenDatenJson.lese(text).takeIf { it.id.wert == asset.id }
    }.getOrNull()
}

internal fun enzyklopädieKonzeptFürKnoten(context: Context, knoten: KnotenDaten): KonzeptDefinition? {
    if (istZahlenRechnerFormel(knoten)) return null
    if (knoten.art == MENGEN_KNOTEN_ART && knoten.kartenVerweis != null) return null
    val wissen = findeWissensEintrag(knoten) ?: return null
    val vorlage = passendeVorlage(wissen, knoten) ?: return null
    val variantenId = vorlage.stabileVariantenId()
    val deklarierteAssets = wissen.karten
        .filterIsInstance<WissensKartenReferenz.Asset>()
        .filter { asset -> asset.varianten.isEmpty() || variantenId in asset.varianten }
    val assets = deklarierteAssets.ifEmpty { StatischeKonzeptKarten.fürVariante(variantenId) }
    if (assets.isEmpty()) return null
    val quelle = AndroidKonzeptKartenQuelle(context)
    val statischeIds = StatischeKonzeptKarten.alle.mapTo(hashSetOf()) { it.id }
    val manifest = if (assets.any { it.id in statischeIds }) quelle.ladeManifest().getOrNull() else null
    val lader = manifest?.let { KonzeptKartenLader(quelle, it) }
    val geladene = assets.associateWith { asset ->
        if (asset.id in statischeIds) {
            val ergebnis = lader?.lade(KonzeptKartenId(asset.id)) ?: return null
            when (ergebnis) {
                is KonzeptKartenLadeErgebnis.Erfolg -> ergebnis.karte
                is KonzeptKartenLadeErgebnis.Fehler -> return null
            }
        } else {
            quelle.ladeDirekt(asset) ?: return null
        }
    }
    val basis = assets.filter { it.darstellung == null }.map { asset ->
        val varianten = assets.filter { kandidat ->
            kandidat.darstellungsGruppe != null &&
                kandidat.darstellungsGruppe == asset.darstellungsGruppe &&
                kandidat.darstellung != null
        }.mapNotNull { kandidat ->
            runCatching { KomplexDarstellung.valueOf(kandidat.darstellung!!) }.getOrNull()
                ?.let { it to geladene.getValue(kandidat) }
        }.toMap()
        KonzeptReiter(
            id = asset.id,
            titel = asset.titel,
            rolle = asset.rolle.zuAppRolle(),
            karte = geladene.getValue(asset),
            darstellungsVarianten = varianten,
        )
    }
    if (basis.count { it.rolle == KonzeptReiterRolle.Definition } != 1) return null
    return KonzeptDefinition(
        id = KonzeptId(wissen.id.wert),
        name = wissen.titel,
        beschreibung = wissen.kurzbeschreibung,
        pfad = wissen.fachPfade.minBy { it.stabileId }.segmente,
        tags = wissen.alleSuchtexte,
        knotenArten = wissen.knotenArten,
        knotenParameter = vorlage.standardParameter,
        reiter = basis,
    )
}

internal fun alleEnzyklopädieKonzepte(context: Context): List<KonzeptDefinition> =
    MathematikEnzyklopädie.standard.alle.mapNotNull { wissen ->
        wissen.knotenVorlagen.firstNotNullOfOrNull { vorlage ->
            enzyklopädieKonzeptFürKnoten(context, vorlage.erzeuge(de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt.Zero))
        }
    }

private fun WissensKartenRolle.zuAppRolle(): KonzeptReiterRolle = when (this) {
    WissensKartenRolle.Definition -> KonzeptReiterRolle.Definition
    WissensKartenRolle.Spezialfall -> KonzeptReiterRolle.Spezialfall
    WissensKartenRolle.Beispiel -> KonzeptReiterRolle.Beispiel
    WissensKartenRolle.Äquivalenz -> KonzeptReiterRolle.Äquivalenz
}

private fun passendeVorlage(wissen: WissensEintrag, knoten: KnotenDaten) =
    wissen.knotenVorlagen.firstOrNull { vorlage ->
        vorlage.art == knoten.art && vorlage.standardParameter.all { (schlüssel, wert) -> knoten.parameter[schlüssel] == wert }
    } ?: wissen.knotenVorlagen.singleOrNull { it.art == knoten.art }

private fun findeWissensEintrag(knoten: KnotenDaten): WissensEintrag? {
    val kandidaten = MathematikEnzyklopädie.standard.fürKnotenArt(knoten.art)
    return kandidaten.firstOrNull { passendeVorlage(it, knoten) != null } ?: kandidaten.singleOrNull()
}

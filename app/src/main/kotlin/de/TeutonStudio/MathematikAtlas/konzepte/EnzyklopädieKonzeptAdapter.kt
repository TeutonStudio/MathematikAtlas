package de.TeutonStudio.MathematikAtlas

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.MENGEN_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.istZahlenRechnerFormel
import de.TeutonStudio.MathematikKnoten.enzyklopädie.*
import de.TeutonStudio.MathematikKnoten.konzeptkarte.*
import de.TeutonStudio.MathematikKnoten.konzeptknoten.stabileVariantenId

internal class AndroidKonzeptKartenQuelle(context: Context) : KonzeptKartenQuelle {
    private val assets = context.applicationContext.assets
    override fun lese(pfad: String): String? = runCatching {
        assets.open(pfad).bufferedReader().use { it.readText() }
    }.getOrNull()
}

internal fun enzyklopädieKonzeptFürKnoten(context: Context, knoten: KnotenDaten): KonzeptDefinition? {
    if (istZahlenRechnerFormel(knoten)) return null
    if (knoten.art == MENGEN_KNOTEN_ART && knoten.kartenVerweis != null) return null
    val wissen = findeWissensEintrag(knoten) ?: return null
    val vorlage = passendeVorlage(wissen, knoten) ?: return null
    val assets = StatischeKonzeptKarten.fürVariante(vorlage.stabileVariantenId())
    if (assets.isEmpty()) return null
    val quelle = AndroidKonzeptKartenQuelle(context)
    val manifest = quelle.ladeManifest().getOrNull() ?: return null
    val lader = KonzeptKartenLader(quelle, manifest)
    val geladene = assets.associateWith { asset ->
        when (val ergebnis = lader.lade(KonzeptKartenId(asset.id))) {
            is KonzeptKartenLadeErgebnis.Erfolg -> ergebnis.karte
            is KonzeptKartenLadeErgebnis.Fehler -> return null
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

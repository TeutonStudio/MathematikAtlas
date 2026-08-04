package de.TeutonStudio.MathematikAtlas

import android.content.Context
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKnoten.MENGEN_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.istZahlenRechnerFormel
import de.TeutonStudio.MathematikKnoten.enzyklopädie.MathematikEnzyklopädie
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensKartenReferenz
import de.TeutonStudio.MathematikKnoten.konzeptkarte.KonzeptKartenId
import de.TeutonStudio.MathematikKnoten.konzeptkarte.KonzeptKartenLadeErgebnis
import de.TeutonStudio.MathematikKnoten.konzeptkarte.KonzeptKartenLader
import de.TeutonStudio.MathematikKnoten.konzeptkarte.KonzeptKartenQuelle

internal class AndroidKonzeptKartenQuelle(context: Context) : KonzeptKartenQuelle {
    private val assets = context.applicationContext.assets

    override fun lese(pfad: String): String? = runCatching {
        assets.open(pfad).bufferedReader().use { it.readText() }
    }.getOrNull()
}

internal fun enzyklopädieKonzeptFürKnoten(
    context: Context,
    knoten: KnotenDaten,
): KonzeptDefinition? {
    if (istZahlenRechnerFormel(knoten)) return null
    if (knoten.art == MENGEN_KNOTEN_ART && knoten.kartenVerweis != null) return null

    val wissen = findeWissensEintrag(knoten) ?: return null
    val asset = wissen.karten.filterIsInstance<WissensKartenReferenz.Asset>()
        .singleOrNull { it.primär }
        ?: return null
    val karte = when (
        val ergebnis = KonzeptKartenLader(AndroidKonzeptKartenQuelle(context)).lade(KonzeptKartenId(asset.id))
    ) {
        is KonzeptKartenLadeErgebnis.Erfolg -> ergebnis.karte
        is KonzeptKartenLadeErgebnis.Fehler -> return null
    }
    val passendeVorlage = wissen.knotenVorlagen.firstOrNull { vorlage ->
        vorlage.art == knoten.art && vorlage.standardParameter.all { (schlüssel, wert) ->
            knoten.parameter[schlüssel] == wert
        }
    } ?: wissen.knotenVorlagen.firstOrNull { it.art == knoten.art }

    return KonzeptDefinition(
        id = KonzeptId(wissen.id.wert),
        name = wissen.titel,
        beschreibung = wissen.kurzbeschreibung,
        pfad = wissen.fachPfade.minBy { it.stabileId }.segmente,
        tags = wissen.alleSuchtexte,
        knotenArten = wissen.knotenArten,
        knotenParameter = passendeVorlage?.standardParameter.orEmpty(),
        reiter = listOf(
            KonzeptReiter(
                id = "definition-json",
                titel = "Definition",
                rolle = KonzeptReiterRolle.Definition,
                karte = karte,
            ),
        ),
    )
}

internal fun kombiniereEnzyklopädieUndSpezialkonzept(
    enzyklopädie: KonzeptDefinition?,
    spezial: KonzeptDefinition?,
): KonzeptDefinition? = when {
    enzyklopädie == null -> spezial
    spezial == null -> enzyklopädie
    enzyklopädie.id == spezial.id && enzyklopädie.reiter == spezial.reiter -> enzyklopädie
    else -> enzyklopädie.copy(
        reiter = enzyklopädie.reiter + spezial.reiter.mapIndexed { index, reiter ->
            reiter.copy(
                id = "spezial-${reiter.id}-$index",
                rolle = if (reiter.rolle == KonzeptReiterRolle.Definition) {
                    KonzeptReiterRolle.Spezialfall
                } else {
                    reiter.rolle
                },
            )
        },
    )
}

private fun findeWissensEintrag(knoten: KnotenDaten): WissensEintrag? {
    val kandidaten = MathematikEnzyklopädie.standard.fürKnotenArt(knoten.art)
    return kandidaten.firstOrNull { wissen ->
        wissen.knotenVorlagen.any { vorlage ->
            vorlage.art == knoten.art && vorlage.standardParameter.all { (schlüssel, wert) ->
                knoten.parameter[schlüssel] == wert
            }
        }
    } ?: kandidaten.singleOrNull()
}

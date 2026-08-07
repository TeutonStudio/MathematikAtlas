package de.TeutonStudio.MathematikAtlas

import android.content.Context
import de.TeutonStudio.MathematikAtlas.speicher.KartenSpeicher
import de.TeutonStudio.MathematikAtlas.speicher.StandardKartenInstallationsBericht
import de.TeutonStudio.MathematikAtlas.speicher.StandardKartenInstaller
import de.TeutonStudio.MathematikKnoten.GeometrieKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen

internal fun installiereStandardkarten(
    context: Context,
    speicher: KartenSpeicher,
): StandardKartenInstallationsBericht {
    val bekannteArten = buildSet {
        alleMathematikKnotenVorlagen().mapTo(this) { it.art }
        MengenraumKnotenVorlagen.alle.mapTo(this) { it.art }
        GeometrieKnotenVorlagen.alle.mapTo(this) { it.art }
        KartenWerkzeugVorlagen.alle.mapTo(this) { it.art }
        add(MathematikKnotenVorlagen.KartenEingang.art)
        add(MathematikKnotenVorlagen.KartenAusgang.art)
        add("mathematik.gruppe")
    }
    return StandardKartenInstaller(
        context = context,
        speicher = speicher,
        bekannteKnotenArten = bekannteArten,
    ).installiere()
}

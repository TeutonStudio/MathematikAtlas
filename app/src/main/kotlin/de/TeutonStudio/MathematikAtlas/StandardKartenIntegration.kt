package de.TeutonStudio.MathematikAtlas

import android.content.Context
import de.TeutonStudio.MathematikAtlas.speicher.KartenSpeicher
import de.TeutonStudio.MathematikAtlas.speicher.StandardKartenInstallationsBericht
import de.TeutonStudio.MathematikAtlas.speicher.StandardKartenInstaller
import de.TeutonStudio.MathematikKnoten.GeometrieKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.MengenraumKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.historischeMengenKnotenArten
import de.TeutonStudio.MathematikKnoten.historischeSkalarproduktArten
import de.TeutonStudio.MathematikKnoten.historischeZahlenRechnerArten

/**
 * Knotentypen, die gebündelte Standardkarten verwenden dürfen.
 *
 * Entscheidend ist hier Lade-Kompatibilität, nicht Sichtbarkeit im Erstellen-Dialog:
 * historische Typen werden beim Laden auf ihre konsolidierten Nachfolger migriert und
 * müssen deshalb vom Standardkarten-Installer als unterstützt akzeptiert werden.
 */
internal fun bekannteStandardKartenKnotenArten(): Set<String> = buildSet {
    alleMathematikKnotenVorlagen().mapTo(this) { it.art }
    MengenraumKnotenVorlagen.alle.mapTo(this) { it.art }
    GeometrieKnotenVorlagen.alle.mapTo(this) { it.art }
    KartenWerkzeugVorlagen.alle.mapTo(this) { it.art }
    add(MathematikKnotenVorlagen.KartenEingang.art)
    add(MathematikKnotenVorlagen.KartenAusgang.art)
    add("mathematik.gruppe")

    // Diese Arten sind absichtlich nicht mehr im Erstellen-Dialog sichtbar, bleiben
    // aber durch die Karten-Lademigration vollständig unterstützt.
    addAll(historischeZahlenRechnerArten)
    addAll(historischeMengenKnotenArten)
    addAll(historischeSkalarproduktArten)
    add("mathematik.einheitsSpalte")
    add("mathematik.einheitsZeile")
}

internal fun installiereStandardkarten(
    context: Context,
    speicher: KartenSpeicher,
): StandardKartenInstallationsBericht = StandardKartenInstaller(
    context = context,
    speicher = speicher,
    bekannteKnotenArten = bekannteStandardKartenKnotenArten(),
).installiere()

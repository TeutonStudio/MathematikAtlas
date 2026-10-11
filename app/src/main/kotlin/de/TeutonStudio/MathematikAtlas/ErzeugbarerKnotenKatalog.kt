package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog

/** Statische Erstellen-Vorlagen; Karteninstanzen ergänzen den bestehenden Gruppentyp. */
internal fun statischeKnotenVorlagen(): List<KnotenVorlage> =
    KanonischerMathematikKnotenKatalog.alle() + MengenraumKnotenVorlagen.alle +
        GeometrieKnotenVorlagen.alle + KartenWerkzeugVorlagen.alle

internal fun erzeugbareKnotenArten(): Set<String> = statischeKnotenVorlagen().map { it.art }.toSet() + "mathematik.gruppe"

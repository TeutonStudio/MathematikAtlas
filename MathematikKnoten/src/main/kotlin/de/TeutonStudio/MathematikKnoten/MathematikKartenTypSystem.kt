package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeTypen
import de.TeutonStudio.TypSystem.StandardTypSystem
import de.TeutonStudio.TypSystem.TypSystem

/**
 * Verbindet die semantische Mathematik-Hierarchie mit den weiterhin gültigen
 * groben Anschlussarten. Godot kann später auf derselben neutralen Schnittstelle
 * zusätzliche nominale Typbeziehungen registrieren.
 */
fun erzeugeMathematikKartenTypSystem(arten: AnschlussArtRegister): TypSystem = StandardTypSystem(
    istAtomUntertyp = { von, erwartet ->
        MathematischeTypen.istAtomUntertyp(von, erwartet) ||
            arten.istUnterart(AnschlussArtId(von.wert), AnschlussArtId(erwartet.wert))
    },
    konstruktoren = MathematischeTypen.konstruktoren,
)

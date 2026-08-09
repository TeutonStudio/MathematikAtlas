package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Gemeinsamer Laufzeitvertrag für dynamisch typisierte Mathematikausgänge. */
fun anschlussArtFürMathematischesObjekt(objekt: MathematischesObjekt): AnschlussArtId = when (objekt) {
    is ZahlAusdruck -> MathematikAnschlussArten.Zahl.id
    is Aussage -> MathematikAnschlussArten.Aussage.id
    is MengenAusdruck -> MathematikAnschlussArten.Menge.id
    is SpaltenVektor -> MathematikAnschlussArten.SpaltenVektor.id
    is ZeilenVektor -> MathematikAnschlussArten.ZeilenVektor.id
    is Matrix -> MathematikAnschlussArten.Matrix.id
    is Tensor -> MathematikAnschlussArten.Tensor.id
    is Tupel -> MathematikAnschlussArten.Tupel.id
    is Methode -> MathematikAnschlussArten.Methode.id
    is TypisiertesElement -> AnschlussArtId(objekt.anschlussArt)
    else -> MathematikAnschlussArten.Objekt.id
}

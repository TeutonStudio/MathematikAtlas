package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypAusdruck

/** Gemeinsame Strukturinformation; Quellenrolle und symbolische Repräsentation bleiben beim Aufrufer. */
internal data class QuellenGrundraumInformation(
    val typ: TypAusdruck,
    val art: AnschlussArtId,
    val form: List<ZahlAusdruck>?,
)

internal fun quellenKomponentenRäume(raum:MengenAusdruck):List<MengenAusdruck>? {
    raum.kartesischeFaktorenOderNull()?.let {return it}
    if(raum !is EndlicheMenge || raum.elemente.isEmpty() || raum.elemente.any {it !is Tupel}) return null
    val tupel=raum.elemente.filterIsInstance<Tupel>()
    val n=tupel.first().elemente.size
    if(tupel.any {it.elemente.size!=n}) return null
    return List(n) {i -> EndlicheMenge(tupel.map {it.elemente[i]}.toSet())}
}

internal fun quellenGrundraumInformation(raum:MengenAusdruck):QuellenGrundraumInformation {
    val typ=raum.elementTypAusdruck()
    val form=when(raum) {
        is Vektorraum -> listOf(RationaleZahl.von(raum.dimension.toLong()))
        is Matrizenraum -> listOf(RationaleZahl.von(raum.zeilen.toLong()),RationaleZahl.von(raum.spalten.toLong()))
        is Tensorraum -> raum.dimensionen
        else -> quellenKomponentenRäume(raum)?.let {listOf(RationaleZahl.von(it.size.toLong()))}
    }
    return QuellenGrundraumInformation(typ,variablenAnschlussArt(typ),form?.takeUnless {achse -> achse.any {it==RationaleZahl.Null}})
}

internal fun allgemeinerParameterWert(k:KnotenDaten):BedingterWert {
    val name=k.parameter["name"]?.trim().orEmpty().ifBlank {"a"}
    val bereich=WertebereichKonfiguration.vonEigenschaft(k.eigenschaften[WertebereichKonfiguration.EIGENSCHAFT])
    val raum=bereich.zuMenge()
    val info=quellenGrundraumInformation(raum)
    // Historische Methodenbildung verwendete für allgemeine Parameter bereits die Knoten-ID.
    val id=k.id.wert
    val intern="__parameter:${k.id.wert}"
    val objekt:MathematischesObjekt=when(bereich) {
        WertebereichKonfiguration.Winkel -> WinkelParameter(name,bindungsId=id)
        is WertebereichKonfiguration.Zahl -> Variable(name,bindungsId=id)
        is WertebereichKonfiguration.Vektor,is WertebereichKonfiguration.Matrix -> TypisiertesElement(intern,info.art.wert,latex=name,strukturForm=info.form)
        else -> AllgemeinerParameter(name,bindungsId=id)
    }
    return BedingterWert(objekt,werteVorrat=raum,komponentenMengen=quellenKomponentenRäume(raum),
        variablenQuellen=listOf(VariablenQuelle(k.id,if(objekt is TypisiertesElement) intern else name,raum,bindungsId=id,bindungsName="wert",
            lösungsRolle=LösungsQuellenRolle.Parameter,strukturBindung=StrukturQuellenBindung(id,name,raum))))
}

internal fun aussagenKonstantenWert(wahr:Boolean)=BedingterWert(WahrheitsKonstante(wahr))

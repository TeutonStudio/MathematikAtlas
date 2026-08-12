package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypTragend

/** Ein unendlicher Hyperindex bleibt symbolisch und wird nie in Int oder Long gezwungen. */
data class HyperNatuerlicherIndex(
    val name: String,
    val unendlich: Boolean = true,
) {
    init { require(name.isNotBlank()) }
    fun zuLatex(): String = if (unendlich) "$name\\in{}^*\\mathbb N\\setminus\\mathbb N" else name
}

sealed interface UnendlicheIndexStruktur : MathematischesObjekt, TypTragend {
    val id: String
    val name: String
    val indexMenge: MengenAusdruck
    val zielMenge: MengenAusdruck
    val vorschrift: Methode
    val nachweislichKonstant: Boolean

    override val typAusdruck: TypAusdruck
        get() = TypAusdruck.Parameterisiert(
            MathematischeTypen.UnendlichesTupel,
            listOf(zielMenge.elementTypAusdruck()),
        )

    fun standardKomponente(index: ZahlAusdruck): MathematischesObjekt
}

private fun pruefeEinstelligeVorschrift(vorschrift: Methode) {
    require(vorschrift.parameter.size == 1) {
        "Eine unendliche Indexstruktur benötigt genau einen Indexparameter."
    }
}

/** Allgemeines, nicht materialisiertes Objekt-Tupel über ℕ={1,2,3,…}. */
data class UnnatuerlichesObjektTupel(
    override val id: String,
    override val name: String,
    override val zielMenge: MengenAusdruck,
    override val vorschrift: Methode,
    override val nachweislichKonstant: Boolean = false,
) : UnendlicheIndexStruktur, UnnatuerlichesTupel<MathematischesObjekt> {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        pruefeEinstelligeVorschrift(vorschrift)
    }

    override val indexMenge: MengenAusdruck = NatürlicheZahlen

    override fun komponente(index: Long): MathematischesObjekt {
        require(index >= 1L) { "Ein unnatürliches Tupel ist ab Index 1 definiert." }
        return standardKomponente(RationaleZahl.von(index))
    }

    override fun standardKomponente(index: ZahlAusdruck): MathematischesObjekt {
        require(index.istPositiverNatuerlicherIndex()) {
            "Unnatürliche Tupel verwenden die Projektkonvention ℕ={1,2,3,…}."
        }
        return vorschrift.wendeAn(listOf(index))
    }

    override fun zuLatex(): String = "$name:\\mathbb N\\to${zielMenge.zuLatex()}"
}

data class UnnatuerlichesKartesischesTupel(
    override val id: String,
    override val name: String,
    override val zielMenge: MengenAusdruck,
    override val vorschrift: Methode,
    override val nachweislichKonstant: Boolean = false,
) : UnendlicheIndexStruktur, UnnatuerlichesTupel<ZahlAusdruck> {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(zielMenge.fundamentalerZahlbereichOderNull() != null || zielMenge is BeschraenkteZahlmenge) {
            "Ein unnatürliches kartesisches Tupel benötigt einen numerischen Komponentenbereich."
        }
        pruefeEinstelligeVorschrift(vorschrift)
    }

    override val indexMenge: MengenAusdruck = NatürlicheZahlen

    override fun komponente(index: Long): ZahlAusdruck {
        require(index >= 1L) { "Ein unnatürliches Tupel ist ab Index 1 definiert." }
        return standardKomponente(RationaleZahl.von(index)) as ZahlAusdruck
    }

    override fun standardKomponente(index: ZahlAusdruck): MathematischesObjekt {
        require(index.istPositiverNatuerlicherIndex()) {
            "Unnatürliche kartesische Tupel verwenden die Projektkonvention ℕ={1,2,3,…}."
        }
        val wert = vorschrift.wendeAn(listOf(index))
        require(wert is ZahlAusdruck) { "Die Komponentenvorschrift muss eine Zahl liefern." }
        return wert
    }

    override fun zuLatex(): String = "$name:\\mathbb N\\to${zielMenge.zuLatex()}"
}

data class GanzzahligeFolge(
    override val id: String,
    override val name: String,
    override val zielMenge: MengenAusdruck,
    override val vorschrift: Methode,
    override val nachweislichKonstant: Boolean = false,
) : UnendlicheIndexStruktur, ZFolge<MathematischesObjekt> {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        pruefeEinstelligeVorschrift(vorschrift)
    }

    override val indexMenge: MengenAusdruck = GanzeZahlen

    override fun wert(index: Long): MathematischesObjekt = standardKomponente(RationaleZahl.von(index))

    override fun standardKomponente(index: ZahlAusdruck): MathematischesObjekt {
        require(index.istGanzzahligerIndex()) { "Eine Folge verwendet ganzzahlige Indizes." }
        return vorschrift.wendeAn(listOf(index))
    }

    override fun zuLatex(): String = "$name:\\mathbb Z\\to${zielMenge.zuLatex()}"
}

private fun ZahlAusdruck.istGanzzahligerIndex(): Boolean =
    this !is RationaleZahl || nenner == java.math.BigInteger.ONE

private fun ZahlAusdruck.istPositiverNatuerlicherIndex(): Boolean =
    this !is RationaleZahl || (nenner == java.math.BigInteger.ONE && zähler.signum() > 0)

/** Symbolische Hypererweiterung ohne Materialisierung unendlich vieler Komponenten. */
data class HyperErweiterteIndexStruktur(val original: UnendlicheIndexStruktur) {
    val indexMenge: MengenAusdruck = BenannteMenge(
        "hyper_${original.id}_index",
        "{}^*${original.indexMenge.zuLatex()}",
    )
    val zielMenge: MengenAusdruck = BenannteMenge(
        "hyper_${original.id}_ziel",
        "{}^*${original.zielMenge.zuLatex()}",
    )

    fun komponente(index: HyperNatuerlicherIndex): AllgemeinerParameter = AllgemeinerParameter(
        name = "hyper_${original.id}_${index.name}",
        latex = "{}^*${original.name}_{${index.name}}",
    )

    fun zuLatex(): String =
        "{}^*${original.name}:${indexMenge.zuLatex()}\\to${zielMenge.zuLatex()}"
}

fun UnendlicheIndexStruktur.hyperErweiterung(): HyperErweiterteIndexStruktur =
    HyperErweiterteIndexStruktur(this)

/** Metrikvertrag der Nichtstandardanalyse; bewusst getrennt vom allgemeinen Raumvertrag. */
data class NichtstandardMetrikVertrag(
    val traeger: MengenAusdruck,
    val abstandLatex: String,
    val axiome: NachweisStatus,
    val hyperErweiterbar: Boolean,
) {
    init { require(abstandLatex.isNotBlank()) }
}

fun standardMetrik(traeger: MengenAusdruck): NichtstandardMetrikVertrag? = when (
    traeger.fundamentalerZahlbereichOderNull()
) {
    FundamentalerZahlbereich.RATIONAL,
    FundamentalerZahlbereich.REELL,
    -> NichtstandardMetrikVertrag(traeger, "|x-y|", NachweisStatus.Nachgewiesen, true)
    FundamentalerZahlbereich.KOMPLEX ->
        NichtstandardMetrikVertrag(traeger, "|x-y|_{\\mathbb C}", NachweisStatus.Nachgewiesen, true)
    else -> null
}

data class NichtstandardCauchyAussage(
    val tupel: UnnatuerlichesKartesischesTupel,
    val metrik: NichtstandardMetrikVertrag,
) : Aussage {
    init {
        require(metrik.traeger == tupel.zielMenge)
        require(metrik.hyperErweiterbar)
    }

    override fun entscheide(kontext: RechenKontext): AussageErgebnis = if (tupel.nachweislichKonstant) {
        AussageErgebnis(
            Wahrheitswert.Wahr,
            EntscheidungsStatus.Bewiesen,
            "Konstante Tupel besitzen für alle Hyperindizes Abstand 0.",
        )
    } else {
        AussageErgebnis(
            null,
            EntscheidungsStatus.Unbekannt,
            "Die nichtstandardmäßige Cauchy-Bedingung bleibt symbolisch.",
        )
    }

    override fun zuLatex(): String =
        "\\forall H,J\\in{}^*\\mathbb N\\setminus\\mathbb N:\\;" +
            "{}^*d({}^*${tupel.name}_H,{}^*${tupel.name}_J)\\approx0"
}

sealed interface CauchyErgebnis {
    data class AussageWert(val aussage: NichtstandardCauchyAussage) : CauchyErgebnis
    data class NichtAnwendbar(val grund: String) : CauchyErgebnis
}

fun pruefeCauchy(
    tupel: UnnatuerlichesKartesischesTupel,
    metrik: NichtstandardMetrikVertrag? = standardMetrik(tupel.zielMenge),
): CauchyErgebnis {
    val aufgeloest = metrik ?: return CauchyErgebnis.NichtAnwendbar(
        "Für ${tupel.zielMenge.zuLatex()} ist keine eindeutige Metrik registriert.",
    )
    if (!aufgeloest.hyperErweiterbar) {
        return CauchyErgebnis.NichtAnwendbar("Die gewählte Metrik besitzt keine Hypererweiterung.")
    }
    return CauchyErgebnis.AussageWert(NichtstandardCauchyAussage(tupel, aufgeloest))
}

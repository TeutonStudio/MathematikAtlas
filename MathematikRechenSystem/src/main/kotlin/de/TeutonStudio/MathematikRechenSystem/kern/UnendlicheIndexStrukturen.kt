package de.TeutonStudio.MathematikRechenSystem.kern

/** Ein unendlicher Hyperindex wird niemals in Int oder Long umgewandelt. */
data class HyperNatuerlicherIndex(
    val name: String,
    val unendlich: Boolean = true,
) : ZahlAusdruck {
    init { require(name.isNotBlank()) }
    override fun zuLatex(): String = if (unendlich) "$name\\in{}^*\\mathbb N\\setminus\\mathbb N" else name
}

data class HyperErweiterteMenge(val original: MengenAusdruck) : MengenAusdruck {
    override fun zuLatex(): String = "{}^*${original.zuLatex()}"
}

data class HyperErweiterteKomponente(
    val strukturId: String,
    val index: HyperNatuerlicherIndex,
) : MathematischesObjekt {
    override fun zuLatex(): String = "{}^*u_{${index.name}}"
}

sealed interface UnendlicheIndexStruktur : MathematischesObjekt {
    val id: String
    val name: String
    val indexMenge: MengenAusdruck
    val zielMenge: MengenAusdruck
    val vorschrift: Methode
    val nachweislichKonstant: Boolean
    fun standardKomponente(index: ZahlAusdruck): MathematischesObjekt
}

private fun pruefeEinstelligeVorschrift(vorschrift: Methode) {
    require(vorschrift.parameter.size == 1) {
        "Eine unendliche Indexstruktur benötigt genau einen Indexparameter."
    }
}

data class UnnatuerlichesTupel(
    override val id: String,
    override val name: String,
    override val zielMenge: MengenAusdruck,
    override val vorschrift: Methode,
    override val nachweislichKonstant: Boolean = false,
) : UnendlicheIndexStruktur {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        pruefeEinstelligeVorschrift(vorschrift)
    }
    override val indexMenge: MengenAusdruck = NatürlicheZahlen

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
) : UnendlicheIndexStruktur {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(zielMenge.fundamentalerZahlbereichOderNull() != null || zielMenge is BeschraenkteZahlmenge) {
            "Ein unnatürliches kartesisches Tupel benötigt einen numerischen Komponentenbereich."
        }
        pruefeEinstelligeVorschrift(vorschrift)
    }
    override val indexMenge: MengenAusdruck = NatürlicheZahlen

    override fun standardKomponente(index: ZahlAusdruck): MathematischesObjekt {
        require(index.istPositiverNatuerlicherIndex()) {
            "Unnatürliche kartesische Tupel verwenden die Projektkonvention ℕ={1,2,3,…}."
        }
        return vorschrift.wendeAn(listOf(index)).also {
            require(it is ZahlAusdruck) { "Die Komponentenvorschrift muss eine Zahl liefern." }
        }
    }

    override fun zuLatex(): String = "$name:\\mathbb N\\to${zielMenge.zuLatex()}"
}

data class GanzzahligeFolge(
    override val id: String,
    override val name: String,
    override val zielMenge: MengenAusdruck,
    override val vorschrift: Methode,
    override val nachweislichKonstant: Boolean = false,
) : UnendlicheIndexStruktur {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        pruefeEinstelligeVorschrift(vorschrift)
    }
    override val indexMenge: MengenAusdruck = GanzeZahlen

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

data class HyperErweiterteIndexStruktur(
    val original: UnendlicheIndexStruktur,
) : MathematischesObjekt {
    val indexMenge: MengenAusdruck = HyperErweiterteMenge(original.indexMenge)
    val zielMenge: MengenAusdruck = HyperErweiterteMenge(original.zielMenge)

    fun komponente(index: HyperNatuerlicherIndex): HyperErweiterteKomponente =
        HyperErweiterteKomponente(original.id, index)

    override fun zuLatex(): String =
        "{}^*${original.name}:${indexMenge.zuLatex()}\\to${zielMenge.zuLatex()}"
}

fun UnendlicheIndexStruktur.hyperErweiterung(): HyperErweiterteIndexStruktur =
    HyperErweiterteIndexStruktur(this)

data class MetrikVertrag(
    val traeger: MengenAusdruck,
    val abstandLatex: String,
    val axiome: NachweisStatus,
    val hyperErweiterbar: Boolean,
) {
    init { require(abstandLatex.isNotBlank()) }
}

fun standardMetrik(traeger: MengenAusdruck): MetrikVertrag? = when (
    traeger.fundamentalerZahlbereichOderNull()
) {
    FundamentalerZahlbereich.RATIONAL,
    FundamentalerZahlbereich.REELL,
    -> MetrikVertrag(traeger, "|x-y|", NachweisStatus.Nachgewiesen, true)
    FundamentalerZahlbereich.KOMPLEX ->
        MetrikVertrag(traeger, "|x-y|_{\\mathbb C}", NachweisStatus.Nachgewiesen, true)
    else -> null
}

data class NichtstandardCauchyAussage(
    val tupel: UnnatuerlichesKartesischesTupel,
    val metrik: MetrikVertrag,
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
    metrik: MetrikVertrag? = standardMetrik(tupel.zielMenge),
): CauchyErgebnis {
    val aufgeloest = metrik ?: return CauchyErgebnis.NichtAnwendbar(
        "Für ${tupel.zielMenge.zuLatex()} ist keine eindeutige Metrik registriert.",
    )
    if (!aufgeloest.hyperErweiterbar) {
        return CauchyErgebnis.NichtAnwendbar("Die gewählte Metrik besitzt keine Hypererweiterung.")
    }
    return CauchyErgebnis.AussageWert(NichtstandardCauchyAussage(tupel, aufgeloest))
}

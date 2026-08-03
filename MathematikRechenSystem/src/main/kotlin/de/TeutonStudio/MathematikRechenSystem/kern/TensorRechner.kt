package de.TeutonStudio.MathematikRechenSystem.kern

/** Bewahrt eine noch nicht konkret auswertbare Konjugation symbolisch. */
data class KonjugierterZahlAusdruck(val argument: ZahlAusdruck) : ZahlAusdruck {
    override fun zuLatex(): String = "\\overline{${argument.zuLatex()}}"
}

fun konjugiereFormal(argument: ZahlAusdruck): ZahlAusdruck = when (argument) {
    is RationaleZahl -> argument
    is KomplexeZahl -> KomplexeZahl(argument.realteil, negation(argument.imaginärteil))
    is KonjugierterZahlAusdruck -> argument.argument
    else -> KonjugierterZahlAusdruck(argument)
}

enum class SkalarproduktLinearitaet { RECHTSLINEAR, LINKSLINEAR }

data class SkalarproduktSpezifikation(
    val id: String = "skalarprodukt.standard",
    val linearitaet: SkalarproduktLinearitaet = SkalarproduktLinearitaet.RECHTSLINEAR,
    val konjugiert: Boolean = true,
) {
    init { require(id.isNotBlank()) }
}

/**
 * Vektororientierungen werden beim Skalarprodukt absichtlich nicht
 * unterschieden. Auch ein homogenes kartesisches Tupel ist zulässig.
 */
fun standardSkalarprodukt(
    links: MathematischesObjekt,
    rechts: MathematischesObjekt,
    spezifikation: SkalarproduktSpezifikation = SkalarproduktSpezifikation(),
    werteVorraete: Map<String, MengenAusdruck> = emptyMap(),
): StrukturPruefung<ZahlAusdruck> {
    val l = links.numerischeKomponentenAnsicht(werteVorraete = werteVorraete)
    val r = rechts.numerischeKomponentenAnsicht(werteVorraete = werteVorraete)
    if (l !is StrukturPruefung.Gueltig) return l.ohneStrukturWert()
    if (r !is StrukturPruefung.Gueltig) return r.ohneStrukturWert()
    if (l.wert.laenge != r.wert.laenge) {
        return StrukturPruefung.Ungueltig(
            "Das Skalarprodukt benötigt gleich lange Komponentenfolgen; erhalten: " +
                "${l.wert.laenge} und ${r.wert.laenge}.",
        )
    }
    val summanden = l.wert.komponenten.zip(r.wert.komponenten) { a, b ->
        when (spezifikation.linearitaet) {
            SkalarproduktLinearitaet.RECHTSLINEAR ->
                multiplikation(if (spezifikation.konjugiert) konjugiereFormal(a) else a, b)
            SkalarproduktLinearitaet.LINKSLINEAR ->
                multiplikation(a, if (spezifikation.konjugiert) konjugiereFormal(b) else b)
        }
    }
    return StrukturPruefung.Gueltig(addition(summanden))
}

private fun StrukturPruefung<*>.ohneStrukturWert(): StrukturPruefung<Nothing> = when (this) {
    is StrukturPruefung.Gueltig -> error("Ein gültiger Strukturwert kann nicht ohne Wert weitergereicht werden.")
    is StrukturPruefung.Bedingt -> StrukturPruefung.Bedingt<Nothing>(null, bedingungen)
    is StrukturPruefung.Ungueltig -> this
    is StrukturPruefung.Unentscheidbar -> this
}

/** Semantische Beschreibung des Falkschen Schemas, unabhängig von Compose. */
data class FalkSchema(
    val linkeForm: List<Int>,
    val rechteForm: List<Int>,
    val ergebnisForm: List<Int>,
    val ausgewaehlteZeile: Int? = null,
    val ausgewaehlteSpalte: Int? = null,
) {
    init {
        require(linkeForm.size == 2 && rechteForm.size == 2)
        require(linkeForm[1] == rechteForm[0])
        require(ergebnisForm == listOf(linkeForm[0], rechteForm[1]))
        require(ausgewaehlteZeile == null || ausgewaehlteZeile in 0 until linkeForm[0])
        require(ausgewaehlteSpalte == null || ausgewaehlteSpalte in 0 until rechteForm[1])
    }

    fun eintragsFormelLatex(): String {
        val i = ausgewaehlteZeile?.toString() ?: "i"
        val j = ausgewaehlteSpalte?.toString() ?: "j"
        return "c_{$i$j}=\\sum_{k=0}^{${linkeForm[1] - 1}}a_{$i k}b_{k$j}"
    }
}

fun falkSchema(links: Matrix, rechts: Matrix, zeile: Int? = null, spalte: Int? = null): FalkSchema =
    FalkSchema(
        links.tensorForm,
        rechts.tensorForm,
        listOf(links.zeilenAnzahl, rechts.spaltenAnzahl),
        zeile,
        spalte,
    )

data class TensorAchse(
    val id: String,
    val position: Int,
    val dimension: Int,
    val sichtbarerName: String? = null,
) {
    init {
        require(id.isNotBlank())
        require(position >= 0)
        require(dimension > 0)
    }
}

data class TensorFormVertrag(
    val achsen: List<TensorAchse>,
    val zahlBereich: MengenAusdruck,
) {
    init {
        require(achsen.isNotEmpty())
        require(achsen.map { it.id }.distinct().size == achsen.size)
        require(achsen.map { it.position }.sorted() == achsen.indices.toList())
    }
    val form: List<Int> get() = achsen.map { it.dimension }
    val stufe: Int get() = achsen.size
}

fun tensorprodukt(
    links: MathematischesObjekt,
    rechts: MathematischesObjekt,
    werteVorraete: Map<String, MengenAusdruck> = emptyMap(),
    materialisierungsBudget: Int = 100_000,
): StrukturPruefung<MathematischesObjekt> {
    val l = links.tensorielleAnsicht(werteVorraete)
    val r = rechts.tensorielleAnsicht(werteVorraete)
    if (l !is StrukturPruefung.Gueltig) return l.ohneStrukturWert()
    if (r !is StrukturPruefung.Gueltig) return r.ohneStrukturWert()
    val form = l.wert.form + r.wert.form
    val anzahl = runCatching { form.sicheresProdukt() }.getOrElse {
        return StrukturPruefung.Ungueltig("Die Tensorform ist zu groß.")
    }
    if (anzahl > materialisierungsBudget) {
        return StrukturPruefung.Bedingt(
            null,
            listOf("Das Tensorprodukt mit $anzahl Komponenten überschreitet das Budget $materialisierungsBudget."),
        )
    }
    val werte = buildList(anzahl) {
        l.wert.komponenten.forEach { a ->
            r.wert.komponenten.forEach { b -> add(multiplikation(a, b)) }
        }
    }
    return StrukturPruefung.Gueltig(materialisiereTensor(form, werte))
}

data class TensorDimensionenErgebnis(
    val dimensionen: Tupel,
    val stufe: RationaleZahl,
    val form: List<Int>,
) : MathematischesObjekt {
    override fun zuLatex(): String =
        "${form.joinToString("\\times")},\\;\\operatorname{Stufe}=${stufe.zuLatex()}"
}

fun tensorDimensionen(
    objekt: MathematischesObjekt,
    werteVorraete: Map<String, MengenAusdruck> = emptyMap(),
): StrukturPruefung<TensorDimensionenErgebnis> = when (val a = objekt.tensorielleAnsicht(werteVorraete)) {
    is StrukturPruefung.Gueltig -> StrukturPruefung.Gueltig(
        TensorDimensionenErgebnis(
            Tupel(a.wert.form.map { RationaleZahl.von(it.toLong()) }),
            RationaleZahl.von(a.wert.stufe.toLong()),
            a.wert.form,
        ),
    )
    is StrukturPruefung.Bedingt -> StrukturPruefung.Bedingt(null, a.bedingungen)
    is StrukturPruefung.Ungueltig -> a
    is StrukturPruefung.Unentscheidbar -> a
}

enum class TensorRechnerOperator(val stabileId: String) {
    ADDITION("tensor.addition"),
    SUBTRAKTION("tensor.subtraktion"),
    SKALARMULTIPLIKATION("tensor.skalarmultiplikation"),
    HADAMARD_PRODUKT("tensor.hadamard"),
    TENSORPRODUKT("tensor.tensorprodukt"),
    KONTRAKTION("tensor.kontraktion"),
    ACHSENPERMUTATION("tensor.achsenpermutation"),
    TRANSPONIEREN("tensor.transponieren"),
    ACHSENSCHNITT("tensor.achsenschnitt"),
    INDEXAUSWERTUNG("tensor.indexauswertung"),
    NORM("tensor.norm"),
}

data class TensorRechnerEingabe(val rollenId: String, val objekt: MathematischesObjekt)

data class TensorRechnerKonfiguration(
    val achsen: List<Int> = emptyList(),
    val indizes: List<Int> = emptyList(),
    val permutation: List<Int> = emptyList(),
    val materialisierungsBudget: Int = 100_000,
) {
    init { require(materialisierungsBudget > 0) }
}

sealed interface TensorRechnerErgebnis {
    data class Wert(
        val objekt: MathematischesObjekt,
        val form: List<Int>,
        val zahlBereich: MengenAusdruck,
    ) : TensorRechnerErgebnis
    data class Bedingt(val form: List<Int>?, val bedingungen: List<String>) : TensorRechnerErgebnis
    data class Ungueltig(val code: String, val nachricht: String) : TensorRechnerErgebnis
}

object TensorRechner {
    const val KNOTEN_ART = "mathematik.tensorrechner"

    fun erzeuge(
        operator: TensorRechnerOperator,
        eingaben: List<TensorRechnerEingabe>,
        konfiguration: TensorRechnerKonfiguration = TensorRechnerKonfiguration(),
    ): TensorRechnerErgebnis = when (operator) {
        TensorRechnerOperator.ADDITION -> komponentenweise(eingaben, ::addition)
        TensorRechnerOperator.SUBTRAKTION -> komponentenweise(eingaben, ::subtraktion)
        TensorRechnerOperator.HADAMARD_PRODUKT -> komponentenweise(eingaben, ::multiplikation)
        TensorRechnerOperator.SKALARMULTIPLIKATION -> skalarMultiplikation(eingaben)
        TensorRechnerOperator.TENSORPRODUKT -> tensorProdukt(eingaben, konfiguration.materialisierungsBudget)
        TensorRechnerOperator.ACHSENPERMUTATION -> permutiere(eingaben, konfiguration.permutation)
        TensorRechnerOperator.TRANSPONIEREN -> {
            val a = einzigeAnsicht(eingaben)
                ?: return TensorRechnerErgebnis.Ungueltig("tensor_fehlt", "Transponieren benötigt genau einen Tensor.")
            permutiere(eingaben, konfiguration.permutation.ifEmpty { standardTensorPermutation(a.stufe) })
        }
        TensorRechnerOperator.INDEXAUSWERTUNG -> indexAuswertung(eingaben, konfiguration.indizes)
        TensorRechnerOperator.ACHSENSCHNITT -> achsenSchnitt(eingaben, konfiguration.achsen, konfiguration.indizes)
        TensorRechnerOperator.KONTRAKTION -> kontrahiere(eingaben, konfiguration.achsen)
        TensorRechnerOperator.NORM -> norm(eingaben)
    }

    fun alsFormelAusdruck(
        id: String,
        operator: TensorRechnerOperator,
        argumente: List<Pair<String, FormelAusdruck>>,
    ): FormelAusdruck.Operation = FormelAusdruck.Operation(
        id,
        operator.stabileId,
        argumente.mapIndexed { index, (rolle, ausdruck) -> FormelArgument(rolle, index, ausdruck) },
        if (operator == TensorRechnerOperator.INDEXAUSWERTUNG || operator == TensorRechnerOperator.NORM) {
            FormelTyp.ZAHL
        } else {
            FormelTyp.TENSOR
        },
    )

    private fun komponentenweise(
        eingaben: List<TensorRechnerEingabe>,
        operation: (ZahlAusdruck, ZahlAusdruck) -> ZahlAusdruck,
    ): TensorRechnerErgebnis {
        if (eingaben.size < 2) return ungueltig("argumentanzahl", "Mindestens zwei Tensoren werden benötigt.")
        val ansichten = mutableListOf<TensorielleAnsicht>()
        for (eingabe in eingaben) {
            when (val a = eingabe.objekt.tensorielleAnsicht()) {
                is StrukturPruefung.Gueltig -> ansichten += a.wert
                is StrukturPruefung.Bedingt -> return TensorRechnerErgebnis.Bedingt(null, a.bedingungen)
                is StrukturPruefung.Ungueltig -> return ungueltig("struktur", a.grund)
                is StrukturPruefung.Unentscheidbar -> return ungueltig("struktur_unentscheidbar", a.grund)
            }
        }
        val form = ansichten.first().form
        if (ansichten.any { it.form != form }) return ungueltig("form", "Alle Tensoren müssen dieselbe Form besitzen.")
        val werte = List(form.sicheresProdukt()) { index ->
            ansichten.drop(1).fold(ansichten.first().komponenten[index]) { akk, a ->
                operation(akk, a.komponenten[index])
            }
        }
        return alsWert(materialisiereWie(eingaben.first().objekt, form, werte))
    }

    private fun skalarMultiplikation(eingaben: List<TensorRechnerEingabe>): TensorRechnerErgebnis {
        val skalar = eingaben.singleOrNull { it.rollenId == "skalar" }?.objekt as? ZahlAusdruck
            ?: return ungueltig("skalar_fehlt", "Ein Zahlskalar fehlt.")
        val tensor = eingaben.singleOrNull { it.rollenId == "tensor" }?.objekt
            ?: return ungueltig("tensor_fehlt", "Ein Tensor fehlt.")
        val a = ansichtOderFehler(tensor) ?: return ungueltig("struktur", "Der Eingang besitzt keine tensorielle Ansicht.")
        return alsWert(materialisiereWie(tensor, a.form, a.komponenten.map { multiplikation(skalar, it) }))
    }

    private fun tensorProdukt(eingaben: List<TensorRechnerEingabe>, budget: Int): TensorRechnerErgebnis {
        val links = eingaben.singleOrNull { it.rollenId == "links" }?.objekt
        val rechts = eingaben.singleOrNull { it.rollenId == "rechts" }?.objekt
        if (links == null || rechts == null || eingaben.size != 2) {
            return ungueltig("argumentrollen", "Das Tensorprodukt erwartet genau links und rechts.")
        }
        return when (val p = tensorprodukt(links, rechts, materialisierungsBudget = budget)) {
            is StrukturPruefung.Gueltig -> alsWert(p.wert)
            is StrukturPruefung.Bedingt -> TensorRechnerErgebnis.Bedingt(null, p.bedingungen)
            is StrukturPruefung.Ungueltig -> ungueltig("tensorprodukt", p.grund)
            is StrukturPruefung.Unentscheidbar -> ungueltig("tensorprodukt_unentscheidbar", p.grund)
        }
    }

    private fun permutiere(eingaben: List<TensorRechnerEingabe>, permutation: List<Int>): TensorRechnerErgebnis {
        val objekt = eingaben.singleOrNull()?.objekt
            ?: return ungueltig("argumentanzahl", "Eine Achsenpermutation benötigt genau einen Tensor.")
        val a = ansichtOderFehler(objekt) ?: return ungueltig("struktur", "Keine tensorielle Ansicht.")
        if (runCatching { pruefePermutation(permutation, a.stufe) }.isFailure) {
            return ungueltig("permutation", "Die Permutation muss jede Achse genau einmal enthalten.")
        }
        val form = permutation.map(a.form::get)
        val werte = form.indizesFolge().map { neueIndizes ->
            val alteIndizes = MutableList(a.stufe) { 0 }
            permutation.forEachIndexed { neueAchse, alteAchse -> alteIndizes[alteAchse] = neueIndizes[neueAchse] }
            a.komponente(alteIndizes)
        }
        return alsWert(materialisiereTensor(form, werte))
    }

    private fun indexAuswertung(eingaben: List<TensorRechnerEingabe>, indizes: List<Int>): TensorRechnerErgebnis {
        val a = einzigeAnsicht(eingaben) ?: return ungueltig("argumentanzahl", "Genau ein Tensor wird benötigt.")
        if (indizes.size != a.stufe || indizes.zip(a.form).any { (i, n) -> i !in 0 until n }) {
            return ungueltig("index", "Es wird genau ein gültiger Index je Achse benötigt.")
        }
        return TensorRechnerErgebnis.Wert(a.komponente(indizes), emptyList(), a.zahlBereich)
    }

    private fun achsenSchnitt(
        eingaben: List<TensorRechnerEingabe>,
        achsen: List<Int>,
        indizes: List<Int>,
    ): TensorRechnerErgebnis {
        val a = einzigeAnsicht(eingaben) ?: return ungueltig("argumentanzahl", "Genau ein Tensor wird benötigt.")
        if (achsen.size != 1 || indizes.size != 1) return ungueltig("achsenschnitt", "Eine Achse und ein Index werden benötigt.")
        val achse = achsen.single()
        val index = indizes.single()
        if (achse !in a.form.indices || index !in 0 until a.form[achse]) return ungueltig("achsenschnitt", "Achse oder Index ist ungültig.")
        val form = a.form.filterIndexed { position, _ -> position != achse }
        val freie = if (form.isEmpty()) listOf(emptyList()) else form.indizesFolge()
        val werte = freie.map { rest ->
            val voll = rest.toMutableList().also { it.add(achse, index) }
            a.komponente(voll)
        }
        return alsWert(if (form.isEmpty()) werte.single() else materialisiereTensor(form, werte))
    }

    private fun kontrahiere(eingaben: List<TensorRechnerEingabe>, achsen: List<Int>): TensorRechnerErgebnis {
        val a = einzigeAnsicht(eingaben) ?: return ungueltig("argumentanzahl", "Genau ein Tensor wird benötigt.")
        if (achsen.size != 2 || achsen[0] == achsen[1]) return ungueltig("achsen", "Zwei verschiedene Achsen werden benötigt.")
        val x = achsen[0]
        val y = achsen[1]
        if (x !in a.form.indices || y !in a.form.indices || a.form[x] != a.form[y]) {
            return ungueltig("dimension", "Kontrahierte Achsen müssen vorhanden und gleich lang sein.")
        }
        val entfernt = setOf(x, y)
        val form = a.form.filterIndexed { index, _ -> index !in entfernt }
        val freie = if (form.isEmpty()) listOf(emptyList()) else form.indizesFolge()
        val werte = freie.map { rest ->
            addition(List(a.form[x]) { diagonal ->
                val voll = MutableList(a.stufe) { 0 }
                var frei = 0
                for (achse in 0 until a.stufe) voll[achse] = if (achse in entfernt) diagonal else rest[frei++]
                a.komponente(voll)
            })
        }
        return alsWert(if (form.isEmpty()) werte.single() else materialisiereTensor(form, werte))
    }

    private fun norm(eingaben: List<TensorRechnerEingabe>): TensorRechnerErgebnis {
        val a = einzigeAnsicht(eingaben) ?: return ungueltig("argumentanzahl", "Genau ein Tensor wird benötigt.")
        return TensorRechnerErgebnis.Wert(
            Wurzel(addition(a.komponenten.map { multiplikation(konjugiereFormal(it), it) })),
            emptyList(),
            a.zahlBereich,
        )
    }

    private fun einzigeAnsicht(eingaben: List<TensorRechnerEingabe>): TensorielleAnsicht? =
        eingaben.singleOrNull()?.objekt?.let(::ansichtOderFehler)

    private fun ansichtOderFehler(objekt: MathematischesObjekt): TensorielleAnsicht? =
        (objekt.tensorielleAnsicht() as? StrukturPruefung.Gueltig)?.wert

    private fun alsWert(objekt: MathematischesObjekt): TensorRechnerErgebnis {
        if (objekt is ZahlAusdruck) return TensorRechnerErgebnis.Wert(objekt, emptyList(), inferiereZahlenWertevorrat(objekt))
        val a = ansichtOderFehler(objekt) ?: return ungueltig("struktur", "Das Ergebnis ist nicht tensorartig.")
        return TensorRechnerErgebnis.Wert(objekt, a.form, a.zahlBereich)
    }

    private fun ungueltig(code: String, nachricht: String) = TensorRechnerErgebnis.Ungueltig(code, nachricht)
}

private fun materialisiereWie(
    vorlage: MathematischesObjekt,
    form: List<Int>,
    werte: List<ZahlAusdruck>,
): MathematischesObjekt = when (vorlage) {
    is ZeilenVektor if form.size == 1 -> ZeilenVektor(werte)
    is SpaltenVektor if form.size == 1 -> SpaltenVektor(werte)
    is Matrix if form.size == 2 -> Matrix(List(form[0]) { z -> List(form[1]) { s -> werte[z * form[1] + s] } })
    else -> materialisiereTensor(form, werte)
}

private fun materialisiereTensor(form: List<Int>, werte: List<ZahlAusdruck>): MathematischesObjekt = when (form.size) {
    0 -> werte.single()
    2 -> Matrix(List(form[0]) { z -> List(form[1]) { s -> werte[z * form[1] + s] } })
    else -> Tensor(form, werte)
}

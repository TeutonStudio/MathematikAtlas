package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

object TestDefinitionsKarten {
    val alle: List<KonzeptDefinition> by lazy {
        listOf(
            zahlenKonzept(), additionsKonzept(), subtraktionsKonzept(),
            multiplikationsKonzept(), kehrwertKonzept(), divisionsKonzept(),
            ReelleMethodenSummenKonzept.definition,
        )
    }

    fun finde(id: KonzeptId) = alle.firstOrNull { it.id == id }

    fun fürKnoten(knoten: KnotenDaten): KonzeptDefinition? = when (knoten.art) {
        "mathematik.zahl" -> finde(KonzeptId("zahl"))
        "mathematik.addition" -> finde(KonzeptId("addition"))
        "mathematik.subtraktion" -> finde(KonzeptId("subtraktion"))
        "mathematik.multiplikation" -> finde(KonzeptId("multiplikation"))
        "mathematik.kehrwert" -> finde(KonzeptId("kehrwert"))
        "mathematik.division" -> finde(KonzeptId("division"))
        "mathematik.reelleMethodenSumme" -> ReelleMethodenSummenKonzept.definition
        else -> null
    }

    private fun zahlenKonzept(): KonzeptDefinition {
        val leer = knoten(MathematikKnotenVorlagen.EndlicheMenge, "zahl-leer", 30f, 30f, mapOf("elemente" to ""))
        val plus = knoten(MathematikKnotenVorlagen.AllgemeinerParameter, "zahl-plus", 30f, 190f, mapOf("name" to "+"))
        val plusMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "zahl-plus-menge", 330f, 190f)
        val plusMacht = knoten(MathematikKnotenVorlagen.Mächtigkeit, "zahl-plus-macht", 620f, 190f)
        val minus = knoten(MathematikKnotenVorlagen.AllgemeinerParameter, "zahl-minus", 30f, 350f, mapOf("name" to "−"))
        val minusMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "zahl-minus-menge", 330f, 350f)
        val minusMacht = knoten(MathematikKnotenVorlagen.Mächtigkeit, "zahl-minus-macht", 620f, 350f)
        val definition = karte(
            "konzept-zahl-definition", "0 = ∅, 1 = {+}, −1 = {−}",
            listOf(leer, plus, plusMenge, plusMacht, minus, minusMenge, minusMacht),
            listOf(
                verbindung("zahl-d1", plus, "wert", plusMenge, "element"),
                verbindung("zahl-d2", plusMenge, "menge", plusMacht, "menge"),
                verbindung("zahl-d3", minus, "wert", minusMenge, "element"),
                verbindung("zahl-d4", minusMenge, "menge", minusMacht, "menge"),
            ),
        )
        val positiv = nachfolger("positiv", "+")
        val negativ = nachfolger("negativ", "−")
        val bereiche = zahlbereiche()
        return KonzeptDefinition(
            id = KonzeptId("zahl"), name = "Zahl",
            beschreibung = "0 ist die leere Menge. +1 und −1 sind gleichmächtige Einzelmengen ihrer Richtungsoperatoren; ihre Nachfolger erzeugen die beiden Äste von ℤ.",
            pfad = listOf("Grundlagen", "Zahlen"),
            tags = setOf("Zahl", "Nachfolger", "Ganze Zahlen", "Mächtigkeit"),
            knotenArten = setOf("mathematik.zahl"),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, definition),
                KonzeptReiter("positiver-nachfolger", "Positiver Nachfolger", KonzeptReiterRolle.Spezialfall, positiv),
                KonzeptReiter("negativer-nachfolger", "Negativer Nachfolger", KonzeptReiterRolle.Spezialfall, negativ),
                KonzeptReiter("zahlbereiche", "ℕ, ℕ₀ und ℤ", KonzeptReiterRolle.Äquivalenz, bereiche),
            ),
        )
    }

    private fun nachfolger(id: String, operator: String): KartenDaten {
        val x = schnittstelle("nachfolger-$id-x", "x", MathematikAnschlussArten.Menge.id, true, 30f, 100f)
        val op = knoten(MathematikKnotenVorlagen.AllgemeinerParameter, "nachfolger-$id-op", 30f, 280f, mapOf("name" to operator))
        val singleton = knoten(MathematikKnotenVorlagen.Einzelmenge, "nachfolger-$id-singleton", 340f, 280f)
        val vereinigt = binär(MathematikKnotenVorlagen.Vereinigung, "nachfolger-$id-union", 660f, 160f)
        val aus = schnittstelle("nachfolger-$id-aus", "nachfolger", MathematikAnschlussArten.Menge.id, false, 990f, 160f)
        return karte(
            "konzept-zahl-$id", if (id == "positiv") "x ∪ {+}" else "x ∪ {−}",
            listOf(x, op, singleton, vereinigt, aus),
            listOf(
                verbindung("nach-$id-1", op, "wert", singleton, "element"),
                verbindung("nach-$id-2", x, "wert", vereinigt, "a"),
                verbindung("nach-$id-3", singleton, "menge", vereinigt, "b"),
                verbindung("nach-$id-4", vereinigt, "menge", aus, "wert"),
            ),
        )
    }

    private fun zahlbereiche(): KartenDaten {
        val n = knoten(MathematikKnotenVorlagen.NatürlicheZahlen, "bereiche-n", 30f, 40f)
        val nullKnoten = knoten(MathematikKnotenVorlagen.Zahl, "bereiche-null", 30f, 210f, mapOf("wert" to "0"))
        val nullMenge = knoten(MathematikKnotenVorlagen.Einzelmenge, "bereiche-nullmenge", 300f, 210f)
        val n0 = binär(MathematikKnotenVorlagen.Vereinigung, "bereiche-n0", 600f, 120f)
        val z = knoten(MathematikKnotenVorlagen.GanzeZahlen, "bereiche-z", 920f, 120f)
        val nTeil = knoten(MathematikKnotenVorlagen.TeilOderGleichmenge, "bereiche-n-teil", 1230f, 40f)
        val n0Teil = knoten(MathematikKnotenVorlagen.TeilOderGleichmenge, "bereiche-n0-teil", 1230f, 230f)
        return karte(
            "konzept-zahl-bereiche", "ℕ ⊆ ℤ und ℕ₀ ⊆ ℤ",
            listOf(n, nullKnoten, nullMenge, n0, z, nTeil, n0Teil),
            listOf(
                verbindung("bereiche-1", nullKnoten, "wert", nullMenge, "element"),
                verbindung("bereiche-2", n, "menge", n0, "a"),
                verbindung("bereiche-3", nullMenge, "menge", n0, "b"),
                verbindung("bereiche-4", n, "menge", nTeil, "links"),
                verbindung("bereiche-5", z, "menge", nTeil, "rechts"),
                verbindung("bereiche-6", n0, "menge", n0Teil, "links"),
                verbindung("bereiche-7", z, "menge", n0Teil, "rechts"),
            ),
        )
    }

    private fun additionsKonzept(): KonzeptDefinition {
        val a = zahl("addition-a", "2", 30f, 40f)
        val b = zahl("addition-b", "3", 30f, 200f)
        val op = binär(MathematikKnotenVorlagen.Addition, "addition-operator", 330f, 120f)
        val def = karte("konzept-addition-definition", "Addition", listOf(a, b, op), listOf(
            verbindung("add-1", a, "wert", op, "a"), verbindung("add-2", b, "wert", op, "b"),
        ))
        val x = zahl("addition-null-x", "7", 30f, 40f)
        val zero = zahl("addition-null-null", "0", 30f, 200f)
        val plus = binär(MathematikKnotenVorlagen.Addition, "addition-null-plus", 330f, 120f)
        val eq = knoten(MathematikKnotenVorlagen.Gleichheit, "addition-null-eq", 650f, 120f)
        val sonder = karte("konzept-addition-null", "x + 0 = x", listOf(x, zero, plus, eq), listOf(
            verbindung("addn-1", x, "wert", plus, "a"), verbindung("addn-2", zero, "wert", plus, "b"),
            verbindung("addn-3", plus, "wert", eq, "links"), verbindung("addn-4", x, "wert", eq, "rechts"),
        ))
        return KonzeptDefinition(
            id = KonzeptId("addition"), name = "Addition", beschreibung = "Verknüpft zwei oder mehr Summanden.",
            pfad = listOf("Algebra", "Verknüpfungen"), tags = setOf("Addition", "Summe"), knotenArten = setOf("mathematik.addition"),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, def),
                KonzeptReiter("neutral-null", "Sonderfall 0", KonzeptReiterRolle.Spezialfall, sonder),
            ),
            navigation = navigation("definition", def.knoten) + navigation("neutral-null", sonder.knoten),
            erkundungsFreigaben = listOf(
                KonzeptErkundungsFreigabe("definition", a.id, "wert", "Erster Summand"),
                KonzeptErkundungsFreigabe("definition", b.id, "wert", "Zweiter Summand"),
                KonzeptErkundungsFreigabe("neutral-null", x.id, "wert", "Ausgangswert"),
                KonzeptErkundungsFreigabe("neutral-null", zero.id, "wert", "Testwert statt 0"),
            ),
        )
    }

    private fun subtraktionsKonzept(): KonzeptDefinition {
        val a = zahl("sub-a", "7", 30f, 50f)
        val b = zahl("sub-b", "3", 30f, 210f)
        val minus = knoten(ErweiterteMathematikKnotenVorlagen.Subtraktion, "sub-op", 350f, 130f)
        val def = karte("konzept-sub-definition", "a − b", listOf(a, b, minus), listOf(
            verbindung("sub-1", a, "wert", minus, "minuend"), verbindung("sub-2", b, "wert", minus, "subtrahend"),
        ))
        val minusEins = zahl("sub-minus-eins", "-1", 30f, 360f)
        val mal = binär(MathematikKnotenVorlagen.Multiplikation, "sub-mal", 350f, 280f)
        val plus = binär(MathematikKnotenVorlagen.Addition, "sub-plus", 690f, 150f)
        val zA = a.copy(id = KnotenId("sub-z-a"))
        val zB = b.copy(id = KnotenId("sub-z-b"))
        val z = karte("konzept-sub-z", "a + (−1)·b", listOf(zA, zB, minusEins, mal, plus), listOf(
            verbindung("subz-1", zB, "wert", mal, "a"), verbindung("subz-2", minusEins, "wert", mal, "b"),
            verbindung("subz-3", zA, "wert", plus, "a"), verbindung("subz-4", mal, "wert", plus, "b"),
        ))
        return KonzeptDefinition(
            id = KonzeptId("subtraktion"), name = "Subtraktion", beschreibung = "Subtraktion ist Addition des additiv Inversen.",
            pfad = listOf("Algebra", "Verknüpfungen"), tags = setOf("Subtraktion", "Differenz"), knotenArten = setOf("mathematik.subtraktion"),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, def),
                KonzeptReiter("ganze-zahlen", "Sonderfall ℤ", KonzeptReiterRolle.Spezialfall, z),
            ),
            navigation = navigation("definition", def.knoten) + navigation("ganze-zahlen", z.knoten),
        )
    }

    private fun multiplikationsKonzept(): KonzeptDefinition {
        val a = zahl("multiplikation-a", "4", 30f, 40f)
        val b = zahl("multiplikation-b", "5", 30f, 200f)
        val op = binär(MathematikKnotenVorlagen.Multiplikation, "multiplikation-operator", 330f, 120f)
        val def = karte("konzept-multiplikation-definition", "Multiplikation", listOf(a, b, op), listOf(
            verbindung("mul-1", a, "wert", op, "a"), verbindung("mul-2", b, "wert", op, "b"),
        ))
        return KonzeptDefinition(
            id = KonzeptId("multiplikation"), name = "Multiplikation", beschreibung = "Verknüpft Faktoren zu einem Produkt.",
            pfad = listOf("Algebra", "Verknüpfungen"), tags = setOf("Multiplikation", "Produkt"), knotenArten = setOf("mathematik.multiplikation"),
            reiter = listOf(KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, def)),
            navigation = navigation("definition", def.knoten),
            erkundungsFreigaben = listOf(
                KonzeptErkundungsFreigabe("definition", a.id, "wert", "Erster Faktor"),
                KonzeptErkundungsFreigabe("definition", b.id, "wert", "Zweiter Faktor"),
            ),
        )
    }

    private fun kehrwertKonzept(): KonzeptDefinition {
        val x = zahl("kehrwert-zahl", "4", 30f, 100f)
        val inv = knoten(MathematikKnotenVorlagen.Kehrwert, "kehrwert-operator", 330f, 100f)
        val def = karte("konzept-kehrwert-definition", "x⁻¹", listOf(x, inv), listOf(verbindung("inv-1", x, "wert", inv, "zahl")))
        return KonzeptDefinition(
            id = KonzeptId("kehrwert"), name = "Kehrwert", beschreibung = "x⁻¹ für x≠0.",
            pfad = listOf("Algebra", "Verknüpfungen"), tags = setOf("Kehrwert", "Inverse"), knotenArten = setOf("mathematik.kehrwert"),
            reiter = listOf(KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, def)),
            navigation = navigation("definition", def.knoten),
        )
    }

    private fun divisionsKonzept(): KonzeptDefinition {
        val definition = divisionsDefinition()
        val reell = einfacheDivision("reell-reell")
        val komplex = komplexeDivision()
        return KonzeptDefinition(
            id = KonzeptId("division"), name = "Division", beschreibung = "Division ist Multiplikation mit dem Kehrwert und besitzt einen expliziten Nullfall.",
            pfad = listOf("Algebra", "Verknüpfungen"), tags = setOf("Division", "Kehrwert", "Konjugierte"), knotenArten = setOf("mathematik.division"),
            reiter = listOf(
                KonzeptReiter("definition", "Definition", KonzeptReiterRolle.Definition, definition),
                KonzeptReiter("reell-reell", "Reell durch reell", KonzeptReiterRolle.Spezialfall, reell),
                KonzeptReiter("komplexer-divisor", "Komplexer Divisor", KonzeptReiterRolle.Spezialfall, komplex),
            ),
            navigation = navigation("definition", definition.knoten) + navigation("reell-reell", reell.knoten) + navigation("komplexer-divisor", komplex.knoten),
        )
    }

    private fun divisionsDefinition(): KartenDaten {
        val x = schnittstelle("div-x", "x", MathematikAnschlussArten.Zahl.id, true, 20f, 50f)
        val y = schnittstelle("div-y", "y", MathematikAnschlussArten.Zahl.id, true, 20f, 210f)
        val ersatz = schnittstelle("div-e", "falls Nenner null", MathematikAnschlussArten.Zahl.id, true, 360f, 520f)
        val zero = zahl("div-zero", "0", 20f, 380f)
        val eq = knoten(MathematikKnotenVorlagen.Gleichheit, "div-eq", 340f, 300f)
        val inv = knoten(MathematikKnotenVorlagen.Kehrwert, "div-inv", 340f, 170f)
        val mal = binär(MathematikKnotenVorlagen.Multiplikation, "div-mal", 650f, 80f)
        val fall = knoten(MathematikKnotenVorlagen.Fall, "div-fall", 950f, 240f)
        return karte("konzept-division-definition", "Division", listOf(x, y, ersatz, zero, eq, inv, mal, fall), listOf(
            verbindung("div-1", y, "wert", eq, "links"), verbindung("div-2", zero, "wert", eq, "rechts"),
            verbindung("div-3", y, "wert", inv, "zahl"), verbindung("div-4", x, "wert", mal, "a"),
            verbindung("div-5", inv, "wert", mal, "b"), verbindung("div-6", ersatz, "wert", fall, "wahr"),
            verbindung("div-7", eq, "aussage", fall, "aussage"), verbindung("div-8", mal, "wert", fall, "lüge"),
        ))
    }

    private fun einfacheDivision(id: String): KartenDaten {
        val a = schnittstelle("$id-a", "a", MathematikAnschlussArten.Zahl.id, true, 20f, 60f)
        val b = schnittstelle("$id-b", "b", MathematikAnschlussArten.Zahl.id, true, 20f, 230f)
        val inv = knoten(MathematikKnotenVorlagen.Kehrwert, "$id-inv", 340f, 230f)
        val mal = binär(MathematikKnotenVorlagen.Multiplikation, "$id-mal", 660f, 140f)
        return karte("konzept-$id", "a·b⁻¹", listOf(a, b, inv, mal), listOf(
            verbindung("$id-1", b, "wert", inv, "zahl"), verbindung("$id-2", a, "wert", mal, "a"), verbindung("$id-3", inv, "wert", mal, "b"),
        ))
    }

    private fun komplexeDivision(): KartenDaten {
        val a = schnittstelle("komplex-a", "a", MathematikAnschlussArten.Zahl.id, true, 20f, 50f)
        val b = schnittstelle("komplex-b", "b", MathematikAnschlussArten.Zahl.id, true, 20f, 260f)
        val konj = knoten(MathematikKnotenVorlagen.Konjugierte, "komplex-konj", 330f, 260f)
        val zaehler = binär(MathematikKnotenVorlagen.Multiplikation, "komplex-z", 640f, 50f)
        val nenner = binär(MathematikKnotenVorlagen.Multiplikation, "komplex-n", 640f, 280f)
        val inv = knoten(MathematikKnotenVorlagen.Kehrwert, "komplex-inv", 950f, 280f)
        val ergebnis = binär(MathematikKnotenVorlagen.Multiplikation, "komplex-e", 1250f, 150f)
        return karte("konzept-division-komplexer-divisor", "Rationalisierung", listOf(a, b, konj, zaehler, nenner, inv, ergebnis), listOf(
            verbindung("k-1", b, "wert", konj, "zahl"), verbindung("k-2", a, "wert", zaehler, "a"),
            verbindung("k-3", konj, "wert", zaehler, "b"), verbindung("k-4", b, "wert", nenner, "a"),
            verbindung("k-5", konj, "wert", nenner, "b"), verbindung("k-6", nenner, "wert", inv, "zahl"),
            verbindung("k-7", zaehler, "wert", ergebnis, "a"), verbindung("k-8", inv, "wert", ergebnis, "b"),
        ))
    }

    private fun zahl(id: String, wert: String, x: Float, y: Float) = knoten(MathematikKnotenVorlagen.Zahl, id, x, y, mapOf("wert" to wert))

    private fun binär(vorlage: KnotenVorlage, id: String, x: Float, y: Float): KnotenDaten {
        val basis = knoten(vorlage, id, x, y)
        return basis.copy(anschlüsse = basis.anschlüsse.filter { it.name in setOf("a", "b", "wert", "menge") })
    }

    private fun schnittstelle(id: String, name: String, art: AnschlussArtId, ausgang: Boolean, x: Float, y: Float): KnotenDaten {
        val basis = knoten(if (ausgang) MathematikKnotenVorlagen.KartenEingang else MathematikKnotenVorlagen.KartenAusgang, id, x, y)
        return basis.copy(
            parameter = mapOf("name" to name),
            anschlüsse = listOf(AnschlussDaten(
                id = AnschlussId("$id-wert"), name = "wert",
                richtung = if (ausgang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang,
                kante = if (ausgang) AnschlussKante.Rechts else AnschlussKante.Links,
                art = art,
            )),
        )
    }

    private fun knoten(vorlage: KnotenVorlage, id: String, x: Float, y: Float, parameter: Map<String, String> = emptyMap()): KnotenDaten {
        val basis = vorlage.erzeuge(GraphPunkt(x, y))
        return basis.copy(
            id = KnotenId(id), parameter = basis.parameter + parameter,
            anschlüsse = basis.anschlüsse.map { it.copy(id = AnschlussId("$id-${it.name}-${it.reihenfolge}")) },
        )
    }

    private fun karte(id: String, name: String, knoten: List<KnotenDaten>, verbindungen: List<VerbindungDaten>) =
        KartenDaten(id = KartenId(id), name = name, knoten = knoten, verbindungen = verbindungen)

    private fun verbindung(id: String, von: KnotenDaten, vonName: String, zu: KnotenDaten, zuName: String) = VerbindungDaten(
        id = VerbindungsId(id),
        von = AnschlussVerweis(von.id, von.anschlüsse.first { it.name == vonName }.id),
        zu = AnschlussVerweis(zu.id, zu.anschlüsse.first { it.name == zuName }.id),
    )

    private fun navigation(reiter: String, knoten: Iterable<KnotenDaten>) = knoten.mapNotNull { k ->
        val ziel = when (k.art) {
            "mathematik.zahl" -> KonzeptId("zahl")
            "mathematik.addition" -> KonzeptId("addition")
            "mathematik.subtraktion" -> KonzeptId("subtraktion")
            "mathematik.multiplikation" -> KonzeptId("multiplikation")
            "mathematik.kehrwert" -> KonzeptId("kehrwert")
            "mathematik.division" -> KonzeptId("division")
            "mathematik.reelleMethodenSumme" -> KonzeptId("reelle-methodensumme")
            else -> null
        }
        ziel?.let { KonzeptKnotenSchlüssel(reiter, k.id) to it }
    }.toMap()
}

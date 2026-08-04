from pathlib import Path
import re

def replace_once(path: str, old: str, new: str) -> None:
    datei = Path(path)
    text = datei.read_text(encoding="utf-8")
    anzahl = text.count(old)
    if anzahl != 1:
        raise SystemExit(f"{path}: erwartete Stelle {anzahl} statt genau einmal gefunden: {old[:120]!r}")
    datei.write_text(text.replace(old, new, 1), encoding="utf-8")

def regex_once(path: str, pattern: str, replacement: str) -> None:
    datei = Path(path)
    text = datei.read_text(encoding="utf-8")
    neu, anzahl = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if anzahl != 1:
        raise SystemExit(f"{path}: Regex traf {anzahl} statt genau einmal: {pattern[:120]!r}")
    datei.write_text(neu, encoding="utf-8")

vorlagen_pfad = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikKnotenVorlagen.kt"
replace_once(
    vorlagen_pfad,
    '''    val GleichungLösen = KnotenVorlage(
        "mathematik.gleichungLösen", "Lineare Gleichung lösen", "Algebra", "Löst eine lineare Gleichung nach der gewählten Variablen.", GraphGröße(240f, 115f),
        listOf(eingang("gleichung", MathematikAnschlussArten.Aussage.id), ausgang("lösungen", MathematikAnschlussArten.Menge.id)), mapOf("variable" to "x"),
    )
    val Auswerten = KnotenVorlage(
        "mathematik.auswerten", "Auswerten", "Steuerung", "Vereinfacht einen Zahlterm und gibt wieder einen Zahlterm aus.", GraphGröße(230f, 110f),
        listOf(eingang("term", MathematikAnschlussArten.Zahl.id), ausgang("term", MathematikAnschlussArten.Zahl.id)),
    )''',
    '''    val Auflösen = KnotenVorlage(
        "mathematik.gleichungLösen", "Auflösen", "Algebra",
        "Bestimmt die Lösungsmenge einer Relation. Exakt lösbare Relationen werden berechnet; andernfalls bleibt die Lösungsmenge symbolisch definiert.",
        GraphGröße(240f, 115f),
        listOf(
            eingang("relation", MathematikAnschlussArten.Aussage.id),
            ausgang("lösungsmenge", MathematikAnschlussArten.Menge.id),
        ),
        mapOf("variable" to "x"),
    )
    /** Quellkompatibler Name für bestehende Aufrufer und gespeicherte Karten. */
    val GleichungLösen = Auflösen
    val Auswerten = KnotenVorlage(
        "mathematik.auswerten", "Auswerten", "Steuerung",
        "Vereinfacht einen mathematischen Term typ-erhaltend, etwa eine Zahl, Aussage, Matrix, einen Vektor, ein Tupel oder eine Menge.",
        GraphGröße(230f, 110f),
        listOf(
            eingang("term", MathematikAnschlussArten.Objekt.id),
            AnschlussDaten(
                name = "term",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Objekt.id,
                artFolgtEingang = "term",
            ),
        ),
    )''',
)

auswerter_pfad = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikAuswerter.kt"
helper = r'''
private data class TermVereinfachung(
    val objekt: MathematischesObjekt,
    val schritte: List<UmformungsSchritt> = emptyList(),
)

private data class RelationsAuflösung(
    val menge: MengenAusdruck,
    val schritte: List<UmformungsSchritt> = emptyList(),
    val warnungen: List<String> = emptyList(),
)

private fun vereinfacheTerm(
    objekt: MathematischesObjekt,
    kontext: RechenKontext,
): TermVereinfachung {
    if (objekt is ZahlAusdruck) {
        val ergebnis = vereinfacheMitSchritten(objekt, kontext)
        return TermVereinfachung(ergebnis.ergebnis, ergebnis.schritte)
    }

    val vereinfacht: MathematischesObjekt = when (objekt) {
        is Aussage -> vereinfacheAussage(objekt, kontext)
        is Matrix -> Matrix(objekt.zeilen.map { zeile ->
            zeile.map { wert -> vereinfache(wert, kontext) }
        })
        is SpaltenVektor -> SpaltenVektor(objekt.werte.map { wert -> vereinfache(wert, kontext) })
        is ZeilenVektor -> ZeilenVektor(objekt.werte.map { wert -> vereinfache(wert, kontext) })
        is Tupel -> Tupel(objekt.elemente.map { element -> vereinfacheTerm(element, kontext).objekt })
        is EndlicheMenge -> {
            val elemente = objekt.elemente.mapTo(linkedSetOf()) { element ->
                vereinfacheTerm(element, kontext).objekt
            }
            if (elemente.isEmpty()) LeereMenge else EndlicheMenge(elemente)
        }
        is ReellesIntervall -> reellesIntervall(
            vereinfache(objekt.links, kontext),
            objekt.linksOffen,
            vereinfache(objekt.rechts, kontext),
            objekt.rechtsOffen,
            kontext,
        )
        is Vereinigung -> vereinige(objekt.mengen.map { menge ->
            vereinfacheTerm(menge, kontext).objekt as MengenAusdruck
        })
        is Schnitt -> schneide(
            objekt.mengen.map { menge -> vereinfacheTerm(menge, kontext).objekt as MengenAusdruck },
            objekt.grundMenge?.let { grundMenge ->
                vereinfacheTerm(grundMenge, kontext).objekt as MengenAusdruck
            },
        )
        is MengenDifferenz -> mengenDifferenz(
            vereinfacheTerm(objekt.links, kontext).objekt as MengenAusdruck,
            vereinfacheTerm(objekt.rechts, kontext).objekt as MengenAusdruck,
        )
        is DefinierteMenge -> {
            val variablen = objekt.variablen.map { gebunden ->
                gebunden.copy(
                    grundMenge = vereinfacheTerm(gebunden.grundMenge, kontext).objekt as MengenAusdruck,
                )
            }
            when (val bedingung = vereinfacheAussage(objekt.bedingung, kontext)) {
                is WahrheitsKonstante -> if (bedingung.wert) variablenRaum(variablen) else LeereMenge
                else -> DefinierteMenge(variablen, bedingung)
            }
        }
        else -> objekt
    }

    if (vereinfacht == objekt) return TermVereinfachung(objekt)
    return TermVereinfachung(
        objekt = vereinfacht,
        schritte = listOf(
            UmformungsSchritt(
                vorher = objekt,
                nachher = vereinfacht,
                regelId = "standard.term-vereinfachen",
                titel = "Term vereinfachen",
                erklärung = "Unterterme wurden mit den für ihren Typ registrierten Regeln vereinfacht.",
            ),
        ),
    )
}

private fun vereinfacheAussage(
    aussage: Aussage,
    kontext: RechenKontext,
): Aussage {
    val strukturell = when (aussage) {
        is Gleichheit -> Gleichheit(
            vereinfacheTerm(aussage.links, kontext).objekt,
            vereinfacheTerm(aussage.rechts, kontext).objekt,
        )
        is Ungleichheit -> Ungleichheit(
            vereinfacheTerm(aussage.links, kontext).objekt,
            vereinfacheTerm(aussage.rechts, kontext).objekt,
        )
        is Vergleich -> Vergleich(
            vereinfache(aussage.links, kontext),
            aussage.art,
            vereinfache(aussage.rechts, kontext),
        )
        is Negation -> Negation(vereinfacheAussage(aussage.aussage, kontext))
        is Konjunktion -> Konjunktion(aussage.aussagen.map { teil -> vereinfacheAussage(teil, kontext) })
        is Disjunktion -> Disjunktion(aussage.aussagen.map { teil -> vereinfacheAussage(teil, kontext) })
        is Implikation -> Implikation(
            vereinfacheAussage(aussage.voraussetzung, kontext),
            vereinfacheAussage(aussage.folgerung, kontext),
        )
        is Äquivalenz -> Äquivalenz(
            vereinfacheAussage(aussage.links, kontext),
            vereinfacheAussage(aussage.rechts, kontext),
        )
        is Adjunktion -> Adjunktion(
            vereinfacheAussage(aussage.links, kontext),
            vereinfacheAussage(aussage.rechts, kontext),
        )
        else -> aussage
    }
    return when (strukturell.entscheide(kontext).wahrheitswert) {
        Wahrheitswert.Wahr -> WahrheitsKonstante(true)
        Wahrheitswert.Lüge -> WahrheitsKonstante(false)
        null -> strukturell
    }
}

private fun relationsVariablen(
    eingang: BedingterWert,
    relation: Aussage,
    kontext: KnotenAuswertungsKontext,
): List<GebundeneMengenVariable> {
    val ausMetadaten = eingang.variablenQuellen
        .geordnetEindeutig()
        .filter { quelle -> quelle.argumentArt == ArgumentQuellenArt.Wert }
        .map { quelle -> GebundeneMengenVariable(Variable(quelle.name), quelle.werteVorrat) }
        .filter { gebunden -> relation.enthältVariable(gebunden.variable) }
        .distinctBy { gebunden -> gebunden.variable.name }
    if (ausMetadaten.isNotEmpty()) return ausMetadaten

    val name = kontext.knoten.parameter["variable"].orEmpty().trim().ifBlank { "x" }
    val variable = Variable(name)
    if (!relation.enthältVariable(variable)) return emptyList()
    val grundMenge = eingang.reelleVariablen[name] ?: eingang.werteVorrat ?: ReelleZahlen
    return listOf(GebundeneMengenVariable(variable, grundMenge))
}

private fun variablenRaum(variablen: List<GebundeneMengenVariable>): MengenAusdruck {
    require(variablen.isNotEmpty()) { "Ein Lösungsraum benötigt mindestens eine Variable." }
    return if (variablen.size == 1) {
        variablen.single().grundMenge
    } else {
        KartesischesProdukt(variablen.map { gebunden -> gebunden.grundMenge })
    }
}

private fun löseRelation(
    eingang: BedingterWert,
    kontext: KnotenAuswertungsKontext,
): RelationsAuflösung {
    val relation = eingang.objekt as? Aussage
        ?: error("Auflösen benötigt eine Relation beziehungsweise Aussage.")
    val rechenKontext = kontext.rechenKontext.copy(
        annahmen = kontext.rechenKontext.annahmen + eingang.annahmen,
    )
    val variablen = relationsVariablen(eingang, relation, kontext)
    val entscheidung = relation.entscheide(rechenKontext)

    if (entscheidung.wahrheitswert == Wahrheitswert.Lüge) {
        return RelationsAuflösung(LeereMenge)
    }
    if (variablen.isEmpty()) {
        if (entscheidung.wahrheitswert == Wahrheitswert.Wahr) {
            error("Die Relation ist wahr, enthält aber keine freie Variable und definiert daher keinen Variablenraum.")
        }
        error("Die Relation enthält keine erkennbare freie Variable.")
    }
    if (entscheidung.wahrheitswert == Wahrheitswert.Wahr) {
        return RelationsAuflösung(variablenRaum(variablen))
    }

    if (relation is Gleichheit && variablen.size == 1) {
        val variable = variablen.single().variable
        val linear = runCatching { löseLinear(relation, variable) }.getOrNull()
        if (linear != null) {
            val menge = if (linear.lösungen.isEmpty()) {
                LeereMenge
            } else {
                EndlicheMenge(linear.lösungen.toSet())
            }
            return RelationsAuflösung(menge, linear.schritte)
        }
    }

    return RelationsAuflösung(
        menge = DefinierteMenge(variablen, vereinfacheAussage(relation, rechenKontext)),
        warnungen = listOf(
            "Die Relation konnte nicht weiter algorithmisch aufgelöst werden; die Lösungsmenge bleibt exakt in Mengenschreibweise definiert.",
        ),
    )
}

'''
replace_once(
    auswerter_pfad,
    "import de.TeutonStudio.MathematikRechenSystem.kern.*\n\nobject StandardMathematikAuswerter",
    "import de.TeutonStudio.MathematikRechenSystem.kern.*\n\n" + helper + "object StandardMathematikAuswerter",
)

regex_once(
    auswerter_pfad,
    r'''        registriere\("mathematik\.gleichungLösen"\) \{ k ->.*?\n        \}\n        registriere\("mathematik\.auswerten"\)''',
    '''        registriere("mathematik.gleichungLösen") { k ->
            val eingang = k.eingänge["relation"]
                ?: k.eingänge["gleichung"]
                ?: error("Relation fehlt.")
            val auflösung = löseRelation(eingang, k)
            val ausgangsName = k.knoten.anschlüsse.firstOrNull {
                it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang
            }?.name ?: "lösungsmenge"
            KnotenAuswertungsErgebnis(
                ausgaben = mapOf(
                    ausgangsName to BedingterWert(
                        objekt = auflösung.menge,
                        annahmen = eingang.annahmen,
                    ),
                ),
                schritte = auflösung.schritte,
                warnungen = auflösung.warnungen,
            )
        }
        registriere("mathematik.auswerten")''',
)

regex_once(
    auswerter_pfad,
    r'''        registriere\("mathematik\.auswerten"\) \{ k ->.*?\n        \}\n        registriere\("mathematik\.ableiten"\)''',
    '''        registriere("mathematik.auswerten") { k ->
            val eingang = k.eingänge["term"]
                ?: k.eingänge["objekt"]
                ?: error("Term fehlt.")
            val vereinfachung = vereinfacheTerm(eingang.objekt, k.rechenKontext)
            val ausgangsName = k.knoten.anschlüsse.firstOrNull {
                it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang
            }?.name ?: "term"
            KnotenAuswertungsErgebnis(
                ausgaben = mapOf(
                    ausgangsName to eingang.copy(
                        objekt = vereinfachung.objekt,
                        annahmen = eingang.annahmen + k.rechenKontext.annahmen,
                        latexDarstellung = null,
                    ),
                ),
                schritte = vereinfachung.schritte,
            )
        }
        registriere("mathematik.ableiten")''',
)

replace_once(
    "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/AuswertenTabellenModell.kt",
    "import de.TeutonStudio.MathematikKnoten.GAUSS_MODUS_PARAMETER\n",
    "",
)
replace_once(
    "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/AuswertenTabellenModell.kt",
    'modus = knoten.parameter[GAUSS_MODUS_PARAMETER],',
    'modus = knoten.parameter["gaussModus"],',
)
replace_once(
    "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/AuswertenInspektor.kt",
    "Dieser Knoten vereinfacht ausschließlich Zahlterme. Gleichheiten werden im Gleichheitsknoten definiert und in dafür vorgesehenen Gleichungs- oder Lösungsverfahren verarbeitet.",
    "Dieser Knoten vereinfacht mathematische Terme typ-erhaltend, darunter Zahlen, Aussagen, Matrizen, Vektoren, Tupel und Mengen. Relationen werden nicht hier gelöst, sondern im Knoten „Auflösen“.",
)

Path("MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/AuswertenUndStandardwertDarstellungTest.kt").write_text(r'''package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.VariablenQuelle
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikRechenSystem.kern.DefinierteMenge
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Gleichheit
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Matrix
import de.TeutonStudio.MathematikRechenSystem.kern.Potenz
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante
import de.TeutonStudio.MathematikRechenSystem.kern.addition
import de.TeutonStudio.MathematikRechenSystem.kern.multiplikation
import de.TeutonStudio.MathematikRechenSystem.kern.vereinfache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuswertenUndStandardwertDarstellungTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private val kartenAuswerter = KartenAuswerter(register)

    @Test
    fun `Auswerten besitzt einen typfolgenden Term Ein und Ausgang`() {
        val vorlage = alleMathematikKnotenVorlagen().single { it.art == "mathematik.auswerten" }
        val knoten = vorlage.erzeuge(GraphPunkt.Zero)
        val eingang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }
        val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals("term", eingang.name)
        assertEquals("term", ausgang.name)
        assertEquals(MathematikAnschlussArten.Objekt.id, eingang.art)
        assertEquals(MathematikAnschlussArten.Objekt.id, ausgang.art)
        assertEquals("term", ausgang.artFolgtEingang)
        assertTrue(AussagenLogikKnotenVorlagen.alle.none { it.art == "mathematik.auswerten" })
    }

    @Test
    fun `Auswerten vereinfacht Zahl Aussage und Matrix`() {
        val knoten = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val auswerter = requireNotNull(register.finde(knoten.art))
        val potenz = Potenz(RationaleZahl.von(2), RationaleZahl.von(-1))

        val zahl = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("term" to BedingterWert(potenz)),
                RechenKontext(),
            ),
        ).ausgaben.getValue("term").objekt
        assertEquals(vereinfache(potenz), zahl)

        val aussage = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "term" to BedingterWert(
                        Gleichheit(
                            Potenz(RationaleZahl.von(2), RationaleZahl.von(2)),
                            RationaleZahl.von(4),
                        ),
                    ),
                ),
                RechenKontext(),
            ),
        ).ausgaben.getValue("term").objekt
        assertEquals(WahrheitsKonstante(true), aussage)

        val matrix = Matrix(listOf(listOf(potenz)))
        val matrixErgebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf("term" to BedingterWert(matrix)),
                RechenKontext(),
            ),
        ).ausgaben.getValue("term").objekt
        assertEquals(Matrix(listOf(listOf(vereinfache(potenz)))), matrixErgebnis)
    }

    @Test
    fun `Auflösen bestimmt endliche leere universelle und symbolische Lösungsmengen`() {
        val knoten = MathematikKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        val auswerter = requireNotNull(register.finde(knoten.art))
        val x = Variable("x")
        fun löse(relation: Gleichheit) = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "relation" to BedingterWert(
                        objekt = relation,
                        variablenQuellen = listOf(VariablenQuelle(knoten.id, "x", ReelleZahlen)),
                    ),
                ),
                RechenKontext(),
            ),
        ).ausgaben.getValue("lösungsmenge").objekt

        assertEquals(
            EndlicheMenge(setOf(RationaleZahl.von(2))),
            löse(Gleichheit(multiplikation(RationaleZahl.von(2), x), RationaleZahl.von(4))),
        )
        assertEquals(
            LeereMenge,
            löse(Gleichheit(x, addition(listOf(x, RationaleZahl.Eins)))),
        )
        assertEquals(ReelleZahlen, löse(Gleichheit(x, x)))
        assertIs<DefinierteMenge>(
            löse(Gleichheit(Potenz(x, RationaleZahl.von(2)), RationaleZahl.von(2))),
        )
    }

    @Test
    fun `Auflösen akzeptiert die leere Menge als gültiges Ergebnis`() {
        val knoten = MathematikKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        val ergebnis = requireNotNull(register.finde(knoten.art)).auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "relation" to BedingterWert(
                        Gleichheit(RationaleZahl.von(1), RationaleZahl.von(2)),
                    ),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(LeereMenge, ergebnis.ausgaben.getValue("lösungsmenge").objekt)
        assertEquals(null, ergebnis.fehler)
    }

    @Test
    fun `historische Anschlussnamen bleiben lesbar`() {
        val auswertenBasis = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val historischAuswerten = auswertenBasis.copy(
            anschlüsse = auswertenBasis.anschlüsse.map { anschluss ->
                when (anschluss.richtung) {
                    AnschlussRichtung.Eingang ->
                        anschluss.copy(name = "objekt", art = MathematikAnschlussArten.Objekt.id)
                    AnschlussRichtung.Ausgang ->
                        anschluss.copy(name = "wert", art = MathematikAnschlussArten.Objekt.id)
                    AnschlussRichtung.Neutral -> anschluss
                }
            },
        )
        val auswertenErgebnis = requireNotNull(register.finde(historischAuswerten.art)).auswerten(
            KnotenAuswertungsKontext(
                historischAuswerten,
                mapOf("objekt" to BedingterWert(RationaleZahl.von(2))),
                RechenKontext(),
            ),
        )
        assertEquals(setOf("wert"), auswertenErgebnis.ausgaben.keys)

        val auflösenBasis = MathematikKnotenVorlagen.Auflösen.erzeuge(GraphPunkt.Zero)
        val historischAuflösen = auflösenBasis.copy(
            anschlüsse = auflösenBasis.anschlüsse.map { anschluss ->
                when (anschluss.richtung) {
                    AnschlussRichtung.Eingang -> anschluss.copy(name = "gleichung")
                    AnschlussRichtung.Ausgang -> anschluss.copy(name = "lösungen")
                    AnschlussRichtung.Neutral -> anschluss
                }
            },
        )
        val auflösenErgebnis = requireNotNull(register.finde(historischAuflösen.art)).auswerten(
            KnotenAuswertungsKontext(
                historischAuflösen,
                mapOf(
                    "gleichung" to BedingterWert(
                        Gleichheit(RationaleZahl.von(1), RationaleZahl.von(2)),
                    ),
                ),
                RechenKontext(),
            ),
        )
        assertEquals(setOf("lösungen"), auflösenErgebnis.ausgaben.keys)
        assertEquals(LeereMenge, auflösenErgebnis.ausgaben.getValue("lösungen").objekt)
    }

    @Test
    fun `Standardwerte erzeugen eine operative LaTeX Darstellung`() {
        val potenz = MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf(
                "standardwert.basis" to "2",
                "standardwert.exponent" to "-1",
            ),
        )
        val ergebnis = kartenAuswerter.auswerten(
            KartenDaten(name = "Standardwert-LaTeX", knoten = listOf(potenz)),
        )
        val wert = ergebnis.knoten.getValue(potenz.id).ausgaben.getValue("wert")

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        assertEquals("\\left(2\\right)^{-1}", wert.anzeigeLatex())
    }
}
''', encoding="utf-8")

replace_once(
    "app/build.gradle.kts",
    'versionCode = 2025001\n        versionName = "2.25.1"',
    'versionCode = 2025002\n        versionName = "2.25.2"',
)

roadmap = Path("release/roadmap.toml")
roadmap_text = roadmap.read_text(encoding="utf-8")
if 'version = "2.25.2"' in roadmap_text:
    raise SystemExit("release/roadmap.toml enthält v2.25.2 bereits.")
roadmap_text = roadmap_text.replace('current_version = "2.25.1"', 'current_version = "2.25.2"', 1)
roadmap_text += '''

[[releases]]
version = "2.25.2"
title = "Generische Termauswertung und Relationsauflösung"
roadmap = "v2.25.x Strukturrechner, CAS und dynamische Definitionskarten"
status = "released"
previous_release = "2.25.1"
branch = "agent/auswerten-standardwerte"
kind = "fix"
version_axis = "x"
reason = "Korrigiert die vorhandenen Knoten Auswerten und Gleichung lösen: Auswerten vereinfacht mathematische Objekte typ-erhaltend, Auflösen bestimmt oder definiert Lösungsmengen von Relationen und Standardwerte behalten eine gültige LaTeX-Pfaddarstellung. Es wird kein neuer registrierter Knotentyp eingeführt."
'''
roadmap.write_text(roadmap_text, encoding="utf-8")

Path("scripts/agent_fix_auswerten_aufloesen.py").unlink()
Path(".github/workflows/build.yml").write_text('''name: Android-Build

on:
  push:
  pull_request:

jobs:
  pruefen:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: android-actions/setup-android@v3
      - name: Android SDK 36 installieren
        run: sdkmanager "platforms;android-36" "build-tools;35.0.0"
      - name: Architektur prüfen
        run: python3 scripts/pruefe_architektur.py
      - name: Bauen und testen
        run: ./gradlew --stacktrace test :app:assembleDebug
''', encoding="utf-8")

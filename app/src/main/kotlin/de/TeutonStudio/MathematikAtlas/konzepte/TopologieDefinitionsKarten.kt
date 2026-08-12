package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikKnoten.EIGENSCHAFT_PARAMETER
import de.TeutonStudio.MathematikKnoten.MENGEN_EIGENSCHAFT_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.METHODEN_EIGENSCHAFT_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.METRISCHER_RAUM_KNOTEN_ART
import de.TeutonStudio.MathematikKnoten.MathematischeEigenschaftRegister
import de.TeutonStudio.MathematikKnoten.TOPOLOGISCHER_RAUM_KNOTEN_ART

internal fun topologieKonzeptFürKnoten(knoten: KnotenDaten): KonzeptDefinition? = when {
    knoten.art == TOPOLOGISCHER_RAUM_KNOTEN_ART -> topologischerRaumKonzept()
    knoten.art == METRISCHER_RAUM_KNOTEN_ART -> metrischerRaumKonzept()
    knoten.art == MENGEN_EIGENSCHAFT_KNOTEN_ART -> mengenEigenschaftsKonzept(knoten)
    knoten.art == METHODEN_EIGENSCHAFT_KNOTEN_ART &&
        knoten.parameter[EIGENSCHAFT_PARAMETER] == MathematischeEigenschaftRegister.Stetig.id -> stetigkeitsKonzept()
    else -> null
}

private fun topologischerRaumKonzept() = KonzeptDefinition(
    id = KonzeptId("topologie-topologischer-raum"),
    name = "Topologischer Raum",
    beschreibung = "Ein topologischer Raum ist ein Träger X zusammen mit einer Topologie τ auf X.",
    pfad = listOf("Mengenlehre", "Topologie", "Räume"),
    tags = setOf("Topologie", "Topologischer Raum", "offen", "Teilraum", "Produkttopologie", "Metrik"),
    knotenArten = setOf(TOPOLOGISCHER_RAUM_KNOTEN_ART),
    reiter = listOf(
        topologieReiter("definition", "Definition", KonzeptReiterRolle.Definition,
            "(X,τ) mit τ⊆P(X)",
            "Die Topologie ist Teil der Struktur. Eine nackte Menge besitzt im Atlas keine implizit angenommene Topologie."),
        topologieReiter("axiome", "Topologieaxiome", KonzeptReiterRolle.Äquivalenz,
            "∅,X∈τ; beliebige Vereinigungen und endliche Schnitte offener Mengen liegen wieder in τ.",
            "Diese drei Bedingungen bilden den Strukturvertrag einer Topologie."),
        topologieReiter("diskret", "Diskrete Topologie", KonzeptReiterRolle.Spezialfall,
            "τ=P(X)",
            "Jede Teilmenge ist offen und abgeschlossen."),
        topologieReiter("indiskret", "Indiskrete Topologie", KonzeptReiterRolle.Spezialfall,
            "τ={∅,X}",
            "Nur die leere Menge und der gesamte Träger sind offen und abgeschlossen."),
        topologieReiter("standard", "Standardtopologien", KonzeptReiterRolle.Spezialfall,
            "ℝ und ℂ besitzen registrierte kanonische Standardtopologien.",
            "Eine Standardtopologie wird nur verwendet, wenn sie für den konkreten Träger registriert ist."),
        topologieReiter("teilraum", "Teilraumtopologie", KonzeptReiterRolle.Spezialfall,
            "τ_Y={Y∩U | U∈τ_X}",
            "Für Y⊆X wird die Topologie relativ zum umgebenden topologischen Raum gebildet."),
        topologieReiter("produkt", "Produkttopologie", KonzeptReiterRolle.Spezialfall,
            "Die Produkttopologie auf X×Y wird von Mengen U×V mit U∈τ_X und V∈τ_Y erzeugt.",
            "Die Faktoren und ihre Topologien bleiben als Strukturreferenzen erhalten."),
        topologieReiter("metrisch", "Metrisch induzierte Topologie", KonzeptReiterRolle.Spezialfall,
            "τ_d={U⊆X | ∀x∈U ∃ε>0: B_d(x,ε)⊆U}",
            "Jede Metrik erzeugt damit eine topologische Raumsicht, die von allen topologischen Auswertern gemeinsam verwendet wird."),
        topologieReiter("beispiele", "Beispiele", KonzeptReiterRolle.Beispiel,
            "Dieselbe Menge kann mit verschiedenen Topologien verschiedene Offenheits-, Rand- und Stetigkeitseigenschaften besitzen.",
            "Der Graph macht diese Wahl deshalb durch einen eigenen Strukturknoten sichtbar."),
    ),
)

private fun metrischerRaumKonzept() = KonzeptDefinition(
    id = KonzeptId("topologie-metrischer-raum"),
    name = "Metrischer Raum",
    beschreibung = "Ein metrischer Raum (X,d) erweitert einen Träger um eine Metrik und induziert automatisch eine Topologie.",
    pfad = listOf("Mengenlehre", "Topologie", "Räume"),
    tags = setOf("Metrik", "Metrischer Raum", "Distanz", "offene Kugel", "induzierte Topologie", "Stetigkeit"),
    knotenArten = setOf(METRISCHER_RAUM_KNOTEN_ART),
    reiter = listOf(
        topologieReiter("definition", "Definition", KonzeptReiterRolle.Definition,
            "(X,d), d:X×X→ℝ_{≥0}",
            "Die Metrik ist eine Methode auf dem Träger; der metrische Raum liefert zusätzlich seine induzierte topologische Raumsicht."),
        topologieReiter("axiome", "Metrikaxiome", KonzeptReiterRolle.Äquivalenz,
            "d(x,y)=0⇔x=y; d(x,y)=d(y,x); d(x,z)≤d(x,y)+d(y,z)",
            "Zusammen mit Nichtnegativität bilden diese Bedingungen den Metrikvertrag."),
        topologieReiter("kugeln", "Offene Kugeln", KonzeptReiterRolle.Spezialfall,
            "B_d(x,ε)={y∈X | d(x,y)<ε}",
            "Offene Kugeln erzeugen die von d induzierte Topologie."),
        topologieReiter("topologie", "Induzierte Topologie", KonzeptReiterRolle.Äquivalenz,
            "U∈τ_d ⇔ ∀x∈U ∃ε>0: B_d(x,ε)⊆U",
            "Es existiert kein paralleler metrischer Offenheitsbegriff; verwendet wird die allgemeine Topologiesemantik."),
        topologieReiter("stetigkeit", "Stetigkeit", KonzeptReiterRolle.Äquivalenz,
            "f:(X,d_X)→(Y,d_Y) ist stetig genau dann, wenn die zugehörige Abbildung zwischen (X,τ_{d_X}) und (Y,τ_{d_Y}) stetig ist.",
            "Die ε-δ-Charakterisierung ist eine Auswertungsstrategie derselben Stetigkeitsaussage."),
        topologieReiter("standardmetriken", "Standardmetriken", KonzeptReiterRolle.Spezialfall,
            "Auf ℝⁿ erzeugt die euklidische Metrik die übliche Topologie; die diskrete Metrik erzeugt die diskrete Topologie.",
            "Standardmetriken werden als konkrete Methoden bzw. registrierte Verträge modelliert."),
        topologieReiter("beispiele", "Beispiele", KonzeptReiterRolle.Beispiel,
            "Ein metrischer Raum kann überall dort als topologischer Kontext dienen, wo Offenheit, Rand oder Stetigkeit benötigt werden.",
            "Die induzierte Topologie wird reproduzierbar aus der Metrik abgeleitet und nicht separat persistiert."),
    ),
)

private fun mengenEigenschaftsKonzept(knoten: KnotenDaten): KonzeptDefinition? {
    val id = knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty()
    val definition = MathematischeEigenschaftRegister.finde(id) ?: return null
    if (definition.subjektArt.name != "Menge") return null
    val istKardinal = definition.gruppe.name == "Kardinalität"
    val istTopologisch = definition.gruppe.name == "Topologie"
    val istAffin = definition.gruppe.name == "Konvexität"
    if (!istKardinal && !istTopologisch && !istAffin) return null

    return KonzeptDefinition(
        id = KonzeptId("mengeneigenschaft-${definition.id}"),
        name = definition.titel,
        beschreibung = "Mengeneigenschaften sind nach der Struktur getrennt, die zu ihrer Definition benötigt wird.",
        pfad = when {
            istKardinal -> listOf("Mengenlehre", "Mengeneigenschaften", "Kardinalität")
            istTopologisch -> listOf("Mengenlehre", "Mengeneigenschaften", "Topologie")
            else -> listOf("Mengenlehre", "Mengeneigenschaften", "Affine Eigenschaften")
        },
        tags = setOf(definition.titel, definition.adjektiv, definition.gruppe.name, "Mengeneigenschaft"),
        knotenArten = setOf(MENGEN_EIGENSCHAFT_KNOTEN_ART),
        knotenParameter = mapOf(EIGENSCHAFT_PARAMETER to definition.id),
        reiter = listOf(
            topologieReiter("definition", "Definition", KonzeptReiterRolle.Definition,
                "${definition.adjektiv}: strukturabhängige Aussage über eine Menge",
                when {
                    istKardinal -> "Diese Eigenschaft hängt nur vom Kardinalitätsvertrag der Menge ab und benötigt ausdrücklich keine Topologie."
                    istTopologisch -> "Diese Eigenschaft ist erst relativ zu einem verbundenen topologischen Raum (X,τ) definiert."
                    else -> "Diese Eigenschaft benötigt eine affine bzw. konvexe Struktur und wird nicht aus der Topologie abgeleitet."
                }),
            topologieReiter("kardinalitaet", "Kardinalität", KonzeptReiterRolle.Spezialfall,
                "endlich / unendlich und abzählbar / überabzählbar",
                "Die beiden Paare sind unabhängige Achsen. Jede endliche Menge ist dabei abzählbar. Begriffe wie abzählbar gehören ausschließlich hierher."),
            topologieReiter("topologie", "Topologie", KonzeptReiterRolle.Spezialfall,
                "offen / abgeschlossen",
                "Diese Eigenschaften benötigen Menge A, topologischen Raum (X,τ) und den Nachweis A⊆X. Kardinalität wird hier nicht verwendet."),
            topologieReiter("affin", "Affine Eigenschaften", KonzeptReiterRolle.Spezialfall,
                "konvex",
                "Konvexität benötigt einen affinen Kontext und ist weder eine Kardinalitäts- noch eine rein topologische Eigenschaft."),
        ),
    )
}

private fun stetigkeitsKonzept() = KonzeptDefinition(
    id = KonzeptId("analysis-stetigkeit-topologisch"),
    name = "Stetigkeit",
    beschreibung = "Stetigkeit einer Methode ist relativ zu einer Quell- und einer Zieltopologie definiert.",
    pfad = listOf("Analysis", "Eigenschaften", "Regularität"),
    tags = setOf("Stetigkeit", "Topologie", "Urbild", "Metrik", "ε-δ"),
    knotenArten = setOf(METHODEN_EIGENSCHAFT_KNOTEN_ART),
    knotenParameter = mapOf(EIGENSCHAFT_PARAMETER to MathematischeEigenschaftRegister.Stetig.id),
    reiter = listOf(
        topologieReiter("definition", "Definition", KonzeptReiterRolle.Definition,
            "f:(X,τ_X)→(Y,τ_Y) stetig ⇔ ∀V∈τ_Y: f⁻¹(V)∈τ_X",
            "Quell- und Zieltopologie sind Bestandteil der Aussage und müssen mit der Methodensignatur kompatibel sein."),
        topologieReiter("metrisch", "Metrische Charakterisierung", KonzeptReiterRolle.Äquivalenz,
            "∀ε>0 ∃δ>0: d_X(x,x₀)<δ ⇒ d_Y(f(x),f(x₀))<ε",
            "Für metrische Räume ist dies dieselbe Stetigkeit, ausgewertet über die induzierten Topologien."),
        topologieReiter("beispiele", "Beispiele", KonzeptReiterRolle.Beispiel,
            "Jede Abbildung aus einem diskreten Raum und jede Abbildung in einen indiskreten Raum ist stetig.",
            "Dieselbe Vorschrift kann bei anderer Wahl der Topologien einen anderen Stetigkeitsstatus besitzen."),
    ),
)

private fun topologieReiter(
    id: String,
    titel: String,
    rolle: KonzeptReiterRolle,
    regel: String,
    erklärung: String,
): KonzeptReiter = KonzeptReiter(
    id = id,
    titel = titel,
    rolle = rolle,
    karte = topologieRegelKarte("topologie-$id-${regel.hashCode().toUInt()}", titel, regel, erklärung),
)

private fun topologieRegelKarte(id: String, titel: String, regel: String, erklärung: String): KartenDaten = KartenDaten(
    id = KartenId(id),
    name = titel,
    knoten = listOf(
        KnotenDaten(
            id = KnotenId("$id-regel"),
            art = KonzeptKnotenArten.REGEL,
            name = titel,
            position = GraphPunkt(70f, 60f),
            größe = GraphGröße(760f, 230f),
            parameter = mapOf("regel" to regel, "erklärung" to erklärung),
        ),
    ),
)

package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnsichtsFenster
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen

internal fun matrixProduktKonzept(vorlage: KnotenVorlage = MathematikKnotenVorlagen.MatrixProdukt): KonzeptDefinition =
    KonzeptDefinition(
        id = KonzeptId("matrixprodukt"),
        name = vorlage.name,
        beschreibung = vorlage.beschreibung,
        pfad = vorlage.kategorie.split(':').map(String::trim).filter(String::isNotBlank),
        tags = setOf("Matrixprodukt", "Falksches Schema", "Lineare Algebra", vorlage.art),
        knotenArten = setOf(vorlage.art),
        reiter = listOf(
            KonzeptReiter(
                id = "definition",
                titel = "Definition",
                rolle = KonzeptReiterRolle.Definition,
                karte = TestDefinitionsKarten.definitionsKarte(vorlage, 0),
            ),
            KonzeptReiter(
                id = "falksches-schema",
                titel = "Falksches Schema",
                rolle = KonzeptReiterRolle.Beispiel,
                karte = falkschesSchemaDefinitionsKarte(),
            ),
        ),
    )

/**
 * Selbstbezugsfreie Dokumentationskarte des binären Falk-Schemas.
 * Die fachliche Eintragsberechnung stammt aus dem UI-unabhängigen FalkSchemaModell im Rechenkern.
 */
internal fun falkschesSchemaDefinitionsKarte(): KartenDaten {
    val linkeMatrix = regelKnoten(
        id = "falk-links",
        name = "A unten links",
        x = 40f,
        y = 330f,
        breite = 280f,
        text = "Wähle Zeile i der linken Matrix A. Ihre Einträge bleiben in der gespeicherten Reihenfolge aᵢ₁,…,aᵢₙ.",
    )
    val rechteMatrix = regelKnoten(
        id = "falk-rechts",
        name = "B oben rechts",
        x = 390f,
        y = 40f,
        breite = 300f,
        text = "Wähle Spalte j der rechten Matrix B. Erforderlich ist spalten(A)=zeilen(B).",
    )
    val ergebnis = regelKnoten(
        id = "falk-ergebnis",
        name = "C = A·B unten rechts",
        x = 390f,
        y = 330f,
        breite = 360f,
        text = "Der gewählte Eintrag ist cᵢⱼ. Das Ergebnis besitzt zeilen(A) Zeilen und spalten(B) Spalten.",
    )
    val summenRegel = regelKnoten(
        id = "falk-summe",
        name = "Geordnete Produktsumme",
        x = 90f,
        y = 590f,
        breite = 660f,
        text = "cᵢⱼ = Σₖ₌₁ⁿ aᵢₖ·bₖⱼ. Keine Konjugation und keine Vertauschung: Auch über ℍ steht aᵢₖ stets vor bₖⱼ.",
    )
    val schrittRegel = regelKnoten(
        id = "falk-schritt",
        name = "Tatsächlicher binärer Schritt",
        x = 800f,
        y = 180f,
        breite = 360f,
        text = "Bei mehreren Faktoren zeigt das Schema genau einen Schritt der gespeicherten Klammerung, niemals zwei nur zufällig benachbarte Ursprungsmatrizen.",
    )

    return KartenDaten(
        id = KartenId("matrixprodukt-falksches-schema"),
        name = "Falksches Schema des Matrixprodukts",
        knoten = listOf(linkeMatrix, rechteMatrix, ergebnis, summenRegel, schrittRegel),
        ansicht = AnsichtsFenster(zoom = .72f),
    )
}

private fun regelKnoten(
    id: String,
    name: String,
    x: Float,
    y: Float,
    breite: Float,
    text: String,
): KnotenDaten = KnotenDaten(
    id = KnotenId(id),
    art = TestDefinitionsKarten.KONZEPT_REGEL_ART,
    name = name,
    position = GraphPunkt(x, y),
    größe = GraphGröße(breite, 170f),
    parameter = mapOf(
        "regel" to text,
        "knotenArt" to MathematikKnotenVorlagen.MatrixProdukt.art,
    ),
)

package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*

/** Dynamische Definitionen passen zur gespeicherten Konfiguration, ohne statische Assetvariante. */
internal fun alphaStrukturKonzeptFürKnoten(knoten: KnotenDaten): KonzeptDefinition? = when (knoten.art) {
    ÜBERSETZER_ART -> übersetzerKonzept(knoten)
    TUPEL_ABFLACHEN_ART -> strukturKonzept(
        "tupel-abflachen", "Tupel abflachen", TUPEL_ABFLACHEN_ART,
        "Rekursive Tiefensuche öffnet ausschließlich Tupel und erhält die Reihenfolge ihrer Blätter.",
        listOf(
            "Definition: DFS von links nach rechts" to "DFS von links nach rechts: F(())=(); F((a₁,…,aₙ))=F(a₁) ⧺ … ⧺ F(aₙ). Für ein Blatt b, das kein Tupel ist, gilt F(b)=(b). ⧺ verbindet die Ergebnislisten geordnet.",
            "Blätter und leere Tupel" to "Der Eingang muss ein Tupel sein. Leere innere Tupel tragen keine Elemente bei; vollständig leere Verschachtelung ergibt (). Mengen, Zeilen, Spalten, Aussagen und andere mathematische Objekte bleiben jeweils ein unverändertes Blatt.",
            "Beispiel und Metadaten" to "((1,()),(2,(3,)),{4}) wird zu (1,2,3,{4}). Es wird weder sortiert noch dedupliziert. Annahmen und Variablenquellen bleiben erhalten; die Zielmenge wird für das abgeflachte Ergebnis neu bestimmt.",
        ),
    )
    else -> null
}

private fun übersetzerKonzept(knoten: KnotenDaten): KonzeptDefinition {
    val ziel = übersetzerZiel(knoten)
    val regel = when (ziel) {
        "grad" -> "q: R → Grad; x ↦ [180x/π]°. R wird als Bogenmaß gelesen, vollständige Umdrehungen werden identifiziert."
        "bogenmass" -> "Grad → R: w ↦ sein Hauptvertreter im gespeicherten Bogenmaßintervall. Diese Auswahl ist eine Rechtsinverse von q; R → Grad → R normalisiert auf dieses Intervall."
        "polar" -> "(x₁,…,xₙ) → (r,(φ,θ₁,…)). r ist die euklidische Norm; φ=atan2(x₂,x₁); θₖ=atan2(‖(x₁,…,xₖ₊₁)‖,xₖ₊₂). Nicht bestimmte Teilwinkel werden 0°. R¹: x → (|x|,sign(x))."
        "kartesisch" -> "(r,(φ,θ₁,…)) → (x₁,…,xₙ), rekursiv mit Sinus und Cosinus. In R³: x=r sin θ cos φ, y=r sin θ sin φ, z=r cos θ. Bei aktivem R¹-Modus: (r,s) → (r·s)."
        "spalte" -> "Tupel (a₁,…,aₙ) → Spalte [a₁;…;aₙ]. Zeile [a₁,…,aₙ] → dieselbe Komponentenfolge als Spalte, ohne Konjugation."
        "zeile" -> "Tupel (a₁,…,aₙ) → Zeile [a₁,…,aₙ]. Spalte [a₁;…;aₙ] → dieselbe Komponentenfolge als Zeile, ohne Konjugation."
        "tupel" -> "Zeile [a₁,…,aₙ] oder Spalte [a₁;…;aₙ] → Tupel (a₁,…,aₙ), in unveränderter Komponentenreihenfolge."
        "indexmethode" -> "Struktur A → Indexmethode a: (i₁,…,iᵣ) ↦ Aᵢ₁,…,ᵢᵣ. Tupel und Vektoren besitzen einen Index, Matrizen zwei, Tensoren einen Index je Achse. Jeder Index beginnt bei 1. Verschachtelte Tupel bleiben Komponenten und werden nicht abgeflacht."
        "methode" -> "Term t → Methode f mit internem Vertrag Tupel → Tupel. Ohne gebundene Parameter: f(())=(c), Definitionsraum {()}. Mit x: f((x))=(t(x)). Eine bereits tupelförmige Vorschrift erhält ihre äußere Tupelstruktur; auch () und Einertupel bleiben strukturell erhalten."
        else -> "Das gespeicherte Ziel '$ziel' ist unbekannt; es wird keine Ersatzübersetzung angenommen."
    }
    val zulässigkeit = if (ziel in setOf("grad", "bogenmass")) "Grad ist ein eigener Winkelraum, keine reelle Teilmenge. Vertreterintervall: ${übersetzerVertreter(knoten).menge.zuLatex()}. 360°=0°. Die additive Winkelgruppe besitzt keine von der üblichen reellen Multiplikation induzierte Ringstruktur."
    else if (ziel in setOf("polar", "kartesisch"))
        de.TeutonStudio.MathematikRechenSystem.kern.Polarkoordinaten.KONVENTION +
            " Kartesisch: nichtleere flache Tupel reeller Komponenten. Polar ab R²: Radius und verschachteltes Winkeltupel; historische flache Polartupel bleiben lesbar. Radius nichtnegativ; skalares Vorzeichen −1,0,+1, wobei 0 nur mit Radius 0 erlaubt ist. Ergebnisse sind numerische Näherungen."
    else if (ziel == "indexmethode")
        "Konkrete Tupel, Vektoren, Matrizen und Tensoren sind zulässig; symbolische Komponenten bleiben erhalten. Indexanzahl und Grenzen werden geprüft. Der Zielraum enthält die Komponenten einschließlich verschachtelter Tupel."
    else if (ziel == "methode")
        "Nur tatsächlich freie, ausdrücklich als Methodenparameter markierte Quellen werden gebunden. Explizite Argumentreihenfolge gilt zuerst, übrige Namen folgen deterministisch. Freie Außenquellen bleiben erhalten; fehlende oder widersprüchliche gleichnamige Quellen sind Fehler. Annahmen werden beim Aufruf substituiert."
    else "Tupel → Zeile/Spalte benötigt ein nichtleeres, flaches Tupel aus ZahlAusdruck-Komponenten; symbolische und komplexe Zahlen sind erlaubt. Verschachtelte Tupel, Mengen, Aussagen und leere Tupel sind dabei unzulässig. Identische Darstellungen werden nicht nochmals übersetzt."
    return strukturKonzept("uebersetzer-$ziel", "Übersetzer · Ziel $ziel", ÜBERSETZER_ART,
        "Expliziter Darstellungswechsel mit gespeichertem Ziel '$ziel'; ein Eingang wert und ein Ausgang ergebnis.",
        listOf("Definition: Ziel $ziel" to regel, "Zulässigkeit und Parameter" to zulässigkeit,
            "Verträge und Erhalt" to "Erlaubt sind Tupel ↔ Zeile/Spalte, Zeile ↔ Spalte ohne Konjugation, kartesisch ↔ polar und Term → Methode. Annahmen und nicht gebundene Quellen bleiben erhalten; Zielmetadaten entsprechen dem Ergebnis. Zielwechsel behalten kompatible Anschluss-IDs und zeigen Verbindungsverlust vor der atomaren, rückgängig machbaren Übernahme."))
}

private fun strukturKonzept(id: String, titel: String, art: String, beschreibung: String, regeln: List<Pair<String,String>>): KonzeptDefinition =
    KonzeptDefinition(id=KonzeptId(id),name=titel,beschreibung=beschreibung,pfad=listOf("Strukturen","Tupel und Übersetzungen"),
        tags=setOf("Tupel","Übersetzung","Definition",art),knotenArten=setOf(art),
        reiter=listOf(KonzeptReiter("definition","Definition",KonzeptReiterRolle.Definition,
            KartenDaten(id=KartenId("definition-$id"),name="Definition: $titel",knoten=regeln.mapIndexed { index,(name,regel) ->
                KnotenDaten(id=KnotenId("definition-$id-regel-$index"),art=KonzeptKnotenArten.REGEL,name=name,
                    position=GraphPunkt(40f,index*260f+40f),größe=GraphGröße(780f,230f),parameter=mapOf("regel" to regel,"knotenArt" to art))
            }))))

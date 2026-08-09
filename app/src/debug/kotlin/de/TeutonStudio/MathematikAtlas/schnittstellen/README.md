# Schnittstellenkatalog

Dieser Debug-Quellordner macht die sichtbaren Oberflächen des Mathematik Atlas einzeln in Android Studio renderbar. Jede Vorschau verwendet die echte Produktionskomponente. Vorschau-Dateien dürfen keine zweite Layoutimplementierung enthalten.

## Regeln

1. Neue Dialoge, Fenster, relevante Inspektoren und größere UI-Bereiche erhalten zusammen mit der Produktionskomponente eine eigene `<Komponente>Vorschau.kt`.
2. Jede Vorschau besitzt aussagekräftige, fachlich plausible und deterministische Testdaten.
3. Leere Standard-Previews, `Lorem ipsum`, generische `Test`-Beschriftungen und zufällige UUIDs sind unzulässig.
4. Für layoutkritische Oberflächen werden schmale, breite, helle, dunkle oder fehlerhafte Zustände ergänzt.
5. Gemeinsame Fixtures liegen in `VorschauDaten.kt`; komponentenspezifische Daten bleiben in der jeweiligen Vorschau-Datei.
6. Netzwerk, Dateiauswahl, Teilen, GitHub, externe Intents und dauerhafte Hintergrundaufgaben werden nicht ausgelöst.
7. Der Ordner liegt ausschließlich im Debug-Source-Set und gelangt nicht in Release-Builds.

## Katalog

| Oberfläche | Produktionsdatei | Vorschau-Datei | Zustände | Aussagekräftige Testdaten |
|---|---|---|---|---|
| Gesamtoberfläche | `MathematikAtlasApp.kt` | `MathematikAtlasAppVorschau.kt` | Desktop, dunkel | vorhandene Beispielkarten und mathematische Knoten |
| Verwaltungsfenster | `VerwaltungsFenster.kt` | `VerwaltungsFensterVorschau.kt` | Karten, Auswertung, hell/dunkel | lokale Beispielkarten und ausgewertete Knotenzustände |
| Knoten einfügen | `KnotenAuswahlFenster.kt` | `KnotenAuswahlFensterVorschau.kt` | Standardliste, Matrixsuche, Konzeptbibliothek | registrierte produktive Knotenvorlagen |
| Konzeptbibliothek | `KonzeptBibliothekDialog.kt` / `KonzeptBibliothekUi.kt` | `KonzeptBibliothekDialogVorschau.kt` | Fachgebietsübersicht | vollständiges produktives Konzeptregister |
| Vollständiger Inspector | `KnotenInspektorFenster.kt` | `KnotenInspektorFensterVorschau.kt` | ausgewählte Matrixdiagonale | Nebendiagonale mit realer Inspector-Konfiguration |
| Matrixdiagonale-Inspector | `MatrixdiagonaleInspektor.kt` | `MatrixdiagonaleInspektorVorschau.kt` | hell/dunkel, Telefon/Tablet | rechteckig interpretierte Nebendiagonale |
| Endliche-Menge-Inspector | `EndlicheMengeInspektor.kt` | `EndlicheMengeInspektorVorschau.kt` | befüllt, hell/dunkel | Primzahlen `2, 3, 5, 7` |
| Zahlenrechner-Inspector | `ZahlenRechnerInspektor.kt` | `ZahlenRechnerInspektorVorschau.kt` | Potenz, hell/dunkel | reeller Potenzoperator mit plausibler Benennung |
| Rechner-Operatorauswahl | `RechnerOperatorAuswahlDialog.kt` | `RechnerOperatorAuswahlDialogVorschau.kt` | breit mit Verbindungswarnung, kompakt | Addition, Division, Sinus und eigene Formel mit deterministischer Ersetzungswarnung |
| Geometrie-Teilobjekt-Inspector | `GeometrieTeilobjektInspektor.kt` | `GeometrieTeilobjektInspektorVorschau.kt` | unverbundene Kante, hell/dunkel | dritte Kante eines Würfels als Auswahlabsicht |
| KartenKnoten-Inspector | `KartenKnotenInspektor.kt` | `KartenKnotenInspektorVorschau.kt` | Methodenmodus | reale gespeicherte Beispielkarte und Version |
| Iterierte Kartenmethode | `IterierteMethodenKartenInspektor.kt` | `IterierteMethodenKartenInspektorVorschau.kt` | ausgewählte Kartenmethode | Summe der Quadratzahlen mit Karten-Fallback |
| Formelbauer | `FormelBauerDialog.kt` | `FormelBauerDialogVorschau.kt` | strukturierte Formel, Summe, hell/dunkel | Bruch mit Sinus und Wurzel; endliche Quadratsumme |
| Aussagenoperator | `AussagenOperatorDialog.kt` | `AussagenOperatorDialogVorschau.kt` | Adjunktionstabelle | Adjunktion mit drei Aussageeingängen |
| Knotendefinition | `konzepte/KonzeptUi.kt` | `KnotenKonzeptDialogVorschau.kt` | Matrixdiagonale | echte Definitionskarte des Knotens |
| Karten-JSON | `KartenJsonDialogV2311.kt` | `KartenJsonDialogVorschau.kt` | befüllte Karte, dunkel | Karte „Lineares Gleichungssystem mit Gauß-Verfahren“, Version 4 |
| Profilverwaltung | `ProfilVerwaltungDialog.kt` | `ProfilVerwaltungDialogVorschau.kt` | befülltes lokales Profil | deterministisches lokales Profil und Beispielkarten |
| Profilfarbauswahl | `ProfilFarbAuswahl.kt` | `ProfilFarbAuswahlVorschau.kt` | Petrol, Violett, hell/dunkel | exakte Hex-Farben `#0F766E` und `#7C3AED` |
| Visuelle Gruppenebene | `VisuelleGruppenEbene.kt` | `VisuelleGruppenEbeneVorschau.kt` | Gruppe mit zwei Knoten | Gruppe „Matrixoperationen und Auswertung“ |
| Namensdialog | `MathematikAtlasApp.kt` | `NameAendernDialogVorschau.kt` | lange Bezeichnung | lange fachliche Bezeichnung einer linearen Abbildung |

## Bewusst nicht separat previewt

- `FreigabeTeilen.kt` enthält ausschließlich die Android-Intent-Integration und keine sichtbare Compose-Oberfläche.
- `ProfilScroll.kt` ist nur eine lokale Modifier-Brücke.
- `KartenJsonDialog.kt` ist die ältere JSON-Oberfläche. Die produktiv verwendete Fassung `KartenJsonDialogV2311.kt` besitzt die Preview.
- Kleine Tabellenzellen, Divider, Spacer und rein interne Unterkomponenten werden über ihre übergeordnete Oberfläche beurteilt.

## Prüfen

```bash
./gradlew :app:compileDebugKotlin test :app:assembleDebug
```

Die Architekturtests prüfen Dateikonvention, reale Produktionsimporte, vorhandene Vorschauannotationen und verbotene Platzhalterdaten.

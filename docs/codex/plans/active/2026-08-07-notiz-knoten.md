# Notiz-Knoten `karte.notiz`

## 1. Titel und Status

Status: **Implementierung auf Draft-Branch abgeschlossen, CI- und Releaseabschluss noch offen.**

Branch: `samai/v2.27.0/notiz-knoten`

Pull Request: #336

Geplanter neuer Typ-Schlüssel: `karte.notiz`

Versionsklassifikation: neue `y`-Version, weil ein neuer separat erzeugbarer Knotentyp registriert wird.

Der Branchname verwendet vorläufig `v2.27.0`. Eine formale Release-Reservierung ist noch nicht als abgeschlossen dokumentiert, weil der aktuelle Masterzustand widersprüchliche Versionsinformationen enthält: Der Master-Commit trägt die Nachricht `v2.26.1`, während `app/build.gradle.kts` und `release/roadmap.toml` am selben Stand noch `2.25.5` angeben. Die Featureimplementierung verändert diese widersprüchlichen Release-Metadaten nicht nebenbei.

## 2. Ziel und Nutzerwirkung

Der Atlas erhält einen frei erzeugbaren, nichtmathematischen Notiz-Knoten. Er dient dazu, Hinweise, erwartete Ergebnisse und Dokumentation direkt auf einer Karte abzulegen, insbesondere für die geplanten Standardkarten aus Analysis I und II.

Der Nutzer kann:

- mehrzeiligen Text im Inspector bearbeiten,
- linksbündige, rechtsbündige, zentrierte oder Blocksatz-Darstellung wählen,
- die Schriftgröße zwischen 8 und 96 sp ändern,
- den Knoten mit dem vorhandenen Resize-Griff vergrößern und verkleinern.

Der Knoten besitzt keine Anschlüsse und beeinflusst die mathematische Auswertung nicht.

## 3. Nicht-Ziele

Nicht Teil dieser Umsetzung sind:

- Markdown oder Rich Text,
- LaTeX-Auswertung innerhalb der Notiz,
- Inline-Bearbeitung direkt auf der Karte,
- eigene Farben, Rahmen oder Hintergrundoptionen,
- automatische Größenanpassung an Text,
- mathematische Ein- oder Ausgänge,
- ein künstlicher Mathematikauswerter für Präsentationsknoten.

## 4. Untersuchter Istzustand

- `KnotenVorlage` unterstützt leere Anschlusslisten, Standardparameter und persistente Standardgrößen.
- `KnotenKartenEditor` rendert Anschlüsse ausschließlich aus `KnotenDaten.anschlüsse`; eine leere Liste ergibt daher natürlich einen anschlusslosen Knoten.
- Die vorhandene Knotengröße liegt in `KnotenDaten.größe` und wird über `KartenAktion.KnotenGrößeÄndern` verändert. Der Editor besitzt bereits den globalen Resize-Griff und Mindestmaße.
- `KnotenRenderer` unterstützt `KnotenInteraktionsModus.GanzeFlächeZiehbar`.
- `KnotenInspektorFenster` schreibt Parameter über `KartenAktion.KnotenParameterÄndern`.
- `AtlasZustand` kombiniert die vorhandenen Vorlagenkataloge und wählt spezialisierte Renderer.
- `KartenAuswerter` meldete für jede unbekannte Knotenart bislang `Kein Auswerter ... registriert`; ein Präsentationsknoten würde daher ohne expliziten Vertrag eine ansonsten gültige Karte fehlerhaft machen.

## 5. Fachliche und mathematische Semantik

Der Notiz-Knoten besitzt **keine mathematische Semantik**. Er ist ausschließlich Präsentations- und Dokumentationsinhalt einer Karte.

Die mathematische Auswertung muss ihn vollständig ignorieren. Gleichzeitig dürfen weiterhin echte unbekannte mathematische oder auswertbare Knotenarten als Fehler erkannt werden.

## 6. Daten-, Node-, Handle- und Edge-Vertrag

Vorlage:

```text
art:                 karte.notiz
name:                Notiz
kategorie:           Darstellung
standardGröße:       280 × 160
anschlüsse:          []
```

Persistente Parameter:

```text
text                 String, Standard ""
textAusrichtung      links | rechts | zentriert | blocksatz
schriftgrößeSp       Ganzzahl als String, 8..96, Standard 16
```

Renderer-Fallbacks:

- unbekannte Ausrichtung → links,
- ungültige oder außerhalb 8..96 liegende Schriftgröße → 16 sp.

Handle-/Edge-Vertrag:

- keine Eingänge,
- keine Ausgänge,
- keine neutralen oder dynamischen Anschlüsse,
- dadurch keine gültige Edge zum oder vom Notiz-Knoten.

## 7. Architekturentscheidungen

1. Der Knoten liegt als anwendungsspezifisches Kartenwerkzeug im `app`-Modul und nicht im mathematischen Vorlagenkatalog.
2. `KartenAuswerter` erhält einen generischen Satz explizit nicht auswertbarer Knotenarten. Der Adapter kennt dadurch keinen speziellen Schlüssel `karte.notiz`.
3. `AtlasZustand` konfiguriert `karte.notiz` als nicht auswertbar und registriert Vorlage sowie Renderer.
4. Der Inspector verwendet ausschließlich die vorhandene Kartenaktionspipeline.
5. Die Knotengröße bleibt ausschließlich `KnotenDaten.größe`; es entsteht kein zweites Breiten-/Höhenmodell.

## 8. Betroffene Dateien und Symbole

- `app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/NotizKnoten.kt`
  - `KartenWerkzeugVorlagen`
  - `NotizKnotenRenderer`
  - `NotizKnotenInspektor`
  - Formatierungs- und Validierungsfunktionen
- `app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/AtlasZustand.kt`
  - Vorlagenkatalog
  - Rendererwahl
  - Konfiguration nicht auswertbarer Knotenarten
- `app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KnotenInspektorFenster.kt`
  - Inspectorwahl
- `MathematikKartenAdapter/src/main/kotlin/de/TeutonStudio/MathematikKartenAdapter/KartenAuswerter.kt`
  - generischer Vertrag `nichtAuswertbareKnotenArten`
- `MathematikKartenAdapter/src/test/kotlin/de/TeutonStudio/MathematikKartenAdapter/KartenAuswerterTest.kt`
- `app/src/test/kotlin/de/TeutonStudio/MathematikAtlas/NotizKnotenTest.kt`

## 9. Meilensteine

- [x] Bestand und vergleichbare Pfade untersucht.
- [x] Anschlusslose Vorlage und Kartenwerkzeug-Katalog implementiert.
- [x] Eigenen Renderer mit bestehender Knotengröße implementiert.
- [x] Inspector für Text, Ausrichtung und Schriftgröße implementiert.
- [x] Generischen Vertrag für nicht auswertbare Präsentationsknoten implementiert.
- [x] Adapter- und Persistenztests ergänzt.
- [-] CI-Verifikation. Release-Guard ist grün; vollständiger Android-Build läuft. Der separate Mathematikkern-Workflow wird vor Tests durch eine bestehende Gradle-Wrapper-Validierung blockiert.
- [!] Releaseabschluss. Die Versionsmetadaten auf `master` widersprechen der Commitbezeichnung und müssen vor einem korrekten Release durch den Release-/Master-Verwalter konsolidiert werden.
- [ ] Unabhängige Node-Verifikation und Verschieben dieses Plans nach `plans/completed/`.

## 10. Konkrete Umsetzungsschritte

1. `KartenWerkzeugVorlagen.Notiz` mit leeren Anschlüssen und stabilen Standardparametern anlegen.
2. `NotizKnotenRenderer` registrieren und Text auf die aktuelle Kartenfläche umbrechen lassen; Überlauf wird abgeschnitten, die Höhe bleibt nutzergesteuert.
3. `NotizKnotenInspektor` an die bestehende Inspector-Aktionspipeline anbinden.
4. `KartenAuswerter` um eine generische Menge nicht auswertbarer Typen erweitern und aus `AtlasZustand` konfigurieren.
5. Neue Vorlage in den normalen Erstellen-Katalog integrieren.
6. Unit- und JSON-Roundtrip-Tests ergänzen.
7. Diff und CI prüfen; nur featurebezogene Fehler korrigieren.
8. Releaseintegration erst nach konsistentem Versionsplan durchführen.

## 11. Tests und Validierung

Implementierte Tests prüfen:

- Art-Schlüssel, Kategorie, Standardgröße und leere Anschlussliste,
- alle vier Textausrichtungen sowie Fallback,
- Schriftgrößenbereich und Fallback,
- mehrzeiligen Unicode-Text,
- Persistenz von Text, Ausrichtung, Schriftgröße und gezogener Größe,
- fehlerfreie Auswertung explizit nicht auswertbarer Knoten,
- unveränderte Fehlerdiagnose für echte unbekannte auswertbare Knoten.

Repository-CI:

- `Release-Guard`: erfolgreich.
- `Mathematikkern prüfen`: Infrastrukturfehler vor den Projektprüfungen, weil `gradle/actions/setup-gradle@v4` den bereits vorhandenen `gradle/wrapper/gradle-wrapper.jar`-Hash nicht als bekannten offiziellen Wrapper akzeptiert. Der Feature-Diff verändert den Wrapper nicht.
- `Android-Build`: führt `python3 scripts/pruefe_architektur.py` sowie `./gradlew --stacktrace test :app:assembleDebug` aus; Ergebnis bei letzter Planaktualisierung noch offen.

Mangels lokal eingebundenem Repository konnten die Gradle-Befehle in dieser Sitzung nicht zusätzlich lokal ausgeführt werden.

## 12. Persistenz und Migration

Es ist keine Schemaänderung erforderlich. Die Notiz verwendet ausschließlich bestehende `KnotenDaten`-Felder:

- `art`,
- `name`,
- `größe`,
- `parameter`,
- leere `anschlüsse`.

`KartenJson` benötigt daher keine Formatversionserhöhung oder Migration. Ein Roundtrip-Test sichert mehrzeiligen Unicode-Text und alle neuen Parameter.

## 13. Risiken und Rückfallstrategie

Risiken:

- Compose-spezifische Darstellung ist erst nach erfolgreichem Android-Build und einer späteren Laufzeitprüfung vollständig bestätigt.
- Der aktuelle Masterzustand besitzt widersprüchliche Release-Metadaten.
- Connector-Commits besitzen nicht die vorgeschriebene SamAI Author-/Committeridentität.

Rückfallstrategie:

- Der gesamte Produktumfang liegt isoliert auf `samai/v2.27.0/notiz-knoten`.
- `master` wurde nicht verändert.
- Ein Verwerfen des Branches entfernt die Funktion vollständig ohne Persistenzmigration.

## 14. Fortschritt

2026-08-07:

- Issue #333 als verbindliche Produktspezifikation verwendet.
- Branch aus Master-SHA `de81277947367822d42d59f849e6bfaca8075373` erstellt.
- Produktcode und Tests umgesetzt.
- Draft-PR #336 erstellt.
- Release-Guard erfolgreich.
- Mathematikkern-Workflow als vom Feature unabhängigen Wrapper-Validierungsfehler klassifiziert.

## 15. Entscheidungsprotokoll

### 2026-08-07: Notiz ist Kartenwerkzeug statt Mathematikknoten

Alternativen: Aufnahme in `MathematikKnoten` oder in den fachneutralen Karteneditor.

Entscheidung: App-seitiger Kartenwerkzeug-Katalog.

Begründung: Die Notiz besitzt keine mathematische Semantik, ist aber eine konkrete Atlas-Produktfunktion. Der fachneutrale Editor soll keine app-spezifischen Typ-Schlüssel kennen.

Konsequenz: `AtlasZustand` kombiniert mathematische und visuelle Vorlagen.

### 2026-08-07: Präsentationsknoten werden generisch aus der Auswertung ausgenommen

Alternative: Dummy-Auswerter für `karte.notiz`.

Entscheidung: `KartenAuswerter` erhält `nichtAuswertbareKnotenArten`.

Begründung: Ein Dummy-Auswerter würde Präsentation fälschlich als mathematische Semantik modellieren und künftige Präsentationsknoten zu weiteren Sonderfällen zwingen.

Konsequenz: Unbekannte normale Knoten bleiben Fehler, explizit deklarierte Präsentationsknoten nicht.

### 2026-08-07: Kein nachträgliches Umschreiben von `v2.26.1`

Alternative: neuen Typ direkt in den vorhandenen Masterstand mit Commitbezeichnung `v2.26.1` einbauen.

Entscheidung: Feature auf dem nächsten y-Release-Branch isolieren.

Begründung: Das Repository verbietet neue Knotentypen in x-Releases und historische Releasecommits werden nicht nachträglich umgeschrieben. Außerdem sind die Versionsmetadaten auf `master` bereits widersprüchlich.

Konsequenz: Branch und Draft-PR verwenden vorläufig `v2.27.0`; finale Releaseintegration bleibt blockiert, bis die Versionsquelle konsolidiert ist.

## 16. Abweichungen vom ursprünglichen Plan

- Der Nutzerauftrag nannte `v2.26.1`. Während der Repositoryprüfung zeigte sich, dass `master` bereits einen Commit mit genau dieser Releasebezeichnung enthält. Gemäß Repositoryvertrag wurde der neue Knotentyp daher nicht in diesen bestehenden Releasecommit geschrieben.
- Die eigentliche Produktfunktion entspricht Issue #333. Die Releaseintegration wurde bewusst getrennt.

## 17. Ergebnis und Verifikation

Aktueller Ergebnisstand:

- `karte.notiz` ist auf dem Featurebranch erzeugbar,
- besitzt keine Anschlüsse,
- hat einen eigenen Renderer,
- wird über den Inspector bearbeitet,
- verwendet die vorhandene Knotengröße,
- wird von der mathematischen Auswertung explizit ignoriert,
- besitzt Tests für Datenvertrag, Fallbacks und JSON-Roundtrip.

Noch ausstehend vor Abschluss:

- erfolgreicher vollständiger Android-Build,
- unabhängige Node-Verifikation,
- konsistente Release-Reservierung und Versionsmetadaten,
- korrekter finaler SamAI-Commit über die lokale Git-Identitätsroutine oder ausdrückliche Behandlung der Connector-Commit-Ausnahme.

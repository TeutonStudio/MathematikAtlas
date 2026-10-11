# Gemeinsamer Formelentwurf und versionierte Vektorstriche

Status: implementiert; Laufzeitabnahme und unabhängige Verifikation im V3.1.0-ExecPlan.

## Kontext

Android und Desktop benötigen dieselbe Formeloberfläche und dauerhafte Stifteingabe, ohne Erkennung vorzutäuschen oder mathematische Interpretation mit Originaldaten zu vermischen. Die vorhandenen vollständigen Ausdrucksbaum-Editoren müssen erhalten bleiben. Konfigurationswechsel können abhängige Anschlussverträge und Verbindungen verändern.

## Entscheidung

`MathematikKnoten/formel` stellt den gemeinsamen Dialograhmen einschließlich Moduswahl, Vorschauplatzierung und Bestätigung bereit. Zahl- und Strukturpaletten verwenden ihre bestehenden fachlichen Editormodelle innerhalb dieses Rahmens. Android-Dialoge sind dünne Adapter; Desktop-Toolbar und Inspector rufen dieselben Oberflächen auf. Das zusammenhängende Falk-Schema liegt ebenfalls im gemeinsamen Modul.

Drei getrennte Eigenschaften speichern Formelbaum (`formel.ast`), Interpretation (`formel.interpretation`) und Originalzeichnung (`formel.zeichnung`). Zeichnungsversion1 verwendet logische Koordinaten1024×512, stabile Strich-IDs, Farbe, Breite und geordnete Punktlisten. Radierungen sind geordnete Vektorgesten; sichtbare Fragmente werden aus der Historie abgeleitet. Ein Radiersegment ist eine vollständige Kapselspur, nicht bloß eine Menge seiner Stützpunkte. Wiederbesuche einer Koordinate bleiben erhalten. Endliche Koordinaten, Mengenbegrenzungen und ein Darstellungsbudget verhindern unkontrollierte Eingaben. Unbekannte Zeichnungsversionen bleiben unverändert erhalten und sind bis zur Unterstützung nicht bearbeitbar.

Eine abgeschlossene Stiftgeste ist ein Undo-Schritt im Dialogentwurf. Abbruch verwirft den Entwurf. Bestätigung übernimmt Formel und Zeichnung gemeinsam über die zentrale reine Konfigurationsplanung samt abhängiger Fixpunktsynchronisation und atomarer Kartenaktion. Diese Planung liegt in MathematikKnoten und wird auf beiden Plattformen verwendet.

Fotoimport und automatische Erkennung sind in Alpha explizit nicht verfügbar. Quellfotos werden nicht gespeichert. Es entstehen keine neuen Produktionsabhängigkeiten.

## Folgen und Nachweis

Bestehender JSON-/`.matlas`-Eigenschaftstransport erhält Zeichnungen ohne neues Graphformat. Regressionen decken sparse Radierkreuzungen, geschlossene Konturen, Gesture-Undo/Redo, unbekannte Versionen, Grenzen sowie JSON-/`.matlas`-Persistenz ab. UI-Abnahme bei schmaler Ansicht und großer Schrift bleibt ein separater Laufzeitnachweis im ExecPlan.

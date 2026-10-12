# 0047 – Einheitliches Compose-Dialogsystem

- Datum: 2026-10-12
- Status: angenommen; Laufzeitabnahme siehe ExecPlan.

## Kontext

Android-App, Desktop und Formeleditor verwendeten Material-AlertDialog und eigene
Dialog/Surface-Kombinationen mit unterschiedlichen Außenabständen und Kopfzeilen.
Titel konkurrierten mit Aktionen um Breite; Unterseiten addierten weitere Paddings.

## Entscheidung

Fachneutrale Compose-Bausteine liegen in `KnotenKartenVerwalter/schnittstelle/AtlasDialog.kt`.
Die bereits vorhandene Desktop-Quelleneinbindung verwendet denselben Code; es entstehen
keine neuen Shadowmodule, Abhängigkeiten oder App-Abhängigkeiten in Bibliotheken.

`AtlasDialogAbstände` ist unabhängig von `LocalAtlasAbstände`: Fensterrand 16 dp,
Kopfrand oben 24 dp, horizontaler Inhaltsrand 24 dp, Kopf unten 16 dp,
Titel/Untertitel 8 dp, Abschnitte 24 dp, Beschreibung 8 dp, Felder 12 dp,
Einträge innen 16/12 dp mit 4 dp Textabstand, Aktionen innen 24/16 dp mit 8 dp Abstand.
Titel verwenden headlineSmall, Abschnitte titleMedium, Beschreibungen bodyMedium.

Kompakte Dialoge sind maximal 560 dp, Arbeitsdialoge maximal 1180 dp breit.
Fenstergrenzen, safeDrawing und IME begrenzen die Oberfläche. Die Hülle besitzt
keinen eigenen Inhaltsscroller. Der Slot-Rahmen und Bestätigungsdialog scrollen
Formulare; Lazy-Listen/Grids und bereits scrollende Formulare melden eigenen
Scrollbesitz ausdrücklich über `inhaltScrolltSelbst` an.

Kopfaktionen stehen immer unter dem Text und umbrechen. Das vermeidet einen zweiten,
breitenabhängigen Kopfvertrag. Kopf und Fuß erhalten jeweils höchstens 40 Prozent
der verfügbaren Fensterhöhe und sind bei Überlänge selbst scrollbar. Inhalte mit
Fußleisten verwenden den verbleibenden Platz über weight, keine unabhängige
Prozenthöhe. Titel werden nicht durch Ellipsen oder feste Zeilenzahlen gekürzt.

Listeneinträge besitzen einen gemeinsamen Innenabstand. Optionale Nebenaktionen
stehen unter dem Eintragstext, damit lange Beschreibungen ihre Breite behalten.

## Alternativen

- Nur Abstandskonstanten: verhindert keine doppelten Ränder oder überlaufende Aktionen.
- Gemeinsames System in app: für Bibliotheken und Desktop nicht erreichbar.
- Neues UI-/KMP-Modul: unnötige Toolchainänderung für diesen Auftrag.

## Folgen

Fachliche Dialogparameter und Zustandsaktionen bleiben bestehen. Native Dateidialoge
bleiben plattformeigen. Kartendaten, mathematische Semantik, Versionen und Persistenz
werden durch diese Änderung nicht verändert. Allgemeine App-Abstände bleiben erhalten.

---
name: dialog-erstellen
description: Dialoge im Mathematik Atlas erstellen, ändern oder prüfen; gemeinsame Compose-Dialogbausteine, zum Inhalt passende Bedienung und nachweisbare visuelle sowie funktionale Konsistenz sicherstellen.
---

# Dialoge passend gestalten

Ein Dialog muss zur bestehenden Dialogfamilie und zur konkreten Aufgabe passen. Sichere dies durch Wiederverwendung und die Abnahme unten ab. Eine Skill-Anweisung allein garantiert kein korrektes Laufzeitverhalten; melde ungeprüfte Eigenschaften ausdrücklich als offen.

## Bestand und Aufgabe klären

- Lies die Projektanweisungen sowie die relevanten Teile von `docs/codex/PROJECT_CONTEXT.md`, `docs/codex/CURRENT_STATE.md` und `docs/codex/ARCHITECTURE.md`. Alle folgenden Repositorypfade sind relativ zum Projektwurzelverzeichnis.
- Verfolge den tatsächlichen Aufrufpfad vom Öffnen bis zur Übernahme, zum Abbruch und zur betroffenen Modellaktion. Untersuche mindestens einen fachlich vergleichbaren bestehenden Dialog und den gemeinsamen Rahmen `KnotenKartenVerwalter/src/main/kotlin/de/TeutonStudio/KnotenKartenVerwalter/schnittstelle/AtlasDialog.kt`. Bestehende Abweichungen sind keine automatische Gestaltungsvorgabe.
- Halte vor der Umsetzung knapp fest: Zweck, betroffene Daten, benötigte Eingaben, Dialogtyp, primäre Aktion, Abbruchverhalten, relevante Leer-/Fehlerzustände und Vergleichsdialog. Kläre nur tatsächlich fehlende fachliche Entscheidungen mit dem Nutzer.

## Gemeinsame Gestaltung verwenden

- Verwende `AtlasBestätigungsDialog` für kompakte Meldungen, Bestätigungen und kurze Formulare; verwende `AtlasDialog` für umfangreiche Arbeitsdialoge. Entscheide nach Inhalt und Interaktion, nicht nach einer festen Anzahl von Feldern.
- Nutze `AtlasDialogKopf`, `AtlasDialogAktionen`, `AtlasDialogAbschnitt`, `AtlasDialogEintrag` beziehungsweise `AtlasDialogListenEintrag`, wo ihre Verantwortung passt. Abstände stammen aus `AtlasDialogAbstände`, Farben, Typografie und Formen aus `MaterialTheme`. Erzeuge keinen parallelen Dialograhmen und keine unabhängige Abstandsskala.
- Übernimm Fenstergrenzen, Safe-Area-/Tastaturbehandlung und Oberflächenstil aus dem gemeinsamen Rahmen. Begründe notwendige fachliche Sonderlayouts; korrigiere einen gemeinsamen Fehler am zuständigen Baustein statt durch lokale Gegenwerte. Ändere dabei keine anderen Dialoge beiläufig.
- Halte Titel und abschließende Aktionen außerhalb des scrollenden Hauptinhalts erreichbar. Lazy-Listen, Grids und Zeichenflächen benötigen begrenzten Platz und einen klaren Scrollbesitzer. Setze bei selbst scrollendem Inhalt `inhaltScrolltSelbst = true`; lege keine zusätzliche gleichgerichtete Scrollhülle darum. Bei zusammengesetzten Speziallayouts müssen verschachtelte Scrollbereiche begrenzt und praktisch geprüft sein.
- Verwende umbrechende Aktionszeilen statt starrer Breiten. Verhindere abgeschnittene Titel, horizontale Überläufe und durch Tastatur oder Inhalt verdeckte Aktionen. Verwende die vorhandenen Größenvarianten statt gerätespezifischer Pixelannahmen.

## Inhalt und Verhalten zusammen entwerfen

- Benenne den Titel nach der Aufgabe und die primäre Aktion nach ihrer Wirkung, etwa „Exportieren“ oder „Formel übernehmen“. Verwende „Abbrechen“ für verworfene Entwürfe und „Schließen“ für reine Ansichten. Beschreibe Folgen irreversibler Aktionen konkret und benenne das betroffene Objekt.
- Ordne Inhalte nach fachlicher Abhängigkeit: Zweck beziehungsweise nötiger Kontext, Modus/Auswahl, abhängige Eingaben, optionale Details, Ergebnis oder Vorschau und Abschlussaktion. Gruppiere zusammengehörige Felder; halte verbleibende Elemente bei Zustandswechseln möglichst stabil.
- Wähle Bedienelemente passend zur Bedeutung: eindeutige Auswahl, mehrere unabhängige Optionen, Werteingabe oder reine Information. Zeige nur fachlich relevante Einstellungen samt zugehörigen Beschriftungen und Hilfen. Relevante vorübergehend nicht ausführbare Aktionen dürfen mit verständlichem Grund deaktiviert bleiben.
- Nutze die Projektbegriffe und zeige Einheiten, Wertebereiche und notwendige Bedingungen am zugehörigen Feld. Fehlende Eingaben, leere Treffer, ungültige Konfigurationen und mathematisch offene Ergebnisse benötigen unterschiedliche verständliche Rückmeldungen. Interne Implementierungsdetails gehören nur in ausdrücklich technische Ansichten.
- Zeige Formeln über den vorhandenen nativen Formel-/LaTeX-Pfad und leite Ergebnisse aus dem fachlichen Modell ab. Prüfe lange Formeln und nicht unterstützte Notation; erfinde keine Mathematik im Dialog.
- Validiere Entwürfe vor der Übernahme; verhindere ungültige Bestätigung und stille Ersatzwerte. Lokaler Entwurfszustand ist zulässig, aber keine zweite persistierte Wahrheit. Übernimm Änderungen über die vorgesehenen Aktionen, bei einer logischen Kartenänderung atomar mit einem Undo-Schritt. Berücksichtige Laden/Speichern, Anschlüsse und bestehende Verbindungen, sofern betroffen.
- Lege das Verhalten von Abbruch, Zurück/Escape und Außenklick bewusst fest und führe diese Wege durch denselben passenden Abschluss-/Abbruchpfad. Verwerfe relevante ungespeicherte Arbeit nicht versehentlich; verwende vorhandene Schutzmechanismen, wenn der Ablauf sie benötigt. Eine sofort wirksame Einstellung darf nicht den Eindruck eines erst später übernommenen Entwurfs erwecken.
- Stelle verständliche Labels, zugängliche Symbolbeschreibungen, erkennbare Auswahl-/Fehlerzustände und logische Fokusreihenfolge sicher. Fehler dürfen nicht ausschließlich durch Farbe erkennbar sein. Prüfe Tastaturbedienung für Desktop und Eingabefelder sowie ausreichend große Touchziele für Android.

## Konkrete Referenzpfade

Prüfe den aktuellen Code; diese Beispiele dienen der Orientierung und sind keine unveränderlichen Kopiervorlagen:

- Kurze Auswahl mit eindeutiger Abschlussaktion: `app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KartenExportFormatDialog.kt`.
- Arbeitsdialog mit Vorschau und atomarer Entwurfsübernahme: `MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/formel/FormelEditorDialogRahmen.kt`.
- Bestehende Android-Vorschauen: `app/src/debug/kotlin/de/TeutonStudio/MathematikAtlas/schnittstellen/`.
- Echte Compose-Layout- und Außenklickprüfungen: `desktopApp/src/test/kotlin/de/TeutonStudio/MathematikAtlas/desktop/AtlasDialogLayoutTest.kt`.

## Verbindliche Abnahme

- Vergleiche den betroffenen Dialog mit dem gewählten Referenzdialog: Oberfläche, Titelhierarchie, Abstände, Felder, Aktionsanordnung und Sprache müssen dieselbe Gestaltung erkennen lassen; fachliche Abweichungen müssen begründet sein.
- Prüfe den tatsächlichen Inhalt: Normalfall, längster realistischer Text beziehungsweise Formel, viele Einträge, leerer Zustand, ungültiger Entwurf sowie modusabhängige Felder, soweit vorhanden. Prüfe gültige Übernahme, Abbruch ohne unbeabsichtigte Änderung, erneutes Öffnen und betroffene Undo-/Redo-Pfade.
- Prüfe Darstellung und Bedienung bei schmalem Hochformat, niedriger Querformat-/Fensterhöhe, normaler und großer Schrift (bis 2-fach), hellem und dunklem Theme und bei eingeblendeter Tastatur. Bei plattformübergreifendem Code sind Android- und Desktoppfad relevant. Titel, Inhalt und Aktionen müssen lesbar beziehungsweise durch Scrollen erreichbar bleiben; Scrollen des Inhalts darf den Abschluss nicht verdrängen.
- Leite passende Prüfungen aus `docs/codex/TEST_STRATEGY.md`, Gradle und CI ab. Nutze vorhandene Vorschauen und Compose-/Laufzeitprüfungen. Ergänze gezielte Verhaltenstests bei neuer Validierung oder Interaktion, keine Tests, die lediglich Abstandskonstanten oder Quelltextformulierungen festschreiben.
- Behebe nachgewiesene Abweichungen im Aufgabenumfang und wiederhole die betroffenen Prüfungen. Sind Laufzeitprüfungen nicht ausführbar, benenne den konkreten Grund und die offenen Fälle. Ein Build oder eine Vorschau allein ist kein Beleg für funktionierende Interaktion und keine vollständige Abnahme.
- Kontrolliere den Abschlussdiff. Berichte knapp den gewählten Dialogtyp und Vergleichspfad, das erreichte Verhalten, tatsächlich ausgeführte Prüfungen und offene Prüfgrenzen. Behaupte Konsistenz und Bedienbarkeit nur im nachgewiesenen Umfang.

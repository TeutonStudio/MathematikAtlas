# Gemeinsame Visualisierungsansicht und begrenzte Samplingkoordination

- Datum: 15.10.2026
- Status: angenommen; 2D-Gesamtframezeitziel weiterhin offen
- Bezug: [ExecPlan](../plans/active/2026-10-15-visualisierung-ueberarbeiten.md)

## Entscheidung

Der bestehende Visualisierungsknoten verwendet auf Android und Desktop dieselbe native Compose-Zeichnung. Die Vorschau konsumiert keine Graphgesten. Der gemeinsame Arbeitsdialog bearbeitet einen lokalen Entwurf und übernimmt eigene Eigenschaften atomar; unbekannte Schemafelder bleiben erhalten, dynamische Schnittbelegungen werden vollständig ersetzt.

Eine reine Zahlengeometrie beschreibt Welt-/Bildschirmtransformation, Pan, Zoom und Sichtbereich. R²/ℂ verwenden gleiche Einheitenskalierung. Persistierte Kameraangaben ohne ansichtVersion=2 werden beim Lesen der Konfiguration in das zentrierte Modell überführt; bei R³ werden die früher mitrotierten Translationen berücksichtigt. Der Karten- und Anschlussvertrag bleibt unverändert.

Mathematische Parameterdomänen und räumliche Samplingfenster sind getrennt. Räumliche Regionen und Funktionsgraphen dürfen sichtbereichsabhängig nachladen; parametrische Bilder behalten ihren Definitionsbereich. Die konservative räumliche R³-Abdeckung umfasst eine Kugel um das Szenenzentrum einschließlich Pan/Zoomreserve und bleibt rotationsunabhängig. Das kann größere Ausschnitte und gröbere Ergebnisse bei gleichem Budget verursachen; unbekannte Zellen werden weiter ausdrücklich markiert.

Die gemeinsame Berechnung begrenzt parallele Arbeit auf zwei Aufträge, priorisiert wartende Dialoganfragen, teilt identische Arbeit und bricht sie nach Abmeldung des letzten Verbrauchers ab. Der LRU hält höchstens 32 MiB geschätzte Ergebnisgeometrie; dies ist kein Limit des gesamten Prozessheaps. Sampling-, Fortschritts- und Zeichendaten bleiben Laufzeitzustand. Cachetreffer sind an den vollständigen mathematischen Sampling-Schlüssel gebunden; reine Stiländerungen ändern die Geometrie nicht.

## Alternativen

Ein separater Webrenderer oder plattformspezifische Ersatzimplementierungen würden neue Abhängigkeiten und unterschiedliche Semantik einführen. Vollständige Neuberechnung je Pointerereignis würde fachliche Arbeit an die Bildrate koppeln. Deshalb werden während einer Geste vorhandene Detailgeometrien mit unverändertem Zeichenstil transformiert; Nachberechnung erfolgt nach der Geste mit 120 ms Ruhezeit.

## Konsequenzen und Nachweise

Der neutrale Karteneditor bleibt mathematikfrei. Die bestehende Desktop-Quellfreigabe genügt; nur der Ausschluss des Android-gebundenen alten Renderers entfällt. Beweis-, numerische und unbekannte Zustände bleiben fachlich getrennt. Regressionen prüfen Transformation, alte Kameras, Stützstellenbudget, Domänenerhalt, Konflikte, Abbruch, Kartencodec und Duplizieren.

Die Gerätemessung verbessert Vorschauzeit und 3D-Gesten deutlich. Das Ziel p95 ≤16,7 ms für die gesamte 2D-Framezeit ist nicht erreicht und wird nicht als abgenommen bezeichnet. Rohdaten und Reproduktionsbefehl stehen im ExecPlan.

## Ergänzung: Mengenebenen und Graphdarstellung

Der Visualisierer ist bei Neuerzeugung eine Senke mit zwei optionalen erweiterbaren Eingängen. Die explizite Umstellung historischer Knoten verwendet die bestehende Verlustvorschau; Lesen löscht keine Ausgangsverbindung. Ebenenstile werden über Anschlussnamen gespeichert, nicht über kopierabhängige Port-IDs oder Reihenfolge. Kamera und mathematische Projektion bleiben gemeinsam; inkompatible Ebenen werden diagnostiziert.

Sampling und Stil bleiben getrennt: Vollfläche, Schraffur, Kreuzschraffur und Hexagonmuster verwenden dieselbe Geometrie. Rasterproben sind ausdrücklich markiert; fachlich diskrete Punkte werden bei Flächenvereinigungen separat materialisiert. Zertifizierte Zellen werden zuerst in Breitensuche klassifiziert, damit Randverfeinerung nicht früh andere grobe Regionen verdrängt. Polynomiale Ungleichungen erhalten nur bei abgetastetem Vorzeichenwechsel numerische Randzüge; strikte Grenzen werden gestrichelt gezeichnet. Berührende Nullniveaus ohne Vorzeichenwechsel werden nicht als sichere Gebietsgrenzen ausgegeben. Gemischte und unbekannte Zellen bleiben gesondert schraffiert.

Alle 3D-Dreiecke erhalten eine gemeinsame Tiefenreihenfolge über die Ebenen. Ein Laufzeitcache merkt sich Geometrie und Kamera; Farben werden erst beim Zeichnen zugeordnet. Automatisch/Standard bleiben auf 8000, Hoch auf 64000 Auswertungen begrenzt; sichtbare Ebenen teilen das Gesamtbudget. Vorschau und Dialog verfeinern progressiv.

Graphmengen verwenden den kanonischen Methodenvertrag. Ein einzelnes strukturiertes Argument wird für das Sampling als Struktur gepackt; es wird nicht mit mehreren öffentlichen Argumenten verwechselt. Termmodus und Definitionsübersetzer verwenden den vorhandenen Term-zu-Methode-Pfad samt Quellenbindung.

Die Hauptkörperkontur von Adapterknoten besitzt eine gemeinsame Laufzeit-Fußzeilenhöhe. Diese Zahl wird vom Editor aus Typografie/Schriftskalierung abgeleitet und vom Karten-Codec nicht geschrieben. Anschlusspositionen, Verbindungen und Treffertests verwenden dieselbe Kontur und gleichmäßige Bogenlängen.

## Ergänzung 16.10.2026: Gefilterte Regionen und Arbeitsgrenzen

Ein Filter über einem unterstützten Koordinatenraum wird über die kanonische Methoden-Anwendung als definierte Region normalisiert. Ein strukturiertes Tupel bleibt ein öffentliches Argument; Komponenten werden erst innerhalb dieses Arguments gepackt. Gemeinsame Definitionsräume, abhängige Grundmengen und Voraussetzungen werden strukturell gebunden. Externe freie Parameter werden identitätsbasiert erkannt und ausdrücklich als bedingt darstellbar diagnostiziert, statt zufällig gleichnamige Achsen zu binden. Nur bekannte reelle Koordinaten erlauben die Vereinfachung ihrer Reell-Mitgliedschaft; partielle Zahlterme behalten die Mitgliedschaftsbedingung.

Bewiesene Zellen bleiben maßgeblich. Unbekannte Blätter kontinuierlicher zweidimensionaler Regionen können lokal durch numerische Eckproben ergänzt werden; deren Füllung bleibt eine Näherung und keine Zertifizierung. Unbekannte Proben bleiben unbekannt. Gemischte Zellen dichter/disjunkter Mengen werden nicht in volle Flächen umgewandelt. Tatsächlich diskrete Punkte bleiben sichtbar. Numerische Ergänzung ersetzt das einzelne unbekannte Blatt und überlagert keine schon nutzbare Fläche mit einer globalen Schraffur.

Zellbesuche samt Intervallarbeit, Punktproben einschließlich Farbdimension und Randberechnungen teilen das Arbeitsbudget. Ein nicht unterstützter Zellnachweis wird nicht erneut über das ganze Fenster unterteilt. Die Plotdiagnose trennt sichtbaren Weltbereich, Berechnungsbereich mit 20 % gesamter Breiten-/Höhenreserve und Parameterfenster; Navigation verändert keine fachliche Definitionsmenge. Alle neuen Angaben sind Laufzeitzustand. Kartenformat und Anschlüsse ändern sich nicht.

## Ergänzung 16.10.2026: Detailgeometrie während Navigation

Gesten verwenden dieselben fertig vorbereiteten Zeichenpakete wie die ruhende Ansicht. Ein Fingerkontakt, Pan, Pinch, Mausrad oder Tastaturwechsel löst keinen Wechsel auf Grobgeometrie und keine Ausblendung von Dreieckkanten aus. Die Koordinateninspektion greift auf dieselben vorbereiteten Daten zu; die 3D-Tiefensortierung bleibt ebenenübergreifend.

Die Anfrageverwaltung unterscheidet fachliche Inhaltsidentität von Kamera und Detailanforderung. Eingang, Raum, Achsen, Parameterfenster, Projektion, feste Parameter, fachliche Toleranz, Prädikatsfenster und Orbitgrenzen invalidieren die Wiederverwendung. Kamera, gemessene Fenstergröße, reine Stile und Qualitätsanforderung dürfen ein passendes Ergebnis behalten. Eine grobe Vorschau wird nur ohne vorhandenes passendes Ergebnis berechnet. Navigation wartet 120 ms Ruhe und fordert unmittelbar Detail an; kooperative Abbrüche und zusätzliche Aktivitätsprüfung verhindern verspätete Veröffentlichungen.

Jede Ebene behält ihr unveränderliches veröffentlichtes Zeichenpaket, bis dessen Ersatz vollständig im Hintergrund vorbereitet ist. Änderungen der fachlichen Identität setzen das Paket synchron zurück. Dies ersetzt die früheren separaten Grob-/Detail-Zeichenlisten und verhindert leere Zwischenbilder bei bloßer Kameraänderung. Neu sichtbare, noch nicht berechnete Gebiete bleiben ungefüllt. Die Arbeitsgrenzen, zwei parallele Aufträge und der 32-MiB-LRU bleiben bestehen: Darstellungstreue während Navigation macht Rechen- oder Speichergrenzen nicht überflüssig.

Native Desktop-Bildregressionen vergleichen den unveränderten Ausschnitt während einer offenen Geste in R² mit mehreren Ebenen und R³. Beide schlagen gegen den Ausgangsrenderer fehl und bestehen mit der neuen Implementierung. Android-Gegenstücke sind im Test-APK enthalten; mangels Gerät heute nicht ausgeführt. Keine neue Aussage über Framezeiten oder das weiterhin offene 60-Hz-Ziel.

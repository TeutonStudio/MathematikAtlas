# Automatische Visualisierungsverfeinerung

Status: implementiert, Leistungsabnahme teilweise offen; 17.10.2026.

## Entscheidung und Nutzerwirkung
Der automatische UI-Pfad besitzt kein Gesamtlimit an Samplingauswertungen. Die reguläre Budgeteingabe entfällt. Bestehende Qualitätswahl, Orbitlänge und gespeicherte Budgetfelder bleiben erhalten. Numerische Approximation, unbekannte Mitgliedschaft und technische Begrenzung bleiben getrennt.

## Bestehende Architektur fortsetzen
VisualisierungsBerechnung teilt Sitzungen und Ergebnisse auf Android/Desktop. Die vorhandenen synchronen Sampler und Kerncallbacks bleiben kompatibel: ihr Kotlin-Aufrufstack wird auf begrenzt zugelassenen IO-Workern erhalten. Eine faire Warteschlange gibt nach etwa 8 ms an kooperativen Kontrollstellen den Rechenplatz frei; maximal zwei rechnen gleichzeitig. Maximal acht Hintergrundsitzungen und zwei zusätzliche Dialogsitzungen dürfen Worker belegen. Weitere Aufträge warten suspendiert; Cachetreffer und zusätzliche Verbraucher derselben Arbeit benötigen keinen Platz. Einzelne atomare Kernoperationen sind nicht präemptierbar; dies ist kein harter Echtzeitvertrag.

Zwischenstände werden grundsätzlich höchstens alle 50 ms, erster Aufbau/Abschluss sofort veröffentlicht und zum Verbraucher zusammengefasst. Nicht mehr benötigte Aufträge werden abgebrochen. Symbolische Normalisierung und Nachweisarbeit besitzen weiterhin eigene konservative Schutzgrenzen; diese sind keine Lebenszeit- oder Samplinggrenze der Ansicht.

## Speicher und Wiederverwendung
Ein gemeinsamer 32 MiB-LRU enthält Geometrie und mathematisch passende Funktionsproben. Aktive Sampler, Snapshotkopien und vorbereitete Zeichenpakete reservieren zusammen geschätzte 64 MiB vor Wachstum. Die Zählung ist ein konservatives Modell verwalteter Geometrie, keine exakte Prozess-/Heap-RSS-Garantie; Threadstacks und Kern-Zwischenobjekte sind nicht vollständig darin enthalten.

Diskrete Kombinationen werden mit fortsetzbaren Schleifen/gemischten Radixindizes statt vollständigen Kreuzproduktlisten erzeugt. Bei einer tatsächlichen Speichergrenze bleibt der letzte veröffentlichte Teilstand mit Ursache erhalten. Neue Definitionslücken seit diesem Snapshot führen zum konservativen Verwerfen veralteter Flächen; ein alter grober Patch darf keine inzwischen bekannte Lücke verdecken.

## Zeichnung und Bedienung
2D-Zell-/Segmentpfade werden außerhalb des UI-Threads relativ zu einem lokalen Ursprung vorbereitet und bei Navigation nativ transformiert. Bereits abgedeckte ausreichend genaue Geometrie bleibt erhalten. Eine kurzlebige Samplingkamera lädt während langer Touchgesten etwa alle 160 ms Ränder nach; die vorhandene Kameraübernahme erfolgt weiter am Gestenende. Interaktion verwendet ein 3 Pixel-Ziel, danach Standard/Automatisch 1 Pixel bzw. Hoch 0,5 Pixel mit 3D-Zoomkompensation.

Adaptive und reguläre zweiparametrige Flächen liefern zusätzliche zusammenhängende Interaktionsdreiecke (höchstens 1024 je einzelnem unterstütztem Flächennetz; Vereinigungen können mehr enthalten). Bekannte ungültige Proben deaktivieren diesen Ersatz. Vereinigungen erhalten pro Teilfläche entweder deren grobes Netz oder deren vollständige Geometrie. Transparente Flächen werden weiterhin gemeinsam über alle Ebenen tiefensortiert.

## Kompatibilität und Diagnose
Kartenformat, Knotentypen und Anschluss-IDs ändern sich nicht. Laufzeitflags, Cache und Interaktionsnetze werden nicht persistiert. VisualisierungsDetailansicht besitzt eine optionale Zeichenzeitdiagnose: Callback (Nanosekunden, gezeichnete Dreiecke), ohne Einfluss auf fachliche Ergebnisse. Gemessene Canvas-CPU-Zeit ist keine vollständige Display-/Present-Framezeit.

## Verifikation
Gezielte Regressionen und native Desktopinteraktionen werden im ExecPlan belegt. Nach Entsperrung des Referenzgeräts bestehen 19 Android-Bild-/Interaktionstests und drei instrumentierte Leistungsläufe. Die 3D-FrameMetrics-p95 sinkt von 499–509 ms auf 68–69 ms; das 16,7-ms-Ziel bleibt verfehlt. Vollständige 3D-Verfeinerung dauert länger als im budgetierten Ausgangsstand; die erste Vorschau und separate Present-Latenz wurden nicht gemessen. Es wird keine pauschale 60-Hz-Garantie behauptet. Details im aktiven Plan und den Plattformmessberichten.

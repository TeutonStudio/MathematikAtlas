---
name: ui-entwickeln
description: Verbessert Jetpack-Compose-Oberflächen, Inspektoren, Dialoge und Formelrenderer des Mathematik Atlas. Verwenden bei Layout, Padding, Material-3-Verhalten, State und Maus-/Touch-/Stiftbedienung.
---
# Compose-UI entwickeln

Lies `AGENTS.md`; suche vergleichbare echte Compose-Komponenten. Kein React, CSS, KaTeX oder Web-UI erfinden.

## Vorgehen
- Reproduziere den sichtbaren Fehler in konkreten Fenstergrößen, Skalierungen und Eingabemodi.
- Prüfe Material-3-Abstände, minimum touch target, Kontrast, Textumbruch, Inhalts- und Außenpadding, Scrollbarkeit und Accessibility.
- Unterscheide lokalen UI-Zustand von kanonischem `KnotenDaten`-Zustand. Persistente Inspectoränderungen ausschließlich über Kartenaktionen.
- Knotenrenderer, Inspector, Dialog und nativen LaTeX-Renderer nicht unabhängig voneinander neu interpretieren.
- Bei dynamischen Elementen stabile Schlüssel, Recomposition und State-Lebensdauer beachten.
- Nur begründete kleine Layoutkorrekturen ohne umfangreichen ExecPlan; bei Vertrags- und Persistenzänderung passende Spezialskills ergänzen.

## Prüfen
Screenshot-/Layoutbetrachtung in schmalen und breiten Ansichten; Tastatur-/Maus-/Touch-/Stiftpfade nach Relevanz; Dialogöffnung/-schließung, Zoom, Textvergrößerung. Gezielte Tests und Android-Lint/Debug-Build. Verhalten auf Emulator/Gerät nur bestätigen, wenn tatsächlich geprüft.

# Linux-Desktop v2.28.8

Das Desktopziel verwendet denselben Rechenkern, dieselben Knotenvorlagen, denselben Editor und dasselbe Karten-JSON wie die Android-App. Es benötigt JDK 17 zum Entwickeln; das native Paket bringt seine Laufzeit mit.

## Entwickeln und paketieren

```bash
./gradlew :desktopApp:run
./gradlew :desktopApp:test
./gradlew :desktopApp:packageRpm
```

Das RPM liegt anschließend unter `desktopApp/build/compose/binaries/main/rpm`. Karten werden in `$XDG_DATA_HOME/MathematikAtlas` oder, wenn die Variable fehlt, in `~/.local/share/MathematikAtlas` versioniert und atomar gespeichert.

## Eingabevertrag

| Aktion | Maus/Touchpad | Tastatur |
| --- | --- | --- |
| Knoten auswählen | Primärklick | Tab/F6 zum Graphen, Pfeiltasten zur Bewegung |
| Auswahl erweitern/umschalten | Shift-/Ctrl-Klick | Ctrl+A |
| Bereich auswählen | Primärdrag auf Hintergrund | – |
| Ansicht verschieben | Mittlere Taste oder Leertaste+Drag; Shift+Rad horizontal | – |
| Zoomen | Rad am Zeiger | `+`, `-`, `0` |
| Kontextaktionen | Sekundärklick | Shift+F10 |
| Rückgängig/Wiederholen | Menü/Toolbar | Ctrl+Z, Ctrl+Shift+Z oder Ctrl+Y |
| Kopieren/Ausschneiden/Einfügen | Menü/Kontextmenü | Ctrl+C, Ctrl+X, Ctrl+V |
| Löschen/Duplizieren | Kontextmenü | Entf, Ctrl+D |
| Inhalt/Auswahl zentrieren | Toolbar | Home, F |
| Knoten suchen/einfügen | Katalog | Ctrl+F, N |
| Interaktion abbrechen | – | Esc |

Textfelder und Dialoge haben Vorrang vor Graphkürzeln. Wiederholte Pfeiltastenbewegungen bilden einen gemeinsamen Undo-Schritt. Einfügen übernimmt nur vollständig interne Verbindungen und vergibt neue Knoten-, Anschluss- und Gruppen-IDs.

## Abnahmematrix

Vor einem Master-Merge werden folgende manuellen Prüfungen protokolliert:

- Fedora Wayland: Maus, Touchpad und deutsche Tastatur;
- X11/XWayland: Start, Menü, Kontextmenü, Pan und Zoom;
- RPM: Installieren, Starten, Speichern, Neustarten, Persistenz prüfen, Deinstallieren;
- Android: Touch, physische Tastatur sowie Maus/Stift, sofern verfügbar;
- Konzeptbibliothek: kurzer Klick, Halten, Halten+Drag und Scrollabbruch je Eingabegerät, ohne Doppelaktion.

Die CI baut das RPM und führt Installations-, Start-, Neustartpersistenz- und Deinstallationsprüfungen in einer isolierten X11-Sitzung aus. Wayland-, Touchpad- und reale Android-Geräteprüfungen bleiben eine manuelle Releasefreigabe.

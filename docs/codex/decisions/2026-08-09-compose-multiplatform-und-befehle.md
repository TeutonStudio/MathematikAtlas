# ADR: Gemeinsame JVM-Compose-Schichten und zentraler Befehlsvertrag

- Status: angenommen für v2.28.8
- Datum: 2026-08-09

## Kontext

Android-Touchaktionen, Desktopmenüs, Mausgesten und Tastenkürzel dürfen Graphoperationen nicht mehrfach oder mit unterschiedlichen Regeln implementieren. Gleichzeitig soll die bestehende Compose-Oberfläche auf Linux nutzbar werden, ohne Rechenkern oder Kartenmodell zu duplizieren.

## Entscheidung

Der Atlas führt einen plattformneutralen `AtlasBefehl` samt `BefehlsKontext` und zentralem Ausführer im fachneutralen Karteneditor ein. Android- und Desktopereignisse übersetzen ausschließlich in diese Befehle.

Die JVM-fähigen Compose- und Adaptermodule werden als Kotlin-Multiplatform-Module mit Android- und Desktop-JVM-Ziel veröffentlicht. Gemeinsame, JVM-spezifische Quellen werden einmal kompiliert; ausschließlich Android-gebundene Renderer liegen in `androidMain`. Das neue `desktopApp` koordiniert Fenster, Linux-Speicher, Systemdialoge und Paketierung.

## Alternativen

- Eine separate Desktopkopie des Editors wurde verworfen, weil sie Graph- und Eingabelogik dupliziert.
- Direkte Shortcutlogik in Composables wurde verworfen, weil Toolbar, Menü und Kontextaktionen sonst abweichende Aktivierungsregeln entwickeln.
- Vollständige Umstellung der Android-App auf einen einzigen gemeinsamen Einstieg wurde für diesen Release verworfen; Android-Dateidialoge und -Speicher bleiben plattformspezifisch.

## Konsequenzen

- Befehle und Verfügbarkeit sind ohne Compose testbar.
- Persistierte Kartendaten enthalten keine Plattformereignisse.
- Beide Plattformvarianten müssen im Release gebaut und getestet werden.
- Neue Plattformdienste werden über kleine Schnittstellen angebunden; der Rechenkern bleibt unverändert.

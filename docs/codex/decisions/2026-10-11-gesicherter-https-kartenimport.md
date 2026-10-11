# Gesicherter HTTPS-Kartenimport

Status: implementiert, Laufzeitabnahme offen. Datum: 2026-10-11.

Der Import muss private und lokale Ziele auch nach DNS-Wechseln und Weiterleitungen abweisen. HttpsURLConnection erlaubt keine kontrollierte DNS-Auflösung für den tatsächlich benutzten Socket. Eine IP-URL mit manuell gesetztem Host/SNI scheiterte zudem an der JDK-Hostheader- und Zertifikatsprüfung.

Deshalb verwendet der Kartenabruf OkHttp 4.12.0 als notwendige zusätzliche Produktionsabhängigkeit. Sein DNS-Adapter prüft sämtliche zurückgegebenen Adressen vor dem Verbindungsaufbau; URL, Hostheader, SNI und Zertifikatsprüfung behalten den Originalhost. Numerische URLs werden ausdrücklich geprüft. Systemproxies und automatische Weiterleitungen sind deaktiviert. Jede der höchstens vier Weiterleitungen durchläuft die URL-/Zielprüfung erneut. Größenlimit: 16 MiB; Verbindungs-/Lesetimeout jeweils zehn Sekunden; zusätzlicher Gesamtzeitrahmen sechzig Sekunden.

Regressionen prüfen verbotene Adressen, gemischte DNS-Antworten und wechselnde Antworten. Netzwerk-/TLS-Integration und Android-Laufzeitprüfung ergänzen die statische unabhängige Prüfung.

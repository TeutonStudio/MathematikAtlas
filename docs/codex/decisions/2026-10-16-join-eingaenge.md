# Join-Eingänge für geschlossene kommutative Operationen

Datum: 2026-10-16. Status: angenommen.

## Entscheidung

Ein neutraler Mehrfacheingang ersetzt die einzelnen Operandenanschlüsse geeigneter vorhandener Operatoren. Die mathematische Eignung stammt aus dem Rechenkern; der Editor kennt nur `AnschlussBelegung`, Beitragsreihenfolge und eine injizierte Darstellungsentscheidung. Matrixprodukt und Tensorprodukt bleiben geordnet. Multiplikation und komponentenweise Produkte erhalten die Kapseldarstellung ausschließlich bei nachgewiesenem kommutativem Zahlenbereich; andernfalls zeigt die Projektion geordnete Einzelenden. Der persistierte Graph bleibt dabei stabil.

Jede Verbindung ist ein eigener Beitrag, auch bei identischer Quelle. `eingangsReihenfolge` und die Positionen lokaler Werte bilden eine gemeinsame stabile Folge. `nächsteBeitragsReihenfolge` reserviert den nächsten Platz nach sämtlichen Beiträgen. Eine Quelle bedeutet nach fachlicher Validierung Identität einschließlich Bedingungen und Herkunft; null Beiträge sind unvollständig. Historische leere Operandenplätze und implizite Wahrheitswerte entfallen beim Umstellen.

## Architektur und Persistenz

Format 10 ergänzt Belegung und Verbindungsreihenfolge. Fehlende Belegung bedeutet im neutralen Codec Einzelbelegung. Die idempotente mathematische Codec-Pipeline migriert geeignete Operatoren, übernimmt explizite lokale Werte und erhält Verbindungs-IDs. Kartenfallbacks bleiben eigene Rollen; eine Verbindung trägt die stabile Kennung des von ihr übersteuerten Fallbacks. Kopien benötigen deshalb keine versteckten Verbindungs-ID-Verweise in mathematischen Parametern.

Der Adapter sammelt sämtliche Werte mit eindeutigen Beitragskennungen. Der Auswertungskontext stellt vollständige Listen bereit und entfaltet sie am Registerrand zu den bestehenden Fachauswerterverträgen. Einzelwertauswerter behalten ihre Schnittstelle. Die gemeinsame Mindestgeometrie gilt vor Gruppenprojektion und Einfügelayout; virtuelle Andockpunkte werden aus Verbindungs-IDs abgeleitet und nicht gespeichert.

Operatorwechsel werden graphbewusst geplant. Join zu Join erhält alle Beiträge. Beim Wechsel zu festen Eingängen werden Beiträge geordnet übertragen; überzählige Verbindungen und lokale Werte werden vor Übernahme im bestehenden Konfliktweg angezeigt. Der komplette Wechsel bleibt ein Undo-Schritt. Eingebettete historische Methodenquellen behalten ihre öffentliche Signatur; ihre interne Karte wird erst nach Aufbau der Schnittstelle normalisiert.

## Alternativen

Nur visuelles Zusammenfassen bisheriger binärer Ports erfüllt keine echte Mehrfachbelegung. Ungeordnete Mengen würden wiederholte Quellen verlieren. Immer ungeordnete Multiplikation wäre für Quaternionen falsch. Automatisches Entfernen zusätzlicher Beiträge beim Operatorwechsel würde Nutzerdaten verlieren.

## Konsequenzen

Gemeinsame Implementierung für Android und Desktop, keine neuen Knotentypen und keine neue Abhängigkeit. Das Speichern neuer Karten verwendet Format 10; ein Rückexport in Format 9 wird nicht angeboten. Historische Dateien werden beim Lesen nicht überschrieben. Android-Gestenabnahme bleibt bei fehlendem Gerät getrennt von erfolgreicher Kompilierung dokumentiert.

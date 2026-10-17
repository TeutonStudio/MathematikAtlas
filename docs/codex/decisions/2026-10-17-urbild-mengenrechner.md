# Urbild als Mengenrechneroperation

Datum: 17.10.2026. Status: umgesetzt und abgenommen.

Der vorhandene Operator `menge.urbild` wird im bestehenden Mengenrechner aktiviert. Es entsteht kein neuer Knotentyp. Einzelziel y und Zielmenge B sind zwei persistierte Eingabeformen derselben Operation, semantisch f⁻¹[{y}] beziehungsweise f⁻¹[B]. Nichtinjektive Methoden liefern alle Argumente gemeinsam.

Der Zieleingang folgt der neutralen Ergebnissignatur, der Mengenausgang enthält Elemente aus dem effektiven mathematischen Definitionsraum. Nur die äußere einzelne Ergebniskomponente wird projiziert; enthaltene Tupel bleiben strukturiert. Die Kernfunktion arbeitet exakt durch vollständige endliche Prüfung oder den vorhandenen Prädikatslöser. Unentscheidbare Fälle bleiben als strukturierte Mengendefinition erhalten; es gibt keine Näherungssuche.

Symbolische strukturierte Restmengen verwenden `PrädikatsMenge` und `StrukturellerMethodenAufruf`. Dessen Voraussetzungen werden bei Mitgliedschaft vor der vereinfachenden Substitution geprüft, damit auch konstante Methoden keine offenen Bedingungen verlieren. Der vorhandene aktuelle Wertcodec erhält einen Tag für PrädikatsMenge; seine Versionsprüfung bleibt unverändert.

Plattformen teilen Konfiguration und Typableitung. Explizite Modus- und abhängige Typwechsel durchlaufen die vorhandene Verlustvorschau und bilden eine atomare Kartenaktion. Implizite Synchronisierung erhält inkompatible Quellen mit Diagnose, bis eine bestätigbare Neukonfiguration erfolgt. Kompatible lokale Werte bleiben bei Typverfeinerung erhalten.

Historische Urbild-Platzhalter besitzen den Mengeneingang a. Dieser wird einschließlich ID, Standardwerten und Kartenfallbacks in ziel umbenannt und im Zielmengenmodus ergänzt. Mehrdeutige Altdaten bleiben mit Diagnose erhalten. Das Kartenformat bleibt unverändert.

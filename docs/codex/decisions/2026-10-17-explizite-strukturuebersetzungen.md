# Explizite Strukturübersetzungen im vorhandenen Übersetzer

Datum: 2026-10-17. Status: M3 in Umsetzung, v3.4.6/x reserviert.

## Entscheidung

Tupel zu Zeile/Spalte, Zeile/Spalte zu Tupel und Abflachen werden als Ziele
des vorhandenen atlas.uebersetzer erzeugt. ÜbersetzungsDienst berechnet und
bezeugt diese Strukturänderungen; der Knotenauswerter überträgt Bedingungen,
Bindungsidentitäten, Räume, Formelsnapshot und Übersetzungsherkunft.
Die bestehenden Ziel- und Wire-Verträge bleiben erhalten, Kartenformat 10 bleibt.

Abflachen hat zwei ausdrücklich gespeicherte Verträge: fehlende Tiefe oder
rekursiv öffnet rekursiv ausschließlich Tupel; 1 öffnet eine direkte Ebene
von Tupeln und orientierten Vektoren. Unbekannte Tiefen werden diagnostiziert.
Leere Teilstrukturen, heterogene Blätter und Reihenfolge bleiben rückführbar
über den vorhandenen Strukturplan. Informationserhaltung mit diesem Kontext
behauptet keine Injektivität; Abflachen bleibt aus automatischen Ketten ausgeschlossen.

## Migration und Erzeugung

Alte Typen werden nur bei eindeutigen Rollen, Arten, Kardinalitäten und
Identitäten umgesetzt. Richtung unterscheidet die gleichnamigen Tupelports.
Alle IDs, Standardwerte, feste Kartenfallbacks, semantischen Rollenreferenzen
und Verbindungsreihenfolgen werden erhalten. Mehrdeutige Originale bleiben
exportierbar und nicht auswertbar. Migration läuft vor Darstellungsklassifikation;
diese respektiert Sperrdiagnosen und ausdrücklich gespeicherte Darstellungen.

Katalog, Definitionen, Suchindex und dynamische Builder erzeugen kanonische
Ziele. Android-Auswahlvarianten berücksichtigen die Zieleigenschaft; Anzeigenamen
begründen keine Identität. Historische Wissens- und Assetreferenzen bleiben auflösbar.

TupelAuflösen und TupelErgänzen behalten ihre eigenständigen mathematischen
Verträge. Die unaufgerufene pauschale Umsetzung in Vektorrechnermodi entfällt.

## Nachweis und Grenzen

Tests prüfen alle Codecphasen, Originalbewahrung, feste Fallbacks, gerichtete
Rollen, leere und verschachtelte Strukturen, beide Tiefen, Rückführung,
gleichnamige Quellen mit unterschiedlichen Bindungsidentitäten, Suche,
Auswahlidentitäten und Kopieren. Gemeinsamer Android-Inspector-Test prüft
Zielwechsel/Tiefe und getrennte Undo-Schritte. Gesamtsuite und SHA-bezogene
Android-/Linux-Laufzeitabnahme stehen vor Veröffentlichung noch aus.

Historische Tiefenbezeichnung alle ist durch den bestehenden Kern-/Wiretest
belegt und bleibt als rekursiver Alias gültig. Neuere explizite Auswahl verwendet
rekursiv; bestehende alle-Parameter werden nicht umgeschrieben. Unbekannte
Zahlen/Texte bleiben diagnostiziert. Der Wire-Zeugnisprüfer verwendet denselben
Tiefenvertrag; Payload-Version und Format bleiben unverändert.

Lokale Gesamtsuite am eingefrorenen M3-Produktstand: 2.193 Tests ohne Fehler,
ein bestehender Opt-in-Skip, vollständige angeforderte Build-/Lint-/Desktoptasks
erfolgreich. Unabhängige Lesekontrolle ohne offene Findings. Die lokale
Standalone-kotlinc-/RPM-Werkzeuggrenze und Android-Signaturgrenze bleiben;
endgültige PR-/Master-CI und isolierte Laufzeitprüfung stehen noch aus.

## Bewahrung gespeicherter Originalrevisionen

Androids vorhandener Speicher überschreibt unreferenzierte aktuelle Revisionen
bei gewöhnlichen Bearbeitungen. Eine mathematische oder Formatmigration darf
diesen Weg nicht verwenden: Die vorhandene Zielrevision wird fachneutral
dekodiert und mit ihrer vollständigen aktuellen Codec-Normalisierung verglichen.
Bei notwendiger Migration, abweichendem Format/ID/Version oder unlesbarem
Original wird eine neue freie Version oberhalb sämtlicher physischer Revisionen
geschrieben. Die Originaldatei bleibt unverändert, auch wenn keine fremde Karte
auf sie verweist. Die vorgeschlagene Nutzereingabe entscheidet diesen Trigger
nicht; gewöhnliche aktuelle Bearbeitungen behalten ihre Speichersemantik.
Desktop erzeugt bereits für jeden Speichervorgang eine neue Revision.

Gezielte JVM-Prüfung und Kompilierung dreier realer Android-Speichertests
erfolgreich, unabhängige Prüfung ohne Findings; erneute Gesamt- und CI-Abnahme
am neuen finalen M3-Head erforderlich.

M3-Revisionsschutz: vollständiger lokaler Abschlusslauf bestanden (49 s, 277 Tasks;
2195 Tests, 0 Fehler, 1 bestehender Skip), einschließlich Lint, APKs, Test-APKs
und Desktop. Protokoll: `/tmp/atlas-m3-revision-final.log`. CI am ergänzten
Release-Head und dessen Veröffentlichung stehen weiterhin aus.

# 0045 – Nullstellige Mengenoperatoren und gemeinsames Mengenwissen

Status: angenommen, 2026-10-12.

Die zehn bestehenden Mengenquellen sind `MengenKonstante` im Android-freien Kern.
Sie besitzen stabile Auswahlkennungen und die bestehende `OperatorSignatur` ohne
Eingänge mit Mengenausgang. Der Mengenknoten delegiert seine Auswahl an diesen
Katalog und verwendet den vorhandenen RechnerOperatorAuswahlDialog.
Eigene versionsfeste Mengenkarten bleiben gesonderte Quellen desselben Dialogs.

Mengenquellen werden keine numerische Rechnerfamilie: Dafür wären sachfremde
Formel- und Dispatcherpfade notwendig. Eine eigene UI oder ein weiterer
Signaturtyp würde dagegen bestehende Infrastruktur duplizieren.

Mitgliedschaft und Inklusion werden im Kern entschieden. Nur belegte Beziehungen
vereinfachen Mengenalgebra; nicht entscheidbare Fälle bleiben unbekannt.
Die vorhandene N0-Darstellung bleibt erhalten, wodurch keine Kartenmigration
oder neue Persistenzversion erforderlich ist.

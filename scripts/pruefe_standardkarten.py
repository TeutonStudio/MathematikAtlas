#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "app/src/main/assets/de/TeutonStudio/MathematikAtlas/standardkarten"
PACKAGE = ASSET_ROOT / "manifest.json"


def fail(message: str) -> None:
    raise SystemExit(f"Standardkarten-Prüfung fehlgeschlagen: {message}")


def canonical_hash(card: dict) -> str:
    payload = json.dumps(card, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


if not PACKAGE.is_file():
    fail(f"Paket fehlt: {PACKAGE.relative_to(ROOT)}")

data = json.loads(PACKAGE.read_text(encoding="utf-8"))
if data.get("standardKartenFormatVersion") != 1:
    fail("standardKartenFormatVersion muss 1 sein")
if data.get("targetCardFormatVersion") != 7:
    fail("targetCardFormatVersion muss dem Kartenformat 7 entsprechen")

entries = data.get("entries", [])
if not entries:
    fail("Paket muss Manifest-Einträge enthalten")

cards = []
for entry in entries:
    path = entry.get("path", "")
    candidate = (ASSET_ROOT / path).resolve()
    if not path or ASSET_ROOT.resolve() not in candidate.parents or not candidate.is_file():
        fail(f"{entry.get('sourceId')}: ungültiger oder fehlender Kartenpfad {path!r}")
    cards.append(json.loads(candidate.read_text(encoding="utf-8")))

entry_ids = [entry.get("sourceId") for entry in entries]
card_ids = [card.get("id") for card in cards]
if len(entry_ids) != len(set(entry_ids)):
    fail("sourceId ist nicht eindeutig")
if len(card_ids) != len(set(card_ids)):
    fail("Karten-ID ist nicht eindeutig")

entry_by_id = {entry["sourceId"]: entry for entry in entries}
card_by_id = {card["id"]: card for card in cards}
if set(entry.get("cardId") for entry in entries) != set(card_by_id):
    fail("cardId-Menge des Manifests stimmt nicht mit den Karten überein")

for entry in entries:
    source_id = entry["sourceId"]
    card_id = entry["cardId"]
    card = card_by_id[card_id]
    if card.get("formatVersion") != 7:
        fail(f"{source_id}: Kartenformat ist nicht 7")
    if card.get("version") != 1 or card.get("erstelltAm") != 0:
        fail(f"{source_id}: Quellkarte muss deterministisch als Version 1 mit erstelltAm=0 vorliegen")
    if entry.get("sourceHash") != canonical_hash(card):
        fail(f"{source_id}: sourceHash stimmt nicht mit dem Quellinhalt überein")

    nodes = card.get("knoten", [])
    node_ids = [node.get("id") for node in nodes]
    if len(node_ids) != len(set(node_ids)):
        fail(f"{source_id}: doppelte Knoten-ID")

    endpoints: set[tuple[str, str]] = set()
    actual_types: set[str] = set()
    note_count = 0
    referenced_cards: set[str] = set()

    for node in nodes:
        actual_types.add(node.get("art", ""))
        handles = node.get("anschlüsse", [])
        handle_ids = [handle.get("id") for handle in handles]
        if len(handle_ids) != len(set(handle_ids)):
            fail(f"{source_id}: doppelte Anschluss-ID in {node.get('id')}")
        endpoints.update((node["id"], handle["id"]) for handle in handles)

        if node.get("art") == "karte.notiz":
            if handles:
                fail(f"{source_id}: Notiz-Knoten darf keine Anschlüsse besitzen")
            text = node.get("parameter", {}).get("text", "")
            if "Erwartetes Ergebnis" in text:
                note_count += 1

        ref = node.get("kartenVerweis")
        if ref:
            target = ref.get("kartenId")
            if target not in card_by_id:
                fail(f"{source_id}: Kartenreferenz {target!r} fehlt im Paket")
            if ref.get("version") != 1:
                fail(f"{source_id}: Quell-Kartenreferenzen müssen Version 1 verwenden")
            referenced_cards.add(target)

    if note_count < 1:
        fail(f"{source_id}: anschlusslose Soll-Ergebnis-Notiz fehlt")

    required = set(entry.get("requiredNodeTypes", []))
    if not actual_types.issubset(required):
        fail(f"{source_id}: requiredNodeTypes fehlen {sorted(actual_types - required)}")

    edge_ids: set[str] = set()
    for edge in card.get("verbindungen", []):
        edge_id = edge.get("id")
        if edge_id in edge_ids:
            fail(f"{source_id}: doppelte Verbindungs-ID {edge_id}")
        edge_ids.add(edge_id)
        for side in ("von", "zu"):
            ref = edge.get(side, {})
            endpoint = (ref.get("knotenId"), ref.get("anschlussId"))
            if endpoint not in endpoints:
                fail(f"{source_id}: Verbindung {edge_id} verweist auf fehlenden Anschluss")

    declared_deps = set(entry.get("dependsOn", []))
    missing_deps = declared_deps - set(entry_by_id)
    if missing_deps:
        fail(f"{source_id}: unbekannte dependsOn-Einträge {sorted(missing_deps)}")
    if referenced_cards != {entry_by_id[dep]["cardId"] for dep in declared_deps}:
        fail(f"{source_id}: Kartenreferenzen und dependsOn stimmen nicht überein")

    folder = entry.get("folder", [])
    if len(folder) < 3 or folder[0] != "Standardkarten":
        fail(f"{source_id}: ungültiger Standardordner {folder!r}")
    if folder[1] == "Funktionen":
        outputs = [
            handle
            for node in nodes if node.get("art") == "mathematik.kartenAusgang"
            for handle in node.get("anschlüsse", [])
            if handle.get("richtung") == "Eingang" and handle.get("art") == "mathematik.methode"
        ]
        if not outputs:
            fail(f"{source_id}: Funktionskarte besitzt keinen typisierten Methodenausgang")

visiting: set[str] = set()
visited: set[str] = set()


def visit(source_id: str) -> None:
    if source_id in visited:
        return
    if source_id in visiting:
        fail(f"zyklische Kartenabhängigkeit bei {source_id}")
    visiting.add(source_id)
    for dep in entry_by_id[source_id].get("dependsOn", []):
        visit(dep)
    visiting.remove(source_id)
    visited.add(source_id)


for source_id in entry_ids:
    visit(source_id)

print(
    f"Standardkarten geprüft: {len(cards)} Karten, "
    f"{sum(1 for e in entries if e['folder'][1] == 'Funktionen')} Funktionskarten, "
    f"{sum(1 for e in entries if e['folder'][1] == 'Analysis 1')} Analysis-I-Karten, "
    f"{sum(1 for e in entries if e['folder'][1] == 'Analysis 2')} Analysis-II-Karten."
)

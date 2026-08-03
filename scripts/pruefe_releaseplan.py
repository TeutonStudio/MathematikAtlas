#!/usr/bin/env python3
"""Prüft den maschinenlesbaren Releaseplan und die Android-Versionsmetadaten."""

from __future__ import annotations

import re
import sys
import tomllib
from pathlib import Path

WURZEL = Path(__file__).resolve().parents[1]
PLAN_PFAD = WURZEL / "release" / "roadmap.toml"
APP_GRADLE = WURZEL / "app" / "build.gradle.kts"
SEMVER = re.compile(r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")
ERLAUBTE_STATUS = {
    "planned",
    "reserved",
    "implementing",
    "review",
    "ready",
    "released",
    "blocked",
    "superseded",
    "skipped",
}


def fehler(text: str) -> None:
    print(f"Releaseplan-Fehler: {text}", file=sys.stderr)
    raise SystemExit(1)


def versionsteile(version: str) -> tuple[int, int, int]:
    treffer = SEMVER.fullmatch(version)
    if treffer is None:
        fehler(f"Ungültige SemVer-Version {version!r}.")
    return tuple(map(int, treffer.groups()))


def erlaubte_releasebranches(version: str) -> tuple[str, ...]:
    """Historische und aktuelle Branchmuster für einen veröffentlichten Stand."""
    return (
        f"agent/v{version}-",
        f"release/v{version}-",
        f"samai/v{version}-",
        f"samai/v{version}/",
    )


def lade_plan() -> dict:
    if not PLAN_PFAD.is_file():
        fehler(f"{PLAN_PFAD.relative_to(WURZEL)} fehlt.")
    with PLAN_PFAD.open("rb") as datei:
        return tomllib.load(datei)


def prüfe_plan(plan: dict) -> tuple[str, dict[str, dict]]:
    if plan.get("schema_version") != 1:
        fehler("schema_version muss 1 sein.")
    if plan.get("default_branch") != "master":
        fehler("default_branch muss master sein.")

    aktuelle_version = str(plan.get("current_version", ""))
    versionsteile(aktuelle_version)
    einträge = plan.get("releases")
    if not isinstance(einträge, list) or not einträge:
        fehler("Mindestens ein [[releases]]-Eintrag ist erforderlich.")

    nach_version: dict[str, dict] = {}
    for eintrag in einträge:
        if not isinstance(eintrag, dict):
            fehler("Jeder Releaseeintrag muss eine TOML-Tabelle sein.")
        version = str(eintrag.get("version", ""))
        versionsteile(version)
        if version in nach_version:
            fehler(f"Version {version} ist doppelt eingetragen.")
        status = eintrag.get("status")
        if status not in ERLAUBTE_STATUS:
            fehler(f"Version {version} besitzt den unbekannten Status {status!r}.")
        for feld in ("title", "roadmap"):
            if not str(eintrag.get(feld, "")).strip():
                fehler(f"Version {version} benötigt das Feld {feld}.")
        nach_version[version] = eintrag

    aktuell = nach_version.get(aktuelle_version)
    if aktuell is None:
        fehler(f"current_version {aktuelle_version} besitzt keinen Releaseeintrag.")
    if aktuell.get("status") != "released":
        fehler("current_version muss im veröffentlichten Stand den Status released besitzen.")

    aktive = [
        version
        for version, eintrag in nach_version.items()
        if eintrag.get("status") in {"reserved", "implementing", "review", "ready"}
    ]
    if aktive:
        fehler(f"Der veröffentlichte Plan enthält noch aktive Versionen: {', '.join(sorted(aktive))}.")

    for version, eintrag in nach_version.items():
        status = eintrag["status"]
        if status == "superseded":
            ziel = str(eintrag.get("superseded_by", ""))
            if ziel not in nach_version or nach_version[ziel].get("status") != "released":
                fehler(f"Version {version} verweist nicht auf ein veröffentlichtes superseded_by-Ziel.")
            if not str(eintrag.get("reason", "")).strip():
                fehler(f"Version {version} benötigt eine Begründung für superseded.")
        if status == "skipped" and not str(eintrag.get("reason", "")).strip():
            fehler(f"Version {version} benötigt eine Begründung für skipped.")

    besucht: set[str] = set()
    cursor = aktuelle_version
    while cursor:
        if cursor in besucht:
            fehler(f"Zyklus in der Kette veröffentlichter Releases bei {cursor}.")
        besucht.add(cursor)
        eintrag = nach_version[cursor]
        if eintrag.get("status") != "released":
            fehler(f"Releasekette erreicht die nicht veröffentlichte Version {cursor}.")
        vorgänger = eintrag.get("previous_release")
        if vorgänger is None:
            break
        vorgänger = str(vorgänger)
        if vorgänger not in nach_version:
            fehler(f"Vorgänger {vorgänger} von {cursor} fehlt.")
        cursor = vorgänger

    aktueller_branch = str(aktuell.get("branch", ""))
    erwartete_präfixe = erlaubte_releasebranches(aktuelle_version)
    if aktueller_branch and not aktueller_branch.startswith(erwartete_präfixe):
        fehler(
            f"Branch {aktueller_branch!r} passt nicht zur aktuellen Version {aktuelle_version}. "
            f"Erlaubt sind die Präfixe: {', '.join(erwartete_präfixe)}."
        )

    return aktuelle_version, nach_version


def prüfe_android_version(aktuelle_version: str) -> None:
    text = APP_GRADLE.read_text(encoding="utf-8")
    name_treffer = re.search(r'\bversionName\s*=\s*"([^"]+)"', text)
    code_treffer = re.search(r"\bversionCode\s*=\s*(\d+)", text)
    if name_treffer is None or code_treffer is None:
        fehler("versionName oder versionCode fehlt in app/build.gradle.kts.")

    major, minor, patch = versionsteile(aktuelle_version)
    erwarteter_code = major * 1_000_000 + minor * 1_000 + patch
    if name_treffer.group(1) != aktuelle_version:
        fehler(
            f"Android versionName {name_treffer.group(1)!r} stimmt nicht mit {aktuelle_version!r} überein."
        )
    if int(code_treffer.group(1)) != erwarteter_code:
        fehler(
            f"Android versionCode muss für {aktuelle_version} den Wert {erwarteter_code} besitzen."
        )


def main() -> None:
    plan = lade_plan()
    aktuelle_version, einträge = prüfe_plan(plan)
    prüfe_android_version(aktuelle_version)
    print(
        f"Releaseplan erfolgreich geprüft: {len(einträge)} Einträge, "
        f"aktuelle Version {aktuelle_version}."
    )


if __name__ == "__main__":
    main()

package de.TeutonStudio.KnotenKartenVerwalter.daten

import java.util.UUID

@JvmInline value class KartenId(val wert: String) { override fun toString() = wert }
@JvmInline value class KnotenId(val wert: String) { override fun toString() = wert }
@JvmInline value class AnschlussId(val wert: String) { override fun toString() = wert }
@JvmInline value class VerbindungsId(val wert: String) { override fun toString() = wert }
@JvmInline value class AnschlussArtId(val wert: String) { override fun toString() = wert }

typealias KnotenArtId = String

fun neueKartenId() = KartenId(UUID.randomUUID().toString())
fun neueKnotenId() = KnotenId(UUID.randomUUID().toString())
fun neueAnschlussId() = AnschlussId(UUID.randomUUID().toString())
fun neueVerbindungsId() = VerbindungsId(UUID.randomUUID().toString())

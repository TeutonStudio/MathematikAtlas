package de.TeutonStudio.MathematikAtlas

import kotlin.test.Test
import kotlin.test.assertEquals

class KartenExportFormatTest {
    @Test
    fun `exportendung wird genau einmal normalisiert`() {
        assertEquals("Atlas.json", normalisiereExportDateiname("Atlas.matlas", KartenExportFormat.JSON))
        assertEquals("Atlas.matlas", normalisiereExportDateiname("Atlas.json", KartenExportFormat.MATLAS))
    }
}

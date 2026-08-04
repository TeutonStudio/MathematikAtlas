package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikAtlas.speicher.sichererFreigabeDateiname
import kotlin.test.Test
import kotlin.test.assertEquals

class MatlasDateitypTest {
    @Test
    fun `matlas verwendet eigenen JSON Vendor MIME Typ`() {
        assertEquals("application/vnd.mathematik-atlas+json", MATLAS_MIME_TYPE)
    }

    @Test
    fun `Freigaben erhalten die matlas Dateiendung`() {
        assertEquals("Analysis.matlas", sichererFreigabeDateiname("Analysis"))
    }
}

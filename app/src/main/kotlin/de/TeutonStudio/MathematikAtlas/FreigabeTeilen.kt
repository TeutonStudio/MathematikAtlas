package de.TeutonStudio.MathematikAtlas

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import de.TeutonStudio.MathematikAtlas.speicher.sichererFreigabeDateiname
import java.io.File

internal fun Context.teileMathematikAtlasPaket(name: String, inhalt: String) {
    val ordner = File(cacheDir, "freigaben").apply { mkdirs() }
    val basisName = name.removeSuffix(MATLAS_DATEIENDUNG)
    val datei = File(ordner, sichererFreigabeDateiname(basisName))
    datei.writeText(inhalt)
    val uri = FileProvider.getUriForFile(this, "$packageName.dateien", datei)
    val senden = Intent(Intent.ACTION_SEND).apply {
        type = MATLAS_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, datei.name)
        clipData = ClipData.newUri(contentResolver, datei.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(senden, "Mathematik-Atlas-Freigabe teilen"))
}

package de.TeutonStudio.MathematikAtlas

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import de.TeutonStudio.MathematikAtlas.speicher.sichererFreigabeDateiname
import java.io.File

internal fun Context.teileMathematikAtlasPaket(name: String, inhalt: String) {
    val ordner = File(cacheDir, "freigaben").apply { mkdirs() }
    val datei = File(ordner, sichererFreigabeDateiname(name))
    datei.writeText(inhalt)
    val uri = FileProvider.getUriForFile(this, "$packageName.dateien", datei)
    val senden = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, datei.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(senden, "Mathematik-Atlas-Freigabe teilen"))
}

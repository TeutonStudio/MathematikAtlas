import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.tasks.JavaExec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin { jvmToolchain(17) }

fun systemdBenutzerUmgebung(): Map<String, String> {
    if (!System.getProperty("os.name").startsWith("Linux", ignoreCase = true)) return emptyMap()

    return runCatching {
        val prozess = ProcessBuilder("systemctl", "--user", "show-environment")
            .redirectErrorStream(true)
            .start()
        val ausgabe = prozess.inputStream.bufferedReader().use { it.readText() }
        if (prozess.waitFor() != 0) return@runCatching emptyMap()

        ausgabe.lineSequence()
            .mapNotNull { zeile ->
                val trenner = zeile.indexOf('=')
                if (trenner <= 0) null
                else zeile.substring(0, trenner) to zeile.substring(trenner + 1)
            }
            .toMap()
    }.getOrDefault(emptyMap())
}

fun brauchbareSitzungsVariable(name: String, wert: String?): String? {
    val normalisiert = wert?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return if (name == "XAUTHORITY" && !File(normalisiert).isFile) null else normalisiert
}

dependencies {
    implementation(project(":KnotenKartenVerwalterDesktop"))
    implementation(project(":MathematikRechenSystem"))
    implementation(project(":MathematikKartenAdapterDesktop"))
    implementation(project(":MathematikKnotenDesktop"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.json:json:20250517")
    testImplementation(kotlin("test-junit"))
}

compose.desktop {
    application {
        mainClass = "de.TeutonStudio.MathematikAtlas.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Rpm, TargetFormat.Deb)
            packageName = "mathematik-atlas"
            packageVersion = "2.33.0"
            description = "Mathematische Prozesse als interaktive Knotenkarten"
            vendor = "TeutonStudio"
            linux {
                menuGroup = "Education"
                appCategory = "Education"
                shortcut = true
            }
            modules("java.desktop", "java.logging", "java.prefs")
        }
    }
}

// Android Studio und bereits laufende Gradle-Daemons erben unter GNOME/Wayland
// nicht immer DISPLAY/XAUTHORITY. Fehlende Werte werden deshalb beim lokalen
// Desktop-Start aus der systemd-Benutzersitzung ergänzt. Gültige geerbte Werte
// haben Vorrang, damit z. B. SSH-X11-Forwarding nicht auf die lokale Sitzung
// umgebogen wird.
tasks.named<JavaExec>("run") {
    doFirst {
        if (!System.getProperty("os.name").startsWith("Linux", ignoreCase = true)) return@doFirst

        val systemdUmgebung = systemdBenutzerUmgebung()
        val ausSystemd = mutableListOf<String>()

        fun auflösen(name: String): String? {
            brauchbareSitzungsVariable(name, System.getenv(name))?.let { return it }
            return brauchbareSitzungsVariable(name, systemdUmgebung[name])?.also { ausSystemd += name }
        }

        val display = auflösen("DISPLAY")
            ?: throw GradleException(
                "Desktop-Start nicht möglich: DISPLAY fehlt. " +
                    "Starte Gradle aus einer grafischen Linux-Sitzung oder stelle sicher, " +
                    "dass 'systemctl --user show-environment' DISPLAY enthält.",
            )

        environment("DISPLAY", display)
        listOf("XAUTHORITY", "WAYLAND_DISPLAY", "XDG_SESSION_TYPE").forEach { name ->
            auflösen(name)?.let { environment(name, it) }
        }

        if (ausSystemd.isNotEmpty()) {
            logger.lifecycle(
                "Desktop-Start: Sitzungsvariablen aus systemd übernommen: ${ausSystemd.distinct().joinToString()}"
            )
        }
    }
}

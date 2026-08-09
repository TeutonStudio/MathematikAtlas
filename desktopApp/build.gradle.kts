import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin { jvmToolchain(17) }

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
            packageVersion = "2.28.8"
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

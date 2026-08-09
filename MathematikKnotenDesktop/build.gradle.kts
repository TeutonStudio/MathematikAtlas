plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(17)
    sourceSets.named("main") {
        kotlin.srcDir("../MathematikKnoten/src/main/kotlin")
        kotlin.exclude("de/TeutonStudio/MathematikKnoten/visualisierung/ui/VisualisierungsKnotenRenderer.kt")
    }
}

sourceSets.main {
    resources.srcDir("../MathematikKnoten/src/main/assets")
}

dependencies {
    api(project(":KnotenKartenVerwalterDesktop"))
    api(project(":MathematikRechenSystem"))
    api(project(":MathematikKartenAdapterDesktop"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation("io.github.huarangmeng:latex-base:1.3.0")
    implementation("io.github.huarangmeng:latex-parser:1.3.0")
    implementation("io.github.huarangmeng:latex-renderer:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.json:json:20250517")
    testImplementation(kotlin("test-junit"))
}

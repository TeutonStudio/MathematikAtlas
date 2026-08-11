plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(17)
    sourceSets.named("main") {
        kotlin.srcDir("../KnotenKartenVerwalter/src/main/kotlin")
    }
}

dependencies {
    api(project(":TypSystem"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation("org.json:json:20250517")
    testImplementation(kotlin("test-junit"))
}

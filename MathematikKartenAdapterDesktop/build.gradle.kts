plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
    sourceSets.named("main") {
        kotlin.srcDir("../MathematikKartenAdapter/src/main/kotlin")
    }
}

dependencies {
    api(project(":KnotenKartenVerwalterDesktop"))
    api(project(":MathematikRechenSystem"))
    testImplementation(kotlin("test-junit"))
}

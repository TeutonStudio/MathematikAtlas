plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin { jvmToolchain(17) }

fun normalisiereGitRevision(wert: String): String = wert
    .trim()
    .takeIf { it.matches(Regex("[0-9a-fA-F]{7,40}")) }
    ?.lowercase()
    ?: "local"

val lokaleGitRevision = providers.provider {
    runCatching {
        val prozess = ProcessBuilder("git", "rev-parse", "--verify", "HEAD")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        val ausgabe = prozess.inputStream.bufferedReader().use { it.readText() }.trim()
        if (prozess.waitFor() == 0) ausgabe else ""
    }.getOrDefault("")
}

val gitCommitSha = providers.environmentVariable("GITHUB_SHA")
    .orElse(lokaleGitRevision)
    .map(::normalisiereGitRevision)
    .getOrElse("local")

android {
    namespace = "de.TeutonStudio.MathematikAtlas"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.TeutonStudio.MathematikAtlas"
        minSdk = 26
        targetSdk = 36
        versionCode = 2028008
        versionName = "2.28.8"
        buildConfigField("String", "GIT_COMMIT_SHA", "\"$gitCommitSha\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(project(":KnotenKartenVerwalter"))
    implementation(project(":MathematikRechenSystem"))
    implementation(project(":MathematikKartenAdapter"))
    implementation(project(":MathematikKnoten"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}

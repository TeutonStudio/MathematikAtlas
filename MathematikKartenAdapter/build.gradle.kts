plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

kotlin { jvmToolchain(17) }

android {
    namespace = "de.TeutonStudio.MathematikKartenAdapter"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":KnotenKartenVerwalter"))
    api(project(":MathematikRechenSystem"))
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MathematikAtlas"
include(":app")
include(":KnotenKartenVerwalter")
include(":MathematikRechenSystem")
include(":MathematikKartenAdapter")
include(":MathematikKnoten")
include(":KnotenKartenVerwalterDesktop")
include(":MathematikKartenAdapterDesktop")
include(":MathematikKnotenDesktop")
include(":desktopApp")

package de.TeutonStudio.MathematikAtlas

internal data class AppBuildInformation(
    val versionName: String,
    val versionCode: Int,
    val commitSha: String?,
) {
    val versionsZeile: String
        get() = "Version ${versionName.trim().ifBlank { "unbekannt" }}"

    val kurzeCommitSha: String?
        get() = commitSha
            ?.trim()
            ?.takeIf { it.matches(Regex("[0-9a-fA-F]{7,40}")) }
            ?.lowercase()
            ?.take(12)

    val buildZeile: String
        get() = kurzeCommitSha?.let { "Build $it" } ?: "Lokaler Build"
}

internal fun aktuelleAppBuildInformation(): AppBuildInformation = AppBuildInformation(
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    commitSha = BuildConfig.GIT_COMMIT_SHA,
)

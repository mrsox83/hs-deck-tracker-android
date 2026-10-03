package com.stroexd.hsdecktracker.core.update

import com.stroexd.hsdecktracker.core.data.HttpClient

/** A release of the app on GitHub; its APK always has the same name. */
data class AppRelease(val version: String, private val repository: String = REPOSITORY) {
    val apkUrl: String get() = "$repository/releases/download/v$version/hs-deck-tracker.apk"

    companion object {
        const val REPOSITORY = "https://github.com/stroexd/hs-deck-tracker-android"
    }
}

class ReleaseChecker(private val http: HttpClient, private val repository: String = AppRelease.REPOSITORY) {
    /** GitHub's "latest" link redirects to the release's tag, which saves the rate-limited API. */
    suspend fun latest(): AppRelease? {
        val tag = http.head("$repository/releases/latest").url.substringAfter("/releases/tag/", "")
        val version = tag.removePrefix("v")
        return if (AppVersion.parse(version) != null) AppRelease(version, repository) else null
    }
}

object AppVersion {
    /** "1.4.0" or "1.4.0-debug" → [1, 4, 0]. */
    fun parse(version: String): List<Int>? {
        val parts = version.substringBefore('-').split('.')
        return parts.map { it.toIntOrNull() ?: return null }
    }

    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate) ?: return false
        val b = parse(current) ?: return true
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

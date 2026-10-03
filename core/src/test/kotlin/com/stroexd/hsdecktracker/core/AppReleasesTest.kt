package com.stroexd.hsdecktracker.core

import com.stroexd.hsdecktracker.core.data.HttpClient
import com.stroexd.hsdecktracker.core.update.AppRelease
import com.stroexd.hsdecktracker.core.update.AppVersion
import com.stroexd.hsdecktracker.core.update.ReleaseChecker
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppReleasesTest {
    private val server = MockWebServer()

    @AfterTest
    fun stopServer() = server.shutdown()

    @Test
    fun comparesVersionsNumerically() {
        assertTrue(AppVersion.isNewer("1.10.0", "1.9.3"))
        assertTrue(AppVersion.isNewer("1.4.1", "1.4.0-debug"))
        assertTrue(AppVersion.isNewer("2.0", "1.9.9"))
        assertFalse(AppVersion.isNewer("1.4.0", "1.4.0"))
        assertFalse(AppVersion.isNewer("1.3.9", "1.4.0"))
        assertFalse(AppVersion.isNewer("latest", "1.4.0"))
        assertNull(AppVersion.parse("v1.4"))
    }

    @Test
    fun followsTheLatestLinkAndDownloadsTheApk() = runTest {
        var tag = "v1.5.0"
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path == "/repo/releases/latest" -> MockResponse().setResponseCode(302).setHeader("Location", "/repo/releases/tag/$tag")
                    path.startsWith("/repo/releases/tag/") -> MockResponse()
                    path == "/repo/releases/download/v1.5.0/hs-deck-tracker.apk" -> MockResponse().setBody("apk")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val repository = server.url("/repo").toString()
        val http = HttpClient()
        val release = ReleaseChecker(http, repository).latest()
        assertEquals(AppRelease("1.5.0", repository), release)

        val target = File(Files.createTempDirectory("update").toFile(), "updates/app.apk")
        http.download(release!!.apkUrl, target)
        assertEquals("apk", target.readText())
        assertFalse(File(target.path + ".part").exists())

        tag = "nightly"
        assertNull(ReleaseChecker(http, repository).latest())
    }
}

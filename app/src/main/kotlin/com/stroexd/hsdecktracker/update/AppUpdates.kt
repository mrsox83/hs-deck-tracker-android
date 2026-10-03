package com.stroexd.hsdecktracker.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import androidx.core.content.pm.PackageInfoCompat
import com.stroexd.hsdecktracker.BuildConfig
import com.stroexd.hsdecktracker.R
import com.stroexd.hsdecktracker.appContainer
import com.stroexd.hsdecktracker.core.update.AppVersion
import com.stroexd.hsdecktracker.core.update.ReleaseChecker
import com.stroexd.hsdecktracker.ui.localized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Keeps the app up to date from its GitHub releases. Checked twice a day on Wi-Fi and never installed while a game
 * is tracked: the update restarts the app. Android 12+ installs it without asking once installing is allowed.
 */
object AppUpdates {
    enum class Result { UP_TO_DATE, INSTALLING, BUSY, FAILED }

    private const val JOB_ID = 4_201
    private const val CHANNEL_ID = "updates"
    private const val NOTIFICATION_ID = 4_202
    internal const val EXTRA_INTERACTIVE = "interactive"
    internal const val EXTRA_VERSION = "version"
    private val mutex = Mutex()

    fun schedule(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (BuildConfig.DEBUG || !context.appContainer.settings.value.autoUpdates) {
            scheduler.cancel(JOB_ID)
            return
        }
        if (scheduler.getPendingJob(JOB_ID) != null) return
        scheduler.schedule(
            JobInfo.Builder(JOB_ID, ComponentName(context, UpdateJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setRequiresBatteryNotLow(true)
                .setPeriodic(TimeUnit.HOURS.toMillis(12))
                .build(),
        )
    }

    fun canInstallSilently(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /** [interactive]: started by the user, who then sees Android's install dialog if one is needed. */
    suspend fun update(context: Context, interactive: Boolean): Result = withContext(Dispatchers.IO) {
        if (!mutex.tryLock()) return@withContext Result.BUSY
        try {
            checkAndInstall(context, interactive)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.FAILED
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun checkAndInstall(context: Context, interactive: Boolean): Result {
        val container = context.appContainer
        val release = ReleaseChecker(container.http).latest() ?: return Result.FAILED
        if (!AppVersion.isNewer(release.version, BuildConfig.VERSION_NAME)) {
            downloads(context).deleteRecursively()
            return Result.UP_TO_DATE
        }
        if (container.recognition.value.active) return Result.BUSY
        val apk = File(downloads(context), "hs-deck-tracker-${release.version}.apk")
        if (!apk.exists()) {
            downloads(context).deleteRecursively()
            container.http.download(release.apkUrl, apk)
        }
        if (!isUpdate(context, apk)) {
            apk.delete()
            return Result.FAILED
        }
        // The download can take a while; a game may have started meanwhile
        if (container.recognition.value.active) return Result.BUSY
        install(context, apk, release.version, interactive)
        return Result.INSTALLING
    }

    internal fun downloads(context: Context) = File(context.cacheDir, "updates")

    @Suppress("DEPRECATION")
    private fun isUpdate(context: Context, apk: File): Boolean {
        val archive = context.packageManager.getPackageArchiveInfo(apk.path, 0) ?: return false
        val installed = context.packageManager.getPackageInfo(context.packageName, 0)
        return archive.packageName == context.packageName &&
            PackageInfoCompat.getLongVersionCode(archive) > PackageInfoCompat.getLongVersionCode(installed)
    }

    private fun install(context: Context, apk: File, version: String, interactive: Boolean) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val result = Intent(context, InstallResultReceiver::class.java)
                .putExtra(EXTRA_INTERACTIVE, interactive)
                .putExtra(EXTRA_VERSION, version)
            // Mutable: the installer adds the status
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            session.commit(PendingIntent.getBroadcast(context, sessionId, result, flags).intentSender)
        }
    }

    internal fun notifyReady(context: Context, confirm: Intent, version: String) {
        val text = context.localized()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, text.getString(R.string.updates_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val tap = PendingIntent.getActivity(
            context,
            0,
            confirm,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tracker)
            .setContentTitle(text.getString(R.string.update_ready))
            .setContentText(text.getString(R.string.update_ready_text, version))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }
}

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java) ?: return
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.getBooleanExtra(AppUpdates.EXTRA_INTERACTIVE, false)) {
                    runCatching { context.startActivity(confirm) }
                } else {
                    AppUpdates.notifyReady(context, confirm, intent.getStringExtra(AppUpdates.EXTRA_VERSION).orEmpty())
                }
            }
            PackageInstaller.STATUS_SUCCESS -> AppUpdates.downloads(context).deleteRecursively()
        }
    }
}

class UpdateJob : JobService() {
    private var job: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        job = appContainer.appScope.launch {
            val result = AppUpdates.update(this@UpdateJob, interactive = false)
            jobFinished(params, result == AppUpdates.Result.BUSY)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        job?.cancel()
        return true
    }
}

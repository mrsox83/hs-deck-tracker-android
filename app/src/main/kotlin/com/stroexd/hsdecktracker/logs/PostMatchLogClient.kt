package com.stroexd.hsdecktracker.logs

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import com.stroexd.hsdecktracker.core.fusion.PostMatchLogSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Foreground-owner scoped: create/close on main thread; permission requests require a user action.
 * Not instantiated by the app yet. No Accessibility, overlay, polling, storage or cloud dependency.
 */
class PostMatchLogClient(context: Context) : Closeable {
    enum class Availability { STOPPED, NEEDS_PERMISSION, READY, CONNECTING, CONNECTED, LOST, CLOSED }
    private val mutableAvailability = MutableStateFlow(Availability.STOPPED)
    val availability = mutableAvailability.asStateFlow()
    private val mutableLastError = MutableStateFlow<String?>(null)
    val lastError = mutableLastError.asStateFlow()
    private val closed = AtomicBoolean(false)
    @Volatile private var reader: ILogReader? = null
    private var pending: CompletableDeferred<ILogReader>? = null
    private var binding = false
    private val args = Shizuku.UserServiceArgs(ComponentName(context.applicationContext, RestrictedLogReader::class.java))
        .daemon(false).processNameSuffix("fusionlogreader").debuggable(false).version(1)
    private val received = Shizuku.OnBinderReceivedListener { if (!closed.get()) refresh() }
    private val dead = Shizuku.OnBinderDeadListener { loseConnection("Shizuku stopped") }
    private val permission = Shizuku.OnRequestPermissionResultListener { code, _ ->
        if (code == PERMISSION_REQUEST && !closed.get()) refresh()
    }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            if (closed.get() || !binding) return
            if (!binder.isBinderAlive) { loseConnection("Reader Binder is not alive"); return }
            val connected = ILogReader.Stub.asInterface(binder)
            reader = connected
            mutableAvailability.value = Availability.CONNECTED
            pending?.complete(connected)
        }
        override fun onServiceDisconnected(name: ComponentName) { loseConnection("Reader disconnected") }
    }

    init {
        requireMain()
        Shizuku.addBinderDeadListener(dead)
        Shizuku.addRequestPermissionResultListener(permission)
        Shizuku.addBinderReceivedListenerSticky(received)
        refresh()
    }

    /** No automatic request/retry; UI must explain restricted post-match access before calling. */
    fun requestPermission() {
        requireMain()
        check(!closed.get()) { "Reader client closed" }
        check(Shizuku.pingBinder()) { "Start Shizuku first" }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
            Shizuku.requestPermission(PERMISSION_REQUEST)
        else refresh()
    }

    suspend fun connect() = withContext(Dispatchers.Main.immediate) {
        check(!closed.get()) { "Reader client closed" }
        reader?.let { return@withContext }
        check(!binding) { "Reader connection already in progress; wait or reopen" }
        refresh()
        check(mutableAvailability.value == Availability.READY) { "Start Shizuku and grant permission before connecting" }
        val waiting = CompletableDeferred<ILogReader>()
        mutableLastError.value = null
        pending = waiting
        binding = true
        mutableAvailability.value = Availability.CONNECTING
        try {
            Shizuku.bindUserService(args, connection)
            withTimeout(15_000) { waiting.await() }
        } catch (error: Throwable) {
            mutableLastError.value = error.message ?: error.javaClass.simpleName
            releaseBinding()
            if (!closed.get()) mutableAvailability.value = Availability.LOST
            throw error
        } finally {
            if (pending === waiting) pending = null
        }
        Unit
    }

    suspend fun listSessions(): List<String> = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        val names = connectedReader().listSessions() ?: throw IOException("Reader returned no session list")
        currentCoroutineContext().ensureActive()
        names.forEach(PostMatchLogSnapshot::requireSession)
        names.distinct().sortedDescending()
    }

    suspend fun snapshot(session: String): PostMatchLogSnapshot.Snapshot = withContext(Dispatchers.IO) {
        PostMatchLogSnapshot.requireSession(session)
        val jobContext = currentCoroutineContext()
        jobContext.ensureActive()
        val source = connectedReader()
        // Descriptor size and stream refer to the same open inode. Outer use also covers bad size,
        // cancellation before stream construction and constructor failure, unlike the old null dereference.
        val descriptor = source.openLog(session) ?: throw IOException("Reader returned no Power.log descriptor")
        descriptor.use { fd ->
            PostMatchLogSnapshot.read(session, fd.statSize, { ParcelFileDescriptor.AutoCloseInputStream(fd) }) {
                jobContext.ensureActive()
                closed.get() || reader !== source
            }
        }
    }

    private fun connectedReader(): ILogReader {
        check(!closed.get()) { "Reader client closed" }
        return reader ?: throw IOException("Reader disconnected; reconnect manually")
    }

    private fun refresh() {
        requireMain()
        try {
            mutableAvailability.value = when {
                closed.get() -> Availability.CLOSED
                !Shizuku.pingBinder() -> Availability.STOPPED
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> Availability.NEEDS_PERMISSION
                reader != null -> Availability.CONNECTED
                binding -> Availability.CONNECTING
                else -> Availability.READY
            }
        } catch (error: Exception) {
            // Binder may die between ping and permission check; report instead of crashing owner UI.
            loseConnection(error.message ?: "Shizuku permission check failed")
        }
    }

    private fun loseConnection(reason: String) {
        mutableLastError.value = reason
        reader = null
        pending?.completeExceptionally(IOException(reason))
        // Keep binding registration until explicit close or connect-failure cleanup, rather than
        // forgetting a registration and leaking it on retry. Owner reopens after a connected loss.
        if (!closed.get()) mutableAvailability.value = Availability.LOST
    }

    private fun releaseBinding() {
        reader = null
        if (binding) {
            binding = false
            // Always remove local callbacks; remote removal may fail if Shizuku itself died.
            val detach = runCatching { Shizuku.unbindUserService(args, connection, false) }.exceptionOrNull()
            val remove = runCatching { Shizuku.unbindUserService(args, connection, true) }.exceptionOrNull()
            if (detach != null || remove != null) {
                mutableLastError.value = "Reader cleanup could not be confirmed: " +
                    listOfNotNull(detach?.message, remove?.message).joinToString("; ")
            }
        }
    }

    override fun close() {
        requireMain()
        if (!closed.compareAndSet(false, true)) return
        pending?.completeExceptionally(IOException("Reader client closed"))
        Shizuku.removeBinderReceivedListener(received)
        Shizuku.removeBinderDeadListener(dead)
        Shizuku.removeRequestPermissionResultListener(permission)
        releaseBinding()
        mutableAvailability.value = Availability.CLOSED
    }

    private fun requireMain() = check(Looper.myLooper() == Looper.getMainLooper()) { "Reader lifecycle must run on main thread" }
    companion object { private const val PERMISSION_REQUEST = 0x4853 }
}

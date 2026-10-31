package com.stroexd.hsdecktracker.core.vision

/** Single callback slot used by the existing projection source, confined to its handler thread.
 * One fallback grace deadline is allowed per request; old deadlines cannot complete a newer one.
 */
class PendingCaptureRequest<T>(
    private val timeoutMillis: Long,
    private val post: (Runnable, Long) -> Boolean,
    private val remove: (Runnable) -> Unit,
    private val onTimeout: () -> Boolean,
) {
    init { require(timeoutMillis > 0) }
    private class Request<T>(val callback: (T?) -> Unit) {
        lateinit var timeout: Runnable
        var graceUsed = false
    }
    private var active: Request<T>? = null

    /** Reject overlap rather than losing the existing request's callback. */
    fun begin(callback: (T?) -> Unit): Boolean {
        if (active != null) { callback(null); return false }
        val request = Request<T>(callback)
        request.timeout = Runnable {
            if (active !== request) return@Runnable
            val grace = runCatching(onTimeout).getOrDefault(false)
            if (active !== request) return@Runnable
            if (grace && !request.graceUsed) {
                request.graceUsed = true
                arm(request)
            } else take()?.invoke(null)
        }
        active = request
        arm(request)
        return active === request
    }

    private fun arm(request: Request<T>) {
        runCatching { remove(request.timeout) }
        if (!runCatching { post(request.timeout, timeoutMillis) }.getOrDefault(false) && active === request) take()?.invoke(null)
    }

    /** Clears/cancels before returning callback, so completion can safely request another frame. */
    fun take(): ((T?) -> Unit)? {
        val request = active ?: return null
        active = null
        runCatching { remove(request.timeout) }
        return request.callback
    }
}

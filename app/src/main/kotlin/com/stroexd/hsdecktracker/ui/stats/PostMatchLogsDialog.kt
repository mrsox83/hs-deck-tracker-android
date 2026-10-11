package com.stroexd.hsdecktracker.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stroexd.hsdecktracker.AppContainer
import com.stroexd.hsdecktracker.core.fusion.FusionImportState
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.logs.PostMatchLogClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Explicit foreground-only manual pilot. Closing/backgrounding cancels collection and unbinds. */
@Composable
fun PostMatchLogsDialog(container: AppContainer, match: MatchRecord, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val client = remember { PostMatchLogClient(context) }
    val scope = rememberCoroutineScope()
    val availability by client.availability.collectAsStateWithLifecycle()
    val readerError by client.lastError.collectAsStateWithLifecycle()
    val collections by container.postMatchCollections.entries.collectAsStateWithLifecycle()
    var sessions by remember { mutableStateOf(emptyList<String>()) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var busy by remember { mutableStateOf(false) }
    var confirmedFinished by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("No collection started.") }
    DisposableEffect(client, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { client.close(); onDismiss() }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); client.close() }
    }
    fun runOperation(operation: suspend () -> String) {
        if (busy) return
        busy = true
        message = "Working; keep this screen open. Closing stops this attempt."
        scope.launch {
            try { message = operation() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { message = "Not completed: ${error.message ?: error.javaClass.simpleName}. Retained raw logs, if any, remain local." }
            finally { busy = false }
        }
    }
    fun resultText(state: FusionImportState, errors: String?): String = when (state) {
        FusionImportState.FUSED_LOCAL -> "Fused locally. No cloud upload was performed."
        FusionImportState.PENDING_PAIRING -> "Raw logs saved locally; pairing is pending. ${errors.orEmpty()}"
        else -> "Raw logs saved locally; fusion did not complete. ${errors.orEmpty()}"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collect logs and fuse — manual pilot") },
        text = {
            Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("After a match, Shizuku reads only Hearthstone session Power.log files. Raw logs may contain account names. Copies stay in this app's private storage; Hearthstone originals are never changed. No screen capture or upload is started.")
                Row {
                    Checkbox(confirmedFinished, { confirmedFinished = it }, enabled = !busy)
                    Text("I have finished the match and stopped active tracking.")
                }
                Text("Reader: ${availability.name.lowercase().replace('_', ' ')}")
                readerError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (availability == PostMatchLogClient.Availability.STOPPED) Text("Start Shizuku first, then return here. The companion exporter remains available as fallback.")
                if (availability == PostMatchLogClient.Availability.NEEDS_PERMISSION) {
                    TextButton(enabled = !busy, onClick = {
                        try { client.requestPermission() } catch (error: Exception) { message = error.message.orEmpty() }
                    }) { Text("Authorize restricted reader") }
                }
                TextButton(enabled = !busy && confirmedFinished && availability in listOf(PostMatchLogClient.Availability.READY, PostMatchLogClient.Availability.CONNECTED), onClick = {
                    runOperation { client.connect(); sessions = client.listSessions(); selected = emptySet(); "Select one session, or two for reconnect. Nothing is selected automatically." }
                }) { Text("Connect / refresh sessions") }
                sessions.forEach { session ->
                    Row {
                        Checkbox(session in selected, { checked -> selected = if (checked) selected + session else selected - session },
                            enabled = !busy && (session in selected || selected.size < 2))
                        Text(session.removePrefix("Hearthstone_").replace('_', ' '))
                    }
                }
                Button(enabled = !busy && confirmedFinished && selected.isNotEmpty() && availability == PostMatchLogClient.Availability.CONNECTED, onClick = {
                    val chosen = selected.sorted()
                    runOperation {
                        val result = container.collectPostMatchLogs(match, chosen, client::snapshot)
                        resultText(result.entry.state, result.entry.lastError ?: result.entry.pairing?.reason)
                    }
                }) { Text("Collect selected logs and fuse") }
                collections.lastOrNull { it.matchId == match.id }?.let { retained ->
                    Text("Retained collection: ${retained.logs.size} raw log(s), ${retained.readFailures.size} read failure(s). ${retained.state.name.lowercase()}")
                    retained.readFailures.forEach { (session, error) -> Text("$session: $error") }
                    retained.error?.let { Text(it) }
                    TextButton(enabled = !busy && confirmedFinished && retained.logs.isNotEmpty(), onClick = {
                        runOperation {
                            val result = container.retryCollectedLogs(match, retained.id)
                            resultText(result.entry.state, result.entry.lastError ?: result.entry.pairing?.reason)
                        }
                    }) { Text("Retry fusion from retained logs (no Shizuku read)") }
                }
                Text(message)
                Text("Use Import fusion bundle for the companion-exporter fallback. Local retention is not Drive upload confirmation.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (busy) "Stop / close" else "Close") } },
    )
}

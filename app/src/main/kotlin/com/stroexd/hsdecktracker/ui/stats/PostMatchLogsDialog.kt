package com.stroexd.hsdecktracker.ui.stats

import androidx.compose.foundation.layout.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.stroexd.hsdecktracker.core.fusion.PostMatchPresentation
import com.stroexd.hsdecktracker.core.fusion.FusionImportState
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.logs.PostMatchLogClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.stroexd.hsdecktracker.overlay.OverlayLauncher
import com.stroexd.hsdecktracker.ui.toast
import java.io.File
import java.text.DateFormat
import java.util.Date

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
    val imports by container.fusionImports.entries.collectAsStateWithLifecycle()
    val fusedEntry = imports.firstOrNull { it.matchId == match.id && it.state == FusionImportState.FUSED_LOCAL }
    var sessions by remember { mutableStateOf(emptyList<String>()) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var busy by remember { mutableStateOf(false) }
    var confirmedFinished by remember { mutableStateOf(false) }
    var message by remember {
        val saved = container.fusionImports.entries.value.firstOrNull { it.matchId == match.id }
        mutableStateOf(if (saved != null)
            PostMatchPresentation.resultText(saved.state, saved.lastError ?: saved.pairing?.reason,
                saved.state == FusionImportState.FUSED_LOCAL, saved.coverageWarnings)
        else "No collection started.")
    }
    var pendingExport by remember { mutableStateOf<File?>(null) }
    var choosingExport by remember { mutableStateOf(false) }
    val saveEvidence = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        choosingExport = false
        val source = pendingExport
        pendingExport = null
        if (uri == null) {
            busy = false
            message = "Export cancelled. Private evidence remains saved locally; nothing was exported."
            context.toast("Export cancelled")
        } else if (source != null) {
            scope.launch {
                try {
                    val copied = withContext(Dispatchers.IO) {
                        val job = currentCoroutineContext()
                        val output = context.contentResolver.openOutputStream(uri, "w")
                            ?: error("Selected destination could not be opened")
                        output.use { destination ->
                            source.inputStream().use { input ->
                                val buffer = ByteArray(64 * 1024)
                                var count = 0L
                                while (true) {
                                    job.ensureActive()
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    destination.write(buffer, 0, read)
                                    count += read
                                }
                                destination.flush()
                                count
                            }
                        }
                    }
                    message = "Export written to selected provider: $copied bytes. Drive sync/cloud arrival is not verified. Private originals retained. Close/reopen to reconnect the reader."
                    context.toast("Fusion evidence written to selected destination")
                } catch (error: CancellationException) { throw error
                } catch (error: Exception) {
                    message = "Export not completed: ${error.message}. Destination may be partial; private originals retained."
                    context.toast("Fusion evidence export failed; see dialog")
                } finally { busy = false }
            }
        } else { busy = false; message = "Export not completed: source unavailable. Private originals retained." }
    }
    fun exportEvidence(prepare: suspend () -> File) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                pendingExport = prepare()
                choosingExport = true
                saveEvidence.launch("fusion-test-${match.id}-${pendingExport!!.name}")
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) {
                choosingExport = false
                pendingExport = null
                busy = false
                message = "Export not started: ${error.message}. Private originals retained."
            }
        }
    }
    DisposableEffect(client, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { client.close(); if (!choosingExport) onDismiss() }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collect logs and fuse — manual pilot") },
        text = {
            Column(Modifier.heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Status", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(message, color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("After a match, Shizuku reads only Hearthstone session Power.log files. Raw logs may contain account names. Copies stay in this app's private storage; Hearthstone originals are never changed. No screen capture or upload is started.")
                Text("Selected game: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(match.timestamp))} (phone local time)")
                Text("${match.playerClass} vs ${match.opponentClass} • ${match.result} • ${match.deckName}")
                Text("Session times below come from folder names; their timezone and game contents are not verified. Choose manually, not by a suggested match.")
                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
                    container.tracker.stop()
                    OverlayLauncher.stop(context)
                    message = "Tracking stop requested. Confirm the finished-match checkbox separately; no collection started."
                }) { Text("Stop tracking") }
                Row {
                    Checkbox(confirmedFinished, { confirmedFinished = it }, enabled = !busy)
                    Text("I have finished the match and stopped active tracking.")
                }
                Text("Reader: ${availability.name.lowercase().replace('_', ' ')}")
                readerError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (availability == PostMatchLogClient.Availability.STOPPED) Text("Start Shizuku first, then return here. The companion exporter remains available as fallback.")
                if (availability == PostMatchLogClient.Availability.CLOSED) Text("Reader closed while choosing the export destination. Close and reopen this dialog to connect again. Retained export/retry does not need a reader connection.")
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
                        Column {
                            Text(PostMatchPresentation.sessionLabel(session))
                            Text(session, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Button(modifier = Modifier.fillMaxWidth(), enabled = !busy && confirmedFinished && selected.isNotEmpty() && availability == PostMatchLogClient.Availability.CONNECTED, onClick = {
                    val chosen = selected.sorted()
                    runOperation {
                        val result = container.collectPostMatchLogs(match, chosen, client::snapshot)
                        PostMatchPresentation.resultText(result.entry.state, result.entry.lastError ?: result.entry.pairing?.reason,
                            coverageWarnings = result.entry.coverageWarnings)
                    }
                }) { Text("Collect selected logs and fuse") }
                collections.lastOrNull { it.matchId == match.id }?.let { retained ->
                    Text("Retained collection: ${retained.logs.size} raw log(s), ${retained.readFailures.size} read failure(s). ${retained.state.name.lowercase()}")
                    retained.readFailures.forEach { (session, error) -> Text("$session: $error") }
                    retained.error?.let { Text(it) }
                    Text("If fusion is pending or failed, export the logs ZIP for investigation. If fusion succeeded, skip Retry and export the fused JSON below.")
                    FilledTonalButton(modifier = Modifier.fillMaxWidth(), enabled = !busy && retained.logs.isNotEmpty(), onClick = {
                        exportEvidence { container.postMatchCollections.buildBundle(retained.id) }
                    }) { Text("Export saved logs (ZIP)") }
                    Text("Saves the retained logs and decoded evidence. This does not fuse them or confirm Drive arrival.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy && confirmedFinished && retained.logs.isNotEmpty() && fusedEntry == null, onClick = {
                        runOperation {
                            val result = container.retryCollectedLogs(match, retained.id)
                            PostMatchPresentation.resultText(result.entry.state, result.entry.lastError ?: result.entry.pairing?.reason,
                                PostMatchPresentation.EXISTING_ARTIFACT_PRESERVED in result.diagnostics,
                                result.entry.coverageWarnings)
                        }
                    }) { Text("Retry using saved logs") }
                    Text(if (fusedEntry != null) "Already fused: retry is unnecessary. Export the fused JSON below."
                        else "Recovery only: reuses the same logs without reading Hearthstone again. It cannot supply missing evidence or correct the selected sessions.", style = MaterialTheme.typography.bodySmall)
                }
                FilledTonalButton(modifier = Modifier.fillMaxWidth(), enabled = !busy && fusedEntry?.artifactFile != null, onClick = {
                    exportEvidence { withContext(Dispatchers.IO) { container.fusionImports.artifactForExport(match.id) } }
                }) { Text("Export fused game (JSON)") }
                Text(if (fusedEntry == null) "Unavailable until this game is successfully fused. Export the logs ZIP if pairing is pending."
                    else "Saves the existing fused result; review any partial-reconstruction warning. During validation, also export the logs ZIP.", style = MaterialTheme.typography.bodySmall)
                Text("Export uses Android's file picker: select Drive or a local folder. Evidence may contain account names. Coaching eligibility follows your instructions; diagnostic tests stay separate. Keep this dialog open until the result appears.")
                Text("Use Import fusion bundle for the companion-exporter fallback. Local retention is not Drive upload confirmation.")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (busy) "Stop / close" else "Close") } },
    )
}

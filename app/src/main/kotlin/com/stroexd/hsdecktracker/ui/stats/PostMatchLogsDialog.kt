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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stroexd.hsdecktracker.AppContainer
import com.stroexd.hsdecktracker.core.fusion.PostMatchPresentation
import com.stroexd.hsdecktracker.core.fusion.FusionImportState
import com.stroexd.hsdecktracker.core.fusion.PostMatchDiscoveryResult
import com.stroexd.hsdecktracker.core.fusion.PairingStatus
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
    val fusionConsistency = fusedEntry?.let { PostMatchPresentation.fusionConsistency(match, it) }
    var sessions by remember { mutableStateOf(emptyList<String>()) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var discovery by remember { mutableStateOf<PostMatchDiscoveryResult?>(null) }
    var broadSearch by remember { mutableStateOf<PostMatchDiscoveryResult?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }
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
    var showStatusDetails by remember(message) { mutableStateOf(false) }
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
    fun exportEvidence(suggestedName: String? = null, prepare: suspend () -> File) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                pendingExport = prepare()
                choosingExport = true
                saveEvidence.launch(suggestedName ?: "fusion-test-${match.id}-${pendingExport!!.name}")
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
        title = { Text("Game logs & export") },
        text = {
            Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Status", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(PostMatchPresentation.statusSummary(message), color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { showStatusDetails = !showStatusDetails }) {
                            Text(if (showStatusDetails) "Hide status details" else "Show status details")
                        }
                        if (showStatusDetails) Text(message,
                            modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState()),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Find logs → review → confirm fusion → export. Exports open Android's save picker; choose Drive there. Saving locally is not a cloud upload.")
                Text("Selected game: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(match.timestamp))} (phone local time)")
                Text("${match.playerClass} vs ${match.opponentClass} • ${match.result} • ${match.deckName}")
                if (fusedEntry != null) {
                    val conflicted = fusionConsistency?.consistency == PostMatchPresentation.FusionConsistency.RESULT_CONFLICT
                    Surface(
                        color = if (conflicted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(fusionConsistency?.title.orEmpty(), style = MaterialTheme.typography.titleSmall)
                            Text(fusionConsistency?.message.orEmpty(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(if (conflicted)
                        "Historical fusion remains available for evidence export. Do not treat it as the current saved result or retry blindly. Use the match-detail download action for the current revisioned match JSON."
                    else "Ready to export — fusion already saved. Do not search or retry again.",
                        style = MaterialTheme.typography.titleSmall)
                    Button(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
                        exportEvidence { withContext(Dispatchers.IO) { container.fusionImports.artifactForExport(match.id) } }
                    }) { Text(if (conflicted) "Save preserved historical fusion (JSON)" else "Save fused game (JSON) to Drive / folder") }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
                        val date = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(Date(match.timestamp))
                        exportEvidence("HS-game-$date-${match.id.take(8)}-logs.zip") { container.exportFusedGameLogs(match) }
                    }) { Text("Save this game's troubleshooting logs (ZIP)") }
                    Text("ZIP automatically uses this fused game's source hashes, not the newest session. It can include other games from those same sessions and account names. No new Shizuku read. Existing saved artifacts are not regenerated.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (fusedEntry == null) {
                Text("Search uses folder times only to order reads, not to prove a match. Evidence must support a unique candidate; manual session selection remains available.")
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
                Button(modifier = Modifier.fillMaxWidth(), enabled = !busy && confirmedFinished && fusedEntry == null &&
                    availability in listOf(PostMatchLogClient.Availability.READY, PostMatchLogClient.Availability.CONNECTED), onClick = {
                    val requestedSessions = selected.toList()
                    discovery = null
                    broadSearch = null
                    runOperation {
                        client.connect()
                        sessions = client.listSessions()
                        selected = emptySet()
                        val result = container.discoverPostMatchLogs(match,
                            requestedSessions.ifEmpty { sessions }, client::snapshotWithinBudget)
                        discovery = result
                        result.statusText()
                    }
                }) { Text(if (selected.isEmpty()) "Find matching logs" else "Find match in selected sessions") }
                Text("Search checks the most likely current session first. If no match is found, Search next 3 expands on request and reuses the saved evidence/results. Total caps: 4 sessions / 64 MiB raw / 24 matches; 60-second cooperative deadline per batch. Closing cancels; slow system reads may take time to return.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Only inspected sessions support a recommendation, not global uniqueness. If unreadable logs or timeouts block confirmation, review candidate sessions and search an explicitly selected scope.",
                    style = MaterialTheme.typography.bodySmall)
                discovery?.let { result ->
                    broadSearch?.let { original ->
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy && fusedEntry == null, onClick = {
                            discovery = original
                            broadSearch = null
                            message = original.statusText()
                        }) { Text("Back to all search candidates") }
                    }
                    if (result.pendingSessions.isNotEmpty() && result.pairing.status != PairingStatus.ACCEPTED) {
                        FilledTonalButton(modifier = Modifier.fillMaxWidth(), enabled = !busy && confirmedFinished && fusedEntry == null &&
                            availability == PostMatchLogClient.Availability.CONNECTED, onClick = {
                            runOperation {
                                val expanded = container.discoverPostMatchLogs(match, sessions, client::snapshotWithinBudget, result)
                                discovery = expanded
                                expanded.statusText()
                            }
                        }) { Text("Search next ${minOf(3, result.pendingSessions.size)}") }
                    }
                    result.stopReasons.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                    Text("Search: ${result.pairing.status.name.lowercase()} — ${result.pairing.reason}")
                    result.candidates.forEach { candidate ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                Text(candidate.label)
                                Text((candidate.reasons + candidate.rejected.map { "Not supported: $it" }).joinToString("; "),
                                    style = MaterialTheme.typography.bodySmall)
                                if (candidate.sessions.size in 1..2) OutlinedButton(modifier = Modifier.fillMaxWidth(),
                                    enabled = !busy && confirmedFinished && fusedEntry == null,
                                    onClick = {
                                        runOperation {
                                            val reviewed = container.reviewDiscoveredCandidate(match, result, candidate.sourceId)
                                            if (broadSearch == null) broadSearch = result
                                            discovery = reviewed
                                            selected = emptySet()
                                            reviewed.statusText()
                                        }
                                    }) {
                                    Text("Review this game's saved logs")
                                }
                                Text("Reviews all games in this candidate's saved sessions only; other unreadable/unsearched sessions remain excluded. No new reader connection. Confirm fusion separately.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Button(modifier = Modifier.fillMaxWidth(), enabled = !busy && confirmedFinished && fusedEntry == null &&
                        result.pairing.status == PairingStatus.ACCEPTED, onClick = {
                        runOperation {
                            val fused = container.confirmDiscoveredMatch(match, result)
                            PostMatchPresentation.resultText(fused.entry.state, fused.entry.lastError ?: fused.entry.pairing?.reason,
                                PostMatchPresentation.EXISTING_ARTIFACT_PRESERVED in fused.diagnostics, fused.entry.coverageWarnings)
                        }
                    }) { Text("Confirm candidate and fuse saved logs") }
                    Text("Confirmation rechecks the saved evidence without a new Shizuku read. Incomplete/ambiguous searches cannot be confirmed; use manual review.", style = MaterialTheme.typography.bodySmall)
                    result.bundles.forEachIndexed { index, bundle ->
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
                            exportEvidence { container.postMatchCollections.buildBundle(bundle.collectionId) }
                        }) {
                            val names = collections.firstOrNull { it.id == bundle.collectionId }?.logs.orEmpty()
                                .joinToString(" + ") { PostMatchPresentation.sessionLabel(it.session) }
                            Text("Save logs ZIP — ${names.ifEmpty { "searched session ${index + 1}" }}")
                        }
                    }
                }
                }
                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) "Hide advanced recovery" else "Advanced: choose sessions / retry saved logs")
                }
                if (showAdvanced) {
                Text("Manual recovery only. Session start time is not a game identity. Select one session, or two for a reconnect; raw logs may include account names. Existing Hearthstone files are never changed.")
                if (fusedEntry != null) {
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
                        container.tracker.stop()
                        OverlayLauncher.stop(context)
                        message = "Tracking stop requested. Confirm the finished-match checkbox separately."
                    }) { Text("Stop tracking before additional reads") }
                    Row {
                        Checkbox(confirmedFinished, { confirmedFinished = it }, enabled = !busy)
                        Text("I have finished the match and stopped active tracking.")
                    }
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
                    Text("Advanced recovery exports the last collection, which may not be this game's accepted source. Prefer this game's troubleshooting ZIP above after fusion.")
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
                    Text(if (fusedEntry != null) "Already fused: retry is unnecessary. Use the export buttons above."
                        else "Recovery only: reuses the same logs without reading Hearthstone again. It cannot supply missing evidence or correct the selected sessions.", style = MaterialTheme.typography.bodySmall)
                }
                }
                Text("Export uses Android's file picker: select Drive or a local folder. Evidence may contain account names. Coaching eligibility follows your instructions; diagnostic tests stay separate. Keep this dialog open until the result appears.")
                Text("Use Import fusion bundle for the companion-exporter fallback. Local retention is not Drive upload confirmation.")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (busy) "Stop / close" else "Close") } },
    )
}

package com.stroexd.hsdecktracker.ui.stats

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stroexd.hsdecktracker.AppContainer
import com.stroexd.hsdecktracker.R
import com.stroexd.hsdecktracker.core.fusion.*
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import kotlinx.coroutines.*
import java.io.File

/** In-memory pinned session survives configuration changes, never persists report contents to saved state. */
class PlayerHandDialogModel : ViewModel() {
    var artifact by mutableStateOf<PlayerHandExport.Artifact?>(null); private set
    var checkpoints by mutableStateOf(emptyList<PlayerHandExport.Checkpoint>()); private set
    var checkpoint by mutableStateOf<String?>(null); private set
    var side by mutableStateOf(""); private set
    var payload by mutableStateOf<PlayerHandExport.Payload?>(null); private set
    var message by mutableStateOf(""); private set
    var busy by mutableStateOf(false); private set
    var choosing by mutableStateOf(false); private set
    var consistency by mutableStateOf(""); private set
    private var job: Job? = null
    private var session = 0

    fun load(source: () -> File, notice: String) {
        if (artifact != null || busy) return
        busy=true; message=""
        val currentSession=session
        job=viewModelScope.launch {
            try {
                val loaded=withContext(Dispatchers.IO) {
                    val ctx=currentCoroutineContext()
                    val file=source()
                    require(file.length() in 1..PlayerHandExport.MAX_INPUT_BYTES) { "This report supports fusion files up to 32 MiB. Use the existing fusion export for larger originals." }
                    file.inputStream().use { PlayerHandExport.load(it) { ctx.ensureActive() } }
                }
                val choices=withContext(Dispatchers.Default) { PlayerHandExport.checkpoints(loaded) }
                if(currentSession==session) { artifact=loaded; checkpoints=choices; consistency=notice }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if(currentSession==session) message=error.message ?: "Unable to load saved fusion" }
            finally { if(currentSession==session) busy=false }
        }
    }
    fun select(id: String) { if (!busy && !choosing) { checkpoint=id; payload=null; message="" } }
    fun selectSide(value: String) { if (!busy && !choosing) { side=value.take(10); payload=null; message="" } }
    fun generate() {
        val loaded=artifact ?: return
        val point=checkpoint ?: return
        val controller=side.toIntOrNull()?.takeIf { it>0 } ?: return
        if (busy || choosing) return
        busy=true; payload=null; message=""
        val currentSession=session
        job=viewModelScope.launch {
            try {
                val prepared=withContext(Dispatchers.Default) {
                    val ctx=currentCoroutineContext()
                    PlayerHandExport.prepare(loaded,point,controller) { ctx.ensureActive() }
                }
                if(currentSession==session) payload=prepared
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if(currentSession==session) message=error.message ?: "Unable to prepare report" }
            finally { if(currentSession==session) busy=false }
        }
    }
    fun launchSave(): String? {
        if (busy || choosing) return null
        val name=payload?.filename ?: return null
        choosing=true
        return name
    }
    fun pickerFailed(error: Exception) { choosing=false; message=error.message ?: "Save picker unavailable" }
    fun save(context: Context, uri: Uri?, cancelled: String, saved: String, failed: String, missing: String) {
        val expectedResult=choosing
        choosing=false
        if (uri==null) { message=cancelled; return }
        val prepared=payload
        if (!expectedResult || prepared==null || busy) { message=missing; return }
        busy=true
        val currentSession=session
        job=viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val ctx=currentCoroutineContext()
                    ctx.ensureActive()
                    context.contentResolver.openOutputStream(uri,"w")?.use { output ->
                        val bytes=prepared.text.toByteArray(Charsets.UTF_8)
                        var offset=0
                        while(offset<bytes.size) {
                            ctx.ensureActive(); val size=minOf(8192,bytes.size-offset)
                            output.write(bytes,offset,size); offset+=size
                        }
                        ctx.ensureActive(); output.flush()
                    } ?: error("Selected destination could not be opened")
                }
                if(currentSession==session) message=saved
            } catch(error: CancellationException) { if(currentSession==session) message=failed; throw error }
            catch(error: Exception) { if(currentSession==session) message="$failed\n${error.message.orEmpty()}" }
            finally { if(currentSession==session) busy=false }
        }
    }
    fun close() {
        session++
        job?.cancel(); job=null; artifact=null; checkpoints=emptyList(); checkpoint=null
        side=""; payload=null; message=""; busy=false; choosing=false; consistency=""
    }
}

private fun Context.playerHandActivity(): Activity? = when(this) {
    is Activity -> this
    is ContextWrapper -> if(baseContext !== this) baseContext.playerHandActivity() else null
    else -> null
}

@Composable
fun PlayerHandDialog(container: AppContainer, match: MatchRecord, onDismiss: () -> Unit) {
    val context=LocalContext.current
    val model: PlayerHandDialogModel=viewModel(key="player-hand-${match.id}")
    val cancelled=stringResource(R.string.player_hand_cancelled)
    val saved=stringResource(R.string.player_hand_saved)
    val failed=stringResource(R.string.player_hand_failed)
    val missing=stringResource(R.string.player_hand_missing_payload)
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        model.save(context.applicationContext,uri,cancelled,saved,failed,missing)
    }
    LaunchedEffect(match.id) {
        val entry=container.fusionImports.entries.value.firstOrNull { it.matchId==match.id && it.state==FusionImportState.FUSED_LOCAL }
        model.load({ container.fusionImports.artifactForExport(match.id) },
            entry?.let { PostMatchPresentation.fusionConsistency(match,it).message }.orEmpty())
    }
    DisposableEffect(model) {
        onDispose { if (context.playerHandActivity()?.isChangingConfigurations != true) model.close() }
    }
    var filter by remember { mutableStateOf("") }
    var details by remember { mutableStateOf(false) }
    fun dismiss() { if (!model.choosing) { model.close(); onDismiss() } }
    AlertDialog(onDismissRequest=::dismiss, title={ Text(stringResource(R.string.player_hand_title)) },
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=540.dp).verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.player_hand_warning))
                if(model.consistency.isNotEmpty()) Text(model.consistency,style=MaterialTheme.typography.bodySmall)
                if(model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedTextField(filter,{ filter=it.take(120) },label={Text(stringResource(R.string.player_hand_filter))},enabled=!model.busy&&!model.choosing)
                val visible=model.checkpoints.filter { filter.isBlank() || it.label.contains(filter,true) || it.id.contains(filter,true) }
                Text(stringResource(R.string.player_hand_checkpoint))
                LazyColumn(Modifier.fillMaxWidth().height(150.dp)) {
                    itemsIndexed(visible.take(200)) { _, point ->
                        TextButton(onClick={ model.select(point.id) },enabled=point.unique&&!model.busy&&!model.choosing) {
                            Text((if(model.checkpoint==point.id) "✓ " else "")+point.label+"\n"+PlayerHandExport.displayLabel(point.id),maxLines=4)
                        }
                    }
                }
                if(visible.size>200) Text(stringResource(R.string.player_hand_refine))
                Text(stringResource(R.string.player_hand_side_help))
                OutlinedTextField(model.side,model::selectSide,label={Text(stringResource(R.string.player_hand_side))},enabled=!model.busy&&!model.choosing,singleLine=true)
                val sides=model.artifact?.match?.snapshots?.singleOrNull { it.id==model.checkpoint }?.entityTags?.values
                    ?.filter { it["ZONE"]=="HAND" }?.mapNotNull { it["CONTROLLER"]?.toIntOrNull()?.takeIf { n->n>0 } }?.distinct()?.sorted().orEmpty()
                if(sides.isNotEmpty()) Text(stringResource(R.string.player_hand_side_options,sides.joinToString(", ")),style=MaterialTheme.typography.bodySmall)
                Button(onClick=model::generate,enabled=model.checkpoint!=null&&(model.side.toIntOrNull() ?: 0)>0&&!model.busy&&!model.choosing) { Text(stringResource(R.string.player_hand_preview)) }
                model.payload?.let { prepared ->
                    Text(prepared.text,style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={ details=!details }) { Text(stringResource(R.string.player_hand_details)) }
                    if(details) Text("SHA256: ${model.artifact?.sha256}\n${prepared.filename}",style=MaterialTheme.typography.bodySmall)
                    Button(onClick={ model.launchSave()?.let { name -> try { launcher.launch(name) } catch(error:Exception) { model.pickerFailed(error) } } },enabled=!model.busy&&!model.choosing) { Text(stringResource(R.string.player_hand_save)) }
                }
                if(model.message.isNotEmpty()) Text(model.message,style=MaterialTheme.typography.bodySmall)
            }
        }, confirmButton={ TextButton(onClick=::dismiss,enabled=!model.choosing) { Text(stringResource(R.string.close)) } })
}

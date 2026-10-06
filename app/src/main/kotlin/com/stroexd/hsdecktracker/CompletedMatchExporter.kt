package com.stroexd.hsdecktracker

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.stroexd.hsdecktracker.core.stats.MatchJsonExport
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Writes only to a user-granted document tree; no legacy storage permissions. */
class CompletedMatchExporter(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val mutex = Mutex()

    suspend fun export(record: MatchRecord, folder: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val tree = Uri.parse(folder)
            val parentId = DocumentsContract.getTreeDocumentId(tree)
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, parentId)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
            val filename = MatchJsonExport.filename(record)
            val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val cursor = resolver.query(children, projection, null, null, null)
                ?: error("Cannot read export folder")
            val pending = mutableListOf<Uri>()
            var alreadyExported = false
            cursor.use {
                while (it.moveToNext()) {
                    if (it.getString(0) == filename) alreadyExported = true
                    if (it.getString(0) == "$filename.part") {
                        pending += DocumentsContract.buildDocumentUriUsingTree(tree, it.getString(1))
                    }
                }
            }
            // A process interruption can leave our incomplete staging document behind.
            pending.forEach { check(DocumentsContract.deleteDocument(resolver, it)) }
            if (alreadyExported) return@withLock
            val document = DocumentsContract.createDocument(resolver, parent, "application/octet-stream", "$filename.part")
                ?: error("Cannot create match export")
            try {
                val stream = resolver.openOutputStream(document, "w") ?: error("Cannot write match export")
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(MatchJsonExport.encode(record)) }
                checkNotNull(DocumentsContract.renameDocument(resolver, document, filename)) { "Cannot finalize match export" }
            } catch (e: Exception) {
                // Remove only the document created by this attempt, never existing exports.
                runCatching { DocumentsContract.deleteDocument(resolver, document) }
                throw e
            }
        }
    }
}


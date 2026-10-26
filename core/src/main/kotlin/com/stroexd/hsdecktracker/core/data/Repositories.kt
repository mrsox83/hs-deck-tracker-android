package com.stroexd.hsdecktracker.core.data

import com.stroexd.hsdecktracker.core.cards.CardDatabase
import com.stroexd.hsdecktracker.core.cards.FormatRules
import com.stroexd.hsdecktracker.core.cards.HsClass
import com.stroexd.hsdecktracker.core.collection.CardCollection
import com.stroexd.hsdecktracker.core.collection.CollectionChange
import com.stroexd.hsdecktracker.core.collection.CollectionImportResult
import com.stroexd.hsdecktracker.core.collection.OwnedCard
import com.stroexd.hsdecktracker.core.collection.withScannedCopies
import com.stroexd.hsdecktracker.core.deck.Deck
import com.stroexd.hsdecktracker.core.deck.DeckTextParser
import com.stroexd.hsdecktracker.core.stats.MatchRecord
import com.stroexd.hsdecktracker.core.tracker.TrackerState
import com.stroexd.hsdecktracker.core.util.PrettyJson
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DeckRepository(dir: File, private val clock: () -> Long = System::currentTimeMillis) {
    private val store = JsonFileStore(File(dir, "decks.json"), ListSerializer(Deck.serializer()), emptyList())

    val decks: StateFlow<List<Deck>> = store.state

    fun get(id: String): Deck? = store.value.firstOrNull { it.id == id }

    suspend fun upsert(deck: Deck): Deck {
        val now = clock()
        val saved = deck.copy(updatedAt = now, createdAt = if (deck.createdAt == 0L) now else deck.createdAt)
        store.update { list ->
            if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved
        }
        return saved
    }

    suspend fun delete(id: String) {
        store.update { list -> list.filterNot { it.id == id } }
    }

    suspend fun importFromText(
        text: String,
        db: CardDatabase,
        defaultName: (HsClass) -> String = { "${it.englishName} Deck" },
    ): List<Deck> {
        val now = clock()
        val decks = DeckTextParser.parseAll(text).map { parsed ->
            Deck.fromDefinition(parsed.definition, parsed.name, db, now, defaultName = defaultName)
        }
        if (decks.isNotEmpty()) store.update { it + decks }
        return decks
    }

    suspend fun replaceAll(decks: List<Deck>) {
        store.set(decks)
    }
}

/** The entries before a change (null: not owned) and the dust it added. */
data class CollectionUndo(val cards: Map<Int, OwnedCard?>, val dust: Int)

class CollectionRepository(dir: File, private val clock: () -> Long = System::currentTimeMillis) {
    private val store = JsonFileStore(File(dir, "collection.json"), CardCollection.serializer(), CardCollection())

    val collection: StateFlow<CardCollection> = store.state

    suspend fun applyImport(result: CollectionImportResult, keepDustIfMissing: Boolean = true) {
        store.update { current ->
            result.collection.copy(
                dust = if (result.dust == null && keepDustIfMissing) current.dust else result.collection.dust,
                updatedAt = clock(),
            )
        }
    }

    suspend fun setNormalCount(dbfId: Int, count: Int) {
        store.update { it.withNormalCount(dbfId, count).copy(updatedAt = clock()) }
    }

    suspend fun setNormalCounts(counts: Map<Int, Int>) {
        store.update { current ->
            counts.entries.fold(current) { acc, (id, count) -> acc.withNormalCount(id, count) }.copy(updatedAt = clock())
        }
    }

    /** Applies a change computed from the current collection and returns what it takes to undo it. */
    suspend fun apply(compute: (CardCollection) -> CollectionChange): Pair<CollectionChange, CollectionUndo> {
        lateinit var result: Pair<CollectionChange, CollectionUndo>
        store.update { current ->
            val change = compute(current)
            val ids = change.cards.mapTo(HashSet()) { it.dbfId }
            result = change to CollectionUndo(ids.associateWith { current.cards[it] }, change.collection.dust - current.dust)
            change.collection.copy(updatedAt = clock())
        }
        return result
    }

    suspend fun undo(undo: CollectionUndo) {
        store.update { current ->
            val cards = current.cards.toMutableMap()
            undo.cards.forEach { (id, owned) -> if (owned == null) cards -= id else cards[id] = owned }
            current.copy(cards = cards, dust = (current.dust - undo.dust).coerceAtLeast(0), updatedAt = clock())
        }
    }

    /** Returns how many cards changed. */
    suspend fun applyScan(totals: Map<List<Int>, Int>, db: CardDatabase, rules: FormatRules): Int {
        var changed = 0
        store.update { current ->
            val next = current.withScannedCopies(totals, db, rules)
            changed = (current.cards.keys + next.cards.keys).count { current.owned(it) != next.owned(it) }
            next.copy(updatedAt = clock(), source = "Hearthstone")
        }
        return changed
    }

    suspend fun setDust(dust: Int) {
        store.update { it.copy(dust = dust.coerceAtLeast(0)) }
    }

    suspend fun addDeck(cards: Map<Int, Int>) {
        store.update { current ->
            cards.entries.fold(current) { acc, (id, count) -> acc.ensureAtLeast(id, count) }.copy(updatedAt = clock())
        }
    }

    suspend fun replace(collection: CardCollection) {
        store.set(collection)
    }

    suspend fun clear() {
        store.update { CardCollection(dust = it.dust) }
    }
}

/** Equal revisions must describe the same saved truth; an older retry is never a new edit. */
private fun newestMatchRecord(vararg records: MatchRecord?): MatchRecord {
    val available = records.filterNotNull()
    require(available.isNotEmpty() && available.map { it.id }.distinct().size == 1) {
        "Match reconciliation requires one saved identity"
    }
    val revision = available.maxOf { it.revision }
    val newest = available.filter { it.revision == revision }
    require(newest.distinct().size == 1) { "Conflicting match content at revision $revision" }
    return newest.first()
}

class MatchRepository(dir: File) {
    private val store = JsonFileStore(File(dir, "matches.json"), ListSerializer(MatchRecord.serializer()), emptyList())

    val matches: StateFlow<List<MatchRecord>> = store.state

    fun contains(id: String): Boolean = store.value.any { it.id == id }

    fun get(id: String): MatchRecord? = store.value.firstOrNull { it.id == id }

    suspend fun add(record: MatchRecord) {
        store.update { it + record }
    }

    suspend fun addIfAbsent(record: MatchRecord): Boolean {
        var added = false
        store.update { records ->
            val existing = records.firstOrNull { it.id == record.id }
            require(existing == null || existing == record) { "Match id ${record.id} already has different content" }
            if (existing == null) {
                added = true
                records + record
            } else records
        }
        return added
    }

    suspend fun delete(id: String) {
        store.update { list -> list.filterNot { it.id == id } }
    }

    suspend fun update(record: MatchRecord) {
        store.update { list -> list.map { if (it.id == record.id) newestMatchRecord(record, it) else it } }
    }

    suspend fun upsert(record: MatchRecord) {
        upsertLatest(record)
    }

    /** Resolve inside the store lock so an intervening UI edit cannot be overwritten by recovery. */
    internal suspend fun upsertLatest(record: MatchRecord): MatchRecord {
        var selected = record
        store.update { list ->
            val existing = list.firstOrNull { it.id == record.id }
            selected = newestMatchRecord(record, existing)
            if (existing != null) list.map { if (it.id == record.id) selected else it }
            else list + selected
        }
        return selected
    }

    suspend fun replaceAll(matches: List<MatchRecord>) {
        store.set(matches)
    }
}

@Serializable
enum class MatchOutboxState {
    STAGED,
    LOCALLY_COMMITTED,
    TRANSFER_PENDING,
    LOCALLY_EXPORTED,
    TRANSFER_FAILED,
    REMOTELY_VERIFIED,
}

@Serializable
data class MatchOutboxEntry(
    val match: MatchRecord,
    val state: MatchOutboxState,
    val attempts: Int = 0,
    val lastError: String? = null,
    val updatedAt: Long,
)

class MatchOutboxRepository(dir: File, private val clock: () -> Long = System::currentTimeMillis) {
    private val store = JsonFileStore(
        File(dir, "match-outbox.json"),
        ListSerializer(MatchOutboxEntry.serializer()),
        emptyList(),
    )

    val entries: StateFlow<List<MatchOutboxEntry>> = store.state

    fun contains(id: String): Boolean = store.value.any { it.match.id == id }

    internal fun get(id: String): MatchOutboxEntry? = store.value.firstOrNull { it.match.id == id }

    suspend fun stage(record: MatchRecord): MatchOutboxEntry {
        var result: MatchOutboxEntry? = null
        store.update { entries ->
            val existing = entries.firstOrNull { it.match.id == record.id }
            val latest = newestMatchRecord(record, existing?.match)
            if (existing?.match == latest) {
                result = existing
                entries
            } else if (existing != null) {
                val revised = MatchOutboxEntry(latest, MatchOutboxState.STAGED, updatedAt = clock())
                result = revised
                entries.map { if (it.match.id == record.id) revised else it }
            } else {
                MatchOutboxEntry(latest, MatchOutboxState.STAGED, updatedAt = clock()).also { entry ->
                    result = entry
                }.let(entries::plus)
            }
        }
        return checkNotNull(result)
    }

    suspend fun transition(id: String, state: MatchOutboxState, error: String? = null): MatchOutboxEntry {
        var result: MatchOutboxEntry? = null
        store.update { entries -> entries.map { entry ->
            if (entry.match.id != id) entry else entry.copy(
                state = state,
                attempts = entry.attempts + if (state == MatchOutboxState.TRANSFER_PENDING) 1 else 0,
                lastError = error,
                updatedAt = clock(),
            ).also { result = it }
        } }
        return requireNotNull(result) { "Unknown outbox match $id" }
    }

    fun pending(): List<MatchOutboxEntry> = entries.value.filter {
        it.state != MatchOutboxState.LOCALLY_EXPORTED && it.state != MatchOutboxState.REMOTELY_VERIFIED
    }
}

enum class MatchTransferResult { WRITTEN, ALREADY_PRESENT }

data class CompletedMatchCommitResult(
    val entry: MatchOutboxEntry,
    val transferResult: MatchTransferResult? = null,
)

class CompletedMatchCommitter(
    private val matches: MatchRepository,
    private val outbox: MatchOutboxRepository,
    private val export: suspend (MatchRecord) -> MatchTransferResult,
) {
    private val mutex = Mutex()

    suspend fun commit(record: MatchRecord, transferEnabled: Boolean): CompletedMatchCommitResult = mutex.withLock {
        val staged = stageLatest(record)
        if (!transferEnabled && (staged.state == MatchOutboxState.LOCALLY_EXPORTED || staged.state == MatchOutboxState.REMOTELY_VERIFIED)) {
            return@withLock CompletedMatchCommitResult(staged, MatchTransferResult.ALREADY_PRESENT)
        }
        outbox.transition(record.id, MatchOutboxState.LOCALLY_COMMITTED).let { committed ->
            if (!transferEnabled) CompletedMatchCommitResult(committed) else transfer(committed)
        }
    }

    suspend fun recover(transferEnabled: Boolean): List<CompletedMatchCommitResult> = mutex.withLock {
        outbox.pending().map { entry ->
            val staged = stageLatest(entry.match)
            if (staged.state == MatchOutboxState.LOCALLY_EXPORTED || staged.state == MatchOutboxState.REMOTELY_VERIFIED) {
                CompletedMatchCommitResult(staged, MatchTransferResult.ALREADY_PRESENT)
            } else {
                val committed = outbox.transition(entry.match.id, MatchOutboxState.LOCALLY_COMMITTED)
                if (!transferEnabled) CompletedMatchCommitResult(committed) else transfer(committed)
            }
        }
    }

    private suspend fun stageLatest(record: MatchRecord): MatchOutboxEntry {
        // Check conflicts before staging, retain stage-before-history crash recovery, then resolve
        // history again atomically in case the UI saved a newer revision during that write.
        val latest = newestMatchRecord(record, matches.get(record.id), outbox.get(record.id)?.match)
        val staged = outbox.stage(latest)
        val saved = matches.upsertLatest(staged.match)
        return outbox.stage(saved)
    }

    private suspend fun transfer(entry: MatchOutboxEntry): CompletedMatchCommitResult {
        outbox.transition(entry.match.id, MatchOutboxState.TRANSFER_PENDING)
        return try {
            val result = export(entry.match)
            CompletedMatchCommitResult(
                outbox.transition(entry.match.id, MatchOutboxState.LOCALLY_EXPORTED),
                result,
            )
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            CompletedMatchCommitResult(
                outbox.transition(
                    entry.match.id,
                    MatchOutboxState.TRANSFER_FAILED,
                    error.message ?: error::class.simpleName ?: "Unknown transfer failure",
                ),
            )
        }
    }
}

@Serializable
data class ActiveMatchJournal(val active: TrackerState? = null)

class ActiveMatchJournalRepository(dir: File) {
    private val store = JsonFileStore(
        File(dir, "active-match.json"),
        ActiveMatchJournal.serializer(),
        ActiveMatchJournal(),
    )

    val active: TrackerState? get() = store.value.active

    suspend fun save(state: TrackerState) {
        require(state.draftActive) { "Inactive tracker state cannot be journaled" }
        store.set(ActiveMatchJournal(state))
    }

    suspend fun clear(draftId: String) {
        store.update { current ->
            if (current.active?.draftId == draftId) ActiveMatchJournal() else current
        }
    }
}

class SettingsRepository(dir: File) {
    private val store = JsonFileStore(File(dir, "settings.json"), AppSettings.serializer(), AppSettings())

    val settings: StateFlow<AppSettings> = store.state
    val value: AppSettings get() = store.value

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.update(transform)
    }
}

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = 0,
    val decks: List<Deck> = emptyList(),
    val collection: CardCollection = CardCollection(),
    val matches: List<MatchRecord> = emptyList(),
    val settings: AppSettings = AppSettings(),
)

object Backup {
    fun export(data: BackupData): String = PrettyJson.encodeToString(BackupData.serializer(), data)
    fun import(json: String): BackupData = PrettyJson.decodeFromString(BackupData.serializer(), json)
}

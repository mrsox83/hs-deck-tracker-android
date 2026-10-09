package com.stroexd.hsdecktracker.core.fusion

object FusionSnapshotMaterializer {
    /** Lazily reconstructs full snapshot state; the returned sequence is single-use. */
    fun materialize(snapshots: Iterable<FusionSnapshot>): Sequence<FusionSnapshot> = sequence {
        var entityTags = emptyMap<String, Map<String, String>>()
        var counters = emptyMap<String, Map<String, Claim<Int>>>()
        var players = emptyMap<Int, PlayerSnapshot>()
        var quests = emptyMap<String, QuestSnapshot>()

        for (snapshot in snapshots) {
            if (snapshot.stateMode == SnapshotStateMode.FULL) {
                entityTags = snapshot.entityTags
                counters = snapshot.counters
                players = snapshot.players
                quests = snapshot.quests
            } else {
                entityTags = mergeNested(entityTags, snapshot.entityTags)
                counters = mergeNested(counters, snapshot.counters)
                players = players + snapshot.players
                quests = quests + snapshot.quests
            }
            yield(
                snapshot.copy(
                    stateMode = SnapshotStateMode.FULL,
                    entityTags = entityTags,
                    counters = counters,
                    players = players,
                    quests = quests,
                ),
            )
        }
    }

    private fun <K1, K2, V> mergeNested(
        state: Map<K1, Map<K2, V>>,
        changes: Map<K1, Map<K2, V>>,
    ): Map<K1, Map<K2, V>> {
        if (changes.isEmpty()) return state
        val merged = state.toMutableMap()
        changes.forEach { (outerKey, values) -> merged[outerKey] = merged[outerKey].orEmpty() + values }
        return merged
    }
}

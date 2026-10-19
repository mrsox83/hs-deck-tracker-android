package com.stroexd.hsdecktracker.core.fusion

/** Resolves only identities observed in the requested source and epoch by that source line. */
object HistoricalIdentity {
    fun cardIdAt(match: FusedMatch, entityId: String, epoch: Int, sourceId: String, sourceLine: Int): String? {
        val observations = match.entityEpochs.singleOrNull { it.chronologyEpoch == epoch }?.entities?.get(entityId)
            ?.identityHistory.orEmpty().filter {
                it.evidence.sourceId == sourceId && it.evidence.sourceLine?.let { line -> line <= sourceLine } == true
            }
        val latestLine = observations.maxOfOrNull { it.evidence.sourceLine!! } ?: return null
        return observations.filter { it.evidence.sourceLine == latestLine }.map { it.cardId }.distinct().singleOrNull()
    }
}

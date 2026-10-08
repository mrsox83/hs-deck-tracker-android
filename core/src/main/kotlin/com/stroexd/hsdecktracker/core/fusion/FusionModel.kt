package com.stroexd.hsdecktracker.core.fusion

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

const val FUSION_SCHEMA = "hs-fused-match/1"

@Serializable
enum class ClaimStatus { OBSERVED, DERIVED, INFERRED, CONFLICTED, UNKNOWN }

@Serializable
enum class Confidence { HIGH, MEDIUM, LOW }

@Serializable
data class EvidenceRef(
    val sourceId: String,
    val sourceLine: Int? = null,
    val jsonPointer: String? = null,
    val observedTime: String? = null,
    val rule: String? = null,
)

@Serializable
data class Claim<T>(
    val value: T? = null,
    val status: ClaimStatus = ClaimStatus.UNKNOWN,
    val confidence: Confidence = Confidence.LOW,
    val reason: String,
    val evidence: List<EvidenceRef> = emptyList(),
)

@Serializable
enum class SourceType { ANDROID_TRACKER_JSON, ANDROID_POWER_EVIDENCE, USER_ANNOTATION }

@Serializable
data class FusionSource(
    val id: String,
    val artifactSha256: String,
    val type: SourceType,
    val schema: String? = null,
    val captureMethod: String,
    val appBuild: String? = null,
    val sessionAlias: String? = null,
    val matchAlias: String? = null,
    val sourceLineStart: Int? = null,
    val sourceLineEnd: Int? = null,
    val complete: Boolean? = null,
    val parentSourceIds: List<String> = emptyList(),
    val contentSha256: Map<String, String> = emptyMap(),
    val artifactAliases: List<String> = emptyList(),
)

@Serializable
data class TagObservation(
    val sequence: Int,
    val sourceLine: Int,
    val logTime: String,
    val name: String,
    val value: String,
    val evidence: EvidenceRef,
)

@Serializable
data class IdentityObservation(
    val cardId: String,
    val evidence: EvidenceRef,
)

@Serializable
data class ValueObservation<T>(
    val value: T,
    val evidence: EvidenceRef,
)

@Serializable
data class EntityState(
    val id: String,
    val cardId: Claim<String> = Claim(reason = "Identity not observed"),
    val controller: Claim<Int> = Claim(reason = "Controller not observed"),
    val tags: Map<String, String> = emptyMap(),
    val tagHistory: List<TagObservation> = emptyList(),
    val identityHistory: List<IdentityObservation> = emptyList(),
    val zoneHistory: List<ValueObservation<String>> = emptyList(),
    val positionHistory: List<ValueObservation<Int>> = emptyList(),
    val controllerHistory: List<ValueObservation<Int>> = emptyList(),
    val visibilityHistory: List<ValueObservation<Boolean>> = emptyList(),
    val aliasHistory: List<ValueObservation<String>> = emptyList(),
    val entityLinks: Map<String, Claim<String>> = emptyMap(),
)

@Serializable
data class CanonicalEvent(
    val id: String,
    val sequence: Int,
    val sourceSequence: Int,
    val kind: String,
    val actorEntityId: String? = null,
    val targetEntityId: String? = null,
    val parentEventId: String? = null,
    val rawTurn: Int? = null,
    val activeController: Int? = null,
    val playerTurnIndex: Int? = null,
    val evidence: List<EvidenceRef>,
)

@Serializable
data class ChoiceStage(
    val entityRefs: List<String> = emptyList(),
    val observedTime: String? = null,
    val evidence: List<EvidenceRef> = emptyList(),
)

@Serializable
data class FusedChoice(
    val id: String,
    val type: String? = null,
    val sourceEntity: String? = null,
    val offered: ChoiceStage = ChoiceStage(),
    val submitted: ChoiceStage = ChoiceStage(),
    val confirmed: ChoiceStage = ChoiceStage(),
)

@Serializable
data class PlayerSnapshot(
    val controller: Int,
    val playerEntityId: String,
    val heroEntityId: Claim<String>,
    val health: Claim<Int> = Claim(reason = "HEALTH not observed"),
    val damage: Claim<Int> = Claim(reason = "DAMAGE not observed"),
    val remainingHealth: Claim<Int> = Claim(reason = "HEALTH and DAMAGE are both required"),
    val armor: Claim<Int> = Claim(reason = "ARMOR not observed"),
    val resources: Claim<Int> = Claim(reason = "RESOURCES not observed"),
    val resourcesUsed: Claim<Int> = Claim(reason = "RESOURCES_USED not observed"),
    val temporaryResources: Claim<Int> = Claim(reason = "TEMP_RESOURCES not observed"),
    val overloadOwed: Claim<Int> = Claim(reason = "OVERLOAD_OWED not observed"),
    val overloadLocked: Claim<Int> = Claim(reason = "OVERLOAD_LOCKED not observed"),
    val heraldAmount: Claim<Int> = Claim(reason = "HERALD_COLOSSAL_AMOUNT not observed"),
    val heraldClass: Claim<Int> = Claim(reason = "HERALD_COLOSSAL_CLASS not observed"),
    val heraldThresholdReached: Claim<Boolean> = Claim(reason = "No game-build-specific Herald threshold configured"),
)

@Serializable
data class QuestSnapshot(
    val entityId: String,
    val controller: Claim<Int>,
    val progress: Claim<Int> = Claim(reason = "QUEST_PROGRESS not observed"),
    val total: Claim<Int> = Claim(reason = "QUEST_PROGRESS_TOTAL not observed"),
    val completed: Claim<Boolean> = Claim(reason = "QUEST_COMPLETED not observed"),
    val rewardEntityId: Claim<String> = Claim(reason = "REWARD_ENTITY not observed"),
)

@Serializable
data class FusionSnapshot(
    val id: String,
    val sequence: Int,
    val phase: String,
    val activeController: Int? = null,
    val rawTurn: Int? = null,
    val playerTurnIndices: Map<Int, Int> = emptyMap(),
    val unresolvedBlock: Boolean = false,
    val entityTags: Map<String, Map<String, String>>,
    val counters: Map<String, Map<String, Claim<Int>>> = emptyMap(),
    val players: Map<Int, PlayerSnapshot> = emptyMap(),
    val quests: Map<String, QuestSnapshot> = emptyMap(),
    val evidence: List<EvidenceRef>,
)

@Serializable
data class ReducedPowerMatch(
    val source: FusionSource,
    val index: Int,
    val startedLogTime: String,
    val endedLogTime: String? = null,
    val endedEvidence: EvidenceRef? = null,
    val completed: Boolean,
    val sourceTruncated: Boolean,
    val events: List<CanonicalEvent>,
    val entities: Map<String, EntityState>,
    val choices: List<FusedChoice>,
    val snapshots: List<FusionSnapshot>,
    val diagnostics: List<String> = emptyList(),
)

@Serializable
enum class PairingStatus { ACCEPTED, PENDING, CONFLICTED, REJECTED_COMPOSITE }

@Serializable
data class PairingCandidate(
    val powerSourceId: String,
    val score: Int,
    val reasons: List<String>,
    val rejectedReasons: List<String> = emptyList(),
    val evidence: List<EvidenceRef> = emptyList(),
)

@Serializable
data class PairingDecision(
    val status: PairingStatus,
    val acceptedSourceId: String? = null,
    val candidates: List<PairingCandidate>,
    val reason: String,
)

@Serializable
data class FusedMatch(
    val schema: String = FUSION_SCHEMA,
    val matchId: String,
    val revision: Int = 1,
    val fusionVersion: String,
    val sources: List<FusionSource>,
    val pairing: PairingDecision,
    val events: List<CanonicalEvent>,
    val entities: Map<String, EntityState>,
    val choices: List<FusedChoice>,
    val snapshots: List<FusionSnapshot>,
    val unknownFields: Map<String, JsonElement> = emptyMap(),
    val diagnostics: List<String> = emptyList(),
)

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
data class EntityState(
    val id: String,
    val cardId: Claim<String> = Claim(reason = "Identity not observed"),
    val controller: Claim<Int> = Claim(reason = "Controller not observed"),
    val tags: Map<String, String> = emptyMap(),
    val tagHistory: List<TagObservation> = emptyList(),
    val identityHistory: List<IdentityObservation> = emptyList(),
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
data class FusionSnapshot(
    val id: String,
    val sequence: Int,
    val phase: String,
    val activeController: Int? = null,
    val rawTurn: Int? = null,
    val unresolvedBlock: Boolean = false,
    val entityTags: Map<String, Map<String, String>>,
    val counters: Map<String, Map<String, Claim<Int>>> = emptyMap(),
    val evidence: List<EvidenceRef>,
)

@Serializable
data class ReducedPowerMatch(
    val source: FusionSource,
    val index: Int,
    val startedLogTime: String,
    val endedLogTime: String? = null,
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

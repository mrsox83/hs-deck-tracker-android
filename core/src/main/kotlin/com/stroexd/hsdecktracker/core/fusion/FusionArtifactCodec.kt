package com.stroexd.hsdecktracker.core.fusion

import com.stroexd.hsdecktracker.core.util.AppJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

class FusionArtifactException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

object FusionArtifactCodec {
    /** Convenience API for bounded artifacts. Prefer [encodeToStream] for large match histories. */
    fun encode(match: FusedMatch): String {
        requireSupportedSchema(match.schema)
        requireValidProvenance(match)
        return AppJson.encodeToString(FusedMatch.serializer(), match)
    }

    /** Encodes directly to the destination without materializing the complete JSON as a String. */
    @OptIn(ExperimentalSerializationApi::class)
    fun encodeToStream(match: FusedMatch, output: OutputStream) {
        requireSupportedSchema(match.schema)
        requireValidProvenance(match)
        AppJson.encodeToStream(FusedMatch.serializer(), match, output)
    }

    fun decode(json: String): FusedMatch {
        val document = try {
            AppJson.parseToJsonElement(json).jsonObject
        } catch (error: Exception) {
            throw FusionArtifactException("Invalid fused artifact JSON", error)
        }
        val schemaElement = document["schema"] ?: throw FusionArtifactException("Fused artifact schema is required")
        val schema = try {
            val primitive = schemaElement.jsonPrimitive
            if (!primitive.isString) throw FusionArtifactException("Fused artifact schema must be a string")
            primitive.content
        } catch (error: Exception) {
            if (error is FusionArtifactException) throw error
            throw FusionArtifactException("Fused artifact schema must be a string", error)
        }
        requireSupportedSchema(schema)

        val match = try {
            AppJson.decodeFromJsonElement(FusedMatch.serializer(), document)
        } catch (error: Exception) {
            throw FusionArtifactException("Invalid $FUSION_SCHEMA artifact", error)
        }
        requireValidProvenance(match)
        return match
    }

    /** Decodes without first materializing the complete JSON input as a String. */
    @OptIn(ExperimentalSerializationApi::class)
    fun decodeFromStream(input: InputStream): FusedMatch {
        val match = try {
            AppJson.decodeFromStream(FusedMatch.serializer(), input)
        } catch (error: Exception) {
            throw FusionArtifactException("Invalid $FUSION_SCHEMA artifact", error)
        }
        requireSupportedSchema(match.schema)
        requireValidProvenance(match)
        return match
    }

    private fun requireSupportedSchema(schema: String) {
        if (schema != FUSION_SCHEMA) {
            throw FusionArtifactException("Unsupported fused artifact schema: $schema")
        }
    }

    private fun requireValidProvenance(match: FusedMatch) {
        val errors = FusionProvenanceValidator.validate(match)
        if (errors.isNotEmpty()) {
            throw FusionArtifactException("Invalid fused artifact provenance: ${errors.joinToString("; ")}")
        }
    }
}

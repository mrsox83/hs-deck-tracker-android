package com.stroexd.hsdecktracker.core.fusion

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object PowerClock {
    private val sessionDate = Regex(".*_(\\d{4})_(\\d{2})_(\\d{2})_(\\d{2})_(\\d{2})_(\\d{2})$")
    private val logTime = DateTimeFormatter.ofPattern("HH:mm:ss[.SSSSSSS]")

    fun resolve(sessionAlias: String, observedLogTime: String, zoneId: ZoneId): Instant? {
        val match = sessionDate.matchEntire(sessionAlias) ?: return null
        return try {
            val date = LocalDate.of(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
            val sessionStart = LocalTime.of(match.groupValues[4].toInt(), match.groupValues[5].toInt(), match.groupValues[6].toInt())
            val observed = LocalTime.parse(observedLogTime, logTime)
            val resolvedDate = if (observed.isBefore(sessionStart.minusHours(12))) date.plusDays(1) else date
            LocalDateTime.of(resolvedDate, observed).atZone(zoneId).toInstant()
        } catch (_: DateTimeParseException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }
}

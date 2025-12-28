package com.selfcontrol.data.remote.mapper

import com.selfcontrol.data.local.entity.ViolationEntity
import com.selfcontrol.data.remote.dto.ViolationDto
import com.selfcontrol.domain.model.Violation
import com.selfcontrol.domain.model.ViolationType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps between Violation models (API, Entity, and Domain)
 */
@Singleton
class ViolationMapper @Inject constructor() {
    
    /**
     * Convert API DTO to domain model
     */
    fun toDomain(dto: ViolationDto): Violation {
        return Violation(
            id = dto.id,
            appPackage = dto.packageName,
            type = parseType(dto.violationType),
            message = dto.details ?: "No message",
            timestamp = dto.timestamp,
            details = dto.details ?: "",
            synced = dto.synced
        )
    }
    
    /**
     * Convert local entity to domain model
     */
    fun toDomain(entity: ViolationEntity): Violation {
        return Violation(
            id = entity.id,
            appPackage = entity.appPackage,
            type = parseType(entity.violationType),
            message = entity.message,
            timestamp = entity.timestamp,
            details = entity.details ?: "",
            synced = entity.synced
        )
    }
    
    /**
     * Convert domain model to API DTO
     */
    fun toDto(domain: Violation, deviceId: String): ViolationDto {
        return ViolationDto(
            id = domain.id,
            deviceId = deviceId,
            packageName = domain.appPackage,
            appName = domain.appName,
            violationType = domain.type.name.lowercase(),
            timestamp = domain.timestamp,
            details = domain.details,
            synced = domain.synced
        )
    }
    
    /**
     * Convert domain model to local entity
     */
    fun toEntity(domain: Violation): ViolationEntity {
        return ViolationEntity(
            id = domain.id,
            appPackage = domain.appPackage,
            packageName = domain.appPackage,
            appName = domain.appName,
            violationType = domain.type.name.lowercase(),
            message = domain.message,
            timestamp = domain.timestamp,
            details = domain.details,
            synced = domain.synced
        )
    }
    
    /**
     * Convert list of DTOs to domain models
     */
    fun toDomainList(dtos: List<ViolationDto>): List<Violation> {
        return dtos.map { toDomain(it) }
    }
    
    /**
     * Convert list of domain models to DTOs
     */
    fun toDtoList(domains: List<Violation>, deviceId: String): List<ViolationDto> {
        return domains.map { toDto(it, deviceId) }
    }
    
    /**
     * Parse violation type string to enum
     */
    private fun parseType(type: String): ViolationType {
        return try {
            ViolationType.valueOf(type.uppercase().replace("-", "_"))
        } catch (e: Exception) {
            when (type.lowercase()) {
                "app_launch_attempt" -> ViolationType.APP_LAUNCH_ATTEMPT
                "url_access_attempt" -> ViolationType.URL_ACCESS_ATTEMPT
                "policy_bypass_attempt" -> ViolationType.POLICY_BYPASS_ATTEMPT
                "policy_enforcement_failed" -> ViolationType.POLICY_ENFORCEMENT_FAILED
                else -> ViolationType.UNKNOWN
            }
        }
    }
}

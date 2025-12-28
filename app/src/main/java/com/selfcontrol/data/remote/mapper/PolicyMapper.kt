package com.selfcontrol.data.remote.mapper

import com.selfcontrol.data.remote.dto.PolicyDto
import com.selfcontrol.domain.model.AppPolicy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps between PolicyDto (API) and AppPolicy (Domain)
 */
@Singleton
class PolicyMapper @Inject constructor() {
    
    /**
     * Convert API DTO to domain model
     */
    fun toDomain(dto: PolicyDto): AppPolicy {
        return AppPolicy(
            id = dto.id,
            packageName = dto.packageName,
            isBlocked = dto.isBlocked,
            isLocked = dto.isLocked,
            expiresAt = dto.expiresAt,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            reason = dto.reason ?: ""
        )
    }
    
    /**
     * Convert domain model to API DTO
     */
    fun toDto(domain: AppPolicy, deviceId: String): PolicyDto {
        return PolicyDto(
            id = domain.id,
            deviceId = deviceId,
            packageName = domain.packageName,
            isBlocked = domain.isBlocked,
            isLocked = domain.isLocked,
            expiresAt = domain.expiresAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            reason = domain.reason
        )
    }
    
    /**
     * Convert list of DTOs to domain models
     */
    fun toDomainList(dtos: List<PolicyDto>): List<AppPolicy> {
        return dtos.map { toDomain(it) }
    }
    
    /**
     * Convert list of domain models to DTOs
     */
    fun toDtoList(domains: List<AppPolicy>, deviceId: String): List<PolicyDto> {
        return domains.map { toDto(it, deviceId) }
    }
}

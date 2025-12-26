package com.selfcontrol.data.remote.mapper

import com.selfcontrol.data.remote.dto.RequestDto
import com.selfcontrol.domain.model.Request
import com.selfcontrol.domain.model.RequestStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps between RequestDto (API) and Request (Domain)
 */
@Singleton
class RequestMapper @Inject constructor() {
    
    /**
     * Convert API DTO to domain model
     */
    fun toDomain(dto: RequestDto): Request {
        return Request(
            id = dto.id,
            packageName = dto.packageName,
            appName = dto.appName,
            reason = dto.reason,
            status = parseStatus(dto.status),
            requestedAt = dto.requestedAt,
            reviewedAt = dto.reviewedAt,
            expiresAt = dto.expiresAt,
            reviewerNote = dto.reviewerNote
        )
    }
    
    /**
     * Convert domain model to API DTO
     */
    fun toDto(domain: Request, deviceId: String): RequestDto {
        return RequestDto(
            id = domain.id,
            deviceId = deviceId,
            packageName = domain.packageName,
            appName = domain.appName,
            reason = domain.reason,
            status = domain.status.name.lowercase(),
            requestedAt = domain.requestedAt,
            reviewedAt = domain.reviewedAt,
            expiresAt = domain.expiresAt,
            reviewerNote = domain.reviewerNote
        )
    }
    
    /**
     * Convert list of DTOs to domain models
     */
    fun toDomainList(dtos: List<RequestDto>): List<Request> {
        return dtos.map { toDomain(it) }
    }
    
    /**
     * Convert list of domain models to DTOs
     */
    fun toDtoList(domains: List<Request>, deviceId: String): List<RequestDto> {
        return domains.map { toDto(it, deviceId) }
    }
    
    /**
     * Parse status string to enum
     */
    private fun parseStatus(status: String): RequestStatus {
        return when (status.lowercase()) {
            "pending" -> RequestStatus.PENDING
            "approved" -> RequestStatus.APPROVED
            "rejected" -> RequestStatus.REJECTED
            "expired" -> RequestStatus.EXPIRED
            else -> RequestStatus.PENDING
        }
    }
}

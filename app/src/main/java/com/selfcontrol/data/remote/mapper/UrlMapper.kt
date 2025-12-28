package com.selfcontrol.data.remote.mapper

import com.selfcontrol.data.local.entity.UrlEntity
import com.selfcontrol.data.remote.dto.UrlDto
import com.selfcontrol.domain.model.UrlBlacklist
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps between URL related models
 */
@Singleton
class UrlMapper @Inject constructor() {
    
    fun dtoToDomain(dto: UrlDto): UrlBlacklist {
        return UrlBlacklist(
            id = dto.id,
            url = dto.url.ifEmpty { dto.pattern },
            pattern = dto.pattern,
            description = dto.description ?: "",
            deviceId = dto.deviceId,
            isBlocked = dto.isBlocked,
            isActive = dto.isActive,
            isSynced = true,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }
    
    fun domainToDto(domain: UrlBlacklist, deviceId: String): UrlDto {
        return UrlDto(
            id = domain.id,
            deviceId = domain.deviceId.ifEmpty { deviceId },
            url = domain.url,
            pattern = domain.pattern.ifEmpty { domain.url },
            description = domain.description,
            isBlocked = domain.isBlocked,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
    
    fun entityToDomain(entity: UrlEntity): UrlBlacklist {
        return UrlBlacklist(
            id = entity.id,
            url = entity.url.ifEmpty { entity.pattern },
            pattern = entity.pattern,
            description = entity.description ?: "",
            deviceId = entity.deviceId,
            isBlocked = entity.isBlocked,
            isActive = entity.isActive,
            isSynced = entity.isSynced,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
    
    fun domainToEntity(domain: UrlBlacklist): UrlEntity {
        return UrlEntity(
            id = domain.id,
            url = domain.url,
            pattern = domain.pattern.ifEmpty { domain.url },
            description = domain.description,
            deviceId = domain.deviceId,
            isBlocked = domain.isBlocked,
            isActive = domain.isActive,
            isSynced = domain.isSynced,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
    
    fun dtoToEntity(dto: UrlDto): UrlEntity {
        return UrlEntity(
            id = dto.id,
            url = dto.url.ifEmpty { dto.pattern },
            pattern = dto.pattern,
            description = dto.description,
            deviceId = dto.deviceId,
            isBlocked = dto.isBlocked,
            isActive = dto.isActive,
            isSynced = true,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }
}

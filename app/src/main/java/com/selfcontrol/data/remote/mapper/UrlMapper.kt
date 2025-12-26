package com.selfcontrol.data.remote.mapper

import com.selfcontrol.data.local.entity.UrlEntity
import com.selfcontrol.data.remote.dto.UrlDto
import com.selfcontrol.domain.model.UrlBlacklist

object UrlMapper {
    
    fun dtoToDomain(dto: UrlDto): UrlBlacklist {
        return UrlBlacklist(
            id = dto.id,
            pattern = dto.pattern,
            isBlocked = dto.isBlocked
        )
    }
    
    fun domainToDto(domain: UrlBlacklist): UrlDto {
        return UrlDto(
            id = domain.id,
            pattern = domain.pattern,
            isBlocked = domain.isBlocked
        )
    }
    
    fun entityToDomain(entity: UrlEntity): UrlBlacklist {
        return UrlBlacklist(
            id = entity.id,
            pattern = entity.pattern,
            isBlocked = entity.isBlocked,
            createdAt = entity.createdAt,
            isSynced = entity.isSynced
        )
    }
    
    fun domainToEntity(domain: UrlBlacklist): UrlEntity {
        return UrlEntity(
            id = domain.id,
            pattern = domain.pattern,
            isBlocked = domain.isBlocked,
            createdAt = domain.createdAt,
            isSynced = domain.isSynced
        )
    }
}

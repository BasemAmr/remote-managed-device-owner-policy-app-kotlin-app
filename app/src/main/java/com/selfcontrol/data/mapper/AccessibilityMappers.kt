package com.selfcontrol.data.mapper

import com.selfcontrol.data.local.entity.AccessibilityServiceEntity
import com.selfcontrol.data.local.entity.PermissionEntity
import com.selfcontrol.data.remote.dto.AccessibilityServiceDto
import com.selfcontrol.data.remote.dto.PermissionDto
import com.selfcontrol.domain.model.AccessibilityService
import com.selfcontrol.domain.model.Permission
import com.selfcontrol.domain.model.PermissionSeverity

// Entity <-> Domain mappings
fun AccessibilityServiceEntity.toDomain() = AccessibilityService(
    serviceId = serviceId,
    packageName = packageName,
    serviceName = serviceName,
    label = label,
    isEnabled = isEnabled,
    isLocked = isLocked
)

fun AccessibilityService.toEntity() = AccessibilityServiceEntity(
    serviceId = serviceId,
    packageName = packageName,
    serviceName = serviceName,
    label = label,
    isEnabled = isEnabled,
    isLocked = isLocked
)

fun PermissionEntity.toDomain(severity: PermissionSeverity = PermissionSeverity.LOW) = Permission(
    permissionName = permissionName,
    isGranted = isGranted,
    severity = severity
)

fun Permission.toEntity() = PermissionEntity(
    permissionName = permissionName,
    isGranted = isGranted
)

// DTO <-> Domain mappings
fun AccessibilityServiceDto.toDomain() = AccessibilityService(
    serviceId = serviceId,
    packageName = packageName ?: serviceId.substringBefore("/"),
    serviceName = serviceName ?: serviceId.substringAfter("/"),
    label = label ?: "Unknown Service",
    isEnabled = isEnabled ?: false,
    isLocked = isLocked ?: false
)

fun AccessibilityService.toDto() = AccessibilityServiceDto(
    serviceId = serviceId,
    packageName = packageName,
    serviceName = serviceName,
    label = label,
    isEnabled = isEnabled,
    isLocked = isLocked
)

fun PermissionDto.toDomain(severity: PermissionSeverity = PermissionSeverity.LOW) = Permission(
    permissionName = permissionName,
    isGranted = isGranted,
    severity = severity
)

fun Permission.toDto() = PermissionDto(
    permissionName = permissionName,
    isGranted = isGranted
)

package com.selfcontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for policy endpoints
 * Wraps a list of policies returned from the server
 */
data class PolicyResponseDto(
    @SerializedName("policies")
    val policies: List<PolicyDto>
)

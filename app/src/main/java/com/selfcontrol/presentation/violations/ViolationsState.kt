package com.selfcontrol.presentation.violations

import com.selfcontrol.domain.model.Violation

data class ViolationsState(
    val violations: List<Violation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

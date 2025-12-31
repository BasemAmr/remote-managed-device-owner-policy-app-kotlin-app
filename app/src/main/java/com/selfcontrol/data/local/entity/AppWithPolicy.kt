package com.selfcontrol.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relation class to fetch AppEntity with its corresponding PolicyEntity
 */
data class AppWithPolicy(
    @Embedded val app: AppEntity,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "packageName"
    )
    val policy: PolicyEntity?
)

package com.selfcontrol.data.local.database

import androidx.room.TypeConverter
import com.selfcontrol.domain.model.RequestStatus
import com.selfcontrol.domain.model.RequestType
import com.selfcontrol.domain.model.ViolationType

class Converters {
    @TypeConverter
    fun fromRequestType(value: RequestType): String = value.name

    @TypeConverter
    fun toRequestType(value: String): RequestType = try {
        RequestType.valueOf(value)
    } catch (e: IllegalArgumentException) {
        RequestType.APP_ACCESS // Default fallback
    }

    @TypeConverter
    fun fromRequestStatus(value: RequestStatus): String = value.name

    @TypeConverter
    fun toRequestStatus(value: String): RequestStatus = try {
        RequestStatus.valueOf(value)
    } catch (e: IllegalArgumentException) {
        RequestStatus.PENDING
    }

    @TypeConverter
    fun fromViolationType(value: ViolationType): String = value.name

    @TypeConverter
    fun toViolationType(value: String): ViolationType = try {
        ViolationType.valueOf(value)
    } catch (e: IllegalArgumentException) {
        ViolationType.UNKNOWN
    }
}

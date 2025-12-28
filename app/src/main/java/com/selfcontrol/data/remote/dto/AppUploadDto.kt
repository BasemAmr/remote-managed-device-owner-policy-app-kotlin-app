package com.selfcontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for uploading installed app information to server
 */
data class AppUploadDto(
    @SerializedName("package_name")
    val packageName: String,

    @SerializedName("app_name")
    val appName: String,

    @SerializedName("version_code")
    val versionCode: Int,

    @SerializedName("version_name")
    val versionName: String
)

/**
 * Request body for uploading multiple apps
 */
data class AppUploadRequest(
    @SerializedName("apps")
    val apps: List<AppUploadDto>
)
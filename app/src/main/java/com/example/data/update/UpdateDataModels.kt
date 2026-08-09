package com.example.data.update

import android.net.Uri
import java.io.File

data class GitHubAssetDto(
    val name: String,
    val browser_download_url: String,
    val content_type: String? = null,
    val size: Long? = null
)

data class GitHubReleaseDto(
    val tag_name: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean? = false,
    val prerelease: Boolean? = false,
    val assets: List<GitHubAssetDto>? = null
)

data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkFileName: String,
    val apkSize: Long
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val updateInfo: UpdateInfo) : UpdateState()
    data class Downloading(val updateInfo: UpdateInfo, val progress: Int) : UpdateState()
    data class Downloaded(val updateInfo: UpdateInfo, val apkUri: Uri, val apkFile: File) : UpdateState()
    data class PermissionRequired(val updateInfo: UpdateInfo, val apkFile: File) : UpdateState()
    data class Error(val message: String, val updateInfo: UpdateInfo? = null) : UpdateState()
    object UpToDate : UpdateState()
}

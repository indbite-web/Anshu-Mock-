package com.example.data.update

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateManager(
    private val context: Context,
    private val checker: UpdateChecker = UpdateChecker(),
    private val downloader: UpdateDownloader = UpdateDownloader()
) {
    companion object {
        private const val TAG = "UpdateManager"
        private const val PREFS_NAME = "anshu_update_prefs"
        private const val KEY_LAST_CHECK = "last_update_check_timestamp"
        private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
        private const val CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000L // 12 hours

        @Volatile
        private var instance: UpdateManager? = null

        fun getInstance(context: Context): UpdateManager {
            return instance ?: synchronized(this) {
                instance ?: UpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var dismissedVersionInSession: String? = null

    fun checkForUpdates(isManual: Boolean = false) {
        scope.launch {
            if (!isManual) {
                val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
                val now = System.currentTimeMillis()
                if (now - lastCheck < CHECK_INTERVAL_MS) {
                    Log.d(TAG, "Skipping automatic check: last check was less than 12 hours ago.")
                    return@launch
                }
            } else {
                _updateState.value = UpdateState.Checking
            }

            val result = checker.checkForUpdate()
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_CHECK, now).apply()

            result.fold(
                onSuccess = { updateInfo ->
                    if (updateInfo != null) {
                        if (!isManual && updateInfo.versionName == dismissedVersionInSession) {
                            Log.d(TAG, "Update ${updateInfo.versionName} was dismissed in current session.")
                            _updateState.value = UpdateState.Idle
                        } else {
                            _updateState.value = UpdateState.Available(updateInfo)
                        }
                    } else {
                        if (isManual) {
                            _updateState.value = UpdateState.UpToDate
                        } else {
                            _updateState.value = UpdateState.Idle
                        }
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Update check failed: ${error.message}", error)
                    if (isManual) {
                        _updateState.value = UpdateState.Error("Unable to check for updates. Please try again.")
                    } else {
                        _updateState.value = UpdateState.Idle
                    }
                }
            )
        }
    }

    fun dismissUpdate(updateInfo: UpdateInfo) {
        dismissedVersionInSession = updateInfo.versionName
        _updateState.value = UpdateState.Idle
    }

    fun startDownload(updateInfo: UpdateInfo) {
        scope.launch {
            _updateState.value = UpdateState.Downloading(updateInfo, 0)
            val result = downloader.downloadApk(context, updateInfo) { progress ->
                _updateState.value = UpdateState.Downloading(updateInfo, progress)
            }

            result.fold(
                onSuccess = { apkFile ->
                    checkPermissionAndInstall(updateInfo, apkFile)
                },
                onFailure = { error ->
                    _updateState.value = UpdateState.Error(
                        message = error.localizedMessage ?: "Update download failed. Please try again.",
                        updateInfo = updateInfo
                    )
                }
            )
        }
    }

    fun checkPermissionAndInstall(updateInfo: UpdateInfo, apkFile: File) {
        try {
            if (UpdateInstaller.canInstallUnknownApps(context)) {
                val authority = "${context.packageName}.fileprovider"
                val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
                _updateState.value = UpdateState.Downloaded(updateInfo, apkUri, apkFile)
                UpdateInstaller.installApk(context, apkFile)
            } else {
                _updateState.value = UpdateState.PermissionRequired(updateInfo, apkFile)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error in checkPermissionAndInstall", e)
            _updateState.value = UpdateState.Error(
                message = e.localizedMessage ?: "Failed to install update.",
                updateInfo = updateInfo
            )
        }
    }

    fun openSettingsForPermission() {
        UpdateInstaller.openUnknownAppsSettings(context)
    }

    fun retryInstallation(updateInfo: UpdateInfo, apkFile: File) {
        checkPermissionAndInstall(updateInfo, apkFile)
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }
}

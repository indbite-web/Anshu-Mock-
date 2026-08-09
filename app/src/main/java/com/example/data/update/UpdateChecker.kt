package com.example.data.update

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class UpdateChecker(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build(),
    private val repoPath: String = DEFAULT_REPO
) {

    companion object {
        const val DEFAULT_REPO = "anshumock/anshu-mock"
        private const val TAG = "UpdateChecker"

        fun cleanVersion(version: String): String {
            return version.trim()
                .removePrefix("v")
                .removePrefix("V")
        }

        fun isVersionNewer(installedVersion: String, latestVersion: String): Boolean {
            val currentParts = cleanVersion(installedVersion).split(".").mapNotNull { it.toIntOrNull() }
            val latestParts = cleanVersion(latestVersion).split(".").mapNotNull { it.toIntOrNull() }

            val maxLength = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLength) {
                val curr = currentParts.getOrElse(i) { 0 }
                val lat = latestParts.getOrElse(i) { 0 }
                if (lat > curr) return true
                if (lat < curr) return false
            }
            return false
        }
    }

    suspend fun checkForUpdate(
        customRepo: String = repoPath,
        currentVersion: String = BuildConfig.VERSION_NAME
    ): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            var releaseJson: JSONObject? = null
            val latestUrl = "https://api.github.com/repos/$customRepo/releases/latest"

            val request = Request.Builder()
                .url(latestUrl)
                .header("User-Agent", "Anshu-Mock-Android-App")
                .header("Accept", "application/vnd.github+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        releaseJson = JSONObject(body)
                    }
                } else if (response.code == 404) {
                    Log.i(TAG, "GET /releases/latest returned 404 for $customRepo. Attempting /releases list.")
                    // Fallback to releases list
                    val listUrl = "https://api.github.com/repos/$customRepo/releases"
                    val listRequest = Request.Builder()
                        .url(listUrl)
                        .header("User-Agent", "Anshu-Mock-Android-App")
                        .header("Accept", "application/vnd.github+json")
                        .build()

                    client.newCall(listRequest).execute().use { listResponse ->
                        if (listResponse.isSuccessful) {
                            val listBody = listResponse.body?.string()
                            if (!listBody.isNullOrBlank()) {
                                val releasesArray = JSONArray(listBody)
                                if (releasesArray.length() > 0) {
                                    releaseJson = releasesArray.getJSONObject(0)
                                }
                            }
                        } else {
                            Log.i(TAG, "GitHub releases list returned HTTP ${listResponse.code} for $customRepo.")
                        }
                    }
                } else {
                    Log.w(TAG, "GitHub API returned status code ${response.code} for $customRepo")
                }
            }

            if (releaseJson == null) {
                // No release published on GitHub yet or repo not found - user is on latest version
                Log.d(TAG, "No GitHub release found for $customRepo. Treating as up-to-date.")
                return@withContext Result.success(null)
            }

            val releaseObj = releaseJson ?: return@withContext Result.success(null)

            val tagName = releaseObj.optString("tag_name", "")
            if (tagName.isBlank()) {
                Log.w(TAG, "Release JSON contained no tag_name.")
                return@withContext Result.success(null)
            }

            val latestVersion = cleanVersion(tagName)
            if (!isVersionNewer(currentVersion, latestVersion)) {
                Log.d(TAG, "Installed version ($currentVersion) is up-to-date with latest GitHub release ($latestVersion).")
                return@withContext Result.success(null)
            }

            val releaseTitle = releaseObj.optString("name", "Anshu Mock v$latestVersion")
            val releaseNotes = releaseObj.optString("body", "Bug fixes and performance improvements.")
            val assetsArray = releaseObj.optJSONArray("assets")

            if (assetsArray == null || assetsArray.length() == 0) {
                Log.w(TAG, "Release $tagName has no release assets attached.")
                return@withContext Result.success(null)
            }

            var apkDownloadUrl: String? = null
            var apkFileName: String? = null
            var apkSize: Long = 0L

            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(i)
                val name = asset.optString("name", "")
                val contentType = asset.optString("content_type", "")
                if (name.endsWith(".apk", ignoreCase = true) ||
                    contentType.contains("package-archive", ignoreCase = true)
                ) {
                    apkDownloadUrl = asset.optString("browser_download_url", "")
                    apkFileName = name
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }

            if (apkDownloadUrl.isNullOrEmpty() || apkFileName.isNullOrEmpty()) {
                Log.w(TAG, "Release $tagName found, but no .apk asset was present in release assets.")
                return@withContext Result.success(null)
            }

            val updateInfo = UpdateInfo(
                tagName = tagName,
                versionName = latestVersion,
                title = releaseTitle,
                releaseNotes = releaseNotes,
                apkUrl = apkDownloadUrl,
                apkFileName = apkFileName,
                apkSize = apkSize
            )

            Log.i(TAG, "New update available: ${updateInfo.versionName} at ${updateInfo.apkUrl}")
            Result.success(updateInfo)

        } catch (e: Throwable) {
            Log.e(TAG, "Error checking for updates from GitHub", e)
            Result.failure(e)
        }
    }
}

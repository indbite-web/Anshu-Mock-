package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object Constants {
    const val API_KEY_TUTORIAL_URL = "https://www.youtube.com/shorts/JYg2jiR_7rA"

    fun openTutorialVideo(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(API_KEY_TUTORIAL_URL)).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(API_KEY_TUTORIAL_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicInteger

object AdManager {
    private const val TAG = "AdManager"

    // Real AdMob App ID: ca-app-pub-8406665897328392~8550491121
    // Real Interstitial Ad Unit ID: ca-app-pub-8406665897328392/5232007308
    const val REAL_AD_UNIT_ID = "ca-app-pub-8406665897328392/5232007308"
    const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // FEATURE FLAGS
    const val ADS_ENABLED = true
    const val IS_AD_TEST_MODE = true

    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private val generationCounter = AtomicInteger(0)

    fun getAdUnitId(): String {
        return if (IS_AD_TEST_MODE) TEST_AD_UNIT_ID else REAL_AD_UNIT_ID
    }

    fun initialize(context: Context) {
        if (!ADS_ENABLED) return
        if (isInitialized) return

        try {
            MobileAds.initialize(context.applicationContext) { status ->
                isInitialized = true
                Log.d(TAG, "AdMob MobileAds initialized successfully")
                loadInterstitialAd(context.applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds safely", e)
        }
    }

    fun loadInterstitialAd(context: Context) {
        if (!ADS_ENABLED) return
        if (interstitialAd != null || isLoadingAd) return

        isLoadingAd = true
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context.applicationContext,
                getAdUnitId(),
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isLoadingAd = false
                        Log.d(TAG, "Interstitial ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        isLoadingAd = false
                        Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            isLoadingAd = false
            Log.e(TAG, "Exception while loading interstitial ad", e)
        }
    }

    fun shouldShowAdForNextGeneration(): Boolean {
        if (!ADS_ENABLED) return false
        // Next generation attempt index = current count + 1
        // Odd = no ad (1, 3, 5...), Even = ad (2, 4, 6...)
        val nextIndex = generationCounter.get() + 1
        return nextIndex % 2 == 0
    }

    fun onGenerationStart(context: Context) {
        if (!ADS_ENABLED) return
        if (shouldShowAdForNextGeneration()) {
            loadInterstitialAd(context)
        }
    }

    fun onGenerationSuccess() {
        if (!ADS_ENABLED) return
        val current = generationCounter.incrementAndGet()
        Log.d(TAG, "Generation succeeded. New completed generation count: $current")
    }

    fun onGenerationFailed() {
        Log.d(TAG, "Generation failed. Generation count remains: ${generationCounter.get()}")
    }

    fun showInterstitialAdIfEligible(activity: Activity?, onComplete: () -> Unit) {
        if (!ADS_ENABLED || activity == null) {
            onComplete()
            return
        }

        // Show ad if loaded and if current generation (which just succeeded) was even
        val currentCompleted = generationCounter.get()
        val isEvenGeneration = currentCompleted > 0 && (currentCompleted % 2 == 0)

        val ad = interstitialAd
        if (isEvenGeneration && ad != null) {
            interstitialAd = null
            try {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad dismissed by user")
                        loadInterstitialAd(activity)
                        onComplete()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.w(TAG, "Ad failed to show: ${adError.message}")
                        loadInterstitialAd(activity)
                        onComplete()
                    }
                }
                ad.show(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Exception showing interstitial ad", e)
                loadInterstitialAd(activity)
                onComplete()
            }
        } else {
            loadInterstitialAd(activity)
            onComplete()
        }
    }
}

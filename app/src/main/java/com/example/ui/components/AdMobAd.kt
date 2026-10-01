package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Whether ad loading / display is enabled. Always enabled for test ads.
 */
fun isAdSupported(): Boolean {
    return true
}

private const val TAG = "AdMobAd"

/**
 * Official AdMob Test Banner Unit ID (Guaranteed to load test ads across all devices & emulators).
 */
private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

/**
 * Official AdMob Test Interstitial Unit ID.
 */
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

/**
 * Composable banner ad rendered via official AdView through [AndroidView].
 */
@Composable
fun AdMobBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = BANNER_AD_UNIT_ID
) {
    val inspectionMode = LocalInspectionMode.current
    if (inspectionMode) {
        Spacer(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("ad_banner_placeholder")
        )
        return
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("ad_banner"),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        Log.w(TAG, "Banner ad failed to load: ${p0.message}")
                    }
                }
                try {
                    loadAd(AdRequest.Builder().build())
                } catch (e: Throwable) {
                    Log.w(TAG, "Banner load exception: ${e.message}")
                }
            }
        },
        update = {
            // Banner loading is handled during creation
        }
    )
}

/**
 * Backend helper responsible for loading and showing AdMob interstitial ads.
 */
object AdMobInterstitialHelper {
    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    /**
     * Preload an interstitial ad.
     */
    fun loadAd(context: Context, adUnitId: String = INTERSTITIAL_AD_UNIT_ID) {
        if (mInterstitialAd != null || isAdLoading) return
        isAdLoading = true

        val adRequest = AdRequest.Builder().build()
        try {
            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        mInterstitialAd = null
                        isAdLoading = false
                        Log.w(TAG, "Interstitial ad failed to load: ${adError.message}")
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        mInterstitialAd = interstitialAd
                        isAdLoading = false
                    }
                }
            )
        } catch (e: Throwable) {
            isAdLoading = false
            Log.w(TAG, "Interstitial load exception: ${e.message}")
        }
    }

    /**
     * Show the preloaded interstitial, invoking [onAdClosed] when dismissed.
     */
    fun showAd(activity: Activity, onAdClosed: () -> Unit) {
        val ad = mInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    onAdClosed()
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    Log.w(TAG, "Interstitial ad failed to show: ${adError.message}")
                    onAdClosed()
                }
            }
            try {
                ad.show(activity)
            } catch (e: Throwable) {
                mInterstitialAd = null
                onAdClosed()
            }
        } else {
            onAdClosed()
            loadAd(activity)
        }
    }
}

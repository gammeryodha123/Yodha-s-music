package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
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
 * Whether ad loading / display is supported in the current environment.
 *
 * AdMob is disabled on emulators and inside Compose previews to avoid policy
 * violations, crashes on devices without Google Play services, and preview errors.
 */
fun isAdSupported(): Boolean {
    if (isEmulator()) return false
    return true
}

private fun isEmulator(): Boolean {
    return (android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.MANUFACTURER.contains("Genymotion")
            || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
            || "google_sdk" == android.os.Build.PRODUCT
            || android.os.Build.HARDWARE.contains("goldfish")
            || android.os.Build.HARDWARE.contains("ranchu"))
}

private const val TAG = "AdMobAd"

/**
 * Production AdMob banner ad unit id (matches the application id in the manifest).
 */
private const val BANNER_AD_UNIT_ID = "ca-app-pub-1565038231841255/7148486913"

/**
 * Test interstitial ad unit id. Replace with a production interstitial ad unit id
 * for release builds.
 */
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

/**
 * Composable banner ad rendered via the official AdView through [AndroidView].
 *
 * The ad is only rendered when [isAdSupported] returns true and the composable is
 * not being previewed. When ads are not supported an empty, correctly-sized spacer
 * is emitted so the layout does not shift.
 */
@Composable
fun AdMobBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = BANNER_AD_UNIT_ID
) {
    val inspectionMode = LocalInspectionMode.current
    if (!isAdSupported() || inspectionMode) {
        // Reserve the banner height so the bottom bar layout stays stable.
        androidx.compose.foundation.layout.Spacer(
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
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // Banner loading is handled once during creation; nothing to update.
        }
    )
}

/**
 * Backend helper responsible for loading and showing AdMob interstitial ads.
 *
 * A single interstitial is preloaded and reused. After the ad is dismissed the next
 * one is preloaded automatically so it is ready for the following trigger. When no ad
 * is available the supplied [onAdClosed] callback is still invoked so playback always
 * continues.
 */
object AdMobInterstitialHelper {
    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    /**
     * Preload an interstitial ad. Safe to call repeatedly: it is a no-op while an ad
     * is already loaded or loading.
     */
    fun loadAd(context: Context, adUnitId: String = INTERSTITIAL_AD_UNIT_ID) {
        if (mInterstitialAd != null || isAdLoading) return
        isAdLoading = true

        val adRequest = AdRequest.Builder().build()
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
    }

    /**
     * Show the preloaded interstitial, invoking [onAdClosed] when the user dismisses
     * it (or immediately if no ad is ready). Always continues with [onAdClosed].
     */
    fun showAd(activity: Activity, onAdClosed: () -> Unit) {
        if (!isAdSupported()) {
            onAdClosed()
            return
        }

        val ad = mInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    onAdClosed()
                    // Preload the next ad automatically so it is ready next time.
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    Log.w(TAG, "Interstitial ad failed to show: ${adError.message}")
                    onAdClosed()
                }
            }
            ad.show(activity)
        } else {
            // No ad cached yet: continue playback and try to preload for next time.
            onAdClosed()
            loadAd(activity)
        }
    }
}

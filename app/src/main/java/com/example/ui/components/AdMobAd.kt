package com.example.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

@Composable
fun AdMobBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-1565038231841255/5937342019" // User Created Banner ID
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            try {
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    setAdUnitId(adUnitId)
                    loadAd(AdRequest.Builder().build())
                }
            } catch (e: Throwable) {
                android.util.Log.e("AdMobAd", "Failed to create AdView: ${e.message}")
                android.widget.FrameLayout(context) // Fallback empty view to avoid crash
            }
        },
        update = { adView ->
            // No-op - ad loading is handled during creation
        }
    )
}

object AdMobInterstitialHelper {
    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    fun loadAd(context: Context, adUnitId: String = "ca-app-pub-3940256099942544/1033173712") {
        if (mInterstitialAd != null || isAdLoading) return
        isAdLoading = true

        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isAdLoading = false
                }
            })
        } catch (e: Throwable) {
            android.util.Log.e("AdMobInterstitial", "Failed to load InterstitialAd: ${e.message}")
            isAdLoading = false
            mInterstitialAd = null
        }
    }

    fun showAd(activity: Activity, onAdClosed: () -> Unit) {
        val ad = mInterstitialAd
        if (ad != null) {
            try {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        mInterstitialAd = null
                        onAdClosed()
                        // Preload the next ad automatically
                        loadAd(activity)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        mInterstitialAd = null
                        onAdClosed()
                    }
                }
                ad.show(activity)
            } catch (e: Throwable) {
                android.util.Log.e("AdMobInterstitial", "Failed to show InterstitialAd: ${e.message}")
                mInterstitialAd = null
                onAdClosed()
            }
        } else {
            onAdClosed()
            // Try loading an ad for the next attempt
            loadAd(activity)
        }
    }
}

package com.example.network

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return AudioPlayerManager.getMediaSession(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioPlayerManager.releaseSession()
    }
}

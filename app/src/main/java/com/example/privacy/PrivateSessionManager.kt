package com.example.privacy

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

class PrivateSessionManager {

    fun createPrivateSessionSettings(): GeckoSessionSettings {
        return GeckoSessionSettings.Builder()
            .usePrivateMode(true)
            .useTrackingProtection(true)
            .build()
    }

    fun createNormalSessionSettings(): GeckoSessionSettings {
        return GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .useTrackingProtection(true)
            .build()
    }

    fun purgePrivateSession(session: GeckoSession) {
        try {
            session.close()
        } catch (e: Exception) {
            // Safe cleanup
        }
    }

    companion object {
        val instance by lazy { PrivateSessionManager() }
    }
}

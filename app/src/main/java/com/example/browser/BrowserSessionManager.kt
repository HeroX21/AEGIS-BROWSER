package com.example.browser

import android.content.Context
import com.example.privacy.PrivateSessionManager
import com.example.privacy.TrackingProtectionManager
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the lifecycle, configuration, and runtime binding of GeckoSession instances.
 */
class BrowserSessionManager(
    private val context: Context,
    private val runtime: GeckoRuntime = GeckoEngineProvider.getRuntime(context),
    private val privateSessionManager: PrivateSessionManager = PrivateSessionManager.instance,
    private val trackingProtectionManager: TrackingProtectionManager = TrackingProtectionManager.instance
) {
    private val activeSessions = ConcurrentHashMap<String, GeckoSession>()

    fun createSession(tabId: String, isPrivate: Boolean): GeckoSession {
        val settings = if (isPrivate) {
            privateSessionManager.createPrivateSessionSettings()
        } else {
            privateSessionManager.createNormalSessionSettings()
        }

        val session = GeckoSession(settings)
        session.contentBlockingDelegate = trackingProtectionManager.createContentBlockingDelegate(tabId)
        session.open(runtime)
        activeSessions[tabId] = session
        return session
    }

    fun getSession(tabId: String): GeckoSession? = activeSessions[tabId]

    fun closeSession(tabId: String) {
        val session = activeSessions.remove(tabId)
        try {
            session?.close()
        } catch (e: Exception) {
            // Graceful cleanup
        }
    }

    fun closeAllSessions(privateOnly: Boolean = false) {
        val iterator = activeSessions.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val session = entry.value
            if (!privateOnly || session.settings.usePrivateMode) {
                try {
                    session.close()
                } catch (e: Exception) {
                    // Safe cleanup
                }
                iterator.remove()
            }
        }
    }

    fun attachToView(view: GeckoView, session: GeckoSession) {
        view.setSession(session)
    }

    fun detachFromView(view: GeckoView) {
        view.releaseSession()
    }
}

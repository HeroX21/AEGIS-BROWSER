package com.example.browser

import android.content.Context
import android.util.Log
import com.example.blocking.BlockCategory
import com.example.blocking.BlockingEventRepository
import com.example.blocking.FilterRule
import org.json.JSONObject
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.WebExtension

object GeckoEngineProvider {

    private const val TAG = "GeckoEngineProvider"
    private const val EXTENSION_ID = "aegis-blocker@aegisbrowser.local"
    private const val EXTENSION_URI = "resource://android/assets/extensions/aegis-blocker/"

    @Volatile
    private var runtime: GeckoRuntime? = null

    @Volatile
    private var installedExtension: WebExtension? = null

    fun getRuntime(context: Context): GeckoRuntime {
        return runtime ?: synchronized(this) {
            runtime ?: initRuntime(context.applicationContext).also { runtime = it }
        }
    }

    private fun initRuntime(context: Context): GeckoRuntime {
        val settings = GeckoRuntimeSettings.Builder()
            .contentBlocking(
                ContentBlocking.Settings.Builder()
                    .antiTracking(ContentBlocking.AntiTracking.DEFAULT)
                    .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                    .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                    .build()
            )
            .javaScriptEnabled(true)
            .build()

        val r = GeckoRuntime.create(context, settings)

        // Install built-in Aegis Blocker extension and set message delegate
        try {
            r.webExtensionController.ensureBuiltIn(EXTENSION_URI, EXTENSION_ID)
                .accept(
                    { extension ->
                        installedExtension = extension
                        extension?.setMessageDelegate(
                            object : WebExtension.MessageDelegate {
                                override fun onMessage(
                                    nativeApp: String,
                                    message: Any,
                                    sender: WebExtension.MessageSender
                                ): GeckoResult<Any>? {
                                    handleExtensionMessage(message)
                                    return null
                                }
                            },
                            "browser"
                        )
                        Log.i(TAG, "Aegis Blocker WebExtension installed successfully")
                    },
                    { throwable ->
                        Log.e(TAG, "Failed to install Aegis Blocker WebExtension", throwable)
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "WebExtension installation error", e)
        }

        return r
    }

    private fun handleExtensionMessage(message: Any) {
        try {
            val json = when (message) {
                is JSONObject -> message
                is String -> JSONObject(message)
                else -> return
            }

            if (json.optString("type") == "REQUEST_BLOCKED") {
                val url = json.optString("url")
                val domain = json.optString("domain", FilterRule.extractHost(url))
                BlockingEventRepository.instance.recordEvent(
                    tabId = "active",
                    category = BlockCategory.AD,
                    url = url,
                    domain = domain
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse extension message: $e")
        }
    }
}

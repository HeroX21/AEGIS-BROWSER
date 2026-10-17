package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.browser.GeckoEngineProvider
import com.example.ui.BrowserScreen
import com.example.ui.BrowserViewModel
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.DarkVoid
import org.mozilla.geckoview.GeckoRuntime

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()
    private lateinit var geckoRuntime: GeckoRuntime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Mozilla GeckoView Runtime engine
        geckoRuntime = GeckoEngineProvider.getRuntime(applicationContext)

        // Handle incoming web URL from external intents
        handleIncomingIntent(intent)

        setContent {
            AegisTheme {
                val tabs = viewModel.tabs.collectAsStateWithLifecycle().value
                val activeTabId = viewModel.activeTabId.collectAsStateWithLifecycle().value
                val activeTab = tabs.find { it.id == activeTabId }

                // Native BackHandler: if active tab can go back in history, navigate backward
                BackHandler(enabled = activeTab?.canGoBack == true) {
                    viewModel.goBack()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkVoid
                ) {
                    BrowserScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val dataUri = intent.dataString
            if (!dataUri.isNullOrBlank()) {
                viewModel.loadUrl(dataUri)
            }
        }
    }
}

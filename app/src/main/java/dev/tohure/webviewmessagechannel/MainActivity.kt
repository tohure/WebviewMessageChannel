package dev.tohure.webviewmessagechannel

import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tohure.webviewmessagechannel.ui.theme.WebviewMessageChannelTheme
import java.io.BufferedReader

const val TAG = "tohure-log"
private const val BASE_URL = "https://app.assets.android"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebviewMessageChannelTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WebViewScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun WebViewScreen(
    modifier: Modifier = Modifier,
    webViewModel: WebViewViewModel = viewModel()
) {
    val webViewState by webViewModel.webViewState.collectAsState()
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE

                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest?) {
                            request?.grant(request.resources)
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d(
                                TAG,
                                "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}"
                            )
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (view != null) {
                                webViewModel.createMessageChannel(view)
                            }
                        }
                    }

                    val html =
                        ctx.assets.open("index.html").bufferedReader().use(BufferedReader::readText)
                    loadDataWithBaseURL(BASE_URL, html, "text/html", "UTF-8", null)
                }
            },
            update = { webViewInstance ->
                webView = webViewInstance
            },
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = {
                webViewModel.initializePort(webView, context, BASE_URL)
            },

            enabled = !webViewState.isPortInitialized,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Initialize Communication Port")
        }

        Button(
            onClick = {
                webViewModel.sendMessageToWeb()
            },
            enabled = webViewState.isPortInitialized
        ) {
            Text("Send message by Port")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WebPreview() {
    WebviewMessageChannelTheme {
        WebViewScreen()
    }
}
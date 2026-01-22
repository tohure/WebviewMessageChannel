package dev.tohure.webviewmessagechannel

import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebMessagePort
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import dev.tohure.webviewmessagechannel.ui.theme.WebviewMessageChannelTheme

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
fun WebViewScreen(modifier: Modifier = Modifier) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var port1: WebMessagePort? by remember { mutableStateOf(null) }
    var port2: WebMessagePort? by remember { mutableStateOf(null) }
    var ctx = LocalContext.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
    ) {
        AndroidView(factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.grant(request.resources)
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        //Console Log from Web to Android
                        Log.d(
                            "tohure",
                            "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}"
                        )
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(
                        view: WebView?,
                        url: String?
                    ) {
                        super.onPageFinished(view, url)

                        //Start setting communication with WebMessagePort
                        if (port1 == null) {
                            val channel =
                                createWebMessageChannel() //Creating two communication ports
                            port1 = channel[0] //Native channel port
                            port2 = channel[1] //Web communication channel port
                        }
                    }
                }

                loadUrl("https://rpp.pe")
            }
        }, update = { webViewInstance ->
            webView = webViewInstance
        }, modifier = Modifier.weight(1f))
    }
}


@Preview(showBackground = true)
@Composable
fun WebPreview() {
    WebviewMessageChannelTheme {
        WebViewScreen()
    }
}
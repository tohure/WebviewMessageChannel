package dev.tohure.webviewmessagechannel

import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.core.net.toUri
import dev.tohure.webviewmessagechannel.ui.theme.WebviewMessageChannelTheme

const val TAG = "tohure-log"

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
    val ctx = LocalContext.current

    Column(
        modifier =
            modifier
                .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            factory = { context ->
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
                                TAG,
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

                    loadUrl("file:///android_asset/index.html")
                }
            }, update = { webViewInstance ->
                webView = webViewInstance
            }, modifier = Modifier.weight(1f)
        )

        Button(
            onClick = {
                Log.d(TAG, "WebViewScreen: Initializing port...")

                //Setting interface for web listening
                port1?.setWebMessageCallback(object : WebMessagePort.WebMessageCallback() {
                    override fun onMessage(
                        port: WebMessagePort?,
                        message: WebMessage?
                    ) {
                        super.onMessage(port, message)

                        val response = message?.data //Manage web response
                        Toast.makeText(ctx, "Web response: $response", Toast.LENGTH_SHORT)
                            .show()
                        Log.d(TAG, "onMessage: $response")
                    }
                })

                //Initialization of communication channel
                val webMessage = WebMessage("Hello from Android", arrayOf(port2))
                webView?.postWebMessage(webMessage, "*".toUri())
                Log.d(TAG, "Web Ports: ${webMessage.ports.toString()}")
                Log.d(TAG, "Web Data: ${webMessage.data.toString()}")
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Initialize port")
        }

        Button(onClick = {
            port1?.postMessage(WebMessage("Android Compose Message"))
        }) {
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
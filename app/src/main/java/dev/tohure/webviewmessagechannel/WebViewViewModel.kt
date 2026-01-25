package dev.tohure.webviewmessagechannel

import android.util.Log
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class WebViewState(
    val isPortInitialized: Boolean = false,
    val lastMessageFromWeb: String? = null
)

class WebViewViewModel : ViewModel() {

    val webViewState: StateFlow<WebViewState>
        field = MutableStateFlow(WebViewState())

    private var port1: WebMessagePort? = null
    private var port2: WebMessagePort? = null

    fun createMessageChannel(webView: WebView): Array<WebMessagePort> {
        val channel = webView.createWebMessageChannel()
        port1 = channel[0]
        port2 = channel[1]
        return channel
    }

    fun initializePort(webView: WebView?, baseUrl: String) {
        Log.d(TAG, "WebViewViewModel: Initializing port...")

        port1?.setWebMessageCallback(object : WebMessagePort.WebMessageCallback() {
            override fun onMessage(port: WebMessagePort?, message: WebMessage?) {
                super.onMessage(port, message)
                val response = message?.data
                Log.d(TAG, "onMessage: $response")
                webViewState.update {
                    it.copy(
                        lastMessageFromWeb = "Web response: $response"
                    )
                }
            }
        })

        val webMessage = WebMessage("token_xyz-ultra-secret", arrayOf(port2))
        webView?.postWebMessage(webMessage, baseUrl.toUri())
        webViewState.update { it.copy(isPortInitialized = true) }
    }

    fun onToastMessageShown() {
        webViewState.update { it.copy(lastMessageFromWeb = null) }
    }

    fun sendMessageToWeb() {
        val message = "Message from Android App --> ${System.currentTimeMillis()}"
        port1?.postMessage(WebMessage(message))
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, closing ports.")
        port1?.close()
        port2?.close()
    }
}
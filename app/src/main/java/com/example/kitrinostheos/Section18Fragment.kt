package com.example.kitrinostheos

import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.ByteArrayInputStream

class Section18Fragment : Fragment(), WebViewReloadable {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // User Agents
    private val mobileUA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
    private val desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_webview, container, false)

        webView = rootView.findViewById(R.id.webView)
        progressBar = rootView.findViewById(R.id.progressBar)
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout)

        // 1. Βασικές Ρυθμίσεις (ΠΡΕΠΕΙ ΝΑ ΥΠΑΡΧΟΥΝ ΓΙΑ ΝΑ ΦΟΡΤΩΣΕΙ)
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        // Προαιρετικό: Αν το chat φαίνεται πολύ μικρό, ανέβασε το scale
        webView.setInitialScale(100)

        // Χρησιμοποιούμε Mobile UA στην αρχή για να είναι σίγουρο το Login
        webSettings.userAgentString = mobileUA

        // Cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // 2. Διαχείριση Login Popup
        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                val newWebView = WebView(requireContext())
                newWebView.settings.javaScriptEnabled = true
                newWebView.settings.domStorageEnabled = true
                newWebView.settings.userAgentString = mobileUA // Mobile για το Login

                val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                dialog.setContentView(newWebView)
                dialog.show()

                newWebView.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView?) {
                        dialog.dismiss()
                        webView.reload()
                    }
                }

                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false // Αφήνουμε τη Google να κάνει τα redirects της
                    }
                }

                val transport = resultMsg?.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                return true
            }
        }

        // 3. WebViewClient για την κεντρική σελίδα
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE

                if (url != null) {
                    // ΜΟΝΟ αν πάμε σε σελίδα Google Login γυρνάμε σε Mobile
                    if (url.contains("accounts.google.com") || url.contains("ServiceLogin")) {
                        if (webView.settings.userAgentString != mobileUA) {
                            webView.settings.userAgentString = mobileUA
                            view?.reload() // Reload με το σωστό UA για να μην φάμε άκυρο από τη Google
                        }
                    } else if (url.contains("youtube.com")) {
                        if (webView.settings.userAgentString != desktopUA) {
                            webView.settings.userAgentString = desktopUA
                        }
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false

                if (url?.contains("youtube.com") == true) {
                    val script = """
                        (function() {
                            // Δίνουμε λίγο χρόνο στο YouTube να φορτώσει το chat frame
                            setTimeout(function() {
                                var style = document.createElement('style');
                                style.innerHTML = `
                                    #masthead-container, #header-wide { display: none !important; }
                                    ytd-live-chat-frame { 
                                        display: block !important; 
                                        height: 600px !important; 
                                        visibility: visible !important;
                                    }
                                    iron-collapse { display: block !important; }
                                `;
                                document.head.appendChild(style);
                                
                                // Force click στο κουμπί "Show Chat" αν είναι κλειστό
                                var chatButton = document.querySelector('button[aria-label="Show chat"]');
                                if (chatButton) chatButton.click();
                            }, 2000);
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(script, null)
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        swipeRefreshLayout.setOnRefreshListener { webView.reload() }

        // Φόρτωση του URL
        webView.loadUrl("https://player.yellowradio.gr/mobile/")

        return rootView
    }

    override fun reloadWebView() = webView.reload()
    override fun getWebView(): WebView = webView
}
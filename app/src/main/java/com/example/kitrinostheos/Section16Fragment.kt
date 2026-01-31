package com.example.kitrinostheos

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class Section16Fragment : Fragment(), WebViewReloadable {

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

        // Προαιρετικό: Αν το chat φαίνεται πολύ μικρό, ανέβασε το scale
        webSettings.useWideViewPort = false
        webSettings.loadWithOverviewMode = true
        webView.setInitialScale(150) // Αυτό θα δώσει το "διπλάσιο" πλάτος που ζητάς
        webSettings.textZoom = 115 // Δοκίμασε από 130 έως 150 για να βρεις αυτό που σε βολεύει
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
                var style = document.createElement('style');
                style.innerHTML = `
                    /* 1. Βασικά */
                    #masthead-container, #header-wide, #guide { display: none !important; }

                    /* 2. ΑΥΤΟΜΑΤΟ WRAPPING & ΥΨΟΣ (Η Λύση) */
                    /* Λέμε σε ΟΛΑ τα στοιχεία που περιέχουν κείμενο να μην κόβουν ΠΟΤΕ τίποτα */
                    #video-title, 
                    #metadata-line, 
                    ytd-video-meta-block,
                    .style-scope.ytd-rich-grid-media {
                        white-space: normal !important;      /* Επιτρέπει στο κείμενο να αλλάζει σειρά */
                        display: block !important;           /* Σπάει τον περιορισμό της ίδιας γραμμής */
                        max-height: none !important;         /* Καταργεί το όριο ύψους */
                        height: auto !important;             /* Το ύψος ορίζεται από το κείμενο */
                        overflow: visible !important;        /* Δείχνει ό,τι περισσεύει */
                        -webkit-line-clamp: unset !important;/* Απενεργοποιεί το "κόψιμο" μετά από 2 γραμμές */
                    }

                    /* 3. ΔΙΟΡΘΩΣΗ ΓΙΑ ΤΑ ΓΡΑΜΜΑΤΑ ΠΟΥ ΚΡΕΜΟΝΤΑΙ */
                    #video-title {
                        line-height: 1.5em !important;       /* Δίνει αέρα ανάμεσα στις γραμμές */
                        padding-bottom: 10px !important;     /* Χώρος για g, y, p, q */
                        margin-bottom: 5px !important;
                    }
                `;
                document.head.appendChild(style);
                window.dispatchEvent(new Event('resize'));
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
        webView.loadUrl("https://www.youtube.com/@AllAboutARISTV/streams")

        return rootView
    }

    override fun reloadWebView() = webView.reload()
    override fun getWebView(): WebView = webView
}
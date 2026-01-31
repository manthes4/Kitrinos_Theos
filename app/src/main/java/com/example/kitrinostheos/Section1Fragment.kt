package com.example.kitrinostheos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.ByteArrayInputStream

class Section1Fragment : Fragment(), WebViewReloadable {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val adKeywords = listOf(
        "googleads", "doubleclick", "pagead", "googlesyndication",
        "adservice", "taboola", "outbrain", "facebook.com/tr/", "adsbygoogle"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_webview, container, false)

        webView = rootView.findViewById(R.id.webView)
        progressBar = rootView.findViewById(R.id.progressBar)
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout)

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.loadsImagesAutomatically = true

        // Σταθερό Scale για πρακτικότητα
        webView.setInitialScale(140)
        webSettings.textZoom = 150

        // Απενεργοποιημένο για να μην ξεχειλώνει
        webSettings.useWideViewPort = false
        webSettings.loadWithOverviewMode = true

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // 1. Ενεργοποιεί το swipe μόνο αν το WebView είναι στο πάνω μέρος
        webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            swipeRefreshLayout.isEnabled = scrollY == 0
        }

        // 2. Περιορίζει το swipe μόνο στο πάνω 35% της οθόνης
        swipeRefreshLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val y = event.rawY
                val screenHeight = resources.displayMetrics.heightPixels
                // Ελέγχει αν το WebView είναι στο 0 και το δάκτυλο είναι ψηλά
                swipeRefreshLayout.isEnabled = webView.scrollY == 0 && y < screenHeight * 0.35f
            }
            false
        }

        webView.webViewClient = object : WebViewClient() {

            // Inject τη στιγμή που εμφανίζεται το περιεχόμενο
            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                applyCleanScript(view)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                applyCleanScript(view)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                for (keyword in adKeywords) {
                    if (url.contains(keyword)) {
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }
        }

        swipeRefreshLayout.setOnRefreshListener { webView.reload() }
        webView.loadUrl("https://allaboutaris.gr/tag/%CE%B1%CF%81%CE%B7%CF%83/")
        return rootView
    }

    private fun applyCleanScript(view: WebView?) {
        val cleanScript = """
    (function() {
        // 1. Προσθήκη CSS Styles
        var style = document.getElementById('clean-style');
        if (!style) {
            style = document.createElement('style');
            style.id = 'clean-style';
            document.head.appendChild(style);
        }
        style.innerHTML = `
            /* Ο κώδικας που δούλεψε */
            .adsbygoogle, .advertisement, .ad-container, .at-above-post,
            .top-banner-area, .mg_box, div[class*="td-a-ad"], .td-header-ad-wrap { 
                display: none !important; 
                height: 0 !important; 
                margin: 0 !important; 
                padding: 0 !important;
            }
            .td-header-menu-wrap-menu { margin-bottom: 0 !important; }
            .td-main-content-wrap, .td-container-wrap { padding-top: 0 !important; margin-top: 0 !important; }
            .tdc-row [class*="tdi_"] .td-element-style { display: none !important; }

            /* Διόρθωση πλάτους */
            body, html { overflow-x: hidden !important; width: 100% !important; }
        `;

        // 2. Το "Επιθετικό μάζεμα" που έκανε τη διαφορά
        var allDivs = document.querySelectorAll('div');
        allDivs.forEach(function(div) {
            if (div.className && typeof div.className === 'string' && div.className.includes('ad')) {
                // Αν το div είναι ψηλό και περιέχει τη λέξη ad, το εξαφανίζουμε
                if (div.offsetHeight > 10) {
                    div.style.display = 'none';
                    div.style.height = '0';
                    div.style.margin = '0';
                }
            }
        });
    })();
""".trimIndent()
        view?.evaluateJavascript(cleanScript, null)
    }

    override fun reloadWebView() = webView.reload()
    override fun getWebView(): WebView = webView
}
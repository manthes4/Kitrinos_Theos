package com.example.kitrinostheos

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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

class Section15Fragment : Fragment(), WebViewReloadable {

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
        webView.setInitialScale(110)
        webSettings.textZoom = 110

        // Απενεργοποιημένο για να μην ξεχειλώνει
        webSettings.useWideViewPort = true
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
        webView.loadUrl("https://www.emperor.gr/forums/search.php?search_id=newposts")
        return rootView
    }

    private fun applyCleanScript(view: WebView?) {
        val cleanScript = """
    (function() {
        if (window.cleanerInitialized) return;
        window.cleanerInitialized = true;

        const removeAds = () => {
            const selectors = [
                '[id*="ls-ad"]', '.advertising-component', '#d_as_f_t', 
                '#onetrust-banner-sdk', 'iframe', 'ins.adsbygoogle'
            ];
            selectors.forEach(s => {
                document.querySelectorAll(s).forEach(el => el.remove());
            });
        };

        // MutationObserver: Παρακολουθεί τη σελίδα και σβήνει ads ακαριαία
        const observer = new MutationObserver(removeAds);
        observer.observe(document.body, { childList: true, subtree: true });

        // Άμεση εκτέλεση
        removeAds();

        // CSS για ταχύτητα στο rendering
        const style = document.createElement('style');
        style.innerHTML = 'body, html { overflow-x: hidden !important; } .ot-sdk-row, .banner { display:none !important; }';
        document.head.appendChild(style);
    })();
""".trimIndent()
        view?.evaluateJavascript(cleanScript, null)
    }

    override fun reloadWebView() = webView.reload()
    override fun getWebView(): WebView = webView
}
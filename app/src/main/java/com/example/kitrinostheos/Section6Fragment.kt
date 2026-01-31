package com.example.kitrinostheos

import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.ByteArrayInputStream

class Section6Fragment : Fragment(), WebViewReloadable {

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

        // ΕΠΑΝΑΦΟΡΑ ΡΥΘΜΙΣΕΩΝ ΠΟΥ ΔΟΥΛΕΥΑΝ ΣΤΟ SKY SPORTS
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.setSupportZoom(true)

        // Scale & Zoom (Το 140 που ήθελες)
        webView.setInitialScale(140)
        webSettings.textZoom = 100

        webSettings.useWideViewPort = true // Το Sky Sports το χρειάζεται true
        webSettings.loadWithOverviewMode = true

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Listeners για Scroll και Touch
        webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            swipeRefreshLayout.isEnabled = scrollY == 0
        }

        swipeRefreshLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val y = event.rawY
                val screenHeight = resources.displayMetrics.heightPixels
                swipeRefreshLayout.isEnabled = webView.scrollY == 0 && y < screenHeight * 0.35f
            }
            false
        }

        webView.webViewClient = object : WebViewClient() {
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

                // ΕΠΑΝΑΦΟΡΑ LAZY LOAD (Από τον παλιό κώδικα)
                view?.evaluateJavascript("javascript:(function() { var images = document.getElementsByTagName('img'); for (var i = 0; i < images.length; i++) { if (images[i].hasAttribute('data-src')) { images[i].setAttribute('src', images[i].getAttribute('data-src')); images[i].removeAttribute('data-src'); } } })();", null)

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
        }

        swipeRefreshLayout.setOnRefreshListener { webView.reload() }
        webView.loadUrl("https://www.gazzetta.gr/gmotion/formula1")
        return rootView
    }

    private fun applyCleanScript(view: WebView?) {
        val cleanScript = """
    (function() {
        var style = document.getElementById('clean-style') || document.createElement('style');
        style.id = 'clean-style';
        document.head.appendChild(style);
        
        style.innerHTML = `
            /* Στόχευση μόνο πραγματικών διαφημιστικών θέσεων του Gazzetta */
            .advertising, .ad-slot, .dfp-ad, .outbrain-ad, 
            [id^="div-gpt-ad"], .gnt-ad, .ad-container,
            .recommender-container, .widget-video-playlist { 
                display: none !important; 
                height: 0 !important; 
                visibility: hidden !important;
            }

            /* Επαναφορά στοιχείων που μπορεί να κρύφτηκαν κατά λάθος */
            header, .header, .main-content { 
                display: block !important; 
                visibility: visible !important; 
            }

            /* Διόρθωση πλάτους για Gazzetta */
            body, html { 
                overflow-x: hidden !important; 
                width: 100% !important; 
                margin: 0 !important;
                padding: 0 !important;
            }
        `;

        // Πιο προσεκτικό "μάζεμα" - Μόνο αν η κλάση ξεκινάει ή είναι ακριβώς 'ad-'
        var allDivs = document.querySelectorAll('div');
        allDivs.forEach(function(div) {
            var cls = div.className || "";
            if (typeof cls === 'string') {
                // Ψάχνουμε συγκεκριμένα patterns και όχι απλά τη λέξη "ad"
                if (cls.includes('google-ad') || cls.includes('ad-manager') || cls.includes('banner-wrapper')) {
                    div.style.display = 'none';
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
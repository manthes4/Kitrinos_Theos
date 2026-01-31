package com.example.kitrinostheos

import android.os.Bundle
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

class Section7Fragment : Fragment(), WebViewReloadable {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val adKeywords = listOf(
        "googleads", "doubleclick", "pagead", "googlesyndication",
        "adservice", "taboola", "outbrain", "facebook.com/tr/", "adsbygoogle",
        "pabidding", "cleverpush", "smartadserver", "crashlytics", "glomex", "smartadserver"
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
        webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        webSettings.loadsImagesAutomatically = true

        // Απενεργοποιημένο για να μην ξεχειλώνει
        webSettings.useWideViewPort = false
        webSettings.loadWithOverviewMode = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setSupportMultipleWindows(true)

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
                swipeRefreshLayout.isEnabled = webView.scrollY == 0 && y < screenHeight * 0.5f
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

                // Τρέχουμε το καθάρισμα
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

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()

                // Επιτρέπουμε στο WebView να χειριστεί μόνο του τη ροή
                // Αν επιστρέψουμε false, το WebView αναλαμβάνει να φορτώσει τη σελίδα κανονικά
                return false
            }
        }

        swipeRefreshLayout.setOnRefreshListener { webView.reload() }
        webView.loadUrl("https://www.sport24.gr/formula-1")
        return rootView
    }

    private fun applyCleanScript(view: WebView?) {
        val cleanScript = """
    (function() {
        var style = document.getElementById('clean-style') || document.createElement('style');
        style.id = 'clean-style';
        document.head.appendChild(style);
        
        style.innerHTML = `
            /* 1. Εξαφάνιση μόνο των γνωστών ad-slots */
            div[id*="div-gpt-ad"], .advertising-slot, .mapped-ad, 
            [data-testid="ad-placeholder"], .ad-impact, .content-ad {
                display: none !important;
                height: 0 !important;
                min-height: 0 !important;
                max-height: 0 !important;
                margin: 0 !important;
                padding: 0 !important;
                overflow: hidden !important;
            }

            /* 2. Διασφάλιση ότι το άρθρο θα είναι ΠΑΝΤΑ ορατό */
            article, .article-body, .article-main, .main-content, .article-header {
                display: block !important;
                visibility: visible !important;
                opacity: 1 !important;
                height: auto !important;
                max-height: none !important;
            }
        `;

        var cleanSafe = function() {
            var allDivs = document.querySelectorAll('div, section, aside');
            allDivs.forEach(function(el) {
                // Ελέγχουμε αν το στοιχείο είναι μέρος του άρθρου
                var isArticlePart = el.closest('article') || el.className.includes('article') || el.className.includes('content');
                
                // Αν ΔΕΝ είναι μέρος του άρθρου και "μυρίζει" διαφήμιση
                if (!isArticlePart) {
                    var h = el.offsetHeight;
                    var hasAdInfo = el.id.includes('ad') || el.className.includes('ad');
                    
                    // Μόνο αν είναι κενό πλαίσιο ή ad-slot
                    if (hasAdInfo && h > 100) {
                        el.style.display = 'none';
                    }
                }
            });
        };

        cleanSafe();
        setTimeout(cleanSafe, 1000);
        setTimeout(cleanSafe, 3000);
    })();
""".trimIndent()
        view?.evaluateJavascript(cleanScript, null)
    }

    override fun reloadWebView() = webView.reload()
    override fun getWebView(): WebView = webView
}
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

class Section1Fragment : Fragment(), WebViewReloadable {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_webview, container, false)

        // Initialize views
        webView = rootView.findViewById(R.id.webView)
        progressBar = rootView.findViewById(R.id.progressBar)
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout)

        // WebView settings
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true // Required for mutation observer
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK // Χρήση cache αν υπάρχει, αλλιώς δίκτυο
        webSettings.loadsImagesAutomatically = true // Μην περιμένεις το τέλος της σελίδας για τις εικόνες
        webSettings.javaScriptCanOpenWindowsAutomatically = false
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.setSupportZoom(true)
        webView.setInitialScale(140)
        webSettings.textZoom = 150
        webSettings.useWideViewPort = false
        webSettings.loadWithOverviewMode = true
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // WebViewClient for handling page loading and ad blocking
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false

                // Ελαφρύ script: Μόνο κρύψιμο, όχι MutationObserver (που τρώει CPU)
                val cleanScript = """
                    (function() {
                        var style = document.createElement('style');
                        style.innerHTML = '.ad, .advert, .ad-container, .adsbygoogle, #at-cv-lightbox-button-holder { display: none !important; }';
                        document.head.appendChild(style);
                        
                        // Lazy load images αν υπάρχουν ακόμα
                        var images = document.querySelectorAll('img[data-src]');
                        images.forEach(img => {
                            img.src = img.getAttribute('data-src');
                            img.removeAttribute('data-src');
                        });
                    })();
                """.trimIndent()
                view?.evaluateJavascript(cleanScript, null)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e("WebView", "Error loading: ${error?.description}")
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null

                // Ταχύτατος έλεγχος χωρίς Regex
                for (keyword in adKeywords) {
                    if (url.contains(keyword)) {
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.startsWith("http")) {
                    view?.loadUrl(url)
                }
                return true
            }
        }

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // Enable swipe only at the top
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

        // Preload critical resources and DNS prefetch
        webView.loadUrl(
            """
            javascript:(function() {
                document.write('<link rel="preload" href="https://allaboutaris.gr/wp-content/themes/theme/style.css" as="style">');
                var domains = ['allaboutaris.gr', 'cdn.jsdelivr.net'];
                domains.forEach(function(domain) {
                    var link = document.createElement('link');
                    link.rel = 'dns-prefetch';
                    link.href = '//' + domain;
                    document.head.appendChild(link);
                });
            })();
            """.trimIndent()
        )

        // Load the initial URL
        webView.loadUrl("https://allaboutaris.gr/tag/%CE%B1%CF%81%CE%B7%CF%83/")
        return rootView
    }

    // Κράτα μόνο τα απολύτως απαραίτητα patterns
    private val adKeywords = listOf(
        "googleads", "doubleclick", "pagead", "googlesyndication",
        "adservice", "taboola", "outbrain", "facebook.com/tr/"
    )

    private fun isNetworkFast(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI
    }

    override fun reloadWebView() {
        webView.reload()
    }

    override fun getWebView(): WebView {
        return webView
    }
}
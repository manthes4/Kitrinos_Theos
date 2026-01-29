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

    // Define patterns or URLs to block (e.g., common ad networks)
    private val adUrlPatterns = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "adservice.google.com",
        "adclick.g.doubleclick.net",
        "ads.yahoo.com",
        "pagead2.googlesyndication.com", // Added common ad domain
        "googletagservices.com", // Another common one
        "adzerk.net" // Add more as needed
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_webview, container, false)

        webView = rootView.findViewById(R.id.webView)
        progressBar = rootView.findViewById(R.id.progressBar)
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout)

        // WebView settings
        // WebView settings
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        // Remove or comment out the following line as setAppCacheEnabled is deprecated
        // webSettings.setAppCacheEnabled(true)
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.javaScriptCanOpenWindowsAutomatically = false
        webSettings.loadsImagesAutomatically = true // Can be adjusted based on user preference or page content

        // Enable zoom controls and gestures
        webSettings.builtInZoomControls = true  // Show zoom buttons
        webSettings.displayZoomControls = false // Hide default zoom controls (optional)
        webSettings.setSupportZoom(true)        // ✅ Correct

        // Enable hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // WebViewClient for managing loading
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                // Lazy load images if they have 'data-src' attribute
                view?.evaluateJavascript("javascript:(function() { var images = document.getElementsByTagName('img'); for (var i = 0; i < images.length; i++) { if (images[i].hasAttribute('data-src')) { images[i].setAttribute('src', images[i].getAttribute('data-src')); images[i].removeAttribute('data-src'); } } })();", null)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                // Handle error, perhaps show a custom error page or a retry mechanism
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (request != null && isAdUrl(request.url.toString()) && request.isForMainFrame.not()) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // Enable/Disable swipe based on scroll position and touch location
        webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            swipeRefreshLayout.isEnabled = scrollY == 0
        }

        swipeRefreshLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val y = event.rawY
                val screenHeight = resources.displayMetrics.heightPixels
                swipeRefreshLayout.isEnabled = webView.scrollY == 0 && y < screenHeight * 0.35
            }
            false
        }

        // Preload critical resources
        webView.loadUrl("javascript:(function() { document.write('<link rel=\"preload\" href=\"your-critical-resource.css\" as=\"style\">'); })();")

        // DNS Prefetching
        webView.loadUrl("javascript:(function() { var link = document.createElement('link'); link.rel = 'dns-prefetch'; link.href = 'www.sport24.gr'; document.head.appendChild(link); })();")

        // Load the initial URL
        webView.loadUrl("https://www.sport24.gr/formula-1")
        return rootView
    }

    // Check if the URL matches known ad patterns
    private fun isAdUrl(url: String): Boolean {
        return adUrlPatterns.any { url.contains(it) }
    }

    // Implement the reloadWebView method
    override fun reloadWebView() {
        webView.reload()
    }

    // Implement the getWebView method
    override fun getWebView(): WebView {
        return webView
    }
}
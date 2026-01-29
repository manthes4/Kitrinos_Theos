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

class Section2Fragment : Fragment(), WebViewReloadable {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // Ad URL patterns for blocking network requests
    private val adUrlPatterns = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "adservice.google.com",
        "adclick.g.doubleclick.net",
        "ads.yahoo.com",
        "pagead2.googlesyndication.com",
        "googletagservices.com",
        "adzerk.net",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.com",
        "adnxs.com",
        "criteo.com",
        "amazon-adsystem.com",
        "facebook.com/tr/",
        "taboola.com",
        "outbrain.com",
        "adsbygoogle.js"
    )
    private val adUrlRegex = Regex(adUrlPatterns.joinToString("|") { Regex.escape(it) })

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
        webSettings.cacheMode = if (isNetworkFast()) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
        webSettings.loadsImagesAutomatically = isNetworkFast()
        webSettings.javaScriptCanOpenWindowsAutomatically = false
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.setSupportZoom(true)
        webView.setInitialScale(140)
        webSettings.textZoom = 140
        webSettings.useWideViewPort = false
        webSettings.loadWithOverviewMode = true
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // WebViewClient for handling page loading and ad blocking
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                swipeRefreshLayout.isRefreshing = false
                Log.d("WebView", "Started loading: $url at ${System.currentTimeMillis()}")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                // Enable images after page load
                view?.settings?.loadsImagesAutomatically = true
                // Lazy load images, remove ad containers, and collapse empty parents
                view?.evaluateJavascript(
                    """
                    (function() {
                        // Lazy load images
                        var images = document.getElementsByTagName('img');
                        for (var i = 0; i < images.length; i++) {
                            if (images[i].hasAttribute('data-src')) {
                                images[i].setAttribute('src', images[i].getAttribute('data-src'));
                                images[i].removeAttribute('data-src');
                            }
                        }
                        // Define ad selectors
                        var adSelectors = [
                            'ins.adsbygoogle',
                            '.ad, .advert, .ad-container, .ad-slot, .ad-unit, .ad-block, .ad-banner, .adsbygoogle',
                            '[data-ad-client]', // Google Ads
                            '[data-ad-slot]',   // Google Ads
                            '.g-ad, .google-ad, .google_ads, .ad-wrapper',
                            'div[id*="banner"], div[class*="banner"]', // Common banner containers
                            'iframe[src*="googleads"], iframe[src*="doubleclick"], iframe[src*="ads"]',
                            '.ad-leaderboard, .ad-rectangle, .ad-skyscraper' // Additional ad formats
                        ].join(',');
                        // Remove ad elements and log for debugging
                        var adElements = document.querySelectorAll(adSelectors);
                        adElements.forEach(function(el) {
                            console.log('Removing ad element: ' + (el.id || el.className || el.tagName));
                            el.parentNode.removeChild(el);
                        });
                        // Collapse empty parent containers
                        var style = document.createElement('style');
                        style.innerHTML = `
                            .ad-container:empty, .ad-slot:empty, .ad-unit:empty, .ad-block:empty, 
                            .ad-banner:empty, .adsbygoogle:empty, .ad-wrapper:empty,
                            .ad-leaderboard:empty, .ad-rectangle:empty, .ad-skyscraper:empty {
                                display: none !important;
                                height: 0 !important;
                                margin: 0 !important;
                                padding: 0 !important;
                            }
                        `;
                        document.head.appendChild(style);
                        // Mutation observer for dynamically loaded ads
                        var observer = new MutationObserver(function(mutations) {
                            mutations.forEach(function(mutation) {
                                mutation.addedNodes.forEach(function(node) {
                                    if (node.nodeType === 1) { // Element nodes only
                                        if (node.matches(adSelectors) || node.querySelector(adSelectors)) {
                                            console.log('Dynamic ad removed: ' + (node.id || node.className || node.tagName));
                                            node.remove();
                                        }
                                    }
                                });
                            });
                        });
                        // Observe the entire body (adjust to specific container if known)
                        var contentContainer = document.body || document;
                        observer.observe(contentContainer, { childList: true, subtree: true });
                    })();
                    """.trimIndent(),
                    null
                )
                Log.d("WebView", "Finished loading: $url at ${System.currentTimeMillis()}")
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e("WebView", "Error loading: ${error?.description}")
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (request != null) {
                    val url = request.url.toString()
                    if (adUrlRegex.containsMatchIn(url) || (url.endsWith(".js") && "ads" in url)) {
                        Log.d("WebView", "Blocked ad URL: $url")
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            // Handle subpage navigation for SPA or AJAX
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return true // Handle navigation in WebView
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

        // Preload critical resources and DNS prefetch for allaboutaris.gr
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
        webView.loadUrl("https://allaboutaris.gr/")
        return rootView
    }

    private fun isAdUrl(url: String): Boolean {
        return adUrlRegex.containsMatchIn(url)
    }

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
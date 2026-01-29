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

    private val adUrlRegex = Regex(
        "doubleclick.net/.*(trackimp|dcmads|pfadx)|" +
                "googlesyndication.com/.*(adsbygoogle|omsdk|pagead)|" +
                "googletagservices.com/.*dcmads|" +
                "adservice.google.com|" +
                "adclick.g.doubleclick.net|" +
                "ads.yahoo.com|" +
                "adzerk.net|" +
                "gazzetta.adman.gr|" +
                "x.grxchange.gr|" +
                "c.bannerflow.net|" +
                "srvsynd.com"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_webview, container, false)

        webView = rootView.findViewById(R.id.webView)
        progressBar = rootView.findViewById(R.id.progressBar)
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout)

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.loadsImagesAutomatically = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.setSupportZoom(true)
        webSettings.userAgentString =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.6668.81 Mobile Safari/537.36"

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
                swipeRefreshLayout.isRefreshing = false
                Log.d("WebView", "Started loading: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                Log.d("WebView", "Finished loading: $url")

                // Block and hide common ad elements
                view?.evaluateJavascript(
                    """
                    (function() {
                        var selectors = [
                            'div[id*="ad"]',
                            'div[class*="ad"]',
                            'ins[class*="ads"]',
                            '[data-ad]',
                            '[data-ad-slot]',
                            '.ad-banner',
                            '.top-banner',
                            '.header-ad',
                            '.sponsored',
                            '.google-auto-placed'
                        ];
                        var ads = document.querySelectorAll(selectors.join(', '));
                        ads.forEach(function(ad) {
                            ad.style.display = 'none';
                            ad.style.margin = '0px';
                            ad.style.padding = '0px';
                            ad.style.height = '0px';
                            ad.innerHTML = '';
                            if (ad.parentElement && ad.parentElement.children.length <= 1) {
                                ad.parentElement.style.display = 'none';
                                ad.parentElement.style.height = '0px';
                                ad.parentElement.style.margin = '0px';
                            }
                        });
                        console.log("Ad containers hidden");
                    })();
                    """.trimIndent(), null
                )

                // Shift content upward to hide remaining ad frame
                view?.evaluateJavascript(
                    """
                    (function() {
                        var mainContent = document.querySelector('#wrapper, #page, .main, body');
                        if (mainContent) {
                            mainContent.style.position = 'relative';
                            mainContent.style.top = '-100px';
                            console.log("Main content shifted up");
                        }
                        document.body.style.marginTop = '0px';
                        document.body.style.paddingTop = '0px';
                    })();
                    """.trimIndent(), null
                )

                // Lazy-load images
                view?.evaluateJavascript(
                    """
                    (function() {
                        var images = document.getElementsByTagName('img');
                        for (var i = 0; i < images.length; i++) {
                            if (images[i].hasAttribute('data-src')) {
                                images[i].setAttribute('src', images[i].getAttribute('data-src'));
                                images[i].removeAttribute('data-src');
                            }
                        }
                    })();
                    """.trimIndent(), null
                )

                // Ensure comment sections are visible
                view?.evaluateJavascript(
                    """
                    (function() {
                        var style = document.createElement('style');
                        style.innerHTML = `
                            #myGazzettaContent, .comments_wrapper, #myGazzettaForm, .comments_list, .comments_pager {
                                display: block !important;
                                visibility: visible !important;
                                opacity: 1 !important;
                                position: relative !important;
                                height: auto !important;
                                overflow: visible !important;
                            }
                        `;
                        document.head.appendChild(style);
                    })();
                    """.trimIndent(), null
                )
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Log.e("WebView", "Error loading: ${request?.url} - ${error?.description}")
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                Log.e("WebView", "Legacy error: $failingUrl - $description")
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                request?.url?.toString()?.let { url ->
                    if (!url.contains("gazzetta.gr") && isAdUrl(url)) {
                        Log.d("WebView", "Blocked ad URL: $url")
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) progressBar.visibility = View.GONE
            }
        }

        swipeRefreshLayout.setOnRefreshListener { webView.reload() }

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

        webView.loadUrl("https://www.gazzetta.gr/gmotion/formula1")
        return rootView
    }

    private fun isAdUrl(url: String): Boolean {
        return adUrlRegex.containsMatchIn(url)
    }

    override fun reloadWebView() {
        webView.reload()
    }

    override fun getWebView(): WebView {
        return webView
    }
}
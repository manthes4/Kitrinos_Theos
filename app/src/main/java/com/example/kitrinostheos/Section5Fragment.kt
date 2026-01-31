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

class Section5Fragment : Fragment(), WebViewReloadable {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val adKeywords = listOf(
        "googleads", "doubleclick", "pagead", "googlesyndication",
        "adservice", "taboola", "outbrain", "facebook.com/tr/", "adsbygoogle",
        "smartadserver", "flashscore-gr.com", "livesport.services", "ls-ad",
        "gemius.pl", "hotjar", "bluekai"
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
        webView.loadUrl("https://yellowradio.gr/")
        return rootView
    }

    private fun applyCleanScript(view: WebView?) {
        val cleanScript = """
    (function() {
        var style = document.getElementById('clean-style') || document.createElement('style');
        style.id = 'clean-style';
        document.head.appendChild(style);
        
        style.innerHTML = `
            /* 1. Εξαφάνιση ΟΛΩΝ των διαφημιστικών θέσεων του Newspaper Theme */
            .td-header-ad-wrap, .td-all-devices-ad, .td-a-ad, 
            .td-spot-id-header_ad, .td-spot-id-sidebar, .td-spot-id-post_ad,
            div[id^="td_uid_"], div[class*="td-a-ad"],
            #premium-ads, .interstitial-ad, .video-ads,
            
            /* 2. Στόχευση λέξεων-κλειδιών για καζίνο/στοίχημα */
            div[class*="casino"], div[id*="casino"], 
            div[class*="bet-"], div[id*="bet-"],
            iframe[src*="bet"], iframe[src*="casino"] { 
                display: none !important; 
                height: 0 !important; 
                visibility: hidden !important;
                pointer-events: none !important;
            }

            /* 3. Σταμάτημα των κινουμένων εικόνων (GIF/Animations) */
            img[src$=".gif"] { display: none !important; }
            
            /* 4. Κλείδωμα της σελίδας στην κορυφή */
            html, body { 
                margin-top: 0 !important; 
                top: 0 !important; 
                padding-top: 0 !important;
                position: relative !important;
                overflow-x: hidden !important;
            }

            /* 5. Αφαίρεση του κενού που αφήνει το slide-down banner */
            .td-main-content-wrap { margin-top: 0 !important; }
        `;

        // 5. "Ο Εξολοθρευτής" - Τρέχει συνεχώς για 8 δευτερόλεπτα
        var counter = 0;
        var killer = setInterval(function() {
            // Μηδενίζουμε το padding που βάζουν τα scripts της διαφήμισης
            document.body.style.paddingTop = "0px";
            document.body.style.marginTop = "0px";
            
            // Διαγραφή στοιχείων που περιέχουν ύποπτες λέξεις
            var elements = document.querySelectorAll('div, ins, iframe');
            elements.forEach(function(el) {
                var info = (el.className + el.id).toLowerCase();
                if (info.includes('casino') || info.includes('bet') || info.includes('banner')) {
                    if (!info.includes('wrapper') && !info.includes('container')) {
                        el.style.display = 'none';
                    }
                }
            });

            counter++;
            if (counter > 16) clearInterval(killer); // Σταματάει μετά από 8 δευτερόλεπτα
        }, 500);
    })();
    """.trimIndent()
        view?.evaluateJavascript(cleanScript, null)
    }

    override fun reloadWebView() = webView.reload()
    override fun getWebView(): WebView = webView
}
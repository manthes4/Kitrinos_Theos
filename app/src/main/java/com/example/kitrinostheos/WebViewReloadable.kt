package com.example.kitrinostheos

import android.webkit.WebView

interface WebViewReloadable {
    fun reloadWebView()
    fun getWebView(): WebView // Ensure this method is defined in the interface
}
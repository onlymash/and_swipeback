package com.xbeats.swipebacksample

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.app
        setContentView(R.layout.activity_main)
        val button = findViewById<Button>(R.id.swipe_btn)
        button.setOnClickListener {
            page = 1
            startActivity(Intent(this@MainActivity, SwipeActivity::class.java))
        }
        
        webView = findViewById(R.id.webView)
        webView.apply {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return false
                }
            }
            loadUrl("https://www.google.com/")
        }

        button.postDelayed({ button.text = "测试22" }, 2000)

        button.postDelayed({ button.text = "测试44" }, 4000)

        button.postDelayed({ button.text = "测试66" }, 6000)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    companion object {
        var page = 1
    }
}

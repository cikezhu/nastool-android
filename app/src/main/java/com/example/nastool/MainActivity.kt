package com.example.nastool

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {
    private val mTAG: String = "MainActivity"
    private lateinit var myWebView: WebView
    private lateinit var myImageLogo: ImageView
    private lateinit var myChangeUrl: Button
    private lateinit var myTextUrl: TextView
    private lateinit var myInputUrl: EditText
    private lateinit var mySharedPreferences: SharedPreferences
    private val mScope = MainScope()
    private val buttonTextChange: String = "更改网址"
    private val buttonTextSave: String = "保存更改"
    var hostUrl: String = "" //"http://192.168.137.1:3000"
    val statusBarHeight: Int = 40
    var toCssTop: Int = 5
    var firstUpdated: Boolean = true


    @SuppressLint("MissingInflatedId", "CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 浏览器后退
        if (keyCode == KeyEvent.KEYCODE_BACK && myWebView.canGoBack()) {
            myWebView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun hideBgLogo() {
        // 隐藏logo界面
        val logo: LinearLayout = findViewById(R.id.bg_logo)
        logo.visibility = View.GONE
    }

    private fun showChangeLayout() {
        // 界面处理
        myInputUrl.visibility = View.VISIBLE
        myTextUrl.visibility = View.VISIBLE
        myChangeUrl.text = buttonTextSave
        myChangeUrl.visibility = View.VISIBLE
        myImageLogo.visibility = View.GONE
        // 停止加载
        myWebView.stopLoading()
        // 恢复初始
        firstUpdated = true
    }

    private fun hideChangeLayout() {
        // 界面处理
        myInputUrl.visibility = View.GONE
        myTextUrl.visibility = View.GONE
        myChangeUrl.text = buttonTextChange
        myChangeUrl.visibility = View.GONE
        myImageLogo.visibility = View.VISIBLE
    }

    private fun showChangeButton() {
        // 界面处理
        myChangeUrl.text = buttonTextChange
        myChangeUrl.visibility = View.VISIBLE
    }

    private fun gotoUrl() {
        // 如果存在hostUrl 则载入页面 否则显示输入网址的组件
        if (hostUrl != "") {
            myWebView.loadUrl(hostUrl)
            showDelay()
        } else {
            showChangeLayout()
        }
    }

    private fun showDelay() {
        // 4秒后如果还未载入成功, 显示更改按钮
        if (hostUrl != "") {
            mScope.launch(Dispatchers.Main) {
                delay(4000)
                showChangeButton()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initialize() {
        // 读取配置
        mySharedPreferences = getSharedPreferences("nasTool", MODE_PRIVATE)
        hostUrl = mySharedPreferences.getString("hostUrl", "") ?: return

        // 绑定组件
        myImageLogo = findViewById(R.id.image_logo)
        myChangeUrl = findViewById(R.id.button_change_url)
        myChangeUrl.text = buttonTextChange
        myTextUrl = findViewById(R.id.text_url)
        myInputUrl = findViewById(R.id.input_url)
        myChangeUrl.setOnClickListener {
            Log.d(mTAG, "initialize: " + myChangeUrl.text.toString())
            // 隐藏输入法
            val inputMethodManager: InputMethodManager =
                applicationContext.getSystemService(
                    INPUT_METHOD_SERVICE
                ) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(myInputUrl.windowToken, 0)
            // 逻辑处理
            if (myChangeUrl.text.toString() == buttonTextChange) {
                // 更改网址时 显示输入框
                showChangeLayout()
            } else if (myChangeUrl.text.toString() == buttonTextSave) {
                // 保存
                hostUrl = myInputUrl.text.toString()
                if (hostUrl != "") {
                    hideChangeLayout()
                    mySharedPreferences.edit().putString("hostUrl", hostUrl).apply()
                    gotoUrl()
                    Toast.makeText(applicationContext,"正在连接..",Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext,"请输入网址> http(s)://....",Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 配置 web view
        myWebView = findViewById(R.id.webview_ding)
        val settings: WebSettings = myWebView.settings // webView 配置项
        settings.useWideViewPort = true // 是否启用对视口元标记的支持
        settings.javaScriptEnabled = true // 是否启用 JavaScript
        settings.domStorageEnabled = true // 是否启用本地存储（允许使用 localStorage 等）
        settings.allowFileAccess = true // 是否启用文件访问
        //val appCachePath = applicationContext.cacheDir.absolutePath // 缓存地址
        settings.allowContentAccess = true // 是否启用内容 URL 访问
        settings.javaScriptCanOpenWindowsAutomatically = true // 是否允许 JS 弹窗
        settings.mediaPlaybackRequiresUserGesture = false // 是否需要用户手势来播放媒体
        settings.loadWithOverviewMode = true // 是否以概览模式加载页面，即按宽度缩小内容以适应屏幕
        settings.builtInZoomControls = true // 是否应使用其内置的缩放机制
        // Hide the zoom controls for HONEYCOMB+
        settings.displayZoomControls = false  // 是否应显示屏幕缩放控件
        settings.allowFileAccessFromFileURLs = true // 是否应允许在文件方案 URL 上下文中运行的 JavaScript 访问来自其他文件方案 URL 的内容
        settings.allowUniversalAccessFromFileURLs = true // 是否应允许在文件方案URL上下文中运行的 JavaScript 访问任何来源的内容
        //myWebView.setDrawingCacheEnabled(true) // 启用或禁用图形缓存
        myWebView.webViewClient = WVViewClient(this, this@MainActivity) // 帮助 WebView 处理各种通知、请求事件
        myWebView.webChromeClient = WVChromeClient(this, this@MainActivity) // 处理解析，渲染网页
        gotoUrl()
    }
}

private class WVViewClient(private val _context: Context, private val _m: MainActivity):
    WebViewClient() {
    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.proceed()
    }

    // 非hostUrl从外部浏览器打开
    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (Uri.parse(url).host?.let { _m.hostUrl.indexOf(it) }!! > -1) {
            return false
        }
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            if (view != null) {
                startActivity( _context, this, null)
            }
        }
        return true
    }

    //页面访问出错
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        // 未加载成功 禁止关闭logo图
        _m.firstUpdated = false
    }

    //页面加载完成
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        // 设定安全top边距
        if (_m.toCssTop > 0) {
            _m.toCssTop = _m.toCssTop - 1
            view?.loadUrl("javascript:" +
                    "document.getElementsByTagName('body')[0].style.setProperty('--safe-area-inset-top', '" +
                    _m.statusBarHeight +
                    "px');")
        }
        // 关闭logo图
        if (_m.firstUpdated) {
            _m.firstUpdated = false
            _m.hideBgLogo()
        }
    }

}

class WVChromeClient(private val _context: Context, private val _m: MainActivity) :
    WebChromeClient() {
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        Log.d("WVChromeClient", "newProgress：$newProgress")
    }

    // 标题变更时恢复toCssTop变量
    override fun onReceivedTitle(view: WebView?, title: String) {
        super.onReceivedTitle(view, title)
        Log.d("WVChromeClient", "onReceivedTitle: $title")
        _m.toCssTop = 5
    }

}
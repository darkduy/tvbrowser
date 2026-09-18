package com.duyvietnambeta.tvbrowser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.duyvietnambeta.tvbrowser.databinding.ActivityMainBinding

/**
 * Màn hình duy nhất của app: một trình duyệt web đơn giản tối ưu cho Android TV.
 *
 * Thiết kế tập trung vào điều khiển bằng D-pad (remote control):
 * - Các nút điều hướng (Lùi/Tới/Tải lại/Trang chủ) và ô nhập URL đều focusable,
 *   nối tiếp nhau qua nextFocus trong layout XML.
 * - Phím BACK vật lý trên remote sẽ lùi lại trang web trước đó thay vì thoát app ngay,
 *   trừ khi không còn lịch sử để lùi.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupToolbarActions()

        // Tải trang chủ mặc định khi mở app lần đầu
        loadUrl(getString(R.string.default_home_url))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            // Tăng kích thước chữ mặc định một chút vì người dùng ngồi xa TV
            textZoom = 110
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
                url?.let { binding.editUrl.setText(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE
                updateNavButtonsState()
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@MainActivity,
                    "Lỗi tải trang: $description",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupToolbarActions() {
        binding.btnBack.setOnClickListener { goBack() }
        binding.btnForward.setOnClickListener { goForward() }
        binding.btnReload.setOnClickListener { binding.webView.reload() }
        binding.btnHome.setOnClickListener { loadUrl(getString(R.string.default_home_url)) }
        binding.btnGo.setOnClickListener { submitUrlFromInput() }

        // Cho phép bấm nút "Đi" trên bàn phím ảo hoặc phím Enter để điều hướng
        binding.editUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                submitUrlFromInput()
                true
            } else {
                false
            }
        }
    }

    /** Chuẩn hóa chuỗi nhập của người dùng thành URL hợp lệ rồi tải trang. */
    private fun submitUrlFromInput() {
        val rawInput = binding.editUrl.text.toString().trim()
        if (rawInput.isEmpty()) return
        loadUrl(normalizeToUrl(rawInput))
    }

    private fun normalizeToUrl(input: String): String {
        val looksLikeUrl = input.contains(".") && !input.contains(" ")
        return when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            looksLikeUrl -> "https://$input"
            else -> "https://www.google.com/search?q=${input.replace(" ", "+")}"
        }
    }

    private fun loadUrl(url: String) {
        binding.webView.loadUrl(url)
    }

    private fun goBack() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        }
    }

    private fun goForward() {
        if (binding.webView.canGoForward()) {
            binding.webView.goForward()
        }
    }

    /** Ẩn/hiện nút Lùi/Tới tùy theo lịch sử duyệt web hiện có, tránh người dùng bấm vô ích. */
    private fun updateNavButtonsState() {
        binding.btnBack.isEnabled = binding.webView.canGoBack()
        binding.btnForward.isEnabled = binding.webView.canGoForward()
    }

    /**
     * Ghi đè phím BACK vật lý trên remote: ưu tiên lùi trang web,
     * chỉ thoát app khi không còn lịch sử duyệt web nào để lùi về.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
            goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}

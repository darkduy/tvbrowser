# Giữ lại các JS interface nếu sau này thêm WebView JS bridge
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

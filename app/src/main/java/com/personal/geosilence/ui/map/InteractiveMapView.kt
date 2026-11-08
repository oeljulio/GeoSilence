package com.personal.geosilence.ui.map

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * Mapa interactivo con OpenStreetMap y Leaflet. Permite escoger el centro de la zona
 * arrastrando el pin o tocando el mapa, y muestra el radio configurado.
 * Solo contiene el WebView: el tamaño y la decoración los decide el contenedor.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveMapView(
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    val currentOnLocationSelected by rememberUpdatedState(onLocationSelected)

    // Actualización dinámica del radio y encuadre del círculo
    LaunchedEffect(radiusMeters, isMapLoaded) {
        if (isMapLoaded) {
            webViewRef?.evaluateJavascript("if (window.setRadius) { window.setRadius($radiusMeters); }", null)
            delay(400)
            webViewRef?.evaluateJavascript("if (window.fitCircle) { window.fitCircle(); }", null)
        }
    }

    // Actualización de coordenadas cuando cambian desde fuera
    LaunchedEffect(latitude, longitude, isMapLoaded) {
        if (isMapLoaded && latitude != 0.0 && longitude != 0.0) {
            webViewRef?.evaluateJavascript(
                "if (window.setLocation) { window.setLocation($latitude, $longitude, true); }",
                null
            )
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(Color.parseColor("#F1F3F4"))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false

                // Permitir arrastre y zoom en el mapa sin que un scroll padre lo intercepte
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val uri = request?.url ?: return null
                        if (uri.host == "geosilence.app") {
                            val path = uri.path ?: return null
                            if (path.startsWith("/leaflet/")) {
                                return try {
                                    val assetPath = path.removePrefix("/")
                                    val inputStream = ctx.assets.open(assetPath)
                                    val mimeType = when {
                                        assetPath.endsWith(".css", ignoreCase = true) -> "text/css"
                                        assetPath.endsWith(".js", ignoreCase = true) -> "application/javascript"
                                        else -> "application/octet-stream"
                                    }
                                    WebResourceResponse(mimeType, "UTF-8", inputStream)
                                } catch (e: Exception) {
                                    Log.e("InteractiveMapView", "Error sirviendo asset de Leaflet: $path", e)
                                    null
                                }
                            }
                        }
                        return null
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isMapLoaded = true
                        // Fuerza la altura del contenedor; el recálculo de Leaflet lo hacen los timers del HTML
                        view?.evaluateJavascript(
                            "if (window.forceMapSize) { window.forceMapSize(); }",
                            null
                        )
                    }
                }

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onLocationSelected(lat: Double, lng: Double) {
                            Handler(Looper.getMainLooper()).post {
                                currentOnLocationSelected(lat, lng)
                            }
                        }
                    },
                    "AndroidBridge"
                )

                // Carga local de assets de Leaflet para incrustación directa en el HTML
                val leafletJsContent = try {
                    ctx.assets.open("leaflet/leaflet.js").bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    Log.e("InteractiveMapView", "Error leyendo asset leaflet.js", e)
                    null
                }

                val leafletCssContent = try {
                    ctx.assets.open("leaflet/leaflet.css").bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    Log.e("InteractiveMapView", "Error leyendo asset leaflet.css", e)
                    null
                }

                val html = MapHtmlProvider.generateMapHtml(
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radiusMeters,
                    leafletJs = leafletJsContent,
                    leafletCss = leafletCssContent
                )
                loadDataWithBaseURL("https://geosilence.app/", html, "text/html", "UTF-8", null)
                webViewRef = this
            }
        },
        modifier = modifier.fillMaxSize(),
        onRelease = { it.destroy() }
    )
}

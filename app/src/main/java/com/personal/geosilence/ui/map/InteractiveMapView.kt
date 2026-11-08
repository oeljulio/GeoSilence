package com.personal.geosilence.ui.map

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs

/**
 * Componente Compose para el mapa interactivo con OpenStreetMap.
 * Permite seleccionar la ubicación central arrastrando el pin o tocando el mapa,
 * y muestra en tiempo real la cobertura circular del radio configurado.
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
    var debugInfo by remember { mutableStateOf("WebView: iniciando...") }

    // Actualización dinámica del radio sobre el mapa en tiempo real
    LaunchedEffect(radiusMeters) {
        if (isMapLoaded) {
            webViewRef?.evaluateJavascript("if (window.setRadius) { window.setRadius($radiusMeters); }", null)
        }
    }

    // Actualización de coordenadas cuando cambian por GPS o entrada manual
    LaunchedEffect(latitude, longitude) {
        if (isMapLoaded && latitude != 0.0 && longitude != 0.0) {
            webViewRef?.evaluateJavascript(
                "if (window.setLocation) { window.setLocation($latitude, $longitude, true); }",
                null
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            WebView.setWebContentsDebuggingEnabled(true)
                            setBackgroundColor(Color.parseColor("#F1F3F4"))
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = false
                            settings.displayZoomControls = false

                            // Permitir arrastre y zoom en el mapa sin que el scroll de Compose lo intercepte
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

                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                    Log.d("GeoMap", consoleMessage?.message() ?: "")
                                    if (consoleMessage?.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                        debugInfo += " | JS-ERR: " + consoleMessage.message()
                                    }
                                    return true
                                }
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

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        debugInfo += " | ERR-PAGINA: " + error?.description
                                    }
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    debugInfo += " | RENDERER-CAIDO crash=" + detail?.didCrash()
                                    return true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isMapLoaded = true
                                    view?.evaluateJavascript("if (window.map) { window.map.invalidateSize(); }", null)

                                    // Script de diagnóstico envuelto en IIFE que siempre devuelve un string
                                    // Incluye: location.href, readyState, typeof L, estado #map, dimensiones, UA/Chrome, #mapError
                                    val debugScript = """
                                        (function() {
                                            try {
                                                var mapEl = document.getElementById('map');
                                                var errorEl = document.getElementById('mapError');
                                                var url = document.location.href;
                                                var readyState = document.readyState;
                                                var lType = typeof L;
                                                var mapExists = mapEl !== null ? 'sí' : 'no';
                                                var mapClass = mapEl ? (mapEl.className || '').substring(0, 18) : 'null';
                                                var mapW = mapEl ? mapEl.offsetWidth : 0;
                                                var mapH = mapEl ? mapEl.offsetHeight : 0;
                                                var winW = window.innerWidth;
                                                var winH = window.innerHeight;
                                                var htmlLen = document.documentElement.outerHTML.length;
                                                var errText = errorEl ? errorEl.innerText.substring(0, 60) : 'null';
                                                var ua = navigator.userAgent;
                                                var chromeMatch = ua.match(/Chrome\/[0-9.]+/);
                                                var chromeVer = chromeMatch ? chromeMatch[0] : 'desconocido';
                                                return 'loc=' + encodeURIComponent(url) +
                                                       ' | ready=' + readyState +
                                                       ' | LType=' + lType +
                                                       ' | mapExists=' + mapExists +
                                                       ' | mapClass=' + mapClass +
                                                       ' | mapSize=' + mapW + 'x' + mapH +
                                                       ' | win=' + winW + 'x' + winH +
                                                       ' | htmlLen=' + htmlLen +
                                                       ' | mapError=' + errText +
                                                       ' | UA=' + chromeVer;
                                            } catch (e) {
                                                return 'EXC: ' + e.message;
                                            }
                                        })()
                                    """.trimIndent()

                                    // Acumular en debugInfo sin pisar lectura anterior
                                    val accumKey = "WebView: "
                                    view?.evaluateJavascript(debugScript) { result ->
                                        if (result != null && result != "sin respuesta") {
                                            debugInfo = accumKey + result + " | " + debugInfo
                                        }
                                    }

                                    Handler(Looper.getMainLooper()).postDelayed({
                                        view?.evaluateJavascript(debugScript) { result ->
                                            if (result != null && result != "sin respuesta") {
                                                debugInfo = "WebView(2s): " + result + " | " + debugInfo
                                            }
                                        }
                                    }, 2000)
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    // Acumular URL de inicio sin pisar lectura anterior
                                    val urlTruncated = url?.substring(0, Math.min(url.length, 40)) ?: "desconocido"
                                    debugInfo = "start: " + urlTruncated + " | " + debugInfo
                                }
                            }

                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onLocationSelected(lat: Double, lng: Double) {
                                        Handler(Looper.getMainLooper()).post {
                                            onLocationSelected(lat, lng)
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

                            val (leafletJs, leafletCss) = if (leafletJsContent != null && leafletCssContent != null) {
                                Pair(leafletJsContent, leafletCssContent)
                            } else {
                                Pair(null, null)
                            }

                            val html = MapHtmlProvider.generateMapHtml(
                                latitude = latitude,
                                longitude = longitude,
                                radiusMeters = radiusMeters,
                                leafletJs = leafletJs,
                                leafletCss = leafletCss
                            )
                            loadDataWithBaseURL("https://geosilence.app/", html, "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Barra informativa debajo del mapa
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PinDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Toca el mapa o arrastra el pin rojo para centrar la zona\n[DEBUG] $debugInfo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

package com.personal.geosilence.ui.map

/**
 * Proveedor de HTML5/CSS/JavaScript con Leaflet y OpenStreetMap para el mapa interactivo.
 * Soporta incrustación directa de assets CSS/JS de Leaflet, panel de diagnóstico de errores en pantalla,
 * selección de punto por toque, arrastre de pin y visualización en tiempo real del círculo de radio configurable.
 */
object MapHtmlProvider {

    fun generateMapHtml(
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        leafletJs: String? = null,
        leafletCss: String? = null
    ): String {
        val safeLat = if (latitude == 0.0 && longitude == 0.0) 40.4168 else latitude
        val safeLng = if (latitude == 0.0 && longitude == 0.0) -3.7038 else longitude

        // Si se provee el CSS en memoria se incrusta inline; de lo contrario se usa la URL de respaldo
        val cssTag = if (leafletCss != null) {
            "<style>\n$leafletCss\n</style>"
        } else {
            """<link rel="stylesheet" href="https://geosilence.app/leaflet/leaflet.css" />"""
        }

        // Si se provee el JS en memoria se incrusta inline; de lo contrario se usa el script externo de respaldo
        val jsTag = if (leafletJs != null) {
            "<script>\n$leafletJs\n</script>"
        } else {
            """<script src="https://geosilence.app/leaflet/leaflet.js"></script>"""
        }

        return """
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  $cssTag
  <style>
    * {
      -webkit-tap-highlight-color: transparent;
      user-select: none;
      -webkit-user-select: none;
      box-sizing: border-box;
    }
    /* Posicionamiento estático (sin alturas en % dependientes del padre, que se
       resuelven a 0 si el contenedor no tiene altura computable) */
    html, body {
      position: relative;
      width: 100%;
      margin: 0;
      padding: 0;
      min-height: 0;
      background-color: #f1f3f4;
      overflow: hidden;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    }
    #map {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      /* Altura fija inicialmente; se sobreescribe con window.innerHeight por JS */
      height: 280px;
      min-height: 280px;
      display: block;
      z-index: 0;
    }
    .leaflet-container {
      width: 100%;
      height: 100%;
      display: block;
    }
    .leaflet-control-attribution {
      font-size: 9px !important;
      background: rgba(255, 255, 255, 0.75) !important;
      padding: 2px 5px !important;
      border-radius: 4px;
    }
    .custom-marker {
      display: flex;
      justify-content: center;
      align-items: center;
      filter: drop-shadow(0 4px 6px rgba(0,0,0,0.35));
      cursor: grab;
    }
    .custom-marker:active {
      cursor: grabbing;
    }
    .marker-pulse {
      position: absolute;
      width: 16px;
      height: 16px;
      border-radius: 50%;
      background: rgba(220, 54, 46, 0.35);
      bottom: -6px;
      animation: pulse 2s infinite ease-out;
    }
    @keyframes pulse {
      0% { transform: scale(0.6); opacity: 1; }
      100% { transform: scale(2.6); opacity: 0; }
    }
    .badge-info {
      position: absolute;
      top: 10px;
      left: 10px;
      z-index: 1000;
      background: rgba(25, 28, 36, 0.88);
      color: #ffffff;
      padding: 6px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.3px;
      box-shadow: 0 3px 8px rgba(0,0,0,0.3);
      pointer-events: none;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .badge-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: #6750A4;
    }
  </style>
</head>
<body>
  <div id="infoBadge" class="badge-info">
    <div class="badge-dot"></div>
    <span id="badgeText">Radio: ${radiusMeters.toInt()} m</span>
  </div>
  <div id="map"></div>
  <div id="mapError" style="position: absolute; left: 8px; right: 8px; bottom: 8px; z-index: 2000; background: rgba(220, 38, 38, 0.85); color: #ffffff; font-family: monospace; font-size: 11px; max-height: 50%; overflow: auto; display: none; padding: 6px 10px; border-radius: 4px; white-space: pre-wrap;"></div>

  $jsTag
  <script>
    function showMapError(msg) {
      var errDiv = document.getElementById('mapError');
      if (errDiv) {
        errDiv.style.display = 'block';
        errDiv.innerText += msg + '\n';
      }
    }
    window.onerror = function(msg, src, line) {
      showMapError('JS: ' + msg + ' (' + line + ')');
    };

    var map;
    var marker;
    var circle;
    var currentLat = $safeLat;
    var currentLng = $safeLng;
    var currentRadius = $radiusMeters;
    var isDragging = false;

    var pinSvg = '<div class="marker-pulse"></div><svg width="34" height="44" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
      '<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#D32F2F" stroke="#FFFFFF" stroke-width="1.6"/>' +
      '<circle cx="12" cy="9" r="3" fill="#FFFFFF"/>' +
      '</svg>';

    var customIcon = L.divIcon({
      className: 'custom-marker',
      html: pinSvg,
      iconSize: [34, 44],
      iconAnchor: [17, 44],
      popupAnchor: [0, -40]
    });

    function formatRadiusLabel(meters) {
      if (meters >= 1000) {
        return (meters / 1000).toFixed(1) + ' km';
      }
      return Math.round(meters) + ' m';
    }

    function updateBadge(radius) {
      var badgeText = document.getElementById('badgeText');
      if (badgeText) {
        badgeText.innerText = 'Radio: ' + formatRadiusLabel(radius);
      }
    }

    // Fuerza con prioridad máxima el tamaño del contenedor del mapa y de html/body.
    // No reentrante: si ya está ejecutándose, la segunda llamada sale inmediatamente.
    function forceMapSize() {
      if (window.__forcing) return;
      window.__forcing = true;
      try {
        var h = (window.innerHeight > 0 ? window.innerHeight : 280) + 'px';
        var root = document.documentElement;
        if (root) {
          root.style.setProperty('height', h, 'important');
          root.style.setProperty('margin', '0', 'important');
          root.style.setProperty('padding', '0', 'important');
          root.style.setProperty('overflow', 'hidden', 'important');
        }
        if (document.body) {
          document.body.style.setProperty('height', h, 'important');
          document.body.style.setProperty('margin', '0', 'important');
          document.body.style.setProperty('padding', '0', 'important');
          document.body.style.setProperty('overflow', 'hidden', 'important');
        }
        var m = document.getElementById('map');
        if (m) {
          m.style.setProperty('position', 'absolute', 'important');
          m.style.setProperty('top', '0', 'important');
          m.style.setProperty('left', '0', 'important');
          m.style.setProperty('width', '100%', 'important');
          m.style.setProperty('height', h, 'important');
          m.style.setProperty('min-height', h, 'important');
          m.style.setProperty('display', 'block', 'important');
        }
      } finally {
        window.__forcing = false;
      }
    }

    // Recalcula el tamaño de Leaflet una sola vez por invocación, sin volver a llamarse a sí misma
    function refreshMapSize() {
      forceMapSize();
      if (window.map) window.map.invalidateSize();
    }

    function initMap() {
      if (typeof L === 'undefined') {
        showMapError('Leaflet no cargó');
        return;
      }

      // Crear el mapa antes de cualquier ajuste de tamaño o invalidateSize
      map = L.map('map', {
        zoomControl: true,
        attributionControl: true
      }).setView([currentLat, currentLng], 15);

      // Forzar altura una vez que el contenedor ya existe
      forceMapSize();

      // Recálculo diferido del tamaño: el contenedor puede no tener altura final en el primer render
      setTimeout(refreshMapSize, 0);
      setTimeout(refreshMapSize, 300);
      setTimeout(refreshMapSize, 1000);
      // Los listeners solo fuerzan el tamaño (con bandera); no disparan eventos ni invalidateSize en cascada
      window.addEventListener('resize', function() { forceMapSize(); });
      window.addEventListener('orientationchange', function() { forceMapSize(); });

      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '© OpenStreetMap'
      }).addTo(map);

      // Círculo dinámico de la zona de silencio
      circle = L.circle([currentLat, currentLng], {
        radius: currentRadius,
        color: '#6750A4',
        weight: 2,
        opacity: 0.9,
        fillColor: '#6750A4',
        fillOpacity: 0.25
      }).addTo(map);

      // Pin arrastrable para seleccionar ubicación central
      marker = L.marker([currentLat, currentLng], {
        draggable: true,
        icon: customIcon
      }).addTo(map);

      updateBadge(currentRadius);

      // Toque en el mapa para ubicar pin y zona
      map.on('click', function(e) {
        var lat = e.latlng.lat;
        var lng = e.latlng.lng;
        setMarkerAndCircle(lat, lng);
        if (window.AndroidBridge && window.AndroidBridge.onLocationSelected) {
          window.AndroidBridge.onLocationSelected(lat, lng);
        }
      });

      // Arrastre interactivo del pin
      marker.on('dragstart', function() {
        isDragging = true;
      });

      marker.on('drag', function(e) {
        var pos = marker.getLatLng();
        if (circle) {
          circle.setLatLng(pos);
        }
      });

      marker.on('dragend', function() {
        isDragging = false;
        var pos = marker.getLatLng();
        setMarkerAndCircle(pos.lat, pos.lng);
        if (window.AndroidBridge && window.AndroidBridge.onLocationSelected) {
          window.AndroidBridge.onLocationSelected(pos.lat, pos.lng);
        }
      });
    }

    function setMarkerAndCircle(lat, lng) {
      currentLat = lat;
      currentLng = lng;
      if (marker) marker.setLatLng([lat, lng]);
      if (circle) circle.setLatLng([lat, lng]);
    }

    // Funciones invocadas directamente desde Android / Jetpack Compose
    window.setRadius = function(newRadius) {
      currentRadius = newRadius;
      if (circle) {
        circle.setRadius(newRadius);
      }
      updateBadge(newRadius);
    };

    window.setLocation = function(lat, lng, pan) {
      if (isDragging) return;
      currentLat = lat;
      currentLng = lng;
      setMarkerAndCircle(lat, lng);
      if (pan && map) {
        map.panTo([lat, lng], { animate: true, duration: 0.5 });
      }
    };

    // Expuesta para que Android pueda forzar el recálculo de tamaño desde el WebView
    window.forceMapSize = forceMapSize;
    window.refreshMapSize = refreshMapSize;

    // Inicialización inmediata y robusta del mapa sin depender de window.onload.
    // Bandera para garantizar que el mapa se crea una única vez.
    var initDone = false;
    function initMapOnce() {
      if (initDone) return;
      initDone = true;
      try {
        initMap();
      } catch (e) {
        showMapError('initMap: ' + e);
      }
    }
    initMapOnce();
    window.onload = function() {
      initMapOnce();
      forceMapSize();
    };
  </script>
</body>
</html>
""".trimIndent()
    }
}

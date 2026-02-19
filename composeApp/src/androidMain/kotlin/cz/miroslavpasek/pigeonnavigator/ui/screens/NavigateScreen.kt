package cz.miroslavpasek.pigeonnavigator.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.miroslavpasek.pigeonnavigator.data.FlightLocation
import cz.miroslavpasek.pigeonnavigator.map.MapStyleProvider
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val DEFAULT_ZOOM = 10.5
private val PRAGUE = LatLng(50.0755, 14.4378)

@Composable
fun NavigateScreen(
    modifier: Modifier = Modifier,
    location: FlightLocation?,
    followUser: Boolean = true
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapStyleProvider = remember { MapStyleProvider() }

    // Initialize MapLibre once
    MapLibre.getInstance(context)

    var didSetInitialCamera by remember { mutableStateOf(false) }
    var didLoadStyle by remember { mutableStateOf(false) }
    val styleJson = remember(mapStyleProvider) { mapStyleProvider.getStyleJson() }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    DisposableEffect(lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { mv ->
            mv.getMapAsync { map ->
                if (!didLoadStyle) {
                    didLoadStyle = true
                    map.setStyle(Style.Builder().fromJson(styleJson)) {
                        map.uiSettings.isAttributionEnabled = false
                        map.uiSettings.isLogoEnabled = false

                        if (!didSetInitialCamera) {
                            didSetInitialCamera = true
                            val target = if (location != null)
                                LatLng(location.latitude, location.longitude)
                            else
                                PRAGUE
                            map.cameraPosition = CameraPosition.Builder()
                                .target(PRAGUE)
                                .zoom(DEFAULT_ZOOM)
                                .build()
                        }
                    }
                }

                if (followUser && location != null && map.style != null) {
                    val target = LatLng(location.latitude, location.longitude)
                    val newPosition = CameraPosition.Builder()
                        .target(target)
                        .zoom(map.cameraPosition.zoom)
                        .build()
                    map.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(newPosition)
                    )
                }
            }
        }
    )
}

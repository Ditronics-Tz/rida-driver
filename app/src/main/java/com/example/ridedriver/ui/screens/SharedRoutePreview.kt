package com.example.ridedriver.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ridedriver.data.remote.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
internal fun SharedRoutePreview(pickup:SharedPoint?,dropoff:SharedPoint?){
 if(pickup==null||dropoff==null)return
 var estimate by remember {mutableStateOf("Loading road-route estimate…")}
 var points by remember {mutableStateOf(emptyList<GeoPoint>())}
 LaunchedEffect(pickup,dropoff){
 try{
 val route=MapServiceFactory.osrm.route("${pickup.lng},${pickup.lat};${dropoff.lng},${dropoff.lat}").routes.firstOrNull()
 if(route==null) estimate="Road-route estimate unavailable."
 else {estimate="Road estimate: %.1f km · %.0f minutes".format(route.distance/1000,route.duration/60);points=route.geometry.coordinates.map{GeoPoint(it[1],it[0])}}
 }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){estimate="Road estimate unavailable. Review pickup and destination."}
 }
 val context=LocalContext.current
 val map=remember{MapView(context).apply{setMultiTouchControls(true);controller.setZoom(12.0);controller.setCenter(GeoPoint(pickup.lat,pickup.lng))}}
 DisposableEffect(map){map.onResume();onDispose{map.onPause();map.onDetach()}}
 AndroidView(factory={map},modifier=Modifier.fillMaxWidth().height(150.dp),update={
 map.overlays.clear()
 listOf(pickup,dropoff).forEach{p->map.overlays.add(Marker(map).apply{position=GeoPoint(p.lat,p.lng);title=p.address})}
 if(points.isNotEmpty())map.overlays.add(Polyline().apply{setPoints(points)})
 map.invalidate()
 })
 Text("© OpenStreetMap contributors")
 Text(estimate)
}

@Composable
internal fun NavigateToSharedDestination(point:SharedPoint){
 val context=LocalContext.current
 var info by remember{mutableStateOf("")}
 TextButton(onClick={
 val intent=Intent(Intent.ACTION_VIEW,Uri.parse("geo:${point.lat},${point.lng}?q=${point.lat},${point.lng}"))
 if(intent.resolveActivity(context.packageManager)!=null)context.startActivity(intent) else info="Destination: ${point.lat}, ${point.lng}"
 }){Text("Navigate to destination")}
 if(info.isNotBlank())Text(info)
}

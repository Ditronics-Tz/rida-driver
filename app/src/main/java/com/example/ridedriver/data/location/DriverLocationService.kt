package com.example.ridedriver.data.location

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.ridedriver.MainActivity
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.*
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/** Started from a visible driver screen after permission. Continues active-trip
 * tracking with a persistent notification; never restarts from a background boot. */
class DriverLocationService: Service() {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private var sending=false
 private lateinit var client:FusedLocationProviderClient
 private val callback=object:LocationCallback(){
  override fun onLocationResult(result:LocationResult){
   val p=result.lastLocation?:return
   val age=SystemClock.elapsedRealtimeNanos()-p.elapsedRealtimeNanos
   if(age !in 0..120_000_000_000L||p.accuracy>100){status.value="Waiting for a fresh, accurate GPS fix";return}
   if(sending)return
   sending=true
   scope.launch{
    try{
     if(!SessionManager.observe(this@DriverLocationService).first().loggedIn){stopSelf();return@launch}
     val response=RidaApiClient.driverApi.updateLocation(DriverLocationRequest(p.latitude,p.longitude,accuracy=p.accuracy))
     status.value=if(response.isSuccessful)"Sharing live driver location" else "Location update failed (HTTP ${response.code()})"
     if(response.code()==401||response.code()==403)stopSelf()
    }catch(e:CancellationException){throw e}catch(_:Exception){status.value="Location connection interrupted; retrying"}finally{sending=false}
   }
  }
 }
 override fun onBind(intent:Intent?):IBinder?=null
 @android.annotation.SuppressLint("MissingPermission")
 override fun onCreate(){
  super.onCreate();RidaApiClient.init(this)
  if(!hasLocation(this)){stopSelf();return}
  val manager=getSystemService(NotificationManager::class.java)
  if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel(CHANNEL,"Driver trip location",NotificationManager.IMPORTANCE_LOW))
  val intent=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val notification=NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("Rida driver location").setContentText("Sharing while online or on an active trip").setContentIntent(intent).setOngoing(true).build()
  ServiceCompat.startForeground(this,3217,notification,if(Build.VERSION.SDK_INT>=29)ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
  client=LocationServices.getFusedLocationProviderClient(this)
  client.requestLocationUpdates(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000).setMinUpdateIntervalMillis(8000).build(),callback,Looper.getMainLooper()).addOnFailureListener{status.value="Location permission or GPS unavailable";stopSelf()}
  scope.launch{
   while(isActive){
    try{
     if(!SessionManager.observe(this@DriverLocationService).first().loggedIn){stopSelf();return@launch}
     val hail=RidaApiClient.driverApi.getCurrentRide()
     val shared=RidaApiClient.transport.list("mine").trips.any{it.status in listOf("waiting","in_progress")}
     val online=RidaApiClient.driverApi.getProfile().isOnline==true
     if(hail.code()==204&&!shared&&!online){stopSelf();return@launch}
    }catch(e:CancellationException){throw e}catch(_:Exception){status.value="Checking active-trip connection…"}
    delay(15000)
   }
  }
 }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int)=START_NOT_STICKY
 override fun onDestroy(){if(::client.isInitialized)client.removeLocationUpdates(callback);scope.cancel();status.value="Driver location stopped";super.onDestroy()}
 companion object{
  private const val CHANNEL="rida_driver_location"
  private val status=MutableStateFlow("Driver location not started")
  val state=status.asStateFlow()
  fun hasLocation(context:Context)=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
  fun startFromVisibleScreen(context:Context){if(hasLocation(context)){try{ContextCompat.startForegroundService(context,Intent(context,DriverLocationService::class.java))}catch(_:RuntimeException){status.value="Open Rida to resume driver location"}}}
 }
}

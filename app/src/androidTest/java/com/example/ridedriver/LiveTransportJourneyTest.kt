package com.example.ridedriver

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlinx.coroutines.runBlocking
import org.junit.*

/** Uses a real local HTTP API and PostgreSQL. No mock network or repository. */
class LiveTransportJourneyTest {
 @get:Rule val ui=createEmptyComposeRule()
 @get:Rule val permissions=androidx.test.rule.GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION)
 private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
 private var scenario:ActivityScenario<MainActivity>?=null
 private fun retrofit(token:String?=null):Retrofit {
 val client=OkHttpClient.Builder().addInterceptor{chain->val builder=chain.request().newBuilder().header("X-Device-ID","acceptance-"+context.packageName);if(token!=null)builder.header("Authorization","Bearer $token");chain.proceed(builder.build())}.build()
 return Retrofit.Builder().baseUrl(RidaApiClient.BASE_URL).client(client).addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build())).build()
 }
 private suspend fun client(role:String):TransportApi{
 val response=retrofit().create(RidaApi::class.java).login(LoginRequest("$role@acceptance.test","RidaAcceptance123!"))
 return retrofit(response.getEffectiveAccessToken()).create(TransportApi::class.java)
 }
 private fun login(role:String)=runBlocking{RidaApiClient.init(context);SessionManager.clear(context);val response=retrofit().create(RidaApi::class.java).login(LoginRequest("$role@acceptance.test","RidaAcceptance123!"));SessionManager.saveGoogleSession(context,response)}
 private fun waitText(text:String,substring:Boolean=false){ui.waitUntil(30000){ui.onAllNodesWithText(text,substring=substring).fetchSemanticsNodes().isNotEmpty()}}
 private fun scrollText(text:String){ui.waitUntil(30000){runCatching{ui.onNodeWithTag("shared-trip-content").performScrollToNode(hasText(text,substring=true))}.isSuccess}}
 private fun place(label:String,name:String,lat:String,lng:String){
 ui.onNodeWithTag("shared-trip-content").performScrollToNode(hasTestTag("$label-query"));ui.onNodeWithTag("$label-query").performTextReplacement(name)
 ui.onNodeWithTag("$label-manual").performClick()
 ui.onNodeWithTag("shared-trip-content").performScrollToNode(hasTestTag("$label-latitude"));ui.onNodeWithTag("$label-latitude").performTextReplacement(lat)
 ui.onNodeWithTag("$label-longitude").performTextReplacement(lng)
 ui.onNodeWithTag("shared-trip-content").performScrollToNode(hasTestTag("$label-use-coordinates"));ui.onNodeWithTag("$label-use-coordinates").performClick()
 }
 @Before fun requireLocal(){Assume.assumeTrue("Run against the disposable local acceptance API",java.net.URI(RidaApiClient.BASE_URL).let{it.scheme=="http" && it.host in listOf("10.0.2.2","127.0.0.1","localhost") && it.port==3005})}
 @After fun close(){scenario?.close()}
 @Test fun publishVerifyDepartCompleteInNormalNavigation()=runBlocking {
 login("driver")
 scenario=ActivityScenario.launch(MainActivity::class.java)
 waitText("Publish or manage a shared ride")
 ui.onNodeWithText("Publish or manage a shared ride").performClick()
 waitText("Pickup")
 place("Pickup","Mbande","-6.94","39.19")
 place("Destination","Posta","-6.82","39.28")
 scrollText("Seats offered on this trip");ui.onNodeWithText("Seats offered on this trip").performTextReplacement("1")
 scrollText("Price per seat (TZS)");ui.onNodeWithText("Price per seat (TZS)").performTextReplacement("2000")
 scrollText("Minutes to wait (6–180)");ui.onNodeWithText("Minutes to wait (6–180)").performTextReplacement("20")
 scrollText("Review trip");ui.onNodeWithText("Review trip").performClick()
 ui.onNodeWithText("Publish trip").performClick()
 waitText("1 available",true)
 val trip=RidaApiClient.transport.list("mine").trips.first{it.status=="waiting"}
 val passenger=client("passenger")
 val held=passenger.act(trip.id,"reserve",java.util.UUID.randomUUID().toString())
 scrollText("Passenger pickup code");ui.onNodeWithText("Passenger pickup code").performTextReplacement(held.pin!!)
 scrollText("Verify at pickup");ui.onNodeWithText("Verify at pickup").performClick()
 scrollText("Start shared trip");ui.waitUntil(30000){runCatching{ui.onNodeWithText("Start shared trip").assertIsEnabled()}.isSuccess}
 ui.onNodeWithText("Start shared trip").performClick()
 waitText("in_progress",true)
 scenario!!.recreate()
 waitText("Resume your shared trip");ui.onNodeWithText("Resume your shared trip").performClick()
 waitText("Complete shared trip");scrollText("Complete shared trip");ui.onNodeWithText("Complete shared trip").performClick()
 val result=RidaApiClient.transport.detail(trip.id)
 Assert.assertEquals("completed",result.status);Assert.assertEquals(700L,result.receipt!!.fee)
 }
}

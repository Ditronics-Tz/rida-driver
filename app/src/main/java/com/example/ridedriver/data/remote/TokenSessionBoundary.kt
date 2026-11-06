package com.example.ridedriver.data.remote

import com.squareup.moshi.Moshi
import okio.ByteString.Companion.decodeBase64

/** JWT decoding here only prevents cross-account retry. The server still verifies
 * signatures and authorization; decoded claims are never trusted as credentials. */
internal object TokenSessionBoundary {
 private val adapter=Moshi.Builder().build().adapter(Map::class.java)
 private fun account(token:String?):String?=runCatching {
 val payload=token?.split('.')?.getOrNull(1)?.decodeBase64()?.utf8()?:return@runCatching null
 val claims=adapter.fromJson(payload)?:return@runCatching null
 ((claims["sub"]?:claims["user_id"]) as? String)?.takeIf{it.isNotBlank()}
 }.getOrNull()
 fun canReplay(previous:String?,current:String?):Boolean {
 val identity=account(previous)?:return false
 return identity==account(current)
 }
}

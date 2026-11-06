package com.example.ridedriver.data.remote

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class TransportRecoveryTest {
 private fun token(id:String,tag:String)= "header."+Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sub\":\"$id\"}").toByteArray())+".$tag"
 @Test fun tokenRotationCanRetryWithinSameAccount(){assertTrue(TokenSessionBoundary.canReplay(token("a","old"),token("a","new")))}
 @Test fun accountSwitchNeverReplaysEarlierMutation(){assertFalse(TokenSessionBoundary.canReplay(token("a","old"),token("b","new")));assertFalse(TokenSessionBoundary.canReplay(null,token("b","new")))}
 @Test fun malformedTokensCannotAuthorizeRetry(){assertFalse(TokenSessionBoundary.canReplay("bad","bad"))}
 @Test fun countdownUsesServerDeadlineAndMonotonicElapsedTime(){assertEquals(300L,remainingHoldSeconds(1300,1000,5000,5000));assertEquals(0L,remainingHoldSeconds(1300,1000,5000,305000));assertEquals(0L,remainingHoldSeconds(1300,1000,5000,400000))}
 private fun trip(version:Long,status:String)=SharedTrip("trip",SharedPoint(-6.94,39.19,"Mbande"),SharedPoint(-6.82,39.28,"Posta"),"car",2,2000,700,status,1300,0,0,1,1000,version=version)
 @Test fun lateSnapshotsCannotUndoCompletion(){val latest=trip(4,"completed");assertEquals(latest,mergeTransportSnapshots(listOf(latest),listOf(trip(3,"in_progress"))).single())}
}

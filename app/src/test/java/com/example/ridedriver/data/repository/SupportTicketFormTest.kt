package com.example.ridedriver.data.repository

import com.example.ridedriver.data.remote.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume

class SupportTicketFormTest {
    @Test fun failedSubmitKeepsDraftAndNeverReportsQueuedOrSubmitted() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val api = Proxy.newProxyInstance(RidaApi::class.java.classLoader, arrayOf(RidaApi::class.java)) { _, _, _ -> throw java.io.IOException("offline") } as RidaApi
            val form = SupportTicketForm(api, scope)
            form.edit("Fare problem", "Please investigate this fare")
            form.submit()
            assertEquals("Fare problem", form.state.value.subject)
            assertNull(form.state.value.ticketId)
            assertNotNull(form.state.value.error)
            assertFalse(form.state.value.sending)
        } finally { scope.cancel() }
    }
    @Test fun duplicateTapIsIgnoredAndOnlyAcknowledgementClearsDraft() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var calls = 0
            lateinit var response: Continuation<TicketDto>
            val api = Proxy.newProxyInstance(RidaApi::class.java.classLoader, arrayOf(RidaApi::class.java)) { _, _, args ->
                calls++
                @Suppress("UNCHECKED_CAST")
                response = args.last() as Continuation<TicketDto>
                COROUTINE_SUSPENDED
            } as RidaApi
            val form = SupportTicketForm(api, scope)
            form.edit("Fare problem", "Please investigate this fare")
            form.submit(); form.submit()
            assertEquals(1, calls)
            assertEquals("Fare problem", form.state.value.subject)
            assertTrue(form.state.value.sending)
            response.resume(TicketDto("ticket", "Fare problem", "Details", "driver", "open"))
            assertEquals("ticket", form.state.value.ticketId)
            assertEquals("", form.state.value.subject)
        } finally { scope.cancel() }
    }
}

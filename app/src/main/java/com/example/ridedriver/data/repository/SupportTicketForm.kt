package com.example.ridedriver.data.repository

import com.example.ridedriver.data.remote.CreateTicketRequest
import com.example.ridedriver.data.remote.RidaApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class SupportDraft(
    val subject: String = "", val description: String = "", val sending: Boolean = false,
    val ticketId: String? = null, val error: String? = null
)

/** A failed request is an editable draft; there is no offline submission queue. */
internal class SupportTicketForm(private val api: RidaApi, private val scope: CoroutineScope) {
    private val mutable = MutableStateFlow(SupportDraft())
    val state = mutable.asStateFlow()
    fun edit(subject: String = mutable.value.subject, description: String = mutable.value.description) {
        if (!mutable.value.sending) mutable.value = SupportDraft(subject, description)
    }
    fun submit() {
        val draft = mutable.value
        if (draft.sending || draft.subject.isBlank() || draft.description.isBlank()) return
        mutable.value = draft.copy(sending = true, error = null, ticketId = null)
        scope.launch {
            try {
                val ticket = api.createSupportTicket(CreateTicketRequest(draft.subject.trim(), draft.description.trim(), "driver"))
                currentCoroutineContext().ensureActive()
                mutable.value = SupportDraft(ticketId = ticket.id)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                mutable.value = draft.copy(error = "Could not submit ticket. Your draft is kept; please retry.")
            }
        }
    }
}

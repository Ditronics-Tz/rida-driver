package com.example.ridedriver.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridedriver.data.driver.*
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.remote.*
import com.example.ridedriver.data.repository.UserRepository
import com.example.ridedriver.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val PageBg = Color(0xFFF4FAF7)
private val PageInk = Color(0xFF17232C)
private val PageMuted = Color(0xFF647995)
private val PageGreen = Color(0xFF009568)
private val PageMint = Color(0xFFE5F5EE)
private val PageForest = Color(0xFF075844)
private val PageBorder = Color(0xFFE4EBE7)
private val PageRed = Color(0xFFD64343)
internal fun driverMoney(value: Long) = "TZS ${NumberFormat.getIntegerInstance(Locale.US).format(value)}"
private fun rideDate(value: Long) = SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(value))
internal fun filterDriverRides(records: List<TripRecord>, query: String, status: TripStatus?): List<TripRecord> = records
    .filter { status == null || it.status == status }
    .filter { query.isBlank() || listOf(it.pickup.address, it.dropoff.address, rideDate(it.endedAtMillis)).any { text -> text.contains(query.trim(), true) } }
    .sortedByDescending { it.endedAtMillis }

@Composable
internal fun DriverPage(title: String, subtitle: String, onBack: () -> Unit, status: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(PageBg).statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) { RidaIcon(RidaIcons.Back, "Back", tint = PageInk) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 24.dp)) {
                Box(Modifier.width(5.dp).height(16.dp).background(PageGreen, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(3.dp))
                Box(Modifier.width(5.dp).height(16.dp).background(PageGreen, RoundedCornerShape(2.dp)))
                Text(" Rida", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = PageInk)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = PageInk, modifier = Modifier.weight(1f))
            status?.let { Badge(it, it == "Cancelled") }
        }
        Text(subtitle, fontSize = 13.sp, lineHeight = 19.sp, color = PageMuted, modifier = Modifier.padding(start = 4.dp, top = 5.dp, bottom = 25.dp))
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable private fun PageCard(color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = color, shape = RoundedCornerShape(20.dp), border = if (color == Color.White) BorderStroke(1.dp, PageBorder) else null) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
@Composable private fun SectionTitle(text: String) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PageInk, modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 16.dp)) }
@Composable private fun Eyebrow(text: String) { Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PageMuted, modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 18.dp)) }
@Composable private fun Badge(text: String, bad: Boolean = false) {
    Surface(color = if (bad) Color(0xFFFCEEF0) else PageMint, shape = RoundedCornerShape(30.dp)) {
        Text(text, color = if (bad) PageRed else PageGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
    }
}
@Composable private fun PageRow(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = onClick), color = Color.White, shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, PageBorder)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(PageMint), contentAlignment = Alignment.Center) { RidaIcon(icon, null, Modifier.size(22.dp), PageGreen) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PageInk)
                Text(subtitle, fontSize = 11.sp, lineHeight = 17.sp, color = PageMuted, modifier = Modifier.padding(top = 4.dp))
            }
            RidaIcon(RidaIcons.ChevronRight, null, Modifier.size(18.dp), PageMuted)
        }
    }
}
@Composable private fun Action(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PageGreen)) { Text(text, fontWeight = FontWeight.Bold) }
}
@Composable private fun ErrorRow(error: String?, onRetry: () -> Unit) {
    if (error != null) { Text(error, color = PageRed); TextButton(onClick = onRetry) { Text("Try again") } }
}
private fun userError(e: Exception): String { if (e is CancellationException) throw e; return RidaApiErrors.message(e, "Couldn't load this information. Please try again.") }

@Composable
fun EarningsScreen(onTripClick: (String) -> Unit = {}, onBack: () -> Unit = {}, onBreakdown: () -> Unit = {}, onWallet: () -> Unit = {}) {
    val repo = remember { DriverRepositoryProvider.get() }
    val state by repo.state.collectAsState()
    var summary by remember { mutableStateOf<EarningsSummary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(retry, state.history) { try { summary = repo.getEarnings(EarningsPeriod.WEEK); error = null } catch (e: Exception) { error = userError(e) } }
    DriverPage("My earnings", "See what you have earned with Rida.", onBack) {
        ErrorRow(error) { retry++ }
        PageCard(PageForest) {
            Text("TOTAL EARNINGS THIS WEEK", color = Color(0xFFB8E4D5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(summary?.let { driverMoney(it.netTzs) } ?: "—", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 22.dp))
            Text(summary?.let { "${it.trips} completed trips" } ?: "Loading earnings…", color = Color.White, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { SectionTitle("Earnings trend"); Badge("7 days") }
        PageCard {
            val daily = summary?.daily.orEmpty()
            if (daily.isEmpty()) Text("No earnings recorded this week.", color = PageMuted, modifier = Modifier.padding(vertical = 48.dp))
            else Row(Modifier.fillMaxWidth().height(172.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                val max = daily.maxOfOrNull { it.netTzs }?.coerceAtLeast(1) ?: 1L
                daily.takeLast(7).forEachIndexed { i, day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(Modifier.width(24.dp).height((126f * day.netTzs.coerceAtLeast(0) / max).coerceIn(3f, 126f).dp).background(if (i == daily.takeLast(7).lastIndex) PageGreen else Color(0xFFBCE5D5), RoundedCornerShape(6.dp)))
                        Text(day.label.take(3), fontSize = 11.sp, color = PageMuted, modifier = Modifier.padding(top = 14.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        PageRow(RidaIcons.Earnings, "Earnings breakdown", "See fares, tips and adjustments", onBreakdown)
        PageRow(RidaIcons.Wallet, "Wallet & payouts", "Manage your driver payments", onWallet)
    }
}

@Composable
fun EarningsBreakdownScreen(onBack: () -> Unit, onTrips: () -> Unit) {
    var summary by remember { mutableStateOf<EarningsSummary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(retry) { try { summary = DriverRepositoryProvider.get().getEarnings(EarningsPeriod.WEEK); error = null } catch(e: Exception) { error = userError(e) } }
    DriverPage("Earnings breakdown", "Details of your driving income.", onBack) {
        ErrorRow(error) { retry++ }
        PageCard {
            Text("EARNED THIS WEEK", color = PageMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(summary?.let { driverMoney(it.netTzs) } ?: "—", color = PageInk, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Badge("${summary?.trips ?: 0} rides")
            }
        }
        SectionTitle("How your earnings add up")
        PageCard {
            AmountLine("Trip fares", summary?.grossTzs)
            AmountLine("Platform fees", summary?.commissionTzs?.let { -it })
            AmountLine("Your earnings", summary?.netTzs, last = true)
            Text("Only recorded trip fares and fees are shown. Tips and bonuses will appear when supplied by your account.", fontSize = 11.sp, color = PageMuted, modifier = Modifier.padding(top = 14.dp))
        }
        Spacer(Modifier.height(22.dp))
        PageRow(RidaIcons.Calendar, "View individual trips", "See ride-by-ride payment details", onTrips)
    }
}
@Composable private fun AmountLine(label: String, amount: Long?, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 17.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PageInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(amount?.let(::driverMoney) ?: "—", color = if ((amount ?: 0) < 0) PageRed else PageInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
    if (!last) HorizontalDivider(color = PageBorder)
}

@Composable
fun TripsHistoryScreen(onTripClick: (String) -> Unit, onBack: () -> Unit = {}) {
    val repo = remember { DriverRepositoryProvider.get() }
    val state by repo.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All rides") }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(retry) { loading = true; repo.refreshHistory().onFailure { error = userError(it as? Exception ?: Exception(it)) }.onSuccess { error = null }; loading = false }
    val rows = filterDriverRides(state.history, query, when(filter) { "Completed" -> TripStatus.COMPLETED; "Cancelled" -> TripStatus.CANCELLED; else -> null })
    DriverPage("Ride history", "Your completed and cancelled trips.", onBack) {
        OutlinedTextField(query, { query = it }, placeholder = { Text("Search by destination or date", fontSize = 13.sp) }, leadingIcon = { RidaIcon(RidaIcons.Search, null, Modifier.size(20.dp), PageMuted) }, singleLine = true, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, unfocusedBorderColor = PageBorder))
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All rides", "Completed", "Cancelled").forEach { label ->
                FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label, fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PageGreen, selectedLabelColor = Color.White), border = null, shape = RoundedCornerShape(30.dp))
            }
        }
        Eyebrow("RECENT RIDES")
        ErrorRow(error) { retry++ }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!loading && rows.isEmpty()) Text("No rides match your search.", color = PageMuted)
        rows.forEach { trip ->
            Surface(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onTripClick(trip.id) }, color = Color.White, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, PageBorder)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(PageMint, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { RidaIcon(RidaIcons.Vehicle, null, tint = PageGreen) }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("${trip.pickup.address} → ${trip.dropoff.address}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PageInk)
                        Text(rideDate(trip.endedAtMillis), fontSize = 11.sp, color = PageMuted, modifier = Modifier.padding(vertical = 6.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(driverMoney(trip.netTzs), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PageGreen)
                            Badge(if (trip.status == TripStatus.COMPLETED) "Completed" else "Cancelled", trip.status == TripStatus.CANCELLED)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripDetailScreen(tripId: String, onBack: () -> Unit, onHelp: () -> Unit = {}) {
    val repo = remember { DriverRepositoryProvider.get() }
    val state by repo.state.collectAsState()
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(tripId, retry) { repo.refreshHistory().onFailure { error = userError(it as? Exception ?: Exception(it)) }.onSuccess { error = null }; loaded = true }
    val trip = state.history.firstOrNull { it.id == tripId }
    DriverPage("Ride details", "Trip receipt and journey information.", onBack, status = trip?.let { if(it.status == TripStatus.COMPLETED) "Completed" else "Cancelled" }) {
        ErrorRow(error) { retry++ }
        if (trip == null) { Text(if (loaded) "This ride could not be found in your history." else "Loading ride…", color = PageMuted) }
        else {
            PageCard {
                Text("YOUR EARNINGS", color = PageMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(driverMoney(trip.netTzs), color = PageInk, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
                Text(rideDate(trip.endedAtMillis), fontSize = 12.sp, color = PageMuted)
            }
            Spacer(Modifier.height(16.dp))
            PageCard {
                Row {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 10.dp, end = 14.dp)) {
                        Box(Modifier.size(16.dp).background(PageGreen, CircleShape)); Box(Modifier.width(2.dp).height(62.dp).background(PageGreen)); Surface(Modifier.size(18.dp), shape = CircleShape, border = BorderStroke(2.dp, PageGreen)) {}
                    }
                    Column {
                        Text("PICKUP", fontSize = 10.sp, color = PageMuted, fontWeight = FontWeight.Bold)
                        Text(trip.pickup.address, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PageInk)
                        Spacer(Modifier.height(48.dp))
                        Text("DROP-OFF", fontSize = 10.sp, color = PageMuted, fontWeight = FontWeight.Bold)
                        Text(trip.dropoff.address, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PageInk)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            PageCard {
                DetailLine("Trip distance", String.format(Locale.getDefault(), "%.1f km", trip.distanceMeters / 1000.0)); HorizontalDivider(color = PageBorder)
                DetailLine("Trip duration", "${trip.durationSeconds / 60} min")
                trip.cancelReason?.let { Text(it, color = PageMuted, fontSize = 12.sp) }
            }
            Spacer(Modifier.height(18.dp))
            PageRow(RidaIcons.Help, "Get help with this ride", "Report a fare or passenger issue", onHelp)
        }
    }
}
@Composable private fun DetailLine(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = PageMuted, fontSize = 12.sp); Text(value, fontSize = 13.sp, color = PageInk, fontWeight = FontWeight.SemiBold) } }

@Composable
fun WalletScreen(onBack: () -> Unit) {
    var balance by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var method by rememberSaveable { mutableStateOf("mpesa") }
    var phone by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    // Retain a request key across retries; a changed form starts a new request.
    var requestKey by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    LaunchedEffect(retry) { try { balance = (RidaApiClient.api.wallet()["balance"] as? Number)?.toLong() ?: kotlin.error("Wallet response has no balance"); error = null } catch(e: Exception) { error = userError(e) } }
    DriverPage("Wallet & payouts", "Manage your driver payments.", onBack) {
        ErrorRow(error) { retry++ }
        PageCard(PageForest) {
            Text("AVAILABLE FOR PAYOUT", color = Color(0xFFB8E4D5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(balance?.let(::driverMoney) ?: "—", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(vertical = 24.dp))
        }
        SectionTitle("Payout methods")
        PageRow(RidaIcons.Payment, "M-Pesa", "Withdraw to your mobile money account") { if(method != "mpesa") requestKey = UUID.randomUUID().toString(); method = "mpesa"; showForm = true }
        PageRow(RidaIcons.Add, "Airtel Money", "Use another mobile money account") { if(method != "airtelmoney") requestKey = UUID.randomUUID().toString(); method = "airtelmoney"; showForm = true }
        PageRow(RidaIcons.Payout, "Bank transfer", "Contact support to arrange bank payouts") { notice = "Bank payouts are not currently supported by the app. Contact driver support for assistance." }
        Text("Your withdrawal timing may depend on provider.", color = PageMuted, fontSize = 12.sp, modifier = Modifier.padding(4.dp, 10.dp))
        notice?.let { Text(it, color = PageGreen, modifier = Modifier.padding(vertical = 12.dp)) }
        Spacer(Modifier.height(10.dp))
        Action("Withdraw earnings", balance != null && !busy) { showForm = true }
    }
    if(showForm) AlertDialog(onDismissRequest = { if(!busy) showForm = false }, title = { Text("Withdraw earnings") }, text = {
        Column {
            Text(if(method == "mpesa") "M-Pesa" else "Airtel Money")
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit); requestKey = UUID.randomUUID().toString() }, label = { Text("Amount (TZS)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(phone, { phone = it; requestKey = UUID.randomUUID().toString() }, label = { Text("Mobile money number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            Text("The payout is submitted for processing, not paid instantly.", fontSize = 12.sp)
            error?.let { Text(it, color = PageRed) }
        }
    }, confirmButton = { TextButton(enabled = !busy, onClick = {
        val value = amount.toLongOrNull()
        val normalized = com.example.ridedriver.ui.common.TzPhone.normalize(phone)
        if(value == null || value <= 0 || value > (balance ?: 0)) error = "Enter an amount within your available balance."
        else if(normalized == null) error = "Enter a valid Tanzania mobile money number."
        else confirm = true
    }) { Text("Review withdrawal") } }, dismissButton = { TextButton(enabled = !busy, onClick = { showForm = false }) { Text("Cancel") } })
    if(confirm) AlertDialog(onDismissRequest = { if(!busy) confirm = false }, title = { Text("Confirm payout") }, text = { Text("Request TZS $amount to $phone via ${if(method == "mpesa") "M-Pesa" else "Airtel Money"}?") }, confirmButton = {
        TextButton(enabled = !busy, onClick = { scope.launch {
            busy = true
            try {
                RidaApiClient.api.requestPayout(requestKey, DriverPayoutRequest(amount.toLong(), method, com.example.ridedriver.ui.common.TzPhone.normalize(phone).orEmpty()))
                notice = "Payout request submitted. You will be notified when it is processed."
                confirm = false; showForm = false; retry++; requestKey = UUID.randomUUID().toString()
            } catch(e: Exception) { error = userError(e); confirm = false } finally { busy = false }
        } }) { Text(if(busy) "Submitting…" else "Confirm") }
    }, dismissButton = { TextButton(enabled = !busy, onClick = { confirm = false }) { Text("Back") } })
}

@Composable
fun SupportScreen(onBack: () -> Unit, onCase: (String) -> Unit = {}, rideId: String? = null) {
    var category by remember { mutableStateOf("ride") }
    var form by remember { mutableStateOf(false) }
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var cases by remember { mutableStateOf<List<TicketDto>>(emptyList()) }
    var retry by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(retry) { try { cases = RidaApiClient.api.getSupportTickets().tickets; error = null } catch(e: Exception) { error = userError(e) } }
    fun start(topic: String) { category = topic; subject = ""; description = rideId?.let { "Ride: $it\n" }.orEmpty(); form = true }
    DriverPage("Driver support", "Help with rides, account and earnings.", onBack) {
        PageCard(PageMint) {
            Text("Need assistance?", color = PageInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Get support from the Rida team", color = PageMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            TextButton(onClick = { start("ride") }) { Text("Contact support →", color = PageGreen) }
        }
        SectionTitle("What can we help with?")
        PageRow(RidaIcons.Vehicle, "A recent ride", "Trip, pickup or passenger issue") { start("ride") }
        PageRow(RidaIcons.Payment, "Payments & earnings", "Payouts, charges and balances") { start("payment") }
        PageRow(RidaIcons.Document, "Account & documents", "Identity and driver profile") { start("account") }
        PageRow(RidaIcons.Safety, "Safety & emergencies", "Report urgent or safety concerns") { start("safety") }
        if(cases.isNotEmpty()) {
            SectionTitle("Your support cases")
            cases.forEach { ticket -> PageRow(RidaIcons.Chat, ticket.subject, ticket.status.replace('_',' ')) { onCase(ticket.id) } }
        }
        ErrorRow(error) { retry++ }
    }
    if(form) AlertDialog(onDismissRequest = { if(!sending) form = false }, title = { Text("Contact support") }, text = {
        Column {
            if(category == "safety") TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) }) { Text("Emergency? Call 112") }
            OutlinedTextField(subject, { subject = it }, label = { Text("Subject") })
            OutlinedTextField(description, { description = it }, label = { Text("What happened?") }, minLines = 3)
            error?.let { Text(it, color = PageRed) }
        }
    }, confirmButton = { TextButton(enabled = !sending && subject.isNotBlank() && description.isNotBlank(), onClick = { scope.launch {
        sending = true
        try { val ticket = RidaApiClient.api.createSupportTicket(CreateTicketRequest(subject.trim(), description.trim(), category)); form = false; retry++; onCase(ticket.id) }
        catch(e: Exception) { error = userError(e) } finally { sending = false }
    } }) { Text(if(sending) "Sending…" else "Submit case") } }, dismissButton = { TextButton(enabled = !sending, onClick = { form = false }) { Text("Cancel") } })
}

@Composable
fun SupportCaseScreen(id: String, onBack: () -> Unit) {
    var data by remember(id) { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reply by rememberSaveable(id) { mutableStateOf("") }
    var showReply by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var key by rememberSaveable(id) { mutableStateOf(UUID.randomUUID().toString()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(id, retry) { try { data = RidaApiClient.api.supportCase(id); error = null } catch(e: Exception) { error = userError(e) } }
    val ticket = data["ticket"] as? Map<*, *> ?: emptyMap<Any,Any>()
    val messages = data["messages"] as? List<*> ?: emptyList<Any>()
    DriverPage("Support case #${id.take(8).uppercase()}", ticket["subject"] as? String ?: "Your support conversation", onBack) {
        ErrorRow(error) { retry++ }
        PageCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Current status", color = PageMuted, fontSize = 12.sp); Badge(ticket["status"]?.toString()?.replaceFirstChar(Char::uppercase) ?: "Loading") }
            Text(ticket["created_at"]?.toString()?.replace('T',' ')?.take(16).orEmpty(), fontWeight = FontWeight.SemiBold, color = PageInk, modifier = Modifier.padding(top = 10.dp))
        }
        SectionTitle("Case updates")
        PageCard {
            Text("Request received", fontWeight = FontWeight.Bold, color = PageInk)
            Text(ticket["description"]?.toString().orEmpty(), color = PageMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
            messages.forEach { value ->
                val message = value as? Map<*, *> ?: return@forEach
                HorizontalDivider(color = PageBorder)
                Text(message["body"]?.toString().orEmpty(), color = PageInk, modifier = Modifier.padding(top = 14.dp))
                Text(message["created_at"]?.toString()?.replace('T',' ')?.take(16).orEmpty(), color = PageMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp))
            }
            if(messages.isEmpty()) Text("Awaiting response. Check back here for updates.", color = PageMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(22.dp))
        PageCard(PageMint) { Text("Add more information", fontWeight = FontWeight.Bold, color = PageInk); Text("Reply if you have new details that could help resolve your case.", color = PageMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(18.dp))
        Action("Reply to support", ticket.isNotEmpty()) { showReply = true }
    }
    if(showReply) AlertDialog(onDismissRequest = { if(!busy) showReply = false }, title = { Text("Reply to support") }, text = { Column { OutlinedTextField(reply, { reply = it; key = UUID.randomUUID().toString() }, label = { Text("Your message") }, minLines = 3); error?.let { Text(it, color = PageRed) } } }, confirmButton = { TextButton(enabled = !busy && reply.isNotBlank(), onClick = { scope.launch {
        busy = true
        try { RidaApiClient.api.replyToCase(id, key, mapOf("body" to reply.trim())); reply = ""; key = UUID.randomUUID().toString(); showReply = false; retry++ }
        catch(e: Exception) { error = userError(e) } finally { busy = false }
    } }) { Text(if(busy) "Sending…" else "Send reply") } }, dismissButton = { TextButton(enabled = !busy, onClick = { showReply = false }) { Text("Cancel") } })
}

@Composable
fun DriverProfileScreen(onNavigateToVehicle: () -> Unit, onEditProfile: () -> Unit = {}, onNavigateToDocuments: () -> Unit, onNavigateToSettings: () -> Unit, onNavigateToSupport: () -> Unit, onBack: () -> Unit = {}, onPrivacy: () -> Unit = {}) {
    val context = LocalContext.current
    val session by SessionManager.observe(context).collectAsState(initial = SessionManager.Session())
    LaunchedEffect(Unit) { UserRepository.refreshSessionFromApi(context) }
    DriverPage("Driver profile", "Manage your driver account.", onBack) {
        PageCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(session.name, session.profilePictureUrl, Modifier.size(68.dp), photoVersion = session.profilePictureVersion)
                Column(Modifier.padding(start = 16.dp)) {
                    Text(session.name.ifBlank { "Driver account" }, color = PageInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(session.email, color = PageMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    Badge(if(session.loggedIn) "Signed in" else "Signed out")
                }
            }
        }
        Eyebrow("ACCOUNT & DOCUMENTS")
        PageRow(RidaIcons.Account, "Personal details", "Name, phone, email and profile photo", onEditProfile)
        PageRow(RidaIcons.Vehicle, "Vehicle information", "Registration and inspection", onNavigateToVehicle)
        PageRow(RidaIcons.Document, "Driver documents", "License and verification status", onNavigateToDocuments)
        PageRow(RidaIcons.Settings, "Availability preferences", "Ride preferences and settings", onNavigateToSettings)
        PageRow(RidaIcons.Safety, "Privacy & security", "Control your account and personal data", onPrivacy)
        PageRow(RidaIcons.Help, "Driver support", "Get help with your account", onNavigateToSupport)
    }
}

@Composable
fun PrivacySecurityScreen(onBack: () -> Unit, onSupport: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    DriverPage("Privacy & security", "Control your account and personal data.", onBack) {
        PageCard(PageMint) { Text("Your security matters", color = PageInk, fontWeight = FontWeight.Bold); Text("Review and keep your settings up to date.", color = PageMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
        Eyebrow("ACCOUNT SECURITY")
        PageRow(RidaIcons.Lock, "Password", "Update your account password") { dialog = "Password" }
        PageRow(RidaIcons.Safety, "Two-step verification", "Protect your sign-in") { dialog = "Two-step verification" }
        Eyebrow("DATA & PERMISSIONS")
        PageRow(RidaIcons.SavedPlace, "Location permissions", "Choose when location is shared") { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
        PageRow(RidaIcons.Document, "Personal data", "View or request your information") { dialog = "Personal data" }
        TextButton(onClick = { dialog = "Delete my account" }, modifier = Modifier.padding(top = 18.dp)) { Text("Delete my account", color = PageRed, fontWeight = FontWeight.SemiBold) }
        notice?.let { Text(it, color = PageMuted, modifier = Modifier.padding(8.dp)) }
    }
    dialog?.let { title ->
        AlertDialog(onDismissRequest = { if(!busy) dialog = null }, title = { Text(title) }, text = { Column {
            Text(when(title) {
                "Password" -> "Password changes aren't currently available in the app. Contact support for help recovering your account."
                "Two-step verification" -> "Two-step verification for driver accounts isn't available yet. Your Google account can use Google's own two-step verification."
                "Personal data" -> "Request a copy of your account information. You can track available exports with Rida support."
                else -> "Request account deletion. Retention rules or outstanding obligations may delay deletion. Type DELETE to confirm."
            })
            if(title == "Delete my account") OutlinedTextField(confirmDelete, { confirmDelete = it }, label = { Text("Type DELETE") })
            notice?.let { Text(it, color = PageMuted) }
        } }, confirmButton = { TextButton(enabled = !busy && (title != "Delete my account" || confirmDelete == "DELETE"), onClick = {
            if(title in listOf("Password", "Two-step verification")) { dialog = null; onSupport() }
            else scope.launch {
                busy = true
                try {
                    if(title == "Personal data") { RidaApiClient.api.requestDataExport(); notice = "Your data export has been requested."; dialog = null }
                    else { RidaApiClient.api.requestAccountDeletion(); SessionManager.clear(context); dialog = null; onDeleted() }
                } catch(e: Exception) { notice = userError(e) } finally { busy = false }
            }
        }) { Text(if(busy) "Submitting…" else when(title) { "Personal data" -> "Request export"; "Delete my account" -> "Request deletion"; else -> "Contact support" }) } }, dismissButton = { TextButton(enabled = !busy, onClick = { dialog = null }) { Text("Cancel") } })
    }
}

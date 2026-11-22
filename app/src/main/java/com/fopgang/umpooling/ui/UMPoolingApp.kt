package com.fopgang.umpooling.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ai.VoiceRequestScreen
import com.fopgang.umpooling.auth.*
import com.fopgang.umpooling.booking.*
import com.fopgang.umpooling.booking.DemoRideIds
import com.fopgang.umpooling.booking.RideMatchScreen
import com.fopgang.umpooling.booking.BookingConfirmationScreen
import com.fopgang.umpooling.booking.OfferRideScreen
import com.fopgang.umpooling.booking.FindRideScreen
import com.fopgang.umpooling.booking.ConfirmedTripTicketScreen
import com.fopgang.umpooling.booking.RequestFutureRideScreen
import com.fopgang.umpooling.home.HomeScreen
import com.fopgang.umpooling.notifications.LiveNotificationScreen
import com.fopgang.umpooling.matching.SavedRoutesScreen
import com.fopgang.umpooling.profile.ProfileScreen
import com.fopgang.umpooling.safety.*
import com.fopgang.umpooling.ui.components.PeachBackground
import com.fopgang.umpooling.chat.ChatScreen
import com.fopgang.umpooling.dashboard.GreenCommuterTierScreen
import com.fopgang.umpooling.dashboard.SustainabilityDashboardScreen
import com.fopgang.umpooling.notifications.NotificationPreferences
import com.fopgang.umpooling.notifications.NotificationSettingsScreen

import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import java.time.Clock
import com.fopgang.umpooling.coordination.*
import androidx.compose.ui.platform.LocalContext

/** Session access is decided before rendering destinations, including restored history. */
@Composable
fun UMPoolingApp(authViewModel: AuthViewModel) {
    val state by authViewModel.state.collectAsState()
    if (state.access == AuthAccess.Restoring) {
        PeachBackground { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        return
    }
    val scopeKey = when (val access = state.access) {
        is AuthAccess.Authenticated -> "user/" + access.user.id
        is AuthAccess.VerificationRequired -> "verify/" + access.email
        AuthAccess.Demo -> "demo"
        else -> "signed-out"
    }
    // Discard the prior account's back stack and form state on every access-boundary change.
    key(scopeKey) { UMPoolingContent(authViewModel, state) }
}

@Composable
private fun UMPoolingContent(authViewModel: AuthViewModel, authState: AuthUiState) {
    val nav = rememberNavController()
    val start = when(authState.access) {
        is AuthAccess.Authenticated, AuthAccess.Demo -> "home"
        is AuthAccess.VerificationRequired -> "verify"
        else -> "welcome"
    }
    val demo = authState.access == AuthAccess.Demo
    val protected = demo || authState.access is AuthAccess.Authenticated
    var notice by rememberSaveable { mutableIntStateOf(0) }
    var pendingOffer by rememberSaveable(stateSaver = OfferRideDraftSaver) { mutableStateOf(OfferRideDraft()) }
    var criteria by rememberSaveable(stateSaver = FindRideCriteriaSaver) { mutableStateOf(FindRideCriteria()) }
    var transcript by rememberSaveable { mutableStateOf("") }
    var pendingGroups by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var activeBooking by rememberSaveable { mutableStateOf<String?>(null) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var cancelled by rememberSaveable { mutableStateOf(false) }
    var bookedSeats by rememberSaveable { mutableIntStateOf(1) }
    val owner=(authState.access as? AuthAccess.Authenticated)?.user?.id
    val context=LocalContext.current
    val accountStoreOwner=remember {
        object: androidx.lifecycle.ViewModelStoreOwner {
            override val viewModelStore=androidx.lifecycle.ViewModelStore()
        }
    }
    DisposableEffect(accountStoreOwner) { onDispose {accountStoreOwner.viewModelStore.clear()} }
    val accountViewModel: AccountViewModel = androidx.lifecycle.viewmodel.compose.viewModel(viewModelStoreOwner=accountStoreOwner,key="account/"+(owner ?: "demo"),
        factory=object: androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T: androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AccountViewModel(AccountStorage(context.applicationContext,owner ?: "demo")) as T
            }
        })
    val account by accountViewModel.state.collectAsState()
    val notificationPreferences=account.preferences
    val savePreferences:(NotificationPreferences)->Unit=accountViewModel::savePreferences
    LaunchedEffect(account.saveFailed) {
        if(account.saveFailed) {notice=R.string.not_connected;accountViewModel.dismissError()}
    }
    val link by com.fopgang.umpooling.notifications.NotificationLinks.pending.collectAsState()
    val unavailable: () -> Unit = { notice = R.string.not_connected }
    val message: (Int) -> Unit = { notice = it }
    val navigate: (String) -> Unit = navigate@{ requested ->
        if(requested == "demo") { authViewModel.enterDemo(); return@navigate }
        val legacy = when(requested) {
            "ticket" -> "my_trips"
            "request_current_ride" -> BookingRoute.OfferDetails(DemoRideIds.HOME_OFFER).encode()
            else -> requested
        }
        val booking = BookingRoute.decode(legacy)
        val destination = if(booking != null) {
            val prefix = when(booking) {
                is BookingRoute.OfferDetails -> "offer_details"
                is BookingRoute.Confirmation -> "booking_confirmation"
                is BookingRoute.Ticket -> "confirmed_ticket"
            }
            prefix + "/" + Uri.encode(booking.resourceId)
        } else legacy
        val gated = authState.access.destinationFor(destination)
        nav.navigate(gated) {
            launchSingleTop = true
            if(gated in setOf("home","my_trips","notifications","profile","dashboard")) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                restoreState = true
            } else if(gated in setOf("find_ride","offer_ride") &&
                nav.currentDestination?.route in setOf("find_ride","offer_ride")) {
                popUpTo(nav.currentDestination!!.id) { inclusive = true; saveState = true }
                restoreState = true
            }
        }
    }
    val back: () -> Unit = { nav.popBackStack(); Unit }
    val home: () -> Unit = { navigate("home") }
    val openOffer: (String) -> Unit = { navigate(BookingRoute.OfferDetails(it).encode()) }
    LaunchedEffect(link,owner) {if(owner!=null && link!=null) navigate("notification_open")}
    PeachBackground {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            if(demo) Row(Modifier.fillMaxWidth().padding(horizontal=16.dp), verticalAlignment=Alignment.CenterVertically) {
                Text(stringResource(R.string.auth_demo_banner),Modifier.weight(1f))
                TextButton(authViewModel::logout) { Text(stringResource(R.string.auth_exit_demo)) }
            }
            if(authState.busy && protected) LinearProgressIndicator(Modifier.fillMaxWidth())
            NavHost(navController=nav, startDestination=start, modifier=Modifier.widthIn(max=600.dp).fillMaxWidth().weight(1f)) {
                if(!protected && authState.access !is AuthAccess.VerificationRequired) {
                    composable("welcome") { WelcomeScreen(navigate) }
                    composable("login") { AuthScreen(false,navigate,back,message,authState) { authViewModel.submit(false,it) } }
                    composable("register") { AuthScreen(true,navigate,back,message,authState) { authViewModel.submit(true,it) } }
                } else if(authState.access is AuthAccess.VerificationRequired) {
                    composable("verify") { VerificationScreen(authViewModel::logout,message,navigate,authState,authViewModel::verify,authViewModel::resend) }
                } else {
                    composable("home") {
                        HomeScreen(navigate,unavailable,onSearch={ criteria=it; navigate("find_ride") })
                    }
                    composable("find_ride") {
                        FindRideScreen(back,{ navigate("offer_ride") }, {
                            criteria=it
                            navigate(if(it.isImmediate()) "immediate_results" else "request_future_ride")
                        },initialCriteria=criteria, requestText=transcript)
                    }
                    composable("offer_ride") {
                        OfferRideScreen(back,{ navigate("find_ride") }, {
                            pendingOffer=it
                            if(demo) navigate("posted_offer") else unavailable()
                        },initialDraft=pendingOffer)
                    }
                    composable("posted_offer") {
                        FlowDestination("Offer preview",back,"Demo offer only. A live post requires the ride repository.",
                            listOf("View offer" to { openOffer(DemoRideIds.HOME_OFFER) }, "Home" to home))
                    }
                    composable("immediate_results") {
                        FlowDestination("Available rides",back,"Immediate ride results · demo listings",
                            listOf("View driver and ride details" to { openOffer(DemoRideIds.HOME_OFFER) },
                                "Adjust search" to back,"Save a route" to { navigate("saved") },
                                "Request a ride" to unavailable))
                    }
                    composable("request_future_ride") {
                        RequestFutureRideScreen(back,openOffer,criteria,onPendingGroup={
                            if(demo) {
                                pendingGroups=ArrayList((pendingGroups+it).distinct())
                                navigate("pending_group/"+Uri.encode(it))
                            } else unavailable()
                        })
                    }
                    composable("offer_details/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})) { entry ->
                        val id=entry.arguments!!.getString("id")!!
                        RideMatchScreen(id,back,{ navigate(BookingRoute.Confirmation(it).encode()) },
                            { navigate("chat/offer/"+Uri.encode(it)) },unavailable)
                    }
                    composable("booking_confirmation/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})) { entry ->
                        val id=entry.arguments!!.getString("id")!!
                        BookingConfirmationScreen(id,back,{
                            if(demo) {
                                completed=false;cancelled=false;bookedSeats=criteria.seats
                                nav.navigate("my_trips") { popUpTo("home"); launchSingleTop=true }
                                navigate(BookingRoute.Ticket(DemoRideIds.CONFIRMED_BOOKING).encode())
                            } else unavailable()
                        },{ navigate("chat/offer/"+Uri.encode(it)) },unavailable, requestedSeats=criteria.seats)
                    }
                    composable("confirmed_ticket/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})) { entry ->
                        val id=entry.arguments!!.getString("id")!!
                        if(!demo && owner!=null) LiveTripsScreen(owner,back,navigate,bookingId=id)
                        else ConfirmedTripTicketScreen(id,back,{navigate("chat/booking/"+Uri.encode(it))},{
                            if(activeBooking==it) navigate("sos/"+Uri.encode(it)) else navigate("active_trip/"+Uri.encode(it))
                        },reservedSeats=bookedSeats)
                    }
                    composable("my_trips") {
                        if(!demo && owner!=null) LiveTripsScreen(owner,back,navigate)
                        else {
                        val cards=demoTripItems().filterNot {it.status==MyTripStatus.Active && (completed || cancelled)}.map {item->
                            if(item.status==MyTripStatus.Confirmed) item.copy(
                                seats=bookedSeats,
                                status=when {cancelled->MyTripStatus.Cancelled;completed->MyTripStatus.Completed;else->item.status},
                                detailRoute=when {cancelled->"cancelled_trip";completed->"review";else->item.detailRoute})
                            else item
                        } + pendingGroups.map {id->
                            MyTripItem("group/"+id,MyTripStatus.Pending,"Pickup group",id,
                                summary="No driver assigned yet",detailRoute="pending_group/"+Uri.encode(id))
                        }
                        MyTripsScreen(cards,navigate={route->
                            if(route=="active_trip/demo-active") activeBooking="demo-active"
                            navigate(route)
                        },onSos={_->
                            activeBooking="demo-active"
                            message(R.string.sos_demo)
                        })
                        }
                    }
                    composable("pending_group/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})) {
                        FlowDestination("Pending group",back,"No driver assigned. No seat has been reserved.",
                            listOf("My Trips" to {navigate("my_trips")},"Adjust search" to {navigate("find_ride")}))
                    }
                    composable("active_trip/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})) { entry ->
                        val id=entry.arguments!!.getString("id")!!
                        FlowDestination("Trip status",back,if(activeBooking==id) "Active demo trip" else "The driver must start the trip before SOS is available.",
                            if(demo) listOf("Start demo trip" to {if(!completed && !cancelled) activeBooking=id else unavailable()},
                                "Cancel demo trip" to {activeBooking=null;cancelled=true;navigate("my_trips")},
                                "SOS" to {if(activeBooking==id) navigate("sos/"+Uri.encode(id)) else unavailable()},
                                "Complete demo trip" to {if(activeBooking==id){activeBooking=null;completed=true;navigate("review")} else unavailable()})
                            else emptyList())
                    }
                    composable("cancelled_trip") { FlowDestination("Cancelled trip",back,"This demo record is cancelled.",emptyList()) }
                    composable("notifications") {
                        if(!demo && owner!=null) LiveNotificationScreen(owner,back,navigate)
                        else com.fopgang.umpooling.notifications.DemoNotificationScreen(back,navigate)
                    }
                    composable("notification_open") { if(owner!=null) LiveNotificationScreen(owner,back,navigate,link?.payload) }
                    composable("driver_offer/{id}") {entry->if(owner!=null) LiveTripsScreen(owner,back,navigate,offerId=entry.arguments?.getString("id"))}
                    composable("trip/{id}") {entry->if(owner!=null) LiveTripsScreen(owner,back,navigate,tripId=entry.arguments?.getString("id"))}
                    composable("sos_trip/{id}?prepared={prepared}",arguments=listOf(navArgument("prepared"){type=NavType.BoolType;defaultValue=false})) {entry->if(owner!=null) LiveTripsScreen(owner,back,navigate,tripId=entry.arguments?.getString("id"),sos=true,sosPrepared=entry.arguments?.getBoolean("prepared")==true)}
                    composable("chat/trip/{id}") {entry->if(owner!=null) LiveChatScreen(owner,ConversationKind.TRIP,entry.arguments!!.getString("id")!!,back)}
                    composable("profile") { ProfileScreen(navigate,unavailable,message,
                        notificationPreferences.push,{savePreferences(notificationPreferences.copy(push=it))},authViewModel::logout) }
                    composable("profile_setup") { FlowDestination("Commuter profile",back,"Passenger, Driver or Both · Faculty · Residential area · Vehicle details",listOf("Home" to home)) }
                    composable("emergency_contacts") { EmergencyContactScreen(owner ?: "demo",back) }
                    composable("saved") { SavedRoutesScreen(back,unavailable,onNavigate=navigate,initialRoutes=account.routes,showSamples=demo,onRoutesChanged=accountViewModel::saveRoutes) }
                    composable("voice") { VoiceRequestScreen(back,unavailable,message,onManual={transcript=it;navigate("find_ride")}) }
                    listOf("chat","chat/offer/{id}","chat/booking/{id}").forEach { path ->
                        composable(path) { entry ->
                            if(!demo && owner!=null && path=="chat/booking/{id}") LiveChatScreen(owner,ConversationKind.BOOKING,entry.arguments!!.getString("id")!!,back)
                            else if(!demo) FlowDestination("Conversation",back,"Open an existing booking or active trip to chat.",emptyList())
                            else ChatScreen(back,unavailable,conversationId=path+"/"+entry.arguments?.getString("id").orEmpty())
                        }
                    }
                    composable("sos/{id}") { entry ->
                        if(activeBooking==entry.arguments?.getString("id")) SosScreen(back,message)
                        else FlowDestination("SOS unavailable",back,"SOS is available during an active trip.",emptyList())
                    }
                    composable("review") { ReviewScreen(home,message,onSubmit={if(demo) home() else unavailable()}) }
                    composable("dashboard") { SustainabilityDashboardScreen(navigate,unavailable) }
                    composable("tier") { GreenCommuterTierScreen(back) }
                    composable("notification_settings") { NotificationSettingsScreen(notificationPreferences,savePreferences,back) }
                }
            }
        }
    }
    if(authState.issue!=null && authState.access is AuthAccess.Authenticated) AlertDialog(
        onDismissRequest=authViewModel::dismissMessage,text={Text(stringResource(authState.issue.stringId()))},
        confirmButton={TextButton(authViewModel::dismissMessage){Text(stringResource(R.string.close))}})
    if(notice!=0) AlertDialog(onDismissRequest={notice=0},title={Text(stringResource(R.string.demo_title))},
        text={Text(stringResource(notice))},confirmButton={TextButton({notice=0}){Text(stringResource(R.string.close))}})
}
